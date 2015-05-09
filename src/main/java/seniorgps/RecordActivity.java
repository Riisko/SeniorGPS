package seniorgps;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.OutputStreamWriter;
import java.util.ArrayList;

import risko.seniorgps.R;
import seniorgps.config.Constants;
import seniorgps.config.DataSettings;
import seniorgps.config.GeofenceErrorMessages;
import seniorgps.controller.GeofenceTransitionsIntentService;
import seniorgps.controller.SeniorLocationListener;
import seniorgps.model.DataGeofence;
import seniorgps.model.DataPosition;
import seniorgps.model.DataRoute;
import seniorgps.model.DataStorage;

public class RecordActivity extends FragmentActivity implements LocationListener, OnClickListener, GoogleMap.OnMapLongClickListener, OnMarkerDragListener, OnMapReadyCallback, ResultCallback<Status>, ConnectionCallbacks, OnConnectionFailedListener {

    TextView textPosition;
    TextView textInfo;
    TextView positionInfoText;
    Button buttonSave;
    Button buttonDeleteAll;
    Button buttonDeleteOne;
    Button buttonStop;
    Button buttonAddGeofences, buttonRemoveGeofences;


    final int           SIZE_OF_POSITION_BLOCKS = 1000;

    double[]            listLat         = new double[SIZE_OF_POSITION_BLOCKS];
    double[]            listLong            = new double[SIZE_OF_POSITION_BLOCKS];


    LocationRequest mLocationRequest;
    LocationManager lm;
    android.location.LocationListener ll = new SeniorLocationListener();
    protected Location mLastLocation;
    private SharedPreferences mSharedPreferences;
    private PendingIntent mGeofencePendingIntent;
    LatLng latLng;
    protected ArrayList<Geofence> mGeofenceList = null;
    protected ArrayList<Marker> markerList = null;
    protected ArrayList<Circle> circleList = null;
    protected ArrayList<LatLng> latLngList = null;
    PowerManager.WakeLock wakeLock;
    GoogleMap supportMap;
    GoogleApiClient mGoogleApiClient;
    AlertDialog.Builder alertBuilder;
    Marker stopMarker;
    Circle stopCircle;
    PolylineOptions polyOptions, polyOptionsTemp;

    protected static final String TAG = "create-monitor-geofence";

    double pLat;
    double pLong;

    private boolean isGPSon = false;
    private boolean mRequestingLocationUpdates = true;
    private boolean mGeofencesAdded;
    private long    startTime = 0;
    private int		positionInArray		= 0;
    private int     gpsRange;
    private String lastDraggedGeofence = "";
    private double positionAccuracy = 0.01d;        // zjednodušení
    // hledání
    // pozice
    // pro
    // Českou
    // republiku
    private int refreshRateInMilliseconds = 2000;        // obnovovací
    // frekvence
    // programu
    String pathName;
    // runs without a timer by reposting this handler at the end of the
    // runnable
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            long millis = System
                    .currentTimeMillis()
                    - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            /*TADY*/
            publishLocation(
                    DataStorage.getLastPosition()
                            .getDataLat(),
                    DataStorage.getLastPosition()
                            .getDataLong());

            textInfo.setText(String
                    .format("Ticks: %d:%02d\n"
                                    + (getString(R.string.ticks))
                                    + "%d \nGPS:%b",
                            minutes,
                            seconds,
                            DataRoute.getPositions()
                                    .size(),
                            isGPSon));

