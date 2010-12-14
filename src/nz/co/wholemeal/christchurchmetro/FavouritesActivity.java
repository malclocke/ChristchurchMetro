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
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

/*
 * Some of the authors favourites:
 * "40188", "20763", "21450", "37375", "37334", "14864", "21957"
 */

public class FavouritesActivity extends ListActivity {

  public final static String TAG = "FavouritesActivity";

  public static ArrayList stops = new ArrayList<Stop>();
  private StopAdapter stopAdapter;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (stops.size() == 0) {
      initFavourites();
    }

    stopAdapter = new StopAdapter(this, R.layout.stop_list_item, stops);
    setListAdapter(stopAdapter);

    ListView lv = getListView();

    /* Enables the long click in the ListView to be handled in this Activity */
    registerForContextMenu(lv);

    lv.setOnItemClickListener(new OnItemClickListener() {
      public void onItemClick(AdapterView<?> parent, View view,
          int position, long id) {
        Intent intent = new Intent();
        Stop stop = (Stop)stops.get(position);

        if (stop == null) {
          Log.e(TAG, "Didn't get a stop");
          finish();
        }
        intent.putExtra("platformNumber", stop.platformNumber);

        setResult(RESULT_OK, intent);
        finish();
      }
    });
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
                                  ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    menu.setHeaderTitle("Options");
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.favourite_context_menu, menu);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    Stop stop = (Stop)stops.get((int)info.id);
    switch (item.getItemId()) {
      case R.id.remove_favourite:
        removeFavourite(stop);
        return true;
      default:
        return super.onContextItemSelected(item);
    }
  }

  private void initFavourites() {
    SharedPreferences favourites = getSharedPreferences(ChristchurchMetroActivity.PREFERENCES_FILE, 0);
    String stops_json = favourites.getString("favouriteStops", null);

    if (stops_json != null) {
      Log.d(TAG, "initFavourites(): stops_json = " + stops_json);
      try {
        JSONArray stops_array = (JSONArray) new JSONTokener(stops_json).nextValue();
        for (int i = 0;i < stops_array.length();i++) {
          String stopNumber = (String)stops_array.get(i);
          try {
            Stop stop = new Stop(stopNumber);
            stops.add(stop);
            Log.d(TAG, "initFavourites(): added stop " + stop.platformNumber);
          } catch (Stop.InvalidPlatformNumberException e) {
            Log.e(TAG, "Invalid stop number as favourite: " + stopNumber);
          }
        }
      } catch (JSONException e) {
        Log.e(TAG, "initFavourites(): JSONException: " + e.toString());
      }
    }
    Log.d(TAG, "initFavourites(): stops.size() = " + stops.size());
  }

  public void saveFavourites() {
    SharedPreferences favourites = getSharedPreferences(ChristchurchMetroActivity.PREFERENCES_FILE, 0);
    saveFavourites(favourites);
    stopAdapter.notifyDataSetChanged();
  }

  public static void saveFavourites(SharedPreferences favourites) {
    SharedPreferences.Editor editor = favourites.edit();
    JSONArray stopArray = new JSONArray();
    Iterator iterator = stops.iterator();
    while (iterator.hasNext()) {
      Stop stop = (Stop)iterator.next();
      stopArray.put(stop.platformNumber);
    }
    editor.putString("favouriteStops", stopArray.toString());
    editor.commit();
  }

  public static boolean isFavourite(Stop stop) {
    Iterator iterator = stops.iterator();

    /* Check the Stop is not already present in favourites */
    while (iterator.hasNext()) {
      Stop favourite = (Stop)iterator.next();
      if (favourite.platformNumber.equals(stop.platformNumber)) {
        return true;
      }
    }

    return false;
  }

  public void removeFavourite(Stop stop) {
    if (isFavourite(stop)) {
      Log.d(TAG, "Removed stop " + stop.platformNumber + " from favourites");
      stops.remove(stop);
      saveFavourites();
    } else {
      Log.e(TAG, "Remove requested for stop " + stop.platformNumber +
          " but it's not present in favourites");
    }
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
        v = vi.inflate(R.layout.stop_list_item, null);
      }
      Stop stop = items.get(position);
      if (stop != null) {
        TextView platformNumber = (TextView) v.findViewById(R.id.platform_number);
        TextView platformName = (TextView) v.findViewById(R.id.platform_name);
        TextView platformRoutes = (TextView) v.findViewById(R.id.platform_routes);
        TextView nextBus = (TextView) v.findViewById(R.id.next_bus);
        platformNumber.setText(stop.platformNumber);
        platformName.setText(stop.name);
        platformRoutes.setText("Routes: " + stop.routes);
        nextBus.setTag(stop);
        nextBus.setText("Next bus: Loading ...");
        new AsyncNextArrival().execute(nextBus);
      }
      return v;
    }
  }

  /* Load next arrival for each favourite in the background */
  public class AsyncNextArrival extends AsyncTask<TextView, Void, TextView> {

    private String arrivalText = null;

    protected TextView doInBackground(TextView... textViews) {
      TextView textView = textViews[0];
      Stop stop = (Stop)textView.getTag();
      Arrival arrival = null;
      Log.d(TAG, "Running AsyncNextArrival.doInBackground() for stop " + stop.platformNumber);
      ArrayList arrivals = stop.getArrivals();
      if (!arrivals.isEmpty()) {
        arrival = (Arrival)arrivals.get(0);
        arrivalText = "Next bus: " + arrival.eta + " mins: " +
          arrival.routeNumber + " - " + arrival.destination;
      } else {
        arrivalText = "No buses due in the next 30 minutes";
      }
      return textView;
    }

    protected void onPostExecute(TextView textView) {
      textView.setText(arrivalText);
    }
  }
}
