package com.wangkang.coolweather.gson;

import com.google.gson.annotations.SerializedName;
import com.wangkang.coolweather.gson.weather.AQI;
import com.wangkang.coolweather.gson.weather.Basic;
import com.wangkang.coolweather.gson.weather.Forecast;
import com.wangkang.coolweather.gson.weather.Now;
import com.wangkang.coolweather.gson.weather.Suggestion;

import java.util.List;

public class Weather {

    public String status;

    public Basic basic;

    public AQI aqi;

    public Now now;

    public Suggestion suggestion;

    @SerializedName("daily_forecast")
    public List<Forecast> forecaseList;
}
