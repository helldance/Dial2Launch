/**
 * 
 */
package com.rayy.android.dialer;

import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

/**
 * @author RAY
 * 
 */
public class AppLoader extends AsyncTaskLoader<List<ApplicationInfo>> {
	private static final String TAG = AppLoader.class.getSimpleName();

	final PackageManager mPm;
	private List<ApplicationInfo> mApps;

	public AppLoader(Context context) {
		super(context);

		mPm = context.getPackageManager();
		//loadInBackground();
	}

	@Override
	public List<ApplicationInfo> loadInBackground() {
		List<ApplicationInfo> packages = mPm.getInstalledApplications(PackageManager.GET_META_DATA);
		
		Log.i(TAG, "loading in background..");

		Iterator<ApplicationInfo> it = packages.iterator();
		// for (int i = 0; i < packages.size(); i ++){
		while (it.hasNext()) {
			ApplicationInfo appInfo = it.next();

			Intent launchIntent = mPm
					.getLaunchIntentForPackage(appInfo.packageName);

			if (launchIntent == null) {
				// toRemove.add(i);
				// Log.i("launchIntent",
				// "remove package with null launch intent " + i);
				it.remove();
				continue;
			}
		}
		
		Log.i(TAG, "loading in background..return");
		
		return packages;
	}
	
	@Override
	public void deliverResult(List<ApplicationInfo> apps) {
		Log.i(TAG, "deliver results..app " + apps.size());
		
		if (isStarted()) {
			super.deliverResult(apps);
		}
	}
	
	@Override
	protected void onStartLoading() {
		forceLoad();
	}
	
	 @Override
	 protected void onStopLoading() {
		 cancelLoad();
	 }
	
	@Override
	public void forceLoad() {
		super.forceLoad();
	}
	
	@Override
	public void onCanceled(List<ApplicationInfo> apps) {
		super.onCanceled(apps);
	}
	
	@Override
	protected void onReset() {
		onStopLoading();
		mApps = null;
	}
}
