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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class MetroMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnInfoWindowClickListener, GoogleMap.OnCameraChangeListener,
        GoogleMap.OnMapLoadedCallback {

    GoogleMap mMap;
  /* The location used for the 'Bus Exchange' menu items */
  private final LatLng interchangeLatLng = new LatLng(-43.533798,172.637573);

  /* An optional route tag, if set only stops on this route will be displayed */
  private String routeTag = null;
  private String routeName = null;
  private boolean zoomToRoute = false;

  /* Paint style for the platform name text */
  protected Paint platformTextPaint = null;

  public static final String TAG = "MetroMapActivity";

  private HashMap<Marker, Stop> markerStopMap;

  protected boolean isRouteDisplayed() {
    return false;
  }

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.metro_map);

      SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
              .findFragmentById(R.id.map);
      mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {

      mMap = map;

      SharedPreferences preferences = getSharedPreferences(PlatformActivity.PREFERENCES_FILE, 0);
      float lastLatitude = preferences.getFloat("lastLat",
              (float) interchangeLatLng.latitude);
      float lastLongitude = preferences.getFloat("lastLon",
                (float) interchangeLatLng.longitude);
      float lastZoom = preferences.getFloat("lastZoom", 11);

    /* An intent may have been passed requesting a particular map location
     * to be centered */
    Bundle extras = getIntent().getExtras();
    if (extras != null) {
      double latitude = extras.getDouble("latitude");
      double longitude = extras.getDouble("longitude");
      Log.d(TAG, "Map lat/lon requested: " + latitude + " : " + longitude);
      if (latitude != 0 && longitude != 0) {
        lastLatitude = (float) latitude;
        lastLongitude = (float) longitude;
        lastZoom = 18;
      }

      // If this was not set in the Intent, null is fine
      routeTag = extras.getString("routeTag");
      routeName = extras.getString("routeName");
    }

    LatLng ll = new LatLng(lastLatitude, lastLongitude);
    mMap.moveCamera(CameraUpdateFactory.newLatLng(ll));
    mMap.moveCamera(CameraUpdateFactory.zoomTo(lastZoom));

    if (routeTag != null) {
      // A specific route view was requested.  Try and show the entire route
      // area.
      zoomToRoute = true;
    }
    markerStopMap = new HashMap<Marker, Stop>();

    drawStopsForCameraPosition(mMap.getCameraPosition());
    mMap.getUiSettings().setMapToolbarEnabled(false);
    mMap.setMyLocationEnabled(true);
    mMap.setOnInfoWindowClickListener(this);
    mMap.setOnCameraChangeListener(this);
    mMap.setOnMapLoadedCallback(this);
  }

  private void drawStopsForCameraPosition(CameraPosition cameraPosition) {

    if (routeTag == null && cameraPosition.zoom < 15) {
      return;
    }

    LatLngBounds latLngBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
    ArrayList<Stop> stops = Stop.getAllWithinBounds(this, latLngBounds, routeTag);

    if (stops != null) {
      Iterator<Stop> iterator = stops.iterator();

      while (iterator.hasNext()) {
        Stop stop = iterator.next();
        if (!markerStopMap.containsValue(stop)) {
          LatLng ll = new LatLng(stop.latitude, stop.longitude);
          Marker m = mMap.addMarker(
                  new MarkerOptions()
                          .position(ll)
                          .title(stop.name)
                          .icon(BitmapDescriptorFactory.fromResource(R.drawable.stop_marker))
          );
          Log.d(TAG, "stop = " + stop.name);
          Log.d(TAG, "marker id = " + m.getId());
          markerStopMap.put(m, stop);
        }
      }
    }
    Log.d(TAG, "markerStopMap.size() = " + markerStopMap.size());
  }
  @Override
  public void onStop() {
    SharedPreferences preferences = getSharedPreferences(PlatformActivity.PREFERENCES_FILE, 0);
    SharedPreferences.Editor editor = preferences.edit();

    CameraPosition cameraPosition = mMap.getCameraPosition();

    editor.putFloat("lastLat", (float) cameraPosition.target.latitude);
    editor.putFloat("lastLon", (float) cameraPosition.target.longitude);
    editor.putFloat("lastZoom", cameraPosition.zoom);

    editor.commit();
    super.onStop();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.map_menu, menu);
    return true;
  }

  @Override
  public void onInfoWindowClick(Marker marker) {
    Log.d(TAG, "Info Window Clicked for " + marker.getId());
    Log.d(TAG, "markerStopMap.size() = " + markerStopMap.size());
    Stop stop = markerStopMap.get(marker);
    Intent intent = new Intent();
    intent.putExtra("platformTag", stop.platformTag);
    intent.setClassName(
            "nz.co.wholemeal.christchurchmetro",
            "nz.co.wholemeal.christchurchmetro.PlatformActivity"
    );
    startActivity(intent);
  }

  @Override
  public void onCameraChange(CameraPosition cameraPosition) {
    drawStopsForCameraPosition(cameraPosition);
    Log.d(TAG, "Camera Zoom = " + cameraPosition.zoom);
    for(Marker m : markerStopMap.keySet()) {
      m.setVisible(routeTag != null || cameraPosition.zoom > 14);
    }
  }

  @Override
  public void onMapLoaded() {
    if(zoomToRoute) {
      LatLngBounds latLngBounds = Route.getLatLngBounds(getApplicationContext(),
              routeTag);
      Log.d(TAG, "LatLngBounds: " + latLngBounds);
      mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 5));
      zoomToRoute = false;
    }
  }
}
