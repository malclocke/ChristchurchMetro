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
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MapView.LayoutParams;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public class MetroMapActivity extends MapActivity {

  private MapView mapView;
  private MapController mapController;
  private MyLocationOverlay myLocationOverlay;

  /* The location used for the 'Bus Exchange' menu item */
  private GeoPoint exchangeGeoPoint = new GeoPoint(-43533798,172637573);

  /* The last location received from the location manager */
  private GeoPoint lastFix = null;

  /* An optional route tag, if set only stops on this route will be displayed */
  private String routeTag = null;
  private String routeName = null;

  /* Paint style for the platform name text */
  protected Paint platformTextPaint = null;

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

    SharedPreferences preferences = getSharedPreferences(PlatformActivity.PREFERENCES_FILE, 0);
    int lastLatitude = preferences.getInt("lastLatitude",
        exchangeGeoPoint.getLatitudeE6());
    int lastLongitude = preferences.getInt("lastLongitude",
        exchangeGeoPoint.getLongitudeE6());
    int lastZoom = preferences.getInt("lastZoom", 11);

    /* An intent may have been passed requesting a particular map location
     * to be centered */
    Bundle extras = getIntent().getExtras();
    if (extras != null) {
      int latitude = extras.getInt("latitude");
      int longitude = extras.getInt("longitude");
      if (latitude != 0 && longitude != 0) {
        lastLatitude = latitude;
        lastLongitude = longitude;
        lastZoom = 18;
      }

      // If this was not set in the Intent, null is fine
      routeTag = extras.getString("routeTag");
      routeName = extras.getString("routeName");
    }

    if (routeTag == null) {
      mapController.setCenter(new GeoPoint(lastLatitude, lastLongitude));
      mapController.setZoom(lastZoom);
    } else {
      // A specific route view was requested.  Try and show the entire route
      // area.
      int boundaries[] = Route.getRouteBoundaries(getApplicationContext(),
          routeTag);
      int latitudeSpan = boundaries[1] - boundaries[3];
      int longitudeSpan = boundaries[2] - boundaries[0];
      Log.d(TAG, "Boundaries = " + boundaries[0] + "," + boundaries[1] +
          "," + boundaries[2] + "," + boundaries[3]);
      Log.d(TAG, "Latitude span = " + latitudeSpan +
          ", Longitude span = " + longitudeSpan);
      mapController.setCenter(new GeoPoint(
            boundaries[1] - (latitudeSpan / 2), boundaries[0] + (longitudeSpan / 2)
            ));
      mapController.zoomToSpan(latitudeSpan, longitudeSpan);
    }


    List<Overlay> mapOverlays = mapView.getOverlays();

    myLocationOverlay = new MyLocationOverlay(this, mapView);
    mapOverlays.add(myLocationOverlay);
    myLocationOverlay.runOnFirstFix(new Runnable() {
      public void run() {
        // Test with 'geo fix 172.641437 -43.534675' = Twisted Hop
        lastFix = myLocationOverlay.getMyLocation();
      }
    });

    // The paint style for the stop name text
    platformTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    platformTextPaint.setARGB(255,64,64,64);
    platformTextPaint.setTextAlign(Paint.Align.CENTER);
    platformTextPaint.setTextSize(10);
    platformTextPaint.setTypeface(Typeface.DEFAULT_BOLD);

    Drawable drawable = this.getResources().getDrawable(R.drawable.stop_marker);
    MetroMapOverlay overlay = new MetroMapOverlay(drawable, getApplicationContext());
    mapOverlays.add(overlay);
  }

  @Override
  public void onStop() {
    super.onStop();
    SharedPreferences preferences = getSharedPreferences(PlatformActivity.PREFERENCES_FILE, 0);
    SharedPreferences.Editor editor = preferences.edit();

    editor.putInt("lastLatitude", mapView.getMapCenter().getLatitudeE6());
    editor.putInt("lastLongitude", mapView.getMapCenter().getLongitudeE6());
    editor.putInt("lastZoom", mapView.getZoomLevel());

    editor.commit();
  }

  @Override
  public void onResume() {
    super.onResume();
    myLocationOverlay.enableMyLocation();
  }

  @Override
  public void onPause() {
    super.onPause();
    myLocationOverlay.disableMyLocation();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.map_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Intent intent;
    switch (item.getItemId()) {
      case R.id.my_location:
        Log.d(TAG, "My location selected");
        if (lastFix != null) {
          mapController.animateTo(lastFix);
        }
        return true;
      case R.id.exchange:
        Log.d(TAG, "Bus exchange selected");
        mapController.animateTo(exchangeGeoPoint);
        return true;
      case R.id.favourite_stops:
        Log.d(TAG, "Favourite stops selected");
        intent = new Intent();
        intent.setClassName("nz.co.wholemeal.christchurchmetro",
            "nz.co.wholemeal.christchurchmetro.FavouritesActivity");
        startActivity(intent);
        return true;
      case R.id.routes:
        Log.d(TAG, "Routes selected");
        intent = new Intent();
        intent.setClassName("nz.co.wholemeal.christchurchmetro",
            "nz.co.wholemeal.christchurchmetro.RoutesActivity");
        startActivity(intent);
        return true;
      case R.id.search:
        Log.d(TAG, "Search selected from menu");
        onSearchRequested();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  public class MetroMapOverlay extends Overlay {

    public static final String TAG = "MetroMapOverlay";
    private Context context;
    private Drawable marker;
    private GeoPoint currentTopLeft;
    private GeoPoint currentBottomRight;
    private ArrayList<Stop> stops;
    private Stop selectedStop;
    private View popUp;
    private View routeInfoBox;
    private GestureDetector gestureDetector;

    public MetroMapOverlay(Drawable defaultMarker, Context lcontext) {
      marker = defaultMarker;
      context = lcontext;

      //gestureDetector = new GestureDetector(new MetroMapGestureDetector());
      gestureDetector = new GestureDetector(new SimpleOnGestureListener() {
        @Override
        public boolean onDoubleTapEvent(MotionEvent event) {
          return true;
        }
      });
    }

    public void draw(Canvas canvas, MapView mapView, boolean shadow) {

      int minZoom = (routeTag == null ? 15 : 11);
      if (mapView.getZoomLevel() < minZoom || shadow) {
        return;
      }

      Projection projection = mapView.getProjection();

      int mapViewHeight = mapView.getHeight();
      int mapViewWidth = mapView.getWidth();

      GeoPoint topLeft = projection.fromPixels(0, 0);
      GeoPoint bottomRight = projection.fromPixels(mapViewWidth, mapViewHeight);

      Log.d(TAG, "topLeft coords = " + topLeft.toString());
      Log.d(TAG, "bottomRight coords = " + bottomRight.toString());

      if (mapBoundsChanged(mapView)) {
        if (routeTag == null) {
          stops = Stop.getAllWithinBounds(context, topLeft, bottomRight);
        } else {
          stops = Stop.getAllWithinBounds(context, topLeft, bottomRight,
              routeTag);
        }
      }

      if (stops != null) {
        Iterator iterator = stops.iterator();

        while (iterator.hasNext()) {
          Stop stop = (Stop)iterator.next();
          Point point = projection.toPixels(stop.getGeoPoint(), null);
          Bitmap bitmap = ((BitmapDrawable)marker).getBitmap();
          canvas.drawBitmap(bitmap, point.x - (bitmap.getWidth() / 2),
              point.y - bitmap.getHeight(), null);

          // If zoomed in enough, draw the stop names above the icons
          if (mapView.getZoomLevel() > 16) {
            canvas.drawText(stop.name, point.x, point.y - bitmap.getHeight(),
                platformTextPaint);
          }
        }
      }

      if (routeName != null) {
        if (routeInfoBox == null) {
          LayoutInflater inflater = (LayoutInflater)context.getSystemService(
              Context.LAYOUT_INFLATER_SERVICE
          );

          routeInfoBox = inflater.inflate(R.layout.map_route_info_box,null);
          mapView.addView(routeInfoBox);
          MapView.LayoutParams layoutParams = new MapView.LayoutParams(
              ViewGroup.LayoutParams.WRAP_CONTENT,
              ViewGroup.LayoutParams.WRAP_CONTENT,
              mapView.getWidth() / 2, 10,
              MapView.LayoutParams.TOP | MapView.LayoutParams.CENTER_HORIZONTAL);

          mapView.updateViewLayout(routeInfoBox, layoutParams);
          ((TextView)routeInfoBox).setText(routeName);
        }
      }
    }

    public boolean mapBoundsChanged(MapView mapView) {
      Projection projection = mapView.getProjection();
      int mapViewHeight = mapView.getHeight();
      int mapViewWidth = mapView.getWidth();
      GeoPoint topLeft = projection.fromPixels(0, 0);
      GeoPoint bottomRight = projection.fromPixels(mapViewHeight, mapViewWidth);

      if (currentTopLeft == null || currentBottomRight == null) {
        currentTopLeft = topLeft;
        currentBottomRight = bottomRight;
        return true;
      }

      if (geoPointsEqual(topLeft, currentTopLeft) &&
          geoPointsEqual(bottomRight, currentBottomRight)) {
        Log.d(TAG, "mapView bounds unchanged");
        return false;
      } else {
        currentTopLeft = topLeft;
        currentBottomRight = bottomRight;

        Log.d(TAG, "mapView bounds changed");
        return true;
      }
    }

    public boolean geoPointsEqual(GeoPoint a, GeoPoint b) {
      if (a.getLatitudeE6() == b.getLatitudeE6() && a.getLongitudeE6() == b.getLongitudeE6()) {
        return true;
      } else {
        return false;
      }
    }

    public boolean onTap(GeoPoint point, MapView mapView) {
      boolean removePriorPopup = selectedStop != null;

      Log.d(TAG, "Got tap event at " + point.getLatitudeE6() + "/" + point.getLongitudeE6());

      selectedStop = getHitLocation(point);

      if (selectedStop != null) {

        mapController.animateTo(selectedStop.getGeoPoint());

        LayoutInflater inflater = (LayoutInflater)context.getSystemService(
            Context.LAYOUT_INFLATER_SERVICE
        );

        if (popUp == null) {
          popUp = inflater.inflate(R.layout.map_popup,null);
          mapView.addView(popUp);
        }
        ((TextView)popUp.findViewById(R.id.map_popup_platform_number)).setText(selectedStop.platformNumber);
        ((TextView)popUp.findViewById(R.id.map_popup_platform_name)).setText(selectedStop.name);
        Bitmap bitmap = ((BitmapDrawable)marker).getBitmap();
        MapView.LayoutParams layoutParams = new MapView.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            selectedStop.getGeoPoint(),
            0, -(bitmap.getHeight()),
            MapView.LayoutParams.BOTTOM_CENTER);
        Button goButton = (Button)popUp.findViewById(R.id.map_popup_go);
        goButton.setOnClickListener(new OnClickListener() {
          public void onClick(View v) {
            Intent intent = new Intent();
            intent.putExtra("platformTag", selectedStop.platformTag);
            intent.setClassName("nz.co.wholemeal.christchurchmetro", "nz.co.wholemeal.christchurchmetro.PlatformActivity");
            startActivity(intent);
          }
        });

        mapView.updateViewLayout(popUp, layoutParams);
      }

      if (removePriorPopup && selectedStop == null) {
        mapView.removeView(popUp);
        popUp = null;
      }

      if (removePriorPopup || selectedStop != null) {
        mapView.invalidate();
      }

      return selectedStop != null;
    }

    public boolean onTouchEvent(MotionEvent event, MapView mapView) {
      if (gestureDetector.onTouchEvent(event)) {
        Log.d(TAG, "Got doubletap");
        mapController.zoomInFixing((int)event.getX(), (int)event.getY());
        return true;
      }
      return false;
    }

    public Stop getHitLocation(GeoPoint point) {
      Stop hitStop = null;

      if (stops == null) {
        return null;
      }

      Iterator<Stop> iterator = stops.iterator();

      while (iterator.hasNext()) {
        Stop stop = iterator.next();

        if (checkStopWithinTapRange(stop, point)) {
          Log.d(TAG, "Got hit on platformTag " + stop.platformTag);
          hitStop = stop;
          break;
        }
      }

      return hitStop;
    }

    public boolean checkStopWithinTapRange(Stop stop, GeoPoint point) {
      Bitmap bitmap = ((BitmapDrawable)marker).getBitmap();

      // Convert the clicked point to a pixel location on the MapView
      Projection projection = mapView.getProjection();
      Point comparePoint = new Point();
      projection.toPixels(point, comparePoint);

      /**
       * Get a rectangle describing the map marker boundaries in screen pixels.
       * The anchor point of the map marker is bottom center.
       */
      Point stopPoint = new Point();
      projection.toPixels(stop.getGeoPoint(), stopPoint);
      Rect markerRect = new Rect(
          stopPoint.x - (bitmap.getWidth() / 2),
          stopPoint.y - bitmap.getHeight(),
          stopPoint.x + (bitmap.getWidth() / 2),
          stopPoint.y);

      Log.d(TAG, "tap point (x/y) = " + comparePoint.x + "/" + comparePoint.y);
      Log.d(TAG, "marker rect (l/t/r/b) = " + markerRect.left + "/" +
          markerRect.top + "/" + markerRect.right + "/" + markerRect.bottom);
      return markerRect.contains(comparePoint.x, comparePoint.y);
    }
  }

  private class MetroMapGestureDetector extends SimpleOnGestureListener {
    private static final String TAG = "MetroMapGestureDetector";
    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
      return true;
    }
  }
}
