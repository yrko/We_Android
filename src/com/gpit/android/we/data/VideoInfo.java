package com.gpit.android.we.data;

import java.io.File;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;

import com.gpit.android.we.common.Constants;
import com.gpit.android.we.sqlite.DatabaseAdapter;
import com.gpit.android.we.sqlite.DatabaseMgr;

public class VideoInfo {

	public final static String FIELD_POSTED_EMAIL = "posted_email";
	public final static String FIELD_VIDEO_SIZE = "video_size";
	public final static String FIELD_ADDRESS = "address";
	public final static String FIELD_NICK_NAME = "nickname";
	public final static String FIELD_VIDEO_PATH = "video_path";
	public final static String FIELD_VIDEO_THUMB_PATH = "thumbnail_path";
	public final static String FIELD_VIDEO_TITLE = "video_title";
	public final static String FIELD_LNG = "longitude";
	public final static String FIELD_LAT = "latitude";
	public final static String FIELD_VIDEO_ID = "video_id";
	public final static String FIELD_POST_DATE_TIME = "post_datetime";
	
	public int iVideoUID;
	public String strPostedEmail;
	public long lVideoSize;
	public String strAddress;
	public String strNickName;
	public String strVideoUrl;
	public String strVideoLocalPath;
	public String strVideoThumbPath;
	public String strVideoTitle;
	public int iVideoID;
	public String strPostedDate;
	public String strLng;
	public String strLat;
	
	public VideoInfo(JSONObject aJObj) throws JSONException {
		strPostedEmail = aJObj.getString(FIELD_POSTED_EMAIL);
		lVideoSize = aJObj.getLong(FIELD_VIDEO_SIZE);
		strAddress = aJObj.getString(FIELD_ADDRESS);
		strNickName = aJObj.getString(FIELD_NICK_NAME);
		strVideoUrl = aJObj.getString(FIELD_VIDEO_PATH);
		strVideoThumbPath = aJObj.getString(FIELD_VIDEO_THUMB_PATH);
		strVideoTitle = aJObj.getString(FIELD_VIDEO_TITLE);
		iVideoID = aJObj.getInt(FIELD_VIDEO_ID);
		strPostedDate = aJObj.getString(FIELD_POST_DATE_TIME);
		strLng = aJObj.getString(FIELD_LNG);
		strLat = aJObj.getString(FIELD_LAT);
	}
	
	public VideoInfo() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * check synchroize status
	 * @return
	 */
	public static boolean isSynced(Context aContext, int aVideoId) {
		String strPath = getLocalVideoPath(aContext, aVideoId);
		if (strPath == null || strPath.trim().length() == 0)
			return false;
		File f = new File(strPath);
		if (!f.exists())
			return false;
		return true;
	}
	
	/**
	 * local video path
	 * @param aContext
	 * @param aVideoId
	 * @return
	 */
	public static String getLocalVideoPath(Context aContext, int aVideoId) {
		DatabaseAdapter dbAdp = DatabaseMgr.getDBHandle(aContext);
		if (dbAdp == null)
			return "";
		String strQry = String.format("select %s from %s where %s=%d", DatabaseMgr.V_F_DOWNLOADED_PATH,
				DatabaseMgr.TBL_VIDEO,
				DatabaseMgr.V_F_ID, aVideoId);
		Cursor cr = dbAdp.executeRawSQL(strQry);
		if (cr == null)
			return "";
		if (!cr.moveToFirst()) {
			cr.close();
			return "";
		}
		String strPath = cr.getString(0);
		cr.close();
		return strPath;
	}
	
	/**
	 * update download status
	 * @param aContext
	 * @param aVideoId
	 * @param aStat
	 */
	public static int updateDownloadStatus(Context aContext, int aVideoId, int aStat) {
		DatabaseAdapter dbAdp = DatabaseMgr.getDBHandle(aContext);
		if (dbAdp == null)
			return -1;
		String strQry = String.format("update %s set %s=%d where %s=%d", DatabaseMgr.TBL_VIDEO,
				DatabaseMgr.V_F_DOWNLOADED_STAT, aStat,
				DatabaseMgr.V_F_ID, aVideoId);
		return dbAdp.executeSQL(strQry);
	}
	
