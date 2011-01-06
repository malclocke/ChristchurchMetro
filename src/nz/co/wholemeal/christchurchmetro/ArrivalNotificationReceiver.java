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

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
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

      Notification notification = new Notification(icon,
          tickerText + " " + dueMinutes, when);
      notification.defaults |= Notification.DEFAULT_SOUND;
      notification.defaults |= Notification.DEFAULT_VIBRATE;
      notification.flags |= Notification.FLAG_AUTO_CANCEL;
      Intent notificationIntent = new Intent(context, PlatformActivity.class);
      notificationIntent.putExtra("platformTag", platformTag);
      PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
        notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
      notification.setLatestEventInfo(context, tickerText, dueMinutes,
          contentIntent);

      notificationManager.notify(1, notification);
    }
  }
}
