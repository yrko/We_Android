package com.gpit.android.we.sqlite;

import com.gpit.android.we.WeApplication;

import android.content.Context;

public class DatabaseMgr {
	
	/**
	 * table list
	 */
	public static final String TBL_VIDEO 		= "tbl_video";

	/**
	 * tbl_video table
	 */
	public static final String V_F_UID = "uid";
	public static final String V_F_ID = "video_id";
	public static final String V_F_POST_EMAIL = "posted_email";
	public static final String V_F_SIZE = "video_size";
	public static final String V_F_ADDR = "address";
	public static final String V_F_NICK_NAME = "nickname";
	public static final String V_F_VIDEO_PATH = "video_path";
	public static final String V_F_THUMB_PATH = "thumbnail_path";
	public static final String V_F_TITLE = "video_title";
	public static final String V_F_POST_DATE = "post_datetime";
	public static final String V_F_LNG = "longitude";
	public static final String V_F_LAT = "latitude";
	public static final String V_F_DOWNLOADED_STAT = "downloaded";
	public static final String V_F_DOWNLOADED_PATH = "downloaded_path";
	
	public static String Tag;
	
	public DatabaseMgr() {
		Tag = getClass().getSimpleName();
	}
	
	/**
	 * if DB_HANDLER is null, this will create.
	 * @param context
	 * @return
	 */
	public static DatabaseAdapter getDBHandle(Context context) {
		if(WeApplication.DB_HANDLER == null) {
			WeApplication.DB_HANDLER = new DatabaseAdapter(context);
		}
		if(!WeApplication.DB_HANDLER.isOpen())
			WeApplication.DB_HANDLER.openDatabase();
		
		return WeApplication.DB_HANDLER;
	}
	
	/**
	 * get escape string for sql query
	 * @param aString
	 * @return
	 */
	public static String escapeString(String aString) {
		if (aString == null)
			return null;
		return aString.replace("'", "''");
	}
	
	/**
	 * if DB_HANDLER is null, this will create.
	 * @return
	 */
	public static DatabaseAdapter getDBHandle() {
		if (WeApplication.DB_HANDLER == null)
			return null;
		if(!WeApplication.DB_HANDLER.isOpen())
			WeApplication.DB_HANDLER.openDatabase();
		return WeApplication.DB_HANDLER;
	}
	
	/**
	 * close DB handler
	 */
	public static void closeDBHandle() {
		if(WeApplication.DB_HANDLER == null)
			return;
		
		if(!WeApplication.DB_HANDLER.isOpen())
			return;
		
		WeApplication.DB_HANDLER.close();
	}
}
