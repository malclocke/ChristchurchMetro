package nz.co.wholemeal.christchurchmetro;

import android.app.Activity;
import android.widget.TextView;
import android.os.Bundle;

public class MapActivity extends Activity {
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    TextView textview = new TextView(this);
    textview.setText("Coming soon ...");
    setContentView(textview);
  }
}
