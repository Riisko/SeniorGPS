package seniorgps;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import seniorgps.config.Constants;
import seniorgps.config.DataSettings;
import seniorgps.controller.DataCollisionChecker;
import seniorgps.controller.FetchAddressIntentService;
import seniorgps.controller.GeofenceTransitionsIntentService;
import seniorgps.controller.SeniorLocationListener;
import seniorgps.controller.SmsReciever;
import seniorgps.model.DataGeofence;
import seniorgps.model.DataPosition;
import seniorgps.model.DataRoute;
import seniorgps.model.DataStorage;

import risko.seniorgps.R;

import android.app.PendingIntent;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends Activity implements LocationListener, ConnectionCallbacks, OnConnectionFailedListener, ResultCallback<Status> {

	TextView		textPosition;
	TextView		textInfo;
	TextView		positionInfoText;
	ProgressBar		positionBar;
	Button		    callForHelp;

	private boolean	isGPSon				= false;
    private boolean addressFound = false;
    public static int leftGeofence = 0;
    protected boolean mAddressRequested;
    protected static final String TAG = "main-activity";
    protected static final String ADDRESS_REQUESTED_KEY = "address-request-pending";
    protected static final String LOCATION_ADDRESS_KEY = "location-address";
    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;
    /**
     * Represents a geographical location.
     */
    protected Location mLastLocation;
    private Location currentLocation;
    private PendingIntent mGeofencePendingIntent;
    protected String mAddressOutput;
    private AddressResultReceiver mResultReceiver;
	private long	startTime				= 0;
	private int		refreshRateInMilliseconds	= 2000;						// obnovovací
																// frekvence
																// programu


	private int		smsRate				= DataSettings
											.getTimeSMSAlertInSeconds();	// X=300;
																// X*2000
																// =
																// 10
																// min
	private int		counterForSMS			= 0;							// timing
																// for
																// SMS
																// sender


	// runs without a timer by reposting this handler at the end of the
	// runnable
	Handler		timerHandler			= new Handler();
	Runnable		timerRunnable			= new Runnable() {

										@Override
										public void run() {
											/*
											 * init locals
											 */
											long millis = System
													.currentTimeMillis()
													- startTime;
											int seconds = (int) (millis / 1000);
											int minutes = seconds / 60;
											seconds = seconds % 60;
											int positionsNotInRoute = 0;


											Log.d("RUN TICK",
													"TICK: "
															+ counterForSMS);

											/*
											 * presenting
											 * and
											 * calculating
											 * position
											 * data
											 */
											publishLocation(
													DataStorage.getLastPosition()
															.getDataLat(),
													DataStorage.getLastPosition()
															.getDataLong());
                                            if(DataSettings.monitoring == true){

                                                if (DataStorage
                                                        .getPositions()
                                                        .size() > 0) {
                                                    // výpočet
                                                    // kolize
                                                    // cesty
                                                    // s
                                                    // výsledkem
                                                    // posledních
                                                    // 10
                                                    // měření
                                                    positionsNotInRoute = DataCollisionChecker
                                                            .checkDataAgainstRoute(
                                                                    DataRoute.getPositions(),
                                                                    DataStorage.getLastTenPositions(),
                                                                    DataSettings.getGpsRange()*10);
                                                    if(leftGeofence > 0 && leftGeofence <10){
                                                        leftGeofence++;
                                                    }
                                                    if (leftGeofence == 0 && positionsNotInRoute != 0) {
                                                        positionsNotInRoute = 0;
                                                    }
                                                    if (leftGeofence !=0 && positionsNotInRoute ==0 ) {
                                                        positionsNotInRoute = 0;
                                                    }
                                                        positionsNotInRoute += leftGeofence;

                                                    // grafické
                                                    // znázornění
                                                    // posledních
                                                    // 10
                                                    // měření
                                                    positionBar = (ProgressBar) findViewById(R.id.positionBar);
                                                    positionBar.setProgress(10*(positionsNotInRoute));

                                                }
                                                textInfo.setText(DataSettings
                                                        .getTextPersonName()
                                                        + "\n\n"
                                                        + DataSettings
                                                                .getTextPersonAddress());

                                                callForHelp.setText((getString(R.string.call_for_help))
                                                        + "\n"
                                                        + DataSettings
                                                                .getTextPhoneNumber());

                                                /*
                                                 * Když je
                                                 * senior
                                                 * daleko = 10
                                                 * měření mimo
                                                 * trasu
                                                 */
                                                if (positionsNotInRoute > 9) {
                                                    new Color();
                                                    callForHelp.setBackgroundColor(Color.RED);
                                                    new Color();
                                                    callForHelp.setTextColor(Color.WHITE);
                                                        if (mGoogleApiClient.isConnected() && mLastLocation != null) {
                                                            startIntentService();
                                                            mAddressRequested = true;
                                                        }

                                                    counterForSMS++;
                                                    if (counterForSMS >= smsRate) {
                                                        if(addressFound == true) {
                                                            DataSettings.setTextAlertSMS((getString(R.string.person_is_away_on))
                                                                    + mAddressOutput
                                                                    + (getString(R.string.SMS_help_WHERE_ARE_YOU)));
                                                        } else {
                                                            SmsReciever.mAddressOutput = DataStorage.getLastPosition().getDataLat()
                                                                    + ";"
                                                                    + DataStorage.getLastPosition().getDataLong();
                                                            DataSettings.setTextAlertSMS((getString(R.string.person_is_away_on))
                                                                    + DataStorage.getLastPosition().getDataLat()
                                                                    + ";"
                                                                    + DataStorage.getLastPosition().getDataLong()
                                                                    + (getString(R.string.SMS_help_WHERE_ARE_YOU)));
                                                        }
                                                        DataStorage.clearDataPosition();
                                                        // Odeslání
                                                        // SMS
                                                        sendAlertSMS();
                                                        mAddressRequested=false;
                                                    }
                                                } else {
                                                    mAddressRequested=false;
                                                    counterForSMS = 0;
                                                    new Color();
                                                    // reset
                                                    // v
                                                    // případě
                                                    // návratu
                                                    // na
                                                    // správnou
                                                    // trasu
                                                    callForHelp.setTextColor(Color.BLACK);
                                                }
                                            }
											timerHandler.postDelayed(
													this,
													refreshRateInMilliseconds);
										}
									};

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// keep the display awake
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.activity_main);
        mResultReceiver = new AddressResultReceiver(new Handler());

        mAddressRequested = false;
        mAddressOutput = "";
        mGeofencePendingIntent = null;
        updateValuesFromBundle(savedInstanceState);
        buildGoogleApiClient();
		textPosition = (TextView) findViewById(R.id.textPosition);
		textInfo = (TextView) findViewById(R.id.textInfo);
		positionInfoText = (TextView) findViewById(R.id.positionInfoText);
		callForHelp = (Button) findViewById(R.id.buttonSaveSettings);

		callForHelp.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent callIntent = new Intent(Intent.ACTION_CALL);
				callIntent.setData(Uri.parse("tel:"
						+ DataSettings.getTextPhoneNumber()));
				startActivity(callIntent);
			}
		});

		/*
		 * Start location listener
		 */

		LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        android.location.LocationListener ll = new SeniorLocationListener();
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, ll);
        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, ll);


		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		isGPSon = locationManager
				.isProviderEnabled(LocationManager.GPS_PROVIDER); // Return
													// state
													// of

		timerHandler.postDelayed(timerRunnable, refreshRateInMilliseconds);

	}

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
// Check savedInstanceState to see if the address was previously requested.
            if (savedInstanceState.keySet().contains(ADDRESS_REQUESTED_KEY)) {
                mAddressRequested = savedInstanceState.getBoolean(ADDRESS_REQUESTED_KEY);
            }
