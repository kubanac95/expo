package host.exp.exponent.notifications.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;
import expo.modules.notifications.notifications.interfaces.NotificationTrigger;
import expo.modules.notifications.notifications.model.NotificationContent;
import expo.modules.notifications.notifications.model.NotificationRequest;
import host.exp.exponent.kernel.ExperienceKey;

public class ScopedNotificationRequest extends NotificationRequest {
  // We store String instead of ExperienceId, cause ScopedNotificationRequest must be serializable.
  private String mExperienceIdString;

  public ScopedNotificationRequest(String identifier, NotificationContent content, NotificationTrigger trigger, @Nullable String experienceId) {
    super(identifier, content, trigger);
    mExperienceIdString = experienceId;
  }

  private ScopedNotificationRequest(Parcel in) {
    super(in);
    mExperienceIdString = in.readString();
  }

  public boolean checkIfBelongsToExperience(@Nullable ExperienceKey experienceKey) {
    if (mExperienceIdString == null) {
      return true;
    }
    return mExperienceIdString.equals(experienceKey.getLegacyId()) || mExperienceIdString.equals(experienceKey.getStableLegacyId());
  }

  public static final Creator<ScopedNotificationRequest> CREATOR = new Creator<ScopedNotificationRequest>() {
    public ScopedNotificationRequest createFromParcel(Parcel in) {
      return new ScopedNotificationRequest(in);
    }

    public ScopedNotificationRequest[] newArray(int size) {
      return new ScopedNotificationRequest[size];
    }
  };

  @Nullable
  public String getExperienceIdString() {
    return mExperienceIdString;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeString(mExperienceIdString);
  }
}
