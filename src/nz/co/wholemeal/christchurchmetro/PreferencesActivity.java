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

import android.app.ProgressDialog;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class PreferencesActivity extends PreferenceActivity implements LoadRoutesActivity {

  public static String TAG = "PreferencesActivity";
  // Values for maximum in the progress dialog.  Hopefully will not fluctuate
  // much over time.
  private static int MAX_PLATFORMS = 2600;
  private static int MAX_ROUTES = 125;

  private ProgressDialog loadingRoutesProgressDialog = null;
  private AsyncLoadPlatforms asyncLoadPlatforms = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.preferences);


    /* Update the data from the API */
    Preference updateRoutes = (Preference)findPreference("updateRoutes");
    updateRoutes.setOnPreferenceClickListener(new OnPreferenceClickListener() {

      public boolean onPreferenceClick(Preference preference) {
        new AsyncLoadPlatforms((LoadRoutesActivity)PreferencesActivity.this).execute();
        return true;
      }

    });

    /*
     * This will contain return non null if we received an orientation change
     */
    asyncLoadPlatforms = (AsyncLoadPlatforms)getLastNonConfigurationInstance();
    if (asyncLoadPlatforms != null) {
      Log.d(TAG, "Reinstating async task");
      initProgressDialog();
      asyncLoadPlatforms.attach(this);
    }
  }

  /*
   * This is used to handle rotation while the 'loading routes' dialog
   * is being displayed.
   */
  @Override
  public Object onRetainNonConfigurationInstance() {
    if (asyncLoadPlatforms != null) {
      asyncLoadPlatforms.detach();
    }
    if (loadingRoutesProgressDialog != null) {
      loadingRoutesProgressDialog.dismiss();
    }
    return asyncLoadPlatforms;
  }

  public void showLoadingRoutesProgressDialog() {
    initProgressDialog();
  }

  private void initProgressDialog() {
    loadingRoutesProgressDialog = new ProgressDialog(this);
    loadingRoutesProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    loadingRoutesProgressDialog.setCancelable(false);

    loadingRoutesProgressDialog.setMax(MAX_PLATFORMS);
    loadingRoutesProgressDialog.setMessage(getString(R.string.loading_platforms));
    loadingRoutesProgressDialog.show();
  }

  public void updateLoadingRoutesProgressDialog(int progress) {
    // This is a special value, and means the import mode has
    // progressed from platforms to patterns
    if (progress == -1) {
      loadingRoutesProgressDialog.setMax(MAX_ROUTES);
      loadingRoutesProgressDialog.setMessage(getString(R.string.loading_routes));
    } else {
      loadingRoutesProgressDialog.setProgress(progress);
    }
  }

  public void loadingRoutesComplete(String message) {
    loadingRoutesProgressDialog.dismiss();
    asyncLoadPlatforms = null;
    Toast.makeText(getBaseContext(), message,
        Toast.LENGTH_SHORT).show();
  }
}
