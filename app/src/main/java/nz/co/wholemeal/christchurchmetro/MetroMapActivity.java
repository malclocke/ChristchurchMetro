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

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;


public class MetroMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnInfoWindowClickListener, GoogleMap.OnCameraChangeListener,
        GoogleMap.OnMapLoadedCallback, GoogleMap.OnPolylineClickListener {

    GoogleMap mMap;
    /* The location used for the 'Bus Exchange' menu items */
    private final LatLng interchangeLatLng = new LatLng(-43.533798, 172.637573);

    /* An optional route tag, if set only stops on this route will be displayed */
    private String routeTag = null;
    private String routeName = null;
    private boolean zoomToRoute = false;
    private int polylineWidth = 5; // Overridden later

    /* Paint style for the platform name text */
    protected Paint platformTextPaint = null;

    public static final String TAG = "MetroMapActivity";

    private HashMap<Marker, Stop> markerStopMap;
    private HashMap<Polyline, Route> polylineRouteMap = new HashMap<Polyline, Route>();
    private ArrayList<Polyline> polylines = new ArrayList<Polyline>();

    /* Hide stop markers below this zoom level */
    private int minPlatformZoom = 16;

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
        float zoom = preferences.getFloat("zoom", 11);

        // Calculate polyline width in px
        float density = getResources().getDisplayMetrics().density;
        polylineWidth = (int) (3.0f * density + 0.5f);

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
                zoom = 18;
            }

            // If this was not set in the Intent, null is fine
            routeTag = extras.getString("routeTag");
            routeName = extras.getString("routeName");
        }

        LatLng ll = new LatLng(lastLatitude, lastLongitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(ll));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(zoom));

        if (routeTag != null) {
            // A specific route view was requested.  Try and show the entire route
            // area.
            zoomToRoute = true;
        }
        markerStopMap = new HashMap<Marker, Stop>();

        setRouteDesriptionVisibility();

        drawRoutes(mMap);

        drawStopsForCameraPosition(mMap.getCameraPosition());
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.setMyLocationEnabled(true);
        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnCameraChangeListener(this);
        mMap.setOnMapLoadedCallback(this);
        mMap.setOnPolylineClickListener(this);
    }

    private int setRouteDesriptionVisibility() {
        TextView textView = (TextView) findViewById(R.id.route_description);
        int visibility = (routeName == null) ? View.INVISIBLE : View.VISIBLE;
        textView.setText(routeName);
        textView.setVisibility(visibility);
        return visibility;
    }


    private void drawRoute(GoogleMap map, Route route) {
        PolylineOptions polylineOptions = new PolylineOptions();
        int color = Color.BLACK;
        boolean visibility = true;

        polylineOptions.addAll(route.getCoordinateList());
        if (route.color != null) {
            color = Color.parseColor("#" + route.color);
        }
        polylineOptions.color(color);
        polylineOptions.clickable(true);
        polylineOptions.width(polylineWidth);

        if (routeTag != null) {
            visibility = route.routeTag.equals(routeTag);
        }
        polylineOptions.visible(visibility);

        Polyline polyline = map.addPolyline(polylineOptions);
        polylines.add(polyline);
        polylineRouteMap.put(polyline, route);
    }

    private void drawRoutes(GoogleMap map) {
        ArrayList<Route> routes = Route.getAll(this);
        Iterator<Route> iterator = routes.iterator();

        while (iterator.hasNext()) {
            Route route = iterator.next();
            drawRoute(map, route);
        }
    }

    private void drawStopsForCameraPosition(CameraPosition cameraPosition) {

        if (cameraPosition.zoom < minPlatformZoom) {
            return;
        }

        LatLngBounds latLngBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        ArrayList<Stop> stops = Stop.getAllWithinBounds(this, latLngBounds);

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
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "stop = " + stop.name);
                        Log.d(TAG, "marker id = " + m.getId());
                    }
                    markerStopMap.put(m, stop);
                }
            }
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "markerStopMap.size() = " + markerStopMap.size());
        }
    }

    @Override
    public void onStop() {
        SharedPreferences preferences = getSharedPreferences(PlatformActivity.PREFERENCES_FILE, 0);
        SharedPreferences.Editor editor = preferences.edit();

        CameraPosition cameraPosition = mMap.getCameraPosition();

        // Delete the old preferences with incompatible types
        editor.remove("lastLatitude");
        editor.remove("lastLongitude");
        editor.remove("lastZoom");

        editor.putFloat("lastLat", (float) cameraPosition.target.latitude);
        editor.putFloat("lastLon", (float) cameraPosition.target.longitude);
        editor.putFloat("zoom", cameraPosition.zoom);

        editor.commit();
        super.onStop();
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Info Window Clicked for " + marker.getId());
            Log.d(TAG, "markerStopMap.size() = " + markerStopMap.size());
        }
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
        setMarkersVisibility(cameraPosition);
    }

    private void setMarkersVisibility(CameraPosition cameraPosition) {
        for (Marker m : markerStopMap.keySet()) {
            setMarkerVisibility(cameraPosition, m);
        }
    }

    private boolean setMarkerVisibility(CameraPosition cameraPosition, Marker m) {
        boolean visibility = (cameraPosition.zoom > minPlatformZoom) && markerVisible(m);
        m.setVisible(visibility);
        return visibility;
    }

    private boolean markerVisible(Marker m) {
        if (routeTag == null) {
            return true;
        }
        return Arrays.asList(markerStopMap.get(m).routeTags).contains(routeTag);
    }

    @Override
    public void onMapLoaded() {
        if (zoomToRoute) {
            LatLngBounds latLngBounds = Route.getLatLngBounds(getApplicationContext(),
                    routeTag);
            Log.d(TAG, "LatLngBounds: " + latLngBounds);
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 5));
            zoomToRoute = false;
        }
    }

    @Override
    public void onPolylineClick(Polyline polyline) {
        Iterator<Polyline> iterator = polylines.iterator();
        while (iterator.hasNext()) {
            Polyline p = iterator.next();
            Route route = polylineRouteMap.get(p);
            if (route == null) {
                Log.e(TAG, "Cant find route!");
                return;
            }

            if (!p.equals(polyline)) {
                p.setVisible(!p.isVisible());
            } else {
                if (routeTag == null) {
                    routeTag = route.routeTag;
                    routeName = route.fullRouteName();
                } else {
                    routeTag = routeName = null;
                }
                p.setVisible(true);
            }
        }
        setRouteDesriptionVisibility();
        setMarkersVisibility(mMap.getCameraPosition());
    }
}
