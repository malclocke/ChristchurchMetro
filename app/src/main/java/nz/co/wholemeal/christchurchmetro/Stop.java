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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

/*
 * Represents a bus stop
 */

class Stop {

  public static final String TAG = "Stop";

  public static String arrivalURL = "http://rtt.metroinfo.org.nz/RTT/Public/Utility/File.aspx?Name=JPRoutePositionET.xml&ContentType=SQLXML&PlatformTag=";

  private final ArrayList <Arrival> arrivals = new ArrayList<Arrival>();

  public String name;
  public String platformTag;
  public String platformNumber;
  public String roadName;
  public String routes;
  public double latitude;
  public double longitude;

  // The time of the last call to getArrivals()
  public long lastArrivalFetch = 0;

  public Stop() {
  }

  public Stop(String platformTag, String platformNumber, Context context)
    throws InvalidPlatformNumberException {

    String queryBase = "SELECT platform_tag, platform_number, name, road_name, latitude, longitude FROM platforms ";
    String whereClause;
    String whereParameter;

    if (platformTag == null) {
      whereClause = "WHERE platform_number = ?";
      whereParameter = platformNumber;
    } else {
      whereClause = "WHERE platform_tag = ?";
      whereParameter = platformTag;
    }

    DatabaseHelper databaseHelper = new DatabaseHelper(context);
    SQLiteDatabase database = databaseHelper.getWritableDatabase();

    Cursor cursor = database.rawQuery(queryBase + whereClause,
      new String [] {whereParameter});

    if (cursor.moveToFirst()) {
      this.platformTag = cursor.getString(0);
      this.platformNumber = cursor.getString(1);
      this.name = cursor.getString(2);
      this.roadName = cursor.getString(3);
      this.latitude = cursor.getDouble(4);
      this.longitude = cursor.getDouble(5);
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

    String query = "SELECT p.platform_tag, p.platform_number, p.name, " +
            "p.road_name, p.latitude, p.longitude FROM platforms p";

    return doArrayListQuery(context, query);
  }

  /**
   * Returns an ArrayList of all the stops within the LatLngBounds.  If routeTag
   * is not null then only stops on the provided route are returned.
   */
  public static ArrayList<Stop> getAllWithinBounds(Context context,
      LatLngBounds latLngBounds, String routeTag) {

    String query;
    LatLng northEast = latLngBounds.northeast;
    LatLng southWest = latLngBounds.southwest;
    String topLat = Double.toString(northEast.latitude);
    String topLon = Double.toString(southWest.longitude);
    String bottomLat = Double.toString(southWest.latitude);
    String bottomLon = Double.toString(northEast.longitude);

    if (routeTag == null) {
      query = "SELECT p.platform_tag, p.platform_number, p.name, " +
              "p.road_name, p.latitude, p.longitude FROM platforms p" +
              " WHERE p.latitude < " + topLat + " AND p.longitude > " + topLon +
              " AND p.latitude > " + bottomLat + " AND p.longitude < " + bottomLon;
    } else {
      query = "SELECT p.platform_tag, p.platform_number, p.name, " +
              "p.road_name, p.latitude, p.longitude FROM platforms p" +
              " WHERE p.platform_tag IN" +
              "   (SELECT platform_tag FROM patterns_platforms" +
              "   WHERE route_tag = '" + routeTag + "')" +
              " AND p.latitude < " + topLat + " AND p.longitude > " + topLon +
              " AND p.latitude > " + bottomLat + " AND p.longitude < " + bottomLon;
    }
    return doArrayListQuery(context, query);
  }

  /* Perform a search query for any stops which match query string */
  public static ArrayList<Stop> searchStops(Context context, String queryString) {
    return doArrayListQuery(context, "SELECT platform_tag, platform_number, name, road_name, latitude, longitude FROM platforms " +
                   "WHERE platform_number LIKE '" + queryString + "%' " +
                   "OR name LIKE '%" + queryString +"%' " +
                   "OR road_name LIKE '%" + queryString + "%'");
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
    ArrayList<Stop> stops = new ArrayList<Stop>();

    Log.d(TAG, "query: " + query);

    DatabaseHelper databaseHelper = new DatabaseHelper(context);
    SQLiteDatabase database = databaseHelper.getWritableDatabase();
    Cursor cursor = database.rawQuery(query, null);

    try {
      if (cursor.moveToFirst()) {
        do {
          Stop stop = new Stop();
          stop.platformTag = cursor.getString(0);
          stop.platformNumber = cursor.getString(1);
          stop.name = cursor.getString(2);
          stop.roadName = cursor.getString(3);
          stop.latitude = cursor.getDouble(4);
          stop.longitude = cursor.getDouble(5);
          stops.add(stop);
        } while (cursor.moveToNext());
      }
    } finally {
      cursor.close();
    }
    Log.d(TAG, "stops.size() = " + stops.size());
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
