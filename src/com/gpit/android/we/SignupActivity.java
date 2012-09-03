package com.gpit.android.we;

import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import com.broov.player.VideoPlayer;
import com.gpit.android.we.common.Constants;
import com.gpit.android.we.common.HttpFeeder;
import com.gpit.android.we.common.SessionStore;
import com.gpit.android.we.common.Utils;
import com.gpit.android.we.common.WebApiLevel;
import com.gpit.android.we.data.SignupInfo;
import com.gpit.android.we.data.UserInfo;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
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

public class SignupActivity extends Activity implements OnClickListener, OnEditorActionListener, OnKeyListener {

	EditText etNickName;
	EditText etEmail;
	EditText etPwd;
	EditText etConfirmPwd;
	SignupInfo mSignupInfo;
	Dialog 	mSpinner;
	public static SignupActivity Instance;
	
	
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.signup);
		initUI();
	}
	
	/**
	 * Initialize UI components
	 */
	public void initUI() {
		Instance = SignupActivity.this;
		mSpinner = new Dialog(SignupActivity.this, R.style.NewDialog);
		mSpinner.addContentView(new ProgressBar(SignupActivity.this), new LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		mSpinner.setOnKeyListener(SignupActivity.this);
		Button btnSignup = (Button)findViewById(R.id.signup_button);
		btnSignup.setOnClickListener(SignupActivity.this);
		etNickName = (EditText)findViewById(R.id.nickname_editText);
		etEmail = (EditText)findViewById(R.id.email_editText);
		etPwd = (EditText)findViewById(R.id.pwd_editText);
		etConfirmPwd = (EditText)findViewById(R.id.confirm_pwd_editText);
		etConfirmPwd.setOnEditorActionListener(SignupActivity.this);
		mSignupInfo = new SignupInfo();
	}

	
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if (checkValidate())
			procSignup();
	}

	
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		// TODO Auto-generated method stub
		if (actionId == EditorInfo.IME_ACTION_SEND) {
			if (checkValidate())
				procSignup();
		}
		return false;
	}
	
	/**
	 * process signup
	 */
	public void procSignup() {
		new Thread(new Runnable() {
			
			
			public void run() {
				// TODO Auto-generated method stub
				String strUrl;
				boolean bRet = true;
				URL url = null;
				JSONObject jObj = null;
				strUrl = String.format(Constants.SIGNUP_GET,
						mSignupInfo.strNickName,
						mSignupInfo.strPassword,
						mSignupInfo.strEmail
						);
				try {
					url = new URL(strUrl);
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				try {
					jObj = HttpFeeder.FEEDER.getResponseJSONObjForGetMethod(false, url, WebApiLevel.WEBAPI_HIGH_SECURITY, true, null, null, null);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					bRet = false;
				}

				WeApplication.APP_USER = new UserInfo();
				WeApplication.APP_USER.strEmail = mSignupInfo.strEmail;
				WeApplication.APP_USER.strName = mSignupInfo.strNickName;
				if (bRet) {
					try {
						bRet = parseSignupResult(jObj);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						bRet = false;
					}
				}
	
				if (bRet) {
					SessionStore.setUserID(SignupActivity.this, WeApplication.APP_USER.strEmail);
					SessionStore.setUserName(SignupActivity.this, WeApplication.APP_USER.strName);
				}

				final boolean bSuccess = bRet;
				runOnUiThread(new Runnable() {
					
					
					public void run() {
						// TODO Auto-generated method stub
						hideWaitingDlg();
						if (!bSuccess) {
							Utils.showAlertMsg(getString(R.string.signup_fail_title), getString(R.string.signup_fail_desc), SignupActivity.this, false);
						} else {
							if (LoginActivity.Instance != null)
								LoginActivity.Instance.finish();
							gotoMainActivity();
						}
					}
				});
			}
		}).start();
		showWaitingDlg();
	}
	
	/**
	 * goto main activity
	 */
	public void gotoMainActivity() {
//		Intent intent = new Intent(LoginActivity.this, VideoPlayerActivity.class);
		Intent intent = new Intent(SignupActivity.this, VideoPlayer.class);
		startActivity(intent);
		finish();
	}

	/**
	 * parse login result
	 * @param aJObj
	 * @param aUserInfo
	 * @return
	 * @throws JSONException
	 */
	public boolean parseSignupResult(JSONObject aJObj) throws JSONException {
		int aStatus = aJObj.getInt(Constants.KEY_STATAUS);
		if (aStatus != 1)
			return false;
		return true;
	}

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

	/**
	 * check validation for input email and password
	 * @return
	 */
	public boolean checkValidate() {
		String strVal = null;
		strVal = etNickName.getText().toString().trim();
		if (strVal.length() == 0) {
			etNickName.setError(getText(R.string.empty_nick_name));
			return false;
		}
		mSignupInfo.strNickName = strVal;
		strVal = etEmail.getText().toString().trim();
		if (strVal.length() == 0) {
			etEmail.setError(getText(R.string.empty_email));
			return false;
		}
		if (!Utils.checkValideEmail(strVal)) {
			etEmail.setError(getText(R.string.invalidate_email));
			return false;
		}
		mSignupInfo.strEmail = strVal;
		strVal = etPwd.getText().toString().trim();
		if (strVal.length() == 0) {
			etPwd.setError(getText(R.string.empty_password));
			return false;
		}
		mSignupInfo.strPassword = strVal;
		strVal = etConfirmPwd.getText().toString().trim();
		if (strVal.length() == 0) {
			etConfirmPwd.setError(getText(R.string.empty_confirm_password));
			return false;
		}
		mSignupInfo.strConfirmPassword = strVal;
		if (mSignupInfo.strPassword.compareTo(mSignupInfo.strConfirmPassword) != 0) {
			etConfirmPwd.setError(getText(R.string.no_match_password));
			return false;
		}
		return true;
	}

	
	public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK)
			return true;
		return false;
	}
}
