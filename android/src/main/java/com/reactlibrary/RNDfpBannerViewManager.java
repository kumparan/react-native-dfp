package com.reactlibrary;

import android.util.Log;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableNativeArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.PixelUtil;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.facebook.react.views.view.ReactViewGroup;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RNDfpBannerViewManager extends SimpleViewManager<ReactViewGroup> {

  public static final String REACT_CLASS = "RNDFPBanner";

  public static final String PROP_AD_SIZES = "adSizes";
  public static final String PROP_DIMENSIONS = "dimensions";
  public static final String PROP_BANNER_SIZE = "bannerSize";
  public static final String PROP_AD_UNIT_ID = "adUnitID";
  public static final String PROP_TEST_DEVICE_ID = "testDeviceID";

  private String testDeviceID = null;
  private String adUnitID = null;

  public enum Events {
    EVENT_SIZE_CHANGE("onSizeChange"),
    EVENT_RECEIVE_AD("onAdViewDidReceiveAd"),
    EVENT_ERROR("onDidFailToReceiveAdWithError"),
    EVENT_WILL_PRESENT("onAdViewWillPresentScreen"),
    EVENT_WILL_DISMISS("onAdViewWillDismissScreen"),
    EVENT_DID_DISMISS("onAdViewDidDismissScreen"),
    EVENT_WILL_LEAVE_APP("onAdViewWillLeaveApplication"),
    EVENT_ADMOB_EVENT_RECEIVED("onAdmobDispatchAppEvent");

    private final String mName;

    Events(final String name) {
      mName = name;
    }

    @Override
    public String toString() {
      return mName;
    }
  }

  private ThemedReactContext mThemedReactContext;
  private RCTEventEmitter mEventEmitter;

  @Override
  public String getName() {
    return REACT_CLASS;
  }


  @Override
  protected ReactViewGroup createViewInstance(ThemedReactContext themedReactContext) {
    mThemedReactContext = themedReactContext;
    mEventEmitter = themedReactContext.getJSModule(RCTEventEmitter.class);
    ReactViewGroup view = new ReactViewGroup(themedReactContext);
    attachNewAdView(view);
    return view;
   }

  int viewID = -1;
  protected void attachNewAdView(final ReactViewGroup view) {
    final AdView adView = new AdView(mThemedReactContext);
    // destroy old AdView if present
    AdView oldAdView = (AdView) view.getChildAt(0);
    view.removeAllViews();
    if (oldAdView != null) oldAdView.destroy();
    view.addView(adView);
    attachEvents(view);
  }

  protected void attachEvents(final ReactViewGroup view) {
    viewID = view.getId();
    final AdView adView = (AdView) view.getChildAt(0);
    adView.setAdListener(new AdListener() {
      @Override
      public void onAdLoaded() {
        int width = adView.getAdSize().getWidthInPixels(mThemedReactContext);
        int height = adView.getAdSize().getHeightInPixels(mThemedReactContext);
        int left = adView.getLeft();
        int top = adView.getTop();
        adView.measure(width, height);
        adView.layout(left, top, left + width, top + height);
        mEventEmitter.receiveEvent(view.getId(), Events.EVENT_RECEIVE_AD.toString(), null);
      }

      @Override
      public void onAdFailedToLoad(LoadAdError adError) {
        WritableMap event = Arguments.createMap();
        switch (adError.getCode()) {
          case AdRequest.ERROR_CODE_INTERNAL_ERROR:
            event.putString("error", "ERROR_CODE_INTERNAL_ERROR");
            break;
          case AdRequest.ERROR_CODE_INVALID_REQUEST:
            event.putString("error", "ERROR_CODE_INVALID_REQUEST");
            break;
          case AdRequest.ERROR_CODE_NETWORK_ERROR:
            event.putString("error", "ERROR_CODE_NETWORK_ERROR");
            break;
          case AdRequest.ERROR_CODE_NO_FILL:
            event.putString("error", "ERROR_CODE_NO_FILL");
            break;
        }

        mEventEmitter.receiveEvent(view.getId(), Events.EVENT_ERROR.toString(), event);
      }

      @Override
      public void onAdOpened() {
        mEventEmitter.receiveEvent(view.getId(), Events.EVENT_WILL_PRESENT.toString(), null);
      }

      @Override
      public void onAdClosed() {
        mEventEmitter.receiveEvent(view.getId(), Events.EVENT_WILL_DISMISS.toString(), null);
      }
    });
  }

  @Override
  @Nullable
  public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
    MapBuilder.Builder<String, Object> builder = MapBuilder.builder();
    for (Events event : Events.values()) {
      builder.put(event.toString(), MapBuilder.of("registrationName", event.toString()));
    }
    return builder.build();
  }

  @ReactProp(name = PROP_AD_SIZES)
  public void setAdSizes(final ReactViewGroup view, final ReadableArray adSizesProp) {
    if (adSizesProp != null) {
      ArrayList<AdSize> adSizesArrayList = new ArrayList<AdSize>();
      ReadableNativeArray nativeadSizesArray = (ReadableNativeArray)adSizesProp;
      for (Object obj : nativeadSizesArray.toArrayList()) {
        if (obj instanceof String) {
          AdSize adSize = getAdSizeFromString((String)obj);
          adSizesArrayList.add(adSize);
        }

        if (obj instanceof HashMap) {
          HashMap dimensions = (HashMap)obj;
          if (dimensions.containsKey("width") && dimensions.containsKey("height")) {

            AdSize adSize = new AdSize(
              (int) Double.parseDouble(dimensions.get("width").toString()),
              (int) Double.parseDouble(dimensions.get("height").toString())
            );
            adSizesArrayList.add(adSize);
          }
        }
      }

      AdSize[] adSizes = adSizesArrayList.toArray(new AdSize[adSizesArrayList.size()]);

      attachNewAdView(view);
      AdView newAdView = (AdView) view.getChildAt(0);
      for (AdSize az : adSizes) {
        newAdView.setAdSize(az);
      }
      newAdView.setAdUnitId(adUnitID);

      // send measurements to js to style the AdView in react
      WritableMap event = Arguments.createMap();
      event.putDouble("width", newAdView.getAdSize().getWidth());
      event.putDouble("height", newAdView.getAdSize().getHeight());
      mEventEmitter.receiveEvent(view.getId(), Events.EVENT_SIZE_CHANGE.toString(), event);

      loadAd(newAdView);
    }
  }

  @ReactProp(name = PROP_DIMENSIONS)
  public void setDimensions(final ReactViewGroup view, final ReadableMap dimensions) {
    if (dimensions != null && dimensions.hasKey("width") && !dimensions.isNull("width") && dimensions.hasKey("height") && !dimensions.isNull("height")) {
      AdSize adSize = new AdSize(dimensions.getInt("width"), dimensions.getInt("height"));

      attachNewAdView(view);
      AdView newAdView = (AdView) view.getChildAt(0);
      newAdView.setAdSize(adSize);
      newAdView.setAdUnitId(adUnitID);

      // send measurements to js to style the AdView in react
      WritableMap event = Arguments.createMap();
      event.putDouble("width", adSize.getWidth());
      event.putDouble("height", adSize.getHeight());
      mEventEmitter.receiveEvent(view.getId(), Events.EVENT_SIZE_CHANGE.toString(), event);

      loadAd(newAdView);
    }
  }

  @ReactProp(name = PROP_BANNER_SIZE)
  public void setBannerSize(final ReactViewGroup view, final String sizeString) {
    if (sizeString != null && !sizeString.isEmpty()) {
      AdSize adSize = getAdSizeFromString(sizeString);


      attachNewAdView(view);
      AdView newAdView = (AdView) view.getChildAt(0);
      newAdView.setAdSize(adSize);
      newAdView.setAdUnitId(adUnitID);

      // send measurements to js to style the AdView in react
      int width;
      int height;
      WritableMap event = Arguments.createMap();
      if (adSize == AdSize.SMART_BANNER) {
        width = (int) PixelUtil.toDIPFromPixel(adSize.getWidthInPixels(mThemedReactContext));
        height = (int) PixelUtil.toDIPFromPixel(adSize.getHeightInPixels(mThemedReactContext));
      }
      else {
        width = adSize.getWidth();
        height = adSize.getHeight();
      }
      event.putDouble("width", width);
      event.putDouble("height", height);
      mEventEmitter.receiveEvent(view.getId(), Events.EVENT_SIZE_CHANGE.toString(), event);

      loadAd(newAdView);
    }
  }

  @ReactProp(name = PROP_AD_UNIT_ID)
  public void setAdUnitID(final ReactViewGroup view, final String adUnitID) {
    this.adUnitID = adUnitID;
  }

  @ReactProp(name = PROP_TEST_DEVICE_ID)
  public void setPropTestDeviceID(final ReactViewGroup view, final String testDeviceID) {
    this.testDeviceID = testDeviceID;
  }

  private void loadAd(final AdView adView) {
    if (adView.getAdSize() != null && adUnitID != null) {

      if (adUnitID != adView.getAdUnitId()) {
        adView.setAdUnitId(adUnitID);
      }

      List<String> testDeviceIds = null;

      if (testDeviceID != null) {
        if (testDeviceID.equals("EMULATOR")) {
          testDeviceIds = Arrays.asList(AdRequest.DEVICE_ID_EMULATOR);
        } else {
          testDeviceIds = Arrays.asList(testDeviceID);
        }

        RequestConfiguration configuration =
                new RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build();
        MobileAds.setRequestConfiguration(configuration);
      }

      AdRequest adRequest = new AdRequest.Builder().build();
      adView.loadAd(adRequest);
    }
  }

  private AdSize getAdSizeFromString(String adSize) {
    switch (adSize) {
      case "largeBanner":
        return AdSize.LARGE_BANNER;
      case "mediumRectangle":
        return AdSize.MEDIUM_RECTANGLE;
      case "fullBanner":
        return AdSize.FULL_BANNER;
      case "leaderBoard":
        return AdSize.LEADERBOARD;
      default:
        return AdSize.BANNER;
    }
  }
}
