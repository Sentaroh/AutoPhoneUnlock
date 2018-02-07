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

public class CommonConstants {
	public static final boolean POSITIVE=true;
	public static final boolean NEGATIVE=false;
	
	public static long SERIALIZABLE_NUMBER=1L;
	
	public static final int GENERAL_FILE_BUFFER_SIZE=4096*128;
	public static final int LOG_FILE_BUFFER_SIZE=4096*64;
	
    public static final int MAX_NOTIFICATION_COUNT=20;
    
	public static final String APPLICATION_TAG="AutoPhoneUnlock";
	public static final String PACKAGE_NAME="com.sentaroh.android."+APPLICATION_TAG;
	
	public static final String LOG_FILE_NAME=APPLICATION_TAG+"_log";

	public static final String SETTING_ENABLE_SCHEDULER_KEY="key_setting_enable_scheduler";
	
	public static final int NORMAL_PRIORITY_TASK_THREAD_POOL_COUNT=5;
	public static final int HIGH_PRIORITY_TASK_THREAD_POOL_COUNT=5;
	
	public static final int MINUMUM_PASSWORD_LENGTH=4;
	
	public static final String BLUETOOTH_CONNECTED_DEVICE_LIST_KEY="bluetooth_connected_device_list";
	public static final String TRUSTED_DEVICE_TABLE_KEY="trust_list_key_v3";
	public static final String CURRENT_POWER_SOURCE_AC="AC";
	public static final String CURRENT_POWER_SOURCE_BATTERY="BATTERY";
	public static final String EXPORT_IMPORT_SETTING_NAME="*AutoPhoneUnlock Settings";
	public static final String DEFAULT_PREFS_FILENAME="default_preferences";
	public static final String ACTIVITY_TASK_DATA_FILE_NAME="ActivityHolder.dat";
	
	public static final String CB_NETWORK_CHANGED="NETWORK-CHANGED";
	public static final String CB_BTLE_SCAN_STARTED="BTLE-SCAN-STARTED";
	public static final String CB_BTLE_SCAN_ENDED="BTLE-SCAN-ENDED";
	
	public static final String UNKNOWN_LE_DEVICE_NAME="UN_KNOWN_DEVICE";
	public static final String UNKNOWN_LE_DEVICE_ADDR="UN_KNOWN_ADDR";
	
	public final static String WAKE_LOCK_OPTION_SYSTEM="0";
	public final static String WAKE_LOCK_OPTION_ALWAYS="1";
	public final static String WAKE_LOCK_OPTION_DISCREATE="2";
	
	public final static String WIFI_ON_WHEN_SCREEN_UNLOCKED_KEY="key_wifi_on_when_screen_unlock";
	public final static String WIFI_ON_WHEN_SCREEN_UNLOCKED_DISABLED="0";
	public final static String WIFI_ON_WHEN_SCREEN_UNLOCKED_ALWAYS="1";
	public final static String WIFI_ON_WHEN_SCREEN_UNLOCKED_CHARGING="2";
	public final static String WIFI_OFF_WHEN_CONNECT_TIMEOUT_KEY="key_wifi_off_when_connect_timeout";
	public final static String WIFI_OFF_WHEN_CONNECT_TIMEOUT_DISABLED="0";
	public final static String WIFI_OFF_WHEN_CONNECT_TIMEOUT_ALWAYS="1";
	public final static String WIFI_OFF_WHEN_CONNECT_TIMEOUT_BATTERY="2";
	public final static String WIFI_OFF_WHEN_CONNECT_TIMEOUT_VALUE_KEY="key_wifi_off_when_connect_timeout_value";

	public final static String WIFI_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_KEY="key_wifi_off_when_screen_locked_timeout";
	public final static String WIFI_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_DISABLED="0";
	public final static String WIFI_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_ALWAYS="1";
	public final static String WIFI_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_BATTERY="2";
	public final static String WIFI_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_VALUE_KEY="key_wifi_off_when_screen_locked_timeout_value";

