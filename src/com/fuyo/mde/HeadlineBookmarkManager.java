package com.fuyo.mde;

import android.content.Context;

public class HeadlineBookmarkManager extends BasicHeadlineManager {

	public HeadlineBookmarkManager(Context context) {
		super(context, context.getFilesDir().getAbsolutePath() + "/bookmark_headline");
		// TODO Auto-generated constructor stub
	}

}
