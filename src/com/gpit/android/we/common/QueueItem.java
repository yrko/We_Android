package com.gpit.android.we.common;

import java.net.URL;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.json.JSONArray;
import org.json.JSONObject;

public class QueueItem {
	public final static int GET = 0;
	public final static int POST = 1;
	public final static int PUT = 2;
	
	public int iMothedType;
	public URL reqURL;
	public List<NameValuePair> postParms;
	public JSONObject jObjPostParms;
	public JSONArray jAryPostParms;
	public WebApiLevel level;
	public FeederListener listener;
	public Exception e;
	
	public Object tag;
	public Object result;
	public UsernamePasswordCredentials upc;
	public boolean bReqLogin = false;
	
//	public QueueItem(boolean isGet, URL reqURL, List<NameValuePair> parms, 
//			AMBWebApiLevel level, FeederListener listener, Object result, Object tag) {
//		this.isGet = isGet;
//		this.reqURL = reqURL;
//		this.level = level;
//		this.postParms = parms;
//		this.listener = listener;
//		
//		this.result = result;
//		this.tag = tag;
//	}
	
	public QueueItem(boolean aReqLogin, int aType, URL reqURL, List<NameValuePair> parms, 
			WebApiLevel level, FeederListener listener, Object result, Object tag, UsernamePasswordCredentials aUpc) {
		this.bReqLogin = aReqLogin;
		this.iMothedType = aType;
		this.reqURL = reqURL;
		this.level = level;
		this.postParms = parms;
		this.listener = listener;
		
		this.result = result;
		this.tag = tag;
		this.upc = aUpc;
	}
	
	public QueueItem(int aType, URL reqURL, JSONObject parms, 
			WebApiLevel level, FeederListener listener, Object result, Object tag, UsernamePasswordCredentials aUpc) {
		this.bReqLogin = false;
		this.iMothedType = aType;
		this.reqURL = reqURL;
		this.level = level;
		this.jObjPostParms = parms;
		this.listener = listener;
		
		this.result = result;
		this.tag = tag;
		this.upc = aUpc;
	}
	
	public QueueItem(int aType, URL reqURL, JSONArray parms, 
			WebApiLevel level, FeederListener listener, Object result, Object tag, UsernamePasswordCredentials aUpc) {
		this.bReqLogin = false;
		this.iMothedType = aType;
		this.reqURL = reqURL;
		this.level = level;
		this.jAryPostParms = parms;
		this.listener = listener;
		
		this.result = result;
		this.tag = tag;
		this.upc = aUpc;
	}
}
