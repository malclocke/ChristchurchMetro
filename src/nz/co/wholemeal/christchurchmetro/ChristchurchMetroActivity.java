package nz.co.wholemeal.christchurchmetro;

import android.app.TabActivity;
import android.os.Bundle;
import android.view.View;
import android.content.res.Resources;
import android.content.Intent;
import android.widget.TabHost;

import nz.co.wholemeal.christchurchmetro.StopActivity;
import nz.co.wholemeal.christchurchmetro.FavouritesActivity;

public class ChristchurchMetroActivity extends TabActivity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Resources res = getResources(); // Resource object to get Drawables
        TabHost tabHost = getTabHost();  // The activity TabHost
        TabHost.TabSpec spec;  // Resusable TabSpec for each tab
        Intent intent;  // Reusable Intent for each tab

        // Stop info tab
        intent = new Intent().setClass(this, StopActivity.class);
        spec = tabHost.newTabSpec("stop").setIndicator("Stop",
            res.getDrawable(R.drawable.tab_stop)).setContent(intent);
        tabHost.addTab(spec);

        // Favourites tab
        intent = new Intent().setClass(this, FavouritesActivity.class);
        spec = tabHost.newTabSpec("favourites").setIndicator("Facourites",
            res.getDrawable(R.drawable.tab_map)).setContent(intent);
        tabHost.addTab(spec);

        tabHost.setCurrentTab(0);
    }
}

