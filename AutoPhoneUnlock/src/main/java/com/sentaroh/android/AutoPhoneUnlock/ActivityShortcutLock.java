package com.sentaroh.android.AutoPhoneUnlock;

/*
The MIT License (MIT)
Copyright (c) 2011-2013 Sentaroh

Permission is hereby granted, free of charge, to any person obtaining a copy of 
this software and associated documentation files (the "Software"), to deal 
in the Software without restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to 
the following conditions:

The above copyright notice and this permission notice shall be included in all copies or 
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE 
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

*/

import static com.sentaroh.android.AutoPhoneUnlock.CommonConstants.BROADCAST_LOCK_SCREEN;

import com.sentaroh.android.AutoPhoneUnlock.R;
import com.sentaroh.android.Utilities.NotifyEvent.NotifyEventListener;
import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.Dialog.CommonDialog;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Window;

public class ActivityShortcutLock extends FragmentActivity{
	
	private Context context;

    private EnvironmentParms envParms=null;
    private CommonUtilities util=null;
    private CommonDialog commonDlg=null;

    private int restartStatus=0;
    private boolean displayDialogRequired=false;
    
	@Override  
	final protected void onSaveInstanceState(Bundle outState) {  
		super.onSaveInstanceState(outState);
		outState.putBoolean("displayDialogRequired", displayDialogRequired);
	};  
	  
	@Override  
	final protected void onRestoreInstanceState(Bundle savedState) {  
		super.onRestoreInstanceState(savedState);
		displayDialogRequired=savedState.getBoolean("displayDialogRequired", false);
		restartStatus=2;
	};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
//        setContentView(R.layout.activity_transrucent);
        
        context=this;
        envParms=new EnvironmentParms();
        envParms.loadSettingParms(context);
        GlobalParameters gp=new GlobalParameters();
        gp.setLogParms(envParms);
        util=new CommonUtilities(context, "ShortCutSleep", envParms,gp);
        commonDlg=new CommonDialog(context, getSupportFragmentManager());
		
        util.addDebugMsg(1, "I", "onCreate entered restartStaus="+restartStatus);
        // Application process is follow
    };
    
	@Override
	public void onStart() {
		super.onStart();
		util.addDebugMsg(1, "I", "onStart entered restartStaus="+restartStatus);
	};

	@Override
	public void onRestart() {
		super.onStart();
		util.addDebugMsg(1, "I", "onRestart entered restartStaus="+restartStatus);
	};

	final public void onResume() {
		super.onResume();
		util.addDebugMsg(1, "I", "onResume entered restartStaus="+restartStatus);

    	NotifyEvent ntfy=new NotifyEvent(context);
    	ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				displayDialogRequired=false;
				finish();
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {}
    	});

		if (restartStatus==0 || restartStatus==1) {
	        if (CommonUtilities.isDevicePolicyManagerActive(context)) {
	        	if (envParms.settingEnableScheduler) {
			        Intent intent_sleep = new Intent(this, SchedulerService.class);
			    	intent_sleep.setAction(BROADCAST_LOCK_SCREEN);
			    	startService(intent_sleep);
	        	} else {
		        	util.screenLockNow();
	        	}
	            finish();
	        } else {
	        	displayDialogRequired=true;
	        	showMsgDialog(ntfy);
	        }
		} else {
			if (displayDialogRequired) {
				showMsgDialog(ntfy);
			} else {
				finish();
			}
		}
		restartStatus=1;
	};

	private void showMsgDialog(NotifyEvent ntfy) {
    	commonDlg.showCommonDialog(false, "W", 
    			getString(R.string.msgs_main_shortcust_lock_name), 
    			getString(R.string.msgs_widget_battery_button_not_functional), ntfy);
	};
	
	@Override
	public void onPause() {
		super.onPause();
		util.addDebugMsg(1, "I", "onPause entered restartStaus="+restartStatus);
        // Application process is follow

		
	};

	@Override
	public void onStop() {
		super.onStop();
		util.addDebugMsg(1, "I", "onStop entered restartStaus="+restartStatus);
        // Application process is follow

		
	};

	@Override
	public void onDestroy() {
		super.onDestroy();
		util.addDebugMsg(1, "I", "onDestroy entered restartStaus="+restartStatus);
        // Application process is follow
		System.gc();
	};
	
	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
	    // Ignore orientation change to keep activity from restarting
	    super.onConfigurationChanged(newConfig);
	};
	
}
