package com.abhishek.service;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import com.abhishek.multipledownloads.File1;



public class MyDownLoadService extends IntentService {

	public static final String ID = "id";
	public static String PROGRESS_UPDATE_ACTION = MyDownLoadService.class
			.getName() + ".progress_update";

	private static final String ACTION_CANCEL_DOWNLOAD = MyDownLoadService.class
			.getName() + "action_cancel_download";

	private boolean mIsAlreadyRunning;
	private boolean mReceiversRegistered;

	private ExecutorService mExec;
	private CompletionService<NoResultType> mEcs;
	private LocalBroadcastManager mBroadcastManager;
	private List<DownloadTask> mTasks;

	private static final long INTERVAL_BROADCAST = 800;
	private long mLastUpdate = 0;

	public MyDownLoadService() {
		super("MyDownLoadService");
		mExec = Executors.newFixedThreadPool( /* only 5 at a time */2);
		mEcs = new ExecutorCompletionService<NoResultType>(mExec);
		mBroadcastManager = LocalBroadcastManager.getInstance(this);
		mTasks = new ArrayList<DownloadTask>();
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

		ArrayList<File1> files = intent.getParcelableArrayListExtra("files");
		final Collection<DownloadTask> tasks = mTasks;
		int index = 0;
		for (File1 file : files) {
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
		//publishCurrentProgress();
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
	private synchronized void publishCurrentProgress() {
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
		private final File1 mFile;
		private Random mRand = new Random();

		public DownloadTask(int position, File1 file) {
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
					//if (!fileCheck.exists()) {

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
							//	donutProgress.setVisibility(View.GONE);
							}
						}
						fOutPutStream.close();
					
					//}
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
		filter.addAction(MyDownLoadService.ACTION_CANCEL_DOWNLOAD);
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
					MyDownLoadService.ACTION_CANCEL_DOWNLOAD)) {
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
