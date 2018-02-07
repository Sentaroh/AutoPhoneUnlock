package com.sentaroh.android.AutoPhoneUnlock.Log;

import static com.sentaroh.android.AutoPhoneUnlock.CommonConstants.APPLICATION_TAG;
import static com.sentaroh.android.AutoPhoneUnlock.Log.LogConstants.*;
import android.content.Context;

import com.sentaroh.android.AutoPhoneUnlock.EnvironmentParms;
import com.sentaroh.android.Utilities.CommonGlobalParms;
import com.sentaroh.android.Utilities.LogUtil.CommonLogReceiver;

public class LogReceiver extends CommonLogReceiver{
	@Override
	public void setLogParms(Context c, CommonGlobalParms gp) {
		EnvironmentParms ep=new EnvironmentParms();
		ep.loadSettingParms(c);
		
		gp.setDebugLevel(ep.settingDebugLevel);
		gp.setLogLimitSize(2*1024*1024);
		gp.setLogMaxFileCount(ep.settingLogMaxFileCount);
		gp.setLogEnabled(ep.settingLogOption);
		gp.setLogDirName(ep.settingLogMsgDir);
		gp.setLogFileName(ep.settingLogMsgFilename);
		gp.setApplicationTag(APPLICATION_TAG);
		gp.setLogIntent(BROADCAST_LOG_RESET,
				BROADCAST_LOG_DELETE,
				BROADCAST_LOG_FLUSH,
				BROADCAST_LOG_ROTATE,
				BROADCAST_LOG_SEND,
				BROADCAST_LOG_CLOSE);

	};
}
