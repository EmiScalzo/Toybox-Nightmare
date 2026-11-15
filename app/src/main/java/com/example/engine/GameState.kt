package com.example.engine

import com.example.data.model.*

const val ARENA_W = 1000f
const val ARENA_H = 1050f
const val WALL_THICKNESS = 66f

data class ActiveProjectile(
    val id: Long,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float,
    val damage: Float,
    val fromPlayer: Boolean,
    val colorHex: Long,
    val style: AttackStyle = AttackStyle.BUTTON_SHOT,
    val lifeSec: Float = 1.6f,
    val pierceLeft: Int = 0,
    val bouncesLeft: Int = 0,
    val homing: Boolean = false,
    val explosive: Boolean = false,
    val hitEnemyIds: Set<Long> = emptySet()
)

data class ActiveEnemy(
    val id: Long,
    val type: EnemyType,
    val x: Float,
    val y: Float,
    val vx: Float = 0f,
    val vy: Float = 0f,
    val hp: Float,
    val maxHp: Float,
    val radius: Float,
    val attackTimer: Float = 1.2f,
    val stateTimer: Float = 0f,
    val phase: Int = 0,
    val disguised: Boolean = false,
    val stunnedTimer: Float = 0f,
    val hitFlashTimer: Float = 0f,
    val telegraphTimer: Float = 0f,
    val chargeDirX: Float = 0f,
    val chargeDirY: Float = 0f
)

enum class PickupKind {
    BUTTON,
    GOLD_BUTTON,
    HEART,
    CONSUMABLE,
    DETACHED_PART,
    NEW_PART_PEDESTAL,
    PASSIVE_ITEM_PEDESTAL,
    COMPANION_SHRINE
}

data class GroundPickup(
    val id: Long,
    val kind: PickupKind,
    val x: Float,
    val y: Float,
    val valueStr: String = "",
    val amount: Int = 1,
    val expireSec: Float? = null,
    val detachedPart: EquippedBodyPart? = null
)

data class ParticleFx(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float,
    val colorHex: Long,
    val life: Float,
    val maxLife: Float
)

data class HazardBomb(
    val id: Long,
    val x: Float,
    val y: Float,
    val fuseSec: Float,
    val fromPlayer: Boolean = true,
    val radius: Float = 135f,
    val damage: Float = 36f
)

data class DecoyPuppet(
    val x: Float,
    val y: Float,
    val lifeSec: Float
)

data class ActiveCompanion(
    val id: CompanionId,
    val x: Float,
    val y: Float,
    val vx: Float = 0f,
    val vy: Float = 0f,
    val hp: Int = 3,
    val maxHp: Int = 3,
    val broken: Boolean = false,
    val actionTimer: Float = 1.4f,
    val roomsHeld: Int = 0,
    val giftsOffered: Int = 0,
    val consecutiveDamagedRooms: Int = 0,
    val needsAttention: Boolean = false
)

data class CompanionChoiceOption(
    val id: String,
    val label: String,
    val description: String,
    val buttonCost: Int = 0,
    val isDanger: Boolean = false
)

data class PendingCompanionDialog(
    val companionId: CompanionId,
    val title: String,
    val subtitle: String,
    val narrative: String,
    val offeredItemId: String? = null,
    val options: List<CompanionChoiceOption>
)

enum class SectorState { STABLE, WARNING, GONE }

data class VanishingSector(
    val index: Int,
    val col: Int,
    val row: Int,
    val state: SectorState = SectorState.STABLE,
    val timer: Float = 2.6f
)

data class PendingPartChoice(
    val newPart: BodyPartDef,
    val currentInZone: EquippedBodyPart?,
    val resultingSynergies: List<SynergyDef>
)

data class RunUiBanner(
    val title: String,
    val subtitle: String,
    val colorHex: Long = 0xFFC89948,
    val ttlSec: Float = 3.2f
)

data class ToyboxMemoryState(
    val dodgeUseCount: Int = 0,
    val explosiveUseCount: Int = 0,
    val repairUseCount: Int = 0,
    val compassionScore: Int = 0,
    val curseOrChallengeCleared: Int = 0,
    val enemiesAdaptDodge: Boolean = false,
    val enemiesStealBombs: Boolean = false,
    val secretCluesRevealed: Boolean = false,
    val memoriesCollected: List<String> = emptyList()
)

