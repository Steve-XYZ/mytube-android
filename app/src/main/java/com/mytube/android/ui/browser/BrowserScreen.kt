package com.mytube.android.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mytube.android.R
import com.mytube.android.ui.theme.BrandCoral
import com.mytube.android.ui.theme.BrandPink

private data class StartingPoint(
    val name: String,
    val capabilities: String,
    val url: String,
)

private val startingPoints = listOf(
    StartingPoint("YouTube", "Videos · Shorts · Live", "https://www.youtube.com"),
    StartingPoint("Instagram", "Reels · Posts", "https://www.instagram.com"),
    StartingPoint("TikTok", "Videos", "https://www.tiktok.com"),
    StartingPoint("X", "Video posts", "https://x.com"),
    StartingPoint("Vimeo", "Videos", "https://vimeo.com"),
    StartingPoint("Facebook", "Videos · Reels", "https://www.facebook.com"),
)

@Composable
fun BrowserRoute(
    viewModel: BrowserViewModel,
    session: BrowserSession,
    blockThirdPartyCookies: Boolean,
    saveBrowsingHistory: Boolean,
    onMessage: (String) -> Unit,
    onDownloadRequested: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeTab = uiState.activeTab

    SideEffect {
        session.blockThirdPartyCookies = blockThirdPartyCookies
        session.callbacks = BrowserSessionCallbacks(
            onPageStarted = viewModel::onPageStarted,
            onPageStateChanged = viewModel::onPageStateChanged,
            onPageFinished = { tabId, url, title ->
                viewModel.onPageFinished(
                    tabId = tabId,
                    url = url,
                    title = title,
                    saveHistory = saveBrowsingHistory,
                )
            },
            onPageError = viewModel::onPageError,
            onDownloadRequested = onDownloadRequested,
        )
    }

    LaunchedEffect(
        uiState.activeTabId,
        uiState.tabs.map(BrowserTab::id),
    ) {
        session.removeMissingTabs(uiState.tabs.mapTo(mutableSetOf(), BrowserTab::id))
        session.activate(uiState.activeTabId)
    }

    LaunchedEffect(activeTab.id, activeTab.url) {
        val currentUrl = activeTab.url
        if (currentUrl != null && !session.hasTab(activeTab.id)) {
            session.loadUrl(activeTab.id, currentUrl)
        }
    }

    uiState.navigationRequest?.let { request ->
        LaunchedEffect(request.id) {
            session.loadUrl(request.tabId, request.url)
            viewModel.consumeNavigationRequest(request.id)
        }
    }

    BrowserScreen(
        uiState = uiState,
        session = session,
        onAddressChanged = viewModel::updateAddress,
        onGo = {
            if (!viewModel.requestNavigation()) {
                onMessage("Enter a valid address or search.")
            }
        },
        onOpenStartingPoint = { viewModel.requestNavigation(it) },
        onAddTab = viewModel::addTab,
        onSelectTab = viewModel::selectTab,
        onCloseTab = { tabId ->
            session.destroyTab(tabId)
            viewModel.closeTab(tabId)
        },
        onBack = { session.goBack(activeTab.id) },
        onForward = { session.goForward(activeTab.id) },
        onReload = {
            if (activeTab.isLoading) {
                session.stopLoading(activeTab.id)
            } else {
                session.reload(activeTab.id)
            }
        },
        onToggleBookmark = viewModel::toggleBookmark,
    )
}

