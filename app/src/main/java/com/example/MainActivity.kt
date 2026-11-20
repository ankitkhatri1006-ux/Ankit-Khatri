package com.example

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.auth.attemptAutoSignIn
import com.example.auth.signOutUser
import com.example.data.PlayerProfile
import com.example.data.YodhaRepository
import com.example.ui.components.IntroCinemaOverlay
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CharacterStudioScreen
import com.example.ui.screens.ForumScreen
import com.example.ui.screens.GameArenaScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.LobbyScreen
import com.example.ui.screens.SeasonAndAntiCheatScreen
import com.example.ui.theme.BharatTeal
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.TacticalSlate
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.YodhaRoyaleTheme
import com.example.viewmodel.UiState
import com.example.viewmodel.YodhaViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private sealed class YodhaTab(val route: String, val label: String, val icon: ImageVector) {
    data object Lobby : YodhaTab("lobby", "Lobby", Icons.Filled.SportsEsports)
    data object Arsenal : YodhaTab("arsenal", "Arsenal", Icons.Filled.Backpack)
    data object Studio : YodhaTab("studio", "Desi", Icons.Filled.Face)
    data object Season : YodhaTab("season", "Kavach", Icons.Filled.Security)
    data object Forum : YodhaTab("forum", "Forum", Icons.Filled.Forum)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        enableEdgeToEdge()
        setContent {
            YodhaRoyaleTheme {
                YodhaRoot()
            }
        }
    }
}

@Composable
private fun YodhaRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var checkedAutoSignIn by remember { mutableStateOf(currentUser != null) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        if (currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                    checkedAutoSignIn = true
                },
                onUnauthenticated = {
                    checkedAutoSignIn = true
                },
                scope = scope
            )
        }
    }

    val user = currentUser
    if (!checkedAutoSignIn) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = SaffronGold)
        }
    } else if (user == null) {
        AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
    } else {
        val uid = user.uid
        val displayName = user.displayName ?: "YodhaWarrior"
        key(uid) {
            val viewModel: YodhaViewModel = viewModel(
                key = uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val dbId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(dbId)
                        YodhaViewModel(YodhaRepository(db), uid, displayName)
                    }
                }
            )
            YodhaAuthenticatedShell(
                viewModel = viewModel,
                onSignOut = {
                    signOutUser(credentialManager, scope) {
                        currentUser = null
                    }
                }
            )
        }
    }
}

