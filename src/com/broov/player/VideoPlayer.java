package com.broov.player;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.gpit.android.we.LoginActivity;
import com.gpit.android.we.R;
import com.gpit.android.we.WeApplication;
import com.gpit.android.we.common.Constants;
import com.gpit.android.we.common.HttpFeeder;
import com.gpit.android.we.common.Log;
import com.gpit.android.we.common.MyLocation;
import com.gpit.android.we.common.SessionStore;
import com.gpit.android.we.common.Utils;
import com.gpit.android.we.common.WebApiLevel;
import com.gpit.android.we.common.MyLocation.LocationResult;
import com.gpit.android.we.data.VideoDownloader;
import com.gpit.android.we.data.VideoInfo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnKeyListener;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.hardware.SensorManager;

import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;

import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;
import android.os.PowerManager;

public class VideoPlayer extends Activity implements OnKeyListener, OnTouchListener  {

	ProgressDialog mProgressDlg;
	ArrayList<VideoInfo> mServerVideoAry = new ArrayList<VideoInfo>();
	static ArrayList<VideoInfo> mTotalVideoAry = new ArrayList<VideoInfo>();
	private final static int DELAY_MTIME = 250;
	
	Bitmap mBmp;
	View mControlPanel;
	View mVideoInfo;
	View mVideoSpeedy;
	View imgPlay;
	View imgRec;
	View imgBackwardSpeed;
	View imgForwardSpeedy;
	View imgBackSkip;
	View imgFwdSkip;
	View imgBack5Skip;
	View imgFwd5Skip;
	SeekBar mSeekBar;
	View tvPostUser;
	View tvPostAddress;
	View tvPostDate;
	View tvVideoTitle;
	View tvVideoSpeed;
	DemoRenderer demoRenderer;

	@SuppressWarnings("unused")
	private AudioThread 		  mAudioThread = null;
	private PowerManager.WakeLock wakeLock     = null;
	private Handler mHandler = new Handler();

	private Updater seekBarUpdater = new Updater();
	private static int current_aspect_ratio_type=1; //Default Aspect Ratio of the file
	private static boolean paused;
	final static String TAG = "VideoPlayer";
	public VideoDownloader mVideoDownloader;
	public static VideoPlayer Instance;
	MyLocation 	myPlaceLocation;
	static VideoInfo mCurrentPlayInfo;
	static int mCurrentVideoIndex = -1;
	Toast mEmptyToast;
	static boolean bSyncVideo;
	float xAtDown;
	float xAtUp;
	static final float flingOffset = 80;
	int iCurrentAngle;
	static boolean bStopPlay;
	
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
	
	public void onBackPressed() {
		seekBarUpdater.stopIt();
		demoRenderer.exitApp();
		mVideoDownloader.stopDownloader();
		finish();
		android.os.Process.killProcess(android.os.Process.myPid());
	}
	
