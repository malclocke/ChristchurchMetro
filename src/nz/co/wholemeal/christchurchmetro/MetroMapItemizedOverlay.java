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
import android.graphics.drawable.Drawable;
import android.app.AlertDialog;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

import java.util.ArrayList;

public class MetroMapItemizedOverlay extends ItemizedOverlay {
  private ArrayList<OverlayItem> overlays = new ArrayList<OverlayItem>();
  private Context context;

  public MetroMapItemizedOverlay(Drawable defaultMarker) {
    super(boundCenterBottom(defaultMarker));
  }

  public MetroMapItemizedOverlay(Drawable defaultMarker, Context lcontext) {
    super(boundCenterBottom(defaultMarker));
    context = lcontext;
  }

  public void addOverlay(OverlayItem overlay) {
    overlays.add(overlay);
    populate();
  }

  @Override
  protected OverlayItem createItem(int i) {
    return overlays.get(i);
  }

  @Override
  public int size() {
    return overlays.size();
  }

  @Override
  protected boolean onTap(int index) {
    OverlayItem item = overlays.get(index);
    AlertDialog.Builder dialog = new AlertDialog.Builder(context);
    dialog.setTitle(item.getTitle());
    dialog.setMessage(item.getSnippet());
    dialog.show();
    return true;
  }
}
