package com.fyp.faaiz.ets.service;

import android.Manifest;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.fyp.faaiz.ets.MainActivity;
import com.fyp.faaiz.ets.R;
import com.fyp.faaiz.ets.auth.LoginActivity;
import com.fyp.faaiz.ets.auth.RegisterActivity;
import com.fyp.faaiz.ets.broadcast.LocationReceiver;
import com.fyp.faaiz.ets.model.LocationPost;
import com.fyp.faaiz.ets.session.Session;
import com.google.android.gms.cast.framework.SessionManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TrackerService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "LocationTracker/Service";

    public static final String USER_ID = "user_id";
    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "langitude";

    public static TrackerService service;
    private NotificationManager nm;
    private Notification notification;
    private static boolean isRunning = false;

    private static volatile PowerManager.WakeLock wakeLock;
    private PendingIntent mLocationIntent;

    private GoogleApiClient mGoogleApiClient;
    private LocationListener mLocationListener;
    private Location mLastReportedLocation;

    ArrayList<Messenger> mClients = new ArrayList<>();
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_LOG_RING = 4;

    public TrackerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public void onCreate() {
        // Check whether Google Play Services is installed
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int isAvailable = apiAvailability.isGooglePlayServicesAvailable(getApplicationContext());
        if (isAvailable == ConnectionResult.SUCCESS) {
            mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            mGoogleApiClient.connect();

        } else {
            Toast.makeText(getApplicationContext(), "Google Play Services not found. Please install to use this app.", Toast.LENGTH_SHORT).show();
            stopSelf();
            return;
        }

        TrackerService.service = this;
        isRunning = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        /* kill persistent notification */
        if (nm != null) {
            nm.cancelAll();
        }

        if (mGoogleApiClient != null && mLocationIntent != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, mLocationIntent);
        }
        isRunning = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public static boolean isRunning() {
        return isRunning;
    }

    private synchronized GoogleApiClient buildGoogleApiClient() {
        return new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    private LocationRequest createLocationRequest() {
        return new LocationRequest()
                .setInterval(1 * 1000)
                .setFastestInterval(5000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void showNotification() {
        NotificationManager notificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);

        // prepare intent which is triggered if the
        // notification is selected

        Intent intent = new Intent(this, MainActivity.class);
        // use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);

        // build notification
        // the addAction re-use the same intent to keep the example short
        Notification n = new Notification.Builder(this)
                .setContentTitle("New mail from " + "test@gmail.com")
                .setContentText("Subject")
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark)
                .setContentIntent(pIntent)
                .setAutoCancel(true)
                .build();

        NotificationManager notif =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        notificationManager.notify(0, n);
    }

    private void updateNotification(String text) {
        if (nm != null) {
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                    new Intent(this, MainActivity.class), 0);
            notification.when = System.currentTimeMillis();
            nm.notify(1, notification);
        }
    }

    private String getDeviceId() {
        return Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private Map<String, String> getDeviceInfo() {
        Map<String, String> info = new HashMap<>();
        info.put("deviceId", getDeviceId());
        info.put("brand", Build.BRAND);
        info.put("device", Build.DEVICE);
        info.put("hardware", Build.HARDWARE);
        info.put("id", Build.ID);
        info.put("manufacturer", Build.MANUFACTURER);
        info.put("model", Build.MODEL);
        info.put("product", Build.PRODUCT);

        return info;
    }

    public void sendLocation(Location location) {
    }

/*    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Toast.makeText(getApplicationContext(), "Service onConnected", Toast.LENGTH_SHORT).show();
        //getFuseLocation();
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(final Location location) {
                Toast.makeText(getApplicationContext(), location.getLatitude() + " Faaiz " + location.getLongitude(), Toast.LENGTH_SHORT).show();
                StringRequest locationRequest = new StringRequest(Request.Method.POST, "https://nets.herokuapp.com/location/user/save", new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }){
                    @Override
                    protected Map<String, String> getParams() {
                        Map<String, String> params = new HashMap<String, String>();
                        params.put(USER_ID, "1");
                        params.put(LATITUDE, String.valueOf(location.getLatitude()));
                        params.put(LONGITUDE, String.valueOf(location.getLongitude()));
                        return params;
                    }
                };

                RequestQueue queue = Volley.newRequestQueue(getApplication());
                queue.add(locationRequest);

                LocationRequest request = LocationRequest.create();
                request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                request.setInterval(5000);
                request.setFastestInterval(1000);

                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                    return;
                }
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, request, mLocationListener);
            }
        };
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }*/

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                final String latitude = String.valueOf(location.getLatitude());
                final String longitude = String.valueOf(location.getLongitude());

                Toast.makeText(getApplicationContext(), location.getLatitude() + "Faaiz" + location.getLongitude(), Toast.LENGTH_SHORT).show();

                StringRequest locationRequest = new StringRequest(Request.Method.POST, "https://nets.herokuapp.com/location/user/save", new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }) {
                    @Override
                    protected Map<String, String> getParams() {

                        Session s = new Session(getApplicationContext());
                        String id = s.getId().get(Session.KEY_ID).toString();
                        Map<String, String> params = new HashMap<String, String>();
                        params.put(USER_ID, id);
                        params.put(LATITUDE, latitude);
                        params.put(LONGITUDE, longitude);
                        return params;
                    }
                };

                RequestQueue queue = Volley.newRequestQueue(getApplication());
                queue.add(locationRequest);
            }
        };

        LocationRequest request = LocationRequest.create();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setInterval(5000);
        request.setFastestInterval(1000);
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, request, mLocationListener);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    try {
                        Message replyMsg = Message.obtain(null, MSG_LOG_RING);
                        //replyMsg.obj = mLogRing;
                        msg.replyTo.send(replyMsg);
                    } catch (RemoteException e) {
                        Log.e(TAG, e.getMessage());
                    }
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}