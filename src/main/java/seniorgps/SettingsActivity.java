package seniorgps;

import java.io.OutputStreamWriter;

import seniorgps.config.DataSettings;

import risko.seniorgps.R;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {

	Button	buttonRecord;
	Button	buttonSaveSettings;
	EditText	textPersonName;
	EditText	textPhoneNumber;
	EditText	textAddress;
    CheckBox monitoringSwitch;
    CheckBox smsCheck;
	SeekBar	gpsRange;
	TextView	gpsRangeValue;		// added
	final Context context = this;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		buttonRecord = (Button) findViewById(R.id.buttonRecord);
		textPersonName = (EditText) findViewById(R.id.person_name);
		textPersonName = (EditText) findViewById(R.id.person_name);
		textPhoneNumber = (EditText) findViewById(R.id.phone_number_edit);
		textAddress = (EditText) findViewById(R.id.editAdresa);
        monitoringSwitch = (CheckBox) findViewById(R.id.monitoringSwitch);
        smsCheck = (CheckBox) findViewById(R.id.smsCheck);
		buttonSaveSettings = (Button) findViewById(R.id.buttonSaveSettings);
		gpsRange = (SeekBar) findViewById(R.id.seekBar1);
		gpsRange.setMax(50); // 0,5 km max , pro 50 je 500m, pro 25 je 250m
		gpsRangeValue = (TextView) findViewById(R.id.seekBarValue); // added
		gpsRange.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar gpsRange) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onStartTrackingTouch(SeekBar gpsRange) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onProgressChanged(SeekBar gpsRange, int progress,
                                          boolean fromUser) {
                // TODO Auto-generated method stub
                if (progress > 0) {
                    gpsRangeValue.setText(String.valueOf(progress)
                            + "0m");
                } else {
                    gpsRangeValue.setText(String.valueOf(progress)
                            + "m");
                }
            }
        });
		/*
		 * Set string builder for recording
		 */

		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.information));
		builder.setMessage(getString(R.string.warning_recording));

		builder.setPositiveButton(getString(R.string.yes),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog,
							int which) {
						Intent intent = new Intent(
								getBaseContext(),
								RecordActivity.class);
						startActivity(intent);
						dialog.dismiss();
					}
				});

		builder.setNegativeButton(getString(R.string.no),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog,
							int which) {
						dialog.dismiss();
					}
				});

		buttonRecord.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog alert = builder.create();
				alert.show();
				}
			});
		

		/*
		 * Changing settings and saving them
		 */

		buttonSaveSettings.setOnClickListener(myhandler1);

	}

	// Uložení nastavení po stisknutí tlačítka
	View.OnClickListener	myhandler1	= new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									DataSettings.setTextPersonName(textPersonName
											.getText()
											.toString());
									DataSettings.setTextPhoneNumber(textPhoneNumber
											.getText()
											.toString());
									DataSettings.setTextPersonAddress(textAddress
											.getText()
											.toString());
                                    DataSettings.setMonitoring(monitoringSwitch.isChecked());
                                    DataSettings.setSendSMS(smsCheck.isChecked());
									DataSettings.setGpsRange((int) gpsRange
											.getProgress());
									try {

										OutputStreamWriter out = new OutputStreamWriter(
												openFileOutput(
														"settings.txt",
														0));

										out.write(DataSettings
												.getAllDataAsString());

										out.close();
										Toast.makeText(
												getApplicationContext(),
												getString(R.string.settings_saved)
														+ DataSettings
																.getAllDataAsString(),
												Toast.LENGTH_LONG)
												.show();
									}

									catch (Throwable t) {
										Toast.makeText(
												getApplicationContext(),
												"Exception: "
														+ t.toString(),
												Toast.LENGTH_LONG)
												.show();

									}
								}
							};

	@Override
	protected void onResume() {
		super.onResume();
		/*
		 * Set setting from the database
		 */
		textPersonName.setText(DataSettings.getTextPersonName());
		textPhoneNumber.setText(DataSettings.getTextPhoneNumber());
		textAddress.setText(DataSettings.getTextPersonAddress());
		gpsRange.setProgress((int) (DataSettings.getGpsRange()));
        smsCheck.setChecked(DataSettings.isSendSMS());
        monitoringSwitch.setChecked(DataSettings.isMonitoring());

	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is
		// present.
		// getMenuInflater().inflate(R.menu.settings, menu);
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

}
