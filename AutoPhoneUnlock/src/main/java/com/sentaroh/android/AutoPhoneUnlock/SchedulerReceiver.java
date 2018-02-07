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

//import static com.sentaroh.android.TaskAutomation.CommonConstants.*;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

public class SchedulerReceiver extends BroadcastReceiver{

//	private boolean defaultSettingEnableScheduler=false;
	private static WakeLock mWakeLock=null;

//	private SimpleDateFormat sdfDateTime = new SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.getDefault());
	@SuppressLint("Wakelock")
	@Override
	final public void onReceive(Context c, Intent arg1) {
		if (mWakeLock==null) mWakeLock=
   	    		((PowerManager)c.getSystemService(Context.POWER_SERVICE))
    			.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK    					
    				| PowerManager.ON_AFTER_RELEASE, "AutoPhoneUnlock-Receiver");
		if (!mWakeLock.isHeld()) mWakeLock.acquire(100);
//		mWakeLock.acquire(100);
		
//		initSettingParms();
		String action=arg1.getAction();
		if (action!=null) {
			Intent in = new Intent(c, SchedulerService.class);
			if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
				EnvironmentParms.putSavedBluetoothConnectedDevice(c,null);
			}
			in.setAction(action);
			c.startService(in);
		}
	};
	
//	private void initSettingParms() {
//		SharedPreferences prefs = context.getSharedPreferences(DEFAULT_PREFS_FILENAME,
//        		Context.MODE_PRIVATE|Context.MODE_MULTI_PROCESS);
//		defaultSettingEnableScheduler=prefs.getBoolean(context.getString(R.string.settings_main_enable_scheduler), true);
//	};

}
