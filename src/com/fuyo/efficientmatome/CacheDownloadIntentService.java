package com.fuyo.efficientmatome;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class CacheDownloadIntentService extends IntentService {

	public CacheDownloadIntentService(String name) {
		super(name);
	}
	
	public CacheDownloadIntentService() {
		super("CacheDownloadIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		final String url = intent.getStringExtra("url");
		HtmlCacheManager cacheManager = HtmlCacheManager.getInstance(this);
		cacheManager.downloadArticleBackground(url, new HtmlCacheManager.OnCompleteListener() {
			@Override
			public void onComplete(String body) {
				Log.d("download", "download complete : " + url);
			}
		});
	}

}