	protected void onStop() {
		if (wakeLock != null) {
			wakeLock.release();
			wakeLock = null;
		}
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		Instance = null;
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
	    	LoginActivity.currentLocation = location;
	    }
	};

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Globals.setNativeVideoPlayer(false);
		paused = false;
		
		if (!SessionStore.isRegisteredPuchClientToWe(VideoPlayer.this))
			LoginActivity.registerPushService(VideoPlayer.this);

		// fullscreen mode
		requestWindowFeature(Window.FEATURE_NO_TITLE);		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		setContentView(R.layout.video_custom);
		
		VideoInfo.deleteForUnusedVideo(VideoPlayer.this);
		DemoRenderer.UpdateValuesFromSettings();
		initUI();
		initSDL();
		showVersion();
		getCurrentLocation();
		fetchVideoList();
		
		OrientationEventListener k = new OrientationEventListener(VideoPlayer.this, SensorManager.SENSOR_DELAY_UI) {
			@Override
			public void onOrientationChanged(int orientation) {
				// TODO Auto-generated method stub
				iCurrentAngle = orientation;
				Log.v("Angle", "" + orientation);
			}
		};
		k.enable();
	}

	/**
	 * Initialize UI components
	 */
	public void initUI() {
		Instance = this;
		mProgressDlg = ProgressDialog.show(VideoPlayer.this, "", getString(R.string.downloading), true);
		mSeekBar = (SeekBar) findViewById(R.id.video_seekBar);
		mControlPanel = findViewById(R.id.controls_linearLayout);
		mVideoInfo = findViewById(R.id.user_info_linearLayout);
		mVideoSpeedy = findViewById(R.id.speed_linearLayout);
		imgPlay = findViewById(R.id.play_ImageView);
		imgRec = findViewById(R.id.rec_imageView);
		imgBackSkip = findViewById(R.id.back_skip_imageView);
		imgFwdSkip = findViewById(R.id.forward_skip_imageView);
		imgBack5Skip = findViewById(R.id.back_skip5_imageView);
		imgFwd5Skip = findViewById(R.id.forward_skip5_imageView);
		imgForwardSpeedy = findViewById(R.id.forward_speedy_ImageView);
		imgBackwardSpeed = findViewById(R.id.back_speedy_ImageView);
		tvPostUser = findViewById(R.id.username_textView);
		tvPostAddress = findViewById(R.id.address_textView);
		tvPostDate = findViewById(R.id.postdate_textView);
		tvVideoTitle = findViewById(R.id.title_textView);
		tvVideoSpeed = findViewById(R.id.speed_textView);
		
		mControlPanel.setOnClickListener(mVisibleListener);
		imgBackSkip.setOnClickListener(mBackSkipListener);
		imgFwdSkip.setOnClickListener(mFwdSkipListener);
		imgBack5Skip.setOnClickListener(mBack5SkipListener);
		imgFwd5Skip.setOnClickListener(mFwd5SkipListener);
		
		imgPlay.setOnTouchListener(imgPlayTouchListener);
		imgRec.setOnClickListener(mRecListener);
		imgForwardSpeedy.setOnTouchListener(imgForwardTouchListener);
		imgBackwardSpeed.setOnTouchListener(imgBackwardTouchListener);
		
		mSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
		mVideoDownloader = new VideoDownloader(downloaderHandler, VideoPlayer.this);
		Globals.setVideoLoop(Globals.PLAY_ALL);
	}
	
	/**
	 * fetch video url list
	 */
	public void fetchVideoList() {
		bSyncVideo = true;
		new Thread(new Runnable() {
			
			public void run() {
				// TODO Auto-generated method stub
				String strUrl = String.format(Constants.DOWNLOAD_GET, WeApplication.APP_USER.strEmail, 0);
				boolean bRet = true;
				URL url = null;
				JSONObject jObj = null;
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

				if (bRet) {
					try {
						bRet = parseVideoUrls(jObj, mServerVideoAry);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						bRet = false;
					}
				}

				if (mServerVideoAry.size() > 0) {
					VideoInfo.syncVideo(VideoPlayer.this, mServerVideoAry, false);
				}
				
				mTotalVideoAry = new ArrayList<VideoInfo>();
				mTotalVideoAry.addAll(VideoInfo.getTotalVideoListFromDB(VideoPlayer.this, false));
				for (VideoInfo eachVideo : mTotalVideoAry) {
					if (eachVideo.strVideoLocalPath != null && eachVideo.strVideoLocalPath.equalsIgnoreCase(Constants.getIntroVideoPath()))
						continue;
					if (!VideoInfo.isSynced(VideoPlayer.this, eachVideo.iVideoID)) {
//						VideoInfo.updateDownloadStatus(VideoPlayer.this, eachVideo.iVideoID, Constants.STAT_SYNCING);
						mVideoDownloader.addDownloadVideoQue(eachVideo);
					}
				}

				String strCurPos = SessionStore.getCurrentPosition(VideoPlayer.this);
				if (strCurPos != null && strCurPos.indexOf(",") > 0 && strCurPos.split(",").length == 2) {
					int iUid = Integer.parseInt(strCurPos.split(",")[0]);
					int iPos = Integer.parseInt(strCurPos.split(",")[1]);
					if (iUid > 0 && iPos > 0) {
						int iIndex = 0;
						for (VideoInfo eachInfo : mTotalVideoAry) {
							if (eachInfo.iVideoID == iUid)
								break;
							iIndex++;
						}
						if (iIndex > 0) {
							mCurrentVideoIndex = iIndex - 1;
							Message msg = mVideoStatHandler.obtainMessage(Constants.SEEK_PLAY);
							msg.obj = iPos;
							msg.sendToTarget();
						} else {
							mCurrentVideoIndex = -1;
						}
					} else {
						mCurrentVideoIndex = -1;
					}
				} else {
					mCurrentVideoIndex = -1;
				}

				final boolean bSuccess = bRet;
				runOnUiThread(new Runnable() {
					public void run() {
						// TODO Auto-generated method stub
						bSyncVideo = false;
						if (!bSuccess) {
							hideWaitingDlg();
							Utils.showAlertMsg(getString(R.string.error), getString(R.string.server_error), VideoPlayer.this, false);
						}
					}
				});
			}
		}).start();
		showWaitingDlg();
	}
	
	Handler mVideoStatHandler = new Handler() {
		public void handleMessage(Message msg) {
			bStopPlay = false;
			switch (msg.what) {
			case Constants.SYNC_VIDEO:
				bStopPlay = true;
				showWaitingDlg();
				VideoInfo info = (VideoInfo)msg.obj;
				if (!VideoInfo.isSynced(VideoPlayer.this, info.iVideoID)) {
//				if (VideoInfo.getSyncStat(VideoPlayer.this, info.iVideoID) != Constants.STAT_SYNCING) {
//					VideoInfo.updateDownloadStatus(VideoPlayer.this, info.iVideoID, Constants.STAT_SYNCING);
					mVideoDownloader.addDownloadVideoQue(info);
				}
				break;
			case Constants.STOP_PLAY:
				bStopPlay = true;
				if (mEmptyToast == null) {
					mEmptyToast = Toast.makeText(VideoPlayer.this, R.string.no_exist_videos, Toast.LENGTH_SHORT);
					mEmptyToast.show();
				}
				break;
			case Constants.START_PLAY:
				mEmptyToast = null;
				if (!Globals.getFileName().equalsIgnoreCase(Constants.getIntroVideoPath())) {
					hideWaitingDlg();
					if (mCurrentPlayInfo != null) {
						setVideoInfo(mCurrentPlayInfo);
					}
				}
				break;
			case Constants.SEEK_PLAY:
				demoRenderer.nativePlayerNext();
				final float fPos = Float.parseFloat(msg.obj.toString()) / 1000;
				float fPercent = fPos / demoRenderer.nativePlayerDuration() * 1000;
				demoRenderer.nativePlayerSeek((int)fPercent);
				break;
			}
		}
	};
	
	/**
	 * get video file item from queue
	 * @return
	 */
	public static VideoInfo getNextVideoItem() {
		while(bSyncVideo) {
			SystemClock.sleep(10);
			continue;
		}
		mCurrentPlayInfo = null;
		mCurrentVideoIndex++;
		playVideoPull();
		return mCurrentPlayInfo;
	}
	
	/**
	 * get video file item from queue
	 * @return
	 */
	public static VideoInfo getPrevVideoItem() {
		if (mCurrentVideoIndex > 0)
			mCurrentVideoIndex--;
		mCurrentPlayInfo = null;
		playVideoPull();
		return mCurrentPlayInfo;
	}
	
	/**
	 * play video
	 */
	public static void playVideoPull() {
		while(true) {
			if (Instance == null) {
				SystemClock.sleep(500);
				continue;
			}
			try {
				mCurrentPlayInfo = mTotalVideoAry.get(mCurrentVideoIndex);
			} catch (IndexOutOfBoundsException e) {
				// TODO: handle exception
			}
			if (mTotalVideoAry.size() <= mCurrentVideoIndex) {
				mCurrentVideoIndex = mTotalVideoAry.size();
				Message msg = Instance.mVideoStatHandler.obtainMessage(Constants.STOP_PLAY);
				msg.sendToTarget();
				SystemClock.sleep(1000);
				continue;
			}
			if (VideoInfo.isSynced(Instance, mCurrentPlayInfo.iVideoID)) {
				if (VideoInfo.isSupportedFile(mCurrentPlayInfo)) {
					Globals.setadvSkipFrames(false);
					DemoRenderer.UpdateValuesFromSettings();
				} else {
					Globals.setadvSkipFrames(true);
					DemoRenderer.UpdateValuesFromSettings();
				}
				Message msg = Instance.mVideoStatHandler.obtainMessage(Constants.START_PLAY);
				msg.sendToTarget();
				break;
			} else {
				Message msg = Instance.mVideoStatHandler.obtainMessage(Constants.SYNC_VIDEO);
				msg.obj = mCurrentPlayInfo;
				msg.sendToTarget();
				SystemClock.sleep(1000);
				continue;
			}
		}
	}
	
	Handler downloaderHandler = new Handler() {
		public void handleMessage(Message msg) {
			hideWaitingDlg();
			switch (msg.what) {
			case VideoDownloader.ERR_NONE:
				VideoInfo info = (VideoInfo)msg.obj;
				boolean bSuccess = true;
				int iRet = VideoInfo.updateVideoPath(VideoPlayer.this, info.iVideoID, info.strVideoLocalPath);
				bSuccess &= iRet < 0?false:true;
//				iRet = VideoInfo.updateDownloadStatus(VideoPlayer.this, info.iVideoID, Constants.STAT_SYNCED);
				bSuccess &= iRet < 0?false:true;
				if (!bSuccess)
					Toast.makeText(VideoPlayer.this, R.string.save_fail, Toast.LENGTH_SHORT).show();
				break;
			case VideoDownloader.ERR_UNKNOWN:
				Toast.makeText(VideoPlayer.this, R.string.download_fail, Toast.LENGTH_SHORT).show();
				break;
			}
		}
	};
	
	/**
	 * parse Video urls
	 * @param aJObj
	 * @throws JSONException 
	 */
	public boolean parseVideoUrls(JSONObject aJObj, ArrayList<VideoInfo> aAryVideo) throws JSONException {
		int aStatus = aJObj.getInt(Constants.KEY_STATAUS);
		JSONArray jAry = aJObj.getJSONArray(Constants.KEY_RESULT);
		if (aStatus != 1)
			return false;
		int iCnt = jAry.length();
		if (iCnt > 0) {
			for (int i = 0 ; i < iCnt; i++) {
				JSONObject jObj = jAry.getJSONObject(i);
				aAryVideo.add(new VideoInfo(jObj));
			}
		}
		return true;
	}

	/**
	 * display version string
	 */
	public void showVersion() {
		String versionName = "0.1";
		try {
			versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
		}
		TextView tvVersion = (TextView)findViewById(R.id.version_textView);
		tvVersion.setText("v" + versionName);
	}

	/**
	 * set current video info to UI
	 * @param aInfo
	 */
	public void setVideoInfo(VideoInfo aInfo) {
		if (mControlPanel.getVisibility() == View.VISIBLE)
			mVideoInfo.setVisibility(View.VISIBLE);
		((TextView)tvPostUser).setText(aInfo.strNickName);
		((TextView)tvPostAddress).setText(aInfo.strAddress);
//		((TextView)tvVideoTitle).setText(aInfo.strVideoTitle);
		((TextView)tvVideoTitle).setText(null);
		Date currentDate = new Date();
		Date postDate = Utils.convetToDate(aInfo.strPostedDate);
		if (postDate != null) {
			long iBeforeDate = currentDate.getTime() - postDate.getTime();
			long iSeconds = iBeforeDate / 1000;
			long iMins = iSeconds / 60;
			long iHours = iMins / 60;
			long iDays = iHours / 24;
			long iMonths = iDays / 30;
			String strCreateDate = "";
			if (iMonths > 0) {
				if (iMonths ==1)
					strCreateDate = iMonths + " " + getString(R.string.month);
				else
					strCreateDate = iMonths + " " + getString(R.string.months);
				iDays -= 30 * iMonths;
				if (iDays > 0) {
					if (iDays == 1)
						strCreateDate += " " + iDays + " " + getString(R.string.day);
					else
						strCreateDate += " " + iDays + " " + getString(R.string.days);
				}
			} else {
				if (iDays > 0) {
					if (iDays == 1)
						strCreateDate = " " + iDays + " " + getString(R.string.day);
					else
						strCreateDate = " " + iDays + " " + getString(R.string.days);
					iHours -= iDays * 24;
					if (iHours > 0) {
						if (iHours == 1)
							strCreateDate += " " + iHours + " " + getString(R.string.hour);
						else
							strCreateDate += " " + iHours + " " + getString(R.string.hours);
					}
				} else {
					if (iHours > 0) {
						if (iHours == 1)
							strCreateDate = " " + iHours + " " + getString(R.string.hour);
						else
							strCreateDate = " " + iHours + " " + getString(R.string.hours);
						iMins -= iHours * 60;
						if (iMins > 0) {
							if (iMins == 1)
								strCreateDate += " " + iMins + " " + getString(R.string.min);
							else
								strCreateDate += " " + iMins + " " + getString(R.string.mins);
						}
					} else {
						if (iMins > 0) {
							if (iMins == 1)
								strCreateDate = " " + iMins + " " + getString(R.string.min);
							else
								strCreateDate = " " + iMins + " " + getString(R.string.mins);
							iSeconds -= 60 * iMins;
							if (iSeconds > 0) {
								if (iSeconds == 1)
									strCreateDate += " " + iSeconds + " " + getString(R.string.second);
								else
									strCreateDate += " " + iSeconds + " " + getString(R.string.seconds);
							}
						} else {
							if (iSeconds > 0) {
								if (iSeconds == 1)
									strCreateDate = " " + iSeconds + " " + getString(R.string.second);
								else
									strCreateDate = " " + iSeconds + " " + getString(R.string.seconds);
							}
						}
					}
				}
			}
			((TextView)tvPostDate).setText((strCreateDate + " " + getString(R.string.ago)).trim());
		}
	}
	
	/**
	 * set video speed
	 */
	public void setVideoSpeed(float aSpeed) {
		((TextView)tvVideoSpeed).setText(String.format("%fx", aSpeed));
	}
	
	public void initSDL() {
		//Wake lock code
		try {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, Globals.ApplicationName);
			wakeLock.acquire();
		} catch (Exception e) {
			System.out.println("Inside wake lock exception"+e.toString());
		}
		System.out.println("Acquired wakeup lock");

		//Native libraries loading code
		Globals.LoadNativeLibraries();
		System.out.println("native libraries loaded");

		//Audio thread initializer
		mAudioThread = new AudioThread(this);
		System.out.println("Audio thread initialized");

		GLSurfaceView_SDL mSurfaceView = (GLSurfaceView_SDL) findViewById(R.id.glsurfaceview);
		System.out.println("got the surface view:");

		mSurfaceView.setOnClickListener(mVisibleListener);
		mSurfaceView.setOnTouchListener(VideoPlayer.this);

		DemoRenderer demoRenderer = new DemoRenderer(this);
		this.demoRenderer = demoRenderer;
		mSurfaceView.setRenderer(demoRenderer); 
		System.out.println("Set the surface view renderer");

		SurfaceHolder holder = mSurfaceView.getHolder();
		holder.addCallback(mSurfaceView);
		System.out.println("Added the holder callback");
		holder.setType(SurfaceHolder.SURFACE_TYPE_GPU);
		System.out.println("Hold type set");

		mSurfaceView.setFocusable(true);
		mSurfaceView.requestFocus();

		mHandler.postDelayed(seekBarUpdater, 100);
	}

	/**
	 * Show Waiting Dialog
	 */
	public void showWaitingDlg() {
		if (!mProgressDlg.isShowing())
			mProgressDlg.show();
	}

	/**
	 * Hide Waiting Dialog
	 */
	public void hideWaitingDlg() {
		if (mProgressDlg.isShowing())
			mProgressDlg.cancel();
	}

	public void restartUpdater() {
		seekBarUpdater.stopIt();
		seekBarUpdater = new Updater();
		mHandler.postDelayed(seekBarUpdater, 100);
	}

	private class Updater implements Runnable {
		private boolean stop;

		public void stopIt() {
			System.out.println("Stopped updater");
			stop = true;
		}

		
		public void run() {
			if(demoRenderer != null) {
				long playedDuration = demoRenderer.nativePlayerDuration();				
				long totalDuration = demoRenderer.nativePlayerTotalDuration();
				if(totalDuration != 0) {
					int progress = (int)((1000 * playedDuration) / totalDuration);
					mSeekBar.setProgress(progress);
				}
				Log.v(TAG, "" + playedDuration);
				if(demoRenderer.fileInfoUpdated) {
					demoRenderer.fileInfoUpdated = false;
				}
			}

			if(!stop) {
				if (Globals.fileName != null) {
					//Restart the updater if file is still playing
					mHandler.postDelayed(seekBarUpdater, DELAY_MTIME);
				}
			}
		}
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode != RESULT_OK)
			return;
		switch (requestCode) {
		case R.id.rec_imageView:
			Uri uri = data.getData();
			uploadVideoFileToServer(uri);
			break;
		}
	}

	/**
	 * save video file
	 * @param videoUri
	 */
	public void uploadVideoFileToServer(final Uri videoUri) {
		new Thread(new Runnable() {
			
			public void run() {
				// TODO Auto-generated method stub
				final String strPath = Utils.getRealPathFromURI(VideoPlayer.this, videoUri);
				final VideoInfo vInfo = new VideoInfo();
				vInfo.strVideoLocalPath = strPath;
				if (LoginActivity.currentLocation != null) {
					vInfo.strLng = "" + LoginActivity.currentLocation.getLongitude();
					vInfo.strLat = "" + LoginActivity.currentLocation.getLatitude();
				} else {
					vInfo.strLng = "-180";
					vInfo.strLat = "-180";
				}
				vInfo.strNickName = WeApplication.APP_USER.strName;
				vInfo.strPostedEmail = WeApplication.APP_USER.strEmail;
				JSONObject jObj = null;
				boolean bRet = true;
				try {
					jObj = HttpFeeder.uploadVideo(vInfo);
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					bRet = false;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					bRet = false;
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					bRet = false;
				}
				
				if(bRet) {
					try {
						bRet = parseVideoItem(jObj, vInfo);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						bRet = false;
					}
				}
				
				if(bRet) {
					VideoInfo.insertToDB(VideoPlayer.this, vInfo);
				}
				
				final boolean bSuccess = bRet;
				runOnUiThread(new Runnable() {
					
					public void run() {
						// TODO Auto-generated method stub
						hideWaitingDlg();
						if (bSuccess) {
							mTotalVideoAry.add(vInfo);
						} else {
							//for test
							Toast.makeText(VideoPlayer.this, R.string.fail_upload_video, Toast.LENGTH_SHORT).show();
						}
					}
				});
			}
		}).start();
		showWaitingDlg();
	}

	/**
	 * parse video item
	 * @param aJObj
	 * @param aInfo
	 * @return
	 * @throws JSONException
	 */
	public static boolean parseVideoItem(JSONObject aJObj, VideoInfo aInfo) throws JSONException {
		// TODO Auto-generated method stub
		JSONObject jObj = aJObj.getJSONObject(Constants.KEY_RESULT);
		aInfo.iVideoID = jObj.getInt(VideoInfo.FIELD_VIDEO_ID);
		aInfo.lVideoSize = jObj.getLong(VideoInfo.FIELD_VIDEO_SIZE);
		aInfo.strAddress = jObj.getString(VideoInfo.FIELD_ADDRESS);
		aInfo.strVideoUrl = jObj.getString(VideoInfo.FIELD_VIDEO_PATH);
		aInfo.strVideoThumbPath = jObj.getString("thumbnail");
		aInfo.strVideoTitle = jObj.getString(VideoInfo.FIELD_VIDEO_TITLE);
		aInfo.strPostedDate = jObj.getString(VideoInfo.FIELD_POST_DATE_TIME);
		return true;
	}

	OnClickListener mBackSkipListener = new OnClickListener() {
		
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if (!bStopPlay)
				demoRenderer.nativePlayerPrev();
			else
				mCurrentVideoIndex--;
		}
	};
	
	OnClickListener mFwdSkipListener = new OnClickListener() {
		
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if (!bStopPlay)
				demoRenderer.nativePlayerNext();
		}
	};
	
	OnClickListener mBack5SkipListener = new OnClickListener() {
		
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if (mCurrentVideoIndex > 5) {
				mCurrentVideoIndex -= 4;
				demoRenderer.nativePlayerPrev();
			}
		}
	};
	
	OnClickListener mFwd5SkipListener = new OnClickListener() {
		
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if (mTotalVideoAry.size() > mCurrentVideoIndex + 5) {
				mCurrentVideoIndex += 4;
				demoRenderer.nativePlayerNext();
			}
		}
	};
	
	OnClickListener mRecListener = new OnClickListener() {
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if (!paused)
				pauseVideo(false);
			Intent intent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
			startActivityForResult(intent, v.getId());
		}
	};
	
	OnClickListener mVisibleListener = new OnClickListener() {
		public void onClick(View v) {
			visibleControlPanel();
		}
	};

	/**
	 * show/hide control panel
	 */
	public void visibleControlPanel() {
		if (mControlPanel.getVisibility() != View.VISIBLE) {
			mControlPanel.setVisibility(View.VISIBLE);
			mVideoInfo.setVisibility(View.VISIBLE);
			mVideoSpeedy.setVisibility(View.VISIBLE);
		} else {
			mControlPanel.setVisibility(View.GONE);
			mVideoInfo.setVisibility(View.GONE);
			mVideoSpeedy.setVisibility(View.GONE);
		}
	}
	
	OnTouchListener imgAspectRatioTouchListener = new OnTouchListener() {			
		
		public boolean onTouch(View v, MotionEvent event) {
			ImageView img = (ImageView) v;
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				//Do nothing for now
			}
			else if (event.getAction() == MotionEvent.ACTION_UP) {
				if (current_aspect_ratio_type == 3) {
					img.setImageResource(R.drawable.fs_shadow_4_3);
					demoRenderer.nativePlayerSetAspectRatio(0);
					current_aspect_ratio_type = 1;
				} else if (current_aspect_ratio_type == 1) {
					img.setImageResource(R.drawable.fs_shadow);
					demoRenderer.nativePlayerSetAspectRatio(3);
					current_aspect_ratio_type = 2;
				} else if (current_aspect_ratio_type == 2) {
					img.setImageResource(R.drawable.fs_shadow_16_9);
					demoRenderer.nativePlayerSetAspectRatio(2);
					current_aspect_ratio_type = 3;
				}
			}						
			return true;
		}
	};

	OnTouchListener imgPlayTouchListener = new OnTouchListener() {			
		
		public boolean onTouch(View v, MotionEvent event) {
			ImageView img = (ImageView) v;				

			if (event.getAction() == MotionEvent.ACTION_DOWN ) {	
				System.out.println("Down paused:" + paused);
				if(paused) {
					img.setImageResource(R.drawable.media_playback_start);
				} else {
					img.setImageResource(R.drawable.media_playback_pause);
				}
			} else if (event.getAction() == MotionEvent.ACTION_UP ) {
				System.out.println("Up paused:" + paused);		  
				System.out.println("Total:" + demoRenderer.nativePlayerTotalDuration() + "---Current:" + demoRenderer.nativePlayerDuration());
				pauseVideo(true);
			}
			//resetAutoHider();
			return true;
		}
	};

	/**
	 * pause video stream
	 */
	public void pauseVideo(boolean bUserAction) {
		if(paused) {
			demoRenderer.nativePlayerPause();
			seekBarUpdater = new Updater();
			mHandler.postDelayed(seekBarUpdater, DELAY_MTIME);
			((ImageView)imgPlay).setImageResource(R.drawable.media_playback_pause);
		} else {
			demoRenderer.nativePlayerPlay();
			seekBarUpdater.stopIt();
			((ImageView)imgPlay).setImageResource(R.drawable.media_playback_start);
		}
		paused = !paused;
	}
	
	OnTouchListener imgForwardTouchListener = new OnTouchListener() {			
		
		public boolean onTouch(View v, MotionEvent event) {
			ImageView img = (ImageView) v;
			if (event.getAction() == MotionEvent.ACTION_DOWN ) {		            		            
				img.setImageResource(R.drawable.media_seek_forward);
			} else if (event.getAction() == MotionEvent.ACTION_UP ) {		        	
				img.setImageResource(R.drawable.media_seek_forward);										
				demoRenderer.nativePlayerForward();
			}							
			//resetAutoHider();
			return true;
		}
	};

	OnTouchListener imgBackwardTouchListener = new OnTouchListener() {			
		
		public boolean onTouch(View v, MotionEvent event) {
			ImageView img = (ImageView) v;
			if (event.getAction() == MotionEvent.ACTION_DOWN ) {		            		            
				img.setImageResource(R.drawable.media_seek_backward);
			} else if (event.getAction() == MotionEvent.ACTION_UP ) {		        	
				img.setImageResource(R.drawable.media_seek_backward);										
				demoRenderer.nativePlayerRewind();
			}						
			//resetAutoHider();
			return true;
		}
	};

	OnSeekBarChangeListener mSeekBarChangeListener = new OnSeekBarChangeListener() {
		
		public void onStopTrackingTouch(SeekBar seekBar) {
			int progress = seekBar.getProgress();
			demoRenderer.nativePlayerSeek(progress);
			if (!paused) {
				restartUpdater();
			} 
		}
		
		public void onStartTrackingTouch(SeekBar seekBar) {
			//	// TODO Auto-generated method stub
		}

		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			// TODO Auto-generated method stub
		}
	};

	public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK)
			return true;
		return false;
	}

	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			xAtDown = event.getX();
			return true;
		case MotionEvent.ACTION_UP:
			xAtUp = event.getX();
			if(xAtUp + flingOffset < xAtDown) {
				if (!bStopPlay)
					demoRenderer.nativePlayerNext();
				return true;
			} else if (xAtUp > xAtDown + flingOffset) {
				if (!bStopPlay)
					demoRenderer.nativePlayerPrev();
				else
					mCurrentVideoIndex--;
				return true;
			}
			visibleControlPanel();
			return false;
		}
		return false;
	}

	/**
	 * play video from push message
	 * @param info
	 */
	public static void playVideoFromPush(VideoInfo aInfo) {
		// TODO Auto-generated method stub
		if (Instance == null)
			return;
		if (bSyncVideo)
			return;
		if (Instance.mVideoDownloader.isDownloading())
			Instance.mVideoDownloader.stopDownload();
		Instance.demoRenderer.nativePlayerPause();
		mTotalVideoAry.add(aInfo);
		mCurrentVideoIndex = mTotalVideoAry.size() - 2;
		if (!bStopPlay)
			Instance.demoRenderer.nativePlayerNext();
		else
			mCurrentVideoIndex++;
	}
}
