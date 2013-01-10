package nz.co.wholemeal.christchurchmetro;

import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class LoadPlatformsService extends IntentService {

    public static String PLATFORMS_URL = "http://rtt.metroinfo.org.nz/RTT/Public/Utility/File.aspx?ContentType=SQLXML&Name=JPPlatform.xml";
    public static String ROUTE_PATTERN_URL = "http://rtt.metroinfo.org.nz/RTT/Public/Utility/File.aspx?ContentType=SQLXML&Name=JPRoutePattern.xml";
    //public static String PLATFORMS_URL = "http://10.0.2.2/~malc/JPPlatform.xml";
    //public static String ROUTE_PATTERN_URL = "http://10.0.2.2/~malc/JPRoutePattern.xml";

    private static final String TAG = "LoadPlatformsService";
    private static final int NOTIFICATION_ID = 0;
    private NotificationManager mNotificationManager;

    public LoadPlatformsService() {
        super("LoadPlatformsService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Log.d(TAG, "Loading platforms");
        setUpNotification();

        PlatformHandler platformHandler = null;
        PatternHandler patternHandler = null;
        DatabaseHelper databaseHelper = new DatabaseHelper(this);
        SQLiteDatabase database = databaseHelper.getWritableDatabase();


        try {
          SAXParserFactory spf = SAXParserFactory.newInstance();
          SAXParser sp = spf.newSAXParser();
          XMLReader xr = sp.getXMLReader();
          URL source = new URL(PLATFORMS_URL);
          platformHandler = new PlatformHandler();
          platformHandler.database = database;
          xr.setContentHandler(platformHandler);
          database.beginTransaction();
          database.delete("platforms", null, null);
          xr.parse(new InputSource(source.openStream()));
          database.setTransactionSuccessful();
        } catch (SQLiteException e) {
          Log.e(TAG, "SQLiteException", e);
        } catch (Exception e) {
          Log.e(TAG, "Exception", e);
        } finally {
          database.endTransaction();
        }

        // Tell the progress bar that we're switching from platforms to patterns
        publishProgress(-1);

        try {
          SAXParserFactory spf = SAXParserFactory.newInstance();
          SAXParser sp = spf.newSAXParser();
          XMLReader xr = sp.getXMLReader();
          URL source = new URL(ROUTE_PATTERN_URL);
          patternHandler = new PatternHandler();
          patternHandler.database = database;
          xr.setContentHandler(patternHandler);
          database.beginTransaction();
          database.delete("patterns", null, null);
          database.delete("patterns_platforms", null, null);
          xr.parse(new InputSource(source.openStream()));
          database.setTransactionSuccessful();
        } catch (SQLiteException e) {
          Log.e(TAG, "SQLiteException", e);
        } catch (Exception e) {
          Log.e(TAG, "Exception", e);
        } finally {
          database.endTransaction();
          database.close();
        }

        if (platformHandler != null && patternHandler != null) {

          SharedPreferences preferences =
            getSharedPreferences(PlatformActivity.PREFERENCES_FILE, 0);
          SharedPreferences.Editor editor = preferences.edit();
          editor.putLong("lastDataLoad", System.currentTimeMillis());
          editor.commit();

          broadcastCompleteMessage(getString(R.string.loaded_route_information));

        } else {
          broadcastCompleteMessage(getString(R.string.error_loading_bus_routes));
        }
    }

    private void setUpNotification() {
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.stat_bus_alarm)
                    .setContentTitle(getString(R.string.loading_routes));
        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void publishProgress(int i) {
        // TODO Auto-generated method stub

    }

    private void broadcastCompleteMessage(String message) {
        Intent intent = new Intent();
        intent.putExtra(LoadPlatformsCompleteReceiver.MESSAGE, message);
        intent.setAction("nz.co.wholemeal.christchurchmetro.load_platforms_complete");
        sendBroadcast(intent);
        dismissNotification();
    }

    private void dismissNotification() {
        if (mNotificationManager != null) {
            mNotificationManager.cancel(NOTIFICATION_ID);
        }
    }

    private class PlatformHandler extends DefaultHandler {

        public Integer platformCount = 0;
        public SQLiteDatabase database = null;
        private ContentValues values = null;

        @Override
        public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
          if (localName.equals("Platform")) {
            if( values == null) {
              values = new ContentValues();
            }
            values.put("platform_tag", attributes.getValue("PlatformTag"));
            values.put("platform_number", attributes.getValue("PlatformNo"));
            values.put("name", attributes.getValue("Name"));
            values.put("road_name", attributes.getValue("RoadName"));
          } else if (localName.equals("Position")) {
            values.put("latitude", Double.valueOf(attributes.getValue("Lat")));
            values.put("longitude", Double.valueOf(attributes.getValue("Long")));
          }
        }

        @Override
        public void endElement(String uri, String localName, String qName)
          throws SAXException {
          if (localName.equals("Platform")) {
            if (database != null) {
              database.insert("platforms", null, values);
              platformCount++;

              values.putNull("platform_tag");
              values.putNull("platform_number");
              values.putNull("name");
              values.putNull("road_name");
              values.putNull("latitude");
              values.putNull("longitude");

              if ((platformCount % 10) == 0) {
                publishProgress(platformCount);
              }
            }
          }
        }
      }

      private class PatternHandler extends DefaultHandler {

        public Integer patternCount = 0;
        public SQLiteDatabase database = null;
        private ContentValues patternValues = null;
        private ContentValues patternPlatformsValues = null;

        @Override
        public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
          if (localName.equals("Route")) {
            if( patternValues == null) {
              patternValues = new ContentValues();
            }
            patternValues.put("route_number", attributes.getValue("RouteNo"));
            patternValues.put("route_name", attributes.getValue("Name"));
          } else if (localName.equals("Destination")) {
            patternValues.put("destination", attributes.getValue("Name"));
          } else if (localName.equals("Pattern")) {
            if(patternPlatformsValues == null) {
              patternPlatformsValues = new ContentValues();
            }
            patternValues.put("route_tag", attributes.getValue("RouteTag"));
            patternPlatformsValues.put("route_tag",
                attributes.getValue("RouteTag"));
            patternValues.put("pattern_name", attributes.getValue("Name"));
            patternValues.put("direction", attributes.getValue("Direction"));
            patternValues.put("length",
                Integer.parseInt(attributes.getValue("Length")));
            patternValues.put("active",
                attributes.getValue("Schedule").equals("Active") ? true : false);
          } else if (localName.equals("Platform")) {
            patternPlatformsValues.put("platform_tag",
                attributes.getValue("PlatformTag"));
            if (attributes.getValue("ScheduleAdheranceTimepoint") != null) {
              patternPlatformsValues.put("schedule_adherance_timepoint",
                  attributes.getValue("ScheduleAdheranceTimepoint").equals("true") ? true : false);
            } else {
              patternPlatformsValues.put("schedule_adherance_timepoint", false);
            }
          }
        }

        @Override
        public void endElement(String uri, String localName, String qName)
          throws SAXException {
          if (localName.equals("Platform")) {
            if (database != null) {
              database.insert("patterns_platforms", null, patternPlatformsValues);

              patternPlatformsValues.putNull("platform_tag");
              patternPlatformsValues.putNull("schedule_adherance_timepoint");
            }
          } else if (localName.equals("Pattern")) {
            if (database != null) {
              database.insert("patterns", null, patternValues);
              patternCount++;

              patternValues.putNull("route_tag");
              patternPlatformsValues.putNull("route_tag");
              patternValues.putNull("pattern_name");
              patternValues.putNull("direction");
              patternValues.putNull("length");
              patternValues.putNull("active");

              publishProgress(patternCount);
            }
          } else if (localName.equals("Route")) {
            if (database != null) {
              patternValues.putNull("route_number");
              patternValues.putNull("route_name");
            }
          }
        }
      }
}
