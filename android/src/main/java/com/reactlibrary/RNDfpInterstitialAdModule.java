package com.reactlibrary;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.util.HashMap;
import java.util.Map;

public class RNDfpInterstitialAdModule extends ReactContextBaseJavaModule {

    public static final String REACT_CLASS = "RNDFPInterstitial";

    InterstitialAd mInterstitialAd;
    ReactApplicationContext context;

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    public RNDfpInterstitialAdModule(ReactApplicationContext reactContext) {
        super(reactContext);
        context = reactContext;
    }

    @ReactMethod
    public void loadAdFromAdUnitId(final String adUnitID) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run () {
                AdRequest adRequest = new AdRequest.Builder().build();

                InterstitialAd.load(context, adUnitID, adRequest,
                    new InterstitialAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                            mInterstitialAd = interstitialAd;
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            mInterstitialAd = null;
                        }
                    });
            }
        });
    }

    @ReactMethod
    public void showAd() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run () {
                if (mInterstitialAd != null) {
                    mInterstitialAd.show(context.getCurrentActivity());
                }
            }
        });
    }

    @javax.annotation.Nullable
    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put("simulatorId", AdRequest.DEVICE_ID_EMULATOR);
        return constants;
    }
}