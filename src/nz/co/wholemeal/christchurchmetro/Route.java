/**
 * Copyright 2010 Malcolm Locke
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

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class Route {
  public static final String TAG = "Arrival";

  public String routeNumber;
  public String routeName;
  public String destination;
  public String routeTag;
  public String patternName;
  public String direction;
  public int length;
  public boolean active;

  public static ArrayList<Route> getAll(Context context) {
    return doArrayListQuery(context, "SELECT route_number, route_name, " +
      "destination, route_tag, pattern_name, direction, length, active " +
      "FROM patterns ORDER BY route_number");
  }

  public static ArrayList<Route> getRoutesForPlatform(Context context, String platformTag) {
    return doArrayListQuery(context, "SELECT patterns.route_number, " +
      "patterns.route_name, patterns.destination, patterns.route_tag, " +
      "patterns.pattern_name, patterns.direction, patterns.length, patterns.active " +
      "FROM patterns_platforms JOIN patterns " +
      "ON patterns.route_tag = patterns_platforms.route_tag " +
      "WHERE patterns_platforms.platform_tag = " + platformTag +
      " ORDER BY patterns.route_number");
  }

  /* Perform a search query for any routes which match query string */
  public static ArrayList<Route> searchRoutes(Context context, String queryString) {
    return doArrayListQuery(context, "SELECT route_number, route_name, " +
      "destination, route_tag, pattern_name, direction, length, active " +
      " FROM patterns" +
      " WHERE route_number LIKE '" + queryString + "%'" +
      " OR route_name LIKE '%" + queryString + "%'" +
      " OR destination LIKE '%" + queryString + "%'" +
      " ORDER BY route_number");
  }

  private static ArrayList<Route> doArrayListQuery(Context context, String query) {
    ArrayList<Route> routes = new ArrayList<Route>();

    Log.d(TAG, "Running query: " + query);

    DatabaseHelper databaseHelper = new DatabaseHelper(context);
    SQLiteDatabase database = databaseHelper.getWritableDatabase();
    Cursor cursor = database.rawQuery(query, null);

    try {
      if (cursor.moveToFirst()) {
        do {
          Route route = new Route();
          route.routeNumber = cursor.getString(0);
          route.routeName = cursor.getString(1);
          route.destination = cursor.getString(2);
          route.routeTag = cursor.getString(3);
          route.patternName = cursor.getString(4);
          route.direction = cursor.getString(5);
          route.length = cursor.getInt(6);
          route.active = (cursor.getInt(7) == 0 ? false : true);
          routes.add(route);
        } while (cursor.moveToNext());
      }
    } finally {
      cursor.close();
    }
    Log.d(TAG, "routes.size() = " + routes.size());
    database.close();

    return routes;
  }

  /**
   * Returns an array of integers representing the route boundaries.
   * Array values are in the order west, north, east, south.
   */
  public static int[] getRouteBoundaries(Context context, String routeTag) {
    int[] boundaries = new int[4];

    DatabaseHelper databaseHelper = new DatabaseHelper(context);
    SQLiteDatabase database = databaseHelper.getWritableDatabase();

    String query =
      "SELECT MIN(longitude), MAX(latitude), MAX(longitude), MIN(latitude)" +
      " FROM platforms WHERE platform_tag IN" +
      " (SELECT platform_tag FROM patterns_platforms WHERE route_tag = ?)";

    Log.d(TAG, "Running query: " + query);
    Cursor cursor = database.rawQuery(query, new String[] { routeTag });

    try {
      if (cursor.moveToFirst()) {
        boundaries[0] = (int)(cursor.getDouble(0) * 1E6);
        boundaries[1] = (int)(cursor.getDouble(1) * 1E6);
        boundaries[2] = (int)(cursor.getDouble(2) * 1E6);
        boundaries[3] = (int)(cursor.getDouble(3) * 1E6);
      }
    } finally {
      cursor.close();
    }
    database.close();
    return boundaries;
  }
}
