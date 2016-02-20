package nz.co.wholemeal.christchurchmetro;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.ListAdapter;
import android.widget.ListView;

public class AppCompatListActivity extends AppCompatActivity {

    public ListView listView;

    public Toolbar setToolbar(int id) {
        Toolbar toolbar = (Toolbar) findViewById(id);
        setSupportActionBar(toolbar);
        return toolbar;
    }

    public ListView getListView() {
        if (listView == null) {
            listView = (ListView) findViewById(android.R.id.list);
        }
        return listView;
    }

    public void setListAdapter(ListAdapter listAdapter) {
        getListView().setAdapter(listAdapter);
    }

    public ListAdapter getListAdapter() {
        return getListView().getAdapter();
    }
}
