package nz.co.wholemeal.christchurchmetro;

import android.util.Log;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

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

  public static String stopURL = "http://wholemeal.co.nz/~malc/metro/platforms/";
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

  /* Instantiate a Stop from a stop number */
  public Stop(String platformTag, String platformNumber) throws InvalidPlatformNumberException {
    // Which directory to look for platform, tags or numbers
    String searchPath;

    if (platformTag == null) {
      this.platformNumber = platformNumber;
      searchPath = "numbers/" + platformNumber + ".xml";
    } else {
      this.platformTag = platformTag;
      searchPath = "tags/" + platformTag + ".xml";
    }

    try {
      SAXParserFactory spf = SAXParserFactory.newInstance();
      SAXParser sp = spf.newSAXParser();
      XMLReader xr = sp.getXMLReader();
      URL source = new URL(stopURL + searchPath);
      StopHandler handler = new StopHandler();
      xr.setContentHandler(handler);
      xr.parse(new InputSource(source.openStream()));
    } catch (Exception e) {
      Log.e(TAG, e.toString());
      throw new InvalidPlatformNumberException(e.getMessage());
    }
  }

  public ArrayList getArrivals() {
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
      Log.e(TAG, e.toString());
    }

    Collections.sort(arrivals, new ComparatorByEta());
    return arrivals;
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
      Log.d(TAG, "Got start element <" + localName + ">");
      if (localName.equals("Route")) {
        Log.d(TAG, "Arrivals for RouteNo " + attributes.getValue("RouteNo"));
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
      Log.d(TAG, "Got end element </" + localName + ">");
      if (localName.equals("Route")) {
        Log.d(TAG, "Arrival finished ");
        routeNumber = null;
        routeName = null;
      } else if (localName.equals("Destination")) {
        destination = null;
      }
    }
  }

  private class StopHandler extends DefaultHandler {

    /* At the moment this handler expects there to be a single <Platform>
     * element in the XML */
    public String TAG = "StopHandler";

    public void startElement(String uri, String localName, String qName,
        Attributes attributes) throws SAXException {
      Log.d(TAG, "Got start element <" + localName + ">");
      if (localName.equals("Platform")) {
        Log.d(TAG, "PlatformTag = " + attributes.getValue("PlatformTag"));
        platformNumber = attributes.getValue("PlatformNo");
        platformTag = attributes.getValue("PlatformTag");
        roadName = attributes.getValue("RoadName");
        name = attributes.getValue("Name");
      } else if (localName.equals("Position")) {
        latitude = Double.parseDouble(attributes.getValue("Lat"));
        longitude = Double.parseDouble(attributes.getValue("Long"));
      }
    }

    public void endElement(String uri, String localName, String qName)
      throws SAXException {
      Log.d(TAG, "Got end element </" + localName + ">");
    }
  }
}
