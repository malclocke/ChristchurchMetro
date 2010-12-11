package nz.co.wholemeal.christchurchmetro;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONException;
import org.json.JSONArray;

import android.app.Activity;
import android.app.ListActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

public class FavouritesActivity extends ListActivity {

  public final static String TAG = "FavouritesActivity";
  public final static String FAVOURITES_FILE = "FavouriteStopsFile";

  public static ArrayList stops = new ArrayList<Stop>();

  private static String STOPS_JSON = "[" +
    "{" +
      "\"attributes\" : {" +
        "\"OBJECTID\" : 773," +
        "\"Name\" : \"Bower Ave & Pinewood Ave\"," +
        "\"PlatformTa\" : 821," +
        "\"RoadName\" : \"Bower Ave\"," +
        "\"PlatformNo\" : 40188," +
        "\"Routes\" : \"70:814|70:816\"," +
        "\"Lat\" : -43.488801000000002," +
        "\"Long\" : 172.71179900000001," +
        "\"BearingToR\" : null," +
        "\"RouteNos\" : \"70\"," +
        "\"RouteTags\" : \"814|816\"" +
      "}," +
      "\"geometry\" : {" +
        "\"x\" : 19226189.518700004," +
        "\"y\" : -5386670.7789999992" +
      "}" +
    "}," +
    "{" +
    "  \"attributes\" : {" +
    "    \"OBJECTID\" : 766," +
    "    \"Name\" : \"Bower Ave & Castletown Pl\"," +
    "    \"PlatformTa\" : 814," +
    "    \"RoadName\" : \"Bower Ave\"," +
    "    \"PlatformNo\" : 20763," +
    "    \"Routes\" : \"49:804|70:814|70:816\"," +
    "    \"Lat\" : -43.497529," +
    "    \"Long\" : 172.71062900000001," +
    "    \"BearingToR\" : null," +
    "    \"RouteNos\" : \"49|70\"," +
    "    \"RouteTags\" : \"804|814|816\"" +
    "  }," +
    "  \"geometry\" : {" +
    "    \"x\" : 19226059.2971," +
    "    \"y\" : -5388010.0443000011" +
    "  }" +
    "}" +
  "]";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (stops.size() == 0) {
      initFavourites();
    }

    setListAdapter(new StopAdapter(this, R.layout.list_item, stops));

    ListView lv = getListView();
    lv.setTextFilterEnabled(true);

