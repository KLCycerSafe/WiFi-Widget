package com.w2sv.wifiwidget.ui.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ShareCompat
import com.w2sv.androidutils.extensions.openUrl
import com.w2sv.androidutils.extensions.playStoreUrl
import com.w2sv.androidutils.extensions.showToast
import com.w2sv.wifiwidget.BuildConfig
import com.w2sv.wifiwidget.R
import com.w2sv.wifiwidget.ui.JostText
import com.w2sv.wifiwidget.ui.WifiWidgetTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun NavigationDrawerPrev() {
    WifiWidgetTheme {
        NavigationDrawer {
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationDrawer(modifier: Modifier = Modifier, content: @Composable (DrawerState) -> Unit) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val closeDrawer: () -> Unit = {
        scope.launch {
            drawerState.close()
        }
    }

    BackHandler(drawerState.isOpen, closeDrawer)

    ModalNavigationDrawer(
        modifier = modifier,
        drawerContent = {
            NavigationDrawerContent(closeDrawer)
        },
        drawerState = drawerState
    ) {
        content(drawerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavigationDrawerContent(closeDrawer: () -> Unit) {
    ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.secondary) {
        Column(
            modifier = Modifier
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.padding(vertical = 32.dp)) {
                Image(
                    painterResource(id = R.drawable.logo_foreground),
                    null,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                VersionText(Modifier.padding(top = 26.dp))
            }
            Divider(
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 12.dp)
            )
            remember {
                NavigationDrawerItemProperties.get()
            }
                .forEach {
                    NavigationDrawerItem(properties = it, closeDrawer = closeDrawer)
                }
        }
    }
}

@Composable
fun VersionText(modifier: Modifier = Modifier) {
    JostText(
        text = "Version: ${BuildConfig.VERSION_NAME}",
        modifier = modifier,
        color = MaterialTheme.colorScheme.onPrimary
    )
}

private data class NavigationDrawerItemProperties(
    @DrawableRes val icon: Int,
    @StringRes val label: Int,
    val callback: (Context) -> Unit
) {
    companion object {
        fun get(): List<NavigationDrawerItemProperties> =
            listOf(
                NavigationDrawerItemProperties(R.drawable.ic_share_24, R.string.share) {
                    ShareCompat.IntentBuilder(it)
                        .setType("text/plain")
                        .setText("Check out WiFi Widget!\n${it.playStoreUrl}")
                        .setChooserTitle("Choose an app")
                        .startChooser()
                },
                NavigationDrawerItemProperties(R.drawable.ic_star_rate_24, R.string.rate) {
                    try {
                        it.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(it.playStoreUrl)
                            )
                                .setPackage("com.android.vending")
                        )
                    } catch (e: ActivityNotFoundException) {
                        it.showToast("You're not signed into the Play Store \uD83E\uDD14")
                    }
                },
                NavigationDrawerItemProperties(R.drawable.ic_github_24, R.string.code) {
                    it
                        .openUrl("https://github.com/w2sv/WiFi-Widget")
                }
            )
    }
}

@Composable
private fun NavigationDrawerItem(
    properties: NavigationDrawerItemProperties,
    closeDrawer: () -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                properties.callback(context)
                closeDrawer()
            }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier
                .size(size = dimensionResource(id = R.dimen.size_icon)),
            painter = painterResource(id = properties.icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

        JostText(
            text = stringResource(id = properties.label),
            modifier = Modifier.padding(start = 16.dp),
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}