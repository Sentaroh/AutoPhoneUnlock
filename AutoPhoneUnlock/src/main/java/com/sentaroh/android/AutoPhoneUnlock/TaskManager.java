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

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.sentaroh.android.AutoPhoneUnlock.ISchedulerCallback;
import com.sentaroh.android.AutoPhoneUnlock.R;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Handler;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

//    @SuppressWarnings("unused") 
	final public class TaskManager {
		
    	static final public void callBackToActivity(TaskManagerParms taskMgrParms,
        		final EnvironmentParms envParms, CommonUtilities util,
    			String type, byte[] msg) {
    		if (envParms.settingDebugLevel>=2) util.addDebugMsg(2, "I", "callBackToActivity entered, type=", type);
    		synchronized(taskMgrParms.callBackList) {
    			int on = taskMgrParms.callBackList.beginBroadcast();
    			if (on!=0) {
        			ISchedulerCallback isv=null;
        			for(int i = 0; i < on; i++){
        				try {
        					isv=taskMgrParms.callBackList.getBroadcastItem(i);
        					if (isv!=null && envParms!=null) {
        						isv.notifyToClient(type);
        					}
        				} catch (RemoteException e) {
        					e.printStackTrace();
        					util.addLogMsg("E", "callBackToActivity error, num=",String.valueOf(on),
        							"\n",e.toString());
        				}
        			}
        			taskMgrParms.callBackList.finishBroadcast();
    			}
    		}
    	};
 
		final static public boolean isAcqWakeLockRequired(EnvironmentParms envParms) {
			boolean result=false;
			if (envParms.settingWakeLockOption.equals(WAKE_LOCK_OPTION_ALWAYS)) {
				result=true;
			} else if (envParms.settingWakeLockOption.equals(WAKE_LOCK_OPTION_SYSTEM)) {
		    	if (envParms.proximitySensorActive) {
		    		result=false;
				}
			} else {
			}
			return result;
		};
		
       	final static public void initNotification(TaskManagerParms taskMgrParms, EnvironmentParms envParms) {
       		String appl_ver="";
    		try {
    		    String packegeName = taskMgrParms.context.getPackageName();
    		    PackageInfo packageInfo = taskMgrParms.context.getPackageManager().getPackageInfo(packegeName, PackageManager.GET_META_DATA);
    		    appl_ver=packageInfo.versionName;
    		} catch (NameNotFoundException e) {
    		}

    		taskMgrParms.main_notification_msgs_appname=taskMgrParms.context.getString(R.string.app_name);
    		taskMgrParms.main_notification_msgs_svc_active_task=
    				taskMgrParms.main_notification_msgs_appname+" "+appl_ver+"   ";
//       				"   "+taskMgrParms.context.getString(R.string.msgs_svc_active_task);
    		taskMgrParms.mainNotificationManager =NotificationManagerCompat.from(taskMgrParms.context); 
    		taskMgrParms.mainNotificationManager.cancelAll();
       	    
    		Intent in=new Intent(taskMgrParms.context.getApplicationContext(), SchedulerService.class);
    		in.setAction(BROADCAST_START_ACTIVITY_MAIN);
    		taskMgrParms.mainNotificationPi= 
       				PendingIntent.getService(taskMgrParms.context, 0, in,PendingIntent.FLAG_UPDATE_CURRENT);

    		buildNotification(taskMgrParms, envParms);
    		
		    taskMgrParms.mainNotification=taskMgrParms.mainNotificationBuilder.build();
       	};

       	final static private void buildNotification(TaskManagerParms taskMgrParms, EnvironmentParms envParms) {
       		int icon_id=0;
       		icon_id=R.drawable.lock_icon_64;
    		taskMgrParms.mainNotificationBuilder = new NotificationCompat.Builder(taskMgrParms.context);
		   	taskMgrParms.mainNotificationBuilder.setContentIntent(taskMgrParms.mainNotificationPi)
			   	.setOngoing(true)
			   	.setAutoCancel(false)
			   	.setSmallIcon(icon_id)
			    .setWhen(System.currentTimeMillis())
			    .setOnlyAlertOnce(true)
			    ;

		   	if (envParms.settingDeviceAdmin) {
		        Intent intent_sleep = new Intent(taskMgrParms.context,SchedulerService.class);
		    	intent_sleep.setAction(BROADCAST_LOCK_SCREEN);
		    	PendingIntent pi_sleep = PendingIntent.getService(taskMgrParms.context, 0, intent_sleep, 
		    			PendingIntent.FLAG_UPDATE_CURRENT);
		    	if (Build.VERSION.SDK_INT>=21) {
		    		NotificationCompat.Action action=new NotificationCompat.Action(R.drawable.lock_icon_32, "Lock", pi_sleep);
			    	taskMgrParms.mainNotificationBuilder.addAction(action);
		    	} else {
			    	taskMgrParms.mainNotificationBuilder.addAction(R.drawable.lock_icon_32, "Lock", pi_sleep);
		    	}
		   	}

	        Intent intent_silent = new Intent(taskMgrParms.context,SchedulerService.class);
	    	intent_silent.setAction(BROADCAST_TOGGLE_SILENT);
	    	PendingIntent pi_silent = PendingIntent.getService(taskMgrParms.context, 0, intent_silent, 
	    			PendingIntent.FLAG_UPDATE_CURRENT);
	    	if (envParms.isRingerModeNormal()) taskMgrParms.mainNotificationBuilder.addAction(R.drawable.ic_32_device_silent_off, "On", pi_silent);
	    	else taskMgrParms.mainNotificationBuilder.addAction(R.drawable.ic_32_device_silent_on, "Off", pi_silent);

       	};

       	final static public void showNotification(TaskManagerParms taskMgrParms, EnvironmentParms envParms,
       			CommonUtilities util) {
       		if (!envParms.settingEnableScheduler) return ;
          	synchronized(taskMgrParms.mainNotification) {
          		buildNotification(taskMgrParms, envParms);
	       		StringBuilder title=new StringBuilder(256).append(taskMgrParms.main_notification_msgs_svc_active_task);
        		
	       		String kg_msg="";
	       		int icon_id=R.drawable.lock_icon_64;
	       		if (envParms.trustItemList.size()==0) {
	       			kg_msg=taskMgrParms.svcMsgs.msgs_trust_device_info_device_not_registered;
	       		} else {
	        		if (envParms.isKeyGuardStatusLocked()) {
	        			kg_msg=taskMgrParms.svcMsgs.msgs_trust_device_info_keyguard_enabled;
	        			icon_id=R.drawable.lock_icon_64;
	        		} else{
	        			if (envParms.isKeyGuardStatusManualUnlockRequired()) {
	        				kg_msg=taskMgrParms.svcMsgs.msgs_trust_device_info_keyguard_disabled_after_unlock;
	        				icon_id=R.drawable.unlock_required_icon_64;
	        			} else {
	        				kg_msg=taskMgrParms.svcMsgs.msgs_trust_device_info_keyguard_disabled;
	        				icon_id=R.drawable.unlock_icon_64;
	        			}
	        		}

	       		}
	       		String basic_info=taskMgrParms.svcMsgs.msgs_svc_notification_info_battery_title+" "+
        				envParms.batteryLevel+"%"+" "+envParms.batteryChargeStatusString;
	       		if (Build.VERSION.SDK_INT==24 ||
	       				(Build.VERSION.SDK_INT==17 && !envParms.settingForceUseTrustDevice)) {
				   	taskMgrParms.mainNotificationBuilder
				   	.setSmallIcon(R.drawable.main_icon_64)
		   	    	.setContentTitle(title)
		   	    	.setContentText(basic_info);
	       		} else {
				   	taskMgrParms.mainNotificationBuilder
				   	.setSmallIcon(icon_id)
		   	    	.setContentTitle(title)
		   	    	.setContentText(kg_msg)
		   	    	.setSubText(basic_info);
	       		}
//        		NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
//        		inboxStyle.setBigContentTitle(title.toString());
//        		inboxStyle.addLine(basic_info);
//
//        		inboxStyle.addLine(kg_msg);
//
//        		taskMgrParms.mainNotificationBuilder.setStyle(inboxStyle);

			    taskMgrParms.mainNotification=taskMgrParms.mainNotificationBuilder.build();
			    taskMgrParms.mainNotificationManager.notify(R.string.app_name,taskMgrParms.mainNotification);
       		}
        };

       	final static public void showErrorNotification(TaskManagerParms taskMgrParms, EnvironmentParms envParms,
       			CommonUtilities util) {
       		if (!envParms.settingEnableScheduler) return ;
       		NotificationCompat.Builder nb = new NotificationCompat.Builder(taskMgrParms.context);
    		Intent in=new Intent(taskMgrParms.context.getApplicationContext(), SchedulerService.class);
    		in.setAction(BROADCAST_RESTART_SCHEDULER);
    		PendingIntent pi= 
       				PendingIntent.getService(taskMgrParms.context, 0, in,PendingIntent.FLAG_UPDATE_CURRENT);
    		
    		if (Build.VERSION.SDK_INT>=14) {
    		   	nb.setContentIntent(pi)
			   	.setOngoing(true)
			   	.setAutoCancel(true)
			   	.setSmallIcon(R.drawable.error)
			    .setWhen(System.currentTimeMillis())
			    .setOnlyAlertOnce(true)
	   	    	.setContentTitle(taskMgrParms.main_notification_msgs_appname)
	   	    	.setContentText(taskMgrParms.context.getString(R.string.msgs_svc_notification_proxmity_count_exceed_line1))
	   	    	.setSubText(taskMgrParms.context.getString(R.string.msgs_svc_notification_proxmity_count_exceed_line2))
			    ;
    		} else {
    		   	nb.setContentIntent(pi)
			   	.setOngoing(true)
			   	.setAutoCancel(true)
			   	.setSmallIcon(R.drawable.error)
			    .setWhen(0)
			    .setOnlyAlertOnce(true)
	   	    	.setContentTitle(taskMgrParms.main_notification_msgs_appname)
	   	    	.setContentText(taskMgrParms.context.getString(R.string.msgs_svc_notification_proxmity_count_exceed_line_api10))
			    ;
    		}

		    Notification notify=nb.build();
		    taskMgrParms.mainNotificationManager.notify(R.string.app_name2,notify);
        };

       	final static public void cancelErrorNotification(TaskManagerParms taskMgrParms) {
       		taskMgrParms.mainNotificationManager.cancel(R.string.app_name2);
       	};

       	final static public void cancelNotification(TaskManagerParms taskMgrParms) {
       		taskMgrParms.mainNotificationManager.cancel(R.string.app_name);
       	};
    	
		static final public void initTaskMgrParms(EnvironmentParms envParms, 
        		TaskManagerParms taskMgrParms, Context appContext, CommonUtilities util) {
    		taskMgrParms.resourceCleanupTime=System.currentTimeMillis()+
    				envParms.settingResourceCleanupIntervalTime;
     	    taskMgrParms.context=appContext;
     	    taskMgrParms.callBackList=new RemoteCallbackList<ISchedulerCallback>();
     	    taskMgrParms.svcHandler=new Handler();
     	    
     	    buildNormalPriorityTaskThreadPool(envParms, taskMgrParms, util);
     	    buildHighPriorityTaskThreadPool(envParms, taskMgrParms, util);
     	    
     	    if (Build.VERSION.SDK_INT!=17 || envParms.settingForceUseTrustDevice) loadTrustedList(envParms, taskMgrParms, appContext, util);
     	    
     	    envParms.initKgs(taskMgrParms.context);
        };

        static final public void loadTrustedList(EnvironmentParms envParms, TaskManagerParms taskMgrParms, Context appContext, CommonUtilities util) {
        	envParms.trustItemList=CommonUtilities.loadTrustedDeviceTable(appContext, envParms);
        };
        
        static final public void executeTaskByNormalPriority(final EnvironmentParms envParms, 
    			final TaskManagerParms taskMgrParms, final CommonUtilities util, Runnable r) {
			synchronized(taskMgrParms.normalPriorityTaskThreadPool) {
	            taskMgrParms.normalPriorityTaskThreadPool.execute(r);
			}
        };
        
    	static final public void buildNormalPriorityTaskThreadPool(final EnvironmentParms envParms, 
    			final TaskManagerParms taskMgrParms, final CommonUtilities util) {
    		if (taskMgrParms.normalPriorityTaskThreadPool!=null) 
    			removeNormalPriorityTaskThreadPool(envParms,taskMgrParms,util);
    		SynchronousQueue <Runnable> slq=new SynchronousQueue <Runnable>();
    		RejectedExecutionHandler rh=new RejectedExecutionHandler() {
				@Override
				public void rejectedExecution(final Runnable r, ThreadPoolExecutor executor) {
					util.addDebugMsg(1,"W", "Normal priority task thread pool reject handler entered.");
					Thread th=new Thread() {
						@Override
						public void run() {
							r.run();
						}
					};
//					th.setPriority(THREAD_PRIORITY_TASK_CTRL_HIGH);
					th.start();
				}
    		};
     	    taskMgrParms.normalPriorityTaskThreadPool =new ThreadPoolExecutor(
     	    		NORMAL_PRIORITY_TASK_THREAD_POOL_COUNT, NORMAL_PRIORITY_TASK_THREAD_POOL_COUNT,
					10, TimeUnit.SECONDS, slq, rh);
     	    for (int i=0;i<NORMAL_PRIORITY_TASK_THREAD_POOL_COUNT;i++) {
     	    	final int num=i+1;
     	    	Runnable rt=new Runnable() {
					@Override
					public void run() {
//							Thread.currentThread().setPriority(THREAD_PRIORITY_TASK_CTRL_HIGH);
							Thread.currentThread().setName("Normal-"+num);
					}
     	    	};
     	    	taskMgrParms.normalPriorityTaskThreadPool.execute(rt);
     	    }
     	    taskMgrParms.normalPriorityTaskThreadPool.prestartAllCoreThreads();
     	    util.addDebugMsg(2,"I", "Normal priority task thread pool was created.");
    	};

    	static final public void removeNormalPriorityTaskThreadPool(EnvironmentParms envParms, 
        		TaskManagerParms taskMgrParms, CommonUtilities util) {
    		synchronized(taskMgrParms.normalPriorityTaskThreadPool) {
        		if (taskMgrParms.normalPriorityTaskThreadPool!=null) {
            		try {
            			taskMgrParms.normalPriorityTaskThreadPool.shutdown();
        				boolean rs=taskMgrParms.normalPriorityTaskThreadPool.awaitTermination(1000, TimeUnit.MILLISECONDS);
                		if (!rs) taskMgrParms.normalPriorityTaskThreadPool.shutdownNow();
        			} catch (InterruptedException e) {
        				e.printStackTrace();
        			}
            		util.addDebugMsg(2,"i","Normal priority task thread pool was removed");
        			taskMgrParms.normalPriorityTaskThreadPool=null;
        		}
    		}
    	};

        static final public void executeTaskByHighPriority(final EnvironmentParms envParms, 
    			final TaskManagerParms taskMgrParms, final CommonUtilities util, Runnable r) {
			synchronized(taskMgrParms.highPriorityTaskThreadPool) {
	            taskMgrParms.highPriorityTaskThreadPool.execute(r);
			}
        };

    	static final public void buildHighPriorityTaskThreadPool(final EnvironmentParms envParms, 
    			final TaskManagerParms taskMgrParms, final CommonUtilities util) {
    		if (taskMgrParms.highPriorityTaskThreadPool!=null) 
    			removeHighPriorityTaskThreadPool(envParms,taskMgrParms,util);
    		SynchronousQueue <Runnable> slq=new SynchronousQueue <Runnable>();
    		RejectedExecutionHandler rh=new RejectedExecutionHandler() {
				@Override
				public void rejectedExecution(final Runnable r, ThreadPoolExecutor executor) {
					util.addDebugMsg(1,"W", "High priority task thread pool reject handler entered.");
					Thread th=new Thread() {
						@Override
						public void run() {
							r.run();
						}
					};
					th.setPriority(Thread.MAX_PRIORITY);
					th.start();
				}
    		};
     	    taskMgrParms.highPriorityTaskThreadPool =new ThreadPoolExecutor(
     	    		HIGH_PRIORITY_TASK_THREAD_POOL_COUNT, HIGH_PRIORITY_TASK_THREAD_POOL_COUNT,
					10, TimeUnit.SECONDS, slq, rh);
     	    for (int i=0;i<HIGH_PRIORITY_TASK_THREAD_POOL_COUNT;i++) {
     	    	final int num=i+1;
     	    	Runnable rt=new Runnable() {
					@Override
					public void run() {
//							Thread.currentThread().setPriority(THREAD_PRIORITY_TASK_CTRL_HIGH);
							Thread.currentThread().setName("High-"+num);
					}
     	    	};
     	    	taskMgrParms.highPriorityTaskThreadPool.execute(rt);
     	    }
     	    taskMgrParms.highPriorityTaskThreadPool.prestartAllCoreThreads();
     	    util.addDebugMsg(2,"I", "High priority task thread pool was created.");
    	};

    	static final public void removeHighPriorityTaskThreadPool(EnvironmentParms envParms, 
        		TaskManagerParms taskMgrParms, CommonUtilities util) {
    		synchronized(taskMgrParms.highPriorityTaskThreadPool) {
        		if (taskMgrParms.highPriorityTaskThreadPool!=null) {
            		try {
            			taskMgrParms.highPriorityTaskThreadPool.shutdown();
        				boolean rs=taskMgrParms.highPriorityTaskThreadPool.awaitTermination(1000, TimeUnit.MILLISECONDS);
                		if (!rs) taskMgrParms.highPriorityTaskThreadPool.shutdownNow();
        			} catch (InterruptedException e) {
        				e.printStackTrace();
        			}
            		util.addDebugMsg(2,"i","High priority task thread pool was removed");
        			taskMgrParms.highPriorityTaskThreadPool=null;
        		}
    		}
    	};

    }