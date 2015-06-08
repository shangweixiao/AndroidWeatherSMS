package com.example.weathertest;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

public class SenderService extends IntentService {

	public SenderService() {
		//必须实现父类的构造方法
		super("IntentServiceDemo");
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		System.out.println("onBind");
		return super.onBind(intent);
	}


	@Override
	public void onCreate() {
		System.out.println("onCreate");
		super.onCreate();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		System.out.println("onStart");
		super.onStart(intent, startId);
	}


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		System.out.println("onStartCommand");
		return super.onStartCommand(intent, flags, startId);
	}


	@Override
	public void setIntentRedelivery(boolean enabled) {
		super.setIntentRedelivery(enabled);
		System.out.println("setIntentRedelivery");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		//Intent是从Activity发过来的，携带识别参数，根据参数不同执行不同的任务
		String phoneCodes="1";
		String code="101010100";
	    String saveSms;
	    String sendNotify;
		boolean isAlert=false;

		Bundle bundle = new Bundle();
		bundle = intent.getExtras();
		code=bundle.getString("code");
		phoneCodes=bundle.getString("phoneCode");
		saveSms = bundle.getString("saveSms");
		sendNotify = bundle.getString("sendNotify");
		isAlert = bundle.getString("isAlert").equals("true");

		ScheduleReceiver sr = new ScheduleReceiver();
		sr.sendWeatherSms(getApplicationContext(),phoneCodes,code,saveSms,sendNotify,isAlert);		
	}

	@Override
	public void onDestroy() {
		System.out.println("onDestroy");
		super.onDestroy();
	}
}

