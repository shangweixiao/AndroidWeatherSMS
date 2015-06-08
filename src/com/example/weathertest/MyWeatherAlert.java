package com.example.weathertest;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class MyWeatherAlert implements Callable<String> {

	String weather=null;
	long timestamp;
	private String code="101010100";
	private DBManager dbm;
	private SQLiteDatabase db;
	private int timeout=0;

	public MyWeatherAlert(Context context,String code,int timeout) {
		String newCode = code;
		String cityName;

		dbm = new DBManager(context);
	 	dbm.openDatabase();
	 	db = dbm.getDatabase();
	 	String sql = "select name from citys where city_num='"+code+"'";
	 	Cursor cursor = db.rawQuery(sql,null);
	 	cursor.moveToFirst();
	 	cityName = cursor.getString(0);
	 	System.out.println("cityName:"+cityName);
	 	if(-1 != cityName.indexOf("."))
	 	{
	 		cityName = cityName.substring(0, cityName.indexOf("."));
	 		System.out.println("Destionation city name:"+cityName);
	 		sql = "select city_num from citys where name='"+cityName+"'";
	 		cursor = db.rawQuery(sql,null);
	 		cursor.moveToFirst();
	 		newCode = cursor.getString(0);
	 	}

	 	dbm.closeDatabase();
	 	db.close();	

		System.out.println("ALERT CODE:"+newCode);
		this.code = newCode;
		//this.timeout = timeout;
	}

	@SuppressLint("SimpleDateFormat") @Override
	public String call() throws Exception {
		int i=0;
		do{	
			try {
				//这里使用的是360天气预报，比较几个之后，发现这个比较靠谱
				//url最后的101010100 是指北京，在http://cdn.weather.hao.360.cn/页面选择想要的城市，url中即可获取城市编码101110908 101110101
				HttpGet httpGet = new HttpGet(
						"http://tq.360.cn/api/weatherquery/query?app=tq360&code="+code+"&_jsonp=renderData&_="+System.currentTimeMillis());
				HttpClient httpClient = new DefaultHttpClient();
				HttpParams params=httpClient.getParams();
				HttpConnectionParams.setConnectionTimeout(params, 3000);
				HttpConnectionParams.setSoTimeout(params, 5000);
				HttpProtocolParams.setUserAgent(params,"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:30.0) Gecko/20100101 Firefox/30.0");
		
				System.out.println("ALERT httpGet:"+httpGet.getURI());
				HttpResponse httpResponse = httpClient.execute(httpGet);
				if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					String unicodeResult = EntityUtils.toString(httpResponse.getEntity()).split("\\(")[1].split("\\)")[0];
					
					//System.out.println(unicodeResult);
					JSONArray jsonArray = new JSONObject(unicodeResult).getJSONArray("alert");
					
					if (jsonArray.length()>0) {
						weather = jsonArray.getJSONObject(jsonArray.length()-1).getString("content");
						if(weather!=null)
						{
							System.out.println("Weather alert:"+weather);
							break;
						}
					}
					else
					{
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("Get weather alert error,try again.count="+i);
			Thread.sleep(3000);
			i+=1;
		}while(i<10);

		return weather;
	}
}
