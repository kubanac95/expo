'use strict';
Object.defineProperty(exports, '__esModule', { value: true });
const config_plugins_1 = require('@expo/config-plugins');
const pkg = require('expo-notifications/package.json');

const withNotificationsAndroid_1 = require('./withNotificationsAndroid');
const withNotificationsIOS_1 = require('./withNotificationsIOS');
const withNotifications = (config, props) => {
  config = withNotificationsAndroid_1.withNotificationsAndroid(config, props);
  config = withNotificationsIOS_1.withNotificationsIOS(config, props);
  return config;
};
exports.default = config_plugins_1.createRunOncePlugin(withNotifications, pkg.name, pkg.version);
