// Copyright 2015-present 650 Industries. All rights reserved.

package host.exp.exponent.kernel.services.sensors;

import android.content.Context;
import android.hardware.SensorEvent;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import host.exp.exponent.kernel.ExperienceKey;

public abstract class SubscribableSensorKernelService extends BaseSensorKernelService {
  protected static int DEFAULT_UPDATE_INTERVAL = 100;

  private Map<ExperienceKey, Integer> mExperienceKeyListenersCountMap = new HashMap<>();
  private Map<SensorKernelServiceSubscription, Long> mSensorEventListenerLastUpdateMap = new WeakHashMap<>();
  private Map<ExperienceKey, List<WeakReference<SensorKernelServiceSubscription>>> mExperienceKeySubscriptionsMap = new HashMap<>();

  SubscribableSensorKernelService(Context reactContext) {
    super(reactContext);
  }

  // BaseKernelService

  public void onExperienceForegrounded(ExperienceKey experienceKey) {
    updateObserving();
  }

  public void onExperienceBackgrounded(ExperienceKey experienceKey) {
    updateObserving();
  }

  // BaseSensorKernelService

  @Override
  public void onSensorDataChanged(SensorEvent sensorEvent) {
    long currentTime = System.currentTimeMillis();
    ExperienceKey currentExperienceKey = getCurrentExperienceKey();
    List<WeakReference<SensorKernelServiceSubscription>> listeners = mExperienceKeySubscriptionsMap.get(currentExperienceKey);

    if (listeners != null) {
      for(WeakReference<SensorKernelServiceSubscription> weakReference : listeners) {
        final SensorKernelServiceSubscription sensorKernelServiceSubscription = weakReference.get();
        if (sensorKernelServiceSubscription != null && sensorKernelServiceSubscription.isEnabled()) {
          long lastUpdate = 0;
          if (mSensorEventListenerLastUpdateMap.containsKey(sensorKernelServiceSubscription)) {
            lastUpdate = mSensorEventListenerLastUpdateMap.get(sensorKernelServiceSubscription);
          }

          long updateInterval = DEFAULT_UPDATE_INTERVAL;
          if (sensorKernelServiceSubscription.getUpdateInterval() != null) {
            updateInterval = sensorKernelServiceSubscription.getUpdateInterval();
          }


          if ((currentTime - lastUpdate) > updateInterval) {
            sensorKernelServiceSubscription.getSensorEventListener().onSensorDataChanged(sensorEvent);
            mSensorEventListenerLastUpdateMap.put(sensorKernelServiceSubscription, currentTime);
          }
        }
      }
    }
  }

  // Modules API

  public SensorKernelServiceSubscription createSubscriptionForListener(ExperienceKey experienceKey, SensorEventListener listener) {
    SensorKernelServiceSubscription sensorKernelServiceSubscription = new SensorKernelServiceSubscription(experienceKey, this, listener);
    if (!mExperienceKeySubscriptionsMap.containsKey(experienceKey)) {
      mExperienceKeySubscriptionsMap.put(experienceKey, new ArrayList<WeakReference<SensorKernelServiceSubscription>>());
    }
    mExperienceKeySubscriptionsMap.get(experienceKey).add(new WeakReference<>(sensorKernelServiceSubscription));
    return sensorKernelServiceSubscription;
  }

  public void removeSubscription(SensorKernelServiceSubscription subscriptionToRemove) {
    mSensorEventListenerLastUpdateMap.remove(subscriptionToRemove);
    ExperienceKey experienceKey = subscriptionToRemove.getExperienceKey();
    if (mExperienceKeySubscriptionsMap.containsKey(experienceKey)) {
      List<WeakReference<SensorKernelServiceSubscription>> originalSubscriptions = mExperienceKeySubscriptionsMap.get(experienceKey);
      List<WeakReference<SensorKernelServiceSubscription>> leftSubscriptions = new ArrayList<>();
      for (WeakReference<SensorKernelServiceSubscription> subscriptionWeakReference : originalSubscriptions) {
        SensorKernelServiceSubscription subscription = subscriptionWeakReference.get();
        if (subscription != null && subscription != subscriptionToRemove) {
          leftSubscriptions.add(subscriptionWeakReference);
        }
      }
      if (leftSubscriptions.size() > 0) {
        mExperienceKeySubscriptionsMap.put(experienceKey, leftSubscriptions);
      } else {
        mExperienceKeySubscriptionsMap.remove(experienceKey);
      }
    }
  }

  // SensorKernelServiceSubscription API

  void onSubscriptionEnabledChanged(SensorKernelServiceSubscription sensorKernelServiceSubscription) {
    ExperienceKey experienceKey = sensorKernelServiceSubscription.getExperienceKey();
    int enabledListenersCount = getEnabledListenersForExperienceKey(experienceKey);

    if (sensorKernelServiceSubscription.isEnabled()) {
      mExperienceKeyListenersCountMap.put(experienceKey, enabledListenersCount + 1);
    } else {
      mExperienceKeyListenersCountMap.put(experienceKey, enabledListenersCount - 1);
    }

    if (getEnabledListenersForExperienceKey(experienceKey) == 0) {
      mExperienceKeyListenersCountMap.remove(experienceKey);
    }

    updateObserving();
  }

  // android.hardware.SensorEventListener

  @Override
  public void onSensorChanged(SensorEvent sensorEvent) {
    if (sensorEvent.sensor.getType() == getSensorType()) {
      onSensorDataChanged(sensorEvent);
    }
  }

  // Private helpers

  private int getEnabledListenersForExperienceKey(ExperienceKey experienceKey) {
    if (mExperienceKeyListenersCountMap.containsKey(experienceKey)) {
      return mExperienceKeyListenersCountMap.get(experienceKey);
    }

    return 0;
  }

  private void cleanWeakSubscriptionsList(ExperienceKey experienceKey) {
    List<WeakReference<SensorKernelServiceSubscription>> listeners = mExperienceKeySubscriptionsMap.get(experienceKey);
    List<WeakReference<SensorKernelServiceSubscription>> realListeners = new ArrayList<>();
    if (listeners != null) {
      for (WeakReference<SensorKernelServiceSubscription> subscriptionWeakReference : listeners) {
        if (subscriptionWeakReference.get() != null) {
          realListeners.add(subscriptionWeakReference);
        }
      }
    }

    if (realListeners.size() > 0) {
      mExperienceKeySubscriptionsMap.put(experienceKey, realListeners);
    } else {
      mExperienceKeySubscriptionsMap.remove(experienceKey);
    }
  }

  private void updateObserving() {
    ExperienceKey currentExperienceKey = getCurrentExperienceKey();
    cleanWeakSubscriptionsList(currentExperienceKey);

    // Start/stop observing according to the experience state
    if (getEnabledListenersForExperienceKey(currentExperienceKey) > 0) {
      super.startObserving();
    } else {
      super.stopObserving();
    }
  }
}
