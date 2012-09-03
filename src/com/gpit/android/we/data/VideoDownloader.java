package com.gpit.android.we.data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.gpit.android.we.common.Constants;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;

public class VideoDownloader implements Runnable {

	private Thread videoHandler;
	private ConcurrentLinkedQueue<VideoInfo> requestQueue = new ConcurrentLinkedQueue<VideoInfo>();
	private final static int SLEEP_MTIME = 100;
	private Handler mHandler;
	public final static int ERR_NONE = 0;
	public final static int ERR_UNKNOWN = -1;
	private boolean bStop;
	private Context mContext;
	
	/**
	 * push stack video item
	 * @param aInfo
	 */
	public void addDownloadVideoQue(VideoInfo aInfo) {
		bStop = false;
		for (VideoInfo eachInfo : requestQueue) {
			if (eachInfo.iVideoID == aInfo.iVideoID)
				return;
		}
		requestQueue.offer(aInfo);
	}
	
	/**
	 * stop download video
	 */
	public void stopDownload() {
		bStop = true;
		if (!isDownloading())
			bStop = false;
		while(bStop) {
			SystemClock.sleep(100);
		}
	}
	
	/**
	 * check downloading
	 * @return
	 */
	public boolean isDownloading() {
		return requestQueue.size() == 0 ? false: true;
	}
	
	public void run() {
		while (!videoHandler.isInterrupted()) {
			VideoInfo item = requestQueue.poll();
			if (item != null) {
				boolean bError = false;
				File f = new File(Constants.getVideoPath());
				if (f.exists() && f.isFile()) {
					f.delete();
					f.mkdirs();
				} else {
					f.mkdirs();
				}
				// check if the path really exists
				// 
				String strExt = item.strVideoUrl.substring(item.strVideoUrl.lastIndexOf("."));
				File fTmp = new File(Constants.getVideoPath() + "/" + System.currentTimeMillis() + strExt);
				if (VideoInfo.isSynced(mContext, item.iVideoID)) {
					item.strVideoLocalPath = VideoInfo.getLocalVideoPath(mContext, item.iVideoID);
				} else {
					try {
						URL url = new URL(item.strVideoUrl);
						InputStream in = url.openConnection().getInputStream();
						fTmp.createNewFile();
						FileOutputStream fos = new FileOutputStream(fTmp);
						byte[] buffer = new byte[4096];
						int len = 0;
						while((len = in.read(buffer)) != -1) {
							fos.write(buffer, 0, len);
							if (bStop)
								break;
						}
						fos.close();
						fos.flush();
						in.close();
						if (bStop) {
							fTmp.delete();
							requestQueue.clear();
							bStop = false;
							continue;
						}
					} catch (MalformedURLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						bError = true;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						bError = true;
					}
				}

				if (mHandler != null) {
					Message msg = mHandler.obtainMessage();
					if (!bError) {
						msg.what = ERR_NONE;
						item.strVideoLocalPath = fTmp.getAbsolutePath();
						msg.obj = item;
					} else {
						msg.what = ERR_UNKNOWN;
					}
					msg.sendToTarget();
				}
			}
			SystemClock.sleep(SLEEP_MTIME);
		}
	}
	
	/**
	 * check empty queue
	 * @return
	 */
	public boolean isEmptyQue() {
		return requestQueue.isEmpty();
	}
	
	/**
	 * stop downloader
	 */
	public void stopDownloader() {
		videoHandler.interrupt();
	}
	
	public VideoDownloader(Handler aHandler, Context aContext) {
		// TODO Auto-generated constructor stub
		videoHandler = new Thread(this);
		videoHandler.start();
		mHandler = aHandler;
		mContext = aContext;
	}
}
