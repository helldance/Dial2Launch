/**
 * OutgoingCallReceiver.java
 * Yang Wei
 * Jun 27, 2012
 */
package com.rayy.android.dialer;

import java.util.List;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class OutgoingCallReceiver extends BroadcastReceiver {

	private static final String OUTGOING_CALL_ACTION = "android.intent.action.NEW_OUTGOING_CALL";
	private static final String EXTRA_PHONE_NUMBER = "android.intent.extra.PHONE_NUMBER";
	private SharedPreferences pref;

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		if (intent.getAction().equals(OUTGOING_CALL_ACTION)) {
			String phoneNum = intent.getExtras().getString(EXTRA_PHONE_NUMBER);	
			
			pref = PreferenceManager.getDefaultSharedPreferences(context);
			String appToLaunch = matchApplication(phoneNum);
			
			Log.i("intercept", phoneNum + " " +  appToLaunch);	
			
			if (!appToLaunch.equalsIgnoreCase(""))
				 setResultData(null); 
			
			PackageManager pm = context.getPackageManager();
			List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
			
	        for(ApplicationInfo appInfo : packages){	  
	        	String pkgName = appInfo.packageName;
	        	
	        	//Log.i("Package", pkgName);
	        	
	        	if (pkgName.equals(appToLaunch)){
		        	Intent launchIntent = pm.getLaunchIntentForPackage(pkgName);
		        	
		        	if (launchIntent != null){		        	
		        		Log.i("launchIntent", launchIntent.toString());
		        		
		        		context.startActivity(launchIntent);
		        	}
	        		
	        		break;
	        	}
	        }

			/*if (app != null) {
				Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(app.packageName);
				context.startActivity(launchIntent);
			}*/
		}
	}

	private String matchApplication(String s) {
		Map<String, ?> numList = pref.getAll();
		
		for (String str : numList.keySet()){
			if (numList.get(str).equals(s)){
				return str;
			}
		}
		
		return "";
	}
	
	protected List<ApplicationInfo> getInstalledPackages (PackageManager pm){
		List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        
        return packages;
    }
}
