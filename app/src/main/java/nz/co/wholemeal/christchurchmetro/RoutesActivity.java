/**
 * Copyright 2010 Malcolm Locke
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nz.co.wholemeal.christchurchmetro;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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

import java.util.ArrayList;

public class RoutesActivity extends ListActivity {

    public final static String TAG = "RoutesActivity";

    public static ArrayList<Route> routes = new ArrayList<Route>();
    private RouteAdapter routeAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String platformTag = extras.getString("platformTag");
            if (platformTag != null) {
                routes = Route.getRoutesForPlatform(getApplicationContext(), platformTag);
            }
        } else {
            routes = Route.getAll(getApplicationContext());
        }

        routeAdapter = new RouteAdapter(this, R.layout.route_list_item, routes);
        setListAdapter(routeAdapter);

        ListView listView = getListView();

    /* Enables the long click in the ListView to be handled in this Activity */
        registerForContextMenu(listView);

        listView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Route route = routes.get(position);
                Intent intent;

                if (route == null) {
                    Log.e(TAG, "Didn't get a route");
                    finish();
                }

                intent = getIntentForRouteMap(route);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.routes_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.map:
                Log.d(TAG, "Map selected from menu");
                intent = new Intent();
                intent.setClassName("nz.co.wholemeal.christchurchmetro",
                        "nz.co.wholemeal.christchurchmetro.MetroMapActivity");
                startActivity(intent);
                return true;
            case R.id.search:
                Log.d(TAG, "Search selected from menu");
                onSearchRequested();
                return true;
            case R.id.favourite_stops:
                Log.d(TAG, "Favourite stops selected");
                intent = new Intent();
                intent.setClassName("nz.co.wholemeal.christchurchmetro",
                        "nz.co.wholemeal.christchurchmetro.FavouritesActivity");
                startActivity(intent);
                return true;
            case R.id.preferences:
                Log.d(TAG, "Preferences selected from menu");
                intent = new Intent();
                intent.setClassName("nz.co.wholemeal.christchurchmetro",
                        "nz.co.wholemeal.christchurchmetro.PreferencesActivity");
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle(R.string.options);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.route_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

        Route route = routes.get((int) info.id);
        Intent intent;

        switch (item.getItemId()) {
            case R.id.show_on_map:
                intent = getIntentForRouteMap(route);
                startActivity(intent);
                return true;
            case R.id.timetable:
                Uri uri = Uri.parse("http://rtt.metroinfo.org.nz/rtt/public/Schedule.aspx?RouteNo=" + route.routeNumber);
                intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private Intent getIntentForRouteMap(Route route) {
        Intent intent = new Intent();
        intent.putExtra("routeTag", route.routeTag);
        intent.putExtra("routeName", route.fullRouteName());
        intent.setClassName("nz.co.wholemeal.christchurchmetro", "nz.co.wholemeal.christchurchmetro.MetroMapActivity");
        return intent;
    }

    private class RouteAdapter extends ArrayAdapter<Route> {

        private ArrayList<Route> items;

        public RouteAdapter(Context context, int textViewResourceId, ArrayList<Route> items) {
            super(context, textViewResourceId, items);
            this.items = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.route_list_item, null);
            }
            Route route = items.get(position);
            if (route != null) {
                TextView routeNumber = (TextView) v.findViewById(R.id.route_number);
                TextView destination = (TextView) v.findViewById(R.id.destination);
                TextView direction = (TextView) v.findViewById(R.id.direction);
                routeNumber.setText(route.routeNumber);
                destination.setText(route.destination);
                direction.setText(route.direction);
            }
            return v;
        }
    }
}