	/**
	 * update video path
	 * @param aContext
	 * @param aVideoId
	 * @param aPath
	 * @return
	 */
	public static int updateVideoPath(Context aContext, int aVideoId, String aPath) {
		DatabaseAdapter dbAdp = DatabaseMgr.getDBHandle(aContext);
		if (dbAdp == null)
			return -1;
		String strQry = String.format("update %s set %s='%s' where %s=%d", DatabaseMgr.TBL_VIDEO,
				DatabaseMgr.V_F_DOWNLOADED_PATH, DatabaseMgr.escapeString(aPath),
				DatabaseMgr.V_F_ID, aVideoId);
		return dbAdp.executeSQL(strQry);
	}
	
	/**
	 * synchronize local videos with server videos
	 * @param aContext
	 * @param aAryVideos
	 */
	public static void syncVideo(Context aContext, ArrayList<VideoInfo> aAryVideos, boolean aNeedOnlyAndroid) {
		for (VideoInfo eachVideo : aAryVideos) {
			if (aNeedOnlyAndroid && !isSupportedFile(eachVideo))
				continue;
			if (isExistVideo(aContext, eachVideo))
				continue;
			insertToDB(aContext, eachVideo);
		}
	}
	
	/**
	 * get video sync status
	 * @param aContext
	 * @param aVideoId
	 * @return
	 */
	public static int getSyncStat(Context aContext, int aVideoId) {
		DatabaseAdapter dbAdp = DatabaseMgr.getDBHandle(aContext);
		if (dbAdp == null)
			return -1;
		String strQry = String.format("select %s from %s where %s=%d", 
				DatabaseMgr.V_F_DOWNLOADED_STAT, DatabaseMgr.TBL_VIDEO,
				DatabaseMgr.V_F_ID, aVideoId);
		Cursor cr = dbAdp.executeRawSQL(strQry);
		if (cr == null)
			return -1;
		if (!cr.moveToFirst()) {
			cr.close();
			return -1;
		}
		int iStat = cr.getInt(0);
		cr.close();
		return iStat;
	}
	
	/**
	 * check exist video from local db
	 * @param aContext
	 * @param aInfo
	 * @return
	 */
	public static boolean isExistVideo(Context aContext, VideoInfo aInfo) {
		DatabaseAdapter dbAdp = DatabaseMgr.getDBHandle(aContext);
		if (dbAdp == null)
			return false;
		String strQry = String.format("select count(*) from %s where %s=%d", DatabaseMgr.TBL_VIDEO,
				DatabaseMgr.V_F_ID, aInfo.iVideoID);
		Cursor cr = dbAdp.executeRawSQL(strQry);
		if (cr == null)
			return false;
		if (!cr.moveToFirst()) {
			cr.close();
			return false;
		}
		int iCnt = cr.getInt(0);
		cr.close();
		return iCnt > 0?true:false;
	}
	
	/**
	 * get video list from local db
	 * @return
	 */
	public static ArrayList<VideoInfo> getTotalVideoListFromDB(Context aContext, boolean aNeedOnlyAndroid) {
		ArrayList<VideoInfo> aryInfo = new ArrayList<VideoInfo>();
		DatabaseAdapter dbAdp = DatabaseMgr.getDBHandle(aContext);
		if (dbAdp == null)
			return aryInfo;
		String strQry = String.format("select * from %s order by %s asc", DatabaseMgr.TBL_VIDEO, DatabaseMgr.V_F_POST_DATE);
		Cursor cr = dbAdp.executeRawSQL(strQry);
		if (cr == null)
			return aryInfo;
		if (!cr.moveToFirst()) {
			cr.close();
			return aryInfo;
		}
		
		do {
			VideoInfo info = getVideoInfo(cr);
			if (aNeedOnlyAndroid) {
				if (isSupportedFile(info))
					aryInfo.add(info);
			} else {
				aryInfo.add(info);
			}
		}while(cr.moveToNext());
		cr.close();                                                  
 
		return aryInfo;
	}
	
	/**
	 * check playable video file
	 * @param aInfo
	 * @return
	 */
	public static boolean isSupportedFile(VideoInfo aInfo) {
		String strExt = aInfo.strVideoUrl.substring(aInfo.strVideoUrl.lastIndexOf(".") + 1);
		if (strExt.toLowerCase().equals("mov"))
			return false;
		return true;
	}
	