// Check savedInstanceState to see if the location address string was previously found
// and stored in the Bundle. If it was found, display the address string in the UI.
            if (savedInstanceState.keySet().contains(LOCATION_ADDRESS_KEY)) {
                mAddressOutput = savedInstanceState.getString(LOCATION_ADDRESS_KEY);
                SmsReciever.mAddressOutput = mAddressOutput;
            }
        }
    }

    /**
     * Builds a GoogleApiClient. Uses {@code #addApi} to request the LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStart() {
        super.onStart();
        System.out.println(mGoogleApiClient.isConnecting());
        System.out.println(mGoogleApiClient.isConnected());

    }
    @Override
    protected void onStop(){
        super.onStop();
        if(mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }
    private void logSecurityException(SecurityException securityException) {
        Log.e(TAG, "Invalid locat permi" +
                "You need to use FINE_LOCATION /w geofence", securityException);
    }

	@Override
	protected void onResume() {
		super.onResume();
		/*
		 * Reset all data
		 */
		DataStorage.clearDataPosition();
		SeniorLocationListener.setRecording(false);
        textInfo.setText(DataSettings
                .getTextPersonName()
                + "\n\n"
                + DataSettings
                .getTextPersonAddress());
		/*
		 * Loading data from files
		 */

		// Load route from file
		try {
			InputStream in = openFileInput("route.txt");

			if (in != null) {
				InputStreamReader tmp = new InputStreamReader(in);
				BufferedReader reader = new BufferedReader(tmp);
				String str;
				StringBuilder buf = new StringBuilder();

				while ((str = reader.readLine()) != null) {
					buf.append(str + "\n");
					double plat = Double.valueOf(str.split(";")[0]);
					double plong = Double.valueOf(str.split(";")[1]);
					DataRoute.addNewPosition(new DataPosition(plat,
							plong));
				}

				in.close();
				/*
				 * Toast.makeText(this, "Trasa úspěšně načtena! " +
				 * buf.toString(), Toast.LENGTH_LONG).show();
				 */
			}

		} catch (java.io.FileNotFoundException e) {
			Toast.makeText(this, (getString(R.string.no_track_avaible)),
					Toast.LENGTH_LONG).show();
		} catch (Throwable t) {
			Toast.makeText(
					this,
					(getString(R.string.Track_exception))
							+ t.toString(), Toast.LENGTH_LONG)
					.show();
		}
        // Load geofences from file
        try {
            InputStream in = openFileInput("geofences.txt");

            if (in != null) {
                InputStreamReader tmp = new InputStreamReader(in);
                BufferedReader reader = new BufferedReader(tmp);
                String str;
                StringBuilder buf = new StringBuilder();

                while ((str = reader.readLine()) != null) {
                    buf.append(str + "\n");
                    double plat = Double.valueOf(str.split(";")[0]);
                    double plong = Double.valueOf(str.split(";")[1]);
                    DataGeofence.addNewPosition(new DataPosition(plat,
                            plong));
                }

                in.close();

				/*
				 * Toast.makeText(this, "Trasa úspěšně načtena! " +
				 * buf.toString(), Toast.LENGTH_LONG).show();
				 */
            }

        } catch (java.io.FileNotFoundException e) {
            Toast.makeText(this, (getString(R.string.no_track_avaible)),
                    Toast.LENGTH_LONG).show();
        } catch (Throwable t) {
            Toast.makeText(
                    this,
                    (getString(R.string.Track_exception))
                            + t.toString(), Toast.LENGTH_LONG)
                    .show();
        }

		// Load settings from file
		try {
			InputStream in = openFileInput("settings.txt");

			if (in != null) {
				InputStreamReader tmp = new InputStreamReader(in);
				BufferedReader reader = new BufferedReader(tmp);
				String str;
				StringBuilder buf = new StringBuilder();

				while ((str = reader.readLine()) != null) {
					buf.append(str + "\n");
					String firstValue = str.split(":")[0];
					String secondValue = str.split(":")[1];

					if (firstValue.contains("person_name")) {
						DataSettings.setTextPersonName(secondValue);
					} else if (firstValue.contains("person_address")) {
						DataSettings.setTextPersonAddress(secondValue);
					} else if (firstValue.contains("phone_number")) {
						DataSettings.setTextPhoneNumber(secondValue);
					} else if (firstValue.contains("gps_range")) {
						DataSettings.setGpsRange(Integer.valueOf(secondValue));
					} else if (firstValue.contains("smsCheck")) {
                        DataSettings.setSendSMS(Boolean.valueOf(secondValue));
                    } else if (firstValue.contains("monitoringSwitch")) {
                        DataSettings.setMonitoring(Boolean.valueOf(secondValue));
                    }
				}

				in.close();
				/*
				 * Toast.makeText(this,
				 * "Nastavení bylo úspěšně načteno! " + buf.toString(),
				 * Toast.LENGTH_LONG).show();
				 */
			}

		} catch (java.io.FileNotFoundException e) {
			Toast.makeText(this,
					(getString(R.string.no_settings_avaible)),
					Toast.LENGTH_LONG).show();
		} catch (Throwable t) {
			Toast.makeText(
					this,
					(getString(R.string.no_settings_exception))
							+ t.toString(), Toast.LENGTH_LONG)
					.show();
		}
		//load login from file
		try {
			InputStream in = openFileInput("login.txt");

			if (in != null) {
				InputStreamReader tmp = new InputStreamReader(in);
				BufferedReader reader = new BufferedReader(tmp);
				String str;
				StringBuilder buf = new StringBuilder();

				while ((str = reader.readLine()) != null) {
					buf.append(str + "\n");
					String firstValue = str.split(":")[0];
					String secondValue = str.split(":")[1];

					if (firstValue.contains("email ")) {
						String email = firstValue.split(" ")[1];
						DataSettings.setUserEmail(email);
					}
					if (secondValue.contains("pass ")) { //login.txt neobsahuje :passhello
						String pass = secondValue.split(" ")[1];
						DataSettings.setUserPassword(pass);
					}
				}

				in.close();
				/*
				 * Toast.makeText(this,
				 * "Nastavení bylo úspěšně načteno! " + buf.toString(),
				 * Toast.LENGTH_LONG).show();
				 */
			}

		} catch (java.io.FileNotFoundException e) {
			Toast.makeText(this,
					(getString(R.string.no_settings_avaible)),
					Toast.LENGTH_LONG).show();
		} catch (Throwable t) {
			Toast.makeText(
					this,
					(getString(R.string.no_settings_exception))
							+ t.toString(), Toast.LENGTH_LONG)
					.show();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// tvorba menu
		menu.add(1, 1, 1, (getString(R.string.about)));
		menu.add(2, 2, 2, (getString(R.string.action_settings)));
		// getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void publishLocation(double lat, double lng) {
		StringBuilder sb = new StringBuilder((getString(R.string.position)));
		sb.append(Location.convert(lat, Location.FORMAT_SECONDS));
		sb.append(" : E ");
		sb.append(Location.convert(lng, Location.FORMAT_SECONDS));
        if(mAddressRequested == false){
		    textPosition.setText(sb.toString());
        } else {
            textPosition.setText(mAddressOutput);
        }
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
			Intent intent_settings = new Intent(getBaseContext(),
					LoginActivity.class);
			startActivity(intent_settings);
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * SMS sending hack combined with disabled back button.
	 */
	@Override
	public void onBackPressed() {
		if (counterForSMS >= smsRate) {
			counterForSMS = 0;
			try {
				Toast.makeText(
						getApplicationContext(),
						(getString(R.string.location_sent))
								+ DataSettings
										.getTextPhoneNumber()
								+ ":\n\n"
								+ DataSettings
										.getTextAlertSMS(),
						Toast.LENGTH_LONG).show();

				SmsManager sms = SmsManager.getDefault(); // DataSettings.getTextAlertSMS().toString()
				sms.sendTextMessage(DataSettings.getTextPhoneNumber()
						.toString(), null, DataSettings
						.getTextAlertSMS().replace("\n", "")
						.replace("\r", ""), null, null);
			} catch (Exception e) {
				Toast.makeText(
						getApplicationContext(),
						(getString(R.string.sms_failed))
								+ DataSettings
										.getTextPhoneNumber()
								+ ":\n\n"
								+ DataSettings
										.getTextAlertSMS()
								+ "+++" + counterForSMS + " "
								+ smsRate, Toast.LENGTH_LONG)
						.show();
				e.printStackTrace();
			}

		}
	}

	/**
	 * Sending position SMS
	 */
	private void sendAlertSMS() {
		if(DataSettings.sendSMS == true) {
            onBackPressed(); // hack for SMS sending
        }
	}

    @Override
    public void onLocationChanged(Location location) {
        mResultReceiver = new AddressResultReceiver(new Handler());
        if (mAddressRequested) {
            startIntentService();
        }
        currentLocation = location;
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        // Gets the best and most recent location currently available, which may be null
        // in rare cases when a location is not available.
                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                if (mLastLocation != null) {
        // Determine whether a Geocoder is available.
                    if (!Geocoder.isPresent()) {
                        Toast.makeText(this, R.string.no_geocoder_available, Toast.LENGTH_LONG).show();
                        return;
                    }
        // It is possible that the user presses the button to get the address before the
        // GoogleApiClient object successfully connects. In such a case, mAddressRequested
        // is set to true, but no attempt is made to fetch the address (see
        // fetchAddressButtonHandler()) . Instead, we start the intent service here if the
        // user has requested an address, since we now have a connection to GoogleApiClient.
                    if (mAddressRequested) {
                        startIntentService();
                    }
                }

        //Empty field will result in error, there has to be at least one geofence
        if(DataGeofence.getPositions().size() > 0) {
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

            } catch (SecurityException securityException) {
                // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
                logSecurityException(securityException);
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    /**
     * Creates an intent, adds location data to it as an extra, and starts the intent service for
     * fetching an address.
     */

    protected void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save whether the address has been requested.
                savedInstanceState.putBoolean(ADDRESS_REQUESTED_KEY, mAddressRequested);
        // Save the address string.
                savedInstanceState.putString(LOCATION_ADDRESS_KEY, mAddressOutput);
                super.onSaveInstanceState(savedInstanceState);
    }
    protected void startIntentService() {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);
        startService(intent);
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
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    private GeofencingRequest getGeofencingRequest() {

        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        // Add the geofences to be monitored by geofencing service.

            for (int n = 0; n < DataGeofence.getPositions().size(); n++) {
                builder.addGeofence(new Geofence.Builder()
                        .setRequestId(Integer.toString(n))
                        .setCircularRegion(DataGeofence.getPositions().get(n).getDataLat(), DataGeofence.getPositions().get(n).getDataLong(), DataSettings.getGpsRange() * 100)
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                        .build());
            }

        Toast.makeText(this, "Geofence request added",  Toast.LENGTH_SHORT).show();

        // Return a GeofencingRequest.
        return builder.build();
    }

    @Override
    public void onResult(Status status) {
        //leftGeofence = status.getStatusCode();
    }

    /**
     * Receiver for data sent from FetchAddressIntentService.
     */
    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }
        /**
         * Receives data sent from FetchAddressIntentService and updates the UI in MainActivity.
         */
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            // Display the address string or an error message sent from the intent service.
            mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);

            // Show a toast message if an address was found.
            if (resultCode == Constants.SUCCESS_RESULT) {
                showToast(getString(R.string.address_found));
                addressFound = true;
                textPosition.setText(mAddressOutput);
            }
            mAddressRequested = false;
        }
    }
}