data class GameRunState(
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val isGameOver: Boolean = false,
    val isVictory: Boolean = false,
    val character: ToyCharacterDef,
    val difficulty: Difficulty,
    val floorNumber: Int = 1,
    val floorTheme: FloorTheme,
    val dungeon: DungeonFloor,
    val currentCoord: GridCoord = GridCoord(0, 0),
    val currentRoom: RoomBlueprint,

    // Player physics & combat
    val playerX: Float = ARENA_W / 2f,
    val playerY: Float = ARENA_H / 2f,
    val playerVx: Float = 0f,
    val playerVy: Float = 0f,
    val facingAngle: Float = 0f,
    val aimX: Float = 0f,
    val aimY: Float = 0f,
    val walkCycle: Float = 0f,
    val hearts: Int = 3,
    val maxHearts: Int = 3,
    val invulnTimer: Float = 0f,
    val dodgeTimer: Float = 0f,
    val dodgeCooldownTimer: Float = 0f,
    val dodgeDirX: Float = 0f,
    val dodgeDirY: Float = 0f,
    val fireCooldownTimer: Float = 0f,
    val abilityCooldownTimer: Float = 0f,
    val abilityBuffTimer: Float = 0f,
    val disorientTimer: Float = 0f,
    val lightFlashTimer: Float = 0f,
    val blurPulseTimer: Float = 0f,
    val soundWaveTimer: Float = 0f,
    val stunWaveTimer: Float = 0f,
    val impulsePauseTimer: Float = 0f,
    val impulseBurstTimer: Float = 0f,
    val stealthTimer: Float = 0f,

    // Equipment & progression in run
    val equippedParts: Map<BodyZone, EquippedBodyPart> = emptyMap(),
    val passiveItems: List<PassiveItemDef> = emptyList(),
    val activeSynergies: List<SynergyDef> = emptyList(),
    val activeDefects: List<DefectDef> = emptyList(),
    val consumables: List<ConsumableType> = listOf(ConsumableType.BOMB, ConsumableType.SEWING_KIT),
    val selectedConsumableIdx: Int = 0,
    val buttons: Int = 3,
    val goldenButtons: Int = 0,

    // Companions & Dynamic Room Mechanics
    val activeCompanions: List<ActiveCompanion> = emptyList(),
    val pendingCompanionDialog: PendingCompanionDialog? = null,
    val pendingGrudgeCompanions: List<CompanionId> = emptyList(),
    val betrayedBunnySpawnedThisRun: Boolean = false,
    val tiltX: Float = 0f,
    val tiltY: Float = 0f,
    val tiltTimer: Float = 4.5f,
    val vanishingSectors: List<VanishingSector> = emptyList(),
    val doorShiftTimer: Float = 5.5f,
    val doorMapping: Map<Direction, Direction> = Direction.entries.associateWith { it },
    val blackoutAlpha: Float = 0f,
    val damagedInPrevRoom: Boolean = false,
    val damagedInCurrentRoom: Boolean = false,

    // Arena entities
    val enemies: List<ActiveEnemy> = emptyList(),
    val projectiles: List<ActiveProjectile> = emptyList(),
    val pickups: List<GroundPickup> = emptyList(),
    val particles: List<ParticleFx> = emptyList(),
    val bombs: List<HazardBomb> = emptyList(),
    val decoys: List<DecoyPuppet> = emptyList(),
    val exitTrapdoorOpen: Boolean = false,

    // Interactive overlays
    val activeEvent: RoomEventDef? = null,
    val pendingPartChoice: PendingPartChoice? = null,
    val shopOpen: Boolean = false,
    val repairBenchOpen: Boolean = false,
    val activeBanner: RunUiBanner? = null,

    // Run metrics
    val elapsedSeconds: Float = 0f,
    val roomsClearedCount: Int = 0,
    val enemiesDefeatedCount: Int = 0,
    val bossesDefeatedCount: Int = 0,
    val damageTakenInCurrentBoss: Boolean = false,
    val usedConsumableInBoss: Boolean = false,
    val usedAnyRepair: Boolean = false,
    val screenShake: Float = 0f,
    val toyboxMemory: ToyboxMemoryState = ToyboxMemoryState(),
    val unlockedAchievementsThisRun: List<AchievementDef> = emptyList()
)
