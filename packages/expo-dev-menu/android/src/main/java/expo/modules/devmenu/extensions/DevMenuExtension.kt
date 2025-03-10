package expo.modules.devmenu.extensions

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.UiThreadUtil
import com.facebook.react.devsupport.DevInternalSettings
import expo.interfaces.devmenu.DevMenuExtensionInterface
import expo.interfaces.devmenu.DevMenuExtensionSettingsInterface
import expo.interfaces.devmenu.items.DevMenuDataSourceInterface
import expo.interfaces.devmenu.items.DevMenuItemImportance
import expo.interfaces.devmenu.items.DevMenuItemsContainer
import expo.interfaces.devmenu.items.DevMenuScreen
import expo.interfaces.devmenu.items.KeyCommand
import expo.modules.devmenu.DEV_MENU_TAG

class DevMenuExtension(reactContext: ReactApplicationContext)
  : ReactContextBaseJavaModule(reactContext), DevMenuExtensionInterface {
  override fun getName() = "ExpoDevMenuExtensions"

  override fun devMenuItems(settings: DevMenuExtensionSettingsInterface) = DevMenuItemsContainer.export {
    if (!settings.wasRunOnDevelopmentBridge()) {
      return@export
    }

    val reactDevManager = settings
      .manager
      .getSession()
      ?.reactInstanceManager
      ?.devSupportManager
    val devSettings = reactDevManager?.devSettings

    if (reactDevManager == null || devSettings == null) {
      Log.w(DEV_MENU_TAG, "Couldn't export dev-menu items, because react-native bridge doesn't contain the dev support manager.")
      return@export
    }

    // RN will temporary disable `devSupport` if the current activity isn't active.
    // Because of that we can't call some functions like `toggleElementInspector`.
    // However, we can temporary set the `devSupport` flag to true and run needed methods.
    val runWithDevSupportEnabled = { action: () -> Unit ->
      val currentSetting = reactDevManager.devSupportEnabled
      reactDevManager.devSupportEnabled = true
      action()
      reactDevManager.devSupportEnabled = currentSetting
    }

    val reloadAction = {
      UiThreadUtil.runOnUiThread {
        reactDevManager.handleReloadJS()
      }
    }

    val elementInspectorAction = {
      runWithDevSupportEnabled {
        reactDevManager.toggleElementInspector()
      }
    }

    val performanceMonitorAction = {
      requestOverlaysPermission()
      runWithDevSupportEnabled {
        reactDevManager.setFpsDebugEnabled(!devSettings.isFpsDebugEnabled)
      }
    }

    val remoteDebugAction = {
      UiThreadUtil.runOnUiThread {
        devSettings.isRemoteJSDebugEnabled = !devSettings.isRemoteJSDebugEnabled
        reactDevManager.handleReloadJS()
      }
    }

    action("reload", reloadAction) {
      label = { "Reload" }
      glyphName = { "reload" }
      keyCommand = KeyCommand(KeyEvent.KEYCODE_R)
      importance = DevMenuItemImportance.HIGHEST.value
    }

    action("inspector", elementInspectorAction) {
      isEnabled = { devSettings.isElementInspectorEnabled }
      label = { if (isEnabled()) "Hide Element Inspector" else "Show Element Inspector" }
      glyphName = { "border-style" }
      keyCommand = KeyCommand(KeyEvent.KEYCODE_I)
      importance = DevMenuItemImportance.HIGH.value
    }

    action("performance-monitor", performanceMonitorAction) {
      isEnabled = { devSettings.isFpsDebugEnabled }
      label = { if (isEnabled()) "Hide Performance Monitor" else "Show Performance Monitor" }
      glyphName = { "speedometer" }
      keyCommand = KeyCommand(KeyEvent.KEYCODE_P)
      importance = DevMenuItemImportance.HIGH.value
    }

    action("remote-debug", remoteDebugAction) {
      isEnabled = {
        devSettings.isRemoteJSDebugEnabled
      }
      label = { if (isEnabled()) "Stop Remote Debugging" else "Debug Remote JS" }
      glyphName = { "remote-desktop" }
      importance = DevMenuItemImportance.LOW.value
    }

    if (devSettings is DevInternalSettings) {
      val fastRefreshAction = {
        devSettings.isHotModuleReplacementEnabled = !devSettings.isHotModuleReplacementEnabled
      }

      action("fast-refresh", fastRefreshAction) {
        isEnabled = { devSettings.isHotModuleReplacementEnabled }
        label = { if (isEnabled()) "Disable Fast Refresh" else "Enable Fast Refresh" }
        glyphName = { "run-fast" }
        importance = DevMenuItemImportance.LOW.value
      }
    }
  }

  override fun devMenuScreens(settings: DevMenuExtensionSettingsInterface): List<DevMenuScreen>? {
    return null
  }

  override fun devMenuDataSources(settings: DevMenuExtensionSettingsInterface): List<DevMenuDataSourceInterface>? {
    return null
  }

  /**
   * Requests for the permission that allows the app to draw overlays on other apps.
   * Such permission is required to enable performance monitor.
   */
  private fun requestOverlaysPermission() {
    val context = currentActivity ?: return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
      && !Settings.canDrawOverlays(context)) {
      val uri = Uri.parse("package:" + context.applicationContext.packageName)
      val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
      }
    }
  }
}