            timerHandler.postDelayed(
                    this,
                    refreshRateInMilliseconds);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("Entering onCreate");
        setContentView(R.layout.activity_record);
        // keep the display awake
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.supportMap);
        supportMapFragment.getMapAsync(this);
        DataStorage.clearDataPosition();
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Lock");
        textPosition = (TextView) findViewById(R.id.textPosition);
        textInfo = (TextView) findViewById(R.id.textInfo);
        positionInfoText = (TextView) findViewById(R.id.positionInfoText);

        buttonSave = (Button) findViewById(R.id.buttonSaveSettings);
        buttonSave.setOnClickListener(this);
        buttonDeleteAll = (Button) findViewById(R.id.buttonDeleteAll);
        buttonDeleteAll.setOnClickListener(this);
        buttonDeleteOne = (Button) findViewById(R.id.buttonDeleteOne);
        buttonDeleteOne.setOnClickListener(this);
        buttonStop = (Button) findViewById(R.id.btn_stop_state);
        buttonStop.setOnClickListener(this);
        buttonAddGeofences = (Button) findViewById(R.id.add_geofence);
        buttonAddGeofences.setOnClickListener(this);
        buttonRemoveGeofences = (Button) findViewById(R.id.remove_geofence);
        buttonRemoveGeofences.setOnClickListener(this);


        mGeofencePendingIntent = null;
        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME,
                MODE_PRIVATE);
        mGeofencesAdded = mSharedPreferences.getBoolean(Constants.GEOFENCES_ADDED_KEY, false);
        gpsRange = DataSettings.getGpsRange()*100;

        buildGoogleApiClient();

        mGeofenceList = new ArrayList<Geofence>();
        markerList = new ArrayList<Marker>();
        circleList = new ArrayList<Circle>();

        polyOptions = new PolylineOptions().color(Color.BLUE)
                                            .width(4)
                                            .zIndex(30)
                                            .visible(true);
        polyOptionsTemp = new PolylineOptions().color(Color.RED)
                                                .width(6)
                                                .zIndex(30)
                                                .visible(true);



        LayoutInflater li = LayoutInflater.from(getBaseContext());
        View promptsView = li.inflate(R.layout.prompts, null);
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        ll = new SeniorLocationListener();
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, ll);
        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, ll);

        SeniorLocationListener.setRecording(true);
        mRequestingLocationUpdates = true;


        isGPSon = lm
                .isProviderEnabled(LocationManager.GPS_PROVIDER); // Return
        // a
        // boolean


		/*
		 * Set string alertBuilder for recording
		 */
        alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle(getString(R.string.accept));
        alertBuilder.setMessage(getString(R.string.unreversable_delete_warning));

        alertBuilder.setPositiveButton(getString(R.string.yes),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,
                                        int which) {
                        // vymazání lokálního uložiště
                        DataStorage.clearDataPosition();
                        DataRoute.clearDataPosition();
                        polyOptions = new PolylineOptions().color(Color.BLUE)
                                .width(4)
                                .zIndex(30)
                                .visible(true);
                        polyOptionsTemp = new PolylineOptions().color(Color.RED)
                                .width(6)
                                .zIndex(30)
                                .visible(true);
                        try {
                            // vymazání souboru
                            OutputStreamWriter out = new OutputStreamWriter(
                                    openFileOutput(
                                            "route.txt",
                                            0));
                            out.write(""); //Tohle smaze cely soubor TODO:smazat jednu trasu.
                            out.close();
                            Toast.makeText(
                                    getApplicationContext(),
                                    getString(R.string.all_tracks_deleted),
                                    Toast.LENGTH_LONG).show();
                        } catch (Throwable t) {
                            Toast.makeText(
                                    getApplicationContext(),
                                    "Exception: "
                                            + t.toString(),
                                    Toast.LENGTH_LONG).show();
                        }

                        dialog.dismiss();
                    }
                });

        alertBuilder.setNegativeButton(getString(R.string.no),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,
                                        int which) {
                        dialog.dismiss();
                    }
                });



        timerHandler.postDelayed(timerRunnable, 0);
        System.out.println("Exiting onCreate");
    }

    private void loadGeofences() {
        for (int i = 0; i < DataGeofence.getPositions().size(); i++) {
            double lat = DataGeofence.getPositions().get(i).getDataLat();
            double lng = DataGeofence.getPositions().get(i).getDataLong();
            String title = Integer.toString(markerList.size());
            createGeofence(lat, lng, gpsRange, "CIRCLE", title);
        }
    }
    private void loadTracks() {
            LatLng latLngl;
        for (int i=0; i < DataRoute.getPositions().size(); i++){
            double lat = DataRoute.getPositions().get(i).getDataLat();
            double lng = DataRoute.getPositions().get(i).getDataLong();
            latLngl = new LatLng(lat, lng);
            latLngList.add(latLngl);
        }
        if(latLngList != null) {
            polyOptions.addAll(latLngList);
            supportMap.addPolyline(polyOptions);
        }
    }
    @Override
    public void onClick(View v) {
        if (v == buttonAddGeofences) addGeofencesButtonHandler(v);

        if (v == buttonRemoveGeofences) removeGeofencesButtonHandler(v);

        if (v == buttonDeleteAll) deleteAllButtonHandler(v);

        if(v == buttonDeleteOne) deleteOneButtonHandler(v);

        if (v == buttonSave) saveTrack(v);

        if (v == buttonStop) stopRecording(v);
    }

    private void deleteOneButtonHandler(View v) {
        DataStorage.clearDataPosition();
        polyOptionsTemp = new PolylineOptions().color(Color.RED)
                                .width(6)
                                .zIndex(30)
                                .visible(true);
    }

    public void deleteAllButtonHandler(View v) {
                AlertDialog alert = alertBuilder.create();
                alert.show();
    }
    // Uložení trasy po stisknutí tlačítka
    public void saveTrack(View v) {
            try {

                OutputStreamWriter out = new OutputStreamWriter(
                        openFileOutput(
                                "route.txt",
                                0));
                for (int i =0; i < DataStorage.getPositions().size(); i++){
                    DataRoute.addNewPosition(DataStorage.getGeofences().get(i));
                }
                out.write(DataRoute
                        .getAllDataAsString());

                out.close();
                Toast.makeText(
                        getApplicationContext(),
                        getString(R.string.track_saved)
                                + DataRoute.getAllDataAsString(),
                        Toast.LENGTH_LONG)
                        .show();

                out = new OutputStreamWriter(
                        openFileOutput(
                                "geofences.txt",
                                0));
                for (int i=0; i < DataStorage.getGeofences().size(); i++){
                    DataGeofence.addNewPosition(DataStorage.getGeofences().get(i));
                }
                out.write(DataGeofence
                        .getAllDataAsString());

                out.close();
                Toast.makeText(
                        getApplicationContext(),
                        getString(R.string.geofence_saved)
                                + DataGeofence.getAllDataAsString(),
                        Toast.LENGTH_LONG)
                        .show();
                SeniorLocationListener.setRecording(false);
                mRequestingLocationUpdates = false;
            } catch (Throwable t) {
                Toast.makeText(
                        getApplicationContext(),
                        "Exception: "
                                + t.toString(),
                        Toast.LENGTH_LONG)
                        .show();

            }
    }

    public void stopRecording(View v) {
            try {
                if (mRequestingLocationUpdates == true) {
                    stopLocationUpdates();
                    buttonStop.setText("Stop");
                    buttonStop.setBackgroundColor(Color.GREEN);

                } else {
                    startLocationUpdates();
                    buttonStop.setText("Rec");
                    buttonStop.setBackgroundColor(Color.RED);
                }
            } catch (Throwable t) {
                Toast.makeText(
                        getApplicationContext(),
                        "Exception: "
                                + t.toString(),
                        Toast.LENGTH_LONG)
                        .show();

            }

    }

    @Override
    protected void onStop(){
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        // reset all
        System.out.println("Entering onResume");
        SeniorLocationListener.setRecording(true);
        mRequestingLocationUpdates = true;

        wakeLock.acquire();
        if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
            startLocationUpdates();
        }
        if (supportMap != null) {
            supportMap.setMyLocationEnabled(true);
        }

        DataStorage.clearDataPosition();
        DataStorage.clearGeofences();
        System.out.println("Exiting onResume");
    }

    //added 6.4. 11:58
    @Override
    protected void onPause() {
        super.onPause();
        System.out.println("Entering onPause");
        wakeLock.release();
        System.out.println("Exiting onPause");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(1, 1, 1, (getString(R.string.about)));
        menu.add(2, 2, 2, (getString(R.string.app_name)));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Log in to access settings
        //
        // QUICK TIP:
        switch (item.getItemId()) {
            case 1:
                Intent intent_help = new Intent(getBaseContext(),
                        HelpActivity.class);
                startActivity(intent_help);
                return true;
            case 2:
                Intent intent_main = new Intent(getBaseContext(),
                        MainActivity.class);
                startActivity(intent_main);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    public void publishLocation(double lat, double lng) {
        System.out.println("Entering publishLocation");
        StringBuilder sb = new StringBuilder(getString(R.string.recording));
        sb.append(Location.convert(lat, Location.FORMAT_SECONDS));
        sb.append(" E ");
        sb.append(Location.convert(lng, Location.FORMAT_SECONDS));
        pLat = lat;
        pLong = lng;
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 15);
        this.supportMap.animateCamera(cameraUpdate);
        if(lat != 0 || lng != 0) {
            polyOptionsTemp.add(new LatLng(lat, lng));
            supportMap.addPolyline(polyOptionsTemp);
        }
        textPosition.setText(sb.toString());
        System.out.println(sb.toString());

        System.out.println("mGeofenceList: "+ mGeofenceList.size());
        System.out.println("markerList: "+ markerList.size());
        System.out.println("circleList: "+ circleList.size());
        System.out.println("Exiting publishLocation");
    }

    @Override
    public void onMapReady(final GoogleMap supportMap) {
        System.out.println("Entering onMyMapReady");
        this.supportMap = supportMap;
        if (supportMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            this.supportMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.supportMap))
                    .getMap();
            System.out.println("supportMap was null");

            // Check if we were successful in obtaining the map.
        } else {
            System.out.println("supportMap was not null");
        }

        this.supportMap.setMyLocationEnabled(true);
        this.supportMap.setOnMarkerDragListener(this);
        this.supportMap.setOnMapLongClickListener(this);

        //lm.removeUpdates(ll);
        System.out.println("Exiting onMyMapReady");
    }

    public void showToast(String text){
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        System.out.println("onStart id: "+marker.getId());
    }

    @Override
    public void onMarkerDrag(Marker marker) {
        marker.setPosition(new LatLng(marker.getPosition().latitude, marker.getPosition().longitude));
        //this marker id starts with "m" so we need the second char
        System.out.println("onDrag id: "+marker.getId());
        String id = marker.getId();
        id = Character.toString(id.charAt(1));
        int index = (Integer.parseInt(id));
        stopCircle = circleList.get(index);
        stopCircle.setCenter(marker.getPosition());
        stopMarker = markerList.get(index);
        stopMarker.setPosition(marker.getPosition());
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        System.out.println("onEnd id: "+marker.getId());
        LatLng dragPosition = marker.getPosition();
        double dragLat = dragPosition.latitude;
        double dragLong = dragPosition.longitude;
        marker.setPosition(new LatLng(marker.getPosition().latitude, marker.getPosition().longitude));
        //this marker id starts with "m" so we need the second char
        String id = marker.getId();
        id = Character.toString(id.charAt(1));
        int index = (Integer.parseInt(id));
        stopCircle = circleList.get(index);
        stopCircle.setCenter(dragPosition);
        stopMarker = markerList.get(index);
        stopMarker.setPosition(dragPosition);
        //supportMap.clear();
        removeGeofence(id, false);
        System.out.println("Removed geofence: "+ marker.getId());
        String title = Integer.toString(markerList.size());
        lastDraggedGeofence = id;
        setButtonsEnabledState();
        createGeofence(dragLat, dragLong, gpsRange, "MOVE", title);
        Toast.makeText(
                RecordActivity.this,
                "onMarkerDragEnd dragLat :" + dragLat + " dragLong :"
                        + dragLong, Toast.LENGTH_SHORT).show();
        Log.i("info", "on drag end :" + dragLat + " dragLong :" + dragLong);
    }

    /**
     * Creates a graphical circle on the map.
     * @param latitude
     * @param longitude
     * @param radius
     * @param geofenceType
     * @param title
     */
    private void createGeofence(double latitude, double longitude, int radius,
                                String geofenceType, String title) {
    try {
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    // The GeofenceRequest object.
                    getGeofencingRequest(),
                    // A pending intent that is reused when calling removeGeofences(). This
                    // pending intent is used to generate an intent when a matched geofence
                    // transition is observed.
                    getGeofencePendingIntent()

            ).setResultCallback(this); // Result processed in onResult().
            if(geofenceType != "MOVE") {
                stopMarker = supportMap.addMarker(new MarkerOptions()
                        .draggable(true)
                        .position(new LatLng(latitude, longitude))
                        .title(title));
                System.out.println("Added marker: " + title);

                stopCircle = supportMap.addCircle(new CircleOptions()
                        .center(new LatLng(latitude, longitude)).radius(radius)
                        .fillColor(Color.TRANSPARENT));

                markerList.add(stopMarker);
                circleList.add(stopCircle);

                showToast("Marker Added");
            }
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            logSecurityException(securityException);
        }
        //Save geofence data
        DataStorage.addNewGeofence(new DataPosition(latitude, longitude));
    }


    private void removeGeofence() {

        //deletes all overlays on the map;
        //supportMap.clear();
        stopMarker = markerList.get(markerList.size()-1);
        markerList.remove(markerList.size()-1);
        stopMarker.remove();

        stopCircle = circleList.get(circleList.size()-1);
        circleList.remove(circleList.size()-1);
        stopCircle.remove();


        ArrayList<String> list = new ArrayList<String>();
        list.add(Integer.toString(mGeofenceList.size() - 1));
        try {
            // Remove geofences.
            LocationServices.GeofencingApi.removeGeofences(
                    mGoogleApiClient,
                    // This is the same pending intent that was used in addGeofences().
                    list
            ).setResultCallback(this); // Result processed in onResult().
            mGeofenceList.remove(mGeofenceList.size()-1);
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            logSecurityException(securityException);
        }
    }

    private void removeGeofence(String title, boolean deleteOverlay) {
        ArrayList<String> list = new ArrayList<String>();
        list.add(title);
        try {
            // Remove geofences.
            LocationServices.GeofencingApi.removeGeofences(
                    mGoogleApiClient,
                    // This is the same pending intent that was used in addGeofences().
                    list
            ).setResultCallback(this); // Result processed in onResult().
        mGeofenceList.remove(Integer.parseInt(title));
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            logSecurityException(securityException);
        }
        if (deleteOverlay == true) {
            int id = Integer.parseInt(title);
            stopMarker = markerList.get(id);
            markerList.remove(id);
            stopMarker.remove();

            stopCircle = circleList.get(id);
            circleList.remove(id);
            stopCircle.remove();
        }

    }


    /**
     * Adds geofences, which sets alerts to be notified when the device enters or exits one of the
     * specified geofences. Handles the success or failure results returned by addGeofences().
     */
    public void addGeofencesButtonHandler(View view) {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, "GoogleApiClient not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        String title = Integer.toString(markerList.size());
        System.out.println("Geofence title: "+ title);
        createGeofence(pLat, pLong, gpsRange, "CIRCLE", title);
        lastDraggedGeofence = "";
        showToast("Geofence created at: " + pLong);
    }

    /**
     * Removes geofences, which stops further notifications when the device enters or exits
     * previously registered geofences.
     */
    public void removeGeofencesButtonHandler(View view) {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            return;
        }
        if (lastDraggedGeofence == "") {
            removeGeofence();
        } else {
            removeGeofence(lastDraggedGeofence, true);
        }
    }

    private void logSecurityException(SecurityException securityException) {
        Log.e(TAG, "Invalid locat permi" +
                "You need to use FINE_LOCATION /w geofence", securityException);
    }

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            Toast.makeText(this, "mGeofencePendingIntent != null", Toast.LENGTH_SHORT).show();
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        Toast.makeText(this, "mGeofencePendingIntent == null", Toast.LENGTH_SHORT).show();
        mGeofencePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }

    /**
     * Builds and returns a9* GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    private GeofencingRequest getGeofencingRequest() {

        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        Geofence geofence = new Geofence.Builder()
                .setRequestId(Integer.toString(mGeofenceList.size()))
                .setCircularRegion(pLat, pLong, DataSettings.getGpsRange()*100)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(mGeofenceList);
        builder.addGeofence(geofence);

        mGeofenceList.add(geofence);
        Toast.makeText(this, "Geofence request added",  Toast.LENGTH_SHORT).show();

        // Return a GeofencingRequest.
        return builder.build();
    }


    @Override
    public void onResult(Status status) {
        if (status.isSuccess()) {
            // Update state and save in shared preferences.
            mGeofencesAdded = !mGeofencesAdded;
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putBoolean(Constants.GEOFENCES_ADDED_KEY, mGeofencesAdded);
            editor.commit();

            // Update the UI. Adding geofences enables the Remove Geofences button, and removing
            // geofences enables the Add Geofences button.
            setButtonsEnabledState();

            Toast.makeText(
                    this,
                    getString(mGeofencesAdded ? R.string.Add_Geofence :
                            R.string.Remove_Geofence),
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            // Get the status code for the error and log it
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    status.getStatusCode());
            Log.e(TAG, errorMessage);
        }
    }

    /**
     * Ensures that only one button is enabled at any time. The Add Geofences button is enabled
     * if the user hasn't yet added geofences. The Remove Geofences button is enabled if the
     * user has added geofences.
     */
    private void setButtonsEnabledState() {
        if (mGeofenceList.size() > 0 && mGeofenceList.size() < 9) {
            buttonAddGeofences.setEnabled(true);
            buttonRemoveGeofences.setEnabled(true);
        } else {
            if(mGeofenceList.size() == 0){
                buttonAddGeofences.setEnabled(true);
                buttonRemoveGeofences.setEnabled(false);
            }
            if(mGeofenceList.size() == 9){
                buttonAddGeofences.setEnabled(false);
                buttonRemoveGeofences.setEnabled(true);
            }
        }
    }

    public synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();

    }

    @Override
    public void onConnectionSuspended(int i) {
        showToast("Connection Suspended");
        mGoogleApiClient.connect();
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //showToast("Connection Failed");
    }

    public void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(15000);
        mLocationRequest.setFastestInterval(10000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
        mRequestingLocationUpdates = true;
        SeniorLocationListener.setRecording(true);
    }

    public void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
        mRequestingLocationUpdates = false;
        SeniorLocationListener.setRecording(false);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            //publishLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        }
        if (mRequestingLocationUpdates) {
            createLocationRequest();
            startLocationUpdates();
        }
        if (mLastLocation != null) {
            // Determine whether a Geocoder is available.
            if (!Geocoder.isPresent()) {
                //Toast.makeText(this, "No geocoder available",
                //     Toast.LENGTH_SHORT).show();
                return;
            }

        }
        loadGeofences();
        loadTracks();
        setButtonsEnabledState();

    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        SeniorLocationListener.setRecording(false);
        mRequestingLocationUpdates = false;
        buttonStop.setBackgroundColor(Color.GREEN);
        buttonStop.setText("Stop");
        LatLng oldLatlng = null;
        LatLng newLatlng = latLng;
        if ( polyOptionsTemp.getPoints().size() > 0) {
            oldLatlng = polyOptionsTemp.getPoints().get(polyOptionsTemp.getPoints().size() - 1);
            int x1 = (int) (oldLatlng.longitude * 10000);
            int y1 = (int) (oldLatlng.latitude * 10000);

            // find every pixel between the start and the end of the line with Bresenham
            int x = x1 - (int) (newLatlng.longitude * 10000);
            int y = y1 - (int) (newLatlng.latitude * 10000);
            int dx1 = 0, dy1 = 0, dx2 = 0, dy2 = 0;
            if (x<0) dx1 = -100; else if(x>0) dx1 = 100;
            if (y<0) dy1 = -100; else if(y>0) dy1 = 100;
            if (x<0) dx2 = -100; else if(x>0) dx2 = 100;
            int longest = Math.abs(x);
            int shortest = Math.abs(y);
            if(!(longest>shortest)) {
                longest = Math.abs(y);
                shortest = Math.abs(x);
                if (y<0) dy2 = -100; else if (y>0) dy2 = 100;
            }

            int numerator = longest >> 1;
            for (int i=0; i< longest -100; i+=100) {
                listLat[positionInArray] = x1/10000;
                listLong[positionInArray] = y1/10000;
                positionInArray++;
                DataStorage.addNewPosition(
                        new DataPosition(
                                listLat[SIZE_OF_POSITION_BLOCKS / 2],
                                listLong[SIZE_OF_POSITION_BLOCKS / 2]
                        ));
                numerator += shortest;
                if (!(numerator < longest)) {
                    numerator -= longest ;
                    x1 += dx2;
                    y1 += dy2;
                } else {
                    x1 += dx2;
                    y1 += dy2;
                }
            }
        //last position is important to create an area
        }
        polyOptionsTemp.add(latLng);
        supportMap.addPolyline(polyOptionsTemp);
        listLat[positionInArray] = latLng.latitude;
        listLong[positionInArray] = latLng.longitude;
        positionInArray++;
        DataStorage.addNewPosition(
                new DataPosition(
                        listLat[SIZE_OF_POSITION_BLOCKS / 2],
                        listLong[SIZE_OF_POSITION_BLOCKS / 2]
                ));

    }
}
