/**
 * Copyright 2010 Malcolm Locke
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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;

public class Route {
    public static final String TAG = "Arrival";

    public static final String QUERY_PREFIX
            = "SELECT patterns.route_number, patterns.route_name, patterns.destination, " +
                "patterns.route_tag, patterns.pattern_name, patterns.direction, " +
                "patterns.length, patterns.active, patterns.color, patterns.coordinates ";

    public String routeNumber;
    public String routeName;
    public String destination;
    public String routeTag;
    public String patternName;
    public String direction;
    public int length;
    public boolean active;
    public String color;
    public String coordinates;
    public ArrayList<LatLng> coordinatesList;

    public static Route get(Context context, String routeTag) {
        ArrayList<Route> routes = doArrayListQuery(context,
                QUERY_PREFIX + "FROM patterns WHERE route_tag = " + routeTag
        );
        return routes.get(0);
    }

    public static ArrayList<Route> getAll(Context context) {
        return doArrayListQuery(context, QUERY_PREFIX +
                "FROM patterns ORDER BY CAST(route_number AS INTEGER)");
    }

    public static ArrayList<Route> getRoutesForPlatform(Context context, String platformTag) {
        return doArrayListQuery(context, QUERY_PREFIX +
                "FROM patterns_platforms JOIN patterns " +
                "ON patterns.route_tag = patterns_platforms.route_tag " +
                "WHERE patterns_platforms.platform_tag = " + platformTag +
                " ORDER BY CAST(patterns.route_number AS INTEGER)");
    }

    /* Perform a search query for any routes which match query string */
    public static ArrayList<Route> searchRoutes(Context context, String queryString) {
        String queryWildcard = "%" + queryString + "%";
        return doArrayListQuery(context, QUERY_PREFIX + " FROM patterns" +
                " WHERE route_number LIKE ? OR route_name LIKE ? OR destination LIKE ?"  +
                " ORDER BY CAST(route_number AS INTEGER)",
                new String[] {queryString + "%", queryWildcard, queryWildcard}
        );
    }

    private static ArrayList<Route> doArrayListQuery(Context context, String query) {
        return doArrayListQuery(context, query, null);
    }

    private static ArrayList<Route> doArrayListQuery(Context context, String query, String[] queryArgs) {
        ArrayList<Route> routes = new ArrayList<Route>();

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Running query: " + query);
        }

        DatabaseHelper databaseHelper = new DatabaseHelper(context);
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        Cursor cursor = database.rawQuery(query, queryArgs);

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
                    route.color = cursor.getString(8);
                    route.coordinates = cursor.getString(9);
                    routes.add(route);
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "routes.size() = " + routes.size());
        }
        database.close();

        return routes;
    }

    private static ArrayList<LatLng> getCoordinatesFromString(String coordinateString) {
        ArrayList<LatLng> coordinates = new ArrayList<LatLng>();
        if (coordinateString != null) {
            for (String coordinatePair : coordinateString.split(" ")) {
                String[] pair = coordinatePair.split(",");
                coordinates.add(new LatLng(Double.parseDouble(pair[1]), Double.parseDouble(pair[0])));
            }
        }
        return coordinates;
    }

    /**
     * Returns an the route boundaries.
     */
    public static LatLngBounds getLatLngBounds(Context context, String routeTag) {
        LatLngBounds latLngBounds;
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        DatabaseHelper databaseHelper = new DatabaseHelper(context);
        SQLiteDatabase database = databaseHelper.getWritableDatabase();

        String query =
                "SELECT MIN(longitude), MAX(latitude), MAX(longitude), MIN(latitude)" +
                        " FROM platforms WHERE platform_tag IN" +
                        " (SELECT platform_tag FROM patterns_platforms WHERE route_tag = ?)";

        Log.d(TAG, "Running query: " + query);
        Cursor cursor = database.rawQuery(query, new String[]{routeTag});

        try {
            if (cursor.moveToFirst()) {
                builder.include(new LatLng(cursor.getDouble(1), cursor.getDouble(2)));
                builder.include(new LatLng(cursor.getDouble(3), cursor.getDouble(0)));
            }
        } finally {
            cursor.close();
        }
        database.close();
        latLngBounds = builder.build();
        return latLngBounds;
    }

    public String fullRouteName() {
        return routeNumber + " " + destination + " (" + direction + ")";
    }

    public Iterable<LatLng> getCoordinateList() {
        if (coordinatesList == null) {
            coordinatesList = getCoordinatesFromString(coordinates);
        }
        return coordinatesList;
    }
}
