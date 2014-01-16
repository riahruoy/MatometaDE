package com.fuyo.efficientmatome;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebActivity extends Activity {
	private WebView webView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
    	webView = (WebView)findViewById(R.id.webView);
    	webView.setWebViewClient(new WebViewClient());
    	
    	Intent intent = getIntent();
    	if (intent != null) {
    		String url = intent.getStringExtra("url");
    		String title = intent.getStringExtra("title");
    		setTitle(title);
    		webView.loadUrl(url);
    	}

    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_BACK) {
    		if (webView.canGoBack()) {
    			webView.goBack();
    		} else {
    			finish();
    		}
    		return true;
    	}
    	return super.onKeyDown(keyCode, event);
    }
}
