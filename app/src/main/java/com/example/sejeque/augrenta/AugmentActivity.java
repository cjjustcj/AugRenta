package com.example.sejeque.augrenta;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.wikitude.architect.ArchitectJavaScriptInterfaceListener;
import com.wikitude.architect.ArchitectStartupConfiguration;
import com.wikitude.architect.ArchitectView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;

import static com.google.firebase.auth.FirebaseAuth.getInstance;

/**
 * Created by Faith on 08/04/2018.
 */

public class AugmentActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, SensorEventListener, ArchitectJavaScriptInterfaceListener {
    final static int PERMISSION_ALL = 1;
    final static String[] PERMISSIONS = {android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION};
    LocationManager locationManager;
    private ArchitectView architectView;
    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    String propertyId;
    private DatabaseReference mDatabase;
    private DatabaseReference statusDatabase;
    private DatabaseReference notificationRef;
    private DatabaseReference userTrackingDB;
    private DatabaseReference locationDB;
    Double propertyLat;
    Double propertyLng;
    Double countChange = 0.0;

    private float startRotation = 0;
    private float newRotation = 0;

    Location baseLocation = null;
    Location prevLocation;

    int mAzimuth;
    int rotation;
    private SensorManager mSensorManager;
    private Sensor mRotationV, mAccelerometer, mMagnetometer;
    boolean haveSensor = false, haveSensor2 = false;
    float[] rMat = new float[9];
    float[] orientation = new float[3];
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private boolean hasSensor = true;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    String ownerId;

    String userStatus;
    private boolean isUserTracking = false;

    String acceptedStatus = "false";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ar_view);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        this.architectView = (ArchitectView) this.findViewById(R.id.architectView);
        final ArchitectStartupConfiguration config = new ArchitectStartupConfiguration();
        config.setLicenseKey("Ey14mdrn+mDHTFgnUTl74BPegIFwI4EzXeTSQni4J0SL+rBT0XU6QQOC6s+yFyXFLWEg7qd05XXvdeJbHD/yf+V84XM1UDFb5cDc6jCF0EUzr7Q8/xH/61RhbuuXDzgCbJyIHZAhnDYU6FksrHoAFeyKnztZSX6DdQSywB2yQjxTYWx0ZWRfX6sdVpVO8uCJZ6rumx7yrv8Eh52iZRczHgirrh2UsS0ogYbw1+PGiyxQBkBfhVIs6dSvR9gRJC+ZgcN/YCGAhaR4A40wQVMLgA0tTSzjFyS4iatBI35OQdedsevW3P5BZcX9klCvDNcZrb+LXnbWZvFPnyt71kAdszGEV4Zvn1P0twcjRk5mYlAZnPGbkbjsixn7dbuaS6wdcSuBIQo2ZqrzwuYgZWs8V8aINmnK1hEUnpQnrDE1LsA9vgYN8UZxW0Ul9NWzF98M6SjkEojfFqMNiTDVUMqiqrvkogmv1gSz2JKOtZaokj+VMd9Embe9F8LGz2cc2N77GRsBgLjNlQ1Nzt3KwQKjm7M7u4hXlYBMtGjQI1TJua4r8xeh+BshF/I04VZt/xjxGzQ9OgOysedXsCv0GLMnqVzSLHZpoueYSVlcgTp/xIli7gHrzeDPgzut7GqPAu9T1ANpVZGzlFAzgCqkpRvPYWNYnI69BO6GTrpbAYGQ9O0=");
        architectView.onCreate(config);
        architectView.addArchitectJavaScriptInterfaceListener(this);

        propertyId = getIntent().getExtras().getString("propertyId");

        //get reference from firebase database with child node Property
        statusDatabase = FirebaseDatabase.getInstance().getReference("UserStatus");
        mDatabase = FirebaseDatabase.getInstance().getReference("Property");
        userTrackingDB = FirebaseDatabase.getInstance().getReference("UserTracking");
        locationDB = FirebaseDatabase.getInstance().getReference("Location");
        notificationRef = FirebaseDatabase.getInstance().getReference("Notifications");
        notificationRef.keepSynced(true);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        checkSensors();

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 23 && !isPermissionGranted()) {
            requestPermissions(PERMISSIONS, PERMISSION_ALL);
        } else requestLocation();
        if (!isLocationEnabled())
            showAlert(1);