	public final static String BLUETOOTH_ON_WHEN_SCREEN_UNLOCKED_KEY="key_bluetooth_on_when_screen_unlock";
	public final static String BLUETOOTH_ON_WHEN_SCREEN_UNLOCKED_DISABLED="0";
	public final static String BLUETOOTH_ON_WHEN_SCREEN_UNLOCKED_ALWAYS="1";
	public final static String BLUETOOTH_ON_WHEN_SCREEN_UNLOCKED_CHARGING="2";
	public final static String BLUETOOTH_OFF_WHEN_CONNECT_TIMEOUT_KEY="key_bluetooth_off_when_connect_timeout";
	public final static String BLUETOOTH_OFF_WHEN_CONNECT_TIMEOUT_DISABLED="0";
	public final static String BLUETOOTH_OFF_WHEN_CONNECT_TIMEOUT_ALWAYS="1";
	public final static String BLUETOOTH_OFF_WHEN_CONNECT_TIMEOUT_BATTERY="2";
	public final static String BLUETOOTH_OFF_WHEN_CONNECT_TIMEOUT_VALUE_KEY="key_bluetooth_off_when_connect_timeout_value";

	public final static String BLUETOOTH_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_KEY="key_bluetooth_off_when_screen_locked_timeout";
	public final static String BLUETOOTH_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_DISABLED="0";
	public final static String BLUETOOTH_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_ALWAYS="1";
	public final static String BLUETOOTH_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_BATTERY="2";
	public final static String BLUETOOTH_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_VALUE_KEY="key_bluetooth_off_when_screen_locked_timeout_value";
	
	public final static String PROXIMITY_DISABLED_WWHEN_MULTIPLE_EVENT_KEY="key_proximity_disabled_when_multiple_event_received";
	public final static String PROXIMITY_DISABLED_WWHEN_MULTIPLE_EVENT_DISABLED="0";
	public final static String PROXIMITY_DISABLED_WWHEN_MULTIPLE_EVENT_ENABLED="1";
	
	public final static String PROXIMITY_UNDETECTED_KEY="key_proximity_undetected";
	public final static String PROXIMITY_UNDETECTED_DISABLED="0";
	public final static String PROXIMITY_UNDETECTED_ENABLED="1";
	
	public final static String PROXIMITY_DETECTED_KEY="key_proximity_detected";
	public final static String PROXIMITY_DETECTED_DISABLED="0";
	public final static String PROXIMITY_DETECTED_ALWAYS="1";
	public final static String PROXIMITY_DETECTED_IGNORE_LANDSCAPE="2";
	
	public final static String PROXIMITY_DETECTED_TIMEOUT_VALUE_KEY="key_proximity_detect_timeout_value";
	
	public final static String NOTIFY_WHEN_SCREEN_LOCKED_VIBRATE_KEY="key_notify_when_screen_locked_vibrate";
	public final static String NOTIFY_WHEN_SCREEN_LOCKED_VIBRATE_PATTERN_KEY="key_notify_when_screen_locked_vibrate_pattern";
	public final static String NOTIFY_WHEN_SCREEN_LOCKED_VIBRATE_PATTERN_VALUE1="1";
	public final static String NOTIFY_WHEN_SCREEN_LOCKED_VIBRATE_PATTERN_VALUE2="2";
	public final static String NOTIFY_WHEN_SCREEN_LOCKED_VIBRATE_PATTERN_VALUE3="3";
	public final static String NOTIFY_WHEN_SCREEN_LOCKED_NOTIFICATION_KEY="key_notify_when_screen_locked_notification";
	public final static String NOTIFY_WHEN_SCREEN_LOCKED_NOTIFICATION_TITLE_KEY="key_notify_when_screen_locked_notification_title";
	public final static String NOTIFY_WHEN_SCREEN_LOCKED_NOTIFICATION_PATH_KEY="key_notify_when_screen_locked_notification_path";

