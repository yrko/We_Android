package com.gpit.android.we.data;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.gpit.android.we.common.Log;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.SystemClock;

public class VideoMonitor implements Runnable {

	public static interface ScreenShotEvent {
		public void onHideScreenShot();
		public void onShowScreenShot(long aOffset);
	}
	final static String TAG = "VideoMonitor";
	
	private Thread videoHandler;
	private ConcurrentLinkedQueue<MediaPlayer> requestQueue = new ConcurrentLinkedQueue<MediaPlayer>();
	private final static int SLEEP_MTIME = 100;
	ScreenShotEvent mCss;
	
	public void addQueVideo(MediaPlayer aInfo) {
		requestQueue.offer(aInfo);
	}
	
	
	public void run() {
		// TODO Auto-generated method stub
		while (!videoHandler.isInterrupted()) {
			try {
				MediaPlayer player = requestQueue.poll();
				if (player != null) {
					int iEnd = player.getDuration();
					int iLeft = 0;
					while(true) {
						iLeft = player.getCurrentPosition();
						if (iLeft > 500 && iLeft < 1000) {
							if (mCss != null)
								mCss.onHideScreenShot();
						}
						if (iLeft + 1000 >= iEnd) {
							break;
						}
						SystemClock.sleep(SLEEP_MTIME);
						Log.v(TAG, iLeft + "/" + iEnd);
					}
					if (mCss != null)
						mCss.onShowScreenShot(iLeft);
				}
				try {
					Thread.sleep(SLEEP_MTIME);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}
	}
	
	public VideoMonitor(Context aContext, ScreenShotEvent aCss) {
		// TODO Auto-generated constructor stub
		videoHandler = new Thread(this);
		videoHandler.start();
		mCss = aCss;
	}
}
