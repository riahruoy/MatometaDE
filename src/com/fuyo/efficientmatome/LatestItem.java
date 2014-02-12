package com.fuyo.efficientmatome;

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.ads.Ad;
import com.google.ads.AdListener;
import com.google.ads.AdRequest;
import com.google.ads.AdRequest.ErrorCode;
import com.google.ads.AdSize;
import com.google.ads.AdView;

import android.net.Uri;
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
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
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
import android.support.v4.app.ActionBarDrawerToggle.Delegate;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
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
	protected ItemAdAdapter adAdapter = null;
	protected int[] itemIds = new int[]{};
	protected GetItemAsyncTask mGetItemTask = null;
	protected String uuid = "testUIDD";
	static final String KEY_UUID = "uuid";
	static final String KEY_DEFAULT_TYPE = "default_type";
	List<Item> data = new ArrayList<Item>();
	protected OnScrollListener scrollListener;
	protected SharedPreferences sharedPref;
	private static final int TYPE_ALL = 0;
	private static final int TYPE_UNREAD = 1;
	private static final int TYPE_READ = 2;
	private static final int TYPE_SUGGEST = 100;	
	private int getItemType = TYPE_SUGGEST;
	private static final int AD_INTERVAL = 20;
	private int versionCode = 0;
	SpinnerAdapter mSpinnerAdapter;
	private static final String MY_AD_UNIT_ID = "ca-app-pub-1661412607542997/3526770464";

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
        adAdapter = new ItemAdAdapter(this, adapter);
        listView.setAdapter(adAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {


				data.get(adAdapter.toBasePosition(position)).read = true;
				listView.invalidateViews();
				Item item = (Item)listView.getItemAtPosition(position);
				Intent intent = new Intent(LatestItem.this, WebActivity.class);
				intent.putExtra("title", item.title);
				intent.putExtra("url", item.link);
				intent.putExtra("articleId", item.id);
				intent.putExtra("nouns", item.nouns);
				startActivity(intent);
		    	overridePendingTransition(R.anim.push_right_in, R.anim.push_left_out);
			}
		});
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int position, long id) {
				if (adAdapter.isAd(position)) return false;
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

				if (totalItemCount < firstVisibleItem + visibleItemCount + 40) {
					addtitionalReading();
				}
			}
		};
        listView.setOnScrollListener(scrollListener);
        final ActionBar bar = getActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        mSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.action_list, android.R.layout.simple_spinner_dropdown_item);
        bar.setListNavigationCallbacks(mSpinnerAdapter, this);
        UpdateCheckAsyncTask ucat = new UpdateCheckAsyncTask(this, versionCode, new UpdateCheckAsyncTask.UpdateCheckListener() {
			
			@Override
			public void onNewVersionFound(final String apkName) {
				new AlertDialog.Builder(LatestItem.this)
					.setTitle("New Version Found")
					.setMessage("�V�����o�[�W����:" + apkName + "��������܂����B�_�E�����[�h���܂���")
					.setPositiveButton("�_�E�����[�h", new OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Uri uri = Uri.parse("http://matome.iijuf.net/apk/" + apkName);
							Intent i = new Intent(Intent.ACTION_VIEW, uri);
							startActivity(i);
							finish();
						}

					})
					.setNegativeButton("�L�����Z��", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					})
					.setCancelable(true)
					.show();
				
			}
		});
        ucat.execute(new String[]{});

