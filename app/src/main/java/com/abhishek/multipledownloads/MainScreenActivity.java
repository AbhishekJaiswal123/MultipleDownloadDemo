package com.abhishek.multipledownloads;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import com.abhishek.service.MyDownLoadService;
import com.example.multipledownloads.R;
public class MainScreenActivity extends Activity {
	
	
	public static ListView mListView;
	private Button mNxtBtn;
	private FileAdapter mAdapter;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		long id = 0;
		File1[] files = { getFile(id++), getFile(id++), getFile(id++),
				getFile(id++), getFile(id++), getFile(id++), getFile(id++),
				getFile(id++), getFile(id++), getFile(id++), getFile(id++),
				getFile(id++) };
		mNxtBtn = (Button) findViewById(R.id.click);

		mNxtBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent lIntent = new Intent(MainScreenActivity.this,
						SecondActivity.class);
				startActivity(lIntent);

			}
		});
	    mListView = (ListView) findViewById(R.id.list);
		mAdapter = new FileAdapter(MainScreenActivity.this, R.layout.list_row, files);
		mListView.setAdapter(mAdapter);
		
		if (savedInstanceState == null) {
			Intent intent = new Intent(this, MyDownLoadService.class);
			intent.putParcelableArrayListExtra("files", new ArrayList<File1>(
					Arrays.asList(files)));
			startService(intent);
		}

		mAdapter.registerReceiver();
	}
	
	private File1 getFile(long id) {
		return new File1(id,
				"http://www.dna.caltech.edu/Papers/DNAorigami-nature.pdf");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mAdapter.unregisterReceiver();
	}

//	private void registerReceiver() {
//		unregisterReceiver();
//		IntentFilter intentToReceiveFilter = new IntentFilter();
//		intentToReceiveFilter
//				.addAction(DownloadingService.PROGRESS_UPDATE_ACTION);
//		LocalBroadcastManager.getInstance(this).registerReceiver(
//				mDownloadingProgressReceiver, intentToReceiveFilter);
//		mReceiversRegistered = true;
//	}
//
//	private void unregisterReceiver() {
//		if (mReceiversRegistered) {
//			LocalBroadcastManager.getInstance(this).unregisterReceiver(
//					mDownloadingProgressReceiver);
//			mReceiversRegistered = false;
//		}
//	}
//	
//	protected void onProgressUpdate(int position, int progress) {
//		final ListView listView = mListView;
//		int first = listView.getFirstVisiblePosition();
//		int last = listView.getLastVisiblePosition();
//		mAdapter.getItem(position).progress = progress > 100 ? 100 : progress;
//		if (position < first || position > last) {
//			// just update your data set, UI will be updated automatically in
//			// next
//			// getView() call
//		} else {
//			View convertView = mListView.getChildAt(position - first);
//			// this is the convertView that you previously returned in getView
//			// just fix it (for example:)
//			mAdapter.updateRow(mAdapter.getItem(position), convertView);
//		}
//	}
//	
//	
//	protected void onProgressUpdateOneShot(int[] positions, int[] progresses) {
//		for (int i = 0; i < positions.length; i++) {
//			int position = positions[i];
//			int progress = progresses[i];
//			onProgressUpdate(position, progress);
//		}
//	}
//
//	
//	private final BroadcastReceiver mDownloadingProgressReceiver = new BroadcastReceiver() {
//		@Override
//		public void onReceive(Context context, Intent intent) {
//			if (intent.getAction().equals(
//					DownloadingService.PROGRESS_UPDATE_ACTION)) {
//				final boolean oneShot = intent
//						.getBooleanExtra("oneshot", false);
//				if (oneShot) {
//					final int[] progresses = intent
//							.getIntArrayExtra("progress");
//					final int[] positions = intent.getIntArrayExtra("position");
//					onProgressUpdateOneShot(positions, progresses);
//				} else {
//					final int progress = intent.getIntExtra("progress", -1);
//					final int position = intent.getIntExtra("position", -1);
//					if (position == -1) {
//						return;
//					}
//					onProgressUpdate(position, progress);
//				}
//			}
//		}
//	};
}
