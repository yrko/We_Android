package com.gpit.android.we.sqlite;

import java.io.File;

import com.gpit.android.we.common.Constants;
import com.gpit.android.we.common.Log;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.SystemClock;

public class DatabaseAdapter extends SQLiteOpenHelper {

	public static final int FDBL_ERR_NONE 					= 0;
	public static final int FDBL_ABORT 						= -1;
	public static final int FDBL_FILE_ACCESS_ERROR 			= -2;
	public static final int FDBL_OUT_COLUMNS 				= -3;
	public static final int FDBL_OPEN_HANDLE_ERROR 			= -4;
	public static final int FDBL_NON_UNIQUE_VALUE 			= -5;
	public static final int FDBL_BUSY 						= -6;
	public static final int FDBL_BAD_TYPE 					= -7;
	public static final int FDBL_LACK_MEM 					= -8;
	public static final int FDBL_ALREADY_EXIST_TABLE 		= -9;
	public static final int FDBL_UNKNOWN_TABLE 				= -10;
	public static final int FDBL_INVALID_SCHEMA 			= -11;
	public static final int FDBL_NOT_EXIST_DB_FILE			= -12;

	public static final int MAX_TIMEOUT						= 100;
	
	static int DATABASE_VERSION = 1;
	
	public SQLiteDatabase mDatabase;
	public static String Tag;
	
	/**
	 * Constructor for DatabaseAdapter
	 * @param context
	 */
	public DatabaseAdapter(Context context) {
		super(context, Constants.DB_PATH, null, DATABASE_VERSION);
		// TODO Auto-generated constructor stub
		Tag = getClass().getSimpleName();
		File f = new File(Constants.DB_PATH);
		if (!new File(f.getParent()).exists()) {
			new File(f.getParent()).mkdirs();
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * open data base
	 * @return
	 */
	public boolean openDatabase() {
		if (mDatabase == null)
			mDatabase = SQLiteDatabase.openDatabase(Constants.DB_PATH, null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.CREATE_IF_NECESSARY);
		if (!mDatabase.isOpen())
			mDatabase = SQLiteDatabase.openDatabase(Constants.DB_PATH, null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.CREATE_IF_NECESSARY);
		if(mDatabase == null)
			return false;
		return true;
	}
	
	/**
	 * execute sql query (update or delete)
	 * @param query
	 * @return error code
	 */
	public int executeSQL(String query) {
		int iCount = 0;
		while(mDatabase.isDbLockedByCurrentThread() && mDatabase.isDbLockedByOtherThreads()) {
			SystemClock.sleep(100);
			iCount++;
			if (iCount == 50)
				break;
			continue;
		}
		
		if(mDatabase.isDbLockedByCurrentThread() && mDatabase.isDbLockedByOtherThreads()) {
			return FDBL_BUSY;
		}
		
		if(query.equalsIgnoreCase("BEGIN")) {
			/** if this query is "begin", check pending transaction exists. */
			iCount = 0;
			while(mDatabase.inTransaction()) {
				SystemClock.sleep(100);
				iCount++;
				if (iCount == 50)
					break;
				continue;
			}
		}
		
		try {
			if(query.equalsIgnoreCase("BEGIN")) {
				if(mDatabase.inTransaction()) {
					return FDBL_ABORT;
				}
				mDatabase.beginTransaction();
			} else if(query.equalsIgnoreCase("COMMIT")) {
				mDatabase.setTransactionSuccessful();
				mDatabase.endTransaction();
			} else if(query.equalsIgnoreCase("ROLLBACK")) {
				mDatabase.endTransaction();
			} else {
				mDatabase.execSQL(query);
				Log.v(Tag, query);
			}
		} catch (android.database.sqlite.SQLiteDatabaseCorruptException e) {
			return FDBL_FILE_ACCESS_ERROR;
		} catch (android.database.sqlite.SQLiteConstraintException e) {
			return FDBL_NON_UNIQUE_VALUE;
		} catch (SQLiteException e) {
			e.getMessage();
			return FDBL_ABORT;
		}
		return FDBL_ERR_NONE;
	}

	/**
	 * execute sql query (select)
	 * @param query
	 * @return
	 */
	public Cursor executeRawSQL(String query) {
		int iCount = 0;
		while(mDatabase.isDbLockedByOtherThreads()) {
			SystemClock.sleep(100);
			iCount++;
			if (iCount == 50)
				break;
			
			continue;
		}
		
		if(mDatabase.isDbLockedByOtherThreads()) {
			return null;
		}
		
		try {
			Cursor cursor = mDatabase.rawQuery(query, null);
			return cursor;
		}catch (android.database.sqlite.SQLiteDatabaseCorruptException e) {
			return null;
		}catch (SQLiteException exception) {
			return null;
		}
	}
	
	/**
	 * get int for String column
	 * @param cursor
	 * @param colName
	 * @return
	 */
	public int getColumnIndex(Cursor cursor, String colName) {
		if(colName == null)
			return -1;
		
		int index = 0;
		index = cursor.getColumnIndex(colName);
		return index;
	}
	
	@Override
	public synchronized void close() {
		if (mDatabase != null)
			mDatabase.close();
		super.close();
	}
	
	/**
	 * check opened DB
	 * @return
	 */
	public boolean isOpen() {
		if(mDatabase == null)
			return false;
		if(mDatabase.isOpen())
			return true;
		return false;
	}
}
