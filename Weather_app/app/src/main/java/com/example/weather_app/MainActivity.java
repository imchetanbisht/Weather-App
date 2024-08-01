package com.example.weather_app;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final String BASE_URL = "https://api.openweathermap.org/";
    private static final String API_KEY = "aa0bf1d7b9bc1615d9cf239a53ba7519"; // Replace with your actual API key
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int REQUEST_CHECK_SETTINGS = 2;

    private Geocoder geocoder;
    private SearchView searchView;
    private TextView cityName, temp, weather, minTemp, maxTemp, humidity, windspeed, sea, condition, sunrise, sunset, day, date;
    private ImageView backgroundImageView, weatherIconImageView;
    private FirebaseAuth mAuth;
    private WeatherApi weatherApiService;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(MainActivity.this, OTPActivity2.class);
            startActivity(intent);
            finish();
        }

        geocoder = new Geocoder(this, Locale.getDefault());

        // Initialize UI elements
        searchView = findViewById(R.id.searchView);
        cityName = findViewById(R.id.cityName);
        temp = findViewById(R.id.temp);
        weather = findViewById(R.id.weather);
        minTemp = findViewById(R.id.min_temp);
        maxTemp = findViewById(R.id.max_temp);
        humidity = findViewById(R.id.humidity);
        windspeed = findViewById(R.id.windspeed);
        sea = findViewById(R.id.sea);
        condition = findViewById(R.id.condition);
        sunrise = findViewById(R.id.sunrise);
        sunset = findViewById(R.id.sunset);
        day = findViewById(R.id.day);
        date = findViewById(R.id.date);
        backgroundImageView = findViewById(R.id.backgroundImageView);
        weatherIconImageView = findViewById(R.id.weather_icon);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        weatherApiService = retrofit.create(WeatherApi.class);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                fetchWeatherData(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Optionally handle text changes for city suggestions
                return true;
            }
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestLocationPermission();
        updateDateAndDay();
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            checkLocationSettings();
        }
    }

    private void checkLocationSettings() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);

        Task<LocationSettingsResponse> task = LocationServices.getSettingsClient(this).checkLocationSettings(builder.build());

        task.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    fetchLastLocation(); // Fetch location immediately after settings check
                } catch (ApiException exception) {
                    switch (exception.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            try {
                                ResolvableApiException resolvable = (ResolvableApiException) exception;
                                resolvable.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException e) {
                                e.printStackTrace();
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            Toast.makeText(MainActivity.this, "Location settings are inadequate, and cannot be fixed here. Please fix in Settings.", Toast.LENGTH_LONG).show();
                            break;
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                fetchLastLocation();
            } else {
                Toast.makeText(this, "Location services are required to fetch weather data.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fetchLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        getCityFromLocation(location.getLatitude(), location.getLongitude()); // Fetch weather based on location
                    } else {
                        requestNewLocationData(); // Request new location if last known location is null
                    }
                }
            });
        } else {
            Toast.makeText(this, "Location permission not granted.", Toast.LENGTH_SHORT).show();
        }
    }

    private void getCityFromLocation(double latitude, double longitude) {
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                String city = addresses.get(0).getLocality(); // Get city name
                if (city != null) {
                    fetchWeatherData(city); // Fetch weather using city name
                } else {
                    Toast.makeText(this, "City not found", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Unable to find city", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error getting city name", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestNewLocationData() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000); // 10 seconds
        locationRequest.setFastestInterval(5000); // 5 seconds

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null && locationResult.getLocations().size() > 0) {
                    Location location = locationResult.getLastLocation();
                    getCityFromLocation(location.getLatitude(), location.getLongitude()); // Fetch weather based on new location
                    fusedLocationClient.removeLocationUpdates(locationCallback);
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void fetchWeatherData(String cityName) {
        Call<WeatherResponse> call = weatherApiService.getWeather(cityName, API_KEY, "metric"); // Use getWeather method
        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful()) {
                    WeatherResponse weatherResponse = response.body();
                    if (weatherResponse != null) {
                        updateUIWithWeatherData(weatherResponse);
                    } else {
                        Toast.makeText(MainActivity.this, "No weather data available", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Failed to get weather data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                t.printStackTrace();
                Toast.makeText(MainActivity.this, "Failed to get weather data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUIWithWeatherData(WeatherResponse weatherResponse) {
        cityName.setText(weatherResponse.getName());
        temp.setText(String.format(Locale.getDefault(), "%.2f °C", weatherResponse.getMain().getTemp()));
        weather.setText(weatherResponse.getWeather().get(0).getDescription());
        minTemp.setText(String.format(Locale.getDefault(), "Min: %.2f °C", weatherResponse.getMain().getTempMin()));
        maxTemp.setText(String.format(Locale.getDefault(), "Max: %.2f °C", weatherResponse.getMain().getTempMax()));
        humidity.setText(String.format(Locale.getDefault(), "Humidity: %d%%", weatherResponse.getMain().getHumidity()));
        windspeed.setText(String.format(Locale.getDefault(), "Wind Speed: %.2f m/s", weatherResponse.getWind().getSpeed()));
        sea.setText(String.format(Locale.getDefault(), "Pressure: %d hPa", weatherResponse.getMain().getPressure())); // Corrected %d for integer
        condition.setText(weatherResponse.getWeather().get(0).getMain());
        sunrise.setText(convertTimestampToTime(weatherResponse.getSys().getSunrise()));
        sunset.setText(convertTimestampToTime(weatherResponse.getSys().getSunset()));

        // Set background image based on weather condition
        String weatherCondition = weatherResponse.getWeather().get(0).getMain().toLowerCase();
        Log.d("WeatherCondition", weatherCondition); // Log the weather condition
        int backgroundImageResource = getBackgroundImageResource(weatherCondition);
        backgroundImageView.setImageResource(backgroundImageResource);

        // Set weather icon based on weather condition
        int weatherIconResource = getWeatherIconResource(weatherCondition);
        weatherIconImageView.setImageResource(weatherIconResource); // Add this line to set the weather icon
    }

    private int getBackgroundImageResource(String weatherCondition) {
        weatherCondition = weatherCondition.toLowerCase(); // Convert to lower case for comparison
        switch (weatherCondition) {
            case "clear":
            case "clear sky":
            case "sunny":
                return R.drawable.sunny;
            case "clouds":
            case "partly cloudy":
            case "broken clouds":
            case "scattered clouds":
            case "few clouds":
                return R.drawable.haze;
            case "rain":
            case "drizzle":
            case "heavy rain":
            case "moderate rain":
            case "light rain":
                return R.drawable.rain_back;
            case "snow":
            case "heavy snow":
            case "light snow":
            case "moderate snow":
            case "blizzard":
                return R.drawable.snow;
            case "thunderstorm":
                return R.drawable.thunderstorm;
            case "haze":
            case "mist":
            case "foggy":
                return R.drawable.haze;
            default:
                return R.drawable.welcome_back;
        }
    }

    private int getWeatherIconResource(String weatherCondition) {
        switch (weatherCondition) {
            case "clear":
            case "clear sky":
            case "sunny":
                return R.drawable.sun_symbol;
            case "clouds":
            case "partly cloudy":
            case "broken clouds":
            case "scattered clouds":
            case "few clouds":
                return R.drawable.cloud_icon;
            case "rain":
            case "drizzle":
            case "heavy rain":
            case "moderate rain":
            case "light rain":
                return R.drawable.rain_icon;
            case "snow":
            case "heavy snow":
            case "light snow":
            case "moderate snow":
            case "blizzard":
                return R.drawable.snow_icon;
            case "thunderstorm":
                return R.drawable.thunder_icon;
            case "haze":
            case "mist":
            case "foggy":
                return R.drawable.haze_icon;
            default:
                return R.drawable.default_icon;
        }
    }

    private void updateDateAndDay() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        Date currentDate = new Date();

        date.setText(dateFormat.format(currentDate));
        day.setText(dayFormat.format(currentDate));
    }

    private String convertTimestampToTime(long timestamp) {
        Date date = new Date(timestamp * 1000);
        SimpleDateFormat format = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return format.format(date);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, fetch the last known location
                checkLocationSettings();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
