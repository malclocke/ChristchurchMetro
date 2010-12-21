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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
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


  public class MetroMapOverlay extends Overlay {

    public static final String TAG = "MetroMapOverlay";
    private Context context;
    private Drawable marker;
    private GeoPoint currentTopLeft;
    private GeoPoint currentBottomRight;
    private ArrayList<Stop> stops;
    private Stop selectedStop;
    private View popUp;

    public MetroMapOverlay(Drawable defaultMarker, Context lcontext) {
      marker = defaultMarker;
      context = lcontext;
    }

    public void draw(Canvas canvas, MapView mapView, boolean shadow) {

      if (mapView.getZoomLevel() < 15 || shadow) {
        return;
      }

      Projection projection = mapView.getProjection();

      int mapViewHeight = mapView.getHeight();
      int mapViewWidth = mapView.getWidth();

      GeoPoint topLeft = projection.fromPixels(0, 0);
      GeoPoint bottomRight = projection.fromPixels(mapViewHeight, mapViewWidth);

      Log.d(TAG, "topLeft coords = " + topLeft.toString());
      Log.d(TAG, "bottomRight coords = " + bottomRight.toString());

      if (mapBoundsChanged(mapView)) {
        stops = Stop.getAllWithinBounds(context, topLeft, bottomRight);
      }

      if (stops != null) {
        Iterator iterator = stops.iterator();

        while (iterator.hasNext()) {
          Stop stop = (Stop)iterator.next();
          Point point = projection.toPixels(stop.getGeoPoint(), null);
          Bitmap bitmap = ((BitmapDrawable)marker).getBitmap();
          canvas.drawBitmap(bitmap, point.x - (bitmap.getWidth() / 2),
              point.y - bitmap.getHeight(), null);
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
        MapView.LayoutParams layoutParams = new MapView.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            selectedStop.getGeoPoint(),
            MapView.LayoutParams.BOTTOM_CENTER);
        Button goButton = (Button)popUp.findViewById(R.id.map_popup_go);
        goButton.setOnClickListener(new OnClickListener() {
          public void onClick(View v) {
            Intent intent = new Intent();
            //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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

    public Stop getHitLocation(GeoPoint point) {
      Stop hitStop = null;
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
      int stopLatitude = stop.getGeoPoint().getLatitudeE6();
      int stopLongitude = stop.getGeoPoint().getLongitudeE6();
      int left = point.getLongitudeE6() - 100;
      int right = point.getLongitudeE6() + 100;
      int top = point.getLatitudeE6() + 100;
      int bottom = point.getLatitudeE6() - 100;
      Log.d(TAG, "left = " + left);
      Log.d(TAG, "right = " + right);
      Log.d(TAG, "top = " + top);
      Log.d(TAG, "bottom = " + bottom);
      Log.d(TAG, "stop lat = " + stopLatitude);
      Log.d(TAG, "stop lon = " + stopLongitude);
      return (left < stopLongitude && right > stopLongitude &&
              top > stopLatitude && bottom < stopLatitude);
    }
  }
}
