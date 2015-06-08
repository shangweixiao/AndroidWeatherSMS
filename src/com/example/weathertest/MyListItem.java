package com.example.weathertest;

public class MyListItem {
	private String name;
	private String pcode;
	private String city_num;
	public String getName(){
		return name;
	}
	public String getPcode(){
		return pcode;
	}
	public void setName(String name){
		this.name=name;
	}
	public void setPcode(String pcode){
		this.pcode=pcode;
	}
	public String getCityNum(){
		return city_num;
	}
	public void setCityNum(String city_num){
		this.city_num = city_num;
	}
}
