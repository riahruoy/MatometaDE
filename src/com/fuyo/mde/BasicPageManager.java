package com.fuyo.mde;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

public abstract class BasicPageManager {
	final Context context;
	final String baseDir;
	public BasicPageManager (final Context context, final String baseDir) {
		this.context = context;
		this.baseDir = baseDir;
		File dir = new File(baseDir);
		if (!dir.exists()) {
			dir.mkdirs();
		}
	}
	protected void downloadAndSaveArticle(final int itemId) {
        String url = getZipDownloadPath(itemId);

		HttpParams httpParams = new BasicHttpParams();
		httpParams.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, Integer.valueOf(1000));
		httpParams.setParameter(CoreConnectionPNames.SO_TIMEOUT, Integer.valueOf(30000));
//		httpParams.setParameter(CoreProtocolPNames.USER_AGENT, "Mozilla/5.0 (Linux; U; Android 4.0.1; ja-jp; Galaxy Nexus Build/ITL41D) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30");
		HttpClient httpClient = new DefaultHttpClient(httpParams);

		HttpPost httpPost = new HttpPost(url);
		ResponseHandler<InputStream> responseHandler = new ResponseHandler<InputStream>() {
	
			@Override
			public InputStream handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				switch (response.getStatusLine().getStatusCode()) {
				case HttpStatus.SC_OK:
                    InputStream inputStream = response.getEntity().getContent();
                    writeToCache(itemId, inputStream);
					return null;
				default:
					return null;
				}
			}
		};


		try {
			httpClient.execute(httpPost, responseHandler);

		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		httpClient.getConnectionManager().shutdown();

	}
	abstract protected String getZipDownloadPath(final int itemId);
	protected String getLocalPath(final int itemId) {
		return "file://" + baseDir + "/" + itemId + "/index.html";
	}
    private void writeToCache(final int itemId, InputStream inputStream) {
		String cacheDirPath = baseDir;
		//TODO download picture with client, use picUrl newUrl table
    	ZipInputStream in = null;
    	ZipEntry zipEntry = null;
    	FileOutputStream out = null;
    	try {
    		//http://www.jxpath.com/beginner/zipFile/decode.html
    		in = new ZipInputStream(inputStream);
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
    public boolean isCached(final int itemId) {
   		File file = new File(baseDir + "/" + itemId + "/" + "index.html");
   		return file.exists();
    }
    public int[] getCachedList() {
    	ArrayList<Integer> list = new ArrayList<Integer> ();
    	File cacheDir = new File(baseDir);
    	for (File dir : cacheDir.listFiles()) {
    		int id = Integer.valueOf(dir.getName());
    		if (isCached(id)) {
    			list.add(id);
    		}
    	}
    	int[] itemIds = new int[list.size()];
    	for (int i = 0; i < list.size(); i++) {
    		itemIds[i] = list.get(i);
    	}
    	return itemIds;
    }
    public File getItemDir (final int itemId) {
    	return new File(baseDir + "/" + itemId);
    }
    public void deleteCache(final int itemId) {
    	File cacheDir = new File(baseDir + "/"+itemId);
    	if (!cacheDir.exists()) return;
    	for (File file : cacheDir.listFiles()) {
   			file.delete();
    	}
    	cacheDir.delete();
    }

    public void deleteCacheUntilMaximumFileNumber(final int num) {
        File cacheDir = new File(baseDir);
        if (!cacheDir.exists()) return;
        if (cacheDir.listFiles().length <= num) return;
        TreeMap<Long, File> map = new TreeMap<Long, File>();
        for (File file: cacheDir.listFiles()) {
            map.put(file.lastModified(), file);
        }
        int deleteNum = map.size() - num;
        int counter = 0;
        for (File file : map.values()) {
            int itemId = Integer.valueOf(file.getName());
            deleteCache(itemId);
            counter++;
            if (counter >= deleteNum) return;

        }

    }

    public void deleteCacheUntilQuota(long quota) {
        File cacheDir = new File(baseDir);
        if (!cacheDir.exists()) return;
        if (cacheDir.getFreeSpace() >= quota) return;
        TreeMap<Long, File> map = new TreeMap<Long, File>();
        for (File file: cacheDir.listFiles()) {
            map.put(file.lastModified(), file);
        }
        for (File file : map.values()) {
            int itemId = Integer.valueOf(file.getName());
            deleteCache(itemId);
            if (cacheDir.getFreeSpace() >= quota) {
                return;
            }
        }
    }
    public void deleteCacheOneWeekAgo() {
    	final long ttl = 1000 * 60 * 60 * 24 * 3;
    	File cacheDir = new File(baseDir);
    	if (!cacheDir.exists()) return;
    	for (File file : cacheDir.listFiles()) {
    		long now = System.currentTimeMillis();
    		if (file.lastModified() + ttl < now) {
    			deleteCache(Integer.valueOf(file.getName()));
    		}
    	}
    	//TODO
    }
    public void deleteAllCache() {
    	File cacheDir = new File(baseDir);
    	if (!cacheDir.exists()) return;
    	for (File file : cacheDir.listFiles()) {
    		deleteCache(Integer.valueOf(file.getName()));
    	}
    }
    protected Pair<Integer, Long> getPageSize() {
		long size = 0;
		int count = 0;
		File cacheDir = new File(baseDir);
		for (File dir : cacheDir.listFiles()) {
				count++;
			for (File file : dir.listFiles()) {
	   			size += file.length();
	
			}
		}
		return new Pair<Integer, Long>(count, size);
    }

}

