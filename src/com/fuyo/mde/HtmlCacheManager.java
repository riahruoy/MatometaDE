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
import java.util.Date;
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
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.Browser.BookmarkColumns;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

public class HtmlCacheManager {
	private static HtmlCacheManager singleton = null;
	private final ConnectivityManager cm;
	private final Context context;
	private final SharedPreferences sharedPref;
	private boolean bgPrefetchStopFlag = true; 
	private static final String DIR_NAME = "html";
	public static int CACHE_BOOKMARK = 3;
	public static int CACHE_FULL = 2;
	public static int CACHE_LIGHT = 1;
	public static int CACHE_NONE = 0;
	private static final long BG_TIMEOUT = 5 * 60 * 1000;
	private FullCacheManager fullCacheManager;
	private LightCacheManager lightCacheManager;
	private HeadlineCacheManager detailCacheManager;
	private PageBookmarkManager pageBookmarkManager;
	private HeadlineBookmarkManager headlineBookmarkManager;
	private AsyncTask<int[], Void, Void> bgPrefetchTask2 = null;
	static HtmlCacheManager getInstance (final Context context) {
		if (singleton == null) {
			singleton = new HtmlCacheManager(context);
		}
		return singleton; 
	}
	private HtmlCacheManager (final Context context) {
		this.context = context;
		cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		fullCacheManager = new FullCacheManager(context);
		lightCacheManager = new LightCacheManager(context);
		detailCacheManager = new HeadlineCacheManager(context);
		pageBookmarkManager = new PageBookmarkManager(context);
		headlineBookmarkManager = new HeadlineBookmarkManager(context);
	}


    /**
     * getArticle from Cached file. If no cache, it'll download from the internet
     */
    public String getArticlePath(final int itemId) {
    	//TODO how to treat if bgPrefetch is downloading the same url?
    	if (pageBookmarkManager.isCached(itemId)) {
    		return pageBookmarkManager.getLocalPath(itemId);
    	} else if (fullCacheManager.isCached(itemId)) {
    		return fullCacheManager.getLocalPath(itemId);
    	} else if (lightCacheManager.isCached(itemId)) {
    		return lightCacheManager.getLocalPath(itemId);
    	} else {
    		return "http://matome.iijuf.net/function.createData.php?siteId=0&itemId=" + itemId;
    	}
    }
    public int getCachedType(final int itemId) {
    	if (pageBookmarkManager.isCached(itemId)) {
    		return CACHE_BOOKMARK;
    	}else if (fullCacheManager.isCached(itemId)) {
    		return CACHE_FULL;
    	} else if (lightCacheManager.isCached(itemId)) {
    		return CACHE_LIGHT;
    	} else {
    		return CACHE_NONE;
    	}
    }
    public boolean saveToBookmark (final int itemId) {
    	//TODO to be dealt with that headline is not saved on cache
    	if (!detailCacheManager.isCached(itemId)) return false;
    	headlineBookmarkManager.writeToCache(itemId, detailCacheManager.readFromCache(itemId));
    	if (fullCacheManager.isCached(itemId)) {
    		//copy
    		File dir = fullCacheManager.getItemDir(itemId);
    		pageBookmarkManager.copyFromOtherFolder(dir);
    	} else {
    		pageBookmarkManager.downloadAndSaveArticle(itemId);
    	}
    	return true;
    }
    public boolean removeFromBookmark (final int itemId) {
    	if (headlineBookmarkManager.isCached(itemId)) {
    		headlineBookmarkManager.deleteCache(itemId);
    	}
    	if (pageBookmarkManager.isCached(itemId)) {
    		pageBookmarkManager.deleteCache(itemId);
    	}
    	return true;
    }
    public boolean isBookmarked (final int itemId) {
    	return (headlineBookmarkManager.isCached(itemId)
    			&& pageBookmarkManager.isCached(itemId));
    }
    public void saveHeadlineToCache(final int itemId, final String data) {
    	detailCacheManager.writeToCache(itemId, data);
    }
    public String readHeadlineFromCache(final int itemId) {
    	if (headlineBookmarkManager.isCached(itemId)) {
    		return headlineBookmarkManager.readFromCache(itemId);
    	} else {
    		return detailCacheManager.readFromCache(itemId);
    	}
    }
    public void recordRead(final int itemId) {
    	detailCacheManager.updateRead(itemId);
    	if (headlineBookmarkManager.isCached(itemId)) {
    		headlineBookmarkManager.updateRead(itemId);
    	}
    }
    public boolean isHeadlineCached(final int itemId) {
    	return detailCacheManager.isCached(itemId);
    }
    public void startBackgroundPrefetch(final int[] itemIds) {
        startBackgroundPrefetch(itemIds, new OnUpdateListener() {
            @Override
            public void onUpdate(int itemId) {
            }
        });
    }
    public void startBackgroundPrefetch(final int[] itemIds, final OnUpdateListener listener) {
    	if (!sharedPref.getBoolean("pref_checkbox_prefetch", true)) return;
    	if (itemIds.length == 0) return;
    	if (bgPrefetchTask2 != null) return;
    	lightCacheManager.deleteCacheOneWeekAgo();
    	fullCacheManager.deleteCacheOneWeekAgo();
    	
    	BasicPageManager pageManager_tmp = lightCacheManager;
    	NetworkInfo info = cm.getActiveNetworkInfo();
    	if (info == null) {
    		//no network is active
    		return;
    	}
        if (context.getCacheDir().getFreeSpace() <= context.getCacheDir().getTotalSpace() * 0.1) {
            Log.d("error", "storage is full therefore downloading is stopped");
            return;
        }
    	if (info.getType() == ConnectivityManager.TYPE_WIFI
    			|| !sharedPref.getBoolean("pref_checkbox_prefetch_light_mode", true)) {
    		pageManager_tmp = fullCacheManager;
    	}
    	final BasicPageManager pageManager = pageManager_tmp;
		Log.d("prefetch", "prefetch: started");
    	bgPrefetchStopFlag = false;
    	if (bgPrefetchTask2 == null) {
    		bgPrefetchTask2 = new AsyncTask<int[], Void, Void>() {
    			
    			@Override
    			protected Void doInBackground(int[]... params) {
    				long start = System.currentTimeMillis();
    				int[] itemIds = params[0];
    				for (int i = 0; i < itemIds.length; i++) {
    					if (pageManager.isCached(itemIds[i])) continue;
    					pageManager.downloadAndSaveArticle(itemIds[i]);
    					Log.d("prefetch", "prefetch complete for itemId = " + itemIds[i]);
                        listener.onUpdate(itemIds[i]);

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
    public String getDetailMessage() {
    	Pair<Integer, Long> full = fullCacheManager.getPageSize();
    	Pair<Integer, Long> light = lightCacheManager.getPageSize();
    	double mb = (double)Math.round((double)(full.second + light.second) / 1000) / 1000;
    	String str = (full.first + light.first) + " articles  " + mb + " MB";
    	return str;
    }
    public int[] getBookmarkedList() {
    	return pageBookmarkManager.getCachedList();
    }
    public void deleteAllCache() {
    	lightCacheManager.deleteAllCache();
    	fullCacheManager.deleteAllCache();
    	detailCacheManager.deleteAllCache();
    }
    public interface OnUpdateListener {
        void onUpdate(int itemId);
    }

}
