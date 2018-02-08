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

import com.sentaroh.android.AutoPhoneUnlock.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

public class MakeShortcutLock extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.main);
        
		// ショートカットインテントを作成
        Intent shortcutIntent=new Intent(Intent.ACTION_VIEW);
        shortcutIntent.setClassName(this, ActivityShortcutLock.class.getName());
//        shortcutIntent.putExtra("MESSAGE","THIS IS TEST MESSAGE!!!");
        sendBroadcast(makeIntent(getString(R.string.msgs_main_shortcust_lock_name),
        		shortcutIntent));

        finish();
    }
    
    // ショートカットインテントを設定したインテントを作る
    private Intent makeIntent(String shortcutName, Intent shortcutIntent) {
    	Intent intent = new Intent();
    	
    	// ショートカットインテントを設定
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);

        // ショートカット名を設定
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutName);
        
        // アイコン設定 
        Parcelable iconResource = 
        		Intent.ShortcutIconResource.fromContext(this, R.drawable.lock_icon_128);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);

        // アクションを設定
        intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        setResult(RESULT_OK, intent);
		return intent;
    }
}
