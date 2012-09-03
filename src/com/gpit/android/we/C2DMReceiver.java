package com.gpit.android.we;

import java.net.URL;
import java.net.URLEncoder;

import org.json.JSONObject;

import com.broov.player.VideoPlayer;
import com.gpit.android.we.common.Constants;
import com.gpit.android.we.common.HttpFeeder;
import com.gpit.android.we.common.Log;
import com.gpit.android.we.common.SessionStore;
import com.gpit.android.we.common.WebApiLevel;
import com.gpit.android.we.data.VideoInfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings.Secure;

public class C2DMReceiver extends BroadcastReceiver {

	public static String Tag;
	boolean bRuning = false;
	
	
	public void onReceive(final Context aContext, Intent intent) {
		// TODO Auto-generated method stub
		String action = intent.getAction();
		Tag = getClass().getSimpleName();
		if (!SessionStore.isSessionValide(aContext))
			return;
		if ("com.google.android.c2dm.intent.REGISTRATION".equals(action)) {
			final String registrationId = intent.getStringExtra("registration_id");
			if (registrationId != null) {
				SessionStore.setPushRegID(aContext, registrationId);
				if (!SessionStore.isRegisteredPuchClientToWe(aContext)) {
					new Thread(new Runnable() {

						
						public void run() {
							// TODO Auto-generated method stub
							try {
								sendRegistrationIdToServer(
										aContext,
										WeApplication.APP_USER.strEmail,
										Secure.getString(aContext.getContentResolver(), Secure.ANDROID_ID),
										registrationId
										);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								Log.v(Tag, e.getMessage());
							}
						}
					}).start();
				}
			}
		} else if ("com.google.android.c2dm.intent.RECEIVE".equals(action)) {
			String strMsg = intent.getStringExtra("message");
			final String strVideoID = intent.getStringExtra("video_id");
			int iVideoId = 0;
			try {
				iVideoId = Integer.parseInt(strVideoID);
				if (iVideoId <= 0)
					return;
				final int videoId = iVideoId;
				new Thread(new Runnable() {
					
					public void run() {
						// TODO Auto-generated method stub
						try {
							VideoInfo info = getVideoInfo(aContext, videoId);
							VideoInfo.insertToDB(aContext, info);
							VideoPlayer.playVideoFromPush(info);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}).start();
			} catch (Exception e) {
				// TODO: handle exception
			}
			Log.v(Tag, strMsg);
			Log.v(Tag, strVideoID);
		}
	}
	
	/**
	 * @throws Exception 
	 * 
	 */
	public static VideoInfo getVideoInfo(Context aContext, int aVideoId) throws Exception {
		String strUrl = String.format(Constants.VIDEOINFO_GET, aVideoId);
		URL url = new URL(strUrl);
		JSONObject jObjRet = HttpFeeder.FEEDER.getResponseJSONObjForGetMethod(false, url, WebApiLevel.WEBAPI_HIGH_SECURITY, true, null, null, null);
		int iRet = jObjRet.getInt(Constants.KEY_STATAUS);
		if (iRet != 1)
			return null;
		JSONObject jObjInfo = jObjRet.getJSONObject(Constants.KEY_RESULT);
		return new VideoInfo(jObjInfo);
	}
	
	/**
	 * register device to server
	 * @param aDeviceId
	 * @param aRegId
	 * @throws Exception
	 */
	public static void sendRegistrationIdToServer(Context aContext, String aUserEmail, String aDeviceId, String aRegId) throws Exception {
		String strUrl = String.format(Constants.SETTOKEN_GET, aUserEmail, aDeviceId, URLEncoder.encode(aRegId));
		URL url = new URL(strUrl);
		StringBuffer strRet = HttpFeeder.FEEDER.getResponseForGetMethod(false, url, WebApiLevel.WEBAPI_HIGH_SECURITY, true, null, null, null);
		JSONObject jObj = new JSONObject(strRet.toString());
		int iRet = jObj.getInt(Constants.KEY_STATAUS);
		if (iRet == 1) {
			SessionStore.setRegisteredPuchClientToWe(aContext, true);
		} else {
			SessionStore.setRegisteredPuchClientToWe(aContext, false);
		}
	}
}