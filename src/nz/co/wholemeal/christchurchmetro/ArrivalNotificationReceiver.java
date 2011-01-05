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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
      int minutes = extras.getInt("minutes");

      NotificationManager notificationManager =
        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
      int icon = R.drawable.stat_bus_alarm;
      long when = System.currentTimeMillis();

      Notification notification = new Notification(icon,
        routeNumber + " to " + destination, when);
      notification.defaults |= Notification.DEFAULT_SOUND;
      notification.flags |= Notification.FLAG_AUTO_CANCEL;
      Intent notificationIntent = new Intent(context, PlatformActivity.class);
      notificationIntent.putExtra("platformTag", platformTag);
      PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
        notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
      notification.setLatestEventInfo(context,
        routeNumber + " to " + destination,
        "Due in " + minutes + " minutes", contentIntent);

      notificationManager.notify(1, notification);
    }
  }
}
