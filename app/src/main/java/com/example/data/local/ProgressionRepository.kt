package com.example.data.local

import com.example.data.catalog.GameCatalog
import com.example.data.model.AchievementDef
import com.example.data.model.CharacterId
import com.example.data.model.Difficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class ProgressionSnapshot(
    val profile: PlayerProfileEntity,
    val unlockedAchievements: Map<String, Long>,
    val discoveredKeys: Set<String>,
    val unlockedCharacters: Set<CharacterId>,
    val recentRuns: List<RunHistoryEntity>
)

class ProgressionRepository(private val dao: ToyboxDao) {

    val profileFlow: Flow<PlayerProfileEntity> = dao.observeProfile().map {
        it ?: PlayerProfileEntity()
    }

    val unlockedAchievementsFlow: Flow<Map<String, Long>> = dao.observeAchievements().map { list ->
        list.associate { it.id to it.unlockedAtEpochMs }
    }

    val discoveredKeysFlow: Flow<Set<String>> = dao.observeDiscoveries().map { list ->
        list.map { it.entryKey }.toSet()
    }

    val unlockedCharactersFlow: Flow<Set<CharacterId>> = dao.observeUnlockedCharacters().map { list ->
        val set = list.mapNotNull { runCatching { CharacterId.valueOf(it.characterId) }.getOrNull() }.toMutableSet()
        set.add(CharacterId.PATCH)
        set
    }

    val recentRunsFlow: Flow<List<RunHistoryEntity>> = dao.observeRunHistory()

    suspend fun ensureInitialized() {
        if (dao.getProfile() == null) {
            dao.upsertProfile(PlayerProfileEntity())
        }
        dao.insertUnlockedCharacter(UnlockedCharacterEntity(CharacterId.PATCH.name, System.currentTimeMillis()))
        // Discover starting parts for Patch
        discover("character", CharacterId.PATCH.name)
        GameCatalog.characterById(CharacterId.PATCH).startingParts.values.forEach { partId ->
            discover("part", partId)
        }
    }

    suspend fun discover(category: String, id: String) {
        dao.insertDiscovery(
            CollectionDiscoveryEntity(
                entryKey = "$category:$id",
                category = category,
                discoveredAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun unlockAchievement(achievementId: String): AchievementDef? {
        val def = GameCatalog.achievements.find { it.id == achievementId } ?: return null
        val rowId = dao.insertAchievement(
            AchievementProgressEntity(
                id = achievementId,
                unlockedAtEpochMs = System.currentTimeMillis()
            )
        )
        if (rowId != -1L) {
            def.unlockCharacterId?.let { charId ->
                dao.insertUnlockedCharacter(
                    UnlockedCharacterEntity(charId.name, System.currentTimeMillis())
                )
            }
            val current = dao.getProfile() ?: PlayerProfileEntity()
            dao.upsertProfile(
                current.copy(
                    challengePoints = current.challengePoints + def.rewardChallengePoints
                )
            )
            return def
        }
        return null
    }

    suspend fun unlockCharacter(characterId: CharacterId) {
        dao.insertUnlockedCharacter(
            UnlockedCharacterEntity(characterId.name, System.currentTimeMillis())
        )
    }

    suspend fun markTutorialCompleted() {
        val current = dao.getProfile() ?: PlayerProfileEntity()
        dao.upsertProfile(current.copy(tutorialCompleted = true))
    }

    suspend fun updateSettings(
        soundEnabled: Boolean,
        musicEnabled: Boolean,
        screenShakeEnabled: Boolean,
        darkVignetteIntensity: Float
    ) {
        val current = dao.getProfile() ?: PlayerProfileEntity()
        dao.upsertProfile(
            current.copy(
                soundEnabled = soundEnabled,
                musicEnabled = musicEnabled,
                screenShakeEnabled = screenShakeEnabled,
                darkVignetteIntensity = darkVignetteIntensity
            )
        )
    }

    suspend fun recordRunEnd(
        characterId: CharacterId,
        difficulty: Difficulty,
        victory: Boolean,
        floorReached: Int,
        roomsCleared: Int,
        enemiesDefeated: Int,
        durationSeconds: Int,
        challengeScore: Int,
        medalsEarned: Int
    ) {
        val current = dao.getProfile() ?: PlayerProfileEntity()
        dao.upsertProfile(
            current.copy(
                totalRuns = current.totalRuns + 1,
                totalWins = current.totalWins + if (victory) 1 else 0,
                totalKills = current.totalKills + enemiesDefeated,
                totalRoomsCleared = current.totalRoomsCleared + roomsCleared,
                challengePoints = current.challengePoints + challengeScore,
                medals = current.medals + medalsEarned,
                highestFloor = maxOf(current.highestFloor, floorReached)
            )
        )
        dao.insertRunHistory(
            RunHistoryEntity(
                characterId = characterId.name,
                difficulty = difficulty.displayName,
                victory = victory,
                floorReached = floorReached,
                roomsCleared = roomsCleared,
                enemiesDefeated = enemiesDefeated,
                durationSeconds = durationSeconds,
                challengeScore = challengeScore,
                timestampMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun addCompanionGrudge(companionId: String) {
        val current = dao.getProfile() ?: PlayerProfileEntity()
        val set = current.companionGrudges.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
        if (set.add(companionId)) {
            dao.upsertProfile(current.copy(companionGrudges = set.joinToString(",")))
        }
    }

    suspend fun clearCompanionGrudge(companionId: String) {
        val current = dao.getProfile() ?: return
        val set = current.companionGrudges.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
        if (set.remove(companionId)) {
            dao.upsertProfile(current.copy(companionGrudges = set.joinToString(",")))
        }
    }

    suspend fun markLoreSeen() {
        val current = dao.getProfile() ?: PlayerProfileEntity()
        if (!current.loreSeen) {
            dao.upsertProfile(current.copy(loreSeen = true))
        }
    }

    suspend fun recordManufacturerDecision(accepted: Boolean) {
        val current = dao.getProfile() ?: PlayerProfileEntity()
        dao.upsertProfile(
            current.copy(
                manufacturerAccepted = current.manufacturerAccepted + if (accepted) 1 else 0,
                manufacturerRejected = current.manufacturerRejected + if (!accepted) 1 else 0
            )
        )
    }
}
