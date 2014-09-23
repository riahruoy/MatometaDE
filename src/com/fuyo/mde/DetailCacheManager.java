package com.fuyo.mde;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import com.fuyo.mde.DownloadAsyncTask.DownloadEventListener;

public class DetailCacheManager {
	private static DetailCacheManager singleton = null;
	private final String CACHE_DIR = "/headline/";
	private final Context context;
	static DetailCacheManager getInstance(final Context context) {
		if (singleton == null) {
			singleton = new DetailCacheManager(context);
		}
		return singleton;
	}
	private DetailCacheManager (final Context context) {
		this.context = context;
	}

	public void writeToCache(final int itemId, final String data) {
		String filepath = context.getCacheDir() + CACHE_DIR + itemId;
		File file = new File(filepath);
		file.getParentFile().mkdirs();
		BufferedWriter out = null; 
		try {
			FileOutputStream fos = new FileOutputStream(file);
			out = new BufferedWriter(new OutputStreamWriter(fos));
			out.write(data);
			out.flush();
			out.close();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public String readFromCache(final int itemId) {
		String line = null;
		String filepath = context.getCacheDir() + CACHE_DIR + itemId;
		File file = new File(filepath);
		try {
			BufferedReader br = new BufferedReader(
					new InputStreamReader(new FileInputStream(file)));
	        line = br.readLine();
	        br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return line;
	}
	public boolean isCached(final int itemId) {
		String filepath = context.getCacheDir() + CACHE_DIR + itemId;
		File file = new File(filepath);
		return file.exists();
	}
	public void cancel() {
	}
}
