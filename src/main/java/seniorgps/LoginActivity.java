package seniorgps;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import seniorgps.config.DataSettings;

import risko.seniorgps.R;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class LoginActivity extends Activity {
	/**
	 * A dummy authentication store containing known user names and passwords.
	 * TODO: remove after connecting to a real authentication system.
	 */
	
	private static String[]	CREDENTIALS		= new String[] {
			DataSettings.getUserCredentials(), "senior@gps.cz:hello" };
	
	/**
	 * The default email to populate the email field with.
	 */
	public static final String	EXTRA_EMAIL		= "com.example.android.authenticatordemo.extra.EMAIL";

	/**
	 * Keep track of the login task to ensure we can cancel it if requested.
	 */
	private UserLoginTask		mAuthTask		= null;
	private UserWaitingTask		mWaitingTask	= null;

	// Values for email and password at the time of the login attempt.
	private String			mEmail;
	private String			mPassword;

	// UI references.
	private EditText			mEmailView;
	private EditText			mPasswordView;
	private View			mLoginFormView;
	private View			mLoginStatusView;
	private TextView			mLoginStatusMessageView;

	private boolean			waitingForInput	= true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_login);

		// Set up the login form.
		mPasswordView = (EditText) findViewById(R.id.password);
		mPasswordView
				.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView textView,
							int id, KeyEvent keyEvent) {
						if (id == R.id.login
								|| id == EditorInfo.IME_NULL) {
							try {
								attemptLogin();
							} catch (FileNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							return true;
						}
						return false;
					}
				});

		mLoginFormView = findViewById(R.id.login_form);
		mLoginStatusView = findViewById(R.id.login_status);
		mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

		findViewById(R.id.sign_in_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						try {
							attemptLogin();
						} catch (FileNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				});

		// wait few seconds and get back to last activity
		// showProgress(true);
		mWaitingTask = new UserWaitingTask();
		mWaitingTask.execute((Void) null);
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.login, menu);
		return true;
	}

	/**
	 * Attempts to sign in or register the account specified by the login
	 * form. If there are form errors (invalid email, missing fields, etc.),
	 * the errors are presented and no actual login attempt is made.
	 */
	
	
	
	
	public void attemptLogin() throws FileNotFoundException {
		if (mAuthTask != null) {
			return;
		}

		// Reset errors.
		mPasswordView.setError(null);

		// Store values at the time of the login attempt.
		mEmail = "senior@gps.cz";
		mPassword = mPasswordView.getText().toString();

		boolean cancel = false;
		View focusView = null;

		// Check for a valid password.
		if (TextUtils.isEmpty(mPassword)) {
			mPasswordView
					.setError(getString(R.string.error_field_required));
			focusView = mPasswordView;
			cancel = true;
		} else if (mPassword.length() < 4) {
			mPasswordView
					.setError(getString(R.string.error_invalid_password));
			focusView = mPasswordView;
			cancel = true;
		}

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			waitingForInput = true;
			focusView.requestFocus();
		} else {
			// Show a progress spinner, and kick off a background task to
			// perform the user login attempt.
			mLoginStatusMessageView
					.setText(R.string.login_progress_signing_in);
			showProgress(true);
			waitingForInput = false;
			//first time login = default settings
			if(defaultSettings()) {
				DataSettings.setUserEmail(mEmail.toString());
				DataSettings.setUserPassword(mPassword.toString());
                CREDENTIALS = new String[]{DataSettings.getUserCredentials(), "senior@gps.cz" + mPassword.toString()};
                try {

					OutputStreamWriter out = new OutputStreamWriter(
							openFileOutput(
									"login.txt",
									0));

					out.write(DataSettings
							.getLoginDataAsString());

					out.close();
					Toast.makeText(
							getApplicationContext(),
							getString(R.string.settings_saved)
									+ DataSettings
											.getLoginDataAsString(),
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
                mAuthTask = new UserLoginTask();
				mAuthTask.execute((Void) null);
			} else {
				mAuthTask = new UserLoginTask();
				mAuthTask.execute((Void) null);
			}
		}
	}
	
	//checks if settings have been changed yet
	private boolean defaultSettings() {
		String password = DataSettings.getUserPassword();
		if(password == "hello") return true;
		return false;
	}

	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which
		// allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(
					android.R.integer.config_shortAnimTime);

			mLoginStatusView.setVisibility(View.VISIBLE);
			mLoginStatusView.animate().setDuration(shortAnimTime)
					.alpha(show ? 1 : 0)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(
								Animator animation) {
							mLoginStatusView
									.setVisibility(show ? View.VISIBLE
											: View.GONE);
						}
					});

			mLoginFormView.setVisibility(View.VISIBLE);
			mLoginFormView.animate().setDuration(shortAnimTime)
					.alpha(show ? 0 : 1)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(
								Animator animation) {
							mLoginFormView
									.setVisibility(show ? View.GONE
											: View.VISIBLE);
						}
					});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply
			// show
			// and hide the relevant UI components.
			mLoginStatusView.setVisibility(show ? View.VISIBLE
					: View.GONE);
			mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}

	/**
	 * Represents an asynchronous login/registration task used to authenticate
	 * the user.
	 */
	public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
		@Override
		protected Boolean doInBackground(Void... params) {

			for (String credential : CREDENTIALS) {
				String[] pieces = credential.split(":");
				if (pieces[0].equals(mEmail)) {
					// Account exists, return true if the password
					// matches.
					waitingForInput = false;
					return pieces[1].equals(mPassword);
				}
			}
			// Wrong password
			return false;
		}

		@Override
		protected void onPostExecute(final Boolean success) {
			mAuthTask = null;
			mWaitingTask = null;
			showProgress(false);

			if (success) {
				Intent intent = new Intent(getBaseContext(),
						SettingsActivity.class);
				startActivity(intent);
				finish();
			} else {
				mPasswordView
						.setError(getString(R.string.error_incorrect_password));
				mPasswordView.requestFocus();
			}
		}

		@Override
		protected void onCancelled() {
			mAuthTask = null;
			mWaitingTask = null;
			showProgress(false);
		}
	}

	/**
	 * Represents an asynchronous waiting time to get back to MainActivity in
	 * order to continue with the main process.
	 */
	public class UserWaitingTask extends AsyncTask<Void, Void, Boolean> {
		@Override
		protected Boolean doInBackground(Void... params) {
			int waitingTime = 20; // waiting for X seconds

			while (waitingTime > 0) {
				waitingTime--;
				try {
					if (waitingForInput) {
						Thread.sleep(1000);
					}
				} catch (InterruptedException e) {
					return false;
				}
			}

			// Wrong password
			return false;
		}

		@Override
		protected void onPostExecute(final Boolean success) {
			mWaitingTask = null;
			showProgress(false);
			finish();
		}

		@Override
		protected void onCancelled() {
			mWaitingTask = null;
			showProgress(false);
			finish();
		}
	}

}