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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/*
 * Some of the authors favourites:
 * "40188", "20763", "21450", "37375", "37334", "14864", "21957"
 */

public class FavouritesActivity extends ListActivity {

  public final static String TAG = "FavouritesActivity";
  public static ArrayList<Stop> stops = new ArrayList<Stop>();
  private StopAdapter stopAdapter;
  static final int DIALOG_LOAD_DATA = 0;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.favourites_list);

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

    promptToLoadPlatforms();
  }
/**
 * If this is the first time the user has loaded the application, and the list of
 * routes and platforms has not been loaded into the database yet, ask the user
 * if they want to.
 */
private void promptToLoadPlatforms() {
    SharedPreferences preferences = getSharedPreferences(PlatformActivity.PREFERENCES_FILE, 0);
    if (preferences.getLong("lastDataLoad", -1) == -1) {
      showDialog(DIALOG_LOAD_DATA);
    }
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
    menu.setHeaderTitle(R.string.options);
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
      case R.id.map:
        Log.d(TAG, "Map selected from menu");
        intent = new Intent();
        intent.setClassName("nz.co.wholemeal.christchurchmetro", "nz.co.wholemeal.christchurchmetro.MetroMapActivity");
        startActivity(intent);
        return true;
      case R.id.search:
        Log.d(TAG, "Search selected from menu");
        onSearchRequested();
        return true;
      case R.id.routes:
        Log.d(TAG, "Routes selected from menu");
        intent = new Intent();
        intent.setClassName("nz.co.wholemeal.christchurchmetro", "nz.co.wholemeal.christchurchmetro.RoutesActivity");
        startActivity(intent);
        return true;
      case R.id.preferences:
        Log.d(TAG, "Preferences selected from menu");
        intent = new Intent();
        intent.setClassName("nz.co.wholemeal.christchurchmetro", "nz.co.wholemeal.christchurchmetro.PreferencesActivity");
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
        ArrayList<Stop> favouriteStops = new ArrayList<Stop>();
        JSONArray stopsArray = (JSONArray) new JSONTokener(stops_json).nextValue();

        for (int i = 0;i < stopsArray.length();i++) {
          try {
            String platformTag = (String)stopsArray.get(i);
            Log.d(TAG, "Loading stop platformTag = " + platformTag);
            Stop stop = new Stop(platformTag, null, getApplicationContext());
            favouriteStops.add(stop);
            Log.d(TAG, "initFavourites(): added stop platformTag = " + stop.platformTag);
          } catch (Stop.InvalidPlatformNumberException e) {
            Log.e(TAG, "Invalid platformTag in favourites: " + e.getMessage());
          } catch (JSONException e) {
            Log.e(TAG, "JSONException() parsing favourites: " + e.getMessage());
          }
        }

        if (favouriteStops.size() > 0) {
          stops.addAll(favouriteStops);
        }
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
    Iterator<Stop> iterator = stops.iterator();
    while (iterator.hasNext()) {
      Stop stop = iterator.next();
      stopArray.put(stop.platformTag);
    }
    editor.putString("favouriteStops", stopArray.toString());
    Log.d(TAG, "Saving " + stops.size() + " favourites");
    Log.d(TAG, "json = " + stopArray.toString());
    editor.commit();
  }

  public static boolean isFavourite(Stop stop) {
    Iterator<Stop> iterator = stops.iterator();

    /* Check the Stop is not already present in favourites */
    while (iterator.hasNext()) {
      Stop favourite = iterator.next();
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

  public void removeFavourite(SharedPreferences preferences, Stop stop) {
    if (isFavourite(stop)) {
      Log.d(TAG, "Removed stop " + stop.platformNumber + " from favourites");
      stops.remove(stop);
      saveFavourites();
    } else {
      Log.e(TAG, "Remove requested for stop " + stop.platformNumber +
          " but it's not present in favourites");
    }
  }

  @Override
protected Dialog onCreateDialog(int id) {
    Dialog dialog;
    switch(id) {
      case DIALOG_LOAD_DATA:
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true)
          .setTitle(R.string.route_update_required)
          .setMessage(R.string.do_you_want_to_load_bus_stop_and_route_data)
          .setPositiveButton(R.string.load_now, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
              Intent intent = new Intent(getBaseContext(), LoadPlatformsService.class);
              startService(intent);
              dialog.cancel();
            }
          })
          .setNegativeButton(R.string.do_it_later, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
              dialog.cancel();
            }
          })
        ;
        dialog = builder.create();
        break;
      default:
        dialog = null;
    }
    return dialog;
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
        TextView nextBus = (TextView) v.findViewById(R.id.next_bus);
        platformNumber.setText(stop.platformNumber);
        platformName.setText(stop.name);
        nextBus.setTag(stop);
        nextBus.setText(R.string.next_bus_loading);
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
      ArrayList<Arrival> arrivals = null;
      Log.d(TAG, "Running AsyncNextArrival.doInBackground() for stop " + stop.platformNumber);
      try {
        arrivals = stop.getArrivals();
      } catch (Exception e) {
        arrivalText = getString(R.string.unable_to_retrieve_information);
      }

      if (arrivals != null) {
        if (!arrivals.isEmpty()) {
          arrival = (Arrival)arrivals.get(0);
          arrivalText = getResources().getQuantityString(R.plurals.mins, arrival.eta, arrival.eta) +
            ": " + arrival.routeNumber + " - " + arrival.destination;
        } else {
          arrivalText = getString(R.string.no_buses_due);
        }
      }
      return textView;
    }

    protected void onPostExecute(TextView textView) {
      textView.setText(arrivalText);
    }
  }

}
