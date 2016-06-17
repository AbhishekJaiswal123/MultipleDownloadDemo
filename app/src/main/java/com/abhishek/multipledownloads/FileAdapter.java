package com.abhishek.multipledownloads;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.abhishek.service.MyDownLoadService;
import com.example.multipledownloads.R;

public class FileAdapter extends ArrayAdapter<File1> {

	private Context mContext;
	private File1[] lFile;
	private LayoutInflater mInflator;
	private ViewHolder lViewHolder;
	private View mConvertView;
	private boolean mReceiversRegistered;
	
	public FileAdapter(Context context, int resource, File1[] objects) {
		super(context, resource, objects);
		
		mContext = context;
		lFile = objects;
		mInflator = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		mConvertView = convertView;
		if(mConvertView == null){
			mConvertView = mInflator.inflate(R.layout.list_row, null);
		    lViewHolder = new ViewHolder();
			lViewHolder.titleTxt = (TextView)mConvertView.findViewById(R.id.textTitle);
			lViewHolder.donutProgress = (ProgressBar)mConvertView.findViewById(R.id.donut_progress);
			mConvertView.setTag(lViewHolder);
		}else{
			lViewHolder = (ViewHolder) mConvertView.getTag();
		}
		
		   lViewHolder.titleTxt.setText(lFile[position].toString());
		   lViewHolder.donutProgress.setProgress(lFile[position].progress);
		    
		
		
		return mConvertView;
	}
	
	void updateProgressBar(final File1 pFile, View convertView) {
	
		
		  lViewHolder.donutProgress.setProgress(pFile.progress);
	}

	static class ViewHolder{
		
		 TextView titleTxt;
		ProgressBar donutProgress;
		
	}
	
	public void registerReceiver() {
		unregisterReceiver();
		IntentFilter intentToReceiveFilter = new IntentFilter();
		intentToReceiveFilter
				.addAction(MyDownLoadService.PROGRESS_UPDATE_ACTION);
		LocalBroadcastManager.getInstance(mContext).registerReceiver(
				mDownloadingProgressReceiver, intentToReceiveFilter);
		mReceiversRegistered = true;
	}

	public void unregisterReceiver() {
		if (mReceiversRegistered) {
			LocalBroadcastManager.getInstance(mContext).unregisterReceiver(
					mDownloadingProgressReceiver);
			mReceiversRegistered = false;
		}
	}
	
	protected void onProgressUpdate(int position, int progress) {
		final ListView listView = MainScreenActivity.mListView;
		int first = listView.getFirstVisiblePosition();
		int last = listView.getLastVisiblePosition();
		lFile[position].progress = progress > 100 ? 100 : progress;
      	if (position < first || position > last) {
			// just update your data set, UI will be updated automatically in
			// next
			// getView() call
		} else {
			View convertView = MainScreenActivity.mListView.getChildAt(position - first);
//			// this is the convertView that you previously returned in getView
//			// just fix it (for example:)
			updateProgressBar(lFile[position], convertView);
		}
	}
	
	
	protected void onProgressUpdateOneShot(int[] positions, int[] progresses) {
		for (int i = 0; i < positions.length; i++) {
			int position = positions[i];
			int progress = progresses[i];
			onProgressUpdate(position, progress);
		}
	}

	
	private final BroadcastReceiver mDownloadingProgressReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(
					MyDownLoadService.PROGRESS_UPDATE_ACTION)) {
				final boolean oneShot = intent
						.getBooleanExtra("oneshot", false);
				if (oneShot) {
					final int[] progresses = intent
							.getIntArrayExtra("progress");
					final int[] positions = intent.getIntArrayExtra("position");
					onProgressUpdateOneShot(positions, progresses);
				} else {
					final int progress = intent.getIntExtra("progress", -1);
					final int position = intent.getIntExtra("position", -1);
					if (position == -1) {
						return;
					}
					onProgressUpdate(position, progress);
				}
			}
		}
	};

}
