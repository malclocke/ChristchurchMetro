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

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

import java.util.List;

public class MetroMapActivity extends MapActivity {

  private MapView mapView;
  private MapController mapController;
  private MyLocationOverlay myLocationOverlay;

  public static final String TAG = "MetroMapActivity";
  @Override
  protected boolean isRouteDisplayed() {
    return false;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.metro_map);

    mapView = (MapView) findViewById(R.id.metro_map);
    mapController = mapView.getController();
    mapView.setBuiltInZoomControls(true);

    List<Overlay> mapOverlays = mapView.getOverlays();

    myLocationOverlay = new MyLocationOverlay(this, mapView);
    mapOverlays.add(myLocationOverlay);
    myLocationOverlay.enableMyLocation();
    myLocationOverlay.runOnFirstFix(new Runnable() {
      public void run() {
        // Test with 'geo fix 172.641437 -43.534675' = Twisted Hop
        mapController.animateTo(myLocationOverlay.getMyLocation());
        mapController.setZoom(17);
      }
    });

    Drawable drawable = this.getResources().getDrawable(R.drawable.stop_marker);
    //MetroMapItemizedOverlay itemizedOverlay = new MetroMapItemizedOverlay(drawable, getApplicationContext());
    MetroMapOverlay overlay = new MetroMapOverlay(drawable, getApplicationContext());
    mapOverlays.add(overlay);

    try {
      Stop stop = new Stop("1",null,getApplicationContext());

      mapController.animateTo(stop.getGeoPoint());
      mapController.setZoom(17);
    } catch (Stop.InvalidPlatformNumberException e) {
      Log.e(TAG, "Invalid platform: " + e.getMessage(), e);
    }
  }
}
