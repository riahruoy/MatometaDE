package com.fuyo.efficientmatome;

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.R.integer;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.text.Html;
import android.util.Base64;
import android.view.Menu;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class LatestItem extends Activity implements ActionBar.OnNavigationListener{
	protected ListView listView = null;
	protected static final int IMGVIEW_ID = 0x7f190000;
	protected View mFooter = null;
	protected ItemAdapter adapter = null;
	protected GetItemAsyncTask mGetItemTask = null;
	protected String uuid = "testUIDD";
	static final String KEY_UUID = "uuid";
	List<Item> data = new ArrayList<Item>();
	protected OnScrollListener scrollListener;
	protected SharedPreferences sharedPref;
	private static final int TYPE_ALL = 0;
	private static final int TYPE_UNREAD = 1;
	private static final int TYPE_READ = 2;
	private static final int TYPE_SUGGEST = 100;	
	private int getItemType = TYPE_SUGGEST;
	private int versionCode = 0;
	SpinnerAdapter mSpinnerAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_latest_item);
        listView = (ListView)findViewById(R.id.listViewLatest);
        mFooter = getLayoutInflater().inflate(R.layout.listview_footer, null);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if (!sharedPref.contains(KEY_UUID)) {
        	uuid = UUID.randomUUID().toString();
        	Editor e = sharedPref.edit();
        	e.putString(KEY_UUID, uuid);
        	e.commit();
        } else {
        	uuid = sharedPref.getString(KEY_UUID, "testUUID");
        }
        PackageManager packageManager = this.getPackageManager();
 
        try {
               PackageInfo packageInfo = packageManager.getPackageInfo(this.getPackageName(), PackageManager.GET_ACTIVITIES);
               versionCode = packageInfo.versionCode;
          } catch (NameNotFoundException e) {
               e.printStackTrace();
          }

        
        
        listView.addFooterView(mFooter);
        adapter = new ItemAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {

				data.get(position).read = true;
				listView.invalidateViews();
				Item item = (Item)listView.getItemAtPosition(position);
				Intent intent = new Intent(LatestItem.this, WebActivity.class);
				intent.putExtra("title", item.title);
				intent.putExtra("url", item.link);
				intent.putExtra("articleId", item.id);
				startActivity(intent);
			}
		});
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int position, long id) {
				Item item = (Item)listView.getItemAtPosition(position);
				StringBuilder sb = new StringBuilder();
				sb.append("id : ").append(item.id).append('\n');
				new AlertDialog.Builder(LatestItem.this)
				.setTitle("debug")
				.setMessage(sb.toString())
				.show();
				return false;
			}
		});
        scrollListener = new OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
			}
			
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				if (totalItemCount == firstVisibleItem + visibleItemCount) {
					addtitionalReading();
				}
			}
		};
        listView.setOnScrollListener(scrollListener);
        final ActionBar bar = getActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        mSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.action_list, android.R.layout.simple_spinner_dropdown_item);
        bar.setListNavigationCallbacks(mSpinnerAdapter, this);
    }

    
    private void addtitionalReading() {
    	if (mGetItemTask != null && mGetItemTask.getStatus() == AsyncTask.Status.RUNNING) {
    		return;
    	}
    	int offset = data.size();
    	mGetItemTask = new GetItemAsyncTask(this, uuid, offset, 200,getItemType,versionCode, new GetItemAsyncTask.UploadEventListener() {
			
			@Override
			public void onSuccess(String body) {
				String[] lines = body.split("\n");
				for (int i = 0; i < lines.length; i++) {
					if (lines[i].indexOf("\t") == -1) {
						continue;
					}
					String line = lines[i].trim();
					
					data.add(Item.getFromLine(line));
				}
				adapter.notifyDataSetChanged();
				listView.invalidateViews();
			}
			
			@Override
			public void onPreExecute() {
				
			}
			
			@Override
			public void onFailure() {

			}
		});
    	mGetItemTask.execute("");
    }

    
    private static class Item {
//    	private static final SimpleDateFormat sdfTime = new SimpleDateFormat("");
    	public int id = -1;
    	public String title = "";
    	public String link = "";
    	public String date = "";
    	public String site = "";
    	public String content = "";
    	public byte[] icon;
    	public boolean read = false;
    	public boolean hasShown = false;
    	public int random;
    	public int time = 0;
    	public Item() {}
    	public static Item getFromLine(String line) {
    		Item item = new Item();
    		String[] column = line.split("\t");
    		item.id = Integer.valueOf(column[0]);
    		item.read = (Integer.valueOf(column[1]) > 0);
    		item.title = Html.fromHtml(column[2]).toString();
    		item.link = column[3];
    		item.date = column[4];
    		item.site = column[5];
    		item.time = Integer.valueOf(column[6]);
    		item.content = "";
    		item.icon = null;
    		if (column.length > 7) {
    			if (column[7].length() > 0) {
    				item.icon = Base64.decode(column[7], Base64.DEFAULT);
    			} else {
    				item.icon = null;
    			}
//    			item.content = Html.fromHtml(column[7]).toString();
    		}
    		item.random = (int)Math.floor(Math.random() * 10);
    		return item;
    	}
    }
    
    private class ItemAdapter extends BaseAdapter {
		@Override
		public int getCount() {
			return data.size();
		}

		@Override
		public Object getItem(int position) {
			return data.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(R.layout.latest_row, null);
			}
			TextView textViewTitle = (TextView)convertView.findViewById(R.id.textViewTitle);
			TextView textViewDate = (TextView)convertView.findViewById(R.id.textViewDate);
			TextView textViewSite = (TextView)convertView.findViewById(R.id.textViewSite);
			TextView textViewTime = (TextView)convertView.findViewById(R.id.textViewTime);
//			TextView textContent = (TextView)convertView.findViewById(R.id.textContent);

			LinearLayout llRow = (LinearLayout)convertView.findViewById(R.id.linearLayoutRow);
			Item item = data.get(position);
			textViewTitle.setText(item.title);
			textViewSite.setText(item.site);
			textViewDate.setText(item.date);
			
			ImageView imgViewIcon = (ImageView)llRow.findViewById(IMGVIEW_ID);
			if (imgViewIcon != null) {
				llRow.removeView(imgViewIcon);
			}
			
			if (item.icon != null) {
				imgViewIcon = new ImageView(LatestItem.this);
				imgViewIcon.setId(IMGVIEW_ID);

//				imgViewIcon.setBackgroundColor(Color.DKGRAY);
				Bitmap bmp = BitmapFactory.decodeByteArray(item.icon, 0, item.icon.length);
				imgViewIcon.setImageBitmap(bmp);
				
				if (position % 2 == 0) {
					imgViewIcon.setPadding(5, 5, 20, 5);
					llRow.addView(imgViewIcon, 0);
				} else {
					imgViewIcon.setPadding(20, 5, 5, 5);
					llRow.addView(imgViewIcon, 1);
				}
			}


			
//			final int QUOTE_SIZE = 100;
//			String content = item.content.length() > QUOTE_SIZE ? item.content.substring(0, QUOTE_SIZE - 1) : item.content;
//			textContent.setText(content);
			

			String timeStr = "";
			if (item.time > 0) {
				timeStr = Integer.toString((int)Math.round((double)item.time / 60)) + "min " + Integer.toString(item.time % 60) + "sec";
			}
			textViewTime.setText(timeStr);
			if (item.read) {
				textViewTitle.setTextColor(Color.DKGRAY);
				textViewSite.setTextColor(Color.DKGRAY);
				textViewDate.setTextColor(Color.DKGRAY);
				textViewTime.setTextColor(Color.DKGRAY);
			} else {
				textViewTitle.setTextColor(Color.WHITE);
				textViewSite.setTextColor(Color.WHITE);
				textViewDate.setTextColor(Color.WHITE);
				textViewTime.setTextColor(Color.WHITE);
			}
			if (!item.hasShown) { 
				Animation anim = AnimationUtils.loadAnimation(LatestItem.this, R.anim.item_motion);
				convertView.startAnimation(anim);
				item.hasShown = true;
			}
			return convertView;
		}
    	
    }

	private void reloadDataSet() {
		data.clear();
		adapter.notifyDataSetChanged();
		listView.invalidateViews();
	}


	@Override
	public boolean onNavigationItemSelected(int position, long itemId) {
		//0: suggest, 1: all, 2: unread, 3:read
		switch (position) {
		case 0:
			getItemType = TYPE_SUGGEST;
			break;
		case 1:
			getItemType = TYPE_ALL;
			break;
		case 2:
			getItemType = TYPE_UNREAD;
			break;
		case 3:
			getItemType = TYPE_READ;
			break;
		}
		reloadDataSet();
		return true;
	}
}
