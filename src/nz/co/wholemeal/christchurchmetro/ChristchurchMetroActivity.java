package nz.co.wholemeal.christchurchmetro;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.content.Intent;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.util.Log;

import nz.co.wholemeal.christchurchmetro.R;
import nz.co.wholemeal.christchurchmetro.Stop;

public class ChristchurchMetroActivity extends ListActivity
{
  private EditText entry;

  private Stop current_stop;
  private ArrayList arrivals = new ArrayList<Arrival>();
  private ArrivalAdapter arrival_adapter;

  static final int CHOOSE_FAVOURITE = 0;
  static final String TAG = "ChristchurchMetroActivity";

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.stop);

    arrival_adapter = new ArrivalAdapter(this, R.layout.list_item, arrivals);
    setListAdapter(arrival_adapter);

    current_stop = null;

    entry = (EditText)findViewById(R.id.entry);

    final Button go_button = (Button)findViewById(R.id.go);
    go_button.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        String stop_number = entry.getText().toString();
        if (stop_number.length() == 5) {
          Stop stop = new Stop(stop_number);
          loadStop(stop);
        } else {
          Log.d(TAG, "go_button.onClick(): entry text incorrect length");
        }
      }
    });

    final Button faves_button = (Button)findViewById(R.id.faves);
    faves_button.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        Intent intent = new Intent(ChristchurchMetroActivity.this,
          FavouritesActivity.class);
        ChristchurchMetroActivity.this.startActivityForResult(intent, CHOOSE_FAVOURITE);
      }
    });
  }

  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    Log.d(TAG, "Activity returned resultCode = " + resultCode);
    switch (requestCode) {
      case CHOOSE_FAVOURITE:
        if (resultCode != RESULT_CANCELED) {
          Bundle extras = data.getExtras();
          if (extras != null) {
            Log.d(TAG, "stop " + extras.getString("platformNumber") + " selected");
            Stop stop = new Stop(extras.getString("platformNumber"));
            entry.setText(extras.getString("platformNumber"));
            loadStop(stop);
          }
        }

      default:
        break;
    }
  }

  public void loadStop(Stop stop) {
    Log.d(TAG, "loadStop(): " + stop.getPlatformNumber());
    current_stop = stop;
    ArrayList stopArrivals = stop.getArrivals();
    if (stopArrivals.size() > 0) {
      Log.d(TAG, "arrivals.size() = " + arrivals.size());
      arrivals.clear();
      arrivals.addAll(stopArrivals);
      arrival_adapter.notifyDataSetChanged();
    }
  }

  public void loadStop(String platformNumber) {
    loadStop(new Stop(platformNumber));
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
        TextView routeName = (TextView) v.findViewById(R.id.route_name);
        TextView eta = (TextView) v.findViewById(R.id.eta);
        if (routeNumber != null) {
          routeNumber.setText(arrival.getRouteNumber());
        }
        if (routeName != null) {
          routeName.setText(arrival.getRouteName());
        }
        if (eta != null) {
          eta.setText(arrival.getEta() + " minutes");
        }
      }
      return v;
    }
  }
}
