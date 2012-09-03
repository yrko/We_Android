package com.gpit.android.we.common;

import java.io.File;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import com.gpit.android.we.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.KeyEvent;

public class Utils {

	public static final String KEY_ERR_TITLE = "exception";
	public static final String KEY_ERR_MSG = "message";

	/**
	 * convert to date from string (yyyy-MM-dd);
	 * @param aDate
	 * @return
	 */
	public static Date convetToDate(String aDate) {
		SimpleDateFormat formatter_one = new SimpleDateFormat ("yyyy-MM-dd hh:mm:ss z");
        ParsePosition pos = new ParsePosition (0);
        Date date = null;
        try {
        	date = formatter_one.parse(aDate, pos);
		} catch (Exception e) {
			// TODO: handle exception
		}
		return date;
	}

	/** 
     * Returns a formated time HoursH MinutesM SecondsS
     * 
     * @param millis
     * @return
     */
    public static String formatTime(long seconds) {
              String output = "";
              //long seconds = millis / 1000;
              long minutes = seconds / 60;
              long hours = minutes / 60;
              long days = hours / 24;
              seconds = seconds % 60;
              minutes = minutes % 60;
              hours = hours % 24;

              String secondsD = String.valueOf(seconds);
              String minutesD = String.valueOf(minutes);
              String hoursD = String.valueOf(hours); 

              if (seconds < 10)
                secondsD = "0" + seconds;
              if (minutes < 10)
                minutesD = "0" + minutes;
              if (hours < 10){
                hoursD = "0" + hours;
              }

              if( days > 0 ){
                      output = days +"d ";
              } 
              if(hours > 0) {
            	  output += hoursD + ":";
              }
                      //output += hoursD + ":" + minutesD + ":" + secondsD;
              			output += minutesD + ":" + secondsD;
             
              return output;
    }

	/**
	 * check validation Email
	 * @param aMail
	 * @return
	 */
	public static boolean checkValideEmail(String aMail) {
		/*
		String strPtn = "^\\D.+@.+\\.[a-z]+";
		Pattern p = Pattern.compile(strPtn);
		Matcher m = p.matcher(aMail);
		if (m.matches())
			return true;
		return false;
		*/
		boolean result;
		try {
			result = android.util.Patterns.EMAIL_ADDRESS.matcher(aMail).matches();
		} catch (NullPointerException exception) {
			result = false;
		}
		return result;
	}

	/**
	 * show call back dialog
	 * @param aPositiveButtonTitle
	 * @param aNagativeButtonTitle
	 * @param aTitle
	 * @param aMsg
	 * @param aActivity
	 * @param aHandler
	 */
	public static void showCallBackDialog(String aPositiveButtonTitle, 
			String aNagativeButtonTitle, 
			String aTitle, 
			String aMsg, 
			final Activity aActivity, 
			final Handler aHandler,
			final Object aObj) {
		if (aActivity.isFinishing())
			return;
		AlertDialog.Builder builder = new Builder(aActivity);
		builder.setIcon(R.drawable.ic_launcher);
		builder.setTitle(aTitle);
		builder.setMessage(aMsg);
		builder.setPositiveButton(aPositiveButtonTitle, new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				if (aHandler != null) {
					Message msg = aHandler.obtainMessage();
					msg.what = DialogInterface.BUTTON_POSITIVE;
					msg.obj = aObj;
					msg.sendToTarget();
				}
			}
		});
		builder.setNegativeButton(aNagativeButtonTitle, new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				if (aHandler != null) {
					Message msg = aHandler.obtainMessage();
					msg.what = DialogInterface.BUTTON_NEGATIVE;
					msg.obj = aObj;
					msg.sendToTarget();
				}
			}
		});
		builder.setOnKeyListener(new OnKeyListener() {
			
			
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub
				Message msg = aHandler.obtainMessage();
				msg.what = DialogInterface.BUTTON_POSITIVE;
				msg.obj = aObj;
				msg.sendToTarget();
				return false;
			}
		});
		AlertDialog dlg = builder.create();
		dlg.show();
	}

	/**
	 * get error message from server
	 * @param aJObj
	 * @param aContext
	 * @return
	 */
	public static Bundle getErr(JSONObject aJObj, Context aContext) {
		Bundle bundle = new Bundle();
		String errMsg = null;
		String strTitle = KEY_ERR_TITLE;
		JSONObject jObj = null;
		try {
			jObj = aJObj.getJSONObject(KEY_ERR_TITLE);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (jObj != null) {
			try {
				errMsg = jObj.getString(KEY_ERR_MSG);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		bundle.putString(KEY_ERR_TITLE, strTitle);
		bundle.putString(KEY_ERR_MSG, errMsg);
		return bundle;
	}

	/**
	 * show error message from server
	 * @param aTitle
	 * @param aMsg
	 */
	public static void showAlertMsg(String aTitle, String aMsg, final Activity aActivity, final boolean aShouldFinish) {
		if (aActivity.isFinishing())
			return;
		AlertDialog.Builder builder = new Builder(aActivity);
		builder.setIcon(R.drawable.ic_launcher);
		builder.setTitle(aTitle);
		builder.setMessage(aMsg);
		if (aTitle == null && aMsg == null) {
			builder.setTitle(R.string.error);
			builder.setMessage(R.string.unkwon_error);
		}
		
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				if (aShouldFinish && !aActivity.isFinishing()) {
					aActivity.finish();
				}
			}
		});
		builder.setOnKeyListener(new OnKeyListener() {
			
			
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					if (aShouldFinish && !aActivity.isFinishing()) {
						aActivity.finish();
					}
					return true;
				}
				return false;
			}
		});
		AlertDialog dlg = builder.create();
		dlg.show();
	}

	/**
	 * get file abs path from uri
	 * @param contentUri
	 * @return
	 */
	public static String getRealPathFromURI(Context aContext, Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        ContentResolver contentResolver = aContext.getContentResolver();
        Cursor cursor = contentResolver.query(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String strPath = cursor.getString(column_index);
        cursor.close();
        return strPath;
    }
	
	/**
	 * delete files for folder
	 * @param fileOrDirectory
	 */
	public static void deleteRecursive(File fileOrDirectory) {
		if (fileOrDirectory.isDirectory())
			for (File child : fileOrDirectory.listFiles())
				deleteRecursive(child);
		fileOrDirectory.delete();
	}
}
