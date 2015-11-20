package com.example.weathertest;

import android.annotation.SuppressLint;

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

public class MyWeather implements Callable<String> {
	String weather=null;
	long timestamp;
	private String code="101010100";
	private int timeout=0;

	public MyWeather(String code,int timeout) {
		this.code = code;
		//this.timeout=timeout;
	}

	@SuppressLint("SimpleDateFormat") @Override
	public String call() throws Exception {
		int i=0;
		do
		{
			try {
				//这里使用的是360天气预报，比较几个之后，发现这个比较靠谱
				//url最后的101010100 是指北京，在http://cdn.weather.hao.360.cn/页面选择想要的城市，url中即可获取城市编码101110908
				//http://cdn.weather.hao.360.cn/api_weather_info.php?app=hao360&_jsonp=smartloaddata101010100&code=
				timestamp = System.currentTimeMillis();
				HttpGet httpGet = new HttpGet(
						"http://tq.360.cn/api/weatherquery/querys?app=tq360&code="+code+"&t="+timestamp+"&c="+(timestamp+Long.parseLong(code))+"&_jsonp=renderData&_="+System.currentTimeMillis());
				HttpClient httpClient = new DefaultHttpClient();
				HttpParams params=httpClient.getParams();
				HttpConnectionParams.setConnectionTimeout(params, this.timeout);
				HttpConnectionParams.setSoTimeout(params, this.timeout);
				HttpProtocolParams.setUserAgent(params,"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:30.0) Gecko/20100101 Firefox/30.0");

				System.out.println("WEATHER1 httpGet:"+httpGet.getURI());
				HttpResponse httpResponse = httpClient.execute(httpGet);
				if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					String unicodeResult = EntityUtils.toString(httpResponse.getEntity()).split("\\(")[1].split("\\)")[0];
					
					// unicode解码后获取各节点的值
					//System.out.println(unicodeResult);
					timestamp=(new JSONObject(unicodeResult).getJSONObject("realtime").getLong("dataUptime"));
					weather = getWeather(unicodeResult,timestamp);
					if(weather!=null)
					{
						System.out.println("Weather info:"+weather);
						break;
					}
				}
			} catch (Exception e) {
				// 换个接口重新获取
				e.printStackTrace();
			}
			System.out.println("Get weather info error,try again.count="+i);
			Thread.sleep(3000);
			i+=1;
		}while(i<3);


		if(weather==null)
		{
			System.out.println("Try anther API to get weather message.");
			try {
				//这里使用的是360天气预报，比较几个之后，发现这个比较靠谱
				//url最后的101010100 是指北京，在http://cdn.weather.hao.360.cn/页面选择想要的城市，url中即可获取城市编码101110908
				//http://cdn.weather.hao.360.cn/api_weather_info.php?app=hao360&_jsonp=smartloaddata101010100&code=
				HttpGet httpGet = new HttpGet(
						"http://cdn.weather.hao.360.cn/api_weather_info.php?app=hao360&_jsonp=smartloaddata101010100&code="+code);
				HttpClient httpClient = new DefaultHttpClient();
				HttpParams params=httpClient.getParams();
				HttpConnectionParams.setConnectionTimeout(params, this.timeout);
				HttpConnectionParams.setSoTimeout(params, this.timeout);

				System.out.println("WEATHER2 httpGet:"+httpGet.getURI());
				HttpResponse httpResponse = httpClient.execute(httpGet);
				if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					String unicodeResult = EntityUtils.toString(httpResponse.getEntity()).split("\\(")[1].split("\\)")[0];

					// unicode解码后获取各节点的值
					System.out.println(unescapeUnicode(unicodeResult));
					timestamp=(new JSONObject(unicodeResult).getLong("time"));
					weather = getWeather(unicodeResult,timestamp)+"."; // 加个标点区别两个API
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return weather;
	}
	
	private String getWeather(String unicodeResult,long timestamp)
	{
		StringBuffer weatherMsg = new StringBuffer();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat sdf2 = new SimpleDateFormat("d日");
		String[] weathers;
		String ret;
		try{
			weatherMsg.append(
					new JSONObject(unicodeResult).getJSONArray("area")
							.getString(2).split("\"")[1]).append(";");
			JSONArray jsonArray = new JSONObject(unicodeResult).getJSONArray("weather");
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonDay = jsonArray.getJSONObject(i);
				if(jsonDay.optJSONObject("info") == null)
				{
					continue;
				}

				// 获取日期 几月几日
				weatherMsg.append(sdf2.format(sdf.parse(jsonDay
						.getString("date"))));
				JSONObject jsonInfo = jsonDay.getJSONObject("info");
				String[] dayInfo = jsonInfo.getString("day")
						.split("\"");
				String[] nightInfo = jsonInfo.getString("night").split(
						"\"");
				if (dayInfo[3].equals(nightInfo[3])) {
					weatherMsg.append(dayInfo[3]).append("#");
				} else {
					weatherMsg.append(dayInfo[3]).append("转")
							.append(nightInfo[3]).append("#");
				}
				if (!(dayInfo[9].equals("微风"))) {
					if (dayInfo[7].equals(nightInfo[7])) {
						weatherMsg.append(dayInfo[7]);
						if (dayInfo[9].equals(nightInfo[9])) {
							weatherMsg.append(dayInfo[9]).append("#");
						} else {
							weatherMsg.append("白天").append(dayInfo[9]);
							weatherMsg.append("夜间")
									.append(nightInfo[9]).append("#");
						}
					} else {
						weatherMsg.append("白天").append(dayInfo[7])
								.append(dayInfo[9]).append("#");
						if (!(nightInfo[9].equals("微风"))) {
							weatherMsg.append("夜间")
									.append(nightInfo[7])
									.append(nightInfo[9]).append("#");
						}
					}
				} else if (!(nightInfo[9].equals("微风"))) {
					weatherMsg.append("夜间").append(nightInfo[7])
							.append(nightInfo[9]).append("#");
				}
				weatherMsg.append(nightInfo[5]).append("~")
						.append(dayInfo[5]).append("度;");
			}
			ret = weatherMsg.substring(0, weatherMsg.length() - 1);
			weathers = ret.split(";");

			ret = Arrays.asList(weathers).toString(); // 4 day
			for(int i=0;ret.length()>67;i++) // 保证在70个字内
			{
				ret = Arrays.asList(weathers).subList(0, Arrays.asList(weathers).size()-i).toString();
			}

			ret = ret.replace("[", "").replace("]", "").replace(" ", "").replace(",", ";").replace("#", ",");
			String date = new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date(timestamp * 1000));
			ret = ret + "." + date + "发布";

			return ret;
		}catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	public String unescapeUnicode(String str){
        StringBuffer sb=new StringBuffer();
        Matcher matcher = Pattern.compile("\\\\u([0-9a-fA-F]{4})").matcher(str);
        while(matcher.find()){
            matcher.appendReplacement(sb, (char)Integer.parseInt(matcher.group(1),16)+"");  
        }
        matcher.appendTail(sb);
        return sb.toString().replace("\\", "");//顺便去掉上面的转义字符"\\"
    }
}
