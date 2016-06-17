package com.abhishek.multipledownloads;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.app.Activity;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.example.multipledownloads.R;

public class MainActivity extends Activity {
	public static final String ID = "id";
	private ListView mListView;
	private Button mNxtBtn;
	private ArrayAdapter<File> mAdapter;
	private boolean mReceiversRegistered;
	private static ProgressBar donutProgress;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mNxtBtn = (Button) findViewById(R.id.click);

		mNxtBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent lIntent = new Intent(MainActivity.this,
						SecondActivity.class);
				startActivity(lIntent);

			}
		});
	     mListView = (ListView) findViewById(R.id.list);
		long id = 0;
		File[] files = { getFile(id++), getFile(id++), getFile(id++),
				getFile(id++), getFile(id++), getFile(id++), getFile(id++),
				getFile(id++), getFile(id++), getFile(id++), getFile(id++),
				getFile(id++) };
		mListView.setAdapter(mAdapter = new ArrayAdapter<File>(this,
				R.layout.list_row, R.id.textTitle, files) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View v = super.getView(position, convertView, parent);
				updateRow(getItem(position), v);
				return v;
			}
		});

		if (savedInstanceState == null) {
			Intent intent = new Intent(this, DownloadingService.class);
			intent.putParcelableArrayListExtra("files", new ArrayList<File>(
					Arrays.asList(files)));
			startService(intent);
		}

		registerReceiver();
	}

	private File getFile(long id) {
		return new File(id,
				"http://www.dna.caltech.edu/Papers/DNAorigami-nature.pdf");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver();
	}

	private void registerReceiver() {
		unregisterReceiver();
		IntentFilter intentToReceiveFilter = new IntentFilter();
		intentToReceiveFilter
				.addAction(DownloadingService.PROGRESS_UPDATE_ACTION);
		LocalBroadcastManager.getInstance(this).registerReceiver(
				mDownloadingProgressReceiver, intentToReceiveFilter);
		mReceiversRegistered = true;
	}

	private void unregisterReceiver() {
		if (mReceiversRegistered) {
			LocalBroadcastManager.getInstance(this).unregisterReceiver(
					mDownloadingProgressReceiver);
			mReceiversRegistered = false;
		}
	}

	private void updateRow(final File file, View v) {

		donutProgress = (ProgressBar) v.findViewById(R.id.donut_progress);

		// ProgressBar bar = (ProgressBar) v.findViewById(R.id.progressBar);
		donutProgress.setProgress(file.progress);
		TextView tv = (TextView) v.findViewById(R.id.textTitle);
		tv.setText(file.toString());
		// v.findViewById(R.id.cancel).setOnClickListener(
		// new View.OnClickListener() {
		// @Override
		// public void onClick(View v) {
		// Intent i = new Intent();
		// i.setAction(DownloadingService.ACTION_CANCEL_DOWNLOAD);
		// i.putExtra(ID, file.getId());
		// LocalBroadcastManager.getInstance(MainActivity.this)
		// .sendBroadcast(i);
		// }
		// });
	}

	// don't call notifyDatasetChanged() too frequently, have a look at
	// following url http://stackoverflow.com/a/19090832/1112882
	protected void onProgressUpdate(int position, int progress) {
		final ListView listView = mListView;
		int first = listView.getFirstVisiblePosition();
		int last = listView.getLastVisiblePosition();
		mAdapter.getItem(position).progress = progress > 100 ? 100 : progress;
		if (position < first || position > last) {
			// just update your data set, UI will be updated automatically in
			// next
			// getView() call
		} else {
			View convertView = mListView.getChildAt(position - first);
			// this is the convertView that you previously returned in getView
			// just fix it (for example:)
			updateRow(mAdapter.getItem(position), convertView);
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
					DownloadingService.PROGRESS_UPDATE_ACTION)) {
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

	public static class DownloadingService extends IntentService {
		public static String PROGRESS_UPDATE_ACTION = DownloadingService.class
				.getName() + ".progress_update";

		private static final String ACTION_CANCEL_DOWNLOAD = DownloadingService.class
				.getName() + "action_cancel_download";

		private boolean mIsAlreadyRunning;
		private boolean mReceiversRegistered;

		private ExecutorService mExec;
		private CompletionService<NoResultType> mEcs;
		private LocalBroadcastManager mBroadcastManager;
		private List<DownloadTask> mTasks;

		private static final long INTERVAL_BROADCAST = 800;
		private long mLastUpdate = 0;

		public DownloadingService() {
			super("DownloadingService");
			mExec = Executors.newFixedThreadPool( /* only 5 at a time */4);
			mEcs = new ExecutorCompletionService<NoResultType>(mExec);
			mBroadcastManager = LocalBroadcastManager.getInstance(this);
			mTasks = new ArrayList<MainActivity.DownloadingService.DownloadTask>();
		}

		@Override
		public void onCreate() {
			super.onCreate();
			registerReceiver();
		}

		@Override
		public void onDestroy() {
			super.onDestroy();
			unregisterReceiver();
		}

		@Override
		public int onStartCommand(Intent intent, int flags, int startId) {
			if (mIsAlreadyRunning) {
				publishCurrentProgressOneShot(true);
			}
			return super.onStartCommand(intent, flags, startId);
		}

		@Override
		protected void onHandleIntent(Intent intent) {
			if (mIsAlreadyRunning) {
				return;
			}
			mIsAlreadyRunning = true;

			ArrayList<File> files = intent.getParcelableArrayListExtra("files");
			final Collection<DownloadTask> tasks = mTasks;
			int index = 0;
			for (File file : files) {
				DownloadTask yt1 = new DownloadTask(index++, file);
				tasks.add(yt1);
			}

			for (DownloadTask t : tasks) {
				mEcs.submit(t);
			}
			// wait for finish
			int n = tasks.size();
			for (int i = 0; i < n; ++i) {
				NoResultType r;
				try {
					r = mEcs.take().get();
					if (r != null) {
						// use you result here
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
			// send a last broadcast
			publishCurrentProgressOneShot(true);
			mExec.shutdown();
		}

		private void publishCurrentProgressOneShot(boolean forced) {
			if (forced
					|| System.currentTimeMillis() - mLastUpdate > INTERVAL_BROADCAST) {
				mLastUpdate = System.currentTimeMillis();
				final List<DownloadTask> tasks = mTasks;
				int[] positions = new int[tasks.size()];
				int[] progresses = new int[tasks.size()];
				for (int i = 0; i < tasks.size(); i++) {
					DownloadTask t = tasks.get(i);
					positions[i] = t.mPosition;
					progresses[i] = t.mProgress;
				}
				publishProgress(positions, progresses);
			}
		}

		private void publishCurrentProgressOneShot() {
			publishCurrentProgressOneShot(false);
		}

		private synchronized void publishProgress(int[] positions,
				int[] progresses) {
			Intent i = new Intent();
			i.setAction(PROGRESS_UPDATE_ACTION);
			i.putExtra("position", positions);
			i.putExtra("progress", progresses);
			i.putExtra("oneshot", true);
			mBroadcastManager.sendBroadcast(i);
		}

		// following methods can also be used but will cause lots of broadcasts
		private void publishCurrentProgress() {
			final Collection<DownloadTask> tasks = mTasks;
			for (DownloadTask t : tasks) {
				publishProgress(t.mPosition, t.mProgress);
			}
		}

		private synchronized void publishProgress(int position, int progress) {
			Intent i = new Intent();
			i.setAction(PROGRESS_UPDATE_ACTION);
			i.putExtra("progress", progress);
			i.putExtra("position", position);
			mBroadcastManager.sendBroadcast(i);
		}

		class DownloadTask implements Callable<NoResultType> {
			private int mPosition;
			private int mProgress = 0;
			private boolean mCancelled;
			private final File mFile;
			private Random mRand = new Random();

			public DownloadTask(int position, File file) {
				mPosition = position;
				mFile = file;
			}

			@Override
			public NoResultType call() throws Exception {
				while (mProgress < 100 && !mCancelled) {
					// mProgress += mRand.nextInt(5);
					// Thread.sleep(mRand.nextInt(500));
					String lDownloadPath = "";
					
					// donutProgress.setVisibility(View.VISIBLE);
					try {

						URL lUrl = new URL(mFile.getUrl());
						HttpURLConnection lConnection = (HttpURLConnection) lUrl
								.openConnection();
						lConnection.setRequestMethod("GET");
						lConnection.setDoOutput(true);
						lConnection.connect();

						// String lDownloadPath = Environment
						// .getExternalStorageDirectory().toString();

						java.io.File lFile = Environment
								.getExternalStorageDirectory();
						if (lFile.exists()) {
							lDownloadPath = lFile.getAbsolutePath();

							if (!lDownloadPath.endsWith("/")) {
								lDownloadPath = lDownloadPath.concat("/");

							}
							lDownloadPath = lDownloadPath
									.concat("MultipleDownload/");
							lFile = new java.io.File(lDownloadPath);
							if (!lFile.exists()) {
								lFile.mkdirs();
							}

						}
						String lFileName = mPosition
								+ "_"
								+ mFile.toString().substring(
										mFile.toString().lastIndexOf('/') + 1);
						java.io.File fileCheck = new java.io.File(lDownloadPath
								+ lFileName);
						if (!fileCheck.exists()) {

							java.io.File downloadFile = new java.io.File(
									lDownloadPath, lFileName);
							FileOutputStream fOutPutStream = new FileOutputStream(
									downloadFile);

							InputStream inputStream = new BufferedInputStream(
									lUrl.openStream(), 8192);

							int totalSize = lConnection.getContentLength();
							int downloadSize = 0;

							byte[] mBuffer = new byte[1024];
							int bufferLength = 0;

							while ((bufferLength = inputStream.read(mBuffer)) > 0) {
								fOutPutStream.write(mBuffer, 0, bufferLength);
								downloadSize += bufferLength;
								mProgress = (downloadSize * 100) / totalSize;
								publishProgress(mPosition, mProgress);
								if(mProgress == 100){
									donutProgress.setVisibility(View.GONE);
								}
							}
							fOutPutStream.close();
						
						}
					} catch (MalformedURLException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					// publish progress
					// publishCurrentProgressOneShot();

					// we can also call publishProgress(int position, int
					// progress) instead, which will work fine but avoid
					// broadcasts
					// by aggregating them

				}
				return new NoResultType();
			}

			public int getProgress() {
				return mProgress;
			}

			public int getPosition() {
				return mPosition;
			}

			public void cancel() {
				mCancelled = true;
			}
		}

		private void registerReceiver() {
			unregisterReceiver();
			IntentFilter filter = new IntentFilter();
			filter.addAction(DownloadingService.ACTION_CANCEL_DOWNLOAD);
			LocalBroadcastManager.getInstance(this).registerReceiver(
					mCommunicationReceiver, filter);
			mReceiversRegistered = true;
		}

		private void unregisterReceiver() {
			if (mReceiversRegistered) {
				LocalBroadcastManager.getInstance(this).unregisterReceiver(
						mCommunicationReceiver);
				mReceiversRegistered = false;
			}
		}

		private final BroadcastReceiver mCommunicationReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getAction().equals(
						DownloadingService.ACTION_CANCEL_DOWNLOAD)) {
					final long id = intent.getLongExtra(ID, -1);
					if (id != -1) {
						for (DownloadTask task : mTasks) {
							if (task.mFile.getId() == id) {
								task.cancel();
								break;
							}
						}
					}
				}
			}
		};

		class NoResultType {
		}
	}

	public static class File implements Parcelable {
		private final long id;
		private final String url;
		private int progress;

		public File(long id, String url) {
			this.id = id;
			this.url = url;
		}

		public long getId() {
			return id;
		}

		public String getUrl() {
			return url;
		}

		@Override
		public String toString() {
			return url;
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeLong(this.id);
			dest.writeString(this.url);
			dest.writeInt(this.progress);
		}

		private File(Parcel in) {
			this.id = in.readLong();
			this.url = in.readString();
			this.progress = in.readInt();
		}

		public static final Parcelable.Creator<File> CREATOR = new Parcelable.Creator<File>() {
			public File createFromParcel(Parcel source) {
				return new File(source);
			}

			public File[] newArray(int size) {
				return new File[size];
			}
		};
	}
}
