package apps.eagle.security;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;



/**
 * Created by ashrafiqubal on 12/01/17.
 */

public class LogIn extends AppCompatActivity {
    private static final String TAG="Main Activity";

    private ProgressDialog pDialog;

    String serverResponse=null;
    EditText customerUserName ,customerPassword;

    SharedPreferences sharedpreferences;
    String customerUserNaneString,customerPasswordString;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.log_in);
        customerUserName = (EditText)findViewById(R.id.customer_user_name);
        customerPassword = (EditText)findViewById(R.id.customer_password);
        Log.d(TAG, "Line Executed");
    }

    @Override
    public void onBackPressed(){
        finish();
        Log.d("CDA", "onBackPressed Called");
    }

    public void forgotPassword(View view){
        //Toast.makeText(getApplicationContext(), "Please call our customer care to reset your password", Toast.LENGTH_SHORT).show();
        TextView ifForgotPass = (TextView) findViewById(R.id.ifforgotpass);
        ifForgotPass.setText("Please call our customer support to reset password\nCustomer Care : 8888888888");

    }

    public void logIn(View view){
        customerUserNaneString = customerUserName.getText().toString();
        if(customerUserName.length()<4){
            Toast.makeText(getApplicationContext(),"Enter a valid Username",Toast.LENGTH_SHORT).show();
            return;
        }
        customerPasswordString = customerPassword.getText().toString();
        if(customerPasswordString.length()<4){
            Toast.makeText(getApplicationContext(),"Enter a valid Password",Toast.LENGTH_SHORT).show();
            return;
        }
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("http")
                .authority(MainActivity.SERVERIPADDRESS)
                .appendPath("binbasket")
                .appendPath("customerLogin.jsp")
                .appendQueryParameter("USERNAME", customerUserNaneString)
                .appendQueryParameter("PASSWORD",customerPasswordString);
        String myUrl = builder.build().toString();
        myUrl = myUrl.replace("%3A", ":");
        Log.d("URL://",myUrl);
        ProcessRequest processRequest = new ProcessRequest();
        processRequest.execute(myUrl);
    }

    public class ProcessRequest extends AsyncTask<String, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //System.out.println("Starting download");
            pDialog = new ProgressDialog(LogIn.this);
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
                Toast.makeText(getApplicationContext(),"Log in Successfully",Toast.LENGTH_SHORT).show();
                sharedpreferences = getSharedPreferences(MainActivity.SHAREDPREFERENCES, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putBoolean(MainActivity.ISNEWUSER, false);
                editor.putString(MainActivity.USERMOBILENO, customerUserNaneString);
                editor.putString(MainActivity.USERPASS, customerPasswordString);
                editor.commit();
                Intent mainActivity = new Intent(LogIn.this,MainActivity.class);
                startActivity(mainActivity);
                finish();
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
}
