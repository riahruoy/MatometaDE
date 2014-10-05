package com.fuyo.mde;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import android.content.Context;

public class PageBookmarkManager extends BasicPageManager {

	public PageBookmarkManager(Context context) {
		super(context, context.getFilesDir().getAbsolutePath() + "/bookmark");
	}

	@Override
	protected String getZipDownloadPath(int itemId) {
		// TODO Auto-generated method stub
		return "http://matome.iijuf.net/_api.getZipFromId.php?itemId="+itemId;

	}
	
	public void copyFromOtherFolder(File dir) {
		File bookmarkDir = new File(baseDir + "/" + dir.getName());
		if (!bookmarkDir.exists()) {
			bookmarkDir.mkdirs();
		}
		for (File file : dir.listFiles()) {
			String destPath = bookmarkDir + "/" + file.getName();
			try {
				copyTransfer(file.getAbsolutePath(), destPath);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	private static void copyTransfer(String srcPath, String destPath) 
	    throws IOException {
	    FileInputStream fis = new FileInputStream(srcPath);
	    FileChannel srcChannel = fis.getChannel();
	    FileOutputStream fos = new FileOutputStream(destPath);
	    FileChannel destChannel = fos.getChannel();
	    try {
	        srcChannel.transferTo(0, srcChannel.size(), destChannel);
	    } finally {
	        srcChannel.close();
	        destChannel.close();
	        fis.close();
	        fos.close();
	    }

	}

}
