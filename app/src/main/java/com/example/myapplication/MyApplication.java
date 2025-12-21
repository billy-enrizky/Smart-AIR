package com.example.myapplication;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Firebase if not already initialized
        if (FirebaseApp.getApps(this).isEmpty()) {
            try {
                FirebaseOptions options = new FirebaseOptions.Builder()
                        .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                        .setApplicationId(BuildConfig.FIREBASE_APPLICATION_ID)
                        .setApiKey(BuildConfig.FIREBASE_API_KEY)
                        .setDatabaseUrl(BuildConfig.FIREBASE_DATABASE_URL)
                        .build();
                
                FirebaseApp.initializeApp(this, options);
                Log.d(TAG, "Firebase initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize Firebase", e);
            }
        } else {
            Log.d(TAG, "Firebase already initialized");
        }
    }
}

