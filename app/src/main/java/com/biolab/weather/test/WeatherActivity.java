package com.biolab.weather.test;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.biolab.weather.test.R;
import com.biolab.weather.test.gson.Forecast;
import com.biolab.weather.test.gson.Weather;
import com.biolab.weather.test.service.AutoUpdateService;
import com.biolab.weather.test.util.HttpUtil;
import com.biolab.weather.test.util.Utility;
import com.bumptech.glide.Glide;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    protected DrawerLayout drawer;
    protected SwipeRefreshLayout swipeRefresh;

    private ScrollView weatherLayout;
    private TextView tv_title;
    private TextView tv_updatetime;
    private TextView tv_degree;
    private TextView tv_info;
    private LinearLayout forecastLayout;
    private TextView tv_aqi;
    private TextView tv_pm25;
    private TextView tv_comfort;
    private TextView tv_cw;
    private TextView tv_sport;
    private ImageView bing_pic;
    private Button btn_nav;

    protected String mWeatherId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT >= 21) {
            View decView = getWindow().getDecorView();
            decView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);

        drawer = (DrawerLayout) findViewById(R.id.drawer);
        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        weatherLayout = (ScrollView) findViewById(R.id.weather_layout);
        tv_title = (TextView) findViewById(R.id.title_city);
        tv_updatetime = (TextView) findViewById(R.id.title_update_time);
        tv_degree = (TextView) findViewById(R.id.tv_tmp);
        tv_info = (TextView) findViewById(R.id.tv_weather_info);
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);
        tv_aqi = (TextView) findViewById(R.id.tv_aqi);
        tv_pm25 = (TextView) findViewById(R.id.tv_pm25);
        tv_comfort = (TextView) findViewById(R.id.tv_comfort);
        tv_cw = (TextView) findViewById(R.id.tv_cf);
        tv_sport = (TextView) findViewById(R.id.tv_sport);
        bing_pic = (ImageView) findViewById(R.id.bing_pic);
        btn_nav = (Button) findViewById(R.id.btn_nav);

        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherStr = prefs.getString("weather", null);
        String bingPic = prefs.getString("bing_pic", null);
        if(bingPic != null) {
            Glide.with(this).load(bingPic).into(bing_pic);
        } else {
            loadBingPic();
        }
        if(weatherStr != null) {
            Weather weather = Utility.handleWeatherResponse(weatherStr);
            mWeatherId = weather.getBasic().getWeatherId();
            showWeatherInfo(weather);
        } else {
            mWeatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(mWeatherId);
        }

        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId);
            }
        });

        btn_nav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawer.openDrawer(GravityCompat.START);
            }
        });
    }

    private void loadBingPic() {
        String urlBingPic = "http://guolin.tech/api/bing_pic";
                HttpUtil.sendOkHttpRequest(urlBingPic, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bing_pic);
                    }
                });
            }
        });
    }

    protected void requestWeather(final String weatherId) {
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" +
                weatherId + "&key=360bf9926ee74a23b7c5b880c47584a2";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气失败", Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String weatherResponse = response.body().string();
                final Weather weather= Utility.handleWeatherResponse(weatherResponse);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weather != null && "ok" != weather.getStatus()) {
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather", weatherResponse);
                            editor.apply();
                            showWeatherInfo(weather);
                        }
                        swipeRefresh.setRefreshing(false);
                    }
                });

            }
        });
    }

    private void showWeatherInfo(Weather weather) {
        String cityName = weather.getBasic().getCityName();
        String updateTime = weather.getBasic().getUpdate().getUpdateTime().split(" ")[1];
        String degree = weather.getNow().getTemperature() + "℃";
        String weatherInfo = weather.getNow().getMore().getInfo();
        tv_title.setText(cityName);
        tv_updatetime.setText(updateTime);
        tv_degree.setText(degree);
        tv_info.setText(weatherInfo);

        forecastLayout.removeAllViews();
        for(Forecast forecast : weather.getForecasts()) {
            View view = LayoutInflater.from(WeatherActivity.this).inflate(R.layout.forecast_item, forecastLayout, false);
            TextView tv_date = (TextView) view.findViewById(R.id.tv_date);
            TextView tv_info = (TextView) view.findViewById(R.id.tv_info);
            TextView tv_max = (TextView) view.findViewById(R.id.tv_max);
            TextView tv_min = (TextView)view.findViewById(R.id.tv_min);
            tv_date.setText(forecast.getDate());
            tv_info.setText(forecast.getMore().getInfo());
            tv_max.setText(forecast.getTemperature().getMax());
            tv_min.setText(forecast.getTemperature().getMin());
            forecastLayout.addView(view);
        }

        if(weather.getAqi() != null) {
            tv_aqi.setText(weather.getAqi().getCity().getAqi());
            tv_pm25.setText(weather.getAqi().getCity().getPm25());
        }

        String comfort = "舒适度：" + weather.getSuggestion().getComfort().getInfo();
        String carWash = "洗车指数：" + weather.getSuggestion().getCarWash().getInfo();
        String sport = "运动建议：" + weather.getSuggestion().getSport().getInfo();

        tv_comfort.setText(comfort);
        tv_cw.setText(carWash);
        tv_sport.setText(sport);

        weatherLayout.setVisibility(View.VISIBLE);

        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);
    }
}