	/**
	 * get videoinfo class item from cursor
	 * @param aCur
	 * @return
	 */
	public static VideoInfo getVideoInfo(Cursor aCur) {
		VideoInfo info = new VideoInfo();
		info.iVideoUID= aCur.getInt(aCur.getColumnIndex(DatabaseMgr.V_F_UID));
		info.iVideoID = aCur.getInt(aCur.getColumnIndex(DatabaseMgr.V_F_ID));
		info.strPostedEmail = aCur.getString(aCur.getColumnIndex(DatabaseMgr.V_F_POST_EMAIL));
		info.lVideoSize = aCur.getLong(aCur.getColumnIndex(DatabaseMgr.V_F_SIZE));
		info.strAddress = aCur.getString(aCur.getColumnIndex(DatabaseMgr.V_F_ADDR));
		info.strNickName = aCur.getString(aCur.getColumnIndex(DatabaseMgr.V_F_NICK_NAME));
		info.strVideoUrl = aCur.getString(aCur.getColumnIndex(DatabaseMgr.V_F_VIDEO_PATH));
		info.strVideoLocalPath = aCur.getString(aCur.getColumnIndex(DatabaseMgr.V_F_DOWNLOADED_PATH));
		info.strVideoThumbPath = aCur.getString(aCur.getColumnIndex(DatabaseMgr.V_F_THUMB_PATH));
		info.strVideoTitle = aCur.getString(aCur.getColumnIndex(DatabaseMgr.V_F_TITLE));
		info.strPostedDate = aCur.getString(aCur.getColumnIndex(DatabaseMgr.V_F_POST_DATE));
		info.strLng = aCur.getString(aCur.getColumnIndex(DatabaseMgr.V_F_LNG));
		info.strLat = aCur.getString(aCur.getColumnIndex(DatabaseMgr.V_F_LAT));
		return info;
	}
	
	/**
	 * delete temp video files
	 * @param aContext
	 */
	public static void deleteForUnusedVideo(final Context aContext) {
		new Thread(new Runnable() {
			
			public void run() {
				// TODO Auto-generated method stub
				ArrayList<VideoInfo> aryInfo = getTotalVideoListFromDB(aContext, false);
				File file = Environment.getDownloadCacheDirectory();
				String[] aryF = file.list();
				//String[] aryF = new File(Constants.getVideoPath()).list();
				if (aryF == null) {
				    return;
				}
				for (String eachF : aryF) {
					boolean bExist = false;
					for (VideoInfo eachV : aryInfo) {
						if (eachV == null || eachV.strVideoLocalPath == null || eachV.strVideoLocalPath.trim().length() == 0)
							continue;
						if (eachF.equalsIgnoreCase(new File(eachV.strVideoLocalPath).getName())) {
							bExist = true;
							break;
						}
					}
					if (bExist)
						continue;
					new File(Constants.getVideoPath() + "/" + eachF).delete();
				}
			}
		}).start();
	}
	
	/**
	 * insert video info to local db
	 * @param aContext
	 * @param aInfo
	 */
	public static void insertToDB(Context aContext, VideoInfo aInfo) {
		DatabaseAdapter dbAdp = DatabaseMgr.getDBHandle(aContext);
		if (dbAdp == null)
			return;
		String strQry = String.format("insert into %s " +
				"(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) values " +
				"(%d,'%s',%d,'%s','%s','%s','%s','%s','%s','%s','%s')", DatabaseMgr.TBL_VIDEO,
				DatabaseMgr.V_F_ID,
				DatabaseMgr.V_F_POST_EMAIL,
				DatabaseMgr.V_F_SIZE,
				DatabaseMgr.V_F_ADDR,
				DatabaseMgr.V_F_NICK_NAME,
				DatabaseMgr.V_F_VIDEO_PATH,
				DatabaseMgr.V_F_THUMB_PATH,
				DatabaseMgr.V_F_TITLE,
				DatabaseMgr.V_F_POST_DATE,
				DatabaseMgr.V_F_LNG,
				DatabaseMgr.V_F_LAT,
				
				aInfo.iVideoID,
				aInfo.strPostedEmail,
				aInfo.lVideoSize,
				DatabaseMgr.escapeString(aInfo.strAddress),
				DatabaseMgr.escapeString(aInfo.strNickName),
				DatabaseMgr.escapeString(aInfo.strVideoUrl),
				DatabaseMgr.escapeString(aInfo.strVideoThumbPath),
				DatabaseMgr.escapeString(aInfo.strVideoTitle),
				aInfo.strPostedDate,
				aInfo.strLng,
				aInfo.strLat
				);
		dbAdp.executeSQL(strQry);
	}
}
