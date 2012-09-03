package com.gpit.android.we;


import com.bugsense.trace.BugSenseHandler;
import com.gpit.android.we.common.Constants;
import com.gpit.android.we.common.HttpFeeder;
import com.gpit.android.we.common.Log;
import com.gpit.android.we.common.SessionStore;
import com.gpit.android.we.data.UserInfo;
import com.gpit.android.we.sqlite.DatabaseAdapter;
import com.gpit.android.we.sqlite.DatabaseMgr;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.os.Build;

public class WeApplication extends Application {

	public static boolean isDebugMode;
	public static DatabaseAdapter DB_HANDLER;
	public static UserInfo APP_USER;
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		isDebugMode = ( 0 != ( getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE ) );
		HttpFeeder.init(getApplicationContext());
		BugSenseHandler.setup(getApplicationContext(), Constants.BUG_SENSE_KEY);
		Constants.DB_PATH = getDatabasePath(Constants.DB_NAME).getAbsolutePath();
		DB_HANDLER = new DatabaseAdapter(WeApplication.this);
		initDB();
		StringBuffer buf = new StringBuffer();
		buf.append("VERSION.RELEASE {"+Build.VERSION.RELEASE+"}");
		buf.append("\nVERSION.INCREMENTAL {"+Build.VERSION.INCREMENTAL+"}");
		buf.append("\nVERSION.SDK_INT {"+Build.VERSION.SDK_INT+"}");
		buf.append("\nFINGERPRINT {"+Build.FINGERPRINT+"}");
		buf.append("\nBOARD {"+Build.BOARD+"}");
		buf.append("\nBRAND {"+Build.BRAND+"}");
		buf.append("\nDEVICE {"+Build.DEVICE+"}");
		buf.append("\nMANUFACTURER {"+Build.MANUFACTURER+"}");
		buf.append("\nMODEL {"+Build.MODEL+"}");
		Log.v("WeApplication", buf.toString());
		APP_USER = SessionStore.restoreUserInfo(WeApplication.this);
	}
	
	/**
	 * initialize Database
	 */
	public void initDB() {
		DatabaseAdapter dbAdapter = DatabaseMgr.getDBHandle(WeApplication.this);
		if(dbAdapter == null)
			return;
		
		String strQuery = null;
		dbAdapter.executeSQL("begin");
		
		strQuery = "CREATE TABLE IF NOT EXISTS " + DatabaseMgr.TBL_VIDEO
		+ " ("
		+ String.format(" %s INTEGER PRIMARY KEY NOT NULL,", DatabaseMgr.V_F_UID)
		+ String.format(" %s INTEGER NOT NULL,", DatabaseMgr.V_F_ID)
		+ String.format(" %s VARCHAR (64) NOT NULL,", DatabaseMgr.V_F_POST_EMAIL)
		+ String.format(" %s INTEGER NOT NULL,", DatabaseMgr.V_F_SIZE)
		+ String.format(" %s VARCHAR (256) NOT NULL,", DatabaseMgr.V_F_ADDR)
		+ String.format(" %s VARCHAR (256) NOT NULL,", DatabaseMgr.V_F_NICK_NAME)
		+ String.format(" %s VARCHAR (1024) NOT NULL,", DatabaseMgr.V_F_VIDEO_PATH)
		+ String.format(" %s VARCHAR (1024) NOT NULL,", DatabaseMgr.V_F_THUMB_PATH)
		+ String.format(" %s VARCHAR (256) NOT NULL,", DatabaseMgr.V_F_TITLE)
		+ String.format(" %s VARCHAR (32) NOT NULL,", DatabaseMgr.V_F_POST_DATE)
		+ String.format(" %s INTEGER  NOT NULL DEFAULT 0,", DatabaseMgr.V_F_DOWNLOADED_STAT)
		+ String.format(" %s VARCHAR (256),", DatabaseMgr.V_F_DOWNLOADED_PATH)
		+ String.format(" %s VARCHAR (16) NOT NULL,", DatabaseMgr.V_F_LNG)
		+ String.format(" %s VARCHAR (16) NOT NULL", DatabaseMgr.V_F_LAT)
		+ ")";

		if (dbAdapter.executeSQL(strQuery) != DatabaseAdapter.FDBL_ERR_NONE) {
			dbAdapter.executeSQL("rollback");
			return;
		}
		
		dbAdapter.executeSQL("commit");
	}
}
