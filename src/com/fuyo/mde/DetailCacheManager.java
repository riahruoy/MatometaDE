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
	protected GetItemAsyncTask mGetItemTask = null;
	static DetailCacheManager getInstance(final Context context) {
		if (singleton == null) {
			singleton = new DetailCacheManager(context);
		}
		return singleton;
	}
	private DetailCacheManager (final Context context) {
		this.context = context;
	}
	public void getCachedItemIds(final String uuid, final int[] loadIds, final DownloadEventListener listener) {
		ArrayList<Integer> toBeDownloadedIdList = new ArrayList<Integer>();
		for (int i = 0; i < loadIds.length; i++) {
			if (isCached(loadIds[i])) {
				listener.onSuccess(readFromCache(loadIds[i]));
			} else {
				toBeDownloadedIdList.add(loadIds[i]);
			}
		}
		if (toBeDownloadedIdList.size() == 0) return;
		int[] toBeDownloadedIds = new int[toBeDownloadedIdList.size()];
		for (int i = 0; i < toBeDownloadedIdList.size(); i++) {
			toBeDownloadedIds[i] = toBeDownloadedIdList.get(i);
		}
    	if (mGetItemTask != null && mGetItemTask.getStatus() == AsyncTask.Status.RUNNING) {
    		mGetItemTask.cancel(true);
    		Log.d("loading", "itemTask is cancelled");
//    		return;
    	}

    	mGetItemTask = new GetItemAsyncTask(context, uuid, toBeDownloadedIds, new GetItemAsyncTask.UploadEventListener() {
			@Override
			public void onSuccess(String body) {
				String[] lines = body.split("\n");
				for (int i = 0; i < lines.length; i++) {
					if (lines[i].indexOf("\t") == -1) {
						continue;
					}
					String line = lines[i].trim();
					String[] column = line.split("\t");
					writeToCache(Integer.valueOf(column[0]), line);
					listener.onSuccess(line);
				}
			}
			@Override
			public void onPreExecute() {
			}
			@Override
			public void onFailure() {
			}
		});
    	mGetItemTask.execute("");


	}
	private void writeToCache(final int itemId, final String data) {
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
	private String readFromCache(final int itemId) {
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
		if (mGetItemTask != null) {
			mGetItemTask.cancel(true);
			mGetItemTask = null;
		}
	}
}
