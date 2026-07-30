package com.mytube.android.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mytube.android.R
import com.mytube.android.ui.theme.ThemeMode

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    blockThirdPartyCookies: Boolean,
    saveBrowsingHistory: Boolean,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onBlockThirdPartyCookiesChanged: (Boolean) -> Unit,
    onSaveBrowsingHistoryChanged: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = stringResource(R.string.settings_appearance),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                ThemeOption(
                    label = stringResource(R.string.theme_system),
                    selected = themeMode == ThemeMode.System,
                    onClick = { onThemeModeSelected(ThemeMode.System) },
                )
                ThemeOption(
                    label = stringResource(R.string.theme_light),
                    selected = themeMode == ThemeMode.Light,
                    onClick = { onThemeModeSelected(ThemeMode.Light) },
                )
                ThemeOption(
                    label = stringResource(R.string.theme_dark),
                    selected = themeMode == ThemeMode.Dark,
                    onClick = { onThemeModeSelected(ThemeMode.Dark) },
                )
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                SettingSwitch(
                    title = stringResource(R.string.settings_block_third_party_cookies),
                    summary = stringResource(
                        R.string.settings_block_third_party_cookies_summary,
                    ),
                    checked = blockThirdPartyCookies,
                    onCheckedChange = onBlockThirdPartyCookiesChanged,
                )
                SettingSwitch(
                    title = stringResource(R.string.settings_save_history),
                    summary = stringResource(R.string.settings_save_history_summary),
                    checked = saveBrowsingHistory,
                    onCheckedChange = onSaveBrowsingHistoryChanged,
                )
            }
        }
    }
}

@Composable
private fun ThemeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
        Text(text = label)
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = summary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
        )
    }
}
