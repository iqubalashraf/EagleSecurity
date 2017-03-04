package apps.eagle.security;

import android.*;
import android.app.AlarmManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.AbsoluteLayout;
import android.widget.AnalogClock;
import android.widget.Button;
import android.widget.DigitalClock;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG="Main Activity";

    String serverResponse=null;

    private Location mLastLocation;
    double latitude,longitude;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private static AlarmManager alarmManager;

    private static int UPDATE_INTERVAL = 1;
    private static int FATEST_INTERVAL = 1; // 2 sec
    private static int DISPLACEMENT = 100; // 100 meters

    static Vibrator vibrator;

    private static final int MIN_TIME_BW_UPDATES = 60;

    SharedPreferences sharedpreferences;

    public static final String SERVERIPADDRESS = "54.169.86.227:8080";
    public final static String ISNEWUSER = "apps.eagle.security.isnewuser";
    public final static String SHAREDPREFERENCES = "apps.eagle.security.sharedpreferences";
    public final static String USERMOBILENO = "apps.eagle.security.sharedpreferences.mobileno";
    public final static String USERPASS = "apps.eagle.security.sharedpreferences.userpass";

    public static String customerPasswordString;
    public static String customerUsernameString;

    boolean isNewUser = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedpreferences = getSharedPreferences(SHAREDPREFERENCES, Context.MODE_PRIVATE);
        isNewUser = sharedpreferences.getBoolean(ISNEWUSER,true);
        customerPasswordString = sharedpreferences.getString(MainActivity.USERPASS, null);
        customerUsernameString = sharedpreferences.getString(MainActivity.USERMOBILENO, null);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if(isNewUser){
            Intent logInIntent = new Intent(MainActivity.this,LogIn.class);
            startActivity(logInIntent);
            finish();
        }else {
            //AnalogClock analog = (AnalogClock) findViewById(R.id.analog_clock);
            DigitalClock digital = (DigitalClock) findViewById(R.id.digital_clock);
            //digital clock
        }

    }

    public void sendReport(View view){
        vibrate(50);
    if(AppStatus.getInstance(getApplicationContext()).isOnline()){
        if(GPSchecker()){
            buildGoogleApiClient();
            connectToGoogleAPI();
            createLocationRequest();
        }else {
            sendGPSSetting();
        }
    }else {
        Toast.makeText(getApplicationContext(),"No Internet Connection",Toast.LENGTH_SHORT).show();
    }

    }

    /**
     * Method to display the location on UI
     * */
    private void getLocation() {
        if((ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)){
            mLastLocation = LocationServices.FusedLocationApi
                    .getLastLocation(mGoogleApiClient);
            //Log.d(TAG,"getLocation called, Location Reveived");
            if (mLastLocation != null) {
                latitude = mLastLocation.getLatitude();
                longitude = mLastLocation.getLongitude();
                Log.d(TAG,"getLocation called, LatLong Set "+latitude+"  "+longitude);
                Toast.makeText(getApplicationContext(),latitude+"  "+longitude,Toast.LENGTH_SHORT).show();
                Uri.Builder builder = new Uri.Builder();
                builder.scheme("http")
                        .authority(MainActivity.SERVERIPADDRESS)
                        .appendPath("binbasket")
                        .appendPath("imawake.jsp")
                        .appendQueryParameter("USERNAME", customerUsernameString)
                        .appendQueryParameter("PASSWORD",customerPasswordString)
                        .appendQueryParameter("LATITUDE",Double.toString(latitude))
                        .appendQueryParameter("LONGITUDE",Double.toString(longitude));
                String myUrl = builder.build().toString();
                myUrl = myUrl.replace("%3A", ":");
                Log.d("URL://",myUrl);
                ProcessRequest processRequest = new ProcessRequest();
                processRequest.execute(myUrl);
            } else {
                Toast.makeText(getApplicationContext(),"Unable to fetch location. Try again",Toast.LENGTH_LONG).show();
            }
        }else {
            Toast.makeText(getApplicationContext(),"Please close and restart the app",Toast.LENGTH_LONG).show();
        }
    }

    private boolean GPSchecker(){
        //Log.d(TAG, "GPSchecker Called " );
        LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
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
    /**
     * Creating google api client object
     * */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    private void connectToGoogleAPI(){
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }
    private void disconnectToGoogleAPI(){
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
            //Log.d(TAG, "disconnectToGoogleAPI  "+mGoogleApiClient.isConnected());
        }
    }
    /**
     * Google api callback methods
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }
    @Override
    public void onConnected(Bundle arg0) {
        // Once connected with google api, get the location
        //Log.d(TAG,"onConnected Called");
        //Log.d(TAG, "onConnected Called "+mGoogleApiClient.isConnected());
        //Log.d(TAG, "GoogleApiClient Connected  " +mGoogleApiClient.isConnected());
        startLocationUpdates();
        delaySec(1);
    }
    @Override
    public void onConnectionSuspended(int arg0) {
        Log.d(TAG,"OnConnectionSuspended Called");
        mGoogleApiClient.connect();
    }
    @Override
    public void onLocationChanged(Location location) {
        // Assign the new location
        mLastLocation = location;
        Log.d(TAG,"onLocationChanged Called");
    }
    /**
     * Creating location request object
     * */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
        //Log.d(TAG,"createLocationRequest called successfully" );

    }
    /**
     * Starting the location updates
     * */
    protected void startLocationUpdates() {
        if(ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            try{
                LocationServices.FusedLocationApi.requestLocationUpdates(
                        mGoogleApiClient, mLocationRequest, this);
                //Log.d(TAG,"startLocationUpdates called succesfully" );
            }catch (Exception e){
                Log.d(TAG,"Error:1 "+e.getMessage());
            }
        }else {
            //Toast.makeText(MainActivity.context,"Please close and restart the app",Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Stopping location updates
     */
    protected void stopLocationUpdates() {
        try{
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
            //Log.d(TAG,"StopLocationUpdates Called");
        }catch (Exception e){
            Log.d(TAG,"Error:2 "+e.getMessage());
        }

    }
    public void delaySec(int sec){
        new CountDownTimer(1000*sec, 1000*sec) {

            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                //Log.d(TAG, "delaySec GoogleApiClient Connected  " +mGoogleApiClient.isConnected());
                if (mGoogleApiClient.isConnected()) {
                    getLocation();
                    stopLocationUpdates();
                    disconnectToGoogleAPI();
                }
            }

        }.start();
    }
    private void sendGPSSetting(){
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setCancelable(false);
        dialog.setMessage("GPS not enabled");
        dialog.setPositiveButton("Enable Now", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                // TODO Auto-generated method stub
                Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(myIntent);
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

    public class ProcessRequest extends AsyncTask<String, Void, Boolean> {
        private ProgressDialog pDialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //System.out.println("Starting download");
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }
        @Override
        protected Boolean doInBackground(String... params) {
            // TODO Auto-generated method stub
            Boolean prepared;
            try {
                String str;
                HttpClient myClient = new DefaultHttpClient();
                HttpGet get = new HttpGet(params[0]);
                HttpResponse myResponse = myClient.execute(get);
                BufferedReader br = new BufferedReader(new InputStreamReader(myResponse.getEntity().getContent()));
                while ((str = br.readLine()) != null) {
                    serverResponse = str;
                    Log.d(TAG, str);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            prepared = true;
            return prepared;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            pDialog.dismiss();
            Log.d(TAG, "onPostExecution ");
            Object obj = JSONValue.parse(serverResponse);
            JSONObject jsonObject = (JSONObject)obj;
            String status = (String)jsonObject.get("STATUS");
            if(status.equals("0")){
                Log.d(TAG,"Login Successful");
                Toast.makeText(getApplicationContext(),"Sent Successfully",Toast.LENGTH_SHORT).show();
                chngButtonPosition();
            }
            if(status.equals("1")){
                Toast.makeText(getApplicationContext(),"Phone no not exists",Toast.LENGTH_SHORT).show();
            }
            if(status.equals("2")){
                Log.d(TAG,"Registration Successful");
                Toast.makeText(getApplicationContext(),"Password Mismatch",Toast.LENGTH_SHORT).show();
            }
            if(status.equals("3")){
                Log.d(TAG,"Registration Successful");
                Toast.makeText(getApplicationContext(),"Something went wrong, Try again !",Toast.LENGTH_SHORT).show();
            }
            if(status.equals("4")){
                Log.d(TAG,"Registration Successful");
                Toast.makeText(getApplicationContext(),"Something went wrong, Try again !",Toast.LENGTH_SHORT).show();
            }
        }
    }
    public void takeScreenshot(View view) {
        vibrate(50);
        Date now = new Date();
        android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);

        try {
            // image naming and path  to include sd card  appending name you choose for file
            String mPathTemp = Environment.getExternalStorageDirectory().toString() + "/EagalSecurity";
            File folder = new File(mPathTemp);
            folder.mkdir();
            if(!folder.exists()){
                folder.mkdir();
                Log.d(TAG,"Successful");
            }

            String mPath = mPathTemp + "/EagalSecurity" + now + ".jpg";
            mPath = mPath.replace(" ","_");
            // create bitmap screen capture
            View v1 = getWindow().getDecorView().getRootView();
            v1.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
            v1.setDrawingCacheEnabled(false);

            File imageFile = new File(mPath);

            FileOutputStream outputStream = new FileOutputStream(imageFile);
            int quality = 100;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            outputStream.flush();
            outputStream.close();
            Toast.makeText(getApplicationContext(),"Screenshot Saved",Toast.LENGTH_SHORT).show();
            //openScreenshot(imageFile);
        } catch (Throwable e) {
            // Several error may come out with file handling or OOM
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),"Error: "+e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }

    private void openScreenshot(File imageFile) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        Uri uri = Uri.fromFile(imageFile);
        intent.setDataAndType(uri, "image/*");
        startActivity(intent);
    }

    private void chngButtonPosition(){
        try{
            Button button = (Button)findViewById(R.id.button_awake);
            AbsoluteLayout.LayoutParams absParams =
                    (AbsoluteLayout.LayoutParams)button.getLayoutParams();

            DisplayMetrics displaymetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            int width = displaymetrics.widthPixels;
            int height = displaymetrics.heightPixels;

            width = ((width*3)/5);
            height = ((height*3)/5);
            Random r = new Random();


            absParams.x =  r.nextInt(width ) ;
            absParams.y =  r.nextInt(height);

            Log.d(TAG,width+"  "+height+"  "+absParams.x+"  "+absParams.y);

            button.setLayoutParams(absParams);
        }catch (Exception e){
            Log.d(TAG,"Error: "+e.getMessage());
        }
    }

    public static void vibrate(int milliSeconds){
        vibrator.vibrate(milliSeconds);
    }
}
