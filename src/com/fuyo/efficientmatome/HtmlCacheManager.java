package com.fuyo.efficientmatome;

import java.io.BufferedReader;
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
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

public class HtmlCacheManager {
	private static HtmlCacheManager singleton = null;
	private final Context context;
	private boolean bgPrefetchStopFlag = true; 
	private static final long BG_TIMEOUT = 5 * 60 * 1000;
	private AsyncTask<String[], Void, Void> bgPrefetchTask2 = null;
	static HtmlCacheManager getInstance (final Context context) {
		if (singleton == null) {
			singleton = new HtmlCacheManager(context);
		}
		return singleton; 
	}
	private HtmlCacheManager (final Context context) {
		this.context = context;
	}
	private void downloadArticle(final String url, final OnCompleteListener listener) {
    	DownloadAsyncTask task = new DownloadAsyncTask(context, url, new String[]{}, new String[]{},
    			new DownloadAsyncTask.DownloadEventListener() {
					@Override
					public void onSuccess(String body) {
						writeToCache(url, body);
						listener.onComplete(body);
					}
					@Override
					public void onPreExecute() {
					}
					@Override
					public void onFailure() {
					}
				});
    	task.execute(new String[]{});
    	

    }
    private void writeToCache(final String url, final String body) {
    	try {
			final String filename = URLEncoder.encode(url, "UTF-8");
			File cacheDir = context.getCacheDir();
			File file = new File(cacheDir.getAbsolutePath(), filename);
			FileOutputStream os = new FileOutputStream(file);
			GZIPOutputStream gzos = new GZIPOutputStream(os);
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(gzos));
			writer.append(body);
			writer.close();
			gzos.close();
			os.close();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
    public void getCachedArticle(final String url, final OnCompleteListener listener) {
    	String data = getFromCache(url);
    	//TODO how to treat if bgPrefetch is downloading the same url?
    	if (data.length() == 0) {
    		downloadArticle(url, listener);
    	} else {
    		listener.onComplete(data);
    	}
    }
    public boolean isCached(final String url) {
    	try {
			final String filename = URLEncoder.encode(url, "UTF-8");
			File cacheDir = context.getCacheDir();
			File file = new File(cacheDir.getAbsolutePath(), filename);
			return file.exists();
    	} catch (UnsupportedEncodingException e) {
    		
    	}
    	return false;
    }
    private String getFromCache(final String url) {

    	try {
			final String filename = URLEncoder.encode(url, "UTF-8");
			File cacheDir = context.getCacheDir();
			File file = new File(cacheDir.getAbsolutePath(), filename);
			FileInputStream is = new FileInputStream(file);
			GZIPInputStream gzis = new GZIPInputStream(is);
	    	BufferedReader reader = new BufferedReader(new InputStreamReader(gzis));
	    	String str;
	    	StringBuilder builder = new StringBuilder();
	    	while ((str = reader.readLine()) != null) {
	    		builder.append(str).append('\n');
	    	}
	    	reader.close();
	    	gzis.close();
	    	is.close();

	    	return builder.toString();
		} catch (UnsupportedEncodingException e) {
			return "";
		} catch (FileNotFoundException e) {
//			e.printStackTrace();
			return "";
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
    }
    public interface OnCompleteListener {
    	void onComplete(String body);
    }
    public void startBackgroundPrefetch(final String[] urls) {
    	deleteCacheOneWeekAgo();
    	
		Log.d("prefetch", "prefetch: started");
    	bgPrefetchStopFlag = false;
    	if (bgPrefetchTask2 == null) {
    		bgPrefetchTask2 = new AsyncTask<String[], Void, Void>() {
    			
    			@Override
    			protected Void doInBackground(String[]... params) {
    				long start = System.currentTimeMillis();
    				String[] urls = params[0];
    				for (int i = 0; i < urls.length; i++) {
    					String body = DownloadAsyncTask.download(urls[i], new String[]{}, new String[]{});
    					writeToCache(urls[i], body);
    					Log.d("prefetch", "prefetch: complete " + urls[i]);
    	
    					
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
    		bgPrefetchTask2.execute(urls);
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
    	int deleteCount = 0;
    	File cacheDir = context.getCacheDir();
    	for (File file : cacheDir.listFiles()) {
    		if (file.getName().startsWith("http")) {
    			long last = file.lastModified();
    			if (last + 7 * 24 * 3600 * 1000 < System.currentTimeMillis()) {
    				file.delete();
    				deleteCount++;
    			}
    		}
    	}
    	Log.d("cache", "cache " + deleteCount + " files are deleted");
    	
    }
    
    public String getDetailMessage() {
    	long size = 0;
    	int count = 0;
    	File cacheDir = context.getCacheDir();
    	for (File file : cacheDir.listFiles()) {
    		if (file.getName().startsWith("http")) {
    			size += file.length();
    			count++;
    		}
    	}
    	double mb = (double)Math.round((double)size / 1000) / 1000;
    	String str = count + " files  " + mb + " MB";
    	return str;
    }
    
    public void calcSize() {
    	long size = 0;
    	int count = 0;
    	File cacheDir = context.getCacheDir();
    	for (File file : cacheDir.listFiles()) {
    		if (file.getName().startsWith("http")) {
    			size += file.length();
    			count++;
    		}
    	}
    	Log.d("cache", "cache size = " + Math.round((double)size / 1000 / 1000) + " MB  count = " + count);

    }
}
