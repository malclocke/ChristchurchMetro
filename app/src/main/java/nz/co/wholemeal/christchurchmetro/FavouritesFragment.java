package nz.co.wholemeal.christchurchmetro;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;

public class FavouritesFragment extends ListFragment {

    protected static final String TAG = "FavouritesFragment";
    private FavouritesManager mFavouritesManager;
    private FavouriteSelectedListener mFavouriteSelectedListener;

    public interface FavouriteSelectedListener {
        public void onFavouriteSelected(Stop stop);
    }

    private void reloadFavourites() {
        initFavourites();
        setListAdapter(mFavouritesManager.getStopAdapter());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mFavouriteSelectedListener =
                    (FavouriteSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement FavouriteSelectedListener");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        registerForContextMenu(getListView());
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
//        Intent intent = new Intent();
        Stop stop = (Stop) getListAdapter().getItem(position);

        if (stop != null) {
            mFavouriteSelectedListener.onFavouriteSelected(stop);
        } else {
            Log.e(TAG, "Didn't get a stop");
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        setEmptyText(getText(R.string.no_favourites_help));
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        reloadFavourites();
        super.onResume();
    }

    private void initFavourites() {
        mFavouritesManager = new FavouritesManager(getActivity());
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
      AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
      Stop stop = (Stop) getListAdapter().getItem((int)info.id);
      switch (item.getItemId()) {
        case R.id.remove_favourite:
          removeFavourite(stop);
          return true;
        default:
          return super.onContextItemSelected(item);
      }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
      super.onCreateContextMenu(menu, v, menuInfo);
      menu.setHeaderTitle(R.string.options);
      MenuInflater inflater = getActivity().getMenuInflater();
      inflater.inflate(R.menu.favourite_context_menu, menu);
    }

}
