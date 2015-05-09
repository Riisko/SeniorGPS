package seniorgps;

import risko.seniorgps.R;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

public class HelpActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_help);

		// wait few seconds and get back to last activity
		// showProgress(true);
		UserWaitingTask userWaitingTask = new UserWaitingTask();
		userWaitingTask.execute((Void) null);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is
		// present.
		// getMenuInflater().inflate(R.menu.help, menu);
		menu.add(1, 1, 1, (getString(R.string.app_name)));
		menu.add(2, 2, 2, (getString(R.string.action_settings)));
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Log in to access settings
		//
		// QUICK TIP:
		switch (item.getItemId()) {
		case 1:
			Intent intent_main = new Intent(getBaseContext(),
					MainActivity.class);
			startActivity(intent_main);
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
	 * Represents an asynchronous waiting time to get back to MainActivity in
	 * order to continue with the main process.
	 */
	public class UserWaitingTask extends AsyncTask<Void, Void, Boolean> {
		@Override
		protected Boolean doInBackground(Void... params) {
			int waitingTime = 60; // waiting for X seconds

			while (waitingTime > 0) {
				waitingTime--;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					return false;
				}
			}

			// Wrong password
			return false;
		}

		@Override
		protected void onPostExecute(final Boolean success) {
			finish();
		}

		@Override
		protected void onCancelled() {
			finish();
		}
	}

}
