package com.fuyo.efficientmatome;

import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;
import android.os.Bundle;
import android.R.integer;
import android.app.Activity;
import android.content.Intent;
import android.text.Html;
import android.view.Menu;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class LatestItem extends Activity {
	protected ListView listView = null;
	protected View mFooter = null;
	protected ItemAdapter adapter = null;
	protected GetItemAsyncTask mGetItemTask = null;
	protected String mUid = "testUID";
	List<Item> data = new ArrayList<Item>();
	protected OnScrollListener scrollListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_latest_item);
        listView = (ListView)findViewById(R.id.listViewLatest);
        mFooter = getLayoutInflater().inflate(R.layout.listview_footer, null);
        listView.addFooterView(mFooter);
        adapter = new ItemAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				Item item = (Item)listView.getItemAtPosition(position);
				Intent intent = new Intent(LatestItem.this, WebActivity.class);
				intent.putExtra("title", item.title);
				intent.putExtra("url", item.link);
				startActivity(intent);
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
    }

    
    private void addtitionalReading() {
    	if (mGetItemTask != null && mGetItemTask.getStatus() == AsyncTask.Status.RUNNING) {
    		return;
    	}
    	int offset = data.size();
    	mGetItemTask = new GetItemAsyncTask(this, mUid, offset, 200, new GetItemAsyncTask.UploadEventListener() {
			
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

    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.latest_item, menu);
        return true;
    }
    
    private static class Item {
    	public String title = "";
    	public String link = "";
    	public String date = "";
    	public String site = "";
    	public String note = "";
    	public Item() {}
    	public static Item getFromLine(String line) {
    		Item item = new Item();
    		String[] column = line.split("\t");
    		item.title = Html.fromHtml(column[0]).toString();
    		item.link = column[1];
    		item.date = column[2];
    		item.site = column[3];
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
			Item item = data.get(position);
			textViewTitle.setText(item.title);
			textViewSite.setText(item.site);
			textViewDate.setText(item.date);

			return convertView;
		}
    	
    }
}
