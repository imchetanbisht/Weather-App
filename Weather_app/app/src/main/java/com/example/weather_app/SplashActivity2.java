package com.example.weather_app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SplashActivity2 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash2);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent splash=new Intent(SplashActivity2.this,OTPActivity2.class);
                startActivity(splash);
                finish();
            }
        },2000);
    }
}