package nz.co.wholemeal.christchurchmetro;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class FavouritesManager {

    // TODO - Put this somewhere top level
    static final String PREFERENCES_FILE = "Preferences";

    private static final String TAG = "FavouritesManager";

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
                        Stop stop = new Stop(platformTag, null, mContext);
                        favouriteStops.add(stop);
                        Log.d(TAG, "initFavourites(): added stop platformTag = " + stop.platformTag);
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
            stopArray.put(stop.platformTag);
        }
        editor.putString("favouriteStops", stopArray.toString());
        Log.d(TAG, "Saving " + mStops.size() + " favourites");
        Log.d(TAG, "json = " + stopArray.toString());
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
