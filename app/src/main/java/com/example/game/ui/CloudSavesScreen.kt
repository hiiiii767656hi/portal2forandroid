package com.example.game.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ChamberScore
import com.example.data.model.GameSave
import com.example.ui.theme.ApertureBorder
import com.example.ui.theme.ApertureDarkBackground
import com.example.ui.theme.ApertureSurface
import com.example.ui.theme.ApertureSurfaceVariant
import com.example.ui.theme.ApertureTextMuted
import com.example.ui.theme.ApertureTextPrimary
import com.example.ui.theme.ApertureTextSecondary
import com.example.ui.theme.ApertureYellow
import com.example.ui.theme.LaserRed
import com.example.ui.theme.PortalBlue
import com.example.ui.theme.PortalOrange
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSavesScreen(
    savesFlow: StateFlow<List<GameSave>>,
    recordsFlow: StateFlow<List<ChamberScore>>,
    userDisplayName: String,
    onLoadSave: (GameSave) -> Unit,
    onDeleteSave: (String) -> Unit,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val saves by savesFlow.collectAsState()
    val records by recordsFlow.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "APERTURE CLOUD ARCHIVE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ApertureTextPrimary
                        )
                        Text(
                            text = "Subject: $userDisplayName",
                            style = MaterialTheme.typography.labelSmall,
                            color = PortalBlue
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("cloud_back_button")) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = ApertureTextPrimary
                        )
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("sign_out_button")
                    ) {
                        Text("SIGN OUT", style = MaterialTheme.typography.labelSmall)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ApertureDarkBackground
                )
            )
        },
        containerColor = ApertureDarkBackground,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Hero Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = ApertureSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ApertureBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(110.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_aperture_hero),
                        contentDescription = "Aperture Science Facility",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(14.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Column {
                            Text(
                                text = "CLOUD SYNCHRONIZATION",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Quantum state saved across multiple timeline test chambers.",
                                style = MaterialTheme.typography.labelSmall,
                                color = ApertureYellow
                            )
                        }
                    }
                }
            }

            // Tabs: Saved States vs Speedrun Records
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = ApertureDarkBackground,
                contentColor = PortalBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = if (selectedTab == 0) PortalBlue else PortalOrange
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "SAVED GAMES (${saves.size})",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "SPEEDRUN RECORDS (${records.size})",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }

            // Tab Content
            if (selectedTab == 0) {
                if (saves.isEmpty()) {
                    EmptyCloudState(
                        message = "No cloud saves yet. Hit the Cloud Save button in any chamber to archive your checkpoint."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(saves, key = { it.id }) { save ->
                            SaveItemCard(
                                save = save,
                                onLoad = { onLoadSave(save) },
                                onDelete = { onDeleteSave(save.id) }
                            )
                        }
                    }
                }
            } else {
                if (records.isEmpty()) {
                    EmptyCloudState(
                        message = "No speedrun records logged. Complete test chambers to submit your fastest run times and least portal counts."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(records, key = { it.chamberId }) { score ->
                            RecordItemCard(score = score)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SaveItemCard(
    save: GameSave,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ApertureSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ApertureBorder),
        modifier = Modifier.fillMaxWidth().testTag("save_card_${save.chamberId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = save.chamberName.ifBlank { save.chamberId.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    color = ApertureTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Timer: ${String.format("%.1f", save.elapsedSeconds)}s  •  Portals: ${save.portalsFired}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ApertureTextSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Portals: ${if (save.bluePortalActive) "Blue ACTIVE" else "Blue OFF"} | ${if (save.orangePortalActive) "Orange ACTIVE" else "Orange OFF"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PortalBlue
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onDelete, modifier = Modifier.size(38.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Save",
                        tint = LaserRed
                    )
                }

                Button(
                    onClick = onLoad,
                    colors = ButtonDefaults.buttonColors(containerColor = PortalBlue),
                    modifier = Modifier.testTag("load_save_${save.chamberId}")
                ) {
                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = "Load")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RESUME")
                }
            }
        }
    }
}

@Composable
fun RecordItemCard(score: ChamberScore) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ApertureSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ApertureBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(PortalOrange.copy(alpha = 0.2f), CircleShape)
                        .border(1.5.dp, PortalOrange, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Best Record",
                        tint = ApertureYellow,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = score.chamberId.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = ApertureTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Least portals used: ${score.minPortals}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ApertureTextSecondary
                    )
                }
            }

            Text(
                text = "${String.format("%.2f", score.bestTimeSeconds)}s",
                style = MaterialTheme.typography.headlineMedium,
                color = ApertureYellow,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EmptyCloudState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = "Cloud empty",
                tint = ApertureTextMuted,
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = ApertureTextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
