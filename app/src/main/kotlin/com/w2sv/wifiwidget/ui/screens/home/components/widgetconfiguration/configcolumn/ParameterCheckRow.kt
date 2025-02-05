package com.w2sv.wifiwidget.ui.screens.home.components.widgetconfiguration.configcolumn

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.TextUnit
import com.w2sv.wifiwidget.R
import com.w2sv.wifiwidget.ui.components.JostText
import com.w2sv.wifiwidget.ui.components.bulletPointText

@Stable
open class ParameterCheckRowData<T>(
    val type: T,
    @StringRes val labelRes: Int,
    val isChecked: () -> Boolean,
    val onCheckedChange: (Boolean) -> Unit,
    val allowCheckChange: (Boolean) -> Boolean = { true }
) {
    constructor(
        type: T,
        @StringRes labelRes: Int,
        isCheckedMap: MutableMap<T, Boolean>,
        allowCheckChange: (Boolean) -> Boolean = { true }
    ) : this(
        type = type,
        labelRes = labelRes,
        isChecked = { isCheckedMap.getValue(type) },
        onCheckedChange = { isCheckedMap[type] = it },
        allowCheckChange = allowCheckChange
    )
}

@Composable
internal fun <T> ParameterCheckRow(
    data: ParameterCheckRowData<T>,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    trailingIconButton: (@Composable () -> Unit)? = null
) {
    val checkBoxCD = stringResource(id = R.string.set_unset).format(data.labelRes)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
    ) {
        JostText(
            text = bulletPointText(stringResource(id = data.labelRes)),
            fontSize = fontSize,
            modifier = Modifier.weight(1.0f, true)
        )
        Checkbox(
            checked = data.isChecked(),
            onCheckedChange = {
                if (data.allowCheckChange(it)) {
                    data.onCheckedChange(it)
                }
            },
            modifier = Modifier.semantics {
                contentDescription = checkBoxCD
            }
        )
        trailingIconButton?.invoke()
    }
}