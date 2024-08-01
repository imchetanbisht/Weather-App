package com.example.weather_app;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class WeatherResponse {
    private String name;
    private Main main;
    private List<Weather> weather;
    private Wind wind;
    private Sys sys;

    public String getName() {
        return name;
    }

    public Main getMain() {
        return main;
    }

    public List<Weather> getWeather() {
        return weather;
    }

    public Wind getWind() {
        return wind;
    }

    public Sys getSys() {
        return sys;
    }

    public static class Main {
        private float temp;
        private float temp_min;
        private float temp_max;
        private int humidity;
        private int pressure;

        public float getTemp() {
            return temp;
        }

        public float getTempMin() {
            return temp_min;
        }

        public float getTempMax() {
            return temp_max;
        }

        public int getHumidity() {
            return humidity;
        }

        public int getPressure() {
            return pressure;
        }
    }

    public static class Weather {
        private String description;
        private String main;
        private String icon; // Added icon field

        public String getDescription() {
            return description;
        }

        public String getMain() {
            return main;
        }

        public String getIcon() {
            return icon; // Added getter for icon
        }
    }

    public static class Wind {
        private float speed;

        public float getSpeed() {
            return speed;
        }
    }

    public static class Sys {
        private long sunrise;
        private long sunset;

        public long getSunrise() {
            return sunrise;
        }

        public long getSunset() {
            return sunset;
        }
    }
}
