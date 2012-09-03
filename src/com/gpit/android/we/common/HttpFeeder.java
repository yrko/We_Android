/*
 * AMB
 * Copyright (C) 2011 Commusoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.gpit.android.we.common;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gpit.android.we.data.VideoInfo;

import junit.framework.Assert;

import android.content.Context;
import android.util.Log;

public class HttpFeeder extends CommonFeeder implements Runnable {
	// Define singleton class
	public static HttpFeeder FEEDER;
	
	private final static int SLEEP_MTIME = 100;
	// private final static int HTTP_CONNECTION_MTIMEOUT = 1000 * 20;
	public final static int HTTP_MAXIMUM_RESPONSE_SIZE = 4096;
	
	public String username;
	public String password;
	
	/**
	 * Init Module
	 * @param context
	 */
	public static void init(Context context) {
		FEEDER = new HttpFeeder(context);
	}
	
	// Dispatch thread to send http request
	private Thread httpHandler;
	
	/**
	 * LOG tag
	 */
	static String TAG = "";
	
	// Request queue
	private ConcurrentLinkedQueue<QueueItem> requestQueue = new ConcurrentLinkedQueue<QueueItem>(); 
	
	private HttpFeeder(Context context) {
		super(context);
		
		// Start dispatch thread
		httpHandler = new Thread(this);
		httpHandler.start();
		TAG = getClass().getSimpleName();
	}

	/**
	 * Retrieve response html from HTTP-GET Method
	 * @param getURL
	 * @param listener
	 * @param isBlocking
	 * @return
	 */
	public JSONObject getResponseJSONObjForGetMethod(boolean aReqLogin, URL getURL, WebApiLevel level, 
			boolean isBlocking, FeederListener listener, Object tag, UsernamePasswordCredentials aUpc)  throws Exception {
		StringBuffer response = new StringBuffer(HTTP_MAXIMUM_RESPONSE_SIZE);
		QueueItem newItem = new QueueItem(aReqLogin, QueueItem.GET, getURL, null, level, listener, 
				response, tag, aUpc);
		newItem.tag = tag;
		
		if (isBlocking) {
			response = postRequestItem(newItem);
			return new JSONObject(response.toString());
		} else {
			requestQueue.offer(newItem);
			return null;
		}
	}
	
	/**
	 * Retrieve response html from HTTP-GET Method
	 * @param getURL
	 * @param listener
	 * @param isBlocking
	 * @return
	 */
	public JSONArray getResponseJSONAryForGetMethod(boolean aReqLogin, URL getURL, WebApiLevel level, 
			boolean isBlocking, FeederListener listener, Object tag, UsernamePasswordCredentials aUpc)  throws Exception {
		StringBuffer response = new StringBuffer(HTTP_MAXIMUM_RESPONSE_SIZE);
		QueueItem newItem = new QueueItem(aReqLogin, QueueItem.GET, getURL, null, level, listener, 
				response, tag, aUpc);
		newItem.tag = tag;
		
		if (isBlocking) {
			response = postRequestItem(newItem);
			return new JSONArray(response.toString());
		} else {
			requestQueue.offer(newItem);
			return null;
		}
	}

	/**
	 * 1970 timestamp
	 * @param millis
	 * @return
	 */
	public static Date convertLocalTimestamp(long millis)
	{
	    TimeZone tz = TimeZone.getDefault();
	    Calendar c = Calendar.getInstance(tz);
	    long localMillis = millis;
	    int offset, time;

	    c.set(1970, Calendar.JANUARY, 1, 0, 0, 0);

	    // Add milliseconds
	    while (localMillis > Integer.MAX_VALUE)
	    {
	        c.add(Calendar.MILLISECOND, Integer.MAX_VALUE);
	        localMillis -= Integer.MAX_VALUE;
	    }
	    c.add(Calendar.MILLISECOND, (int)localMillis);

	    // Stupidly, the Calendar will give us the wrong result if we use getTime() directly.
	    // Instead, we calculate the offset and do the math ourselves.
	    time = c.get(Calendar.MILLISECOND);
	    time += c.get(Calendar.SECOND) * 1000;
	    time += c.get(Calendar.MINUTE) * 60 * 1000;
	    time += c.get(Calendar.HOUR_OF_DAY) * 60 * 60 * 1000;
	    offset = tz.getOffset(c.get(Calendar.ERA), c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.DAY_OF_WEEK), time);

	    return new Date(millis - offset);
	}

	/**
	 * Retrieve response html from HTTP-GET Method
	 * @param getURL
	 * @param listener
	 * @param isBlocking
	 * @return
	 */
	public StringBuffer getResponseForGetMethod(boolean aReqLogin, URL getURL, WebApiLevel level, 
			boolean isBlocking, FeederListener listener, Object tag, UsernamePasswordCredentials aUpc)  throws Exception {
		StringBuffer response = new StringBuffer(HTTP_MAXIMUM_RESPONSE_SIZE);
		QueueItem newItem = new QueueItem(aReqLogin, QueueItem.GET, getURL, null, level, listener, 
				response, tag, aUpc);
		newItem.tag = tag;
		
		if (isBlocking) {
			response = postRequestItem(newItem);
			return response;
		} else {
			requestQueue.offer(newItem);
			return null;
		}
	}
	
	/**
	 * Parse xml entry from buffer of string
	 * @param xml
	 * @return
	 */
	public static Document parseXML(String xmlStr) {
		Document doc = null;
		
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			InputStream is = new ByteArrayInputStream(xmlStr.getBytes("UTF-8"));
			doc = db.parse(is);
			doc.getDocumentElement().normalize();
		} catch (Exception e) {
		}
		
		return doc;
	}

	public static String parseGuid(Document aDoc) {
        if (aDoc != null) {
        	org.w3c.dom.Element order = aDoc.getDocumentElement();
        	NodeList items = order.getElementsByTagName("guid");
        	int iNodeCnt = items.getLength();
        	for (int i = 0; i < iNodeCnt; i++) {
        		Node item = items.item(i);
        		NodeList childList = item.getChildNodes();
        		int iChildNodeCount = childList.getLength();
        		for(int j = 0; j < iChildNodeCount; j++) {
        			Node childItem = childList.item(j);
        			if (childItem instanceof org.w3c.dom.Element) {
        				String name = childItem.getNodeName();
        				if (name.equalsIgnoreCase("value")) {
        					return childItem.getChildNodes().item(0).getNodeValue();
        				}
        			}
        		}
        	}
        }
        return null;
	}

	/**
	 * Retrieve response html from HTTP-POST Method
	 * @param getURL
	 * @param listener
	 * @param isBlocking
	 * Ignore useCache option
	 * @return
	 */
	public StringBuffer getResponseForPostMethod(boolean aReqLogin, URL postURL,
			List<NameValuePair> parms, WebApiLevel level, boolean isBlocking, 
			FeederListener listener, Object tag, UsernamePasswordCredentials aUpc) throws Exception {
		StringBuffer response = new StringBuffer(HTTP_MAXIMUM_RESPONSE_SIZE);
		if (parms == null) {
			ArrayList<NameValuePair> aryParam = new ArrayList<NameValuePair>();
			NameValuePair nonVal = new BasicNameValuePair("", "");
			aryParam.add(nonVal);
			parms = aryParam;
		}
		QueueItem newItem = new QueueItem(aReqLogin, QueueItem.POST, postURL, parms, level, listener, 
				response, tag, aUpc);
		
		if (isBlocking) {
			response = postRequestItem(newItem);
			return response;
		} else {
			requestQueue.offer(newItem);
			return null;
		}
	}
	
	/**
	 * Retrieve response html from HTTP-POST Method
	 * @param getURL
	 * @param listener
	 * @param isBlocking
	 * Ignore useCache option
	 * @return
	 */
	public JSONObject getResponseJSONObjForPostMethod(boolean aReqLogin, URL postURL,
			List<NameValuePair> parms, WebApiLevel level, boolean isBlocking, 
			FeederListener listener, Object tag, UsernamePasswordCredentials aUpc) throws Exception {
		StringBuffer response = new StringBuffer(HTTP_MAXIMUM_RESPONSE_SIZE);
		if (parms == null) {
			ArrayList<NameValuePair> aryParam = new ArrayList<NameValuePair>();
			NameValuePair nonVal = new BasicNameValuePair("", "");
			aryParam.add(nonVal);
			parms = aryParam;
		}
		QueueItem newItem = new QueueItem(aReqLogin, QueueItem.POST, postURL, parms, level, listener, 
				response, tag, aUpc);
		
		if (isBlocking) {
			response = postRequestItem(newItem);
			return new JSONObject(response.toString());
		} else {
			requestQueue.offer(newItem);
			return null;
		}
	}
	
	/**
	 * Retrieve response html from HTTP-POST Method
	 * @param getURL
	 * @param listener
	 * @param isBlocking
	 * Ignore useCache option
	 * @return
	 */
	public JSONObject getResponseJSONObjForPostMethod(boolean aReqLogin, URL postURL,
			JSONObject parms, WebApiLevel level, boolean isBlocking, 
			FeederListener listener, Object tag, UsernamePasswordCredentials aUpc) throws Exception {
		StringBuffer response = new StringBuffer(HTTP_MAXIMUM_RESPONSE_SIZE);
		QueueItem newItem = new QueueItem(QueueItem.POST, postURL, parms, level, listener, 
				response, tag, aUpc);
		
		if (isBlocking) {
			response = postRequestItem(newItem);
			return new JSONObject(response.toString());
		} else {
			requestQueue.offer(newItem);
			return null;
		}
	}
	
	/**
	 * Retrieve response html from HTTP-PUT Method
	 * @param getURL
	 * @param listener
	 * @param isBlocking
	 * Ignore useCache option
	 * @return
	 */
	public JSONObject getResponseJSONObjForPutMethod(boolean aReqLogin, URL postURL,
			JSONObject parms, WebApiLevel level, boolean isBlocking, 
			FeederListener listener, Object tag, UsernamePasswordCredentials aUpc) throws Exception {
		StringBuffer response = new StringBuffer(HTTP_MAXIMUM_RESPONSE_SIZE);
		QueueItem newItem = new QueueItem(QueueItem.PUT, postURL, parms, level, listener, 
				response, tag, aUpc);
		
		if (isBlocking) {
			response = postRequestItem(newItem);
			return new JSONObject(response.toString());
		} else {
			requestQueue.offer(newItem);
			return null;
		}
	}
	
	/**
	 * Retrieve response html from HTTP-POST Method
	 * @param getURL
	 * @param listener
	 * @param isBlocking
	 * Ignore useCache option
	 * @return
	 */
	public JSONObject getResponseJSONObjForPostMethod(boolean aReqLogin, URL postURL,
			JSONArray parms, WebApiLevel level, boolean isBlocking, 
			FeederListener listener, Object tag, UsernamePasswordCredentials aUpc) throws Exception {
		StringBuffer response = new StringBuffer(HTTP_MAXIMUM_RESPONSE_SIZE);
		QueueItem newItem = new QueueItem(QueueItem.POST, postURL, parms, level, listener, 
				response, tag, aUpc);
		
		if (isBlocking) {
			response = postRequestItem(newItem);
			return new JSONObject(response.toString());
		} else {
			requestQueue.offer(newItem);
			return null;
		}
	}
	
	/**
	 * Retrieve response html from HTTP-POST Method
	 * @param getURL
	 * @param listener
	 * @param isBlocking
	 * Ignore useCache option
	 * @return
	 */
	public JSONArray getResponseJSONAryForPostMethod(boolean aReqLogin, URL postURL,
			List<NameValuePair> parms, WebApiLevel level, boolean isBlocking, 
			FeederListener listener, Object tag, UsernamePasswordCredentials aUpc) throws Exception {
		StringBuffer response = new StringBuffer(HTTP_MAXIMUM_RESPONSE_SIZE);
		QueueItem newItem = new QueueItem(aReqLogin, QueueItem.POST, postURL, parms, level, listener, 
				response, tag, aUpc);
		
		if (isBlocking) {
			response = postRequestItem(newItem);
			return new JSONArray(response.toString());
		} else {
			requestQueue.offer(newItem);
			return null;
		}
	}

	/**
	 * Execute request item directly
	 * @param item
	 * @return
	 */
	public StringBuffer postRequestItem(QueueItem item) throws Exception {
		StringBuffer response = null;
		
		// ResponseType result;
		switch (item.iMothedType) {
		case QueueItem.GET:
			postHttpGetItem(item);
			break;
		case QueueItem.POST:
			postHttpPostItem(item);
			break;
		case QueueItem.PUT:
			postHttpPutItem(item);
			break;
		}
		response = (StringBuffer)item.result;
		return response;
	}
	
	public void setCredential(UsernamePasswordCredentials aUpc, HttpClient client, HttpRequestBase request, String host) {
//		switch (level) {
//		case AMB_WEBAPI_NONE_SECURITY:
//			username= AMBWebApi.API_KEY;
//			password= "";
//			break;
//		case AMB_WEBAPI_MEDIUM_SECURITY:
//			username= AMBWebApi.API_KEY + "|" + AMBApp.APP.SETTING.username;
//			password= "";
//			break;
//		case AMB_WEBAPI_HIGH_SECURITY:
//			username= AMBWebApi.API_KEY + "|" + AMBApp.APP.SETTING.username;
//			password= AMBApp.APP.SETTING.password;
//			break;
//		}
		
//        UsernamePasswordCredentials upc = new UsernamePasswordCredentials(
//                username, password);
        AuthScope as = new AuthScope(host, 80);
        ((AbstractHttpClient) client).getCredentialsProvider()
                .setCredentials(as, aUpc);
        
        // request.setHeader("Authorization", "Basic " + 
        //        Base64.encodeToString((username + ":" + password).getBytes(), 0));
        		
        /*
		Authenticator.setDefault(new Authenticator(){
		    protected PasswordAuthentication getPasswordAuthentication() {
		        return new PasswordAuthentication(username, null);
		    }});
		*/
	}
	
	/**
	 * Execute HTTP-GET request item directly
	 * @param item
	 * @return
	 */
	public ResponseType postHttpGetItem(QueueItem item) throws Exception {
		Assert.assertTrue(item.iMothedType == QueueItem.GET);
		Assert.assertTrue(item.reqURL != null && !item.reqURL.equals(""));
		
		Log.v(TAG, "HTTP-GET: " + item.reqURL);
		
		try {
			
		} finally {
			
		}
		String line;
		HttpGet httpRequest = null;
		
        httpRequest = new HttpGet(item.reqURL.toURI());
        
        httpRequest.setHeader("Accept", "application/json");

        HttpClient httpclient = new DefaultHttpClient();
        if (item.upc != null)
        	setCredential(item.upc, httpclient, httpRequest, item.reqURL.getHost());
        
        if (item.bReqLogin) {
        } else {
        }
        
        HttpResponse response = (HttpResponse) httpclient.execute(httpRequest);
        HttpEntity entity = response.getEntity();
        BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
        InputStream in = bufHttpEntity.getContent();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in,
				"UTF-8"));

		// Clear buffer
		StringBuffer resp = (StringBuffer)item.result;
		Assert.assertTrue(resp != null);
		resp.setLength(0);
		while ((line = reader.readLine()) != null) {
			resp.append(line);
		}
		
		Log.v(TAG, "HTTP-GET-RESULT: " + resp);
		httpclient.getConnectionManager().shutdown();
		return ResponseType.RESPONSE_SUCCESS;
	}

	/**
	 * generate SHA 256 string
	 * @param aData
	 * @return
	 * @throws NoSuchProviderException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 * @throws UnsupportedEncodingException 
	 */
	public static String getHash(String aAlgorithm, String aData, String aKey) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
	    Mac mac = Mac.getInstance(aAlgorithm);
	    SecretKeySpec secret = new SecretKeySpec(aKey.getBytes(), mac.getAlgorithm());
	    mac.init(secret);
	    mac.reset();
	    byte[] digest = mac.doFinal(aData.getBytes());
	    byte[] result = Base64.encodeBase64(digest);
	    return new String(result);
	}
	
	/**
	 * Execute HTTP-PUT request item directly
	 * @param item
	 * @return
	 */
	public ResponseType postHttpPutItem(QueueItem item) throws Exception {
		Assert.assertTrue(item.iMothedType == QueueItem.PUT);
		Assert.assertTrue(item.reqURL != null && !item.reqURL.equals(""));
		
		Log.v(TAG, "HTTP-POST: " + item.reqURL);
		
		// Create a new HttpClient and Post Header
	    HttpClient httpclient = new DefaultHttpClient();
	    String uri = item.reqURL.toString();
	    HttpPut httpput = new HttpPut(uri);
        if (item.bReqLogin) {
        } else {
        }
        
        if (item.upc != null)
        	setCredential(item.upc, httpclient, httpput, item.reqURL.getHost());
        // Add your data
	    if (item.postParms != null) {
			JSONObject holder = new JSONObject();
			for (NameValuePair eachVal : item.postParms) {
				holder.put(eachVal.getName(), eachVal.getValue());
			}
			httpput.setHeader("Accept", "application/json");
			httpput.setHeader("Content-type", "application/json");
			StringEntity se = new StringEntity(holder.toString());
	    	httpput.setEntity(se);
	    } else {
			httpput.setHeader("Accept", "application/json");
			httpput.setHeader("Content-type", "application/json");
			
			JSONObject jObjHolder = item.jObjPostParms;
			JSONArray jAryHolder = item.jAryPostParms;
			if (jObjHolder != null) {
				StringEntity se = new StringEntity(jObjHolder.toString());
				httpput.setEntity(se);
			} else {
				StringEntity se = new StringEntity(jAryHolder.toString());
				httpput.setEntity(se);
			}
	    }

        // Execute HTTP Post Request
        HttpResponse resp = httpclient.execute(httpput);
        BufferedReader reader = new BufferedReader(new InputStreamReader(
        		resp.getEntity().getContent(), "UTF-8"));
		String line;

		StringBuffer response = (StringBuffer)item.result;
		Assert.assertTrue(response != null);
		// Clear buffer
		response.setLength(0);
		while ((line = reader.readLine()) != null) {
			response.append(line);
		}
		reader.close();
		
	    Log.v(TAG, "HTTP-POST-RESULT: " + response);
	    httpclient.getConnectionManager().shutdown();
	    return ResponseType.RESPONSE_SUCCESS;
	}
	
	/**
	 * upload video file to server
	 * @param aInfo
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws JSONException 
	 */
	public static JSONObject uploadVideo(VideoInfo aInfo) throws MalformedURLException, IOException, JSONException {
    	String strUrl = Constants.UPLOAD_POST;
    	Log.v("CarModel->upload image", "post to group mesage->" + strUrl);
    	String strBoundary = "Ks2VJ99H7GMzhhIdIHu_FT7rHcut6s";
    	String endLine = "\r\n";
    	
		HttpURLConnection conn = (HttpURLConnection) new URL(strUrl).openConnection();

    	conn.setDoOutput(true);
    	conn.setDoInput(true);
    	conn.setConnectTimeout(30000);
    	conn.setUseCaches(true);
    	
    	conn.setRequestMethod("POST");
    	conn.setRequestProperty("Connection", "Keep-Alive");
    	conn.setRequestProperty("Content-Type", "multipart/form-data;boundary="+strBoundary);
    	
    	conn.connect();
    	
    	File f = new File(aInfo.strVideoLocalPath);
    	FileInputStream fos = new FileInputStream(f);
    	OutputStream os = new BufferedOutputStream(conn.getOutputStream());
    	os = new BufferedOutputStream(conn.getOutputStream());
    	
    	os.write(("--" + strBoundary + endLine).getBytes());
    	
    	//&mode=spec&gids=1
    	os.write(("Content-Disposition: form-data; name=\"email\"" + endLine + endLine + aInfo.strPostedEmail).getBytes());
    	os.write((endLine + "--" + strBoundary + endLine).getBytes());
    	
//    	os.write(("Content-Disposition: form-data; name=\"mode\"" + endLine + endLine + "spec").getBytes());
//    	os.write((endLine + "--" + strBoundary + endLine).getBytes());
//    	
//    	os.write(("Content-Disposition: form-data; name=\"gids\"" + endLine + endLine + "1").getBytes());
//    	os.write((endLine + "--" + strBoundary + endLine).getBytes());
    	
    	os.write(("Content-Disposition: form-data; name=\"title\"" + endLine + endLine + "No title").getBytes());
    	os.write((endLine + "--" + strBoundary + endLine).getBytes());
    	
    	os.write(("Content-Disposition: form-data; name=\"lng\"" + endLine + endLine + aInfo.strLng).getBytes());
    	os.write((endLine + "--" + strBoundary + endLine).getBytes());
    	
    	os.write(("Content-Disposition: form-data; name=\"lat\"" + endLine + endLine + aInfo.strLat).getBytes());
    	os.write((endLine + "--" + strBoundary + endLine).getBytes());
    	
    	os.write(("Content-Disposition: form-data; name=\"videofile\"; filename=\"" + f.getName() + "\"" +  endLine + "Content-Type: video/3gpp" + endLine + endLine).getBytes());
    	byte[] buf = new byte[4096];
    	int len = 0;
    	while((len = fos.read(buf)) != -1) {
    		os.write(buf, 0, len);
    	}
    	os.write((endLine + "--" + strBoundary + "--" + endLine).getBytes());
    	os.flush();
    	os.close();
    	String response = read(conn.getInputStream());
    	conn.disconnect();
    	return new JSONObject(response);
	}
	
    /**
     * read from http input stream
     * @param in
     * @return
     * @throws IOException
     */
    public static String read(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(in), 1000);
        for (String line = r.readLine(); line != null; line = r.readLine()) {
            sb.append(line);
        }
        in.close();
        return sb.toString();
    }

	/**
	 * Execute HTTP-POST request item directly
	 * @param item
	 * @return
	 */
	public ResponseType postHttpPostItem(QueueItem item) throws Exception {
		Assert.assertTrue(item.iMothedType == QueueItem.POST);
		Assert.assertTrue(item.reqURL != null && !item.reqURL.equals(""));
		
		Log.v(TAG, "HTTP-POST: " + item.reqURL);
		
		// Create a new HttpClient and Post Header
		HttpClient httpclient = new DefaultHttpClient();
		String uri = item.reqURL.toString();
		HttpPost httppost = new HttpPost(uri);
		if (item.bReqLogin) {
		} else {
		}
		
		if (item.upc != null)
			setCredential(item.upc, httpclient, httppost, item.reqURL.getHost());
		// Add your data
		if (item.postParms != null) {
			JSONObject holder = new JSONObject();
			for (NameValuePair eachVal : item.postParms) {
				holder.put(eachVal.getName(), eachVal.getValue());
			}
			httppost.setHeader("Accept", "application/json");
			httppost.setHeader("Content-type", "application/json");
			StringEntity se = new StringEntity(holder.toString());
			httppost.setEntity(se);
		} else {
			httppost.setHeader("Accept", "application/json");
			httppost.setHeader("Content-type", "application/json");
			JSONObject jObjHolder = item.jObjPostParms;
			JSONArray jAryHolder = item.jAryPostParms;
			if (jObjHolder != null) {
				StringEntity se = new StringEntity(jObjHolder.toString());
				httppost.setEntity(se);
			} else {
				StringEntity se = new StringEntity(jAryHolder.toString());
				httppost.setEntity(se);
			}
		}
		
		// Execute HTTP Post Request
		HttpResponse resp = httpclient.execute(httppost);
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				resp.getEntity().getContent(), "UTF-8"));
		String line;
		
		StringBuffer response = (StringBuffer)item.result;
		Assert.assertTrue(response != null);
		// Clear buffer
		response.setLength(0);
		while ((line = reader.readLine()) != null) {
			response.append(line);
		}
		reader.close();
		
		Log.v(TAG, "HTTP-POST-RESULT: " + response);
		httpclient.getConnectionManager().shutdown();
		return ResponseType.RESPONSE_SUCCESS;
	}
	
	/**
	 * Dispatch request items
	 */
	
	public void run() {
		while (!httpHandler.isInterrupted()) {
			QueueItem item = requestQueue.poll();
			if (item != null) {
				try {
					StringBuffer response = postRequestItem(item);
					item.result = (Object)response;
					if (item.listener != null)
						item.listener.onSuccess(item, item.tag);
				} catch (Exception e) {
					item.e = e;
					if (item.listener != null)
						item.listener.onFailed(item, item.tag);
				}
			}
			try {
				Thread.sleep(SLEEP_MTIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
