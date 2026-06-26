package com.example.project.ui.notifications

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.project.R
import com.example.project.ui.components.DetailTopBar
import com.example.project.ui.components.EmptyState

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { DetailTopBar(title = stringResource(R.string.notifications_title), onBack = onBack) }
    ) { padding ->
        EmptyState(
            icon = Icons.Outlined.NotificationsNone,
            message = stringResource(R.string.notifications_empty),
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }
}
