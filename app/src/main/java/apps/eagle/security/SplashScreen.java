package apps.eagle.security;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

/**
 * Created by ashrafiqubal on 06/01/17.
 */

public class SplashScreen extends AppCompatActivity {
    private static final String TAG = "SplashScreen";
    boolean hasPermissionToGps=false;
    private static final int REQUEST_LOCATION_ON = 112;
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;

    public static Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);
        context = SplashScreen.this;
        hasPermissionToGps = (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
        checkPlayServices();
    }
    @Override
    protected void onStart() {
        super.onStart();
        //Log.d(TAG, "onStart Called");
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Log.d(TAG, "onResume Called ");
        if(!hasPermissionToGps){
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_LOCATION_ON);
        }else{
            if(GPSchecker()){
                startMainActivity();
            }else {
                sendGPSSetting();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Log.d(TAG, "onPause Called");
    }
    @Override
    protected void onStop() {
        super.onStop();
        //Log.d(TAG, "onStop Called " );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            case REQUEST_LOCATION_ON: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //reload my activity with permission granted or use the features what required the permission
                    if(GPSchecker()){
                        startMainActivity();
                    }else {
                        sendGPSSetting();
                    }
                } else {
                    Toast.makeText(this, "Please allow this device to access location", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
            break;
        }
    }

    private void sendGPSSetting(){
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setCancelable(false);
        dialog.setMessage("GPS not enabled");
        dialog.setPositiveButton("Enable Now", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                // TODO Auto-generated method stub
                Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                context.startActivity(myIntent);
                //get gps
            }
        });
        dialog.setNegativeButton("Close", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                // TODO Auto-generated method stub
                finish();

            }
        });
        dialog.show();
    }
    private boolean GPSchecker(){
        //Log.d(TAG, "GPSchecker Called " );
        LocationManager lm = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}
        if( !gps_enabled && !network_enabled){
            return false;
        }else {
            return true;
        }
    }
    private void startMainActivity(){
        Intent intent = new Intent(SplashScreen.this,MainActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Method to verify google play services on the device
     * */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }
}

