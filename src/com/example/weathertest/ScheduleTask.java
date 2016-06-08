package com.example.weathertest;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class ScheduleTask {
	final String tag = getClass().getSimpleName();

	private Context context;

	public ScheduleTask(Context context) {
		this.context = context;
	}

	public void startSchedule(Intent intent, int requestCode, int hour,int minute,int repeatMinute) {
		long triggerAtMillis;

		Calendar calendar = Calendar.getInstance();

		calendar.set(Calendar.HOUR_OF_DAY, hour);
		calendar.set(Calendar.MINUTE, minute);
		calendar.set(Calendar.SECOND, 0);

		PendingIntent sender = PendingIntent.getBroadcast(context, requestCode,intent, PendingIntent.FLAG_UPDATE_CURRENT);

		System.out.println("startSchedule context "+context);
		System.out.println("startSchedule intent "+intent);

		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		// 加重复周期直到到达未来的一个时间
		while(System.currentTimeMillis() >= calendar.getTimeInMillis()){
			calendar.add(Calendar.MINUTE,repeatMinute);
		}

		triggerAtMillis = calendar.getTimeInMillis();

		am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, sender);
		System.out.println("startSchedule:"+hour+":"+minute+" repeatMinute:"+repeatMinute+",Action="+intent.getAction());
		System.out.println("next start:"+calendar.get(Calendar.HOUR_OF_DAY)+":"+calendar.get(Calendar.MINUTE));
	}
	
	public void stopTask(Intent intent, int requestCode) {
		PendingIntent sender = PendingIntent.getBroadcast(context, requestCode,intent, PendingIntent.FLAG_UPDATE_CURRENT);

		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		am.cancel(sender);
	}
}
