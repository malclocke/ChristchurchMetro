package nz.co.wholemeal.christchurchmetro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.Toast;

public class LoadPlatformsCompleteReceiver extends BroadcastReceiver {

    public static final String MESSAGE
        = "nz.co.wholemeal.christchurchmetro.LOAD_PLATFORMS_COMPLETE";

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        Resources res = context.getResources();
        String message = extras.getString(MESSAGE);

        if (message == null) {
            // Set a default message
            message = context.getString(R.string.loaded_route_information);
        }
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.show();
    }

}
