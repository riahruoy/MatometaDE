package com.fuyo.mde;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class UpdateCheckAsyncTask extends AsyncTask<String, Integer, String> {
	int resultStatus = 0;
	String result = "";
	Context context;
	int currentVersion;
	UpdateCheckListener updateCheckListener;
	UpdateCheckAsyncTask(Context context, int version, UpdateCheckListener updateCheckListener) {
		this.context = context;
		this.currentVersion = version;
		this.updateCheckListener = updateCheckListener;
	}
	@Override
	protected String doInBackground(String... params) {
		HttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet("http://matome.iijuf.net/apk/getApkList.php");
		  ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
				
			  @Override
			  public String handleResponse(HttpResponse response)
					  throws ClientProtocolException, IOException {
				  switch (response.getStatusLine().getStatusCode()) {
				  case HttpStatus.SC_OK:
					  String body = EntityUtils.toString(response.getEntity(), "UTF-8");
					  resultStatus = 1;
					  result = body;
					  return body;
				  default:
					  resultStatus = -1;
					  return null;
				  }
			  }
			    	  
		  };
		  try {
			  httpClient.execute(httpGet, responseHandler);
		  } catch (ClientProtocolException e) {
			  e.printStackTrace();
		  } catch (IOException e) {
			  e.printStackTrace();
		  }
		  httpClient.getConnectionManager().shutdown();

		
		return null;
	}
	  @Override
	  protected void onPostExecute(String _result) {
		  int latestVersion = -1;
		  String latestApkName = "";
		  if (result != null) {
			  String[] lines = result.split("\n");
			  for (String line : lines) {
				  if (!line.contains(",")) continue;
				  String[] str = line.split(",");
				  int apkVersion = Integer.valueOf(str[0]);
				  if (latestVersion < apkVersion) {
					  latestVersion = apkVersion;
					  latestApkName = str[1];
				  }
			  }
		  }
//		  if (latestVersion)
		  if (latestVersion > currentVersion) {
			  updateCheckListener.onNewVersionFound(latestApkName);
		  }
	  }
	  
	  public static interface UpdateCheckListener {
		  void onNewVersionFound(String apkName);
	  }
}
