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

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class PlatformSearchActivity extends ListActivity {

  String TAG = "PlatformSearchActivity";

  protected ArrayList<Stop> stops = new ArrayList<Stop>();

  @Override
  public void onCreate(Bundle saveInstanceState) {
    super.onCreate(saveInstanceState);

    setContentView(R.layout.search_list);

    Intent intent = getIntent();

    ListView listView = getListView();

    listView.setOnItemClickListener(new OnItemClickListener() {
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
        finish();
      }
    });

    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
      String query = intent.getStringExtra(SearchManager.QUERY);
      stops = Stop.searchStops(getApplicationContext(), query);
      StopAdapter stopAdapter = new StopAdapter(this, R.layout.stop_list_item,
          stops);
      setListAdapter(stopAdapter);
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
        v = vi.inflate(R.layout.search_list_item, null);
      }
      Stop stop = items.get(position);
      if (stop != null) {
        TextView platformNumber = (TextView) v.findViewById(R.id.platform_number);
        TextView platformName = (TextView) v.findViewById(R.id.platform_name);
        platformNumber.setText(stop.platformNumber);
        platformName.setText(stop.name);
      }
      return v;
    }
  }
}
