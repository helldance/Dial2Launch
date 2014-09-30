/**
 * 
 */
package com.rayy.android.dialer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.parse.Parse;
import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseUser;
import com.parse.PushService;
import com.rayy.android.dialer.SimpleTextDialog.OnTextSetListener;

/**
 * @author RAY
 *
 */
public class PackageListing extends SherlockFragmentActivity implements ActionBar.OnNavigationListener, OnTextSetListener, 
												LoaderManager.LoaderCallbacks<List<ApplicationInfo>>{
	private static final String tag = "PackageListing";
	private static int FILTER_SYSTEM = 1, FILTER_NULL_LAUNCHER = 3, FILTER_DEFINED = 2, FILTER_NON_SYS = 0;
	//private static int TYPE_NEW = 20, TYPE_MOD = 21;
	private static int DIAG_SET = 5;
	private static Context mContext;
	private int posTag;
	private String pkgName, shortNum;
	private String[] filter;
	private SharedPreferences pref;
	private Editor editor;
	private PackageManager pm;
	//private List<ApplicationInfo> appInfoList = new ArrayList<ApplicationInfo>();
	//List<ApplicationInfo> al = new ArrayList<ApplicationInfo>();
	private List<ApplicationInfo> allApps, userApps, sysApps, defApps;
	private List<HashMap<String, Object>> appMapList, userAppsMap, sysAppsMap, defAppsMap;
	private MyItemAdapter curAppsIa, sysAppsIa, userAppsIa, defAppsIa;
	private ListView lv;
	private ProgressDialog diag;
	private int appCount = 0;
	private boolean loadFinish = false, sortSysFinish = false, sortUserFinish = false;
	private int modifyItemIndex;

	public void onCreate (Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		//this.setTheme(R.styleable.SherlockActionBar_background);
		setContentView(R.layout.gen_list);
		
		mContext = PackageListing.this;
		
		// parse integration: data, analytics and push
		Parse.initialize(this, Credential.appId, Credential.clientKey);
		ParseAnalytics.trackAppOpened(getIntent());
		ParseInstallation inst = ParseInstallation.getCurrentInstallation();
		inst.saveInBackground();
		//installation.
		Field [] fields = ParseInstallation.class.getFields();
		Method [] methods = ParseInstallation.class.getMethods();
		
		Log.i("fields", "" + fields.length + " " + methods.length);
		
		for (Field f : fields){
			Log.i(f.getName(), f.getName());
		}
		
		for (Method m : methods){
			//Log.i(m.getName(), m.invoke(inst, null));
		}
		
		Log.i("fields: ", inst.toString());
		
		// subscrible to "Notification" queue
		PushService.subscribe(mContext, "Notification", PackageListing.class);
		
		/* test code */
		ParsePush push = new ParsePush();
		push.setChannel("Notification");
		push.setMessage("The Giants just scored! It's now 2-2 against the Mets.");
		push.sendInBackground();
		
		pref = PreferenceManager.getDefaultSharedPreferences(mContext);
		editor = pref.edit();
		
		filter = getResources().getStringArray(R.array.filter);
		
		Context tempContext = getSupportActionBar().getThemedContext();
        ArrayAdapter<CharSequence> filterList = ArrayAdapter.createFromResource(tempContext, R.array.filter, R.layout.sherlock_spinner_item);
        filterList.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);

        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        getSupportActionBar().setListNavigationCallbacks(filterList, this);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
		
		lv = (ListView) findViewById(R.id.lv_pkg);
		lv.setMinimumHeight(100);
		AnimationSet set = new AnimationSet(true);
		Animation animation = new AlphaAnimation(0.0f, 1.0f);
		animation.setDuration(40);
		set.addAnimation(animation);
		animation = new TranslateAnimation(
		                Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 0.0f,
		                Animation.RELATIVE_TO_SELF, -1.0f,Animation.RELATIVE_TO_SELF, 0.0f
		            );
		animation.setDuration(60);
		set.addAnimation(animation);
		LayoutAnimationController controller = new LayoutAnimationController(set, 0.5f);
		lv.setLayoutAnimation(controller);
		
		pm = getPackageManager();
		
		getSupportLoaderManager().restartLoader(320, null, this);
		
		diag = new ProgressDialog(this);
		diag.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		diag.setMessage(getString(R.string.msg_loading));
		diag.show(); 
	}
	
	protected void saveNumber(String num) {
		SharedPreferences.Editor editor = pref.edit();
		
		for (Object str : pref.getAll().values()){
			if (str.equals(num)){
				Toast.makeText(mContext, R.string.tst_err_dup, Toast.LENGTH_SHORT).show();
				return;
			}
		}
			
		if (num.equalsIgnoreCase("")){
			if (pref.contains(pkgName)){
				editor.remove(pkgName);
			}
		}
		// check for duplicate
		/*else if (pref.getAll().entrySet().contains(num)){
			Toast.makeText(mContext, "number already assigned", Toast.LENGTH_SHORT).show();
		}*/
		
		else {
			editor.putString(pkgName, num);
			
			// add to parse analytics
			Map<String, String> dimensions = new HashMap<String, String>();
			// Define ranges to bucket data points into meaningful segments
			dimensions.put("appName", pkgName);
			// Send the dimensions to Parse along with the 'search' event
			ParseAnalytics.trackEvent("whichApp", dimensions);
		}
		
		editor.commit();
		
		Log.i(tag, "Saved " + num + " for " + pkgName);
	}
	
	public void setNumber(View v){
		switch (getSupportActionBar().getSelectedNavigationIndex()){
		case 0:
			appMapList = defAppsMap;
			break;
		case 1:
			appMapList = userAppsMap;
			break;
		case 2:
			appMapList = sysAppsMap;
			break;
		default:
			break;
		}
		
		posTag = (Integer) v.getTag();
		
		modifyItemIndex = posTag;
		
		pkgName = String.valueOf(appMapList.get(posTag).get("pkg_name"));
		shortNum = pref.getString(pkgName, "");
		
		Log.i(tag, pkgName + "-" + shortNum);
		
		//showDialog(DIAG_SET);
		FragmentManager fragMngr = getSupportFragmentManager();
		//FragmentTransaction ft = fragMngr.beginTransaction();
		DialogFragment frag = new SimpleTextDialog(shortNum);
		frag.show(fragMngr, "n_dialog");
	}
	
	@Override
	public void onTextSet(String str) {
		saveNumber(str);
		
		refreshList();
		
		//curAppsIa.getItem(modifyItemIndex);
		
		//lv.getAdapter().registerDataSetObserver(new MyDatasetObserver());
		
		//onNavigationItemSelected(getSupportActionBar().getSelectedNavigationIndex(), 1);		
	}
	
	public void updateAppList (List<ApplicationInfo> al){
		appMapList = new ArrayList<HashMap<String, Object>> ();
			
		for (ApplicationInfo ai: al){
			HashMap<String, Object> map = new HashMap<String, Object>();
			String pkg = ai.packageName;
			
			map.put("app_icon", ai.loadIcon(pm));
			map.put("app_name", ai.loadLabel(pm));
			map.put("pkg_name", pkg);
			map.put("launch_intent", pm.getLaunchIntentForPackage(pkg));
			
			if (pref.contains(pkg))
				map.put("short_cut", pref.getString(pkg, ""));
			else
				map.put("short_cut", "");
			
			//Order by name
			
			appMapList.add(map);
		}
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		if (!loadFinish)
			return false;
		
		MyItemAdapter myAdapter = null;
		
		switch (itemPosition){
		case 1: // all packages
			if (userAppsIa == null){
				//wait for sorting to finish
				waitAndCheck(sortUserFinish);
			}
				
			myAdapter = prepareAdapter(1);
			break;
		case 2: // system apps
			if (sysAppsIa == null){
				//wait for sorting to finish
				waitAndCheck(sortSysFinish);
			}
			
			myAdapter = prepareAdapter(2);
			break;
		case 0: // defined			
			myAdapter = prepareAdapter(0);
			if (myAdapter.getCount() == 0){
				Toast.makeText(mContext, R.string.tst_no_num_set, Toast.LENGTH_SHORT).show();
			}
			break;
		default:
			break;
		}

		lv.setAdapter(myAdapter);
		lv.startLayoutAnimation();
		
		return false;
	}
	
	private void refreshList (){
		switch (getSupportActionBar().getSelectedNavigationIndex()){
		case 0:
			curAppsIa = defAppsIa = prepareAdapter(0);
			break;
		case 1:
			curAppsIa = userAppsIa = prepareAdapter(1);
			break;
		case 2:
			curAppsIa = sysAppsIa = prepareAdapter(2);
			break;
		default:
			break;
		}
		
		lv.setAdapter(null);
		lv.setAdapter(curAppsIa);
	}
	
	private void waitAndCheck (boolean b){
		int c = 0;
		
		while (!b){
			if (!diag.isShowing()){
				diag.show();
			}
			
			// doing counting
			c ++;
			
			Log.i(tag, c + "");
		}
		
		if (diag.isShowing())
			diag.dismiss();
		
		return;
	}
	
	/*protected Dialog onCreateDialog(int id) {
	    Dialog diag;
	    
	    switch(id) {
	    case 5:
	        // do the work to define the pause Dialog
	    	AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
	    	LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final EditText diag_view = (EditText) inflater.inflate(R.layout.diag_set, null);
			diag_view.setText(shortNum);
			//diag_view.setPadding(10, 2, 10, 2);
			//diag_view.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
	    	builder.setView(diag_view);
	    	builder.setTitle(getString(R.string.title_set_number));
	    	builder.setPositiveButton(android.R.string.ok, new OnClickListener(){

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					saveNumber(diag_view.getText().toString());
				}
	    		
	    	});

	    	diag = builder.create();
	    	//diag.setContentView(R.layout.diag_set);
	    	//diag.setTitle(getString(R.string.title_set_number));
	    	
	        break;

	    default:
	        diag = null;
	    }
	    
	    return diag;
	}*/

	protected List<ApplicationInfo> getInstalledPackages (PackageManager pm, int filter){
    	List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
    	List<Integer> toRemove = new ArrayList<Integer>();
    	List<Integer> toFilter = new ArrayList<Integer>();
        
        // remove empty launch intent.
        /*for (int r = 0; r < packages.size(); r ++){
        	Intent launchIntent = pm.getLaunchIntentForPackage(packages.get(r).packageName);
        	
        	if (launchIntent == null){		        	
        		toFilter.add(r);
        		Log.i("launchIntent", "remove package with null launch intent " + r);
        	}
        }
        
        for (int s = 0; s < toFilter.size(); s ++){
        	int t = toFilter.get(s).intValue();
        	packages.remove(t - s);
        }*/

    	Iterator<ApplicationInfo> it = packages.iterator();
        //for (int i = 0; i < packages.size(); i ++){
    	while (it.hasNext()){
        	ApplicationInfo appInfo = it.next();
        	
        	Intent launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName);
        	
        	if (launchIntent == null){		        	
        		//toRemove.add(i);
        		//Log.i("launchIntent", "remove package with null launch intent " + i);
        		it.remove();
        		continue;
        	}
        	
        	if (filter == FILTER_NON_SYS){
        		if((appInfo.flags & ApplicationInfo.FLAG_SYSTEM)!=0){
        			//Log.i(tag, ">>>>>>system package " + appInfo.packageName);
        			//toRemove.add(i);
        			it.remove();
        		}
        	}
        	
        	else if (filter == FILTER_SYSTEM){
        		if((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0){
        			//Log.i(tag, ">>>>>>non system package " + appInfo.packageName);
        			//toRemove.add(i);
        			it.remove();
        		}
        	}
        	
        	else if (filter == FILTER_DEFINED){
        		Set<String> defined = pref.getAll().keySet();
        		
        		//Log.i(tag, "keyset size: " + defined.size());
        		
        		if (!defined.contains(appInfo.packageName)){
        			//Log.i(tag, ">>>>>not defined yet" + appInfo.packageName);
        			//toRemove.add(i);
        			it.remove();
        		} 
        	}

            //Log.i(TAG,"Installed package :"+ appInfo.packageName);
            //Log.i(TAG,"Launch Activity :"+ pm.getLaunchIntentForPackage(appInfo.packageName)); 
           
        }
        
//        Log.i(tag, "To be removed: " + toRemove.size());
//        
//        for (int j = 0; j < toRemove.size(); j ++){
//        	int k = toRemove.get(j).intValue();
//        	Log.i(tag, k + " /// ");
//        	packages.remove(k - j);
//        }
        
        //appCount = packages.size();
        
        return packages;
    }
	
	protected List<ApplicationInfo> filterInstalledPackages (List<ApplicationInfo> packages, int filter){
		List<ApplicationInfo> packages2 = new ArrayList<ApplicationInfo>(packages);
		Iterator<ApplicationInfo> it = packages2.iterator();
        //for (int i = 0; i < packages.size(); i ++){
    	while (it.hasNext()){
        	ApplicationInfo appInfo = it.next();
        	
        	if (filter == FILTER_NON_SYS){
        		if((appInfo.flags & ApplicationInfo.FLAG_SYSTEM)!=0){
        			//Log.i(tag, ">>>>>>system package " + appInfo.packageName);
        			//toRemove.add(i);
        			it.remove();
        		}
        	}
        	
        	else if (filter == FILTER_SYSTEM){
        		if((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0){
        			//Log.i(tag, ">>>>>>non system package " + appInfo.packageName);
        			//toRemove.add(i);
        			it.remove();
        		}
        	}
        	
        	else if (filter == FILTER_DEFINED){
        		Set<String> defined = pref.getAll().keySet();
        		
        		//Log.i(tag, "keyset size: " + defined.size());
        		
        		if (!defined.contains(appInfo.packageName)){
        			//Log.i(tag, ">>>>>not defined yet" + appInfo.packageName);
        			//toRemove.add(i);
        			it.remove();
        		}
        	}

            //Log.i(TAG,"Installed package :"+ appInfo.packageName);
            //Log.i(TAG,"Launch Activity :"+ pm.getLaunchIntentForPackage(appInfo.packageName)); 
           
        }
    	
    	return packages2;
	}
	
	private Handler cHandler = new Handler(){
		public void handleMessage(Message m){
			if (m.what == 201){				
				if (diag != null && diag.isShowing())
					diag.dismiss();
				
				lv.setAdapter(defAppsIa);
				
				if (defAppsIa.getCount() == 0){
					Toast.makeText(mContext, R.string.tst_no_num_set, Toast.LENGTH_SHORT).show();
				}
			}
		}
	};
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	   com.actionbarsherlock.view.MenuInflater inflater = getSupportMenuInflater();
	   inflater.inflate(R.menu.menu, (com.actionbarsherlock.view.Menu) menu);
	   return super.onCreateOptionsMenu(menu);
	}
	
	public boolean onOptionsItemSelected(MenuItem item) 
	{
	    int id = item.getItemId();

	    if (id == R.id.info) {
	    	FragmentManager fragMngr = getSupportFragmentManager();
			DialogFragment frag = new InfoDialog(getString(R.string.info_text));
			frag.show(fragMngr, "m_dialog");
	    } 
	    else if (id == R.id.backup) {
	    	backupSetting();
	    } 
	    else if (id == R.id.restore) {
	    	restoreSetting();
	    } 
	    else if (id == R.id.reset) {
	    	resetSetting();
	    }
	    else if (id == R.id.sync){
	    	syncWithCloud();
	    }
	    else if (id == R.id.account) {
	    	// start account login or registration
	    	startActivity(new Intent(mContext, Account.class));
	    }
	    else if (id == R.id.logout){
	    	ParseUser.logOut();
	    	
	    	Toast.makeText(mContext, "You have logged out", Toast.LENGTH_SHORT).show();
	    }
	    
	    return false;
	}

	private void syncWithCloud() {
		// TODO Auto-generated method stub
		ParseUser curUser = ParseUser.getCurrentUser();
		
		if (curUser != null){
			Log.i("", curUser.getUsername());
			
			Map<String, ?> settingMap = pref.getAll();
			
			for (String key: settingMap.keySet()){
				// table name
				ParseObject po = new ParseObject("AppSetting");
				
				Log.i(key, settingMap.get(key) + "");
				
				// column name
				po.add("appName", key);
				po.add("number", settingMap.get(key));
				
				try {
					//TODO handle duplicates
					po.save();
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		else {
			startActivity(new Intent(mContext, Account.class));
		}
	}

	private void resetSetting() {	
        editor.clear();        
        editor.commit();
        
        refreshList();
        
        Toast.makeText(mContext, R.string.tst_clear_all, Toast.LENGTH_SHORT).show();
	}

	@SuppressWarnings("unchecked")
	private void restoreSetting() {
		// TODO Auto-generated method stub
		ObjectInputStream ois = null;
		
		File f = new File(Environment.getExternalStorageDirectory(), "D2LSetting.xml");
		
		if (!f.exists()){
			Toast.makeText(mContext, R.string.tst_no_back_up, Toast.LENGTH_SHORT).show();
			return;
		}
	    
		try {
	        ois = new ObjectInputStream(new FileInputStream(f));

	        editor.clear();
            Map<String, ?> entries = (Map<String, ?>) ois.readObject();
            for (Entry<String, ?> entry : entries.entrySet()) {
                String v = (String) entry.getValue();
                String key = entry.getKey();
                
                Log.i(tag, "key " + key + "v " + v);
                
                editor.putString(key, v);
            }
            
            editor.commit();
            
            refreshList();
            
            Toast.makeText(mContext, R.string.tst_restore_ok, Toast.LENGTH_SHORT).show();
		}
		catch (FileNotFoundException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    } catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void backupSetting() {
		try {
			File f = new File(Environment.getExternalStorageDirectory(), "D2LSetting.xml");
		
			if (f.exists()){
				f.delete();
			}
		
			if (f.createNewFile()){
				FileOutputStream fos = new FileOutputStream(f);
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(pref.getAll());
				
				Toast.makeText(mContext, R.string.tst_backup_ok, Toast.LENGTH_SHORT).show();
			}
			else {
				Toast.makeText(mContext, R.string.tst_backup_fail, Toast.LENGTH_SHORT).show();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private MyItemAdapter prepareAdapter (int mode){
    	List<ApplicationInfo> al = null;
    	
    	switch (mode){
    	case 0:
    		al = defApps = filterInstalledPackages(allApps, FILTER_DEFINED);
    		break;
    	case 1:
    		al = userApps = filterInstalledPackages(allApps, FILTER_NON_SYS);
    		break;
    	case 2:
    		al = sysApps = filterInstalledPackages(allApps, FILTER_SYSTEM);
    		break;
    	default:
    		break;
    	}
    	
		Collections.sort(al, new ApplicationInfo.DisplayNameComparator(pm));
		
		Log.i(tag, "Apps: " + al.size());
		
		appMapList = new ArrayList<HashMap<String, Object>> ();
		
    	for (ApplicationInfo ai: al){
			HashMap<String, Object> map = new HashMap<String, Object>();
			String pkg = ai.packageName;
			
			map.put("app_icon", ai.loadIcon(pm));
			map.put("app_name", ai.loadLabel(pm));
			map.put("pkg_name", pkg);
			map.put("launch_intent", pm.getLaunchIntentForPackage(pkg));
			
			if (pref.contains(pkg))
				map.put("short_cut", pref.getString(pkg, ""));
			else
				map.put("short_cut", "");
			
			//Order by name
			
			appMapList.add(map);
		}
    	
    	switch (mode){
    	case 0:
    		defAppsMap = appMapList;
    		break;
    	case 1:
    		userAppsMap = appMapList;
    		break;
    	case 2:
    		sysAppsMap = appMapList;
    		break;
    	default:
    		break;
    	}
		
		return new MyItemAdapter(mContext, appMapList);
    }
	
	class MyItemAdapter implements ListAdapter {
		List<HashMap<String, Object>> appList;
		Context mContext;
		
		public MyItemAdapter (Context mContext, List<HashMap<String, Object>> appList){
			this.mContext = mContext;
			this.appList = appList;
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return appList.size();
		}

		@Override
		public Object getItem(int arg0) {
			// TODO Auto-generated method stub
			return appList.get(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getItemViewType(int arg0) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int pos, View arg1, ViewGroup arg2) {
			// TODO Auto-generated method stub
			/*if (arg1 != null){
				return arg1;
			}*/

			View v = View.inflate(mContext, R.layout.pkg_list_item, null);
			ImageView iv = (ImageView) v.findViewById(R.id.app_icon);
			iv.setImageDrawable((Drawable) appList.get(pos).get("app_icon"));
			TextView tv = (TextView) v.findViewById(R.id.app_name);
			tv.setText((CharSequence) appList.get(pos).get("app_name"));
			/*TextView tv2 = (TextView) v.findViewById(R.id.pkg_name);
			tv2.setText((CharSequence) appList.get(pos).get("pkg_name"));*/
			TextView tv3 = (TextView) v.findViewById(R.id.tv_num);
			tv3.setText((CharSequence) appList.get(pos).get("short_cut"));
			ImageButton btn = (ImageButton) v.findViewById(R.id.btn_set);
			btn.setTag(pos);
			
			return v;
		}

		@Override
		public int getViewTypeCount() {
			// TODO Auto-generated method stub
			return 1;
		}

		@Override
		public boolean hasStableIds() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isEmpty() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void registerDataSetObserver(DataSetObserver arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void unregisterDataSetObserver(DataSetObserver arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean areAllItemsEnabled() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isEnabled(int arg0) {
			// TODO Auto-generated method stub
			return false;
		}
	}
	
	class MyDatasetObserver extends DataSetObserver {
		public MyDatasetObserver (){
			
		}
		
		public void onChanged (){
			Log.i(tag, "observer: dataset changed.");
		}
		
		@Override
		public void onInvalidated (){
			
		}
	}
	
	private class SortingTask extends AsyncTask<Object, Object, Object>{
		private final String tag = SortingTask.class.getSimpleName();
		
		@Override
		protected Object doInBackground(Object... params) {
			Log.i(tag, "Sorting started in background..");
						
			// prepare def apps
			defAppsIa = prepareAdapter2(0);
			
			/*cHandler.obtainMessage();
			cHandler.sendEmptyMessage(201);*/
			
			// prepare sys apps			
			sysAppsIa = prepareAdapter2(2);
			sortSysFinish = true;
			
			// prepare user apps
			userAppsIa = prepareAdapter2(1);		
			sortUserFinish = true;
			
			cHandler.obtainMessage();
			cHandler.sendEmptyMessage(201);
						
			return null;
		}
		
		protected void onProgressUpdate(Integer... progress) {
			Log.i(tag, "progress.. " + progress);
	    }

	    protected void onPostExecute(Long result) {
	    	Log.i(tag, "finish.. " + result);
	    }
	    
	    private MyItemAdapter prepareAdapter2 (int mode){
	    	List<ApplicationInfo> al = null;
	    	
	    	switch (mode){
	    	case 0:
	    		al = defApps;
	    		break;
	    	case 1:
	    		al = userApps;
	    		break;
	    	case 2:
	    		al = sysApps;
	    		break;
	    	default:
	    		break;
	    	}
	    	
			Collections.sort(al, new ApplicationInfo.DisplayNameComparator(pm));
			
			Log.i(tag, "Apps: " + al.size());
			
			appMapList = new ArrayList<HashMap<String, Object>> ();
			
	    	for (ApplicationInfo ai: al){
				HashMap<String, Object> map = new HashMap<String, Object>();
				String pkg = ai.packageName;
				
				map.put("app_icon", ai.loadIcon(pm));
				map.put("app_name", ai.loadLabel(pm));
				map.put("pkg_name", pkg);
				map.put("launch_intent", pm.getLaunchIntentForPackage(pkg));
				
				if (pref.contains(pkg))
					map.put("short_cut", pref.getString(pkg, ""));
				else
					map.put("short_cut", "");
				
				//Order by name
				
				appMapList.add(map);
			}
	    	
	    	switch (mode){
	    	case 0:
	    		defAppsMap = appMapList;
	    		break;
	    	case 1:
	    		userAppsMap = appMapList;
	    		break;
	    	case 2:
	    		sysAppsMap = appMapList;
	    		break;
	    	default:
	    		break;
	    	}
			
			return new MyItemAdapter(mContext, appMapList);
	    }

	}
	
	@Override
	public Loader<List<ApplicationInfo>> onCreateLoader(int id, Bundle args) {
		return new AppLoader(mContext);
	}

	@Override
	public void onLoadFinished(Loader<List<ApplicationInfo>> loader, List<ApplicationInfo> data) {
		allApps = data;
		sysApps = filterInstalledPackages(allApps, FILTER_SYSTEM);
		userApps = filterInstalledPackages(allApps, FILTER_NON_SYS);
		defApps = filterInstalledPackages(allApps, FILTER_DEFINED);
		
		loadFinish = true;
		
		if (diag != null)
			diag.setMessage(getString(R.string.diag_sorting));
		
		new SortingTask().execute(allApps);
	}

	@Override
	public void onLoaderReset(Loader<List<ApplicationInfo>> arg0) {
		// TODO Auto-generated method stub
		lv.setAdapter(null);
	}

}
