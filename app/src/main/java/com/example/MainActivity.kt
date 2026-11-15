package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.catalog.GameCatalog
import com.example.ui.AppRoute
import com.example.ui.ToyboxViewModel
import com.example.ui.components.TutorialOverlayDialog
import com.example.ui.screens.*
import com.example.ui.theme.ToyboxNightmareTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToyboxNightmareTheme {
                ToyboxRootApp()
            }
        }
    }
}

@Composable
fun ToyboxRootApp(vm: ToyboxViewModel = viewModel()) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> vm.onAppForegrounded()
                Lifecycle.Event.ON_STOP -> vm.onAppBackgrounded()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val route by vm.route.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val unlockedAchievements by vm.unlockedAchievements.collectAsStateWithLifecycle()
    val discoveredKeys by vm.discoveredKeys.collectAsStateWithLifecycle()
    val unlockedCharacters by vm.unlockedCharacters.collectAsStateWithLifecycle()
    val recentRuns by vm.recentRuns.collectAsStateWithLifecycle()
    val setup by vm.setup.collectAsStateWithLifecycle()
    val runState by vm.runState.collectAsStateWithLifecycle()
    val showTutorial by vm.showTutorial.collectAsStateWithLifecycle()
    val tutorialStep by vm.tutorialStep.collectAsStateWithLifecycle()
    val newUnlocks by vm.newlyUnlockedAchievements.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        when (route) {
            AppRoute.MAIN_MENU -> MainMenuScreen(
                profile = profile,
                unlockedAchievementCount = unlockedAchievements.size,
                totalAchievements = GameCatalog.achievements.size,
                onNavigate = vm::navigate,
                onOpenTutorial = { vm.openTutorial(fromMenu = true) }
            )

            AppRoute.RUN_SETUP -> RunSetupScreen(
                setup = setup,
                unlockedCharacters = unlockedCharacters,
                onSelectCharacter = vm::selectCharacter,
                onSelectDifficulty = vm::selectDifficulty,
                onToggleDefectMode = vm::toggleDefectMode,
                onRandomizeDefects = vm::randomizeSetupDefects,
                onToggleManualDefect = vm::toggleManualDefect,
                onStartRun = vm::startRun,
                onBack = { vm.navigate(AppRoute.MAIN_MENU) }
            )

            AppRoute.PLAYING -> {
                val activeRun = runState
                if (activeRun != null) {
                    GameScreen(
                        state = activeRun,
                        vignetteStrength = profile.darkVignetteIntensity,
                        screenShakeEnabled = profile.screenShakeEnabled,
                        onMoveInput = vm::setMoveInput,
                        onAimInput = vm::setAimInput,
                        onDodge = vm::triggerDodge,
                        onAbility = vm::triggerAbility,
                        onUseConsumable = vm::useConsumable,
                        onCycleConsumable = vm::cycleConsumable,
                        onTogglePause = { vm.togglePause(it) },
                        onOpenTutorial = { vm.openTutorial(fromMenu = false) },
                        onResolvePartChoice = vm::resolvePartChoice,
                        onResolveEvent = vm::resolveEvent,
                        onPurchaseShopOffer = vm::purchaseShopOffer,
                        onCloseShop = vm::closeShop,
                        onRepairAtBench = vm::repairAtBench,
                        onCloseRepairBench = vm::closeRepairBench,
                        onReopenFacility = vm::reopenRoomFacility,
                        onInspectCompanion = vm::inspectCompanion,
                        onResolveCompanionDialog = vm::resolveCompanionDialog,
                        onAbandonRun = vm::abandonRun
                    )
                }
            }

            AppRoute.RUN_SUMMARY -> {
                val summaryRun = runState
                if (summaryRun != null) {
                    RunSummaryScreen(
                        state = summaryRun,
                        newlyUnlocked = newUnlocks,
                        onReturnToMenu = { vm.navigate(AppRoute.MAIN_MENU) }
                    )
                } else {
                    MainMenuScreen(
                        profile = profile,
                        unlockedAchievementCount = unlockedAchievements.size,
                        totalAchievements = GameCatalog.achievements.size,
                        onNavigate = vm::navigate,
                        onOpenTutorial = { vm.openTutorial(fromMenu = true) }
                    )
                }
            }

            AppRoute.ACHIEVEMENT_BOOK -> AchievementBookScreen(
                unlockedMap = unlockedAchievements,
                onBack = { vm.navigate(AppRoute.MAIN_MENU) }
            )

            AppRoute.COLLECTION_ALBUM -> CollectionAlbumScreen(
                discoveredKeys = discoveredKeys,
                onBack = { vm.navigate(AppRoute.MAIN_MENU) }
            )

            AppRoute.CHALLENGES -> ChallengesScreen(
                profile = profile,
                recentRuns = recentRuns,
                onBack = { vm.navigate(AppRoute.MAIN_MENU) }
            )

            AppRoute.OPTIONS -> OptionsScreen(
                profile = profile,
                onUpdateSettings = vm::updateSettings,
                onOpenTutorial = { vm.openTutorial(fromMenu = true) },
                onBack = { vm.navigate(AppRoute.MAIN_MENU) }
            )

            AppRoute.CREDITS -> CreditsScreen(
                onBack = { vm.navigate(AppRoute.MAIN_MENU) }
            )
        }

        TutorialOverlayDialog(
            visible = showTutorial,
            step = tutorialStep,
            onNext = { vm.nextTutorialStep(6) },
            onPrev = vm::prevTutorialStep,
            onClose = vm::closeTutorial
        )
    }
}
