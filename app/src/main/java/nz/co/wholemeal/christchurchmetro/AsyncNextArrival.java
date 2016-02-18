package nz.co.wholemeal.christchurchmetro;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;

/* Load next arrival for each favourite in the background */
public class AsyncNextArrival extends AsyncTask<TextView, Void, TextView> {

    private static final String TAG = "AsyncNextArrival";
    private String arrivalText = null;
    private final Context mContext;

    public AsyncNextArrival(Context context) {
        mContext = context;
    }

    @Override
    protected TextView doInBackground(TextView... textViews) {
        TextView textView = textViews[0];
        Stop stop = (Stop) textView.getTag();
        Arrival arrival = null;
        ArrayList<Arrival> arrivals = null;
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Running AsyncNextArrival.doInBackground() for stop " + stop.platformNumber);
        }
        try {
            arrivals = stop.getArrivals();
        } catch (Exception e) {
            arrivalText = mContext.getString(R.string.unable_to_retrieve_information);
        }

        if (arrivals != null) {
            if (!arrivals.isEmpty()) {
                arrival = arrivals.get(0);
                arrivalText = mContext.getResources().getQuantityString(R.plurals.mins, arrival.eta, arrival.eta) +
                        ": " + arrival.routeNumber + " - " + arrival.destination;
            } else {
                arrivalText = mContext.getString(R.string.no_buses_due);
            }
        }
        return textView;
    }

    @Override
    protected void onPostExecute(TextView textView) {
        textView.setText(arrivalText);
    }
}