	public final static String NOTIFY_WHEN_SCREEN_UNLOCKED_VIBRATE_KEY="key_notify_when_screen_unlocked_vibrate";
	public final static String NOTIFY_WHEN_SCREEN_UNLOCKED_VIBRATE_PATTERN_KEY="key_notify_when_screen_unlocked_vibrate_pattern";
	public final static String NOTIFY_WHEN_SCREEN_UNLOCKED_VIBRATE_PATTERN_VALUE1="1";
	public final static String NOTIFY_WHEN_SCREEN_UNLOCKED_VIBRATE_PATTERN_VALUE2="2";
	public final static String NOTIFY_WHEN_SCREEN_UNLOCKED_VIBRATE_PATTERN_VALUE3="3";
	public final static String NOTIFY_WHEN_SCREEN_UNLOCKED_NOTIFICATION_KEY="key_notify_when_screen_unlocked_notification";
	public final static String NOTIFY_WHEN_SCREEN_UNLOCKED_NOTIFICATION_TITLE_KEY="key_notify_when_screen_unlocked_notification_title";
	public final static String NOTIFY_WHEN_SCREEN_UNLOCKED_NOTIFICATION_PATH_KEY="key_notify_when_screen_unlocked_notification_path";
	
	public final static String TRUST_DEVICE_DELAY_TIME_VLAUE_KEY="key_trust_device_delay_time_value";
	public final static String TRUST_DEVICE_DELAY_TIME_NOT_USED="0";
	public final static String TRUST_DEVICE_DELAY_TIME_VALUE2="2";
	public final static String TRUST_DEVICE_DELAY_TIME_VALUE3="3";
	public final static String TRUST_DEVICE_DELAY_TIME_VALUE4="4";
	
	public final static String TRUST_DEVICE_SCREEN_LOCK_BY_PASSWORD_RESET_KEY="key_trust_device_screen_lock_by_password_reset_v2";
	
	public final static String TRUST_DEVICE_IMMEDIATE_LOCK_WHEN_TRUSTED_DEVICE_DISCONN_KEY="key_trust_device_immediate_lock_when_trusted_device_disconn";
	public final static String TRUST_DEVICE_IMMEDIATE_LOCK_WHEN_TRUSTED_DEVICE_DISCONN_NOT_USED="0";
	public final static String TRUST_DEVICE_IMMEDIATE_LOCK_WHEN_TRUSTED_DEVICE_DISCONN_USED="1";
	
	public static final String BROADCAST_DISABLE_KEYGUARD=PACKAGE_NAME+"."+"ACTION_DISABLE_KEYGUARD";
	public static final String BROADCAST_ENABLE_KEYGUARD=PACKAGE_NAME+"."+"ACTION_ENABLE_KEYGUARD";
	public static final String BROADCAST_START_SCHEDULER=PACKAGE_NAME+"."+"ACTION_START_SCHEDULER";
	public static final String BROADCAST_START_ACTIVITY_MAIN=PACKAGE_NAME+"."+"ACTION_START_ACTIVITY_MAIN";
	public static final String BROADCAST_RESTART_SCHEDULER=PACKAGE_NAME+"."+"ACTION_RESTART_SCHEDULER";
	public static final String BROADCAST_RESET_SCHEDULER=PACKAGE_NAME+"."+"ACTION_RESET_SCHEDULER";
	public static final String BROADCAST_RELOAD_TRUST_DEVICE_LIST=PACKAGE_NAME+"."+"ACTION_RELOAD_TRUST_DEVICE_LIST";
	public static final String BROADCAST_TOGGLE_SILENT=PACKAGE_NAME+"."+"ACTION_TOGGLE_SILENT";
	public static final String BROADCAST_LOCK_SCREEN=PACKAGE_NAME+"."+"ACTION_LOCK_SCREEN";
	public static final String BROADCAST_RELOAD_DEVICE_ADMIN=PACKAGE_NAME+"."+"ACTION_RELOAD_DEVICE_ADMIN";
	public static final String BROADCAST_SERVICE_HEARTBEAT=PACKAGE_NAME+"."+"ACTION_SERVICE_HEARTBEAT";
	