@Composable
fun BrowserScreen(
    uiState: BrowserUiState,
    session: BrowserSession,
    onAddressChanged: (String) -> Unit,
    onGo: () -> Unit,
    onOpenStartingPoint: (String) -> Unit,
    onAddTab: () -> Unit,
    onSelectTab: (Long) -> Unit,
    onCloseTab: (Long) -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onToggleBookmark: () -> Unit,
) {
    val activeTab = uiState.activeTab
    val isBookmarked = activeTab.url in uiState.bookmarkedUrls

    Column(modifier = Modifier.fillMaxSize()) {
        BrowserToolbar(
            address = uiState.address,
            activeTab = activeTab,
            isBookmarked = isBookmarked,
            onAddressChanged = onAddressChanged,
            onGo = onGo,
            onBack = onBack,
            onForward = onForward,
            onReload = onReload,
            onToggleBookmark = onToggleBookmark,
        )
        TabStrip(
            tabs = uiState.tabs,
            activeTabId = uiState.activeTabId,
            canAddTab = uiState.tabs.size < BrowserViewModel.MaxTabs,
            onAddTab = onAddTab,
            onSelectTab = onSelectTab,
            onCloseTab = onCloseTab,
        )
        if (activeTab.isLoading) {
            LinearProgressIndicator(
                progress = { activeTab.progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            if (activeTab.url == null) {
                NewTabPage(
                    address = uiState.address,
                    onAddressChanged = onAddressChanged,
                    onGo = onGo,
                    onOpenStartingPoint = onOpenStartingPoint,
                )
            } else {
                key(activeTab.id) {
                    AndroidView(
                        factory = { session.obtainWebView(activeTab.id) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            activeTab.errorMessage?.let { message ->
                PageErrorCard(
                    message = message,
                    onRetry = onReload,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )
            }
        }
    }
}

@Composable
private fun BrowserToolbar(
    address: String,
    activeTab: BrowserTab,
    isBookmarked: Boolean,
    onAddressChanged: (String) -> Unit,
    onGo: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onToggleBookmark: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrowserIconButton(
            enabled = activeTab.canGoBack,
            onClick = onBack,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        BrowserIconButton(
            enabled = activeTab.canGoForward,
            onClick = onForward,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
        }
        BrowserIconButton(onClick = onReload) {
            Icon(
                imageVector = if (activeTab.isLoading) Icons.Filled.Close else Icons.Filled.Refresh,
                contentDescription = if (activeTab.isLoading) "Stop" else "Reload",
            )
        }
        OutlinedTextField(
            value = address,
            onValueChange = onAddressChanged,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            placeholder = { Text(stringResource(R.string.browser_address_hint)) },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = ImeAction.Go,
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onGo = { onGo() },
            ),
        )
        BrowserIconButton(
            enabled = activeTab.url != null,
            onClick = onToggleBookmark,
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
                tint = if (isBookmarked) BrandPink else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer {
                    alpha = if (isBookmarked) 1f else 0.55f
                },
            )
        }
    }
}

@Composable
private fun BrowserIconButton(
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    IconButton(
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        content = content,
    )
}

@Composable
private fun TabStrip(
    tabs: List<BrowserTab>,
    activeTabId: Long,
    canAddTab: Boolean,
    onAddTab: () -> Unit,
    onSelectTab: (Long) -> Unit,
    onCloseTab: (Long) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .horizontalScroll(rememberScrollState())
            .padding(start = 8.dp, end = 4.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            val active = tab.id == activeTabId
            Card(
                modifier = Modifier
                    .width(150.dp)
                    .clickable { onSelectTab(tab.id) },
                colors = CardDefaults.cardColors(
                    containerColor = if (active) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
            ) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, end = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = tab.title,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    IconButton(
                        onClick = { onCloseTab(tab.id) },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close ${tab.title}",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
        IconButton(
            enabled = canAddTab,
            onClick = onAddTab,
        ) {
            Icon(Icons.Filled.Add, contentDescription = "New tab")
        }
    }
}

@Composable
private fun NewTabPage(
    address: String,
    onAddressChanged: (String) -> Unit,
    onGo: () -> Unit,
    onOpenStartingPoint: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrandWordmark()
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.browser_tagline),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.browser_intro),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = address,
                onValueChange = onAddressChanged,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.browser_address_hint)) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onGo = { onGo() },
                ),
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

        Spacer(modifier = Modifier.height(32.dp))
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
            startingPoints.forEachIndexed { index, point ->
                StartingPointCard(
                    point = point,
                    accent = if (index % 2 == 0) BrandCoral else BrandPink,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenStartingPoint(point.url) },
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
        fontSize = 38.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-1.2).sp,
    )
}

@Composable
private fun StartingPointCard(
    point: StartingPoint,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f))
                    .padding(9.dp),
            ) {
                Text(
                    text = point.name.take(1),
                    color = accent,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = point.name,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = point.capabilities,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun PageErrorCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Unable to open this page",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Try again")
            }
        }
    }
}
