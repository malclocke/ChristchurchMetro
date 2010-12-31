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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONException;
import org.json.JSONArray;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.params.HttpParams;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.NameValuePair;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/*
 * Represents a bus stop
 */

class Stop {

  public static final String TAG = "Stop";

  public static String arrivalURL = "http://rtt.metroinfo.org.nz/RTT/Public/Utility/File.aspx?Name=JPRoutePositionET.xml&ContentType=SQLXML&PlatformTag=";

  private ArrayList <Arrival> arrivals = new ArrayList<Arrival>();

  public String name;
  public String platformTag;
  public String platformNumber;
  public String roadName;
  public String routes;
  public double latitude;
  public double longitude;

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
   * Returns an ArrayList of all the stops within the square bounded by the
   * two GeoPoints.
   */
  public static ArrayList<Stop> getAllWithinBounds(Context context,
      GeoPoint topLeft, GeoPoint bottomRight) {

    String topLat = Double.toString(topLeft.getLatitudeE6() / 1E6);
    String topLon = Double.toString(topLeft.getLongitudeE6() / 1E6);
    String bottomLat = Double.toString(bottomRight.getLatitudeE6() / 1E6);
    String bottomLon = Double.toString(bottomRight.getLongitudeE6() / 1E6);

    String query = "SELECT p.platform_tag, p.platform_number, p.name, " +
      "p.road_name, p.latitude, p.longitude FROM platforms p" +
      " WHERE p.latitude < " + topLat + " AND p.longitude > " + topLon +
      " AND p.latitude > " + bottomLat + " AND p.longitude < " + bottomLon;

    return doArrayListQuery(context, query);
  }

  /**
   * Returns an ArrayList of all the stops on a given route within the square
   * bounded by the two GeoPoints.
   */
  public static ArrayList<Stop> getAllWithinBounds(Context context,
      GeoPoint topLeft, GeoPoint bottomRight, String routeTag) {

    String topLat = Double.toString(topLeft.getLatitudeE6() / 1E6);
    String topLon = Double.toString(topLeft.getLongitudeE6() / 1E6);
    String bottomLat = Double.toString(bottomRight.getLatitudeE6() / 1E6);
    String bottomLon = Double.toString(bottomRight.getLongitudeE6() / 1E6);

    String query = "SELECT p.platform_tag, p.platform_number, p.name, " +
      "p.road_name, p.latitude, p.longitude FROM platforms p" +
      " WHERE p.platform_tag IN" +
      "   (SELECT platform_tag FROM patterns_platforms" +
      "   WHERE route_tag = '" + routeTag + "')" +
      " AND p.latitude < " + topLat + " AND p.longitude > " + topLon +
      " AND p.latitude > " + bottomLat + " AND p.longitude < " + bottomLon;

    return doArrayListQuery(context, query);
  }

  /* Perform a search query for any stops which match query string */
  public static ArrayList<Stop> searchStops(Context context, String queryString) {
    return doArrayListQuery(context, "SELECT platform_tag, platform_number, name, road_name, latitude, longitude FROM platforms " +
                   "WHERE platform_number LIKE '" + queryString + "%' " +
                   "OR name LIKE '%" + queryString +"%' " +
                   "OR road_name LIKE '%" + queryString + "%'");
  }

  public ArrayList getArrivals() throws Exception {
    arrivals.clear();

    try {
      SAXParserFactory spf = SAXParserFactory.newInstance();
      SAXParser sp = spf.newSAXParser();
      XMLReader xr = sp.getXMLReader();
      URL source = new URL(arrivalURL + platformTag);
      EtaHandler handler = new EtaHandler();
      xr.setContentHandler(handler);
      xr.parse(new InputSource(source.openStream()));
    } catch (Exception e) {
      throw e;
    }

    Collections.sort(arrivals, new ComparatorByEta());
    return arrivals;
  }

  /* Returns a Map OverlayItem for this stop */
  public OverlayItem getOverlayItem() {
    GeoPoint point = getGeoPoint();
    return new OverlayItem(point, this.platformNumber, this.name);
  }

  /* Returns the GeoPoint for this stop */
  public GeoPoint getGeoPoint() {
    return new GeoPoint((int) (this.latitude * 1E6), (int) (longitude * 1E6));
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

    public String platformNumber;

    public InvalidPlatformNumberException(String platformNumber) {
      super("Cannot find platform number " + platformNumber);
      this.platformNumber = platformNumber;
    }
  }

  private class ComparatorByEta implements Comparator {
    public int compare(Object one, Object two) {
      return ((Arrival)one).eta - ((Arrival)two).eta;
    }
  }

  private class EtaHandler extends DefaultHandler {

    private Arrival arrival = null;
    private String routeNumber = null;
    private String routeName = null;
    private String destination = null;

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
        } catch (NumberFormatException e) {
          Log.e(TAG, "NumberFormatException: " + e.getMessage());
        }
        arrivals.add(arrival);
        arrival = null;
      }
    }

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
