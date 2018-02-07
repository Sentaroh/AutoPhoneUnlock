package com.sentaroh.android.AutoPhoneUnlock;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.Dialog.CommonDialog;
import com.sentaroh.android.Utilities.NotifyEvent.NotifyEventListener;

@SuppressLint("NewApi")
public class ActivityMessageDlg extends AppCompatActivity {
	private static int restartStatus=0;

	private GlobalParameters mGp=null;

	private CommonUtilities util;
	private EnvironmentParms envParms=null;

	private CommonDialog commonDlg=null;
	
	private Context context;

	@Override  
	protected void onSaveInstanceState(Bundle outState) {  
		super.onSaveInstanceState(outState);
		util.addDebugMsg(1,"I","onSaveInstanceState entered");
	};  
	  
	@Override  
	protected void onRestoreInstanceState(Bundle savedState) {  
		super.onRestoreInstanceState(savedState);
		util.addDebugMsg(1,"I","onRestoreInstanceState entered");
		restartStatus=2;
	};

    @SuppressLint("ResourceAsColor")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
        context=this;
        
        restartStatus=0;
        envParms=new EnvironmentParms();
		envParms.loadSettingParms(context);
		envParms.loadControlOption(context);

        mGp=(GlobalParameters) this.getApplication();
        mGp.envParms=envParms;
        mGp.setLogParms(envParms);
		if (envParms.settingUseLightTheme) mGp.applicationTheme=R.style.MainLight;
		else mGp.applicationTheme=R.style.Main;
		setTheme(mGp.applicationTheme);

        util=new CommonUtilities(context.getApplicationContext(), "MessageDlg", envParms, mGp);
        
        util.addDebugMsg(1,"I","onCreate entered");
        
        commonDlg=new CommonDialog(context, getSupportFragmentManager());

        util.addDebugMsg(1,"I","onCreate entered");


    };
    
	@Override
	public void onStart() {
		super.onStart();
		util.addDebugMsg(1,"I","onStart entered");
	};

	@Override
	public void onRestart() {
		super.onStart();
		util.addDebugMsg(1,"I","onRestart entered");
	};
	
	@Override
	public void onResume() {
		super.onResume();
		
		util.addDebugMsg(1,"I","onResume entered, restartStatus="+restartStatus);

		final Intent in=getIntent();
		if ((in!=null) && 
				(in!=null && in.getStringExtra("MSG_TEXT")!=null)) {
			if (restartStatus==0) {
				showMsgTypeMessage(in.getStringExtra("MSG_TITLE"), in.getStringExtra("MSG_TEXT"));
			}
			restartStatus=1;
		} else {
			finish();
		}

	};
	
	@Override
	public void onPause() {
		super.onPause();
		util.addDebugMsg(1,"I","onPause entered");
	};

	@Override
	public void onStop() {
		super.onStop();
		util.addDebugMsg(1,"I","onStop entered");
		
	};

	@Override
	public void onDestroy() {
		super.onDestroy();
		util.addDebugMsg(1,"I","onDestroy entered");
			
		util=null;
		envParms=null;
		context=null;
	
	};
	
	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
	    // Ignore orientation change to keep activity from restarting
	    super.onConfigurationChanged(newConfig);
	    util.addDebugMsg(1,"I","onConfigurationChanged Entered");
	};
	
	final public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
//			isTerminateApplication=true;
//			finish();
			return true;
			// break;
		default:
			return super.onKeyDown(keyCode, event);
			// break;
		}
	};

	private void showMsgTypeMessage(String title, String msg) {
		NotifyEvent ntfy=new NotifyEvent(context);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				finish();
			}

			@Override
			public void negativeResponse(Context c, Object[] o) {
				// TODO Auto-generated method stub
			}
		});
		commonDlg.showCommonDialog(false, "W", title, msg, ntfy);
	};
	
}
