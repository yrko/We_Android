package com.gpit.android.we;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.broov.player.VideoPlayer;
import com.gpit.android.we.common.Constants;
import com.gpit.android.we.common.HttpFeeder;
import com.gpit.android.we.common.SessionStore;
import com.gpit.android.we.common.Utils;
import com.gpit.android.we.common.WebApiLevel;
import com.gpit.android.we.data.VideoDownloader;
import com.gpit.android.we.data.VideoInfo;
import com.gpit.android.we.data.VideoMonitor;
import com.gpit.android.we.data.VideoMonitor.ScreenShotEvent;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

public class VideoPlayerActivity extends Activity implements OnKeyListener, OnPreparedListener, OnCompletionListener, OnClickListener {

	ProgressDialog mProgressDlg;
	ArrayList<VideoInfo> mServerVideoAry = new ArrayList<VideoInfo>();
	static ArrayList<VideoInfo> mTotalVideoAry = new ArrayList<VideoInfo>();
	VideoView mVideoView;
	VideoMonitor vmHandler;
	VideoDownloader mVideoDownloader;
	final static String TAG = "VideoPlayerActivity";
	MediaMetadataRetriever mmr;
	ImageView ivVideo;
	Bitmap mBmp;
	View ivBtnPlay;
	public static final String KEY_FILE_NAME = "videofilename";
	static int mCurrentVideoIndex = 0;
	static VideoInfo mCurrentPlayInfo;
	Toast mEmptyToast;
	View tvPostUser;
	View tvPostAddress;
	View tvPostDate;
	View tvVideoTitle;
	View llUserInfo;
	View llVideoSpeed;
	
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.video_default);
		initUI();
	}
	
	/**
	 * Initialize UI components
	 */
	public void initUI() {
		mProgressDlg = ProgressDialog.show(VideoPlayerActivity.this, "", getString(R.string.downloading), true);
		llUserInfo = findViewById(R.id.user_info_linearLayout);
		llVideoSpeed = findViewById(R.id.speed_linearLayout);
		mVideoView = (VideoView)findViewById(R.id.default_videoView);
		ivVideo = (ImageView)findViewById(R.id.screenshot_imageView);
        mVideoView.setOnPreparedListener(VideoPlayerActivity.this);
        mVideoView.setOnCompletionListener(VideoPlayerActivity.this);
        View btnSkipBack = findViewById(R.id.back_skip_imageView);
        View btnSkipFwd = findViewById(R.id.forward_skip_imageView);
        View imgBack5Skip = findViewById(R.id.back_skip5_imageView);
        View imgFwd5Skip = findViewById(R.id.forward_skip5_imageView);
		View btnSeekBack = findViewById(R.id.back_speedy_ImageView);
        View btnSeekFwd = findViewById(R.id.forward_speedy_ImageView);
        View btnRec = findViewById(R.id.rec_imageView);
        ivBtnPlay = findViewById(R.id.play_ImageView);
        
        btnSkipBack.setOnClickListener(VideoPlayerActivity.this);
        btnSkipFwd.setOnClickListener(VideoPlayerActivity.this);
        btnSeekBack.setOnClickListener(VideoPlayerActivity.this);
        imgBack5Skip.setOnClickListener(VideoPlayerActivity.this);
        imgFwd5Skip.setOnClickListener(VideoPlayerActivity.this);
        btnSeekFwd.setOnClickListener(VideoPlayerActivity.this);
        btnRec.setOnClickListener(VideoPlayerActivity.this);
        ivBtnPlay.setOnClickListener(VideoPlayerActivity.this);

		tvPostUser = findViewById(R.id.username_textView);
		tvPostAddress = findViewById(R.id.address_textView);
		tvPostDate = findViewById(R.id.postdate_textView);
		tvVideoTitle = findViewById(R.id.title_textView);

		mServerVideoAry = new ArrayList<VideoInfo>();
		vmHandler = new VideoMonitor(VideoPlayerActivity.this, mScreenShotEvent);
		mVideoDownloader = new VideoDownloader(downloaderHandler, VideoPlayerActivity.this);
		mmr = new MediaMetadataRetriever();
		fetchVideoList(false);
		showVersion();
		setPlayStat(false);
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
	 * set video status
	 * @param bPlaying
	 */
	public void setPlayStat(boolean bPlaying) {
		if (bPlaying) {
			llUserInfo.setVisibility(View.VISIBLE);
			llVideoSpeed.setVisibility(View.VISIBLE);
			ivBtnPlay.setBackgroundResource(R.drawable.media_playback_pause);
		} else {
			llUserInfo.setVisibility(View.INVISIBLE);
			llVideoSpeed.setVisibility(View.INVISIBLE);
			ivBtnPlay.setBackgroundResource(R.drawable.media_playback_start);
		}
	}
	
	ScreenShotEvent mScreenShotEvent = new ScreenShotEvent() {
		public void onHideScreenShot() {
			// TODO Auto-generated method stub
			Message msg = mVideoStatHandler.obtainMessage(Constants.CAPTURED_SCREEN_HIDE);
			msg.sendToTarget();
		}
		
		public void onShowScreenShot(final long aOffset) {
			// TODO Auto-generated method stub
			Message msg = mVideoStatHandler.obtainMessage(Constants.CAPTURED_SCREEN_SHOW);
			msg.sendToTarget();
		}
	};
	
	/**
	 * fetch video url list
	 */
	public void fetchVideoList(final boolean aUploadVideoMode) {
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
					VideoInfo.syncVideo(VideoPlayerActivity.this, mServerVideoAry, true);
				}
				
				mTotalVideoAry = new ArrayList<VideoInfo>();
				mTotalVideoAry.addAll(VideoInfo.getTotalVideoListFromDB(VideoPlayerActivity.this, true));
				for (VideoInfo eachVideo : mTotalVideoAry) {
					if (!VideoInfo.isSynced(VideoPlayerActivity.this, eachVideo.iVideoID)) {
						VideoInfo.updateDownloadStatus(VideoPlayerActivity.this, eachVideo.iVideoID, Constants.STAT_SYNCING);
						mVideoDownloader.addDownloadVideoQue(eachVideo);
					}
				}
				
				if (!aUploadVideoMode) {
					String strCurPos = SessionStore.getCurrentPosition(VideoPlayerActivity.this);
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
								Message msg = mVideoStatHandler.obtainMessage(Constants.NEXT_PLAY);
								msg.sendToTarget();
							}
						} else {
							mCurrentVideoIndex = -1;
							Message msg = mVideoStatHandler.obtainMessage(Constants.NEXT_PLAY);
							msg.sendToTarget();
						}
					} else {
						mCurrentVideoIndex = -1;
						Message msg = mVideoStatHandler.obtainMessage(Constants.NEXT_PLAY);
						msg.sendToTarget();
					}
				}

				
				final boolean bSuccess = bRet;
				runOnUiThread(new Runnable() {
					public void run() {
						// TODO Auto-generated method stub
						hideWaitingDlg();
						if (!bSuccess) {
							Utils.showAlertMsg(getString(R.string.error), getString(R.string.server_error), VideoPlayerActivity.this, false);
						}
					}
				});
			}
		}).start();
		showWaitingDlg();
	}
	
	/**
	 * play video once
	 */
	public void playVideoOnce() {
		if (mCurrentPlayInfo == null)
			return;
		runOnUiThread(new Runnable() {
			public void run() {
				// TODO Auto-generated method stub
				try {
					mVideoView.setTag(0);
					mVideoView.setVideoPath(mCurrentPlayInfo.strVideoLocalPath);
					mmr.setDataSource(mCurrentPlayInfo.strVideoLocalPath);
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			}
		});
	}
	
	/**
	 * play video once
	 */
	public void playVideoSeek(final int aPos) {
		if (mCurrentPlayInfo == null)
			return;
		runOnUiThread(new Runnable() {
			public void run() {
				// TODO Auto-generated method stub
				try {
					mVideoView.setTag(0);
					mVideoView.setVideoPath(mCurrentPlayInfo.strVideoLocalPath);
					mVideoView.seekTo(aPos);
					mmr.setDataSource(mCurrentPlayInfo.strVideoLocalPath);
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			}
		});
	}
	
	Handler mVideoStatHandler = new Handler() {
		public void handleMessage(final Message msg) {
			Message customMsg = null;
			switch (msg.what) {
			case Constants.SYNC_VIDEO:
				showWaitingDlg();
				VideoInfo info = (VideoInfo)msg.obj;
				if (VideoInfo.getSyncStat(VideoPlayerActivity.this, info.iVideoID) != Constants.STAT_SYNCING) {
					VideoInfo.updateDownloadStatus(VideoPlayerActivity.this, info.iVideoID, Constants.STAT_SYNCING);
					mVideoDownloader.addDownloadVideoQue(info);
				}
				break;
			case Constants.STOP_PLAY:
				if (mEmptyToast == null) {
					ivBtnPlay.setBackgroundResource(R.drawable.media_playback_start);
					mEmptyToast = Toast.makeText(VideoPlayerActivity.this, R.string.no_exist_videos, Toast.LENGTH_SHORT);
					mEmptyToast.show();
				}
				break;
			case Constants.START_PLAY:
				mEmptyToast = null;
				hideWaitingDlg();
				if (mCurrentPlayInfo != null) {
					setVideoInfo(mCurrentPlayInfo);
				}
				break;
			case Constants.BK_SKIP:
				if (mCurrentVideoIndex > 0) {
					customMsg = mVideoStatHandler.obtainMessage(Constants.PREV_PLAY);
					customMsg.sendToTarget();
				}
				break;
			case Constants.BK_SKIP5:
				if (mCurrentVideoIndex > 4) {
					mCurrentVideoIndex -= 4;
					customMsg = mVideoStatHandler.obtainMessage(Constants.PREV_PLAY);
					customMsg.sendToTarget();
				}
				break;
			case Constants.FWD_SKIP:
				if (mTotalVideoAry.size() > mCurrentVideoIndex) {
					customMsg = mVideoStatHandler.obtainMessage(Constants.NEXT_PLAY);
					customMsg.sendToTarget();
				}
				break;
			case Constants.FWD_SKIP5:
				if (mTotalVideoAry.size() > mCurrentVideoIndex + 5) {
					mCurrentVideoIndex += 4;
					customMsg = mVideoStatHandler.obtainMessage(Constants.NEXT_PLAY);
					customMsg.sendToTarget();
				}
				break;
			case Constants.CAPTURED_SCREEN_SHOW:
				Log.v(TAG, "end capturing...");
				ivVideo.setImageBitmap(mBmp);
				setPlayStat(false);
				ivVideo.setVisibility(View.VISIBLE);
				customMsg = mVideoStatHandler.obtainMessage(Constants.NEXT_PLAY);
				customMsg.sendToTarget();
				break;
			case Constants.CAPTURED_SCREEN_HIDE:
				Log.v(TAG, "start capturing...");
				ivVideo.setVisibility(View.INVISIBLE);
				if (mBmp != null)
					mBmp.recycle();
				mBmp = mmr.getFrameAtTime(-1);
				setPlayStat(true);
				break;
			case Constants.NEXT_PLAY:
				if(mVideoView.isPlaying())
					mVideoView.pause();
				new Thread(new Runnable() {
					public void run() {
						// TODO Auto-generated method stub
						getNextVideoItem();
						playVideoOnce();
					}
				}).start();
				break;
			case Constants.PREV_PLAY:
				if(mVideoView.isPlaying())
					mVideoView.pause();
				new Thread(new Runnable() {
					public void run() {
						// TODO Auto-generated method stub
						getPrevVideoItem();
						playVideoOnce();
					}
				}).start();
				break;
			case Constants.SEEK_PLAY:
				mCurrentPlayInfo = mTotalVideoAry.get(mCurrentVideoIndex);
				final int iPos = Integer.parseInt(msg.obj.toString());
				new Thread(new Runnable() {
					public void run() {
						// TODO Auto-generated method stub
						getNextVideoItem();
						playVideoSeek(iPos);
					}
				}).start();
				break;
			}
		}
	};
	
	Handler downloaderHandler = new Handler() {
		public void handleMessage(Message msg) {
			hideWaitingDlg();
			switch (msg.what) {
			case VideoDownloader.ERR_NONE:
				VideoInfo info = (VideoInfo)msg.obj;
				boolean bSuccess = true;
				int iRet = VideoInfo.updateVideoPath(VideoPlayerActivity.this, info.iVideoID, info.strVideoLocalPath);
				bSuccess &= iRet < 0?false:true;
				iRet = VideoInfo.updateDownloadStatus(VideoPlayerActivity.this, info.iVideoID, Constants.STAT_SYNCED);
				bSuccess &= iRet < 0?false:true;
				if (!bSuccess)
					Toast.makeText(VideoPlayerActivity.this, R.string.save_fail, Toast.LENGTH_SHORT).show();
				break;
			case VideoDownloader.ERR_UNKNOWN:
				Toast.makeText(VideoPlayerActivity.this, R.string.download_fail, Toast.LENGTH_SHORT).show();
				break;
			}
		}
	};

	/**
	 * set current video info to UI
	 * @param aInfo
	 */
	public void setVideoInfo(VideoInfo aInfo) {
		((TextView)tvPostUser).setText(aInfo.strNickName);
		((TextView)tvPostAddress).setText(aInfo.strAddress);
		((TextView)tvPostDate).setText(aInfo.strNickName);
		((TextView)tvVideoTitle).setText(aInfo.strVideoTitle);
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
	
	public void onBackPressed() {
		// TODO Auto-generated method stub
		if (mVideoView.isPlaying()) {
			mVideoView.pause();
			int lPos = mVideoView.getCurrentPosition();
			SessionStore.setCurrentPosition(VideoPlayerActivity.this, mCurrentPlayInfo.iVideoID + "," + lPos);
		}
		finish();
		android.os.Process.killProcess(android.os.Process.myPid());
	}
	
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
		int iSize = jAry.length();
		if (iSize > 0) {
			for (int i = 0 ; i < iSize; i++) {
				JSONObject jObj = jAry.getJSONObject(i);
				aAryVideo.add(new VideoInfo(jObj));
			}
		}
		return true;
	}
	
	public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK)
			return true;
		return false;
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
	
	public void onCompletion(MediaPlayer arg0) {
		// TODO Auto-generated method stub
		hideWaitingDlg();
		setPlayStat(false);
	}
	
	public void onPrepared(MediaPlayer mp) {
		// TODO Auto-generated method stub
		Object obj = mVideoView.getTag();
		if (obj == null)
			return;
		if (Integer.parseInt(obj.toString()) == 1)
			return;
		hideWaitingDlg();
		mp.start();
		vmHandler.addQueVideo(mp);
		setPlayStat(true);
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
				final String strPath = Utils.getRealPathFromURI(VideoPlayerActivity.this, videoUri);
				final VideoInfo vInfo = new VideoInfo();
				vInfo.strVideoLocalPath = strPath;
				if (LoginActivity.currentLocation != null) {
					vInfo.strLng = "" + LoginActivity.currentLocation.getLongitude();
					vInfo.strLat = "" + LoginActivity.currentLocation.getLatitude();
				} else {
					vInfo.strLng = "-180";
					vInfo.strLat = "-180";
				}
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
						bRet = VideoPlayer.parseVideoItem(jObj, vInfo);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						bRet = false;
					}
				}
				
				if(bRet) {
					VideoInfo.insertToDB(VideoPlayerActivity.this, vInfo);
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
//							Toast.makeText(VideoPlayerActivity.this, R.string.fail_upload_video, Toast.LENGTH_SHORT).show();
							fetchVideoList(true);
						}
					}
				});
			}
		}).start();
		showWaitingDlg();
	}

	/**
	 * get video file name
	 * @return
	 */
	public String getVideoFileName() {
		return Constants.getVideoPath() + "/" + System.currentTimeMillis() + ".3gp";
	}
	
	/**
	 * get file abs path from uri
	 * @param contentUri
	 * @return
	 */
	public String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String strPath = cursor.getString(column_index);
        cursor.close();
        return strPath;
    }
	
	/**
	 * get video file item from queue
	 * @return
	 */
	public void getNextVideoItem() {
		mCurrentPlayInfo = null;
		mCurrentVideoIndex++;
		playVideoPull();
	}
	
	/**
	 * get video file item from queue
	 * @return
	 */
	public void getPrevVideoItem() {
		mCurrentPlayInfo = null;
		mCurrentVideoIndex--;
		playVideoPull();
	}

	/**
	 * play video
	 */
	public void playVideoPull() {
		while(true) {
			try {
				mCurrentPlayInfo = mTotalVideoAry.get(mCurrentVideoIndex);
			} catch (IndexOutOfBoundsException e) {
				// TODO: handle exception
			}
			if (mTotalVideoAry.size() <= mCurrentVideoIndex) {
				mCurrentVideoIndex = mTotalVideoAry.size();
				Message msg = mVideoStatHandler.obtainMessage(Constants.STOP_PLAY);
				msg.sendToTarget();
				SystemClock.sleep(500);
				break;
			}
			if (VideoInfo.isSynced(VideoPlayerActivity.this, mCurrentPlayInfo.iVideoID)) {
				Message msg = mVideoStatHandler.obtainMessage(Constants.START_PLAY);
				msg.sendToTarget();
				break;
			} else {
				Message msg = mVideoStatHandler.obtainMessage(Constants.SYNC_VIDEO);
				msg.obj = mCurrentPlayInfo;
				msg.sendToTarget();
				SystemClock.sleep(1000);
				continue;
			}
		}
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		mVideoView.pause();
		mVideoView.setTag(1);
	}
	
	public void onClick(View v) {
		// TODO Auto-generated method stub
		Message msg = null;
		switch (v.getId()) {
		case R.id.back_skip5_imageView:
			msg = mVideoStatHandler.obtainMessage(Constants.BK_SKIP5);
			msg.sendToTarget();
			break;
		case R.id.forward_skip5_imageView:
			msg = mVideoStatHandler.obtainMessage(Constants.FWD_SKIP5);
			msg.sendToTarget();
			break;
		case R.id.back_skip_imageView:
			msg = mVideoStatHandler.obtainMessage(Constants.BK_SKIP);
			msg.sendToTarget();
			break;
		case R.id.forward_skip_imageView:
			msg = mVideoStatHandler.obtainMessage(Constants.FWD_SKIP);
			msg.sendToTarget();
			break;
		case R.id.back_speedy_ImageView:
			mVideoView.canSeekBackward();
			break;
		case R.id.forward_speedy_ImageView:
			mVideoView.canSeekForward();
			break;
		case R.id.rec_imageView:
			mVideoView.pause();
			mVideoView.setTag(1);
			Intent intent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
			startActivityForResult(intent, R.id.rec_imageView);
			break;
		case R.id.play_ImageView:
			if (!mVideoView.isPlaying()) {
				mVideoView.start();
				mVideoView.setTag(0);
				ivBtnPlay.setBackgroundResource(R.drawable.media_playback_pause);
			} else {
				mVideoView.pause();
				mVideoView.setTag(1);
				ivBtnPlay.setBackgroundResource(R.drawable.media_playback_start);
			}
			break;
		}
	}
}