	public static final String BUILTIN_EVENT_BOOT_COMPLETED="*BOOT-COMPLETED";
	public static final String BUILTIN_EVENT_WIFI_ON="*WIFI-ON";
	public static final String BUILTIN_EVENT_WIFI_CONNECTED="*WIFI-CONNECTED";
	public static final String BUILTIN_EVENT_WIFI_DISCONNECTED="*WIFI-DISCONNECTED";
	public static final String BUILTIN_EVENT_WIFI_OFF="*WIFI-OFF";
	public static final String BUILTIN_EVENT_BLUETOOTH_ON="*BLUETOOTH-ON";
	public static final String BUILTIN_EVENT_BLUETOOTH_DISCONNECTED="*BLUETOOTH-DISCONNECTED";
	public static final String BUILTIN_EVENT_BLUETOOTH_CONNECTED="*BLUETOOTH-CONNECTED";
	public static final String BUILTIN_EVENT_BLUETOOTH_OFF="*BLUETOOTH-OFF";
	public static final String BUILTIN_EVENT_PROXIMITY_DETECTED="*PROXIMITY-DETECTED";
	public static final String BUILTIN_EVENT_PROXIMITY_UNDETECTED="*PROXIMITY-UNDETECTED";
	public static final String BUILTIN_EVENT_LIGHT_DETECTED="*LIGHT-DETECTED";
	public static final String BUILTIN_EVENT_LIGHT_UNDETECTED="*LIGHT-UNDETECTED";
	public static final String BUILTIN_EVENT_SCREEN_UNLOCKED="*SCREEN-UNLOCKED";
	public static final String BUILTIN_EVENT_SCREEN_LOCKED="*SCREEN-LOCKED";
	public static final String BUILTIN_EVENT_SCREEN_OFF="*SCREEN-OFF";
	public static final String BUILTIN_EVENT_SCREEN_ON="*SCREEN-ON";
	public static final String BUILTIN_EVENT_POWER_SOURCE_CHANGED_AC="*PWR-SOURCE-CHANGED-TO-AC";
	public static final String BUILTIN_EVENT_POWER_SOURCE_CHANGED_BATTERY="*PWR-SOURCE-CHANGED-TO-BATTERY";
	public static final String BUILTIN_EVENT_PHONE_CALL_STATE_IDLE="*PHONE-CALL-STATE-IDLE";
	public static final String BUILTIN_EVENT_PHONE_CALL_STATE_OFF_HOOK="*PHONE-CALL-STATE-OFF-HOOK";
	public static final String BUILTIN_EVENT_PHONE_CALL_STATE_RINGING="*PHONE-CALL-STATE-RINGING";
	public static final String BUILTIN_EVENT_BATTERY_LEVEL_CHANGED="*BATTERY-LEVEL-CHANGED";
	public static final String BUILTIN_EVENT_BATTERY_FULLY_CHARGED="*BATTERY-FULLY-CHARGED";
	public static final String BUILTIN_EVENT_BATTERY_LEVEL_LOW="*BATTERY-LEVEL-LOW";
	public static final String BUILTIN_EVENT_BATTERY_LEVEL_CRITICAL="*BATTERY-LEVEL-CRITICAL";
	public static final String BUILTIN_EVENT_BATTERY_LEVEL_HIGH="*BATTERY-LEVEL-HIGH";
	public static final String BUILTIN_EVENT_AIRPLANE_MODE_ON="*AIRPLANE-MODE-ON";
	public static final String BUILTIN_EVENT_AIRPLANE_MODE_OFF="*AIRPLANE-MODE-OFF";
//	public static final String BUILTIN_EVENT_NETWORK_CONNECTED="*NETWORK_CONNCTED";
//	public static final String BUILTIN_EVENT_NETWORK_DISCONNECTED="*NETWORK_DISCONNCTED";
	public static final String BUILTIN_EVENT_MOBILE_NETWORK_CONNECTED="*MOBILE_NETWORK_CONNCTED";
	public static final String BUILTIN_EVENT_MOBILE_NETWORK_DISCONNECTED="*MOBILE_NETWORK_DISCONNCTED";

	public static final String SETTING_PARMS_SAVE_KEY="SETTING_PARMS";
	public static final String SETTING_PARMS_SAVE_STRING="S";
	public static final String SETTING_PARMS_SAVE_BOOLEAN="B";
	public static final String SETTING_PARMS_SAVE_INT="I";
	public static final String SETTING_PARMS_SAVE_LONG="I";
}
