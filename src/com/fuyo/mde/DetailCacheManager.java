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
	private final String baseDir;
	private final Context context;
	public DetailCacheManager (final Context context) {
		this.context = context;
		baseDir = context.getCacheDir().getAbsolutePath() + "/headline";
	}

	public void writeToCache(final int itemId, final String data) {
		String filepath = baseDir + itemId;
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
		String filepath = baseDir + itemId;
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
	public void updateRead(final int itemId) {
		String str = readFromCache(itemId);
		String[] columns = str.split("\t");
		columns[1] = "1";
		String line = "";
		for (int i = 0; i < columns.length; i++) {
			if (i != 0) {
				line += "\t";				
			}
			line += columns[i];
		}
		writeToCache(itemId, line);
	}
	public boolean isCached(final int itemId) {
		String filepath = baseDir + itemId;
		File file = new File(filepath);
		return file.exists();
	}
    public void deleteCacheOneWeekAgo() {
    	final long ttl = 1000 * 60 * 60 * 24 * 3;
    	
    	File cacheDir = new File(baseDir);
    	for (File file : cacheDir.listFiles()) {
    		long now = System.currentTimeMillis();
    		if (file.lastModified() + ttl < now) {
    			deleteCache(Integer.valueOf(file.getName()));
    		}
    	}
    }
    public void deleteAllCache() {
    	File cacheDir = new File(baseDir);
    	for (File file : cacheDir.listFiles()) {
    		deleteCache(Integer.valueOf(file.getName()));
    	}
    }
    private void deleteCache(final int itemId) {
    	File file = new File(baseDir + "/"+itemId);
    	file.delete();
    }
}
