package com.gpit.android.we;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import com.broov.player.VideoPlayer;
import com.gpit.android.we.common.Constants;
import com.gpit.android.we.common.HttpFeeder;
import com.gpit.android.we.common.MyLocation;
import com.gpit.android.we.common.SessionStore;
import com.gpit.android.we.common.MyLocation.LocationResult;
import com.gpit.android.we.common.Utils;
import com.gpit.android.we.common.WebApiLevel;
import com.gpit.android.we.data.UserInfo;
import com.gpit.android.we.data.VideoInfo;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnKeyListener;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class LoginActivity extends Activity implements OnEditorActionListener, OnKeyListener, OnClickListener {

	EditText etEmail;
	EditText etPwd;
	Dialog 	mSpinner;
	MyLocation 	myPlaceLocation;
	public 	static Location		currentLocation;
	public static String TAG = "";
	public static LoginActivity Instance;
	
	
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.login);
		if (WeApplication.APP_USER != null) {
			gotoMainActivity();
			return;
		}
		initUI();
		TAG = getClass().getCanonicalName();
	}
	
	/**
	 * Initialize directory
	 */
	public void initDir() {
		File f = new File(Constants.getVideoPath());
		Utils.deleteRecursive(f);
		f.mkdirs();
	}
	
	/**
	 * goto main activity
	 */
	public void gotoMainActivity() {
//		Intent intent = new Intent(LoginActivity.this, VideoPlayerActivity.class);
		Intent intent = new Intent(LoginActivity.this, VideoPlayer.class);
		startActivity(intent);
		finish();
	}
	
	/**
	 * Initialize UI components
	 */
	public void initUI() {
		Instance = LoginActivity.this;
		mSpinner = new Dialog(LoginActivity.this, R.style.NewDialog);
		mSpinner.addContentView(new ProgressBar(LoginActivity.this), new LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		mSpinner.setOnKeyListener(LoginActivity.this);
		etEmail = (EditText)findViewById(R.id.email_editText);
		etPwd = (EditText)findViewById(R.id.password_editText);
		etPwd.setOnEditorActionListener(LoginActivity.this);
		Button btnLogin = (Button)findViewById(R.id.login_button);
		btnLogin.setOnClickListener(LoginActivity.this);
		Button btnSignup = (Button)findViewById(R.id.signup_button);
		btnSignup.setOnClickListener(LoginActivity.this);
		getCurrentLocation();
	}
	
	/**
	 * check validation for input email and password
	 * @return
	 */
	public boolean checkValidate() {
		String strVal = null;
		strVal = etEmail.getText().toString().trim();
		if (strVal.length() == 0) {
			etEmail.setError(getText(R.string.empty_email));
			return false;
		}
		if (!Utils.checkValideEmail(strVal)) {
			etEmail.setError(getText(R.string.invalidate_email));
			return false;
		}
		strVal = etPwd.getText().toString().trim();
		if (strVal.length() == 0) {
			etPwd.setError(getText(R.string.empty_password));
			return false;
		}
		return true;
	}

	/**
	 * process for login with email and password
	 */
	public void procLogin() {
		final String strEmail = etEmail.getText().toString();
		final String strPwd = etPwd.getText().toString();
		new Thread(new Runnable() {
			
			
			public void run() {
				// TODO Auto-generated method stub
				initDir();
				String strUrl;
				boolean bRet = true;
				URL url = null;
				JSONObject jObj = null;
				if (currentLocation != null) {
					strUrl = String.format(Constants.LOGIN_GET,
							strEmail,
							strPwd,
							Secure.getString(getContentResolver(), Secure.ANDROID_ID),
							SessionStore.getPushRegId(LoginActivity.this),
							currentLocation.getLongitude(),
							currentLocation.getLatitude()
							);
					try {
						url = new URL(strUrl);
					} catch (MalformedURLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					strUrl = String.format(Constants.LOGIN_GET,
							strEmail,
							strPwd,
							Secure.getString(getContentResolver(), Secure.ANDROID_ID),
							SessionStore.getPushRegId(LoginActivity.this),
							0,
							0
							);
					try {
						url = new URL(strUrl);
					} catch (MalformedURLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				try {
					jObj = HttpFeeder.FEEDER.getResponseJSONObjForGetMethod(false, url, WebApiLevel.WEBAPI_HIGH_SECURITY, true, null, null, null);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					bRet = false;
				}

				WeApplication.APP_USER = new UserInfo();
				WeApplication.APP_USER.strEmail = strEmail;
				if (bRet) {
					try {
						bRet = parseLoginResult(jObj, WeApplication.APP_USER);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						bRet = false;
					}
				}
				
				if (bRet) {
					SessionStore.setUserID(LoginActivity.this, WeApplication.APP_USER.strEmail);
					SessionStore.setUserName(LoginActivity.this, WeApplication.APP_USER.strName);
				}
				
				final boolean bSuccess = bRet;
				runOnUiThread(new Runnable() {
					
					
					public void run() {
						// TODO Auto-generated method stub
						hideWaitingDlg();
						if (!bSuccess) {
							Utils.showAlertMsg(getString(R.string.login_fail_title), getString(R.string.login_fail_desc), LoginActivity.this, false);
						} else {
							if (SignupActivity.Instance != null)
								SignupActivity.Instance.finish();
							gotoMainActivity();
						}
					}
				});
			}
		}).start();
		showWaitingDlg();
	}
	
	/**
	 * parse login result
	 * @param aJObj
	 * @param aUserInfo
	 * @return
	 * @throws JSONException
	 */
	public boolean parseLoginResult(JSONObject aJObj, UserInfo aUserInfo) throws JSONException {
		int aStatus = aJObj.getInt(Constants.KEY_STATAUS);
		if (aStatus != 1)
			return false;
		JSONObject jObj = aJObj.getJSONObject(Constants.KEY_RESULT);
		aUserInfo.strName = jObj.getString(VideoInfo.FIELD_NICK_NAME);
		return true;
	}
	
	
	public void onBackPressed() {
		// TODO Auto-generated method stub
		finish();
		android.os.Process.killProcess(android.os.Process.myPid());
	}
	
	/**
	 * get My place
	 */
	public void getCurrentLocation() {
		myPlaceLocation = new MyLocation();
	    myPlaceLocation.getLocation(this, locationResult);
	}
	
	public LocationResult locationResult = new LocationResult(){
	    
	    public void gotLocation(final Location location){
	    	//	store the current user location
	    	currentLocation = location;
	    }
	};

    /**
     * Show Waiting Dialog
     */
    public void showWaitingDlg() {
    	if (!mSpinner.isShowing())
    		mSpinner.show();
    }
    
    /**
     * Hide Waiting Dialog
     */
    public void hideWaitingDlg() {
    	if (mSpinner.isShowing())
    		mSpinner.cancel();
    }

	
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		// TODO Auto-generated method stub
		if (actionId == EditorInfo.IME_ACTION_SEND) {
			if (checkValidate())
				procLogin();
		}
		return false;
	}

	
	public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK)
			return true;
		return false;
	}
	
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.login_button:
			if (checkValidate())
				procLogin();
			break;
		case R.id.signup_button:
			Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
			startActivity(intent);
			break;
		}
	}
	
	/**
	 * start push notification service
	 * @param aContext
	 */
	public static void registerPushService(Context aContext) {
		Intent intent = new Intent("com.google.android.c2dm.intent.REGISTER");
		intent.putExtra("app", PendingIntent.getBroadcast(aContext, 0, new Intent(), 0));
		intent.putExtra("sender", Constants.PUSH_ACCOUNT);
		aContext.startService(intent);
	}
}
