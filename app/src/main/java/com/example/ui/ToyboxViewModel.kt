package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.OstMood
import com.example.audio.OstMusicEngine
import com.example.audio.SoundEffectsEngine
import com.example.data.catalog.DefectCatalog
import com.example.data.catalog.GameCatalog
import com.example.data.local.*
import com.example.data.model.*
import com.example.engine.EngineInput
import com.example.engine.GameEngine
import com.example.engine.GameRunState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppRoute {
    MAIN_MENU,
    RUN_SETUP,
    PLAYING,
    RUN_SUMMARY,
    ACHIEVEMENT_BOOK,
    COLLECTION_ALBUM,
    CHALLENGES,
    OPTIONS,
    CREDITS
}

data class RunSetupState(
    val selectedCharacter: CharacterId = CharacterId.PATCH,
    val selectedDifficulty: Difficulty = Difficulty.NORMAL,
    val randomDefects: Boolean = true,
    val chosenDefects: List<DefectDef> = emptyList()
)

class ToyboxViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ToyboxDatabase.getInstance(application)
    private val repository = ProgressionRepository(db.dao())
    val sfx = SoundEffectsEngine()
    val ost = OstMusicEngine()

    private val _route = MutableStateFlow(AppRoute.MAIN_MENU)
    val route: StateFlow<AppRoute> = _route.asStateFlow()

    private val _setup = MutableStateFlow(RunSetupState())
    val setup: StateFlow<RunSetupState> = _setup.asStateFlow()

    private val _runState = MutableStateFlow<GameRunState?>(null)
    val runState: StateFlow<GameRunState?> = _runState.asStateFlow()

    private val _showTutorial = MutableStateFlow(false)
    val showTutorial: StateFlow<Boolean> = _showTutorial.asStateFlow()

    private val _tutorialStep = MutableStateFlow(0)
    val tutorialStep: StateFlow<Int> = _tutorialStep.asStateFlow()

    private val _newlyUnlockedAchievements = MutableStateFlow<List<AchievementDef>>(emptyList())
    val newlyUnlockedAchievements: StateFlow<List<AchievementDef>> = _newlyUnlockedAchievements.asStateFlow()

    val profile: StateFlow<PlayerProfileEntity> = repository.profileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerProfileEntity())

    val unlockedAchievements: StateFlow<Map<String, Long>> = repository.unlockedAchievementsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val discoveredKeys: StateFlow<Set<String>> = repository.discoveredKeysFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val unlockedCharacters: StateFlow<Set<CharacterId>> = repository.unlockedCharactersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), setOf(CharacterId.PATCH))

    val recentRuns: StateFlow<List<RunHistoryEntity>> = repository.recentRunsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @Volatile private var currentInput = EngineInput()
    private var loopJob: Job? = null
    private var runRecorded = false

    private val engine = GameEngine(
        sfx = sfx,
        onDiscovery = { cat, id ->
            viewModelScope.launch { repository.discover(cat, id) }
        },
        onAchievement = { achId ->
            viewModelScope.launch {
                val unlocked = repository.unlockAchievement(achId)
                if (unlocked != null) {
                    _newlyUnlockedAchievements.update { it + unlocked }
                    _runState.update { st ->
                        st?.copy(unlockedAchievementsThisRun = st.unlockedAchievementsThisRun + unlocked)
                    }
                }
            }
        },
        onCompanionGrudge = { compId ->
            viewModelScope.launch { repository.addCompanionGrudge(compId) }
        },
        onClearCompanionGrudge = { compId ->
            viewModelScope.launch { repository.clearCompanionGrudge(compId) }
        }
    )

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
            repository.profileFlow.collect { p ->
                sfx.enabled = p.soundEnabled
                ost.enabled = p.musicEnabled
            }
        }
        viewModelScope.launch {
            combine(_route, _runState) { r, st -> r to st }.collect { (r, st) ->
                syncOstMood(r, st)
            }
        }
        randomizeSetupDefects()
    }

    private fun syncOstMood(r: AppRoute, st: GameRunState?) {
        val mood = when (r) {
            AppRoute.PLAYING -> {
                if (st == null) OstMood.EXPLORE
                else if (st.enemies.any { it.type.isBoss }) OstMood.BOSS
                else if (st.enemies.isNotEmpty()) OstMood.COMBAT
                else if (st.currentRoom.type == RoomType.SHOP) OstMood.SHOP
                else OstMood.EXPLORE
            }
            AppRoute.RUN_SUMMARY -> {
                if (st?.isVictory == true) OstMood.VICTORY else OstMood.DEATH
            }
            else -> OstMood.MENU
        }
        ost.currentMood = mood
        ost.lowHealthTension = (r == AppRoute.PLAYING && st != null &&
            (st.hearts <= 1 || st.equippedParts.values.any { it.isDetached || it.wearState == WearState.CRITICAL }))
    }

    fun onAppForegrounded() {
        ost.start()
    }

    fun onAppBackgrounded() {
        ost.stop()
    }

    fun navigate(target: AppRoute) {
        if (target != AppRoute.PLAYING) {
            stopLoop()
        }
        _route.value = target
    }

    fun openTutorial(fromMenu: Boolean = false) {
        _tutorialStep.value = 0
        _showTutorial.value = true
        if (fromMenu && _runState.value == null) {
            // Stay on current screen with interactive illustrated tutorial cards
        } else {
            _runState.update { it?.copy(isPaused = true) }
        }
    }

    fun nextTutorialStep(totalSteps: Int) {
        if (_tutorialStep.value + 1 >= totalSteps) {
            closeTutorial()
        } else {
            _tutorialStep.value += 1
        }
    }

    fun prevTutorialStep() {
        _tutorialStep.value = (_tutorialStep.value - 1).coerceAtLeast(0)
    }

    fun closeTutorial() {
        _showTutorial.value = false
        _runState.update { it?.copy(isPaused = false) }
        viewModelScope.launch { repository.markTutorialCompleted() }
    }

    // Run Setup Controls
    fun selectCharacter(id: CharacterId) {
        _setup.update { it.copy(selectedCharacter = id) }
        randomizeSetupDefects()
    }

    fun selectDifficulty(diff: Difficulty) {
        _setup.update { it.copy(selectedDifficulty = diff) }
        randomizeSetupDefects()
    }

    fun toggleDefectMode(randomMode: Boolean) {
        _setup.update { it.copy(randomDefects = randomMode) }
        if (randomMode) randomizeSetupDefects()
    }

    fun randomizeSetupDefects() {
        val cur = _setup.value
        val charDef = GameCatalog.characterById(cur.selectedCharacter)
        val generated = DefectCatalog.generateRandomForDifficulty(
            cur.selectedDifficulty,
            charDef.suggestedDefectIds
        )
        _setup.update { it.copy(chosenDefects = generated) }
    }

    fun toggleManualDefect(defect: DefectDef) {
        val cur = _setup.value
        val list = cur.chosenDefects.toMutableList()
        if (list.any { it.id == defect.id }) {
            list.removeAll { it.id == defect.id }
        } else {
            val maxAllowed = cur.selectedDifficulty.maxDefects.coerceAtLeast(1)
            if (list.size < maxAllowed && DefectCatalog.isCompatible(list, defect)) {
                list.add(defect)
            }
        }
        _setup.update { it.copy(chosenDefects = list, randomDefects = false) }
    }

    fun startRun() {
        val cur = _setup.value
        val defects = if (cur.randomDefects) {
            val charDef = GameCatalog.characterById(cur.selectedCharacter)
            DefectCatalog.generateRandomForDifficulty(cur.selectedDifficulty, charDef.suggestedDefectIds)
        } else cur.chosenDefects

        val charDef = GameCatalog.characterById(cur.selectedCharacter)
        val grudges = profile.value.companionGrudges
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { runCatching { CompanionId.valueOf(it) }.getOrNull() }
        val initial = engine.createNewRun(charDef, cur.selectedDifficulty, defects, initialGrudges = grudges)
        _newlyUnlockedAchievements.value = emptyList()
        runRecorded = false
        currentInput = EngineInput()
        _runState.value = initial
        _route.value = AppRoute.PLAYING
        if (!profile.value.tutorialCompleted) {
            _tutorialStep.value = 0
            _showTutorial.value = true
            _runState.update { it?.copy(isPaused = true) }
        }
        startLoop()
    }

    fun setMoveInput(x: Float, y: Float) {
        currentInput = currentInput.copy(moveX = x, moveY = y)
    }

    fun setAimInput(x: Float, y: Float, active: Boolean) {
        currentInput = currentInput.copy(aimX = x, aimY = y, isShooting = active)
    }

    fun triggerDodge() {
        _runState.update { st ->
            st?.let { engine.triggerDodge(it, currentInput.moveX, currentInput.moveY) }
        }
    }

    fun triggerAbility() {
        _runState.update { st -> st?.let { engine.triggerAbility(it) } }
    }

    fun useConsumable() {
        _runState.update { st -> st?.let { engine.useSelectedConsumable(it) } }
    }

    fun cycleConsumable() {
        _runState.update { st -> st?.let { engine.cycleConsumable(it) } }
    }

    fun togglePause(pause: Boolean? = null) {
        _runState.update { st ->
            st?.copy(isPaused = pause ?: !st.isPaused)
        }
    }

    fun resolvePartChoice(equipNew: Boolean) {
        _runState.update { st -> st?.let { engine.resolvePartChoice(it, equipNew) } }
    }

    fun resolveEvent(optionId: String) {
        _runState.update { st -> st?.let { engine.resolveEventOption(it, optionId) } }
    }

    fun purchaseShopOffer(offerId: String) {
        _runState.update { st -> st?.let { engine.resolveShopPurchase(it, offerId) } }
    }

    fun closeShop() {
        _runState.update { st -> st?.copy(shopOpen = false) }
    }

    fun repairAtBench(zone: BodyZone?) {
        _runState.update { st -> st?.let { engine.resolveRepairBench(it, zone) } }
    }

    fun closeRepairBench() {
        _runState.update { st -> st?.copy(repairBenchOpen = false) }
    }

    fun reopenRoomFacility() {
        _runState.update { st ->
            when (st?.currentRoom?.type) {
                RoomType.SHOP -> st.copy(shopOpen = true)
                RoomType.REPAIR -> st.copy(repairBenchOpen = true)
                else -> st
            }
        }
    }

    fun inspectCompanion(companionId: CompanionId) {
        _runState.update { st -> st?.let { engine.openCompanionInspect(it, companionId) } }
    }

    fun resolveCompanionDialog(optionId: String) {
        _runState.update { st -> st?.let { engine.resolveCompanionDialog(it, optionId) } }
    }

    fun abandonRun() {
        val st = _runState.value
        if (st != null && st.activeCompanions.any { it.id == CompanionId.BUNNY }) {
            viewModelScope.launch { repository.addCompanionGrudge(CompanionId.BUNNY.name) }
        }
        if (st != null && !runRecorded) {
            finalizeRun(st.copy(isGameOver = true, isActive = false))
        }
        stopLoop()
        _route.value = AppRoute.RUN_SUMMARY
    }

    fun updateSettings(sound: Boolean, music: Boolean, shake: Boolean, vignette: Float) {
        sfx.enabled = sound
        ost.enabled = music
        viewModelScope.launch {
            repository.updateSettings(sound, music, shake, vignette)
        }
    }

    private fun startLoop() {
        loopJob?.cancel()
        loopJob = viewModelScope.launch {
            var lastNs = System.nanoTime()
            while (true) {
                delay(16L)
                val now = System.nanoTime()
                val dt = ((now - lastNs) / 1_000_000_000f).coerceIn(0.005f, 0.05f)
                lastNs = now
                val current = _runState.value ?: continue
                if (current.isGameOver || current.isVictory) {
                    if (!runRecorded) finalizeRun(current)
                    _route.value = AppRoute.RUN_SUMMARY
                    break
                }
                val next = engine.step(current, currentInput, dt)
                _runState.value = next
                if (next.isGameOver || next.isVictory) {
                    if (!runRecorded) finalizeRun(next)
                    delay(450L)
                    _route.value = AppRoute.RUN_SUMMARY
                    break
                }
            }
        }
    }

    private fun finalizeRun(finalState: GameRunState) {
        runRecorded = true
        _runState.value = finalState
        val defectPoints = finalState.activeDefects.sumOf { it.challengePoints }
        val baseScore = finalState.roomsClearedCount * 2 +
            finalState.bossesDefeatedCount * 15 +
            finalState.enemiesDefeatedCount +
            (defectPoints * 5) +
            (if (finalState.isVictory) 40 else 0)
        val challengeScore = (baseScore * finalState.difficulty.medalMultiplier).toInt()
        val medals = (challengeScore / 20).coerceAtLeast(if (finalState.roomsClearedCount > 0) 1 else 0)

        viewModelScope.launch {
            repository.recordRunEnd(
                characterId = finalState.character.id,
                difficulty = finalState.difficulty,
                victory = finalState.isVictory,
                floorReached = finalState.floorNumber,
                roomsCleared = finalState.roomsClearedCount,
                enemiesDefeated = finalState.enemiesDefeatedCount,
                durationSeconds = finalState.elapsedSeconds.toInt(),
                challengeScore = challengeScore,
                medalsEarned = medals
            )
            val totalKills = profile.value.totalKills + finalState.enemiesDefeatedCount
            if (totalKills >= 40) repository.unlockAchievement("ach_40_kills")
            if (discoveredKeys.value.count { it.startsWith("part:") } >= 8) {
                repository.unlockAchievement("ach_5_parts")
            }
            if (discoveredKeys.value.count { it.startsWith("item:") } >= 6) {
                repository.unlockAchievement("ach_5_items")
            }
        }
    }

    private fun stopLoop() {
        loopJob?.cancel()
        loopJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopLoop()
        ost.stop()
    }
}
