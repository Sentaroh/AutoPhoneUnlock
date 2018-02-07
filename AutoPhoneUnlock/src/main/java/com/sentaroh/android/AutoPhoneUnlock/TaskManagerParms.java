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

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Handler;
import android.os.RemoteCallbackList;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.sentaroh.android.AutoPhoneUnlock.ISchedulerCallback;
import com.sentaroh.android.Utilities.NotifyEvent;

public class TaskManagerParms {
	public long resourceCleanupTime=0;
	public boolean schedulerEnabled=true;
	
	public Handler svcHandler=null;
	
	public LocationUtilities locationUtil=null;
	
	public Context context=null;
	public RemoteCallbackList<ISchedulerCallback> callBackList=null;
	public NotifyEvent threadReponseNotify=null;
	
	public ThreadPoolExecutor normalPriorityTaskThreadPool=null ;
	public ThreadPoolExecutor highPriorityTaskThreadPool=null ;

//	Notification
	public int msgNotificationId=1;
	public NotificationManagerCompat mainNotificationManager; 
	public NotificationCompat.Builder mainNotificationBuilder;
	public Notification mainNotification;
	public PendingIntent mainNotificationPi;
	public String main_notification_msgs_svc_active_task,
		main_notification_msgs_appname;
//	public StringBuilder mainNotificationNoOfTask=new StringBuilder();
//	public StringBuilder mainNotificationBattery=new StringBuilder();
	
//	Service messages
	public ServiceMessages svcMsgs=new ServiceMessages();
	
	public ReentrantReadWriteLock lockTaskControlRW=new ReentrantReadWriteLock();

}
