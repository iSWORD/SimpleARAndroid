/*
 *    Copyright 2016 Anand Muralidhar
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.anandmuralidhar.simplearandroid;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;

public class SimpleARActivity extends Activity {
    private final static Location BUILDING_LOCATION = createBuildingLocation();
    private GLSurfaceView mGLView = null;
    private CameraClass mCameraObject;
    private boolean appIsExiting = false;
    private GestureClass mGestureObject;
    private SensorClass mSensorObject;
    private FusedLocationProviderClient locationClient;
    private LocationCallback locationCallback = createLocationCallback();
    private SensorEventListener sensorListener = createSensorListener();

    private SensorEventListener createSensorListener() {
        return new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                Log.i("Sensor", Math.toDegrees(sensorEvent.values[0]) + " " + Math.toDegrees(sensorEvent.values[0]) + " " + Math.toDegrees(sensorEvent.values[0]));
//                float degrees[] = new float[3];
//                for (int i = 0; i < 3; i++) {
//                    degrees[i] = (float) Math.toDegrees(sensorEvent.values[i]);
//                }
                GyroscopeUpdated(sensorEvent.values);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
    }

    private SensorManager mSensorManager;
    private Sensor mSensor;

    private native void CreateObjectNative(AssetManager assetManager, String pathToInternalDir);

    private native void DeleteObjectNative();

    private native void SetCameraParamsNative(int previewWidth, int previewHeight, float cameraFOV);

    private native void LocationUpdate(float xDifference, float zDifference);

    private native void GyroscopeUpdated(float[] values);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeLocation();
        AssetManager assetManager = getAssets();
        String pathToInternalDir = getFilesDir().getAbsolutePath();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        mCameraObject = new CameraClass(this);
        if (!mCameraObject.IsResolutionSupported()) {
            ShowExitDialog(this, getString(R.string.exit_no_resolution));
            return;
        }

        // call the native constructors to create an object
        CreateObjectNative(assetManager, pathToInternalDir);
        SetCameraParamsNative(mCameraObject.GetPreviewWidth(), mCameraObject.GetPreviewHeight(),
                mCameraObject.GetFOV());

        // layout has only two components, a GLSurfaceView and a TextView
        setContentView(R.layout.simplear_layout);
        mGLView = (MyGLSurfaceView) findViewById(R.id.gl_surface_view);

        // mGestureObject will handle touch gestures on the screen
        mGestureObject = new GestureClass(this);
        mGLView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mGestureObject.mTapDetector.onTouchEvent(event);
                return true;
            }
        });

        mSensorObject = new SensorClass(this, mGLView);
        if (!mSensorObject.isSensorsAvailable()) {
            ShowExitDialog(this, getResources().getString(R.string.exit_no_sensor));
            appIsExiting = true;
        }
    }

    @Override
    protected void onResume() {

        super.onResume();

        startLocationUpdates();
        mSensorManager.registerListener(sensorListener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);

        if (appIsExiting) {
            return;
        }

        // Android suggests that we call onResume on GLSurfaceView
        if (mGLView != null) {
            mGLView.onResume();
        }

        if (!mSensorObject.RegisterSensors()) {
            ShowExitDialog(this, getResources().getString(R.string.exit_no_reg_sensor));
            appIsExiting = true;
            return;
        }

        // initialize the camera again in case activity was paused and resumed
        mCameraObject.InitializeCamera();
        mCameraObject.StartCamera();
    }

    @Override
    protected void onPause() {

        super.onPause();
        stopLocationUpdates();
        mSensorManager.unregisterListener(sensorListener);
        // Android suggests that we call onPause on GLSurfaceView
        if (mGLView != null) {
            mGLView.onPause();
        }

        mSensorObject.UnregisterSensors();

        mCameraObject.StopCamera();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        // We are exiting the activity, let's delete the native objects
        DeleteObjectNative();
    }

    public void ShowExitDialog(final Activity activity, String exitMessage) {
        appIsExiting = true;
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
        alertDialogBuilder.setMessage(exitMessage)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        activity.finish();
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void initializeLocation() {
        locationClient = new FusedLocationProviderClient(this);
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationClient.requestLocationUpdates(createLocationRequest(),
                locationCallback,
                null);
    }

    private void stopLocationUpdates() {
        locationClient.removeLocationUpdates(locationCallback);
    }

    private LocationCallback createLocationCallback() {
        return new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                locationReceived(locationResult.getLastLocation());
            }
        };
    }

    private void locationReceived(Location lastLocation) {
        float[] xDiff = new float[3];
        float[] zDiff = new float[3];
        Location.distanceBetween(lastLocation.getLatitude(), BUILDING_LOCATION.getLongitude(), BUILDING_LOCATION.getLatitude(), BUILDING_LOCATION.getLongitude(), zDiff);
        Location.distanceBetween(BUILDING_LOCATION.getLatitude(), lastLocation.getLongitude(), BUILDING_LOCATION.getLatitude(), BUILDING_LOCATION.getLongitude(), xDiff);
        LocationUpdate(xDiff[0], zDiff[0]);
    }

    protected LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(100);
        mLocationRequest.setFastestInterval(50);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    private static Location createBuildingLocation() {
        Location location = new Location("elo");
        location.setLongitude(18.551498);
        location.setLatitude(54.477291);
        return location;
    }

    /**
     * load libSimpleARNative.so since it has all the native functions
     */
    static {
        System.loadLibrary("SimpleARNative");
    }
}
