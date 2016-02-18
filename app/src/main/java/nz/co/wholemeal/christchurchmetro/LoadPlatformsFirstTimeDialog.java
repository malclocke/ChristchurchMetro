package nz.co.wholemeal.christchurchmetro;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class LoadPlatformsFirstTimeDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(true)
                .setTitle(R.string.route_update_required)
                .setMessage(R.string.do_you_want_to_load_bus_stop_and_route_data)
                .setPositiveButton(R.string.load_now, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        Intent intent = new Intent(getActivity(), LoadPlatformsService.class);
                        getActivity().startService(intent);
                    }
                })
                .setNegativeButton(R.string.do_it_later, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        return builder.create();
    }

}
