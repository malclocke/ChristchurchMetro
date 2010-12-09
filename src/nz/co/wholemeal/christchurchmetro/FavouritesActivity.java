package nz.co.wholemeal.christchurchmetro;

import java.util.ArrayList;

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
import android.os.Bundle;

public class FavouritesActivity extends ListActivity {

  private ArrayList stops = new ArrayList<Stop>();
  /*
  private static final String[] FAVOURITES = new String[] {
    "40188", "20763", "21450", "37375", "37334", "14864", "21957"
  };
  */
  private static String STOP_JSON = "{" +
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
    "}";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Stop stop;

    stop = new Stop();
    stop.setAttributesFromJSONString(STOP_JSON);
    stops.add(stop);

    setListAdapter(new StopAdapter(this, R.layout.list_item, stops));

    ListView lv = getListView();
    lv.setTextFilterEnabled(true);

    lv.setOnItemClickListener(new OnItemClickListener() {
      public void onItemClick(AdapterView<?> parent, View view,
          int position, long id) {
        Toast.makeText(getApplicationContext(), ((Stop)stops.get(position)).getPlatformNumber(),
            Toast.LENGTH_SHORT).show();
      }
    });
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
