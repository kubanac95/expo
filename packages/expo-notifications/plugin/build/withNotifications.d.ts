import { ConfigPlugin } from '@expo/config-plugins';
export declare type NotificationsPluginProps = {
  /**
   * (Android only) Local path to an image to use as the icon for push notifications.
   * 96x96 all-white png with transparency. We recommend following Google's design guidelines.
   */
  icon?: string;
  /**
   * (Android only) Tint color for the push notification image when it appears in the notification tray.
   * Defaults to #ffffff
   */
  color?: string;
  /**
   * Array of local paths to sound files (.wav recommended) that can be used as custom notification sounds.
   */
  sounds?: string[];
  /**
   * (iOS only) Environment of the app: either 'development' or 'production'. Defaults to 'development'.
   */
  mode?: 'development' | 'production';
};
declare const _default: ConfigPlugin<NotificationsPluginProps>;
export default _default;
