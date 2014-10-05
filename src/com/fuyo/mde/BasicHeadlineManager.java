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

import android.content.Context;

public class BasicHeadlineManager {
	final Context context;
	final String baseDir;
	public BasicHeadlineManager (final Context context, final String baseDir) {
		this.context = context;
		this.baseDir = baseDir;
		File dir = new File(baseDir);
		if (!dir.exists()) {
			dir.mkdirs();
		}
	}
	public void writeToCache(final int itemId, final String data) {
		String filepath = baseDir + "/" + itemId;
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
		String filepath = baseDir +"/"+ itemId;
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
		String filepath = baseDir +"/"+ itemId;
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
    	if (!cacheDir.exists()) return;
    	for (File file : cacheDir.listFiles()) {
    		deleteCache(Integer.valueOf(file.getName()));
    	}
    }
    public void deleteCache(final int itemId) {
    	File file = new File(baseDir + "/"+itemId);
    	if (file.exists()) {
    		file.delete();
    	}
    }

}
