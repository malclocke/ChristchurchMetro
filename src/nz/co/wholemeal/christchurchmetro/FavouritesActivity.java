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

/*
 * Some of the authors favourites:
 * "40188", "20763", "21450", "37375", "37334", "14864", "21957"
 */

public class FavouritesActivity extends ListActivity {

  public final static String TAG = "FavouritesActivity";
  public final static String FAVOURITES_FILE = "FavouriteStopsFile";

  public static ArrayList stops = new ArrayList<Stop>();

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
