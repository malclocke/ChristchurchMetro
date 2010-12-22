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

import android.app.ListActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.View;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.util.Log;

import nz.co.wholemeal.christchurchmetro.R;
import nz.co.wholemeal.christchurchmetro.Stop;

import org.json.JSONArray;

public class PlatformActivity extends ListActivity
{
  private Stop current_stop;
  private ArrayList arrivals = new ArrayList<Arrival>();
  private ArrivalAdapter arrival_adapter;
  private View stopHeader;

  static final int CHOOSE_FAVOURITE = 0;
  static final String TAG = "PlatformActivity";
  static final String PREFERENCES_FILE = "Preferences";

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.stop);

    stopHeader = getLayoutInflater().inflate(R.layout.stop_header, null);
    getListView().addHeaderView(stopHeader);

    arrival_adapter = new ArrivalAdapter(this, R.layout.arrival, arrivals);
    setListAdapter(arrival_adapter);

    current_stop = null;

    /* Load the requested stop information */
    Bundle extras = getIntent().getExtras();
    if (extras != null) {
      String platformTag = extras.getString("platformTag");
      if (platformTag != null) {
        loadStopByPlatformTag(platformTag);
      }
    }
  }

  @Override
  protected void onStop() {
    super.onStop();

  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.platform_menu, menu);
    if (FavouritesActivity.isFavourite(current_stop)) {
      menu.removeItem(R.id.add_to_favourites);
    }
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Intent intent;
    switch (item.getItemId()) {
      case R.id.add_to_favourites:
        Log.d(TAG, "Add to favourites selected");
        addToFavourites(current_stop);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    Log.d(TAG, "Activity returned resultCode = " + resultCode);
    switch (requestCode) {
      case CHOOSE_FAVOURITE:
        if (resultCode != RESULT_CANCELED) {
          Bundle extras = data.getExtras();
          if (extras != null) {
            Log.d(TAG, "platformTag " + extras.getString("platformTag") + " selected");
            loadStopByPlatformTag(extras.getString("platformTag"));
          }
        }

      default:
        break;
    }
  }

  public void loadStop(Stop stop) {
    Log.d(TAG, "loadStop(Stop): " + stop.platformNumber + " platformTag = " + stop.platformTag);
    current_stop = stop;
    setStopHeader(stop);
    arrivals.clear();
    arrival_adapter.notifyDataSetChanged();

    new AsyncLoadArrivals().execute(stop);
  }

  public void loadStopByPlatformNumber(String platformNumber) {
    new AsyncLoadStopByPlatformNumber().execute(platformNumber);
  }

  public void loadStopByPlatformTag(String platformTag) {
    new AsyncLoadStopByPlatformTag().execute(platformTag);
  }

  public void setStopHeader(final Stop stop) {
    TextView platformNumber = (TextView)stopHeader.findViewById(R.id.platform_number);
    TextView platformName = (TextView)stopHeader.findViewById(R.id.platform_name);
    platformNumber.setText(stop.platformNumber);
    platformName.setText(stop.name);
  }

  public void addToFavourites(Stop stop) {
    Log.d(TAG, "addToFavourites(): " + stop.platformNumber);

    if (! FavouritesActivity.isFavourite(stop)) {
      SharedPreferences favourites = getSharedPreferences(PREFERENCES_FILE, 0);
      FavouritesActivity.stops.add(stop);
      FavouritesActivity.saveFavourites(favourites);
      Toast.makeText(getApplicationContext(), "Added '" + stop.name +
          "' to favourites", Toast.LENGTH_LONG).show();
    }
  }

  private class ArrivalAdapter extends ArrayAdapter<Arrival> {

    private ArrayList<Arrival> arrivalList;

    public ArrivalAdapter(Context context, int textViewResourceId, ArrayList<Arrival> items) {
      super(context, textViewResourceId, items);
      arrivalList = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View v = convertView;
      if (v == null) {
        LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = vi.inflate(R.layout.arrival, null);
      }
      Arrival arrival = arrivalList.get(position);
      if (arrival != null) {
        TextView routeNumber = (TextView) v.findViewById(R.id.route_number);
        TextView destination = (TextView) v.findViewById(R.id.destination);
        TextView eta = (TextView) v.findViewById(R.id.eta);
        if (routeNumber != null) {
          routeNumber.setText(arrival.routeNumber);
        }
        if (destination != null) {
          destination.setText(arrival.destination);
        }
        if (eta != null) {
          eta.setText(arrival.eta + " minutes");
        }
      }
      return v;
    }
  }

  public class AsyncLoadStopByPlatformTag extends AsyncTask<String, Void, Stop> {
    protected Stop doInBackground(String... platformTags) {
      Log.d(TAG, "Running AsyncLoadStopByPlatformTag.doInBackground()");
      Stop stop = null;
      try {
        stop = new Stop(platformTags[0], null, getApplicationContext());
      } catch (Stop.InvalidPlatformNumberException e) {
        Log.d(TAG, "InvalidPlatformNumberException: " + e.getMessage());
      }
      return stop;
    }

    protected void onPostExecute(Stop stop) {
      Log.d(TAG, "AsyncLoadStopByPlatformTag.onPostExecute()");
      if (stop == null) {
        Toast.makeText(getApplicationContext(), "Unable to find stop",
            Toast.LENGTH_LONG).show();
      } else {
        loadStop(stop);
      }
    }
  }

  public class AsyncLoadStopByPlatformNumber extends AsyncTask<String, Void, Stop> {
    protected Stop doInBackground(String... platformNumbers) {
      String platformNumber = platformNumbers[0];
      Log.d(TAG, "Running AsyncLoadStopByPlatformNumber.doInBackground() platformNumber = " + platformNumber);
      Stop stop = null;
      try {
        stop = new Stop(null, platformNumber, getApplicationContext());
      } catch (Stop.InvalidPlatformNumberException e) {
        Log.d(TAG, "InvalidPlatformNumberException: " + e.getMessage());
      }
      return stop;
    }

    protected void onPostExecute(Stop stop) {
      Log.d(TAG, "AsyncLoadStopByPlatformNumber.onPostExecute()");
      if (stop == null) {
        Toast.makeText(getApplicationContext(), "Unable to find stop",
            Toast.LENGTH_LONG).show();
      } else {
        loadStop(stop);
      }
    }
  }

  /* Loads the arrival information in a background thread. */
  public class AsyncLoadArrivals extends AsyncTask<Stop, Void, ArrayList> {

    protected void onPostExecute(ArrayList stopArrivals) {
      Log.d(TAG, "onPostExecute()");
      if (stopArrivals.size() > 0) {
        Log.d(TAG, "arrivals.size() = " + arrivals.size());
        arrivals.addAll(stopArrivals);
      } else {
        Log.d(TAG, "No arrivals");
        Toast.makeText(getApplicationContext(),
            "No arrivals for this stop in the next 30 minutes",
            Toast.LENGTH_LONG).show();
      }
      arrival_adapter.notifyDataSetChanged();
    }

    protected ArrayList doInBackground(Stop... stops) {
      Log.d(TAG, "Running doInBackground()");
      return stops[0].getArrivals();
    }
  }
}
