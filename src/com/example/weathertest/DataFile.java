package com.example.weathertest;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import android.content.Context;

/**
 * 文件操作类
 */
public class DataFile {

	private Context context;
	final private String FILE_NAME="weather.dat";
	final private String ALERT_FILE="weather_alert.dat";
	final private String WEATHER_STRING_FILE="weather_string.dat";
	final private String ALERT_STRING_FILE="alert_string.dat";

	public DataFile(Context context) {
		this.context = context;
	}

	//存储数据到文件
	public void saveData(String data) throws Exception{
		//context.getFilesDir();// 得到存放文件的系统目录 /data/data/<package name>/files
		//context.getCacheDir(); //缓存目录  /data/data/<package name>/cache
		System.out.println("save data:"+data);
		FileOutputStream outputStream=context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
		outputStream.write(data.getBytes());
		outputStream.close();
	}
	
	// 读取数据
	public String getData() throws Exception{
		FileInputStream inputStream=context.openFileInput(FILE_NAME);
		ByteArrayOutputStream outStream=new ByteArrayOutputStream();
		byte[] buffer=new byte[1024];
		int len=0;
		while ((len=inputStream.read(buffer))!=-1){
			outStream.write(buffer, 0, len);
		}
		outStream.close();
		byte[] data=outStream.toByteArray();
		String name=new String(data);
		return name;
	}
	
	public void saveAlert(String data) throws Exception{
		System.out.println("save alert:"+data);
		FileOutputStream outputStream=context.openFileOutput(ALERT_FILE, Context.MODE_PRIVATE|Context.MODE_APPEND);
		outputStream.write(data.getBytes());
		outputStream.close();
	}
	
	public void saveAlertString(String data) throws Exception{
		System.out.println("save alert String:"+data);
		FileOutputStream outputStream=context.openFileOutput(ALERT_STRING_FILE, Context.MODE_PRIVATE|Context.MODE_APPEND);
		outputStream.write(data.getBytes());
		outputStream.close();
	}
	
	public void saveWeatherString(String data) throws Exception{
		System.out.println("save weather string:"+data);
		FileOutputStream outputStream=context.openFileOutput(WEATHER_STRING_FILE, Context.MODE_PRIVATE|Context.MODE_APPEND);
		outputStream.write(data.getBytes());
		outputStream.close();
	}

	public String getAlertString() throws Exception{
		return getFileContent(ALERT_STRING_FILE);	
	}
	
	public String getWeatherString() throws Exception{
		return getFileContent(WEATHER_STRING_FILE);	
	}
	
	public String getAlert() throws Exception{
		return getFileContent(ALERT_FILE);		
	}
	private String getFileContent(String fileName) throws Exception{
		FileInputStream inputStream=context.openFileInput(fileName);
		ByteArrayOutputStream outStream=new ByteArrayOutputStream();
		byte[] buffer=new byte[1024*4];
		int len=0;
		while ((len=inputStream.read(buffer))!=-1){
			outStream.write(buffer, 0, len);
		}
		outStream.close();
		byte[] data1=outStream.toByteArray();
		String content=new String(data1);
		return content;	
	}

	public void cleanAlert() throws Exception{
		System.out.println("clean alert:");
		FileOutputStream outputStream=context.openFileOutput(ALERT_FILE, Context.MODE_PRIVATE);
		outputStream.write("".getBytes());
		outputStream.close();
	}
	
	public void deleteFirstAlert() throws Exception{
		String alerts=getAlert();
		int pos = alerts.indexOf(";");
		if(-1 != pos)
		{
			alerts = alerts.substring(pos+1);
		}
		FileOutputStream outputStream=context.openFileOutput(ALERT_FILE, Context.MODE_PRIVATE);
		outputStream.write(alerts.getBytes());
		outputStream.close();
	}
	
}