//        adView = new AdView(this, AdSize.BANNER, MY_AD_UNIT_ID);
//        LinearLayout layout = (LinearLayout)findViewById(R.id.LatestItemLayout);
//        layout.addView(adView);
//        AdRequest request = new AdRequest();
//        request.addKeyword("travel");
//        request.addKeyword("job");
//        request.addKeyword("english");
//        adView.loadAd(request);
    }
	@Override
	public void onDestroy() {
//		adView.destroy();
		super.onDestroy();
	}
    
    private void addtitionalReading() {
    	if (mGetItemTask != null && mGetItemTask.getStatus() == AsyncTask.Status.RUNNING) {
    		mGetItemTask.cancel(true);
    		Log.d("loading", "itemTask is cancelled");
//    		return;
    	}
    	if (itemIds.length == 0) {
    		Log.d("loading", "itemIds.length == 0");
//    		return;
    	}
    	final int offset = data.size();
    	final int LOADSIZE = 10;
    	int[] loadIds = new int [LOADSIZE]; 
    	for (int i = 0; i + offset < itemIds.length && i < LOADSIZE; i++) {
    		loadIds[i] = itemIds[i + offset];
    	}
    	mGetItemTask = new GetItemAsyncTask(this, uuid, loadIds, new GetItemAsyncTask.UploadEventListener() {
			
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
	    		Log.d("loading", "loading finished : " + offset + " -> " + data.size());
				adAdapter.notifyDataSetChanged();
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
    	public String[] nouns;
    	public Item() {}
    	public static Item getFromLine(String line) {
    		Item item = new Item();
    		String[] column = line.split("\t");
    		item.id = Integer.valueOf(column[0]);
    		Log.d("test", "linecount : " + column.length);
    		item.read = (Integer.valueOf(column[1]) > 0);
    		item.title = Html.fromHtml(column[2]).toString();
    		item.link = column[3];
    		item.date = column[4];
    		item.site = column[5];
    		item.time = Integer.valueOf(column[6]);
    		item.content = "";
    		item.icon = null;
    		if (column.length == 8) {
    			Log.d("column", line);
    		}
    		if (column[7].length() > 1) {
    			item.icon = Base64.decode(column[7], Base64.DEFAULT);
    		} else {
    			item.icon = null;
    		}
    		item.nouns = column[8].split(",");
    		item.random = (int)Math.floor(Math.random() * 10);
    		return item;
    	}
    }

    private class ItemAdAdapter extends ItemAdapter implements AdListener {
    	ItemAdapter delegate;
    	Activity activity;
    	public ItemAdAdapter(Activity activity, ItemAdapter delegate) {
    		this.delegate = delegate;
    		this.activity = activity;
    	}
    	@Override
    	public View getView(int position, View convertView, ViewGroup parent) {
    		if (isAd(position)) {
    			if (convertView instanceof AdView) {
    				
    				Set<String> keywords = new HashSet<String>();
    				keywords.add("job");
    				keywords.add("travel");
    				Item item = (Item)getItem(position - 1);
    				for (String keyword : item.nouns) {
   						keywords.add(keyword);
    				}
    		        AdRequest request = new AdRequest();

    		        request.addKeywords(keywords);
    		        AdView adView = (AdView)convertView;
    		        adView.loadAd(request);
    				return convertView;
    			} else {
    				AdView adView = new AdView(activity, AdSize.BANNER, MY_AD_UNIT_ID);
    				float density = activity.getResources().getDisplayMetrics().density;
    		        int height = Math.round(AdSize.BANNER.getHeight() * density);
    		        AbsListView.LayoutParams params = new AbsListView.LayoutParams(
    		            AbsListView.LayoutParams.FILL_PARENT,
    		            height);
    		        adView.setLayoutParams(params);
    		        AdRequest request = new AdRequest();

    		        Set<String> keywords = new HashSet<String>();
    				keywords.add("job");
    				keywords.add("travel");
    				Item item = (Item)getItem(position - 1);
    				for (String keyword : item.nouns) {
   						keywords.add(keyword);
    				}
    				request.addKeywords(keywords);
    		        adView.loadAd(request);
    		        return adView;
    			}
    		} else {
    			return delegate.getView(toBasePosition(position), convertView, parent);
    		}
    	}
    	@Override
    	public long getItemId(int position) {
    		return position;
    	}
    	@Override
    	public int getViewTypeCount() {
    		return delegate.getViewTypeCount() + 1;
    	}
    	@Override
    	public int getItemViewType(int position) {
    		if (isAd(position)) {
    			return delegate.getViewTypeCount();
    		} else {
    			return delegate.getItemViewType(toBasePosition(position));
    		}
    	}
    	@Override
    	public int getCount() {
    		return delegate.getCount() + adCount(delegate.getCount());
    	}
    	@Override
    	public Object getItem(int position) {
    		if (isAd(position)) {
    			return null;
    		} else {
    			return delegate.getItem(toBasePosition(position));
    		}
    	}
    	
		@Override
		public void onDismissScreen(Ad arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onFailedToReceiveAd(Ad arg0, ErrorCode arg1) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onLeaveApplication(Ad arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onPresentScreen(Ad arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onReceiveAd(Ad arg0) {
			// TODO Auto-generated method stub
			
		}
		
		private int toBasePosition(int position) {
			return position - adCount(position);
		}
		private int adCount(int position) {
			return (int)Math.floor((position + 1) / AD_INTERVAL);
		}
		private boolean isAd(int position) {
			return ((position + 1) % AD_INTERVAL == 0);
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
			if (convertView == null || convertView instanceof AdView) {
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
				textViewSite.setTextColor(Color.rgb(255, 160, 122));
				textViewDate.setTextColor(Color.GRAY);
				textViewTime.setTextColor(Color.GRAY);
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

		
		ItemIdsDownloadAsyncTask task = new ItemIdsDownloadAsyncTask(this, uuid, getItemType, new ItemIdsDownloadAsyncTask.UploadEventListener() {
			ProgressDialog dialog;
			@Override
			public void onSuccess(String body) {
				if (dialog.isShowing()) {
					dialog.dismiss();
				}
				body = body.replace("\n", "");
				String[] strIds = body.split("\t");
				itemIds = new int[strIds.length];
				for (int i = 0; i < strIds.length; i++) {
					itemIds[i] = Integer.valueOf(strIds[i]);
				}
			}
			
			@Override
			public void onPreExecute() {
				dialog = new ProgressDialog(LatestItem.this);
				dialog.setMessage("Loading...");
				dialog.show();
			}
			
			@Override
			public void onFailure() {
				if (dialog.isShowing()) {
					dialog.dismiss();
				}
			}
		});
		task.execute("");
		
		try {
			task.get(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		data.clear();//order is important
		itemIds = new int[]{};
		adAdapter.notifyDataSetChanged();
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
