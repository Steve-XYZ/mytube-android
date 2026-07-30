package com.mytube.android.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mytube.android.R
import com.mytube.android.ui.theme.BrandCoral
import com.mytube.android.ui.theme.BrandPink

private val startingPoints = listOf(
    "YouTube" to "Videos · Shorts · Live",
    "Instagram" to "Reels · Posts",
    "TikTok" to "Videos",
    "X" to "Video posts",
    "Vimeo" to "Videos",
    "Facebook" to "Videos · Reels",
)

@Composable
fun BrowserRoute(viewModel: BrowserViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BrowserScreen(
        uiState = uiState,
        onAddressChanged = viewModel::updateAddress,
        onGo = viewModel::prepareNavigation,
    )
}

@Composable
fun BrowserScreen(
    uiState: BrowserUiState,
    onAddressChanged: (String) -> Unit,
    onGo: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(18.dp))
        BrandWordmark()
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.browser_tagline),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.browser_intro),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = uiState.address,
                onValueChange = onAddressChanged,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.browser_address_hint)) },
                singleLine = true,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = onGo,
                modifier = Modifier.height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandCoral,
                    contentColor = Color.White,
                ),
            ) {
                Text(stringResource(R.string.browser_go))
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.content_description_go),
                )
            }
        }

        uiState.preparedAddress?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.browser_phase_two_notice),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = stringResource(R.string.browser_starting_points),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(14.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            startingPoints.forEachIndexed { index, (name, capabilities) ->
                StartingPointCard(
                    name = name,
                    capabilities = capabilities,
                    accent = if (index % 2 == 0) BrandCoral else BrandPink,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BrandWordmark() {
    val wordmark = buildAnnotatedString {
        append("My")
        withStyle(
            SpanStyle(
                brush = Brush.horizontalGradient(listOf(BrandCoral, BrandPink)),
                fontWeight = FontWeight.Black,
            ),
        ) {
            append("Tube")
        }
    }

    Text(
        text = wordmark,
        fontSize = 42.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-1.2).sp,
    )
}

@Composable
private fun StartingPointCard(
    name: String,
    capabilities: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f))
                    .padding(9.dp),
            ) {
                Text(
                    text = name.take(1),
                    color = accent,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = name,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = capabilities,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
