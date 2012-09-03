package com.gpit.android.we.common;

import com.gpit.android.we.data.UserInfo;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class SessionStore {
	private static final String KEY = "we_Session";
	private static final String USER_ID = "user_id";
	private static final String USER_NAME = "user_name";
	private static final String PUSH_REG_ID = "push_reg_id";
	private static final String CUR_POS = "current_position";
	private static final String PUSH_REGISTERED_TO_WE = "_resigered_to_we";

	/**
	 * get shared prefrence
	 * @param aContext
	 * @return
	 */
	public static SharedPreferences getPrefrences(Context aContext) {
		return aContext.getSharedPreferences(KEY, Context.MODE_PRIVATE);
	}
	
	/**
	 * set push register id
	 * @param aContext
	 * @param aPushRegId
	 */
	public static void setPushRegID(Context aContext, String aPushRegId) {
		Editor editor = 
				aContext.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit();
		editor.putString(PUSH_REG_ID, aPushRegId);
		editor.commit();
	}
	
	/**
	 * get push register id
	 * @param aContext
	 * @return
	 */
	public static String getPushRegId(Context aContext) {
		SharedPreferences savedSession =
				aContext.getSharedPreferences(KEY, Context.MODE_PRIVATE);
		return savedSession.getString(PUSH_REG_ID, "");
	}
	
	/**
	 * set user id
	 * @param aContext
	 * @param aUserId
	 */
	public static void setUserID(Context aContext, String aUserId) {
		Editor editor = 
				aContext.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit();
		editor.putString(USER_ID, aUserId);
		editor.commit();
	}
	
	/**
	 * get user name
	 * @param aContext
	 * @return
	 */
	public static String getUserName(Context aContext) {
		SharedPreferences savedSession =
				aContext.getSharedPreferences(KEY, Context.MODE_PRIVATE);
		return savedSession.getString(USER_NAME, "");
	}
	
	/**
	 * set user name
	 * @param aContext
	 * @param aUserName
	 */
	public static void setUserName(Context aContext, String aUserName) {
		Editor editor = 
				aContext.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit();
		editor.putString(USER_NAME, aUserName);
		editor.commit();
	}
	
	/**
	 * get user name
	 * @param aContext
	 * @return
	 */
	public static String getCurrentPosition(Context aContext) {
		SharedPreferences savedSession =
				aContext.getSharedPreferences(KEY, Context.MODE_PRIVATE);
		return savedSession.getString(CUR_POS, null);
	}
	
	/**
	 * set user name
	 * @param aContext
	 * @param aUserName
	 */
	public static void setCurrentPosition(Context aContext, String aCurPos) {
		Editor editor = 
				aContext.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit();
		editor.putString(CUR_POS, aCurPos);
		editor.commit();
	}
	
	/**
	 * get user id
	 * @param aContext
	 * @return
	 */
	public static String getUserId(Context aContext) {
		SharedPreferences savedSession =
				aContext.getSharedPreferences(KEY, Context.MODE_PRIVATE);
		return savedSession.getString(USER_ID, "");
	}
	
	/**
	 * check register for push client to synkmonkey server
	 * @param aContext
	 * @return
	 */
	public static boolean isRegisteredPuchClientToWe(Context aContext) {
		SharedPreferences savedSession =
				aContext.getSharedPreferences(KEY, Context.MODE_PRIVATE);
		return savedSession.getBoolean(PUSH_REGISTERED_TO_WE, false);
	}
	
	/**
	 * set for push client register to SynkMonkey server
	 * @param aContext
	 * @param aSet
	 */
	public static void setRegisteredPuchClientToWe(Context aContext, boolean aSet) {
		Editor editor = 
				aContext.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit();
		editor.putBoolean(PUSH_REGISTERED_TO_WE, aSet);
		editor.commit();
	}
	
	/**
	 * check authentication status
	 * @param aContext
	 * @return
	 */
	public static boolean isSessionValide(Context aContext) {
		SharedPreferences savedSession =
				aContext.getSharedPreferences(KEY, Context.MODE_PRIVATE);
		String strUserId = savedSession.getString(USER_ID, null);
		if (strUserId == null || strUserId.trim().length() == 0)
			return false;
		return true;
	}
	
	/**
	 * get UserInfo
	 * @param aContext
	 * @return
	 */
	public static UserInfo restoreUserInfo(Context aContext) {
		UserInfo userInfo = new UserInfo();
		String strVal = null;
		strVal = getUserId(aContext);
		if (strVal == null || strVal.trim().length() == 0)
			return null;
		userInfo.strEmail = strVal; 
		strVal = getUserName(aContext);
		if (strVal == null || strVal.trim().length() == 0)
			return null;
		userInfo.strName = strVal; 
		return userInfo;
	}
}
