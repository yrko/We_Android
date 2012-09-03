package com.gpit.android.we.common;

import android.os.Environment;

public class Constants {
    
    public static String getVideoPath() {
        final String baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath();
        return baseDir + "/we_videos";
    }
    
    public static String getIntroVideoPath() {
        final String dataDir = Environment.getDataDirectory().getAbsolutePath();
        return dataDir + "/com.gpit.android.we/intro.mp4";
    }

	//public final static String VIDEO_PATH = "/sdcard/we_videos";
	//public final static String ZERO_VIDEO_FILE = "/sdcard/we_videos/zero_video.mp4";
	//public static final String INTRO_VIDEO = "/data/data/com.gpit.android.we/intro.mp4";
	
	/**
	 * DB path
	 */
	public static final String DB_NAME = "we.db";
	public static String DB_PATH = "";
	public static final int DEFAULT_INTERVAL_TIME 		= 60000;	//	min time for location update

	/**
	 * bug sense key
	 */
	public static final String BUG_SENSE_KEY= "667520d1";
	
	public final static String KEY_STATAUS = "status";
	public final static String KEY_RESULT = "results";
	
	/**
	 * push account mail
	 */
//	public static final String PUSH_ACCOUNT= "wedevelo@gmail.com";
	public static final String PUSH_ACCOUNT= "fb@hobbysew.com";
	//public static final String SERVER_IP = "http://172.20.200.2:88";
	public static final String SERVER_IP = "http://172.20.200.2:888";
//	public static final String SERVER_IP = "http://192.168.0.197";
	public static final String WEBAPI_URL = SERVER_IP + "/webapi.php?";
	public static final String SIGNUP_GET = WEBAPI_URL + "task=signup&nickname=%s&passwd=%s&email=%s";
	public static final String LOGIN_GET = WEBAPI_URL + "task=login&email=%s&passwd=%s&udid=%s&token=%s&lng=%s&lat=%s";
	public static final String DOWNLOAD_GET = WEBAPI_URL + "task=download&email=%s&count=%d";
	public static final String VIEWFLAG_GET = WEBAPI_URL + "task=viewflag&email=%s&video_id=%s";
	public static final String SETTOKEN_GET = WEBAPI_URL + "task=settoken&email=%s&udid=%s&token=%s";
	public static final String VIDEOINFO_GET = WEBAPI_URL + "task=videoinfo&video_id=%s";
//	public static final String UPLOAD_POST = SERVER_IP + "task=upload";
	public static final String UPLOAD_POST = WEBAPI_URL + "task=androidupload&mode=spec&gids=1";
	
	public static final int STAT_SYNCED = 0;
	public static final int STAT_SYNCING = 1;
	
	public final static int STOP_PLAY  = 0;
	public final static int START_PLAY  = 1;
	public final static int SYNC_VIDEO = 2;
	public final static int FWD_SKIP = 3;
	public final static int BK_SKIP = 4;
	public final static int FWD_SKIP5 = 5;
	public final static int BK_SKIP5 = 6;
	public final static int CAPTURED_SCREEN_HIDE = 7;
	public final static int CAPTURED_SCREEN_SHOW = 8;
	public final static int NEXT_PLAY = 9;
	public final static int PREV_PLAY = 10;
	public final static int SEEK_PLAY = 11;
}
