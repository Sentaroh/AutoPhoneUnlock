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

import android.content.Context;

public class ServiceMessages {
    public String msgs_svc_started;
    public String msgs_svc_termination;
    public String msgs_widget_battery_status_charge_charging;
    public String msgs_widget_battery_status_charge_discharging;
    public String msgs_widget_battery_status_charge_full;
    
    public String msgs_svc_notification_info_battery_title;
    public String msgs_svc_notification_info_battery_level;
    
    public String msgs_trust_device_info_keyguard_enabled;
    public String msgs_trust_device_info_device_not_registered;
    public String msgs_trust_device_info_keyguard_disabled_after_unlock;
    public String msgs_trust_device_info_keyguard_disabled;
    
    final public void loadString(Context c) {
    	msgs_svc_started=c.getString(R.string.msgs_svc_started);
    	msgs_svc_termination=c.getString(R.string.msgs_svc_termination);
    	msgs_widget_battery_status_charge_charging=c.getString(R.string.msgs_widget_battery_status_charge_charging);
    	msgs_widget_battery_status_charge_discharging=c.getString(R.string.msgs_widget_battery_status_charge_discharging);
    	msgs_widget_battery_status_charge_full=c.getString(R.string.msgs_widget_battery_status_charge_full);
    	
    	msgs_svc_notification_info_battery_title=c.getString(R.string.msgs_svc_notification_info_battery_title);
    	msgs_svc_notification_info_battery_level=c.getString(R.string.msgs_svc_notification_info_battery_level);
    	
    	msgs_trust_device_info_device_not_registered=c.getString(R.string.msgs_trust_device_info_device_not_registered);
        msgs_trust_device_info_keyguard_enabled=c.getString(R.string.msgs_trust_device_info_keyguard_enabled);
        msgs_trust_device_info_keyguard_disabled=c.getString(R.string.msgs_trust_device_info_keyguard_disabled);
        msgs_trust_device_info_keyguard_disabled_after_unlock=c.getString(R.string.msgs_trust_device_info_keyguard_disabled_after_unlock);

    };
}