@Composable
private fun YodhaAuthenticatedShell(
    viewModel: YodhaViewModel,
    onSignOut: () -> Unit
) {
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val matchRoomsState by viewModel.matchRoomsState.collectAsStateWithLifecycle()
    val forumPostsState by viewModel.forumPostsState.collectAsStateWithLifecycle()
    val postCommentsState by viewModel.postCommentsState.collectAsStateWithLifecycle()
    val antiCheatLogsState by viewModel.antiCheatLogsState.collectAsStateWithLifecycle()
    val selectedPostId by viewModel.selectedPostIdForComments.collectAsStateWithLifecycle()
    val matchmaking by viewModel.matchmaking.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceChat.collectAsStateWithLifecycle()
    val kavachState by viewModel.kavachState.collectAsStateWithLifecycle()
    val musicVol by viewModel.musicVolume.collectAsStateWithLifecycle()
    val sfxVol by viewModel.sfxVolume.collectAsStateWithLifecycle()
    val showIntro by viewModel.showIntroCinema.collectAsStateWithLifecycle()
    val inMatch by viewModel.inActiveMatch.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissBanner()
        }
    }

    val activeProfile = (profileState as? UiState.Success)?.data
        ?: PlayerProfile(userId = viewModel.currentUserId)

    if (showIntro) {
        IntroCinemaOverlay(onFinish = { viewModel.setShowIntroCinema(false) })
        return
    }

    if (inMatch) {
        GameArenaScreen(
            profile = activeProfile,
            mapId = matchmaking.selectedMapId,
            modeId = matchmaking.selectedModeId,
            voiceState = voiceState,
            onVoicePermissionResult = viewModel::setVoicePermissionGranted,
            onToggleMic = viewModel::toggleVoiceMic,
            onToggleChannel = viewModel::toggleVoiceChannel,
            onExitMatch = viewModel::exitArenaAndRecordMatch
        )
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: YodhaTab.Lobby.route
    val tabs = remember {
        listOf(YodhaTab.Lobby, YodhaTab.Arsenal, YodhaTab.Studio, YodhaTab.Season, YodhaTab.Forum)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        val useRail = maxWidth >= 640.dp

        Scaffold(
            containerColor = ObsidianBg,
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = TacticalSlate,
                        contentColor = SaffronGold
                    )
                }
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(containerColor = TacticalSlate) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(YodhaTab.Lobby.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ObsidianBg,
                                    selectedTextColor = SaffronGold,
                                    indicatorColor = SaffronGold,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = TacticalSlate,
                        modifier = Modifier.fillMaxHeight().statusBarsPadding()
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(YodhaTab.Lobby.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ObsidianBg,
                                    selectedTextColor = SaffronGold,
                                    indicatorColor = SaffronGold,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = YodhaTab.Lobby.route,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .statusBarsPadding()
                ) {
                    composable(YodhaTab.Lobby.route) {
                        LobbyScreen(
                            profile = activeProfile,
                            currentUserId = viewModel.currentUserId,
                            matchmaking = matchmaking,
                            matchRoomsState = matchRoomsState,
                            voiceState = voiceState,
                            onReplayIntro = { viewModel.setShowIntroCinema(true) },
                            onSignOut = onSignOut,
                            onSelectMap = viewModel::selectMap,
                            onSelectMode = viewModel::selectMode,
                            onStartMatchmaking = viewModel::startBalancedMatchmaking,
                            onCancelMatchmaking = viewModel::cancelMatchmaking,
                            onQuickDeploy = viewModel::launchArenaDirectly,
                            onCreateRoom = viewModel::createSquadRoom,
                            onJoinOrReadyRoom = viewModel::joinOrToggleReadyRoom,
                            onLeaveRoom = viewModel::leaveRoom,
                            onLaunchRoom = viewModel::launchRoomMatch,
                            onVoicePermissionResult = viewModel::setVoicePermissionGranted,
                            onToggleMic = viewModel::toggleVoiceMic,
                            onToggleChannel = viewModel::toggleVoiceChannel
                        )
                    }
                    composable(YodhaTab.Arsenal.route) {
                        InventoryScreen(
                            profile = activeProfile,
                            onEquipWeapon = viewModel::equipWeapon,
                            onToggleAttachment = viewModel::toggleAttachment,
                            onToggleBackpackItem = viewModel::toggleBackpackItem
                        )
                    }
                    composable(YodhaTab.Studio.route) {
                        CharacterStudioScreen(
                            profile = activeProfile,
                            onUpdateCharacter = viewModel::updateCharacterCustomization
                        )
                    }
                    composable(YodhaTab.Season.route) {
                        SeasonAndAntiCheatScreen(
                            profile = activeProfile,
                            kavachState = kavachState,
                            antiCheatLogsState = antiCheatLogsState,
                            musicVolume = musicVol,
                            sfxVolume = sfxVol,
                            onSelectGraphics = viewModel::setGraphicsPreset,
                            onAudioChanged = viewModel::setAudioVolumes,
                            onRunKavachScan = viewModel::runKavachScan
                        )
                    }
                    composable(YodhaTab.Forum.route) {
                        ForumScreen(
                            currentUserId = viewModel.currentUserId,
                            postsState = forumPostsState,
                            selectedPostId = selectedPostId,
                            commentsState = postCommentsState,
                            onCreatePost = viewModel::createForumPost,
                            onToggleLike = viewModel::togglePostLike,
                            onOpenComments = viewModel::openPostComments,
                            onSubmitComment = viewModel::submitForumComment
                        )
                    }
                }
            }
        }
    }
}
