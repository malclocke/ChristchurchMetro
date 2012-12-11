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
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class SearchActivity extends ListActivity {

  String TAG = "SearchActivity";

  protected ArrayList results = new ArrayList();

  @Override
  public void onCreate(Bundle saveInstanceState) {
    super.onCreate(saveInstanceState);

    setContentView(R.layout.search_list);

    Intent intent = getIntent();

    ListView listView = getListView();

    listView.setOnItemClickListener(new OnItemClickListener() {
      public void onItemClick(AdapterView<?> parent, View view,
          int position, long id) {

        Adapter adapter = parent.getAdapter();

        Intent intent = new Intent();

        switch (adapter.getItemViewType(position)) {
          case SearchAdapter.TYPE_PLATFORM:
            Stop stop = (Stop)results.get(position);

            if (stop == null) {
              Log.e(TAG, "Didn't get a stop");
              finish();
            }
            intent.putExtra("platformTag", stop.platformTag);
            intent.setClassName("nz.co.wholemeal.christchurchmetro", "nz.co.wholemeal.christchurchmetro.PlatformActivity");
            break;

          case SearchAdapter.TYPE_ROUTE:
            Route route = (Route)results.get(position);

            if (route == null) {
              Log.e(TAG, "Expected a route but didn't get one");
              finish();
            }
            intent.putExtra("routeTag", route.routeTag);
            intent.putExtra("routeName", route.routeNumber + " " + route.destination);
            intent.setClassName("nz.co.wholemeal.christchurchmetro",
                "nz.co.wholemeal.christchurchmetro.MetroMapActivity");
            break;
        }

        startActivity(intent);
      }
    });

    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
      String query = intent.getStringExtra(SearchManager.QUERY);
      ArrayList routes = Route.searchRoutes(getApplicationContext(), query);
      ArrayList stops = Stop.searchStops(getApplicationContext(), query);
      if (routes.size() > 0) {
        results.add("Routes");
        results.addAll(routes);
      }
      if (stops.size() > 0) {
        results.add("Stops");
        results.addAll(stops);
      }
      SearchAdapter searchAdapter = new SearchAdapter(this, R.layout.stop_list_item,
          results);
      setListAdapter(searchAdapter);
    }
  }

  private class SearchAdapter extends ArrayAdapter {

    private ArrayList items;

    public static final int TYPE_PLATFORM = 0;
    public static final int TYPE_ROUTE = 1;
    public static final int TYPE_SEPARATOR = 2;
    public static final int TYPE_COUNT = TYPE_SEPARATOR + 1;

    public SearchAdapter(Context context, int textViewResourceId, ArrayList<Stop> items) {
      super(context, textViewResourceId, items);
      this.items = items;
    }

    @Override
    public int getViewTypeCount() {
      return TYPE_COUNT;
    }

    @Override
    public boolean isEnabled(int position) {
      return (getItemViewType(position) != TYPE_SEPARATOR);
    }

    @Override
    public int getItemViewType(int position) {
      if (items.get(position) instanceof Route) {
        return TYPE_ROUTE;
      } else if (items.get(position) instanceof Stop) {
        return TYPE_PLATFORM;
      } else {
        return TYPE_SEPARATOR;
      }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View v = convertView;
      int type = getItemViewType(position);
      if (v == null) {
        LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        switch (type) {
          case TYPE_PLATFORM:
            v = vi.inflate(R.layout.search_list_item, null);
            break;

          case TYPE_ROUTE:
            v = vi.inflate(R.layout.route_list_item, null);
            break;

          case TYPE_SEPARATOR:
            v = vi.inflate(R.layout.search_list_separator, null);
            break;
        }
      }

      switch (type) {
        case TYPE_PLATFORM:
          Stop stop = (Stop)items.get(position);
          if (stop != null) {
            TextView platformNumber = (TextView) v.findViewById(R.id.platform_number);
            TextView platformName = (TextView) v.findViewById(R.id.platform_name);
            platformNumber.setText(stop.platformNumber);
            platformName.setText(stop.name);
          }
          break;

        case TYPE_ROUTE:
          Route route = (Route)items.get(position);
          TextView routeNumber = (TextView) v.findViewById(R.id.route_number);
          TextView destination = (TextView) v.findViewById(R.id.destination);
          TextView direction = (TextView) v.findViewById(R.id.direction);
          routeNumber.setText(route.routeNumber);
          destination.setText(route.destination);
          direction.setText(route.direction);
          break;

        case TYPE_SEPARATOR:
          TextView separator = (TextView) v;
          separator.setText((String)items.get(position));
          break;
      }
      return v;
    }
  }
}
