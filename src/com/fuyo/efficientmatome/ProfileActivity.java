package com.fuyo.efficientmatome;

import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

public class ProfileActivity extends Activity {
	private TextView[] topKeywords;
	private TextView[] hateKeywords;
	private String uuid;
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
    	uuid = sharedPref.getString(LatestItem.KEY_UUID, "testUUID");

		setContentView(R.layout.activity_profile);
        initializeViews();
        
        updateKeywords();
        
	}
	
	private void initializeViews() {
		final int KEYWORD_COUNT = 10;
		topKeywords = new TextView[KEYWORD_COUNT];
		topKeywords[0] = (TextView)findViewById(R.id.topKeyword0);
		topKeywords[1] = (TextView)findViewById(R.id.topKeyword1);
		topKeywords[2] = (TextView)findViewById(R.id.topKeyword2);
		topKeywords[3] = (TextView)findViewById(R.id.topKeyword3);
		topKeywords[4] = (TextView)findViewById(R.id.topKeyword4);
		topKeywords[5] = (TextView)findViewById(R.id.topKeyword5);
		topKeywords[6] = (TextView)findViewById(R.id.topKeyword6);
		topKeywords[7] = (TextView)findViewById(R.id.topKeyword7);
		topKeywords[8] = (TextView)findViewById(R.id.topKeyword8);
		topKeywords[9] = (TextView)findViewById(R.id.topKeyword9);

		hateKeywords = new TextView[KEYWORD_COUNT];
		hateKeywords[0] = (TextView)findViewById(R.id.hateKeyword0);
		hateKeywords[1] = (TextView)findViewById(R.id.hateKeyword1);
		hateKeywords[2] = (TextView)findViewById(R.id.hateKeyword2);
		hateKeywords[3] = (TextView)findViewById(R.id.hateKeyword3);
		hateKeywords[4] = (TextView)findViewById(R.id.hateKeyword4);
		hateKeywords[5] = (TextView)findViewById(R.id.hateKeyword5);
		hateKeywords[6] = (TextView)findViewById(R.id.hateKeyword6);
		hateKeywords[7] = (TextView)findViewById(R.id.hateKeyword7);
		hateKeywords[8] = (TextView)findViewById(R.id.hateKeyword8);
		hateKeywords[9] = (TextView)findViewById(R.id.hateKeyword9);

	}
	private void updateKeywords() {
		final String url = "http://matome.iijuf.net/_api.getProfile.php";
		ArrayList<String> keys = new ArrayList<String>();
		ArrayList<String> values = new ArrayList<String>();
		String uuid = "7dd714d8-51c5-4f3c-957a-783b2569b7fb";
		keys.add("uuid");
		values.add(uuid);
		DownloadAsyncTask dat = new DownloadAsyncTask(this, url, keys.toArray(new String[keys.size()]), values.toArray(new String[values.size()]),
				new DownloadAsyncTask.DownloadEventListener() {
					ProgressDialog dialog;
					@Override
					public void onSuccess(String body) {
						if (dialog.isShowing()) {
							dialog.dismiss();
						}
						
						String[] bodyArray = body.split("\n");
						if (bodyArray.length < 2) {
							return;
						}
						
						String[] keywords = bodyArray[0].split("\t");
						for (int i = 0; i < topKeywords.length; i++) {
							if (i < keywords.length) {
								topKeywords[i].setText(keywords[i]);
							} else {
								topKeywords[i].setText("");
							}
						}
						String[] hates = bodyArray[1].split("\t");
						for (int i = 0; i < hateKeywords.length; i++) {
							if (i < hateKeywords.length) {
								hateKeywords[i].setText(hates[i]);
							} else {
								hateKeywords[i].setText("");
							}
						}
					}
					
					@Override
					public void onPreExecute() {
						dialog = new ProgressDialog(ProfileActivity.this);
						dialog.setMessage("Loading...");
						dialog.show();
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void onFailure() {
						if (dialog.isShowing()) {
							dialog.dismiss();
						}
						
					}
		});
		dat.execute("");
	}

}
