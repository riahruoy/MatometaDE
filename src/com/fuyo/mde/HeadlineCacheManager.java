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

public class HeadlineCacheManager extends BasicHeadlineManager {
	public HeadlineCacheManager (final Context context) {
		super (context, context.getCacheDir().getAbsolutePath() + "/headline");
	}
	
}
