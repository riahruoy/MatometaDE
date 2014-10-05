package com.fuyo.mde;

import android.content.Context;

public class LightCacheManager extends BasicPageManager {

	public LightCacheManager(final Context context) {
		super(context, context.getCacheDir().getAbsolutePath() + "/html_light");
		// TODO Auto-generated constructor stub
	}

	@Override
	protected String getZipDownloadPath(final int itemId) {
		return "http://matome.iijuf.net/_api.getZipFromId.php?light=true&itemId="+itemId;
	}

}
