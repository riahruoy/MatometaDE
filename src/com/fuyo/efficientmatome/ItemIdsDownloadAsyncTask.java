package com.fuyo.efficientmatome;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;


import org.apache.http.Header;
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
import android.text.TextUtils;
import android.util.Log;



public class ItemIdsDownloadAsyncTask extends AsyncTask<String, Integer, Integer> {
	private String uuid;
	private String resultBody = "";
	private int resultStatus = 0;
	private int getItemType;
	private UploadEventListener uploadEventListener;
	public ItemIdsDownloadAsyncTask(Context context, String uuid, int getItemType, UploadEventListener listener) {
	    this.uuid = uuid;
	    this.uploadEventListener = listener;
	    this.getItemType = getItemType;
	  }
	  
	@Override
	protected Integer doInBackground(String... params) {
		HttpParams httpParams = new BasicHttpParams();
		httpParams.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, Integer.valueOf(1000));
		httpParams.setParameter(CoreConnectionPNames.SO_TIMEOUT, Integer.valueOf(30000));
		HttpClient httpClient = new DefaultHttpClient(httpParams);

		HttpPost httpPost = new HttpPost("http://matome.iijuf.net/_api.getItemIds.php");
		List<NameValuePair> param = new ArrayList<NameValuePair>();
		ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
	
			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				switch (response.getStatusLine().getStatusCode()) {
				case HttpStatus.SC_OK:
					String body = "";
					InputStream stream = null;
					if (isGZipHttpResponse(response)) {
						stream = new GZIPInputStream(response.getEntity().getContent());
					} else {
						stream = response.getEntity().getContent();
					}
					BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
					try {
					    StringBuilder buf = new StringBuilder();
					    String line;
					    while ((line = reader.readLine()) != null) {
					        buf.append(line + "\n");
					    }
					    body = buf.toString();
					} finally {
					    // レスポンスデータ（InputStream）を閉じる
					    stream.close();
					    reader.close();
					}
						
					Log.d("upload", body);
					resultBody = body;
					resultStatus = 1;
					return body;
				default:
					resultStatus = -1;
					return "NG";
				}
			}
			private boolean isGZipHttpResponse(HttpResponse response) {
			    Header header = response.getEntity().getContentEncoding();
			    if (header == null) return false;
			    
			    String value = header.getValue();
			    return (!TextUtils.isEmpty(value) && value.contains("gzip"));
			}
				    	  
		};
		param.add(new BasicNameValuePair("uuid", uuid));
		param.add(new BasicNameValuePair("type", String.valueOf(getItemType)));
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
