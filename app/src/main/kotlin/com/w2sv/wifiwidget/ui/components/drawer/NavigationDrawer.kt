package com.w2sv.wifiwidget.ui.components.drawer

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.w2sv.data.model.Theme
import com.w2sv.wifiwidget.ui.components.defaultSpringSpec
import com.w2sv.wifiwidget.ui.viewmodels.InAppThemeViewModel
import kotlinx.coroutines.launch

suspend fun DrawerState.closeDrawer() {
    animateTo(DrawerValue.Closed, defaultSpringSpec)
}

suspend fun DrawerState.openDrawer() {
    animateTo(DrawerValue.Open, defaultSpringSpec)
}

@Composable
fun NavigationDrawer(
    state: DrawerState,
    modifier: Modifier = Modifier,
    inAppThemeVM: InAppThemeViewModel = viewModel(),
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        modifier = modifier,
        drawerContent = {
            NavigationDrawerSheet {
                NavigationDrawerSheetContent(
                    closeDrawer = {
                        scope.launch {
                            state.closeDrawer()
                        }
                    },
                    appearanceSection = { modifier ->
                        AppearanceSection(
                            selectedTheme = inAppThemeVM.inAppTheme.collectAsState(Theme.SystemDefault).value,
                            onThemeSelected = { inAppThemeVM.saveInAppTheme(it) },
                            useDynamicTheme = inAppThemeVM.useDynamicTheme.collectAsState(false).value,
                            onToggleDynamicTheme = { inAppThemeVM.saveUseDynamicTheme(it) },
                            modifier = modifier
                        )
                    }
                )
            }
        },
        drawerState = state
    ) {
        content()
    }
}