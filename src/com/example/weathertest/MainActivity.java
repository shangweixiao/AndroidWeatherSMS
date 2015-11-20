package com.example.weathertest;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class MainActivity extends Activity {
	ExecutorService es = Executors.newFixedThreadPool(2);
	ConnectionDetector cd = new ConnectionDetector(MainActivity.this);
	ScheduleTask scheduleTask = new ScheduleTask(MainActivity.this);
	
	private Button saveData;
	private Button stopSms;
	private Button sendManual;
	private EditText phoneCode;
	private String areaCode="101010100";
	private DataFile fileService = new DataFile(MainActivity.this);
	private DBManager dbm;
	private SQLiteDatabase db;
	private Spinner spinner1 = null;
	private Spinner spinner2=null;
	private TextView textView;
	private TextView textCount;
	private TextView textAlert;
	private CheckBox sendNotify;
	private CheckBox saveSms;
	private CheckBox sendAlert;
    private boolean sendNotifyChk;
    private boolean saveSmsChk;
    private boolean sendAlertChk;
    private RadioGroup repeatGroup;
    private RadioButton hour24, hour12;
    private EditText sendTime;
    private int hour=0,minute=0;
    private TimePickerDialog tpd=null;
    private String phone;
  
    private Thread threadWeather,threadAlert;
    private static final int MSG_WEATHER = 0;
    private static final int MSG_ALERT = 1;
    private Handler mHandler = new Handler() {  
        public void handleMessage (Message msg) {//此方法在ui线程运行  
            switch(msg.what) {  
            case MSG_WEATHER:
            	String weather=(String)msg.obj;
				if (weather == null) {
					textView.setText("获取天气失败，请重新获取");
				} else {		
					textView.setText(weather);
					textCount.setText("("+weather.length() + "个字).");
				}
				if(threadWeather!=null)
				{
					threadWeather.interrupt();
					threadWeather=null;
				}
                break;
            case MSG_ALERT:
            	String msgAlert=(String)msg.obj;
				if (msgAlert == null) {
					textAlert.setText("未获取到预警信息。");
				} else {		
					textAlert.setText(msgAlert);
				}
				if(threadAlert!=null)
				{
					threadAlert.interrupt();
					threadAlert=null;				
				}
                break;  
            }  
        }  
    }; 
  
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		String savedData="";
		textView = (TextView) findViewById(R.id.textView);
		textCount = (TextView) findViewById(R.id.textCount);
		textAlert = (TextView) findViewById(R.id.textAlert);
		phoneCode = (EditText) findViewById(R.id.phoneCode);
		sendTime = (EditText) findViewById(R.id.sendTime);
		saveData = (Button)findViewById(R.id.saveData);
		stopSms = (Button)findViewById(R.id.stopSms);
		sendManual = (Button)findViewById(R.id.sendManual);
		spinner1=(Spinner)findViewById(R.id.sp1Provinces);
        spinner2=(Spinner)findViewById(R.id.sp2Citys);
		spinner1.setPrompt("省");
		spinner2.setPrompt("城市");

		repeatGroup = (RadioGroup) findViewById(R.id.repeatGroup);
		hour24 = (RadioButton) findViewById(R.id.hour24);
		hour12 = (RadioButton) findViewById(R.id.hour12);
		hour24.setChecked(true);
		repeatGroup.setOnCheckedChangeListener(radiogpchange);

		sendNotify = (CheckBox)findViewById(R.id.sendNotify);
		saveSms = (CheckBox)findViewById(R.id.saveSms);
		sendAlert = (CheckBox)findViewById(R.id.sendAlert);
		sendNotify.setOnCheckedChangeListener(chkboxListener);
		saveSms.setOnCheckedChangeListener(chkboxListener);
		sendAlert.setOnCheckedChangeListener(chkboxListener);

		saveData.setOnClickListener(mylistener);
		stopSms.setOnClickListener(mylistener);
		sendManual.setOnClickListener(mylistener);

		System.out.println("Start...");

		sendTime.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(tpd==null){  
                    tpd_init(hour,minute);  
                }  
                tpd.show();				
			}
		});

		try {
			savedData=fileService.getData();
			System.out.println(savedData);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			//e1.printStackTrace();
			new AlertDialog.Builder(MainActivity.this).setTitle("授权提示")
			.setIcon(android.R.drawable.ic_dialog_info)
			.setCancelable(false)
			.setMessage("    本应用（《天气短信》）的主要功能是定时自动向设定的手机发送最新的天气预报和天气预警信息短信。此定时发送短信行为会在后台进行，当然您可以在《天气短信》的设置界面里选择\"发送通知\"来在发送短信后给您提示信息。\n"
					+ "    因此我们需要您确认已经知晓以上事宜，并同意授权《天气短信》使用短信发送权限。如果您同意授权请点击“同意”按钮。如果您认为《天气短信》有可能会损坏到您的权益，请点击“不同意”按钮来退出《天气短信》。\n"
					+ "    短信发送所产生的费用请与您的运行商联系。\n"
					+ "    《天气短信》感谢您的使用！") 
		    .setPositiveButton("同意", new DialogInterface.OnClickListener() {
		        @Override 
		        public void onClick(DialogInterface dialog, int which) { 
		        // 点击“确认”后的操作 
		        } 
		    }).setNegativeButton("不同意", new DialogInterface.OnClickListener() { 
		 
		        @Override 
		        public void onClick(DialogInterface dialog, int which) { 
		        // 点击“返回”后的操作,这里不设置没有任何操作 
		        	finish();
		        } 
		    }).show();			
		}

		if(savedData.length()>0) // 文件内容：手机号码,时间,地区码,短信记录,发送通知
		{
			String[] datas;
			String[] tm;

			datas = savedData.split(",");
			if(datas[0].toString().equals("1234567890"))
			{
				stopSms.setEnabled(false);
			}
			else
			{
				phoneCode.setText(datas[0].toString());
				phoneCode.setSelection(datas[0].toString().length());
				
				tm=datas[1].toString().split(":");
				hour = Integer.valueOf(tm[0].toString()).intValue();
				minute = Integer.valueOf(tm[1].toString()).intValue();
				
				areaCode = datas[2].toString();
				
				if(datas.length>3)
				{
					saveSmsChk = datas[3].toString().equals("1")?true:false;
					sendNotifyChk = datas[4].toString().equals("1")?true:false;

					saveSms.setChecked(saveSmsChk);
					sendNotify.setChecked(sendNotifyChk);	
				}
				if(datas[5].toString().equals("12"))
				{
					hour12.setChecked(true);
				}
				
				if(datas.length>5)
				{
					sendAlertChk = datas[6].toString().equals("1")?true:false;
					sendAlert.setChecked(sendAlertChk);
				}
			}
		}

		sendTime.setText((hour>9?String.valueOf(hour):("0"+String.valueOf(hour)))+":"+(minute>9?String.valueOf(minute):("0"+String.valueOf(minute))));

		initSpinner1();
		//spinner1.performClick();

		textView.setText("正在更新天气信息...");
		if(cd.isConnectingToInternet())
		{
			if(threadWeather == null) {  
				threadWeather = new Thread(runnableWeather);  
				threadWeather.start();//线程启动  
            }
		}
		else
		{
			textView.setText("网络未打开，无法获取天气信息。");
		}

		textAlert.setText("正在更新预警信息...");
		if(cd.isConnectingToInternet())
		{
			if(threadAlert == null) {  
				threadAlert = new Thread(runnableAlert);  
				threadAlert.start();//线程启动  
            }
		}
		else
		{
			textAlert.setText("网络未打开，未获取到预警信息。");
		}
	}

	View.OnClickListener mylistener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			phone=phoneCode.getText().toString();
			String h=String.valueOf(hour);
			String m=String.valueOf(minute);

			switch (v.getId()) {
			case R.id.stopSms:
				try {
					new AlertDialog.Builder(MainActivity.this).setTitle("停止发送")
					.setIcon(android.R.drawable.ic_dialog_info)
					.setMessage("停止发送短信并删除已设定的内容，您确定要执行吗？") 
				    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
				        @Override 
				        public void onClick(DialogInterface dialog, int which) { 
				        // 点击“确认”后的操作 
				        	Intent intent = new Intent();
							intent.setClass(getApplicationContext(), ScheduleReceiver.class);
							intent.setAction("com.example.weathertest.ScheduleReceiver.ACTION");
							scheduleTask.stopTask(intent, 1);
							
							Intent alertIntent = new Intent();
							alertIntent.setClass(getApplicationContext(), ScheduleReceiver.class);
							alertIntent.setAction("com.example.weathertest.ScheduleReceiver.ACTION_ALERT");
							scheduleTask.stopTask(alertIntent, 2);

							try {
								fileService.saveData("1234567890"+","+0+":"+0+","+"101010100,0,0,24,0");
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							phoneCode.setText("");
							stopSms.setEnabled(false);
							sendManual.setEnabled(false);

							saveSms.setChecked(false);
							sendNotify.setChecked(false);
							sendAlert.setChecked(false);
							saveSmsChk = false;
							sendNotifyChk = false;
							sendAlertChk = false;
				        } 
				    }).setNegativeButton("取消", new DialogInterface.OnClickListener() { 
				 
				        @Override 
				        public void onClick(DialogInterface dialog, int which) { 
				        // 点击“返回”后的操作,这里不设置没有任何操作 
				        } 
				    }).show();

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case R.id.sendManual:
				while((phone.length()>0) && (phone.lastIndexOf("#")+1) == phone.length()) // 去掉最后多输入的#
				{
					phone = phone.substring(0,phone.length()-1);
				}

				while((phone.length()>0) && (phone.indexOf("#")) == 0) // 去掉前面多输入的#
				{
					phone = phone.substring(1,phone.length());
				}

				if(phone.length()>0)
				{
					if((phone.lastIndexOf("#")+1) == phone.length())
					{
						phone = phone.substring(0,phone.length()-1);
					}

					new AlertDialog.Builder(MainActivity.this).setTitle("手动发送")
					.setIcon(android.R.drawable.ic_dialog_info)
					.setMessage("您要立即发送短信吗？") 
				    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
				        @Override 
				        public void onClick(DialogInterface dialog, int which) { 
				        // 点击“确认”后的操作 
							ScheduleReceiver sr = new ScheduleReceiver();
							sr.sendWeatherSms(MainActivity.this, phone, areaCode, saveSmsChk?"1":"0", sendNotifyChk?"1":"0",false);
							if(sendAlertChk){
								sr.sendWeatherSms(MainActivity.this, phone, areaCode, saveSmsChk?"1":"0", sendNotifyChk?"1":"0",true);
							}
				        } 
				    }).setNegativeButton("返回", new DialogInterface.OnClickListener() { 
				 
				        @Override 
				        public void onClick(DialogInterface dialog, int which) { 
				        // 点击“返回”后的操作,这里不设置没有任何操作 
				        } 
				    }).show();
				}
				else
				{
					new AlertDialog.Builder(MainActivity.this).setTitle("错误").setMessage("您还没有设置电话号码！").setPositiveButton("确定", null).show();
				}
				break;
			case R.id.saveData:
				while((phone.length()>0) && (phone.lastIndexOf("#")+1) == phone.length()) // 去掉最后多输入的#
				{
					phone = phone.substring(0,phone.length()-1);
				}

				while((phone.length()>0) && (phone.indexOf("#")) == 0) // 去掉前面多输入的#
				{
					phone = phone.substring(1,phone.length());
				}

				if(phone.length()>0)
				{
					if((phone.lastIndexOf("#")+1) == phone.length())
					{
						phone = phone.substring(0,phone.length()-1);
					}

					//new AlertDialog.Builder(MainActivity.this).setTitle("提示").setMessage(areaCode).setPositiveButton("确定", null).show();
					Intent intent = new Intent();
					intent.setClass(getApplicationContext(), ScheduleReceiver.class);
					intent.setAction("com.example.weathertest.ScheduleReceiver.ACTION");
					intent.setPackage(getPackageName());
					intent.putExtra("phoneCode",phone);
					intent.putExtra("code",areaCode);
					intent.putExtra("saveSms",saveSmsChk?"1":"0");
					intent.putExtra("sendNotify",sendNotifyChk?"1":"0");
					scheduleTask.startSchedule(intent, 1, Integer.valueOf(h).intValue(), Integer.valueOf(m).intValue(),hour24.isChecked()?24:12);
					System.out.println("call startSchedule");
					
					if(sendAlertChk)
					{
						Intent alertIntent = new Intent();
						alertIntent.setClass(getApplicationContext(), ScheduleReceiver.class);
						alertIntent.setAction("com.example.weathertest.ScheduleReceiver.ACTION_ALERT");
						alertIntent.setPackage(getPackageName());
						alertIntent.putExtra("phoneCode",phone);
						alertIntent.putExtra("code",areaCode);
						alertIntent.putExtra("saveSms",saveSmsChk?"1":"0");
						alertIntent.putExtra("sendNotify",sendNotifyChk?"1":"0");
						Calendar c = Calendar.getInstance();
						
						c.add(Calendar.MINUTE,3);
						scheduleTask.startSchedule(alertIntent, 2, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE),1);
					}
					else
					{
						Intent alertIntent = new Intent();
						alertIntent.setClass(getApplicationContext(), ScheduleReceiver.class);
						alertIntent.setAction("com.example.weathertest.ScheduleReceiver.ACTION_ALERT");
						alertIntent.setPackage(getPackageName());
						scheduleTask.stopTask(alertIntent, 2);
					}

					try {
						// 文件内容：手机号码,时间,地区码,短信记录,发送通知
						fileService.saveData(phone+","+h+":"+m+","+areaCode+(saveSmsChk?",1":",0")+(sendNotifyChk?",1":",0")+(hour24.isChecked()?",24":",12")+(sendAlertChk?",1":",0"));
						new AlertDialog.Builder(MainActivity.this).setTitle("提示").setMessage("设置成功！短信将按照您设定的时间发送。").setPositiveButton("确定", null).show();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					stopSms.setEnabled(true);

					{
						textView.setText("正在更新天气信息...");
						if(cd.isConnectingToInternet())
						{
							if(threadWeather == null) {  
								threadWeather = new Thread(runnableWeather);  
								threadWeather.start();//线程启动  
				            }
						}
						else
						{
							textView.setText("网络未打开，无法获取天气信息。");
						}

						textAlert.setText("正在更新预警信息...");
						if(cd.isConnectingToInternet())
						{
							if(threadAlert == null) {  
								threadAlert = new Thread(runnableAlert);  
								threadAlert.start();//线程启动  
				            }
						}
						else
						{
							textAlert.setText("网络未打开，未获取到预警信息。");
						}
					}
				}
				else
				{
					new AlertDialog.Builder(MainActivity.this).setTitle("错误").setMessage("未设置电话号码").setPositiveButton("确定", null).show();
				}
				break;
			default:
				break;
			}	
		}
	};

    private OnCheckedChangeListener chkboxListener = new OnCheckedChangeListener()
    {
	@Override
	public void onCheckedChanged(CompoundButton buttonView,boolean isChecked)
	{
		switch(buttonView.getId())
		{
		case R.id.saveSms:
			saveSmsChk = isChecked;
			break;
		case R.id.sendNotify:
			sendNotifyChk = isChecked;
			break;
		case R.id.sendAlert:
			sendAlertChk = isChecked;
			break;
			}
		}		     
    };
  
	class TimeListener implements OnTimeChangedListener{
		
		/**
		 * view 当前选中TimePicker控件
		 *  hourOfDay 当前控件选中TimePicker 的小时
		 * minute 当前选中控件TimePicker  的分钟
		 */
		@Override
		public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
			// TODO Auto-generated method stub
			//System.out.println("h:"+ hourOfDay +" m:"+minute);
		}
		
	}
	
    public void initSpinner1(){
		dbm = new DBManager(this);
	 	dbm.openDatabase();
	 	db = dbm.getDatabase();
	 	List<MyListItem> list = new ArrayList<MyListItem>();
		
	 	try {    
	        String sql = "select * from provinces";  
	        Cursor cursor = db.rawQuery(sql,null);  
	        cursor.moveToFirst();
	        while (!cursor.isLast()){ 
		        String code=cursor.getString(cursor.getColumnIndex("_id"));
		        //System.out.println("initSpinner1"+code);
		        byte bytes[]=cursor.getBlob(1); 
		        String name=new String(bytes,"utf-8");
		        MyListItem myListItem=new MyListItem();
		        myListItem.setName(name);
		        myListItem.setPcode(code);
		        list.add(myListItem);
		        cursor.moveToNext();
	        }
	        String code=cursor.getString(cursor.getColumnIndex("_id")); 
	        byte bytes[]=cursor.getBlob(1); 
	        String name=new String(bytes,"utf-8");
	        MyListItem myListItem=new MyListItem();
	        myListItem.setName(name);
	        myListItem.setPcode(code);
	        list.add(myListItem);
	        
	    } catch (Exception e) {  
	    } 
	 	
	 	MyAdapter myAdapter = new MyAdapter(this,list);
	 	spinner1.setAdapter(myAdapter);
	 	
	 	String sql = "select province_id from citys where city_num='"+areaCode+"'";
	 	Cursor cursor = db.rawQuery(sql,null);
	 	cursor.moveToFirst();
	 	spinner1.setSelection(cursor.getInt(0), true);
		spinner1.setOnItemSelectedListener(new SpinnerOnSelectedListener1());

		dbm.closeDatabase();
	 	db.close();	
	 	
	 	String pcode = String.valueOf(cursor.getInt(0)+1);
	 	initSpinner2(pcode);
	}
    public void initSpinner2(String pcode){
		dbm = new DBManager(this);
	 	dbm.openDatabase();
	 	db = dbm.getDatabase();
	 	List<MyListItem> list = new ArrayList<MyListItem>();
        String dstName="";
        int idx=0;
        int found=0;
        {
    	 	String sql = "select name from citys where city_num='"+areaCode+"'";
    	 	Cursor cursor = db.rawQuery(sql,null);
    	 	cursor.moveToFirst();
    	 	byte bytes[]=cursor.getBlob(0);
    		try {
    			dstName = new String(bytes,"utf-8");
    		} catch (UnsupportedEncodingException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
        }

		pcode = String.valueOf(Integer.valueOf(pcode).intValue() - 1);
	 	try {
	        String sql = "select * from citys where province_id='"+pcode+"'";  
	        Cursor cursor = db.rawQuery(sql,null);  
	        cursor.moveToFirst();
	        while (!cursor.isLast()){ 
		        String code=cursor.getString(cursor.getColumnIndex("_id")); 
		        byte bytes[]=cursor.getBlob(2); 
		        String name=new String(bytes,"utf-8");
		        String city_num = cursor.getString(cursor.getColumnIndex("city_num"));
		        if(0==found)
		        {
		        	if(!dstName.equals(name))
		        	{
		        		idx += 1;
		        	}
		        	else
		        	{
		        		found=1;
		        	}
		        }
		        MyListItem myListItem=new MyListItem();
		        myListItem.setName(name);
		        myListItem.setPcode(code);
		        myListItem.setCityNum(city_num);
		        list.add(myListItem);
		        cursor.moveToNext();
	        }
	        String code=cursor.getString(cursor.getColumnIndex("_id")); 
	        byte bytes[]=cursor.getBlob(2); 
	        String name=new String(bytes,"utf-8");
	        String city_num = cursor.getString(cursor.getColumnIndex("city_num"));
	        
	        MyListItem myListItem=new MyListItem();
	        myListItem.setName(name);
	        myListItem.setPcode(code);
	        myListItem.setCityNum(city_num);
	        list.add(myListItem);
	        
	    } catch (Exception e) {  
	    } 
	 	
	 	MyAdapter myAdapter = new MyAdapter(this,list);
	 	spinner2.setAdapter(myAdapter);

	 	spinner2.setSelection(idx, true);
		spinner2.setOnItemSelectedListener(new SpinnerOnSelectedListener2());
		
	 	dbm.closeDatabase();
	 	db.close();	
	}
    
	class SpinnerOnSelectedListener1 implements OnItemSelectedListener{
		
		public void onItemSelected(AdapterView<?> adapterView, View view, int position,
				long id) {
			String pcode =((MyListItem) adapterView.getItemAtPosition(position)).getPcode();
			
			initSpinner2(pcode);
		}

		public void onNothingSelected(AdapterView<?> adapterView) {
			// TODO Auto-generated method stub
		}		
	}
	class SpinnerOnSelectedListener2 implements OnItemSelectedListener{
		
		public void onItemSelected(AdapterView<?> adapterView, View view, int position,
				long id) {
			String city_num =((MyListItem) adapterView.getItemAtPosition(position)).getCityNum();
			areaCode = city_num;
		}

		public void onNothingSelected(AdapterView<?> adapterView) {
			// TODO Auto-generated method stub
		}		
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
        	String versionName = getVersion(MainActivity.this);
        	String info="天气短信\n版本："+versionName+"\n"+"作者：smile\nE-MAIL:shangweixiao@qq.com";
        	new AlertDialog.Builder(MainActivity.this).setTitle("关于").setMessage(info).setPositiveButton("确定", null).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private RadioGroup.OnCheckedChangeListener radiogpchange = new RadioGroup.OnCheckedChangeListener() {
    	  @Override
    	  public void onCheckedChanged(RadioGroup group, int checkedId) {
    		  if(checkedId == hour24.getId()) {
    			  
    		  } else if (checkedId == hour12.getId()) {
    			  
    		  }
    	}
    };
    
    void tpd_init(int h,int m){  
        TimePickerDialog.OnTimeSetListener otsl=new TimePickerDialog.OnTimeSetListener(){  
            public void onTimeSet(TimePicker view, int hourOfDay, int min) {
            	hour = hourOfDay;
            	minute = min;
            	sendTime.setText((hour>9?String.valueOf(hour):("0"+String.valueOf(hour)))+":"+(minute>9?String.valueOf(minute):("0"+String.valueOf(minute))));
                tpd.dismiss();
            }  
        };  

        tpd=new TimePickerDialog(this,otsl,h,m,true);
        tpd.setTitle("设置发送时间");
    }
    
  //遍历设置字体
    public static void changeViewSize(ViewGroup viewGroup,int screenWidth,int screenHeight) {//传入Activity顶层Layout,屏幕宽,屏幕高
    		int adjustFontSize = adjustFontSize(screenWidth,screenHeight);
    		for(int i = 0; i<viewGroup.getChildCount(); i++ ){
    			View v = viewGroup.getChildAt(i);
    			if(v instanceof ViewGroup){
    				changeViewSize((ViewGroup)v,screenWidth,screenHeight);
    			}else if(v instanceof Button){//按钮加大这个一定要放在TextView上面，因为Button也继承了TextView
    				( (Button)v ).setTextSize(adjustFontSize+2);
    			}else if(v instanceof TextView){
    			}
    		}
    	}


    //获取字体大小
    public static int adjustFontSize(int screenWidth, int screenHeight) {
    		screenWidth=screenWidth>screenHeight?screenWidth:screenHeight;
    		/**
    		 * 1. 在视图的 onsizechanged里获取视图宽度，一般情况下默认宽度是320，所以计算一个缩放比率
       			rate = (float) w/320   w是实际宽度
    		   2.然后在设置字体尺寸时 paint.setTextSize((int)(8*rate));   8是在分辨率宽为320 下需要设置的字体大小
      			实际字体大小 = 默认字体大小 x  rate
    		 */
    		int rate = (int)(5*(float) screenWidth/320); //我自己测试这个倍数比较适合，当然你可以测试后再修改
    		return rate<15?15:rate; //字体太小也不好看的
    }

    public static String getVersion(Context context){
		try {
			PackageInfo pi=context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			return pi.versionName;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "unknow";
		}
	}
    
    Runnable runnableWeather = new Runnable() {  
        
        @Override  
        public void run() {//run()在新的线程中运行  
            
        	while(true)
        	{
            	String weather=null;
    			try {
    				weather = es.submit(new MyWeather(areaCode,0)).get();
    			} catch (InterruptedException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			} catch (ExecutionException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
                mHandler.obtainMessage(MSG_WEATHER,weather).sendToTarget();
                break;
        	}
        }  
    };
    
    Runnable runnableAlert = new Runnable() {
        @Override  
        public void run() {//run()在新的线程中运行  
            
        	while(true)
        	{
            	String alert=null;
    			try {
    				alert = es.submit(new MyWeatherAlert(getApplicationContext(),areaCode,0)).get();
    			} catch (InterruptedException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			} catch (ExecutionException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
                mHandler.obtainMessage(MSG_ALERT,alert).sendToTarget();
                break;
        	}
        } 
    };
}