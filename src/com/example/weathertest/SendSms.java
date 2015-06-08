package com.example.weathertest;

import java.util.List;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;
import android.widget.Toast;

public class SendSms {
	SmsManager smsManager = SmsManager.getDefault();
	private Context context;

	public SendSms(Context context) {
		this.context = context;
	}
	public boolean sendMessage(String phoneCode, String content) {
		final String SENT_SMS_ACTION = "SENT_SMS_ACTION";
		final String DELIVERED_SMS_ACTION = "DELIVERED_SMS_ACTION";

		// create the sentIntent parameter
		Intent sentIntent = new Intent(SENT_SMS_ACTION);
		PendingIntent sentPI = PendingIntent.getBroadcast(context, 0, sentIntent,
				0);

		// create the deilverIntent parameter
		Intent deliverIntent = new Intent(DELIVERED_SMS_ACTION);
		PendingIntent deliverPI = PendingIntent.getBroadcast(context, 0,
				deliverIntent, 0);

		// register the Broadcast Receivers
		this.context.getApplicationContext().registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context _context, Intent _intent) {
				switch (getResultCode()) {
				case Activity.RESULT_OK:
					Toast.makeText(context,"天气短信发送成功", Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
					Toast.makeText(context,"天气短信发送出现一般性错误。", Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_RADIO_OFF:
					Toast.makeText(context,"天气短信发送出错，错误原因：无线发送信号被关闭。",Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_NULL_PDU:
					Toast.makeText(context,"天气短信发送出错，错误原因：没有提供数据单元。", Toast.LENGTH_SHORT).show();
					break;
				}
				_context.unregisterReceiver(this);
			}
		}, new IntentFilter(SENT_SMS_ACTION));
		this.context.getApplicationContext().registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context _context, Intent _intent) {
				Toast.makeText(context, "天气短信已送达",Toast.LENGTH_SHORT).show();
				_context.unregisterReceiver(this);
			}
		}, new IntentFilter(DELIVERED_SMS_ACTION));

		if (content.length() > 70) {
			List<String> weatherList = smsManager.divideMessage(content);
			for (String str : weatherList) {
				smsManager.sendTextMessage(phoneCode, null, str, sentPI,deliverPI);
			}
		} else {
			smsManager.sendTextMessage(phoneCode, null, content, sentPI,deliverPI);
		}
		return false;
	}
}
