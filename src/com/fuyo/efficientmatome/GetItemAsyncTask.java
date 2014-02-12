package com.fuyo.efficientmatome;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.HttpConnectionMetricsImpl;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.renderscript.RenderScript.RSErrorHandler;
import android.text.Html;
import android.util.Log;



public class GetItemAsyncTask extends AsyncTask<String, Integer, Integer> {
	private String uuid;
	private String resultBody = "";
	private int resultStatus = 0;
	private int[] itemIds;
	private UploadEventListener uploadEventListener;
	private int versionCode;
	public GetItemAsyncTask(Context context, String uuid, int[] itemIds, UploadEventListener listener) {
	    this.uuid = uuid;
	    this.uploadEventListener = listener;
	    this.itemIds = itemIds;
	  }
	  
	@Override
	protected Integer doInBackground(String... params) {
		HttpParams httpParams = new BasicHttpParams();
		httpParams.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, Integer.valueOf(1000));
		httpParams.setParameter(CoreConnectionPNames.SO_TIMEOUT, Integer.valueOf(30000));
		HttpClient httpClient = new DefaultHttpClient(httpParams);

		HttpPost httpPost = new HttpPost("http://matome.iijuf.net/_api.getItemsFromIdsV2.php");
		List<NameValuePair> param = new ArrayList<NameValuePair>();
		ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
	
			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				switch (response.getStatusLine().getStatusCode()) {
				case HttpStatus.SC_OK:
					String body = EntityUtils.toString(response.getEntity(), "UTF-8");
					Log.d("upload", body);
					resultBody = body;
					resultStatus = 1;
					return body;
				default:
					resultStatus = -1;
					return "NG";
				}
			}
				    	  
		};
		param.add(new BasicNameValuePair("uuid", uuid));
		for (int itemId : itemIds) {
			param.add(new BasicNameValuePair("itemId[]", String.valueOf(itemId)));
		}
		try {
			httpPost.setEntity(new UrlEncodedFormEntity(param, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		try {
			httpClient.execute(httpPost, responseHandler);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		httpClient.getConnectionManager().shutdown();
		param = null;
		return 0;
	}

	@Override
	protected void onPostExecute(Integer result) {
		if (resultStatus == 1) {
			uploadEventListener.onSuccess(resultBody);
			//success
		} else {
			uploadEventListener.onFailure();
			//failed
		}
	}

	@Override
	protected void onPreExecute() {
		resultStatus = 0;
		uploadEventListener.onPreExecute();
		//start uploading
	}
	  
	  
	public static interface UploadEventListener {
		void onSuccess(String body);
		void onFailure();
		void onPreExecute();
	}
	public static class BasicUploadEventListener implements UploadEventListener {
		@Override
		public void onSuccess(String body) {}
		@Override
		public void onFailure() {}
		@Override
		public void onPreExecute() {}
	}
}
