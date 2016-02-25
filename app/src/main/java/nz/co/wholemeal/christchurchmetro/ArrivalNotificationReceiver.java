/**
 * Copyright 2010-2011 Malcolm Locke
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nz.co.wholemeal.christchurchmetro;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;

/**
 * This class receives the broadcast alarms for arrivals, and creates a status
 * bar notification to alert the user that the bus is due
 */
public class ArrivalNotificationReceiver extends BroadcastReceiver {

    public static final String TAG = "ArrivalNotificationReceiver";
    private String mRouteNumber;
    private String mDestination;
    private String mPlatformTag;
    private String mTripNumber;
    private int mMinutes;
    private Context mContext;
    private Intent mIntent;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive()");

        mContext = context;
        mIntent = intent;
        Bundle extras = intent.getExtras();

        if (extras != null) {
            mRouteNumber = extras.getString("routeNumber");
            mDestination = extras.getString("destination");
            mPlatformTag = extras.getString("platformTag");
            mTripNumber = extras.getString("tripNumber");
            mMinutes = extras.getInt("minutes");

            try {
                Stop stop = new Stop(mPlatformTag, null, context);
                new AsyncArrivalsTask().execute(stop);
            } catch (Stop.InvalidPlatformNumberException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }

    private void handleArrivals(ArrayList<Arrival> arrivals) {

        /**
         * Load the given stop, find the trip that the alarm was set for, and
         * check if the ETA is less than or equal to the requested ETA.  If the
         * ETA is still greater than the requested ETA, create another alarm for
         * the trip in 30 seconds time from now.
         *
         * In the event of any kind of error, just fall through and sound the
         * alarm.  It is better for the alarm to sound early or late than not at
         * all.
         */
        try {
            Iterator<Arrival> iterator = arrivals.iterator();

            while (iterator.hasNext()) {
                Arrival arrival = iterator.next();
                if (!arrival.tripNumber.equals(mTripNumber)) {
                    continue;
                }

                /**
                 * If we're here, we have the correct trip.  If the ETA is still
                 * greater than requested, queue another alarm and exit.
                 */
                int requestedEtaDifference = arrival.eta - mMinutes;
                if (requestedEtaDifference > 0) {
                    Log.d(TAG, "ETA " + arrival.eta + " > " + mMinutes);

                    PendingIntent sender = PendingIntent.getBroadcast(mContext, 0,
                            mIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                    Calendar calendar = Calendar.getInstance();

                    calendar.setTimeInMillis(System.currentTimeMillis());

                    if (requestedEtaDifference > 1) {
                        /**
                         * There's more than a minutes difference between the requested
                         * and actual ETA, so just increment the time to the difference
                         */
                        Log.d(TAG, "Adding " + requestedEtaDifference +
                                " minutes to next alarm");
                        calendar.add(Calendar.MINUTE, requestedEtaDifference);
                    } else {
                        /**
                         * There's only one minute difference between the requested
                         * and actual ETA, so increase the check frequency.
                         */
                        Log.d(TAG, "Adding 30 seconds to next alarm");
                        calendar.add(Calendar.SECOND, 30);
                    }

                    AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
                    alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                            sender);
                    Log.d(TAG, "Reset alarm for " + DateFormat.getTimeInstance().format(calendar.getTime()));

                    // Nothing left to do
                    return;
                }
            }
        } catch (Exception e) {
            /**
             * Fall through after this, and just sound the alarm.  Better it
             * sounds at the wrong time than not at all
             */
            Log.e(TAG, "Exception: " + e.getMessage(), e);
        }

        NotificationManager notificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        int icon = R.drawable.ic_directions_bus_white_24dp;
        long when = System.currentTimeMillis();

        Resources res = mContext.getResources();
        // The text that appears in the status bar
        String tickerText = res.getString(R.string.route_number_to_destination,
                mRouteNumber, mDestination);
        // Text for the notification details
        String dueMinutes = String.format(res.getQuantityString(
                R.plurals.due_in_n_minutes, mMinutes), mMinutes);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.MINUTE, mMinutes);
        String dueTime = DateFormat.getTimeInstance().format(calendar.getTime());
        String dueText = dueMinutes + " (" + dueTime + ")";


        sendNotification(mContext, mPlatformTag, notificationManager, icon, when,
                tickerText, dueText);
    }


    private void sendNotification(Context context, String platformTag,
                                  NotificationManager notificationManager, int icon, long when,
                                  String tickerText, String dueText) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSmallIcon(icon)
                .setContentTitle(tickerText)
                .setContentText(dueText);
        /**
         * Create the Intent that will fire when the user clicks on the
         * notification.  This will take the user to display the relevant
         * platform that they set the alarm for.
         */
        Intent notificationIntent = new Intent(context, PlatformActivity.class);
        notificationIntent.putExtra("platformTag", platformTag);

        /**
         * Create a dummy task stack that will take the user back to the home
         * screen if they click back after clicking the notification.
         */
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(PlatformActivity.class);
        stackBuilder.addNextIntent(notificationIntent);

        PendingIntent pendingIntent = stackBuilder.getPendingIntent(
                0, PendingIntent.FLAG_UPDATE_CURRENT
        );
        mBuilder.setContentIntent(pendingIntent);

        NotificationManager mNotificationManager
                = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE
        );
        mNotificationManager.notify(1, mBuilder.build());
    }

    public class AsyncArrivalsTask extends AsyncTask<Stop, Void, ArrayList<Arrival>> {

        @Override
        protected void onPostExecute(ArrayList<Arrival> arrivals) {
            handleArrivals(arrivals);
        }

        @Override
        protected ArrayList<Arrival> doInBackground(Stop... stops) {
            Log.d(TAG, "Running doInBackground()");
            ArrayList<Arrival> arrivals = null;
            try {
                arrivals = stops[0].getArrivals();
            } catch (Exception e) {
                Log.e(TAG, "getArrivals(): ", e);
            }

            return arrivals;
        }
    }
}
