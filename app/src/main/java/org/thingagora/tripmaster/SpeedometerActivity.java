package org.thingagora.tripmaster;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.TextView;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Locale;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class SpeedometerActivity extends AppCompatActivity implements LocationListener, SharedPreferences.OnSharedPreferenceChangeListener {

    // GPS Location update settings
    private int minTimeUpdateSeconds;
    private float minDistanceUpdateMeters;

    // GPS Location manager and current location
    LocationManager mLocationManager;
    Location mLocation;

    // Logging server
    private Socket mLoggerSocket;
    private String mLoggerUser = "xxx";
    private String mLoggerHost = "aaa.bbb.ccc";
    private int mLoggerPort = 12345;

    // Speedometer view settings
    private double speedErrMarginKph;
    private double speedErrFactor;
    // Multiply by 1 + factor up to threshold, then add margin
    private double speedErrThresholdKph;

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 5000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    public SpeedometerActivity() {
        minDistanceUpdateMeters = 0;
        minTimeUpdateSeconds = 1;
        speedErrMarginKph = 10;
        speedErrFactor = 0.2;
        speedErrThresholdKph = speedErrMarginKph / speedErrFactor;
    }

    public void updatePreferences(SharedPreferences prefs) {
        minDistanceUpdateMeters = 0;
        minTimeUpdateSeconds = Integer.valueOf(prefs.getString("location_frequency","1"));
        speedErrMarginKph = Float.valueOf(prefs.getString("err_max","10"));
        speedErrFactor = Float.valueOf(prefs.getString("err_percent","20")) / 100;
        speedErrThresholdKph = speedErrMarginKph / speedErrFactor;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String s) {
        updatePreferences(prefs);
        if (s.equals("location_frequency")) {
            restartTracking();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        Context appContext = getApplicationContext();

        // Manage preferences
        PreferenceManager.getDefaultSharedPreferences(appContext)
                .registerOnSharedPreferenceChangeListener(this);
        updatePreferences(PreferenceManager.getDefaultSharedPreferences(appContext));

        // Initialize location manager and current location
        int permissionCheck = ContextCompat.checkSelfPermission(appContext,"android.permission.ACCESS_FINE_LOCATION");
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            mLocationManager = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
            mLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            startTracking();
        }

        setContentView(R.layout.activity_speedometer);

        mVisible = true;
        mContentView = findViewById(R.id.speedometer_view);


        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon speedometer view, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        mContentView.setOnTouchListener(mDelayHideTouchListener);

        // Update time on start
        updateTime(Calendar.getInstance());

        // If location permissions are not granted, don't display speed
        if (permissionCheck != PackageManager.PERMISSION_GRANTED)
            ((TextView)findViewById(R.id.speed_text)).setText("");
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(500);
    }

    private boolean startTracking() {
        if (mLocationManager == null)
            return false;
        try {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    minTimeUpdateSeconds * 1000,
                    minDistanceUpdateMeters,
                    this);
        }
        catch (SecurityException se) {
            return false;
        }
        return true;
    }

    private boolean stopTracking() {
        if (mLocationManager == null)
            return false;
        try {
            mLocationManager.removeUpdates(this);
        }
        catch (SecurityException se) {
            return false;
        }
        return true;
    }

    private boolean restartTracking() {
        return stopTracking() && startTracking();
    }

    private boolean loggerConnect(String host, int port) {
        if (mLoggerSocket != null && mLoggerSocket.isConnected())
            return true;
        try {
            Log.i("TRIP CONNECT","Connecting");
            mLoggerSocket = new Socket(host, port);
        }
        catch (IOException e) {
            return false;
        }
        Log.i("TRIP CONNECT","Connected");
        return true;
    }

    private boolean loggerDisconnect() {
        if (mLoggerSocket == null)
            return false;
        try {
            mLoggerSocket.close();
        }
        catch (IOException e) {
            return false;
        }
        return true;
    }

    private boolean logLocation() {
        if (!loggerConnect(mLoggerHost, mLoggerPort))
            return false;
        String logString = String.format(Locale.US,"%s:%.04f,%.04f ",mLoggerUser,mLocation.getLatitude(),mLocation.getLongitude());
        Log.i("TRIP STRING",logString);
        byte[] logBytes = logString.getBytes(StandardCharsets.US_ASCII);
        OutputStream outputStream;
        try {
            outputStream = mLoggerSocket.getOutputStream();
            outputStream.write(logBytes);
        }
        catch (IOException e) {
            return false;
        }
        Log.i("TRIP WRITE","OK");
        return true;
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    public void updateTime(Calendar now) {
        String nowstr = String.format("%02d:%02d",now.get(Calendar.HOUR_OF_DAY),now.get(Calendar.MINUTE));
        ((TextView)findViewById(R.id.time_text)).setText(nowstr);
    }

    public void updateSpeedometer(int speed) {
        ((TextView)findViewById(R.id.speed_text)).setText(Integer.valueOf(speed).toString());
    }


    @Override
    public void onLocationChanged(Location location) {
        double mps = location.getSpeed();
        double kph = mps * 3.6;
        if (kph < speedErrThresholdKph)
            kph *= (1 + speedErrFactor);
        else
            kph += speedErrMarginKph;
        updateSpeedometer((int)kph);
        updateTime(Calendar.getInstance());
        logLocation();
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
}
