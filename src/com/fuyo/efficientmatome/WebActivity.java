package com.fuyo.efficientmatome;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
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
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Toast;

public class WebActivity extends Activity {
	private MyWebView webView;
	private SharedPreferences sharedPref;
	private TimeMeasure time;
	private String linkUrl;
	private int articleId;
	private String uuid;
	private double maxScroll;
	private static final String MY_AD_UNIT_ID = "ca-app-pub-1661412607542997/1910436460";
	private AdView adView;
	float scale;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
//        setContentView(R.layout.activity_web);

        maxScroll = 0;
        scale = 1;
        uuid = sharedPref.getString(LatestItem.KEY_UUID, "none");
    	time = new TimeMeasure();

    	
    	Intent intent = getIntent();
    	if (intent != null) {
        	time.start();
        	linkUrl = intent.getStringExtra("url");
    		String title = intent.getStringExtra("title");
    		articleId = intent.getIntExtra("articleId", -1);
    		setTitle(title);
//        	webView = (MyWebView)findViewById(R.id.webView);
    		setContentView(R.layout.activity_web);
            LinearLayout layout = (LinearLayout)findViewById(R.id.WebLinearLayout);

            
            webView = new MyWebView(this);
        	webView.setWebViewClient(new MyWebViewClient());
        	webView.getSettings().setBuiltInZoomControls(true);


        	LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
        			LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        	param.weight = 1;
        	layout.addView(webView, param);
    		webView.loadUrl(linkUrl);

    		
    		adView = new AdView(this, AdSize.BANNER, MY_AD_UNIT_ID);
            layout.addView(adView);
            adView.loadAd(new AdRequest());
    		
    		
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
                int pref_browser_gesturevelo = 350; 
                deltax = e2.getRawX()-e1.getRawX();
                deltay = Math.abs(e1.getRawY()-e2.getRawY());
                velo = Math.abs(velocityX);
     
                //pref_browser_gesturevelo is how fast finger moves.
                //pref_browser_gesturevelo set to 350 as default in my app
                if (deltax > 200 && deltay < 30 && velo > pref_browser_gesturevelo) {
                   	finish();
                   	return true;
                }
                return super.onFling(e1, e2, velocityX, velocityY);
            }
    	};
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
    	@Override
    	public void onScaleChanged(WebView view, float oldScale, float newScale) {
    		scale = newScale;
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
