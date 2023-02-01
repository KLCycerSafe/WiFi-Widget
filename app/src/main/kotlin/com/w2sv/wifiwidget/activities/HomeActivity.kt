@file:Suppress("DEPRECATION")

package com.w2sv.wifiwidget.activities

import android.animation.ObjectAnimator
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.SavedStateHandle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.w2sv.androidutils.SelfManagingLocalBroadcastReceiver
import com.w2sv.androidutils.extensions.showToast
import com.w2sv.wifiwidget.preferences.GlobalFlags
import com.w2sv.wifiwidget.preferences.WidgetProperties
import com.w2sv.wifiwidget.ui.home.HomeScreen
import com.w2sv.wifiwidget.ui.WifiWidgetTheme
import com.w2sv.wifiwidget.utils.getIntExtraOrNull
import com.w2sv.wifiwidget.utils.getMutableStateMap
import com.w2sv.wifiwidget.widget.WifiWidgetProvider
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import slimber.log.i
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : AppActivity() {

    companion object {
        const val EXTRA_OPEN_PROPERTIES_CONFIGURATION_DIALOG_ON_START =
            "com.w2sv.wifiwidget.extra.OPEN_PROPERTIES_CONFIGURATION_DIALOG_ON_START"
    }

    @HiltViewModel
    class ViewModel @Inject constructor(
        private val widgetProperties: WidgetProperties,
        private val globalFlags: GlobalFlags,
        savedStateHandle: SavedStateHandle,
        @ApplicationContext context: Context
    ) : androidx.lifecycle.ViewModel() {

        val openPropertiesConfigurationDialogOnStart =
            savedStateHandle.contains(EXTRA_OPEN_PROPERTIES_CONFIGURATION_DIALOG_ON_START)
                .also { i { "openPropertiesConfigurationDialog: $it" } }

        /**
         * Widget Creation Listening
         */

        fun onWidgetOptionsUpdated(widgetId: Int, context: Context) {
            if (widgetIds.add(widgetId)) {
                i { "New widgetId: $widgetId" }
                context.showToast("Pinned Widget")
            }
        }

        private val widgetIds: MutableSet<Int> =
            WifiWidgetProvider.getWidgetIds(context).toMutableSet()

        /**
         * widgetPropertyStates
         */

        val widgetPropertyStates: SnapshotStateMap<String, Boolean> by lazy {
            widgetProperties.getMutableStateMap()
        }

        fun syncWidgetPropertyStates() {
            widgetProperties.putAll(widgetPropertyStates)
            _propertyStatesDissimilar.value = false
        }

        fun resetWidgetPropertyStates() {
            widgetPropertyStates.putAll(widgetProperties)
            _propertyStatesDissimilar.value = false
        }

        private val _propertyStatesDissimilar = MutableStateFlow(false)
        val propertyStatesDissimilar = _propertyStatesDissimilar.asStateFlow()

        /**
         * @return Boolean indicating whether change has been endorsed
         */
        fun onChangePropertyState(property: String, value: Boolean): Boolean {
            // veto change if leading to all properties being unchecked
            if (!value && widgetPropertyStates.values.count { true } == 1)
                return false

            // implement change and update _propertyStatesDissimilar
            widgetPropertyStates[property] = value
            _propertyStatesDissimilar.value =
                widgetPropertyStates.any { (k, v) ->  // TODO: optimize
                    v != widgetProperties.getValue(k)
                }
            return true
        }

        /**
         * lap := Location Access Permission
         */

        val lapDialogAnswered: Boolean by globalFlags::locationPermissionDialogAnswered

        fun onLapDialogAnswered() {
            globalFlags.locationPermissionDialogAnswered = true
        }
    }

    private val viewModel by viewModels<ViewModel>()

    @Inject
    lateinit var globalFlags: GlobalFlags

    @Inject
    lateinit var widgetProperties: WidgetProperties

    override val lifecycleObservers: List<LifecycleObserver>
        get() = listOf(
            globalFlags,
            widgetProperties,
            lapRequestLauncher,
            WifiWidgetOptionsChangedReceiver(
                LocalBroadcastManager.getInstance(this)
            ) { intent ->
                intent?.getIntExtraOrNull(WifiWidgetProvider.EXTRA_WIDGET_ID, -1)?.let { widgetId ->
                    viewModel.onWidgetOptionsUpdated(widgetId, this)
                }
            }
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setOnExitAnimationListener(
            SwipeUpAnimation()
        )

        super.onCreate(savedInstanceState)

        setContent {
            WifiWidgetTheme {
                HomeScreen()
            }
        }
    }

    fun requestWidgetPin() {
        getSystemService(AppWidgetManager::class.java).let {
            if (it.isRequestPinAppWidgetSupported) {
                it.requestPinAppWidget(
                    ComponentName(
                        this,
                        WifiWidgetProvider::class.java
                    ),
                    null,
                    null
                )
            } else
                showToast("Widget pinning not supported by your device launcher")
        }
    }

    class WifiWidgetOptionsChangedReceiver(
        broadcastManager: LocalBroadcastManager,
        private val callback: (Intent?) -> Unit
    ) : SelfManagingLocalBroadcastReceiver(
        broadcastManager,
        IntentFilter(WifiWidgetProvider.ACTION_WIDGET_OPTIONS_CHANGED)
    ) {
        override fun onReceive(context: Context?, intent: Intent?) {
            callback(intent)
        }
    }

    val lapRequestLauncher by lazy {
        LocationAccessPermissionHandler(this) { permissionGrantedMap ->
            if (permissionGrantedMap.containsValue(true)) {
                with(viewModel) {
                    widgetPropertyStates[widgetProperties::SSID.name] = true
                    syncWidgetPropertyStates()
                }
            }

            requestWidgetPin()
        }
    }
}

private class SwipeUpAnimation : SplashScreen.OnExitAnimationListener {
    override fun onSplashScreenExit(splashScreenViewProvider: SplashScreenViewProvider) {
        ObjectAnimator.ofFloat(
            splashScreenViewProvider.view,
            View.TRANSLATION_Y,
            0f,
            -splashScreenViewProvider.view.height.toFloat()
        )
            .apply {
                interpolator = AnticipateInterpolator()
                duration = 400L
                doOnEnd { splashScreenViewProvider.remove() }
            }
            .start()
    }
}