package nz.co.wholemeal.christchurchmetro;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class StopAdapter extends ArrayAdapter<Stop> {

    private final ArrayList<Stop> mItems;

    public StopAdapter(Context context, int textViewResourceId,
            ArrayList<Stop> items) {
        super(context, textViewResourceId, items);
        this.mItems = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi =
                    (LayoutInflater) getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.stop_list_item, null);
        }
        Stop stop = mItems.get(position);
        if (stop != null) {
            TextView platformNumber = (TextView) v.findViewById(R.id.platform_number);
            TextView platformName = (TextView) v.findViewById(R.id.platform_name);
            TextView nextBus = (TextView) v.findViewById(R.id.next_bus);
            platformNumber.setText(stop.platformNumber);
            platformName.setText(stop.name);
            nextBus.setTag(stop);
            nextBus.setText(R.string.next_bus_loading);
            new AsyncNextArrival(getContext()).execute(nextBus);
        }
        return v;
    }
}