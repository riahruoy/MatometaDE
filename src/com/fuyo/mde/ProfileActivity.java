package com.fuyo.mde;

import java.util.ArrayList;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ProfileActivity extends Activity {
	private LinearLayout topKeywordsList;
	private LinearLayout hateKeywordsList;
	private String uuid;
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
    	uuid = sharedPref.getString(ItemListActivity.KEY_UUID, "testUUID");

		setContentView(R.layout.activity_profile);
        initializeViews();
        
        updateKeywords();
        
	}
	
	private void initializeViews() {
		topKeywordsList = (LinearLayout)findViewById(R.id.topKeywordsLinearLayout);
		hateKeywordsList = (LinearLayout)findViewById(R.id.hateKeywordsLinearLayout);
	}
	private void updateKeywords() {
		final String url = "http://matome.iijuf.net/_api.getProfile.php";
		ArrayList<String> keys = new ArrayList<String>();
		ArrayList<String> values = new ArrayList<String>();
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
						topKeywordsList.removeAllViews();
						for (String noun : keywords) {
							TextView tv = new TextView(ProfileActivity.this);
							tv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
							tv.setGravity(Gravity.CENTER_HORIZONTAL);
							tv.setText(noun);
							topKeywordsList.addView(tv);
						}

						hateKeywordsList.removeAllViews();
						String[] hates = bodyArray[1].split("\t");
						for (String noun : hates) {
							TextView tv = new TextView(ProfileActivity.this);
							tv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
							tv.setGravity(Gravity.CENTER_HORIZONTAL);
							tv.setText(noun);
							hateKeywordsList.addView(tv);
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
