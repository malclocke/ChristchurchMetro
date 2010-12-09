package nz.co.wholemeal.christchurchmetro;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.webkit.WebView;
import android.widget.Toast;
import android.view.View;
import android.text.Html;
import android.text.Spanned;
import android.view.View.OnClickListener;
import android.util.Log;

import nz.co.wholemeal.christchurchmetro.R;
import nz.co.wholemeal.christchurchmetro.Stop;

public class ChristchurchMetroActivity extends Activity
{
  private WebView info;
  private EditText entry;
  static final int CHOOSE_FAVOURITE = 0;
  static final String TAG = "ChristchurchMetroActivity";

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.stop);

    info = (WebView)findViewById(R.id.info);
    entry = (EditText)findViewById(R.id.entry);

    final Button go_button = (Button)findViewById(R.id.go);
    go_button.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        Stop stop = new Stop(entry.getText().toString());
        loadStop(stop);
      }
    });

    final Button faves_button = (Button)findViewById(R.id.faves);
    faves_button.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        Intent intent = new Intent(ChristchurchMetroActivity.this,
          FavouritesActivity.class);
        ChristchurchMetroActivity.this.startActivityForResult(intent, CHOOSE_FAVOURITE);
      }
    });
  }

  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    Log.d(TAG, "Activity returned resultCode = " + resultCode);
    switch (requestCode) {
      case CHOOSE_FAVOURITE:
        if (resultCode != RESULT_CANCELED) {
          Bundle extras = data.getExtras();
          if (extras != null) {
            Log.d(TAG, "stop " + extras.getString("platformNumber") + " selected");
            Stop stop = new Stop(extras.getString("platformNumber"));
            entry.setText(extras.getString("platformNumber"));
            loadStop(stop);
          }
        }

      default:
        break;
    }
  }

  public void loadStop(Stop stop) {
    info.loadUrl(stop.getEtaUrl(10));
  }

  public void loadStop(String platformNumber) {
    loadStop(new Stop(platformNumber));
  }
}

