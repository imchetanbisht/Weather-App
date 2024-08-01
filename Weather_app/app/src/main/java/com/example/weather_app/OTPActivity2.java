package com.example.weather_app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class OTPActivity2 extends AppCompatActivity {

    private EditText edtPhone, edtOTP;
    private Button btnGetOTP, btnVerifyOTP;
    private FirebaseAuth mAuth;
    private String verificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otpactivity2);

        mAuth = FirebaseAuth.getInstance();

        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is signed in, navigate to MainActivity
            navigateToMainActivity();
        }

        edtPhone = findViewById(R.id.idEdtPhoneNumber);
        edtOTP = findViewById(R.id.idEdtOtp);
        btnGetOTP = findViewById(R.id.idBtnGetOtp);
        btnVerifyOTP = findViewById(R.id.idBtnVerify);

        btnGetOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phone = edtPhone.getText().toString().trim();
                if (TextUtils.isEmpty(phone)) {
                    Toast.makeText(OTPActivity2.this, "Enter a valid phone number", Toast.LENGTH_SHORT).show();
                    return;
                }
                sendVerificationCode(phone);
            }
        });

        btnVerifyOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code = edtOTP.getText().toString().trim();
                if (TextUtils.isEmpty(code)) {
                    Toast.makeText(OTPActivity2.this, "Enter OTP", Toast.LENGTH_SHORT).show();
                    return;
                }
                verifyCode(code);
            }
        });
    }

    private void sendVerificationCode(String phone) {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber("+91" + phone)  // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS)  // Timeout and unit
                        .setActivity(this)  // Activity (for callback binding)
                        .setCallbacks(mCallBack)  // OnVerificationStateChangedCallbacks
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallBack =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                    final String code = phoneAuthCredential.getSmsCode();
                    if (code != null) {
                        edtOTP.setText(code);
                        verifyCode(code);
                    }
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    Toast.makeText(OTPActivity2.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }

                @Override
                public void onCodeSent(@NonNull String s,
                                       @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                    super.onCodeSent(s, forceResendingToken);
                    verificationId = s;
                }
            };

    private void verifyCode(String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithCredential(credential);
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(OTPActivity2.this, "Verification Successful", Toast.LENGTH_SHORT).show();
                        navigateToMainActivity();
                    } else {
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(OTPActivity2.this, "Verification Failed, Invalid credentials", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(OTPActivity2.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