//        setToUserLocation();
        //instantiate firebase auth
        mAuth = getInstance();
        //retrieve user information and store to currentUser
        currentUser = mAuth.getCurrentUser();
    }

    public void checkSensors() {
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) == null) {
            if ((mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) || (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null)) {
                noSensorsAlert();
            }
            else {
                mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
                haveSensor = mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                haveSensor2 = mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
        else{
            mRotationV = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            haveSensor = mSensorManager.registerListener(this, mRotationV, SensorManager.SENSOR_DELAY_UI);
        }
    }

    public void noSensorsAlert(){
        hasSensor =false;
    }

    public void stop() {
        if(haveSensor && haveSensor2){
            mSensorManager.unregisterListener(this,mAccelerometer);
            mSensorManager.unregisterListener(this,mMagnetometer);
        }
        else{
            if(haveSensor)
                mSensorManager.unregisterListener(this,mRotationV);
        }
    }

    private void setToUserLocation() {
        mGoogleApiClient = new GoogleApiClient.Builder(AugmentActivity.this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(AugmentActivity.this)
                .addOnConnectionFailedListener(AugmentActivity.this)
                .build();
        mGoogleApiClient.connect();
    }

    protected void onStart() {
        super.onStart();

        mDatabase.child(propertyId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Property property = dataSnapshot.getValue(Property.class);
                propertyLat = Double.valueOf(property.latitude);
                propertyLng = Double.valueOf(property.longitude);
                ownerId = property.owner;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        if(propertyId != null && ownerId != null){
            locationDB.child(propertyId).child(ownerId).child("accepted").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()){
                        acceptedStatus = dataSnapshot.getValue().toString();
                        architectView.callJavascript("loadAcceptedValueFromJava("+ acceptedStatus +");");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.architectView.onPause();
        stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.architectView.onResume();
        checkSensors();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.architectView.onDestroy();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
//
        this.architectView.onPostCreate();
        try {
            this.architectView.load("Direction_AR/index.html");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    double lat;
    double lng;

    @Override
    public void onLocationChanged(Location location) {
        countChange += 1;

        lat = location.getLatitude();
        lng = location.getLongitude();
        double alti = location.getAltitude();

        if (architectView != null) {
            // check if location has altitude at certain accuracy level & call right architect method (the one with altitude information)
            if (location.hasAltitude() && location.hasAccuracy() && location.getAccuracy() < 7) {
                architectView.setLocation(location.getLatitude(), location.getLongitude(), location.getAltitude(), location.getAccuracy());
            } else {
                architectView.setLocation(location.getLatitude(), location.getLongitude(), location.hasAccuracy() ? location.getAccuracy() : 1000);
            }
        }


        if(location == null){
            // doesn't work if gps is unabled
            Toast.makeText(this, "Can't get current location", Toast.LENGTH_SHORT).show();
        }
        else{
            if(isUserTracking){
                UserTracking userTracking = new UserTracking(currentUser.getUid(), currentUser.getDisplayName(), String.valueOf(lat), String.valueOf(lng));
                userTrackingDB.child(ownerId).setValue(userTracking);
            }
            architectView.callJavascript("loadValuesFromJava(" + alti + "," + lat + "," + lng + "," + propertyLat + "," + propertyLng + "," + countChange + ");");
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    private void requestLocation() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        String provider = locationManager.getBestProvider(criteria, true);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(provider, 5000, 1, this);
    }
    private boolean isLocationEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private boolean isPermissionGranted() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Log.v("mylog", "Permission is granted");
            return true;
        } else {
            Log.v("mylog", "Permission not granted");
            return false;
        }
    }
    private void showAlert(final int status) {
        String message, title, btnText;
        if (status == 1) {
            message = "Your Locations Settings is set to 'Off'.\nPlease Enable Location to " +
                    "use this app";
            title = "Enable Location";
            btnText = "Location Settings";
        } else {
            message = "Please allow this app to access location!";
            title = "Permission access";
            btnText = "Grant";
        }
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setCancelable(false);
        dialog.setTitle(title)
                .setMessage(message)
                .setPositiveButton(btnText, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        if (status == 1) {
                            Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(myIntent);
                        } else
                            ActivityCompat.requestPermissions(AugmentActivity.this, PERMISSIONS, PERMISSION_ALL);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        finish();
                    }
                });
        dialog.show();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rMat, event.values);
            mAzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + 360) % 360;
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(rMat, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(rMat, orientation);
            mAzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + 360) % 360;
        }

        mAzimuth = Math.round(mAzimuth);
        rotation = -mAzimuth;

        architectView.callJavascript("loadRotationFromJava(" + rotation + "," + hasSensor + ");");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onJSONObjectReceived(JSONObject jsonObject) {
        try {
            if(jsonObject.getString("status").equals("near")){
                HashMap<String, String> notificationData = new HashMap<>();

                notificationData.put("fromName", currentUser.getDisplayName());
                notificationData.put("fromID", currentUser.getUid());
                notificationData.put("type", "receiver");
                notificationData.put("response", "near");
                notificationData.put("propertyId", propertyId);

                notificationRef.child(ownerId).push().setValue(notificationData).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        {
                            if (task.isSuccessful()) {
                                Log.d("House Seeker", "is near");
                            }
                        }
                    }
                });

                isUserTracking = true;
            }
            if(jsonObject.getString("userLocation").equals("here")){
                UserStatus userStatus = new UserStatus("here", currentUser.getUid(), propertyId, ownerId);
                statusDatabase.child(currentUser.getUid()).setValue(userStatus);
                isUserTracking = false;
                userTrackingDB.child(ownerId).removeValue();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
