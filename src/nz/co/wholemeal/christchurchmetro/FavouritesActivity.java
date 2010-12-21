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
        intent.putExtra("platformTag", stop.platformTag);
        intent.setClassName("nz.co.wholemeal.christchurchmetro", "nz.co.wholemeal.christchurchmetro.PlatformActivity");

        startActivity(intent);
      }
    });
  }

  @Override
  public void onResume() {
    super.onResume();
    /*
     * Other activities can modify the favourites list, so reload every time
     * we come back to the foreground
     */
    stopAdapter.notifyDataSetChanged();
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

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.main_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Intent intent;
    switch (item.getItemId()) {
      case R.id.favourite_stops:
        Log.d(TAG, "Favourite stops selected");
        intent = new Intent();
        intent.setClassName("nz.co.wholemeal.christchurchmetro", "nz.co.wholemeal.christchurchmetro.FavouritesActivity");
        startActivity(intent);
        return true;
      case R.id.map:
        Log.d(TAG, "Map selected from menu");
        intent = new Intent();
        intent.setClassName("nz.co.wholemeal.christchurchmetro", "nz.co.wholemeal.christchurchmetro.MetroMapActivity");
        startActivity(intent);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void initFavourites() {
    SharedPreferences favourites = getSharedPreferences(PlatformActivity.PREFERENCES_FILE, 0);
    String stops_json = favourites.getString("favouriteStops", null);

    if (stops_json != null) {
      Log.d(TAG, "initFavourites(): stops_json = " + stops_json);
      try {
        JSONArray stops_array = (JSONArray) new JSONTokener(stops_json).nextValue();
        new AsyncLoadFavourites().execute(stops_array);
      } catch (JSONException e) {
        Log.e(TAG, "initFavourites(): JSONException: " + e.toString());
      }
    }
  }

  public void saveFavourites() {
    SharedPreferences favourites = getSharedPreferences(PlatformActivity.PREFERENCES_FILE, 0);
    saveFavourites(favourites);
    stopAdapter.notifyDataSetChanged();
  }

  public static void saveFavourites(SharedPreferences favourites) {
    SharedPreferences.Editor editor = favourites.edit();
    JSONArray stopArray = new JSONArray();
    Iterator iterator = stops.iterator();
    while (iterator.hasNext()) {
      Stop stop = (Stop)iterator.next();
      stopArray.put(stop.platformTag);
    }
    editor.putString("favouriteStops", stopArray.toString());
    Log.d(TAG, "Saving " + stops.size() + " favourites");
    Log.d(TAG, "json = " + stopArray.toString());
    editor.commit();
  }

  public static boolean isFavourite(Stop stop) {
    Iterator iterator = stops.iterator();

    /* Check the Stop is not already present in favourites */
    while (iterator.hasNext()) {
      Stop favourite = (Stop)iterator.next();
      if (favourite.platformTag.equals(stop.platformTag)) {
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

  public class AsyncLoadFavourites extends AsyncTask<JSONArray, Void, ArrayList> {

    private String TAG = "AsyncLoadFavourites";

    protected ArrayList doInBackground(JSONArray... stops_array) {
      ArrayList favourite_stops = new ArrayList<Stop>();
      JSONArray json_array = stops_array[0];

      for (int i = 0;i < json_array.length();i++) {
        try {
          String platformTag = (String)json_array.get(i);
          Log.d(TAG, "Loading stop platformTag = " + platformTag);
          Stop stop = new Stop(platformTag, null, getApplicationContext());
          favourite_stops.add(stop);
          Log.d(TAG, "initFavourites(): added stop platformTag = " + stop.platformTag);
        } catch (Stop.InvalidPlatformNumberException e) {
          Log.e(TAG, "Invalid platformTag in favourites: " + e.getMessage());
        } catch (JSONException e) {
          Log.e(TAG, "JSONException() parsing favourites: " + e.getMessage());
        }
      }
      return favourite_stops;
    }

    protected void onPostExecute(ArrayList favouriteStops) {
      Log.d(TAG, "onPostExecute() favouriteStops.size = " + favouriteStops.size());
      if (favouriteStops.size() > 0) {
        stops.addAll(favouriteStops);
      } else {
        Toast.makeText(getApplicationContext(), "No favourite stops yet",
            Toast.LENGTH_LONG).show();
      }
      stopAdapter.notifyDataSetChanged();
    }
  }
}
