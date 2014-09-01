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
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class HtmlCacheManager {
	private static HtmlCacheManager singleton = null;
	private final Context context;
	private TimerTask bgPrefetchTask = null;
	private Timer bgPrefetchTimer = null;
	static HtmlCacheManager getInstance (final Context context) {
		if (singleton == null) {
			singleton = new HtmlCacheManager(context);
		}
		return singleton; 
	}
	private HtmlCacheManager (final Context context) {
		this.context = context;
	}
	public void downloadArticleBackground(final String url, final OnCompleteListener listener) {
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

    public void get(final String url, final OnCompleteListener listener) {
    	String data = getFromCache(url);
    	if (data.length() == 0) {
    		downloadArticleBackground(url, listener);
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
    	bgPrefetchTask = new TimerTask() {

			@Override
			public void run() {
				//check service is running or not
				if (isDownloadServiceRunning()) {
					return;
				}
				calcSize();
				
			    Intent intent = new Intent(context, CacheDownloadIntentService.class);
			    for (int i = 0; i < urls.length; i++) {
			    	if (getFromCache(urls[i]).length() == 0) {
					    intent.putExtra("url", urls[i]);
					    context.startService(intent);
			    		break;
			    	}
			    }
			}
    	};
    	bgPrefetchTimer = new Timer();
    	bgPrefetchTimer.schedule(bgPrefetchTask, 0, 2000);
    }
    public void stopBackgroundPrefetch() {
    	if (bgPrefetchTimer != null) {
    		bgPrefetchTimer.cancel();
        	bgPrefetchTimer = null;
    	}
    }
    private boolean isDownloadServiceRunning() {
    	ActivityManager manager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
    	List<RunningServiceInfo> services = manager.getRunningServices(Integer.MAX_VALUE);
    	for (RunningServiceInfo info :services) {
    		if (info.service.getClassName().equals("CacheDownloadIntentService")) {
    			return true;
    		}
    	}
    	return false;
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
