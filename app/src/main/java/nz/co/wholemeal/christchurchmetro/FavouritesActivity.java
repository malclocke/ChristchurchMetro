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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import java.util.concurrent.TimeUnit;


/*
 * Some of the authors favourites:
 * "40188", "20763", "21450", "37375", "37334", "14864", "21957"
 */

public class FavouritesActivity extends AppCompatListActivity {

    public final static String TAG = "FavouritesActivity";

    static final int DIALOG_LOAD_DATA = 0;
    private FavouritesManager mFavouritesManager;
    public ListView listView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, PlatformActivity.PREFERENCES_FILE,
                MODE_PRIVATE, R.xml.preferences, false);

        setContentView(R.layout.main_layout);

        setToolbar(R.id.toolbar);

        getListView().setEmptyView(findViewById(android.R.id.empty));
        registerForContextMenu(getListView());
        loadPlatformsCheck();
        getListView().setOnItemClickListener(new FavouriteItemClickListener());
    }

    /**
     * If this is the first time the user has loaded the application, and the list of
     * routes and platforms has not been loaded into the database yet, ask the user
     * if they want to.  Otherwise, check how long since the data was loaded and reload if
     * it's out of date.
     */
    private void loadPlatformsCheck() {
        SharedPreferences preferences = getSharedPreferences(PlatformActivity.PREFERENCES_FILE, 0);
        long lastDataLoad = preferences.getLong("lastDataLoad", -1);
        long daysSinceLastLoad = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastDataLoad);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "preferences = " + preferences.getAll());
        }
        int reloadFrequency = Integer.parseInt(
                preferences.getString("autoUpdateRouteFrequency", "14")
        );

        boolean reloadAllowed = preferences.getBoolean("autoUpdateRoutes", false);
        boolean reloadRequired = reloadAllowed && daysSinceLastLoad >= reloadFrequency;

        if(BuildConfig.DEBUG) {
            Log.d(TAG, "loadPlatformsCheck(): lastDataLoad = " + lastDataLoad);
            Log.d(TAG, "loadPlatformsCheck(): reloadFrequency = " + reloadFrequency);
            Log.d(TAG, "loadPlatformsCheck(): currentTimeMillis() = " + System.currentTimeMillis());
            Log.d(TAG, "loadPlatformsCheck(): reloadAllowed = " + reloadAllowed);
            Log.d(TAG, "loadPlatformsCheck(): reloadRequired = " + reloadRequired);
            Log.d(TAG, "loadPlatformsCheck(): days since last load = " + daysSinceLastLoad);
        }

        if (lastDataLoad == -1) {
            showDialog(DIALOG_LOAD_DATA);
        } else if(reloadRequired) {
            loadPlatforms();
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

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        switch (id) {
            case DIALOG_LOAD_DATA:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setCancelable(true)
                        .setTitle(R.string.route_update_required)
                        .setMessage(R.string.do_you_want_to_load_bus_stop_and_route_data)
                        .setPositiveButton(R.string.load_now, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                loadPlatforms();
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

    private void loadPlatforms() {
        Intent intent = new Intent(getBaseContext(), LoadPlatformsService.class);
        startService(intent);
    }

    public void onFavouriteSelected(Stop stop) {
        Intent intent = new Intent(this, PlatformActivity.class);
        intent.putExtra("platformTag", stop.platformTag);
        startActivity(intent);
    }

    private void reloadFavourites() {
        initFavourites();
        setListAdapter(mFavouritesManager.getStopAdapter());
    }

    @Override
    public void onResume() {
        reloadFavourites();
        super.onResume();
    }

    private void initFavourites() {
        mFavouritesManager = new FavouritesManager(this);
    }

    public void removeFavourite(Stop stop) {
        if (mFavouritesManager.removeStop(stop)) {
            Log.d(TAG, "Removed stop " + stop.platformNumber + " from favourites");
        } else {
            Log.e(TAG, "Remove requested for stop " + stop.platformNumber +
                    " but it's not present in favourites");
        }
        reloadFavourites();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Stop stop = (Stop) getListAdapter().getItem((int)info.id);
        switch (item.getItemId()) {
            case R.id.remove_favourite:
                removeFavourite(stop);
                return true;
            case R.id.rename:
                renameFavourite(stop);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void renameFavourite(final Stop stop) {
        final EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        editText.setText(stop.name);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(editText);
        builder.setMessage("Rename favourite");

        builder.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mFavouritesManager.renameStop(stop, editText.getText().toString());
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) { }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle(R.string.options);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.favourite_context_menu, menu);
    }

    private class FavouriteItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Stop stop = (Stop) getListAdapter().getItem(position);

            if (stop != null) {
                onFavouriteSelected(stop);
            } else {
                Log.e(TAG, "Didn't get a stop");
            }
        }
    }
}