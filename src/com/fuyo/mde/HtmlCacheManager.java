package com.fuyo.mde;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

public class HtmlCacheManager {
	private static HtmlCacheManager singleton = null;
	private final Context context;
	private boolean bgPrefetchStopFlag = true; 
	private static final long BG_TIMEOUT = 5 * 60 * 1000;
	private AsyncTask<int[], Void, Void> bgPrefetchTask2 = null;
	static HtmlCacheManager getInstance (final Context context) {
		if (singleton == null) {
			singleton = new HtmlCacheManager(context);
		}
		return singleton; 
	}
	private HtmlCacheManager (final Context context) {
		this.context = context;
	}
	private static byte[] download(final String url) {

		HttpParams httpParams = new BasicHttpParams();
		httpParams.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, Integer.valueOf(1000));
		httpParams.setParameter(CoreConnectionPNames.SO_TIMEOUT, Integer.valueOf(30000));
		httpParams.setParameter(CoreProtocolPNames.USER_AGENT, "Mozilla/5.0 (Linux; U; Android 4.0.1; ja-jp; Galaxy Nexus Build/ITL41D) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30");
		HttpClient httpClient = new DefaultHttpClient(httpParams);

		HttpPost httpPost = new HttpPost(url);
		ResponseHandler<byte[]> responseHandler = new ResponseHandler<byte[]>() {
	
			@Override
			public byte[] handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				switch (response.getStatusLine().getStatusCode()) {
				case HttpStatus.SC_OK:
					return EntityUtils.toByteArray(response.getEntity());
				default:
					return null;
				}
			}
		};

		byte[] result = null;
		try {
			result = httpClient.execute(httpPost, responseHandler);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		httpClient.getConnectionManager().shutdown();
		return result;
	}

	private void downloadArticle(final int itemId) {
		String url = getZipDownloadPath(itemId);
		byte[] zipData = download(url);
		writeToCache(itemId, zipData);

    }
    private void writeToCache(final int itemId, final byte[] body) {
		String cacheDirPath = context.getCacheDir().getAbsolutePath();
		//TODO download picture with client, use picUrl newUrl table
    	ZipInputStream in = null;
    	ZipEntry zipEntry = null;
    	FileOutputStream out = null;
    	try {
    		//http://www.jxpath.com/beginner/zipFile/decode.html
    		in = new ZipInputStream(new ByteArrayInputStream(body));
    		while ((zipEntry = in.getNextEntry()) != null) {
    			File newFile = new File(cacheDirPath, zipEntry.getName());
    			if (zipEntry.isDirectory()) {
    				newFile.mkdirs();
    			} else {
    				newFile.getParentFile().mkdirs();
    				out = new FileOutputStream(newFile);
    				byte[] buf = new byte[1024];
    				int size = 0;
    				while ((size = in.read(buf)) > 0) {
    					out.write(buf, 0, size);
    				}
    				out.close();
    				out = null;
    			}
    			in.closeEntry();
    		}
			in.close();

    	} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    /**
     * getArticle from Cached file. If no cache, it'll download from the internet
     * @param url
     * @param listener
     */
    public void getCachedArticle(final int itemId, final OnCompleteListener listener) {
    	//TODO how to treat if bgPrefetch is downloading the same url?
    	if (!isCached(itemId)) {
    		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void> (){
				@Override
				protected Void doInBackground(Void... params) {
					downloadArticle(itemId);
					return null;
				}
				@Override
				protected void onPostExecute(Void result) {
					listener.onComplete(getLocalPath(itemId));
				}
    		};
    		task.execute();
    	} else {
    		listener.onComplete(getLocalPath(itemId));
    	}
    }
    private String getLocalPath (final int itemId) {
   		String cacheDirPath = context.getCacheDir().getAbsolutePath();
   		File file = new File(cacheDirPath + "/" + itemId + "/" + "index.html");
    	return "file://"+file.getAbsolutePath();
    }
    public boolean isCached(final int itemId) {
   		String cacheDirPath = context.getCacheDir().getAbsolutePath();
   		File file = new File(cacheDirPath + "/" + itemId + "/" + "index.html");
   		return file.exists();
    }
    private String getZipDownloadPath(final int itemId) {
		return "http://matome.iijuf.net/_api.getZipFromId.php?itemId="+itemId;
    }
    public interface OnCompleteListener {
    	void onComplete(String localPath);
    }
    public void startBackgroundPrefetch(final int[] itemIds) {
    	deleteCacheOneWeekAgo();
    	
		Log.d("prefetch", "prefetch: started");
    	bgPrefetchStopFlag = false;
    	if (bgPrefetchTask2 == null) {
    		bgPrefetchTask2 = new AsyncTask<int[], Void, Void>() {
    			
    			@Override
    			protected Void doInBackground(int[]... params) {
    				long start = System.currentTimeMillis();
    				int[] itemIds = params[0];
    				for (int i = 0; i < itemIds.length; i++) {
    					if (isCached(itemIds[i])) continue;
    					String url = getZipDownloadPath(itemIds[i]);
    					byte[] zipBody = download(url);
    					writeToCache(itemIds[i], zipBody);
    					Log.d("prefetch", "prefetch: complete " + url);
    	
    					
    					if (bgPrefetchStopFlag) {
    						Log.d("prefetch", "prefetch: is cancelled with flag");
    						return null;
    					}
    					long now = System.currentTimeMillis();
    					// over 5 minutes
    					if (now - start > BG_TIMEOUT) {
    						Log.d("prefetch", "prefetch: is cancelled with timeout");
    						return null;
    					}
    				}
    				return null;
    			}
    			
    		};
    		bgPrefetchTask2.execute(itemIds);
    	} else {
    		Log.d("prefetch", "prefetch: but skipped");
    	}
    }
    public void stopBackgroundPrefetch() {
    	bgPrefetchStopFlag = true;
    	if (bgPrefetchTask2 != null) {
    		bgPrefetchTask2.cancel(true);
    		bgPrefetchTask2 = null;
    	}
		Log.d("prefetch", "prefetch: stopped");
    	
    }
    public void deleteAllCache() {
    	File cacheDir = context.getCacheDir();
    	for (File file : cacheDir.listFiles()) {
    		if (file.getName().startsWith("http")) {
    			file.delete();
    		}
    	}
    }
    public void deleteCacheOneWeekAgo() {
    	//TODO
    }
    
    public String getDetailMessage() {
    	long size = 0;
    	int count = 0;
    	File cacheDir = context.getCacheDir();
    	for (File file : cacheDir.listFiles()) {
   			size += file.length();
   			count++;

    	}
    	double mb = (double)Math.round((double)size / 1000) / 1000;
    	String str = count + " files  " + mb + " MB";
    	return str;
    }
    
}
