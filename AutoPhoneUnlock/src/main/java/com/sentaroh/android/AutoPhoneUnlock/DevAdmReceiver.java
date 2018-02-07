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

import static com.sentaroh.android.AutoPhoneUnlock.CommonConstants.*;

import com.sentaroh.android.AutoPhoneUnlock.R;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DevAdmReceiver extends DeviceAdminReceiver {
	//DeviceAdminReceiver for TaskScheduler

	private final boolean DEBUG_ENABLED=false;
    @Override
    public void onEnabled(Context c, Intent intent) {
    	//NOP
    	if (DEBUG_ENABLED) Log.v("AutoPhoneUnlock","onEnabled entered, action="+intent.getAction());
    	CommonUtilities.getPrefMgr(c).edit().putBoolean(c.getString(R.string.settings_main_device_admin),true).commit();
    	Intent in=new Intent(c,SchedulerService.class);
    	in.setAction(BROADCAST_RELOAD_DEVICE_ADMIN);
    	c.startService(in);
    }

    @Override
    public void onDisabled(Context c, Intent intent) {
    	//NOP
    	if (DEBUG_ENABLED) Log.v("AutoPhoneUnlock","onDisabled entered, action="+intent.getAction());
    	CommonUtilities.getPrefMgr(c).edit().putBoolean(c.getString(R.string.settings_main_device_admin),false).commit();
    	Intent in=new Intent(c,SchedulerService.class);
    	in.setAction(BROADCAST_RELOAD_DEVICE_ADMIN);
    	c.startService(in);
    }
    
    @Override
    public void onLockTaskModeEntering(Context c, Intent intent, String pkg) {
    	//NOP
    	if (DEBUG_ENABLED) Log.v("AutoPhoneUnlock","onLockTaskModeEntering entered, action="+intent.getAction()+", package="+pkg);
//    	CommonUtilities.getPrefMgr(c).edit().putBoolean(c.getString(R.string.settings_main_device_admin),false).commit();
//    	Intent in=new Intent(c,SchedulerService.class);
//    	in.setAction(BROADCAST_RELOAD_DEVICE_ADMIN);
//    	c.startService(in);
    }
}