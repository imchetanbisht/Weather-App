package com.example.weather_app;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApi {
    @GET("data/2.5/weather")
    Call<WeatherResponse> getWeather(
            @Query("q") String cityName,
            @Query("appid") String apiKey,
            @Query("units") String units
    );

    @GET("data/2.5/weather")
    Call<WeatherResponse> getWeatherByLocation(
            @Query("lat") double latitude,
            @Query("lon") double longitude,
            @Query("appid") String apiKey,
            @Query("units") String units
    );
}