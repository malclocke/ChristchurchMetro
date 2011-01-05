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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;

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

    stopHeader = findViewById(R.id.stop_header);

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
    } else {
      Log.e(TAG, "No extras in intent");
    }

    ListView listView = getListView();
    listView.setOnItemClickListener(new OnItemClickListener() {
      public void onItemClick(AdapterView<?> parent, View view,
          int position, long id) {

        Arrival arrival = (Arrival)arrivals.get(position);
        Log.d(TAG, "Got click on arrival " + arrival.destination);

        createAlarmDialog(arrival);
      }
    });
  }

  @Override
  public void onNewIntent(Intent intent) {
    Log.d(TAG, "onNewIntent() called");
    Bundle extras = intent.getExtras();
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
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Intent intent = new Intent();
    switch (item.getItemId()) {
      case R.id.info:
        Log.d(TAG, "Info selected");
        String message = String.format(
          getResources().getString(R.string.platform_info),
          current_stop.roadName, current_stop.platformNumber,
          current_stop.platformTag, current_stop.latitude,
          current_stop.longitude);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(current_stop.name)
          .setMessage(message)
          .setNeutralButton(R.string.ok, null);
        builder.show();
        return true;

      case R.id.map:
        Log.d(TAG, "Map selected");
        intent.putExtra("latitude", current_stop.getGeoPoint().getLatitudeE6());
        intent.putExtra("longitude", current_stop.getGeoPoint().getLongitudeE6());
        intent.setClassName("nz.co.wholemeal.christchurchmetro",
            "nz.co.wholemeal.christchurchmetro.MetroMapActivity");
        startActivity(intent);
        return true;

      case R.id.refresh:
        Log.d(TAG, "Refresh selected");
        new AsyncLoadArrivals().execute(current_stop);
        return true;

      case R.id.routes_for_platform:
        Log.d(TAG, "Routes for platform selected");
        intent.putExtra("platformTag", current_stop.platformTag);
        intent.setClassName("nz.co.wholemeal.christchurchmetro",
            "nz.co.wholemeal.christchurchmetro.RoutesActivity");
        startActivity(intent);
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

  /**
   * Creates a dialog box that allows a user to set an alarm for a
   * particular eta for an arrival.
   */
  private void createAlarmDialog(final Arrival arrival) {
    AlertDialog.Builder builder;

    Context context = getApplicationContext();
    LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
    final View layout = inflater.inflate(R.layout.alarm_dialog, null);
    final NumberPicker numberPicker =
                  (NumberPicker) layout.findViewById(R.id.number_picker);
    int start = (arrival.eta > 10 ? 10 : arrival.eta - 1);
    numberPicker.setRange(1, arrival.eta);
    numberPicker.setCurrent(start);

    builder = new AlertDialog.Builder(this);
    builder.setTitle(R.string.set_alarm)
            .setMessage(R.string.alarm_dialog_text)
            .setView(layout)
            .setPositiveButton(R.string.set, new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int id) {
                int delay = numberPicker.getCurrent();
                createNotificationForArrival(arrival, delay);
              }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
              }
            })
            .show();
  }

  /**
   * Fires a notification when the arrival is the given number of minutes
   * away from arriving.
   */
  protected void createNotificationForArrival(Arrival arrival, int minutes) {
    Intent intent = new Intent(this, ArrivalNotificationReceiver.class);
    intent.putExtra("routeNumber", arrival.routeNumber);
    intent.putExtra("destination", arrival.destination);
    intent.putExtra("platformTag", current_stop.platformTag);
    intent.putExtra("minutes", minutes);
    PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT);

    // Calculate the time delay for this alarm
    int delay = arrival.eta - minutes;

    if (delay > 0) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(System.currentTimeMillis());
      calendar.add(Calendar.MINUTE, delay);

      AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
      alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
          sender);
      Toast.makeText(this, String.format(getResources().getString(
              R.string.set_alarm_for_time), DateFormat.getTimeInstance().format(calendar.getTime())), Toast.LENGTH_LONG).show();
      Log.d(TAG, "Set alarm for " + minutes + " minutes");
    } else {
      Toast.makeText(this, String.format(getResources().getString(
              R.string.arrival_already_n_minutes_or_less_away), minutes, arrival.eta), Toast.LENGTH_LONG).show();
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

  public void loadStopByPlatformTag(String platformTag) {
    Log.d(TAG, "Running loadStopByPlatformTag.doInBackground()");
    Stop stop = null;
    try {
      stop = new Stop(platformTag, null, getApplicationContext());
    } catch (Stop.InvalidPlatformNumberException e) {
      Log.d(TAG, "InvalidPlatformNumberException: " + e.getMessage());
    }
    if (stop == null) {
      Toast.makeText(getApplicationContext(), R.string.unable_to_find_stop,
          Toast.LENGTH_LONG).show();
    } else {
      loadStop(stop);
    }
  }

  public void setStopHeader(final Stop stop) {
    TextView platformNumber = (TextView)stopHeader.findViewById(R.id.platform_number);
    TextView platformName = (TextView)stopHeader.findViewById(R.id.platform_name);
    final ImageView favouriteIcon = (ImageView)stopHeader.findViewById(R.id.favourite_icon);
    platformNumber.setText(stop.platformNumber);
    platformName.setText(stop.name);

    if (FavouritesActivity.isFavourite(stop)) {
      favouriteIcon.setImageResource(R.drawable.favourite_active);
    } else {
      favouriteIcon.setImageResource(R.drawable.favourite_inactive);
    }

    favouriteIcon.setOnClickListener(new OnClickListener() {
      public void onClick(View view) {
        if (FavouritesActivity.isFavourite(stop)) {
          removeFromFavourites(stop);
          favouriteIcon.setImageResource(R.drawable.favourite_inactive);
        } else {
          addToFavourites(stop);
          favouriteIcon.setImageResource(R.drawable.favourite_active);
        }
      }
    });
  }

  public void addToFavourites(Stop stop) {
    Log.d(TAG, "addToFavourites(): " + stop.platformNumber);

    if (! FavouritesActivity.isFavourite(stop)) {
      SharedPreferences favourites = getSharedPreferences(PREFERENCES_FILE, 0);
      FavouritesActivity.stops.add(stop);
      FavouritesActivity.saveFavourites(favourites);
      Toast.makeText(getApplicationContext(),
        String.format(getResources().getString(R.string.added_to_favourites), stop.name),
        Toast.LENGTH_LONG).show();
    }
  }

  public void removeFromFavourites(Stop stop) {
    if (FavouritesActivity.isFavourite(stop)) {
      SharedPreferences favourites = getSharedPreferences(PREFERENCES_FILE, 0);

      /* Need to compare by platformTag, as stops.remove(stop) won't work
       * directly as this instance and the one in FavouritesActivity.stops
       * are different instances */
      Iterator<Stop> iterator = FavouritesActivity.stops.iterator();

      while (iterator.hasNext()) {
        Stop favouriteStop = iterator.next();
        if (favouriteStop.platformTag.equals(stop.platformTag)) {
          FavouritesActivity.stops.remove(favouriteStop);
          FavouritesActivity.saveFavourites(favourites);
          Toast.makeText(getApplicationContext(),
            String.format(getResources().getString(R.string.removed_from_favourites), stop.name),
            Toast.LENGTH_LONG).show();
          break;
        }
      }
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
          eta.setText(Integer.toString(arrival.eta));
          if (arrival.eta == 1) {
            TextView mins = (TextView) v.findViewById(R.id.mins);
            mins.setText(R.string.min);
          }
        }
      }
      return v;
    }
  }

  /* Loads the arrival information in a background thread. */
  public class AsyncLoadArrivals extends AsyncTask<Stop, Void, ArrayList> {

    ProgressDialog progressDialog;

    protected void onPreExecute() {
      progressDialog = new ProgressDialog(PlatformActivity.this);
      progressDialog.setMessage(getString(R.string.loading_arrivals));
      progressDialog.setIndeterminate(true);
      progressDialog.setCancelable(true);
      progressDialog.show();
    }

    protected void onPostExecute(ArrayList stopArrivals) {
      Log.d(TAG, "onPostExecute()");
      if (stopArrivals == null) {
        ((TextView)findViewById(android.R.id.empty))
          .setText(R.string.unable_to_retrieve_arrival_information);
      } else if (stopArrivals.size() > 0) {
        arrivals.clear();
        arrivals.addAll(stopArrivals);
      } else {
        Log.d(TAG, "No arrivals");
        arrivals.clear();
        ((TextView)findViewById(android.R.id.empty)).setText(R.string.no_arrivals_in_the_next_thirty_minutes);
      }
      progressDialog.dismiss();
      arrival_adapter.notifyDataSetChanged();
    }

    protected ArrayList doInBackground(Stop... stops) {
      Log.d(TAG, "Running doInBackground()");
      ArrayList arrivals = null;
      try {
        arrivals = stops[0].getArrivals();
      } catch (Exception e) {
        Log.e(TAG, "getArrivals(): " + e.getMessage());
      }

      return arrivals;
    }
  }
}
