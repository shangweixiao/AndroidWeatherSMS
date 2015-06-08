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

	public void startSchedule(Intent intent, int requestCode, int hour,int minute,int repeatCircle) {
		final long DAY_MS=(24*60*60*1000);
		long triggerAtMillis;

		Calendar calendar = Calendar.getInstance();

		calendar.set(Calendar.HOUR_OF_DAY, hour);
		calendar.set(Calendar.MINUTE, minute);
		calendar.set(Calendar.SECOND, 0);

		PendingIntent sender = PendingIntent.getBroadcast(context, requestCode,intent, PendingIntent.FLAG_UPDATE_CURRENT);

		System.out.println(context);
		System.out.println(intent);

		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		if(System.currentTimeMillis() < calendar.getTimeInMillis())
		{
			triggerAtMillis = calendar.getTimeInMillis();
		}
		else
		{
			triggerAtMillis = calendar.getTimeInMillis()+DAY_MS;
		}
		am.setRepeating(AlarmManager.RTC_WAKEUP,triggerAtMillis,AlarmManager.INTERVAL_HOUR*repeatCircle, sender);
		System.out.println("startSchedule:"+hour+":"+minute+",Action="+intent.getAction());
	}
	
	public void stopTask(Intent intent, int requestCode) {
		PendingIntent sender = PendingIntent.getBroadcast(context, requestCode,intent, PendingIntent.FLAG_UPDATE_CURRENT);

		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		am.cancel(sender);
	}
}
