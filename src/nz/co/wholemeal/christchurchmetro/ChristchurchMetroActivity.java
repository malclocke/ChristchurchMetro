package nz.co.wholemeal.christchurchmetro;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.webkit.WebView;
import android.widget.Toast;
import android.view.View;
import android.text.Html;
import android.text.Spanned;
import android.view.View.OnClickListener;

import nz.co.wholemeal.christchurchmetro.Stop;

public class ChristchurchMetroActivity extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        final Button go_button = (Button)findViewById(R.id.go);
        go_button.setOnClickListener(new OnClickListener() {
          public void onClick(View v) {
            EditText entry = (EditText)findViewById(R.id.entry);
            WebView info = (WebView)findViewById(R.id.info);
            Stop stop = new Stop(entry.getText().toString());

            info.loadUrl(stop.getEtaUrl(10));
            //Spanned html = Html.fromHtml(stop.getEtaHtml(10));
            //info.setText(html);
          }
        });
    }
}