    lv.setOnItemClickListener(new OnItemClickListener() {
      public void onItemClick(AdapterView<?> parent, View view,
          int position, long id) {
        Intent intent = new Intent();
        Stop stop = (Stop)stops.get(position);

        if (stop == null) {
          Log.e(TAG, "Didn't get a stop");
          finish();
        }
        intent.putExtra("platformNumber", stop.getPlatformNumber());
        /*
        Toast.makeText(getApplicationContext(), ((Stop)stops.get(position)).getPlatformNumber(),
            Toast.LENGTH_SHORT).show();
         */
        setResult(RESULT_OK, intent);
        finish();
      }
    });
  }

  private void initFavourites() {
    SharedPreferences favourites = getSharedPreferences(FAVOURITES_FILE, 0);
    String stops_json = favourites.getString("favouriteStops", null);

    if (stops_json != null) {
      try {
        JSONArray stops_array = (JSONArray) new JSONTokener(stops_json).nextValue();
        for (int i = 0;i < stops_array.length();i++) {
          JSONObject stop_json = (JSONObject)stops_array.get(i);
          Stop stop = new Stop(stop_json);
          stops.add(stop);
          Log.d(TAG, "initFavourites(): added stop " + stop.getPlatformNumber());
        }
      } catch (JSONException e) {
        Log.e(TAG, "initFavourites(): " + e.toString());
      }
    }
    /* "40188", "20763", "21450", "37375", "37334", "14864", "21957" */
    /*
    Stop stop;

    stop = new Stop();
    stop.setName("Bower Ave & Pinewood Ave");
    stop.setPlatformTag("821");
    stop.setRoadName("Bower Ave");
    stop.setPlatformNumber("40188");
    stop.setRoutes("70:814|70:816");
    stop.setLatitude(-43.488801000000002);
    stop.setLongitude(172.71179900000001);
    stops.add(stop);

    stop = new Stop();
    stop.setName("Bowhill Rd");
    stop.setPlatformTag("831");
    stop.setRoadName("Bowhill Rd");
    stop.setPlatformNumber("21957");
    stop.setRoutes("49:804");
    stop.setLatitude(-43.497397999999997);
    stop.setLongitude(172.71731299999999);
    stops.add(stop);

    stop = new Stop();
    stop.setName("Estuary Rd & Halsey St");
    stop.setPlatformTag("981");
    stop.setRoadName("Estuary Rd");
    stop.setPlatformNumber("26221");
    stop.setRoutes("5:684|5X:807");
    stop.setLatitude(-43.534126000000001);
    stop.setLongitude(172.737334);
    stops.add(stop);

    stop = new Stop();
    stop.setName("City Exchange (E)");
    stop.setPlatformTag("5");
    stop.setRoadName("Colombo St");
    stop.setPlatformNumber("37375");
    stop.setRoutes("10:1092|10:1093|18:1055|21:1063|21:1065|28:1078|28:1079|28:1080|9:1036|90:1001|92:996");
    stop.setLatitude(-43.533256999999999);
    stop.setLongitude(172.63655);
    stops.add(stop);

    stop = new Stop();
    stop.setName("Bower Ave & Castletown Pl");
    stop.setPlatformTag("814");
    stop.setRoadName("Bower Ave");
    stop.setPlatformNumber("20763");
    stop.setRoutes("49:804|70:814|70:816");
    stop.setLatitude(-43.497529);
    stop.setLongitude(172.71062900000001);
    stops.add(stop);

    stop = new Stop();
    stop.setName("Baker St & Bowhill Rd");
    stop.setPlatformTag("771");
    stop.setRoadName("Baker St");
    stop.setPlatformNumber("21450");
    stop.setRoutes("84:874");
    stop.setLatitude(-43.498013999999998);
    stop.setLongitude(172.71577600000001);
    stops.add(stop);

    stop = new Stop();
    stop.setName("City Exchange (B)");
    stop.setPlatformTag("2");
    stop.setRoadName("Lichfield St");
    stop.setPlatformNumber("37323");
    stop.setRoutes("20:1057|20:1058|20:1059|20:1060|22:1062|23:1068|23:1069|23:1070|3:1026|3:1027|3:1028|40:791|49:803|5:613|5:684|5:842|5X:904|81:821|81:822|81:895|83:826|83:875|84:828|84:874|B:917|B:918");
    stop.setLatitude(-43.534118999999997);
    stop.setLongitude(172.63733999999999);
    stops.add(stop);

    stop = new Stop();
    stop.setName("City Exchange (C)");
    stop.setPlatformTag("3");
    stop.setRoadName("Lichfield St");
    stop.setPlatformNumber("37334");
    stop.setRoutes("22:1061|3:1031|3:1032|3:1033|35:1071|35:1072|46:869|51:992|51:993|7:1035|70:815|70:817|83:876|84:880");
    stop.setLatitude(-43.534118999999997);
    stop.setLongitude(172.63719);
    stops.add(stop);
    */
  }

  public static void saveFavourites() {
    JSONArray jsonArray = new JSONArray();
    Iterator iterator = stops.iterator();

    while (iterator.hasNext()) {
      Stop stop = (Stop)iterator.next();
      jsonArray.put(stop.toJSONObject());
    }

    if (jsonArray.length() > 0) {
      Log.d(TAG, jsonArray.toString());
    }
  }

  public static boolean isFavourite(Stop stop) {
    Iterator iterator = stops.iterator();

    /* Check the Stop is not already present in favourites */
    while (iterator.hasNext()) {
      Stop favourite = (Stop)iterator.next();
      if (favourite.getPlatformNumber().equals(stop.getPlatformNumber())) {
        return true;
      }
    }

    return false;
  }

  private class StopAdapter extends ArrayAdapter<Stop> {

    private ArrayList<Stop> items;

    public StopAdapter(Context context, int textViewResourceId, ArrayList<Stop> items) {
      super(context, textViewResourceId, items);
      this.items = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View v = convertView;
      if (v == null) {
        LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = vi.inflate(R.layout.list_item, null);
      }
      Stop stop = items.get(position);
      if (stop != null) {
        TextView list_item = (TextView) v;
        if (list_item != null) {
          list_item.setText(stop.getName());
        }
      }
      return v;
    }
  }
}
