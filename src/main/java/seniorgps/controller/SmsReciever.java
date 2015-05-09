package seniorgps.controller;

import seniorgps.MainActivity;
import seniorgps.config.DataSettings;
import seniorgps.model.DataStorage;

import risko.seniorgps.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

public class SmsReciever extends BroadcastReceiver {


    public static String mAddressOutput = "";
	// Get the object of SmsManager
	final SmsManager	sms	= SmsManager.getDefault();

	@Override
	public void onReceive(Context context, Intent intent) {

		// Retrieves a map of extended data from the intent.
		final Bundle bundle = intent.getExtras();

        if(mAddressOutput == ""){
            mAddressOutput = DataStorage.getLastPosition().getDataLat()
                    + ";"
                    + DataStorage.getLastPosition().getDataLong();
        }
		try {

			if (bundle != null) {

				final Object[] pdusObj = (Object[]) bundle.get("pdus");

				for (int i = 0; i < pdusObj.length; i++) {

					SmsMessage currentMessage = SmsMessage
							.createFromPdu((byte[]) pdusObj[i]);
					String phoneNumber = currentMessage
							.getDisplayOriginatingAddress();

					String senderNum = phoneNumber;
					String message = currentMessage
							.getDisplayMessageBody();

					Log.i("SmsReceiver", "senderNum: " + senderNum
							+ "; message: " + message);

					// Show Alert
					int duration = Toast.LENGTH_LONG;
					Toast toast = Toast
							.makeText(context,
									"senderNum: "
											+ senderNum
											+ ", message: "
											+ message,
									duration);
					toast.show();
					/**
					 * Reading SMS - pokud je přítomna SMS s textem
					 * "KDE JSI", naplnit počítadlo = poslat SMS
					 */
					if (message.toLowerCase().contains(
							context.getResources().getString(
									R.string.WHERE_ARE_YOU))) {
						SmsManager sms = SmsManager.getDefault(); // DataSettings.getTextAlertSMS().toString();
                        String contentSMS = context.getResources()
								.getString(R.string.response)
								+ mAddressOutput
								+ context.getResources()
										.getString(R.string.for_update_send);
						sms.sendTextMessage(DataSettings
								.getTextPhoneNumber()
								.toString(), null,
								contentSMS.replace("\n", "")
										.replace("\r", ""),
								null, null);
					}

				} // end for loop
			} // bundle is null

		} catch (Exception e) {
			Log.e("SmsReceiver", "Exception smsReceiver" + e);

		}
	}
}
