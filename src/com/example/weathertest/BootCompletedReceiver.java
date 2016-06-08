package com.example.weathertest;

import java.util.Calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class BootCompletedReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
		String savedData="";
		DataFile fileService;

		String action = intent.getAction();
		//如果是开机广播的话就重新设计闹铃
		if (!action.equals(Intent.ACTION_BOOT_COMPLETED)) {
			return;
		}
		
		fileService = new DataFile(context);
		Log.i("TAG","Start up...");
		try {
			savedData=fileService.getData();
			
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		System.out.println("savedData:"+savedData);
		if(savedData.length()>0) // 文件内容：手机号码,时间,地区码,短信记录,发送通知
		{
			String[] datas;
			String[] tm;
			String phone;
			int hour,minute;
			String areaCode;
			boolean saveSmsChk=false,sendNotifyChk=false,sendAlertChk=false;
			int repeatCircle=24;

			datas = savedData.split(",");
			if(!datas[0].toString().equals("1234567890"))
			{
				phone = datas[0].toString();
				
				tm=datas[1].toString().split(":");
				hour = Integer.valueOf(tm[0].toString()).intValue();
				minute = Integer.valueOf(tm[1].toString()).intValue();
				
				areaCode = datas[2].toString();
				
				if(datas.length>3)
				{
					saveSmsChk = datas[3].toString().equals("1")?true:false;
					sendNotifyChk = datas[4].toString().equals("1")?true:false;
				}
				if(datas[5].toString().equals("12"))
				{
					repeatCircle=12;
				}
				
				if(datas.length>5)
				{
					sendAlertChk = datas[6].toString().equals("1")?true:false;
				}

				//new AlertDialog.Builder(MainActivity.this).setTitle("提示").setMessage(areaCode).setPositiveButton("确定", null).show();
				ScheduleTask scheduleTask = new ScheduleTask(context);
				Intent intentSender = new Intent();
				intentSender.setClass(context, ScheduleReceiver.class);
				intentSender.setAction("com.example.weathertest.ScheduleReceiver.ACTION");
				intentSender.putExtra("phoneCode",phone);
				intentSender.putExtra("code",areaCode);
				intentSender.putExtra("saveSms",saveSmsChk?"1":"0");
				intentSender.putExtra("sendNotify",sendNotifyChk?"1":"0");
				intentSender.putExtra("hour",hour);
				intentSender.putExtra("minute",minute);
				intentSender.putExtra("repeat",repeatCircle*60); // 重复时间，单位分钟

				System.out.println(phone+hour+minute+areaCode+saveSmsChk+sendNotifyChk+repeatCircle);
				scheduleTask.startSchedule(intentSender, 12361, hour, minute,repeatCircle*60);
				
				if(sendAlertChk)
				{
					Intent alertIntent = new Intent();
					alertIntent.setClass(context, ScheduleReceiver.class);
					alertIntent.setAction("com.example.weathertest.ScheduleReceiver.ACTION_ALERT");
					alertIntent.putExtra("phoneCode",phone);
					alertIntent.putExtra("code",areaCode);
					alertIntent.putExtra("saveSms",saveSmsChk?"1":"0");
					alertIntent.putExtra("sendNotify",sendNotifyChk?"1":"0");
					Calendar c = Calendar.getInstance();
					c.add(Calendar.MINUTE,3);

					alertIntent.putExtra("hour",c.get(Calendar.HOUR_OF_DAY));
					alertIntent.putExtra("minute",c.get(Calendar.MINUTE));
					alertIntent.putExtra("repeat",60); // 重复时间，单位分钟
					scheduleTask.startSchedule(alertIntent, 12362, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE),60);
				}
			}
		}
	}
}

