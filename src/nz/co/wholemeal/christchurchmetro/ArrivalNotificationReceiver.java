/**
 * Copyright 2010-2011 Malcolm Locke
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package nz.co.wholemeal.christchurchmetro;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

/**
 * This class receives the broadcast alarms for arrivals, and creates a status
 * bar notification to alert the user that the bus is due
 */
public class ArrivalNotificationReceiver extends BroadcastReceiver {

  public static final String TAG = "ArrivalNotificationReceiver";

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.d(TAG, "onReceive()");

    Bundle extras = intent.getExtras();

    if (extras != null) {
      String routeNumber = extras.getString("routeNumber");
      String destination = extras.getString("destination");
      String platformTag = extras.getString("platformTag");
      String tripNumber = extras.getString("tripNumber");
      int minutes = extras.getInt("minutes");

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
        Stop stop = new Stop(platformTag, null, context);
        ArrayList<Arrival> arrivals = stop.getArrivals();
        Iterator<Arrival> iterator = arrivals.iterator();

        while (iterator.hasNext()) {
          Arrival arrival = iterator.next();
          if (!arrival.tripNumber.equals(tripNumber)) {
            continue;
          }

          /**
           * If we're here, we have the correct trip.  If the ETA is still
           * greater than requested, queue another alarm and exit.
           */
          int requestedEtaDifference = arrival.eta - minutes;
          if (requestedEtaDifference > 0) {
            Log.d(TAG, "ETA " + arrival.eta + " > " + minutes);

            PendingIntent sender = PendingIntent.getBroadcast(context, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

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

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
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
        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
      int icon = R.drawable.stat_bus_alarm;
      long when = System.currentTimeMillis();

      Resources res = context.getResources();
      // The text that appears in the status bar
      String tickerText = res.getString(R.string.route_number_to_destination,
          routeNumber, destination);
      // Text for the notification details
      String dueMinutes = String.format(res.getQuantityString(
            R.plurals.due_in_n_minutes, minutes), minutes);
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(System.currentTimeMillis());
      calendar.add(Calendar.MINUTE, minutes);
      String dueTime = new SimpleDateFormat("HH:mm").format(calendar.getTime());
      String dueText = dueMinutes + " (" + dueTime + ")";

      sendNotification(context, platformTag, notificationManager, icon, when,
            tickerText, dueText);
    }
  }

private void sendNotification(Context context, String platformTag,
            NotificationManager notificationManager, int icon, long when,
            String tickerText, String dueText) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
            .setSmallIcon(icon)
            .setContentTitle(tickerText + " " + dueText);
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
        mNotificationManager.notify(1 , mBuilder.build());
    }
}
