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

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/*
 * Represents a bus stop
 */

class Stop {

    public static final String TAG = "Stop";

    public static String arrivalURL = "http://rtt.metroinfo.org.nz/RTT/Public/Utility/File.aspx?Name=JPRoutePositionET.xml&ContentType=SQLXML&PlatformTag=";

    private final ArrayList<Arrival> arrivals = new ArrayList<Arrival>();

    public String name;
    public String platformTag;
    public String platformNumber;
    public String roadName;
    public String routes;
    public double latitude;
    public double longitude;
    public String[] routeTags;

    public static String QUERY_BASE = "SELECT p.platform_tag, p.platform_number, p.name, " +
            "p.road_name, p.latitude, p.longitude, " +
            "group_concat(pp.route_tag) FROM platforms p " +
            "JOIN patterns_platforms pp ON pp.platform_tag = p.platform_tag ";
    public static String QUERY_SUFFIX = " GROUP BY p.platform_tag ";

    // The time of the last call to getArrivals()
    public long lastArrivalFetch = 0;

    public Stop() {
    }

    public Stop(Cursor cursor) {
        populateFromCursor(this, cursor);
    }

    private void populateFromCursor(Stop stop, Cursor cursor) {
        this.platformTag = cursor.getString(0);
        this.platformNumber = cursor.getString(1);
        this.name = cursor.getString(2);
        this.roadName = cursor.getString(3);
        this.latitude = cursor.getDouble(4);
        this.longitude = cursor.getDouble(5);
        this.routeTags = cursor.getString(6).split(",");
    }

    public Stop(String platformTag, String platformNumber, Context context)
            throws InvalidPlatformNumberException {

        String whereClause;
        String whereParameter;

        if (platformTag == null) {
            whereClause = "WHERE p.platform_number = ?";
            whereParameter = platformNumber;
        } else {
            whereClause = "WHERE p.platform_tag = ?";
            whereParameter = platformTag;
        }

        DatabaseHelper databaseHelper = new DatabaseHelper(context);
        SQLiteDatabase database = databaseHelper.getWritableDatabase();

        Cursor cursor = database.rawQuery(QUERY_BASE + whereClause + QUERY_SUFFIX,
                new String[]{whereParameter});

        if (cursor.moveToFirst()) {
            populateFromCursor(this, cursor);
            cursor.close();
            database.close();
        } else {
            cursor.close();
            database.close();
            throw new InvalidPlatformNumberException("Invalid platform");
        }
    }

    /**
     * Returns an ArrayList of all the stops.
     */
    public static ArrayList<Stop> getAll(Context context) {
        return doArrayListQuery(context, QUERY_BASE + QUERY_SUFFIX);
    }

    /**
     * Returns an ArrayList of all the stops within the LatLngBounds.  If routeTag
     * is not null then only stops on the provided route are returned.
     */
    public static ArrayList<Stop> getAllWithinBounds(Context context, LatLngBounds latLngBounds) {

        LatLng northEast = latLngBounds.northeast;
        LatLng southWest = latLngBounds.southwest;
        String topLat = Double.toString(northEast.latitude);
        String topLon = Double.toString(southWest.longitude);
        String bottomLat = Double.toString(southWest.latitude);
        String bottomLon = Double.toString(northEast.longitude);

        String query = QUERY_BASE +
                " WHERE p.latitude < " + topLat + " AND p.longitude > " + topLon +
                " AND p.latitude > " + bottomLat + " AND p.longitude < " + bottomLon + QUERY_SUFFIX;
        return doArrayListQuery(context, query);
    }

    /* Perform a search query for any stops which match query string */
    public static ArrayList<Stop> searchStops(Context context, String queryString) {
        String wildcard = "%" + queryString + "%";
        return doArrayListQuery(context, QUERY_BASE +
                "WHERE p.platform_number LIKE ? OR p.name LIKE ? OR p.road_name LIKE ? "
                        + QUERY_SUFFIX,
                new String[] {queryString + "%", wildcard, wildcard});
    }

    public ArrayList<Arrival> getArrivals() throws Exception {
        arrivals.clear();

        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            URL source = new URL(arrivalURL + platformTag);
            EtaHandler handler = new EtaHandler();
            xr.setContentHandler(handler);
            xr.parse(new InputSource(source.openStream()));
            lastArrivalFetch = System.currentTimeMillis();
        } catch (Exception e) {
            throw e;
        }

        Collections.sort(arrivals, new ComparatorByEta());
        return arrivals;
    }


    /* Returns the LatLng for this stop */
    public LatLng getLatLng() {
        return new LatLng(latitude, longitude);
    }

    private static ArrayList<Stop> doArrayListQuery(Context context, String query) {
        return doArrayListQuery(context, query, null);
    }

    private static ArrayList<Stop> doArrayListQuery(Context context, String query, String[] queryArgs) {
        ArrayList<Stop> stops = new ArrayList<Stop>();

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "query: " + query);
        }

        DatabaseHelper databaseHelper = new DatabaseHelper(context);
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        Cursor cursor = database.rawQuery(query, queryArgs);

        try {
            if (cursor.moveToFirst()) {
                do {
                    Stop stop = new Stop(cursor);
                    stops.add(stop);
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "stops.size() = " + stops.size());
        }
        database.close();
        return stops;
    }

    public class InvalidPlatformNumberException extends Exception {

        private static final long serialVersionUID = 1L;
        public String platformNumber;

        public InvalidPlatformNumberException(String platformNumber) {
            super("Cannot find platform number " + platformNumber);
            this.platformNumber = platformNumber;
        }
    }

    private class ComparatorByEta implements Comparator<Arrival> {
        @Override
        public int compare(Arrival one, Arrival two) {
            return one.eta - two.eta;
        }
    }

    private class EtaHandler extends DefaultHandler {

        private Arrival arrival = null;
        private String routeNumber = null;
        private String routeName = null;
        private String destination = null;

        @Override
        public void startElement(String uri, String localName, String qName,
                                 Attributes attributes) throws SAXException {
            if (localName.equals("Route")) {
                routeNumber = attributes.getValue("RouteNo");
                routeName = attributes.getValue("Name");
            } else if (localName.equals("Destination")) {
                destination = attributes.getValue("Name");
            } else if (localName.equals("Trip")) {
                arrival = new Arrival();
                arrival.routeNumber = routeNumber;
                arrival.routeName = routeName;
                arrival.destination = destination;
                try {
                    arrival.eta = Integer.parseInt(attributes.getValue("ETA"));
                    arrival.tripNumber = attributes.getValue("TripNo");
                    if (attributes.getValue("WheelchairAccess") != null) {
                        arrival.wheelchairAccess = attributes.getValue("WheelchairAccess").equals("true");
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "NumberFormatException: " + e.getMessage());
                }
                arrivals.add(arrival);
                arrival = null;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName)
                throws SAXException {
            if (localName.equals("Route")) {
                routeNumber = null;
                routeName = null;
            } else if (localName.equals("Destination")) {
                destination = null;
            }
        }
    }
}
