package com.fuyo.mde;

import android.content.Context;

public class FullCacheManager extends BasicPageManager {

	public FullCacheManager(final Context context) {
		super(context, context.getCacheDir().getAbsolutePath() + "/html_full");
		// TODO Auto-generated constructor stub
	}

	@Override
	protected String getZipDownloadPath(int itemId) {
		return "http://matome.iijuf.net/_api.getZipFromId.php?itemId="+itemId;
	}

}
