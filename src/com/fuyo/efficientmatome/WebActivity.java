package com.fuyo.efficientmatome;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class WebActivity extends Activity {
	private WebView webView;
	private SharedPreferences sharedPref;
	private TimeMeasure time;
	private String linkUrl;
	private int articleId;
	private String uuid;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.activity_web);

        uuid = sharedPref.getString(LatestItem.KEY_UUID, "none");
    	time = new TimeMeasure();

    	
    	Intent intent = getIntent();
    	if (intent != null) {
        	time.start();
        	linkUrl = intent.getStringExtra("url");
    		String title = intent.getStringExtra("title");
    		articleId = intent.getIntExtra("articleId", -1);
    		setTitle(title);
        	webView = (WebView)findViewById(R.id.webView);
        	webView.setWebViewClient(new MyWebViewClient());
    		webView.loadUrl(linkUrl);
    		
    		
    		
    	} else {
    		Toast.makeText(this, "Intent error", Toast.LENGTH_SHORT).show();
    		finish();
    	}

    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	time.stop();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	time.start();
    }
    
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_BACK) {
    		if (webView.canGoBack()) {
    			webView.goBack();
    			if (!webView.canGoBack()) {
//	    			Toast.makeText(WebActivity.this, "start", Toast.LENGTH_SHORT).show();
    				time.start();
    			} else {
    				
    			}
    		} else {
//    			Toast.makeText(WebActivity.this, "upload", Toast.LENGTH_SHORT).show();
    			time.stop();
    		    Intent intent = new Intent(this, LogUploader.class);
    		    intent.putExtra("url", "http://matome.iijuf.net/_api.timeUploader.php");
    		    intent.putExtra("paramKeys", new String[]{"uuid", "articleId", "time"});
    		    intent.putExtra("paramValues", new String[] {uuid, Integer.toString(articleId), Long.toString(time.getTime())});
    		    this.startService(intent);
    			finish();
    		}
    		return true;
    	}
    	return super.onKeyDown(keyCode, event);
    }
    private class MyWebViewClient extends WebViewClient {
    	@Override
    	public void onPageStarted(WebView view, String url, Bitmap favicon) {
    		super.onPageStarted(view, url, favicon);
    		if (!url.contains(linkUrl) && !url.contains(".jpg")) {

    			time.stop();
//    			Toast.makeText(WebActivity.this, "stop", Toast.LENGTH_SHORT).show();
    		} else {
    			//support page2 or picture
    			time.start();
//    			Toast.makeText(WebActivity.this, "start", Toast.LENGTH_SHORT).show();
    		}
    	}
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.webactivity_item, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    	case R.id.action_toWeb:
    		Uri uri = Uri.parse(linkUrl);
    		Intent i = new Intent(Intent.ACTION_VIEW,uri);
    		startActivity(i);
    		return true;
    	}
    	return false;
    }
}
