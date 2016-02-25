package nz.co.wholemeal.christchurchmetro;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.Iterator;

public class FavouritesManager {

    // TODO - Put this somewhere top level
    static final String PREFERENCES_FILE = "Preferences";

    private static final String TAG = "FavouritesManager";
    private static final String FAVOURITES_KEY = "favourites";
    private static final String OLD_FAVOURITES_KEY = "favouriteStops";

    private final Context mContext;
    private final ArrayList<Stop> mStops = new ArrayList<Stop>();
    private StopAdapter mStopAdapter;


    public FavouritesManager(Context context) {
        this.mContext = context;
        initFavourites();
    }

    private void initFavourites() {

        SharedPreferences favourites =
                mContext.getSharedPreferences(PlatformActivity.PREFERENCES_FILE, 0);

        if (convertFavouritesToNewFormat(favourites)) {
            return;
        }

        String stops_json = favourites.getString(FAVOURITES_KEY, null);

        if (stops_json != null) {
            Log.d(TAG, "initFavourites(): stops_json = " + stops_json);
            try {
                ArrayList<Stop> favouriteStops = new ArrayList<Stop>();
                JSONArray stopsArray = (JSONArray) new JSONTokener(stops_json).nextValue();

                for (int i = 0; i < stopsArray.length(); i++) {
                    try {
                        JSONObject jsonObject = (JSONObject) stopsArray.get(i);
                        String platformTag = (String) jsonObject.get("platformTag");
                        Stop stop = new Stop(platformTag, null, mContext);
                        stop.name = (String) jsonObject.get("name");
                        favouriteStops.add(stop);
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "initFavourites(): added stop platformTag = " + stop.platformTag);
                        }
                    } catch (Stop.InvalidPlatformNumberException e) {
                        Log.e(TAG, "Invalid platformTag in favourites: " + e.getMessage());
                    } catch (JSONException e) {
                        Log.e(TAG, "JSONException() parsing favourites: " + e.getMessage());
                    }
                }

                if (favouriteStops.size() > 0) {
                    mStops.addAll(favouriteStops);
                }
            } catch (JSONException e) {
                Log.e(TAG, "initFavourites(): JSONException: " + e.toString());
            }
        }
    }

    private boolean convertFavouritesToNewFormat(SharedPreferences favourites) {
        /*
        Old format was an array of strings containing platformTags, e.g.
            ["123","456"]
        If the old format is still present in the preferences, load the stops
        from it, save to the new format and then delete the old entry.
         */
        String stops_json = favourites.getString(OLD_FAVOURITES_KEY, null);
        if (stops_json == null) {
            return false;
        } else {
            try {
                ArrayList<Stop> favouriteStops = new ArrayList<Stop>();
                JSONArray stopsArray = (JSONArray) new JSONTokener(stops_json).nextValue();

                for (int i = 0; i < stopsArray.length(); i++) {
                    try {
                        String platformTag = (String) stopsArray.get(i);
                        Stop stop = new Stop(platformTag, null, mContext);
                        favouriteStops.add(stop);
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "convertFavouritesToNewFormat(): added stop platformTag = " + stop.platformTag);
                        }
                    } catch (Stop.InvalidPlatformNumberException e) {
                        Log.e(TAG, "Invalid platformTag in favourites: " + e.getMessage());
                    } catch (JSONException e) {
                        Log.e(TAG, "JSONException() parsing favourites: " + e.getMessage());
                    }
                }

                if (favouriteStops.size() > 0) {
                    mStops.addAll(favouriteStops);
                    saveFavourites();
                }

                SharedPreferences.Editor editor = favourites.edit();
                editor.remove(OLD_FAVOURITES_KEY);
                editor.apply();

            } catch (JSONException e) {
                Log.e(TAG, "initFavourites(): JSONException: " + e.toString());
            }
            return true;
        }
    }

    public boolean addStop(Stop stop) {
        if (!isFavourite(stop)) {
            mStops.add(stop);
            return saveFavourites();
        } else {
            return false;
        }
    }

    public boolean removeStop(Stop stop) {
        /**
         * Need to compare by platformTag, as stops.remove(stop) won't work
         * directly as the passed instance and the one in the adapter
         * are different instances.
         */
        Iterator<Stop> iterator = mStops.iterator();

        while (iterator.hasNext()) {
            Stop favouriteStop = iterator.next();
            if (favouriteStop.platformTag.equals(stop.platformTag)) {
                mStops.remove(favouriteStop);
                saveFavourites();
                return true;
            }
        }

        // Didn't find the stop in the favourites
        return false;
    }

    public boolean saveFavourites() {
        SharedPreferences favourites =
                mContext.getSharedPreferences(PREFERENCES_FILE, 0);
        SharedPreferences.Editor editor = favourites.edit();
        JSONArray stopArray = new JSONArray();
        Iterator<Stop> iterator = mStops.iterator();
        while (iterator.hasNext()) {
            Stop stop = iterator.next();
            JSONObject object = new JSONObject();
            try {
                object.put("platformTag", stop.platformTag);
                object.put("name", stop.name);
                stopArray.put(object);
            } catch (JSONException e) {
                Log.e(TAG, "JSONException() generating favourite: " + e.getMessage());
            }
        }
        editor.putString(FAVOURITES_KEY, stopArray.toString());
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Saving " + mStops.size() + " favourites");
            Log.d(TAG, "json = " + stopArray.toString());
        }
        editor.commit();
        return true;
    }

    public boolean isFavourite(Stop stop) {
        Iterator<Stop> iterator = mStops.iterator();

        /* Check the Stop is not already present in favourites */
        while (iterator.hasNext()) {
            Stop favourite = iterator.next();
            if (favourite.platformTag.equals(stop.platformTag)) {
                return true;
            }
        }
        return false;
    }

    public StopAdapter getStopAdapter() {
        if (mStopAdapter == null) {
            mStopAdapter =
                    new StopAdapter(mContext, R.layout.stop_list_item, mStops);
        }
        return mStopAdapter;
    }

}
