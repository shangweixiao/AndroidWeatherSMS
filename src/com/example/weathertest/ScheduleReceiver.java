package com.example.weathertest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

public class ScheduleReceiver extends BroadcastReceiver {

    private Context context;
    public static final int NOTIFICATION_ID = 10001;
 
	ExecutorService es = Executors.newFixedThreadPool(2);

	@Override
	public void onReceive(Context context, Intent intent) {
		String phoneCodes="1";
		String code="101010100";
	    String saveSms;
	    String sendNotify;
		boolean isAlert=false;

		String action = intent.getAction();

		System.out.println("onReceive intent action:"+action.toString());

		code=intent.getStringExtra("code");
		phoneCodes=intent.getStringExtra("phoneCode");
		saveSms = intent.getStringExtra("saveSms");
		sendNotify = intent.getStringExtra("sendNotify");
		
		//发送天气预报
		if (action.equals("com.example.weathertest.ScheduleReceiver.ACTION_ALERT")) {
			isAlert = true;
		}
		
		Intent serviceIntent = new Intent("com.example.weathertest.SENDER_SERVICE"); 
		serviceIntent.setPackage(context.getPackageName());
		serviceIntent.setClass(context, SenderService.class);
		Bundle bundle = new Bundle();  
        bundle.putString("code", code);
        bundle.putString("phoneCode", phoneCodes);
        bundle.putString("saveSms", saveSms);
        bundle.putString("sendNotify", sendNotify);
        bundle.putString("isAlert", isAlert?"true":"false");
        serviceIntent.putExtras(bundle);  
          
        context.startService(serviceIntent);
		//sendWeatherSms(context,phoneCodes,code,saveSms,sendNotify,isAlert);
	}

	public void sendWeatherSms(Context context,String phoneCodes,String code,String saveSms,String sendNotify,boolean isAlert) {
		String weather;
		SendSms sendSms = new SendSms(context);
	    String[] phoneCodeArry;
	    this.context = context;
	    String weatherMD5="";

	    ConnectionDetector cd = new ConnectionDetector(context);
	    if(!cd.isConnectingToInternet())
	    {
	    	System.out.println("Network is disconnected.");
	    	return;
	    }

		try {
			if(isAlert)
			{
				DataFile fileService = new DataFile(context);
				String alerts="";
				weather = es.submit(new MyWeatherAlert(context,code,0)).get(); 
				if (weather == null)
				{
					System.out.println("Alert message is nul.");
					return;
				}

				weatherMD5 = getMD5(weather);
				try {
					alerts=fileService.getAlert();
					if(-1 != alerts.indexOf(weatherMD5))
					{
						System.out.println("Message has been send.alerts="+alerts+"weatherMD5="+weatherMD5);
						return; // 上次发送的与本次相同，不需要发送
					}
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				// 清理记录，防止记录文件过大
				if(alerts.length()>((weatherMD5.length()+1)*128)) // 超过128条记录，删除最早的一条
				{
					fileService.deleteFirstAlert();
				}

				System.out.println("save weather MD5="+weatherMD5);
				fileService.saveAlert(weatherMD5+";");
			}
			else
			{
				weather = es.submit(new MyWeather(code,0)).get();	
			}
			//weather="1";
			if (weather != null) {
				System.out.println("send message to "+phoneCodes);
				if(-1 != phoneCodes.indexOf("#"))
				{
					phoneCodeArry = phoneCodes.split("#");
					for(int i=0;i<Arrays.asList(phoneCodeArry).size();i++)
					{
						String phoneCode = Arrays.asList(phoneCodeArry).get(i);
						sendSms.sendMessage(phoneCode,weather);
						if(saveSms.equals("1"))
						{
							saveMeassage(context,phoneCode,weather);
						}
						System.out.println("send message to "+phoneCode);
					}
				}
				else
				{
					sendSms.sendMessage(phoneCodes,weather);
					if(saveSms.equals("1"))
					{
						saveMeassage(context,phoneCodes,weather);
					}
					System.out.println("send message to "+phoneCodes.replace("#", ","));
				}

				if(sendNotify.equals("1"))
				{
					showNotification(phoneCodes);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public boolean saveMeassage(Context context,String phoneCode,String content)
	{
		boolean canWriteSms;
		String thread_id="";
		String id="";
		ContentResolver cr = context.getContentResolver();
		String[] projection = new String[] { "_id","thread_id"};//"_id", "address", "person",, "date", "type
		String where = " address = '"+phoneCode+"' ";
		Cursor cur = cr.query(Uri.parse("content://sms/sent"), projection, where, null, "date desc");
		if (null == cur)
			return true;

		if (cur.moveToFirst()) {
			thread_id = cur.getString(cur.getColumnIndex("thread_id"));//联系人姓名列表
			id = cur.getString(cur.getColumnIndex("_id"));
			System.out.println("SMS:"+thread_id+" "+id);
		}

		if(!SmsWriteOpUtil.isWriteEnabled(context.getApplicationContext())) {
		//    canWriteSms = SmsWriteOpUtil.setWriteEnabled(context.getApplicationContext(), true);
		}

		try{
		    ContentValues values = new ContentValues();
		    values.put("date_sent", System.currentTimeMillis());
		    values.put("address", phoneCode);
		    values.put("body", content);
		    values.put("protocol", 0);
		    values.put("read", 1);
		    values.put("type", 2);
		    values.put("seen", 1);
		    values.put("thread_id",Integer.parseInt(thread_id));
		    context.getContentResolver().insert(Uri.parse("content://sms/sent"),values);
		    }
		catch (Exception e) {
		    e.printStackTrace();
		    }
		return true;
	}

	private void showNotification(String phoneCode) {
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), 0);
        //Notification notification = new Notification(R.drawable.ic_launcher, "天气短信发送", System.currentTimeMillis());
        //notification.setLatestEventInfo(context, "天气短信发送通知", "天气短信已经发送给:"+phoneCode.replace("#", ","), contentIntent);
        
        Notification notification = new Notification.Builder(context)    
        .setAutoCancel(true)    
        .setContentTitle("天气短信发送通知")    
        .setContentText("天气短信已经发送给:"+phoneCode.replace("#", ","))    
        .setContentIntent(contentIntent)    
        .setSmallIcon(R.drawable.ic_launcher)    
        .setWhen(System.currentTimeMillis())    
        .build();  
        
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(
                        android.content.Context.NOTIFICATION_SERVICE);
        
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.flags |= Notification.FLAG_SHOW_LIGHTS;
        notification.flags |= Notification.FLAG_ONLY_ALERT_ONCE;
        notification.defaults |= Notification.DEFAULT_SOUND;
        notification.defaults |= Notification.DEFAULT_LIGHTS;
        notificationManager.notify(NOTIFICATION_ID, notification);
	}
	
	private String getMD5(String string) {
		try {
			// Create MD5 Hash
			MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.update(string.getBytes());
			byte messageDigestByteArray[] = messageDigest.digest();
			if (messageDigestByteArray == null || messageDigestByteArray.length == 0) {
				return "";
			}

			// Create hexadecimal String
			StringBuffer hexadecimalStringBuffer = new StringBuffer();
			int length = messageDigestByteArray.length;
			for (int i = 0; i < length; i++){
				hexadecimalStringBuffer.append(Integer.toHexString(0xFF & messageDigestByteArray[i]));
				}
			return hexadecimalStringBuffer.toString();

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return "";
	}
	
}
