package com.fuyo.mde;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;
import com.fuyo.mde.HtmlCacheManager.OnCompleteListener;
import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ProgressBar;
import android.widget.Toast;

public class WebActivity extends Activity {
	private MyWebView webView;
	private SharedPreferences sharedPref;
	private TimeMeasure time;
	private String linkUrl;
	private int articleId;
	private String[] nouns;
	private String uuid;
	private double maxScroll;
	private String MY_AD_UNIT_ID;
	private AdView adView = null;
	private static final int THRESHOLD_ANGLE = 15;
	private static final int THRESHOLD_X = 150;
	private ProgressBar progress;
	float scale;
	private HtmlCacheManager cacheManager;
	private int[] followingItemIds = new int[]{};
	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        cacheManager = HtmlCacheManager.getInstance(this);
        MY_AD_UNIT_ID = getResources().getString(R.string.admob_id_webview);

        maxScroll = 0;
        scale = 1;
        uuid = sharedPref.getString(ItemListActivity.KEY_UUID, "none");
    	time = new TimeMeasure();

    	
    	Intent intent = getIntent();
    	if (intent != null) {
        	time.start();
        	linkUrl = intent.getStringExtra("url");
    		String title = intent.getStringExtra("title");
    		articleId = intent.getIntExtra("articleId", -1);
    		nouns = intent.getStringArrayExtra("nouns");
    		followingItemIds = intent.getIntArrayExtra("followingItemIds");
    		

    		
    		setTitle(title);
    		setContentView(R.layout.activity_web);
            LinearLayout layout = (LinearLayout)findViewById(R.id.WebLinearLayout);

            
            webView = new MyWebView(this);
        	webView.setWebViewClient(new MyWebViewClient());
//        	webView.getSettings().setBuiltInZoomControls(true);
        	webView.getSettings().setUseWideViewPort(true);
        	webView.setVerticalScrollbarOverlay(true);
//        	webView.setInitialScale(100);



        	LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
        			LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        	param.weight = 1;
        	layout.addView(webView, param);
        	cacheManager.getCachedArticle(articleId, new OnCompleteListener() {
				
				@Override
				public void onComplete(String url) {
					webView.loadUrl(url);
				}
			});

        	

    		progress = (ProgressBar)findViewById(R.id.view_actionbar_progress);

    		progress.setMax(100);
    		progress.setIndeterminate(false);

    		
    		adView = new AdView(this, AdSize.BANNER, MY_AD_UNIT_ID);
            layout.addView(adView);
            AdRequest request = new AdRequest();
			Set<String> keywords = new HashSet<String>();
			keywords.add("job");
			keywords.add("travel");
			for (String keyword : nouns) {
				keywords.add(keyword);
			}
            keywords.add("旅行");
            keywords.add("転職");
            request.addKeywords(keywords);

            adView.loadAd(request);
    		
    		
    	} else {
    		Toast.makeText(this, "Intent error", Toast.LENGTH_SHORT).show();
    		finish();
    	}

    }
    
	@Override
	public void onDestroy() {
		if (adView != null) {
			adView.removeAllViews();
			adView.destroy();
		}
		webView.stopLoading();
		webView.setWebViewClient(null);
		webView.setWebChromeClient(null);
		webView.removeAllViews();
		super.onDestroy();
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	time.stop();
    	cacheManager.stopBackgroundPrefetch();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	cacheManager.startBackgroundPrefetch(followingItemIds);
    	time.start();
    	
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
    @Override
    public void finish() {
		time.stop();
	    Intent intent = new Intent(this, LogUploader.class);
	    intent.putExtra("url", "http://matome.iijuf.net/_api.timeUploader.php");
	    intent.putExtra("paramKeys", new String[]{"uuid", "articleId", "time", "scroll"});
	    intent.putExtra("paramValues", new String[] {uuid, Integer.toString(articleId), Long.toString(time.getTime()), Double.toString(maxScroll)});
	    this.startService(intent);
    	super.finish();
    	overridePendingTransition(R.anim.push_left_in, R.anim.push_right_out);

    }
    
    public class MyWebView extends WebView {
    	GestureDetector gd;
    	public MyWebView(Context context) {
    		super(context);
    		 gd = new GestureDetector(context, onGestureListener);
    	}
    	public MyWebView(Context context, AttributeSet atters) {
    		super(context, atters);
    	}
    	@Override
    	public void onScrollChanged(final int l, final int t, final int oldl, final int oldt) {
			double r = (double)(webView.getScrollY() + webView.getHeight()) / (double)(webView.getContentHeight() * webView.getScale());
//			double r = (double)(t) / (double)(webView.getContentHeight());
			maxScroll = Math.max(r, maxScroll);
    	}
    	@Override
        public boolean onTouchEvent(MotionEvent event) {
            return (gd.onTouchEvent(event) || super.onTouchEvent(event));
        }
    	private final SimpleOnGestureListener onGestureListener = new SimpleOnGestureListener() {
    		@Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,float velocityY) {
                float deltax,deltay,velo;
//                int pref_browser_gesturevelo = 200;
                deltax = e2.getRawX()-e1.getRawX();
                deltay = Math.abs(e1.getRawY()-e2.getRawY());
                velo = Math.abs(velocityX);
     
                //pref_browser_gesturevelo is how fast finger moves.
                //pref_browser_gesturevelo set to 350 as default in my app
                
                if (deltax > THRESHOLD_X && deltay / deltax < Math.tan(Math.toRadians(THRESHOLD_ANGLE))) {
                   	finish();
                   	return true;
                }
                progress.setProgress(0);
                return super.onFling(e1, e2, velocityX, velocityY);
            }
    		@Override
    		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
    			// This is NOT the distance between e1 and e2.
    			float moveX = e2.getRawX() - e1.getRawX();
    			float absMoveY = Math.abs(e2.getRawY() - e1.getRawY());
    			if (moveX < 0) {
    				progress.setProgress(0);
    				return false;
    			}
    			int score = 0;
    			score = Math.min((int)Math.floor(moveX / THRESHOLD_X * 50), 50);
    			float angle = absMoveY / moveX;
    			score += Math.min((int)Math.floor(Math.tan(Math.toRadians(THRESHOLD_ANGLE)) / angle * 50), 50);
    			progress.setProgress(score);
    			return false;
    		}
    	};
    }
    
    private class MyWebViewClient extends WebViewClient {
    	@Override
    	public void onPageStarted(WebView view, String url, Bitmap favicon) {
    		super.onPageStarted(view, url, favicon);
    		cacheManager.stopBackgroundPrefetch();
    		if (!url.contains(linkUrl) && !url.contains(".jpg")) {

    			time.stop();
    		} else {
    			time.start();
    		}
    	}
    	@Override
    	public void onScaleChanged(WebView view, float oldScale, float newScale) {
    		scale = newScale;
    	}
    	@Override
    	public void onPageFinished(WebView view, String url) {
    		cacheManager.startBackgroundPrefetch(followingItemIds);
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
