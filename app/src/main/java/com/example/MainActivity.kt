package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.PortalSaveRepository
import com.example.game.engine.GameEngine
import com.example.game.ui.AuthScreen
import com.example.game.ui.CloudSavesScreen
import com.example.game.ui.GameScreen
import com.example.game.ui.GameViewModel
import com.example.game.ui.signOutUser
import com.example.ui.theme.PortalTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

enum class AppScreen {
    GAME, CLOUD_SAVES
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PortalTheme {
                AppContent(modifier = Modifier.fillMaxSize().safeDrawingPadding())
            }
        }
    }
}

@Composable
fun AppContent(modifier: Modifier = Modifier) {
    var currentUser by remember {
        mutableStateOf(
            try {
                Firebase.auth.currentUser
            } catch (e: Exception) {
                null
            }
        )
    }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        try {
            Firebase.auth.addAuthStateListener(listener)
        } catch (e: Exception) {
            // Firebase initialized in background
        }
        onDispose {
            try {
                Firebase.auth.removeAuthStateListener(listener)
            } catch (e: Exception) {
            }
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser },
            modifier = modifier
        )
    } else {
        val uid = currentUser!!.uid
        val userEmail = currentUser!!.email ?: "Test Subject"
        key(uid) {
            MainGameContainer(
                currentUserId = uid,
                userDisplayName = userEmail,
                modifier = modifier
            )
        }
    }
}

@Composable
fun MainGameContainer(
    currentUserId: String,
    userDisplayName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    // Resolve custom Firestore database ID dynamically
    val gameViewModel: GameViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                val databaseId = try {
                    val resId = app.resources.getIdentifier("firestore_database_id", "string", app.packageName)
                    if (resId != 0) app.getString(resId) else "ai-studio-android-portalmo-83e27219-dc0b-40ab-b6ae-d0046ff69b96"
                } catch (e: Exception) {
                    "ai-studio-android-portalmo-83e27219-dc0b-40ab-b6ae-d0046ff69b96"
                }
                val db = FirebaseFirestore.getInstance(databaseId)
                GameViewModel(PortalSaveRepository(db), currentUserId)
            }
        }
    )

    val gameEngine = remember { GameEngine(0) }
    var currentScreen by remember { mutableStateOf(AppScreen.GAME) }

    when (currentScreen) {
        AppScreen.GAME -> {
            GameScreen(
                gameEngine = gameEngine,
                onSaveToCloud = { save ->
                    gameViewModel.saveGame(save)
                },
                onRecordCompletion = { chamberId, time, portals ->
                    gameViewModel.recordCompletion(chamberId, time, portals)
                },
                onOpenCloudManager = {
                    currentScreen = AppScreen.CLOUD_SAVES
                },
                modifier = modifier
            )
        }
        AppScreen.CLOUD_SAVES -> {
            BackHandler {
                currentScreen = AppScreen.GAME
            }
            CloudSavesScreen(
                savesFlow = gameViewModel.saves,
                recordsFlow = gameViewModel.records,
                userDisplayName = userDisplayName,
                onLoadSave = { save ->
                    gameEngine.restoreFromSave(save)
                    currentScreen = AppScreen.GAME
                },
                onDeleteSave = { saveId ->
                    gameViewModel.deleteSave(saveId)
                },
                onBack = {
                    currentScreen = AppScreen.GAME
                },
                onSignOut = {
                    signOutUser(context, credentialManager, coroutineScope) {
                        currentScreen = AppScreen.GAME
                    }
                },
                modifier = modifier
            )
        }
    }
}
