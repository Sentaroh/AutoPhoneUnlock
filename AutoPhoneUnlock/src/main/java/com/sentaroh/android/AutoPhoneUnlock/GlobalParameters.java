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
import static com.sentaroh.android.AutoPhoneUnlock.Log.LogConstants.*;

import com.sentaroh.android.Utilities.CommonGlobalParms;
import com.sentaroh.android.Utilities.ThemeColorList;

public class GlobalParameters extends CommonGlobalParms{
	
	public EnvironmentParms envParms=null;
	
	public ThemeColorList themeColorList=null;
	public int applicationTheme=R.style.Main;
	
	public String localRootDir;
	
	public GlobalParameters() {};
	
//	@Override
//	public void  onCreate() {
//		super.onCreate();
//		Log.v("GlobalParms","onCreate entered");
//	};
	
	public void clearParms() {
		envParms=null;
		localRootDir=null;
	}
	
	public void setLogParms(EnvironmentParms ep) {
		setDebugLevel(ep.settingDebugLevel);
		setLogLimitSize(2*1024*1024);
		setLogMaxFileCount(ep.settingLogMaxFileCount);
		setLogEnabled(ep.settingLogOption);
		setLogDirName(ep.settingLogMsgDir);
		setLogFileName(ep.settingLogMsgFilename);
		setApplicationTag(APPLICATION_TAG);
		setLogIntent(BROADCAST_LOG_RESET,
				BROADCAST_LOG_DELETE,
				BROADCAST_LOG_FLUSH,
				BROADCAST_LOG_ROTATE,
				BROADCAST_LOG_SEND,
				BROADCAST_LOG_CLOSE);

	}
}
