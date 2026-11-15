package com.example.engine

import com.example.data.catalog.GameCatalog
import com.example.data.model.Difficulty
import com.example.data.model.Direction
import com.example.data.model.EnemyType
import com.example.data.model.RoomMechanic
import com.example.data.model.RoomType
import com.example.data.model.SecretRoomFlavor
import kotlin.math.abs
import kotlin.random.Random

data class GridCoord(val x: Int, val y: Int) {
    fun neighbor(dir: Direction): GridCoord = when (dir) {
        Direction.NORTH -> GridCoord(x, y - 1)
        Direction.SOUTH -> GridCoord(x, y + 1)
        Direction.EAST -> GridCoord(x + 1, y)
        Direction.WEST -> GridCoord(x - 1, y)
    }
}

data class ObstacleRect(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val kind: Int // 0 = wooden crate, 1 = glass vitrine, 2 = needle table, 3 = iron pillar, 4 = toy pile
)

data class RoomBlueprint(
    val coord: GridCoord,
    val type: RoomType,
    val doors: Set<Direction>,
    val secretDoors: Set<Direction> = emptySet(),
    val obstacles: List<ObstacleRect>,
    val initialEnemies: List<EnemySpawn>,
    val eventId: String? = null,
    var cleared: Boolean = false,
    var visited: Boolean = false,
    var rewardSpawned: Boolean = false,
    var shopStock: List<ShopOffer> = emptyList(),
    var repairUsed: Boolean = false,
    var eventResolved: Boolean = false,
    val visualVariant: Int = 0,
    val mechanic: RoomMechanic = RoomMechanic.NONE,
    val secretFlavor: SecretRoomFlavor? = null
)

data class EnemySpawn(
    val type: EnemyType,
    val nx: Float, // normalized 0.15..0.85 inside arena
    val ny: Float
)

data class ShopOffer(
    val id: String,
    val label: String,
    val price: Int,
    val isBodyPart: Boolean = false,
    val isConsumable: Boolean = false,
    val isHeal: Boolean = false,
    val itemId: String? = null,
    var purchased: Boolean = false
)

data class DungeonFloor(
    val floorNumber: Int,
    val rooms: Map<GridCoord, RoomBlueprint>,
    val startCoord: GridCoord,
    val bossCoord: GridCoord
)

object DungeonGenerator {

    fun generateFloor(
        floorNumber: Int,
        difficulty: Difficulty,
        secretBonusChance: Float = 0f,
        forceManufacturerRoom: Boolean = false,
        rng: Random = Random.Default
    ): DungeonFloor {
        val targetCount = (11 + floorNumber * 2).coerceIn(12, 18)
        val coords = linkedSetOf(GridCoord(0, 0))
        val frontier = mutableListOf(GridCoord(0, 0))

        while (coords.size < targetCount && frontier.isNotEmpty()) {
            val base = frontier[rng.nextInt(frontier.size)]
            val dirs = Direction.entries.shuffled(rng)
            var added = false
            for (d in dirs) {
                val next = base.neighbor(d)
                if (next !in coords) {
                    val adjCount = Direction.entries.count { next.neighbor(it) in coords }
                    if (adjCount <= 1 || (coords.size > 6 && rng.nextFloat() < 0.2f)) {
                        coords.add(next)
                        frontier.add(next)
                        added = true
                        break
                    }
                }
            }
            if (!added) frontier.remove(base)
        }

        // Determine distances from start
        val dists = mutableMapOf(GridCoord(0, 0) to 0)
        val queue = ArrayDeque<GridCoord>()
        queue.add(GridCoord(0, 0))
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            val d = dists[cur] ?: 0
            for (dir in Direction.entries) {
                val n = cur.neighbor(dir)
                if (n in coords && n !in dists) {
                    dists[n] = d + 1
                    queue.add(n)
                }
            }
        }

        val deadEnds = coords.filter { c ->
            c != GridCoord(0, 0) && Direction.entries.count { c.neighbor(it) in coords } == 1
        }

        val bossCoord = (deadEnds.maxByOrNull { dists[it] ?: 0 }
            ?: coords.maxByOrNull { dists[it] ?: 0 }
            ?: GridCoord(0, 0))

        val availableSpecial = (deadEnds - bossCoord).shuffled(rng).toMutableList()
        val fallbackPool = (coords - GridCoord(0, 0) - bossCoord - availableSpecial.toSet()).shuffled(rng)
        fun takeSpecial(): GridCoord? =
            if (availableSpecial.isNotEmpty()) availableSpecial.removeAt(0)
            else fallbackPool.firstOrNull { it != bossCoord }

        val typeMap = mutableMapOf<GridCoord, RoomType>()
        typeMap[GridCoord(0, 0)] = RoomType.START
        typeMap[bossCoord] = RoomType.BOSS

        takeSpecial()?.let { typeMap[it] = RoomType.TREASURE }
        takeSpecial()?.let { typeMap[it] = RoomType.SHOP }
        if (rng.nextFloat() < difficulty.repairStationChance + 0.25f) {
            (availableSpecial.firstOrNull() ?: (coords - typeMap.keys).randomOrNull(rng))?.let {
                availableSpecial.remove(it)
                typeMap[it] = RoomType.REPAIR
            }
        }
        (availableSpecial.firstOrNull() ?: (coords - typeMap.keys).randomOrNull(rng))?.let {
            availableSpecial.remove(it)
            typeMap[it] = RoomType.EVENT
        }

        // Occasional Manufacturer workshop or Forbidden room
        if (forceManufacturerRoom || rng.nextFloat() < 0.30f) {
            (availableSpecial.firstOrNull() ?: (coords - typeMap.keys).randomOrNull(rng))?.let {
                availableSpecial.remove(it)
                typeMap[it] = RoomType.MANUFACTURER
            }
        }

        // Optional secret room
        var secretCoord: GridCoord? = null
        if (rng.nextFloat() < 0.65f + secretBonusChance) {
            val candidates = coords.flatMap { c -> Direction.entries.map { c.neighbor(it) } }
                .distinct()
                .filter { it !in coords && it != bossCoord }
                .filter { cand ->
                    val adj = Direction.entries.count { cand.neighbor(it) in coords }
                    adj >= 2 && Direction.entries.none { cand.neighbor(it) == bossCoord }
                }
            if (candidates.isNotEmpty()) {
                secretCoord = candidates.random(rng)
                coords.add(secretCoord)
                typeMap[secretCoord] = RoomType.SECRET
            }
        }

        // Assign remaining rooms
        for (c in coords) {
            if (c !in typeMap) {
                val r = rng.nextFloat()
                typeMap[c] = when {
                    r < 0.06f -> RoomType.FORBIDDEN
                    r < 0.14f -> RoomType.CURSE
                    r < 0.24f -> RoomType.CHALLENGE
                    else -> RoomType.COMBAT
                }
            }
        }

        val floorTheme = GameCatalog.floors[(floorNumber - 1).coerceIn(0, GameCatalog.floors.lastIndex)]
        val floorBoss = listOfNotNull(floorTheme.bossType, floorTheme.altBossType).random(rng)
        val rooms = mutableMapOf<GridCoord, RoomBlueprint>()

        for (c in coords) {
            val type = typeMap[c] ?: RoomType.COMBAT
            val doors = mutableSetOf<Direction>()
            val secretDoors = mutableSetOf<Direction>()
            for (dir in Direction.entries) {
                val n = c.neighbor(dir)
                if (n in coords) {
                    if (type == RoomType.SECRET || typeMap[n] == RoomType.SECRET) {
                        secretDoors.add(dir)
                    } else {
                        doors.add(dir)
                    }
                }
            }

            val isPeaceful = type in setOf(
                RoomType.START, RoomType.TREASURE, RoomType.SHOP,
                RoomType.REPAIR, RoomType.EVENT, RoomType.SECRET, RoomType.MANUFACTURER
            )
            val mechanic = rollRoomMechanic(type, floorNumber, difficulty, rng)
            val obstacles = if (mechanic == RoomMechanic.NURSERY_MAZE) {
                generateNurseryMazeObstacles(rng)
            } else {
                generateObstacles(type, rng)
            }
            val enemies = when {
                type == RoomType.BOSS -> listOf(EnemySpawn(floorBoss, 0.5f, 0.26f))
                type == RoomType.CHALLENGE -> generateEnemies(floorNumber, count = 4 + floorNumber.coerceAtMost(2), mechanic, obstacles, rng)
                type == RoomType.COMBAT -> generateEnemies(floorNumber, count = (2 + floorNumber / 2 + rng.nextInt(2)).coerceAtMost(5), mechanic, obstacles, rng)
                type == RoomType.CURSE || type == RoomType.FORBIDDEN -> generateEnemies(floorNumber, count = 3, mechanic, obstacles, rng)
                else -> emptyList()
            }

            val eventId = if (type == RoomType.EVENT) GameCatalog.events.random(rng).id else null
            val shopStock = if (type == RoomType.SHOP) buildShopStock(rng) else emptyList()
            val variant = ((c.x * 7 + c.y * 13 + floorNumber * 3 + rng.nextInt(4)) and 0x7FFFFFFF) % 4
            val flavor = if (type == RoomType.SECRET) SecretRoomFlavor.entries.random(rng) else null

            rooms[c] = RoomBlueprint(
                coord = c,
                type = type,
                doors = doors,
                secretDoors = secretDoors,
                obstacles = obstacles,
                initialEnemies = enemies,
                eventId = eventId,
                cleared = isPeaceful && type != RoomType.CHALLENGE,
                visited = (c == GridCoord(0, 0)),
                shopStock = shopStock,
                visualVariant = variant,
                mechanic = mechanic,
                secretFlavor = flavor
            )
        }

        return DungeonFloor(
            floorNumber = floorNumber,
            rooms = rooms,
            startCoord = GridCoord(0, 0),
            bossCoord = bossCoord
        )
    }

    private fun rollRoomMechanic(
        type: RoomType,
        floorNumber: Int,
        difficulty: Difficulty,
        rng: Random
    ): RoomMechanic {
        if (type !in setOf(RoomType.COMBAT, RoomType.CHALLENGE, RoomType.CURSE)) {
            return RoomMechanic.NONE
        }
        val chance = when (difficulty) {
            Difficulty.SUAVE -> 0.28f
            Difficulty.NORMAL -> 0.48f
            Difficulty.DIFICIL -> 0.68f
            Difficulty.PESADILLA -> 0.85f
        } + ((floorNumber - 1) * 0.04f)
        if (rng.nextFloat() > chance) return RoomMechanic.NONE

        val eligible = RoomMechanic.entries.filter { m ->
            m != RoomMechanic.NONE &&
                (difficulty.ordinal >= m.minDifficultyOrdinal || floorNumber >= 2)
        }
        return eligible.randomOrNull(rng) ?: RoomMechanic.NONE
    }

    private fun generateNurseryMazeObstacles(rng: Random): List<ObstacleRect> {
        // Structured wooden toy-block corridors inside 0.20..0.80 leaving door lanes (center cross & outer ring) open!
        val layout = rng.nextInt(3)
        return when (layout) {
            0 -> listOf(
                // 4 L-shaped quadrant toy block walls creating a winding nursery maze
                ObstacleRect(0.22f, 0.22f, 0.19f, 0.07f, 5),
                ObstacleRect(0.22f, 0.22f, 0.07f, 0.19f, 5),
                ObstacleRect(0.59f, 0.22f, 0.19f, 0.07f, 5),
                ObstacleRect(0.71f, 0.22f, 0.07f, 0.19f, 5),
                ObstacleRect(0.22f, 0.71f, 0.19f, 0.07f, 5),
                ObstacleRect(0.22f, 0.59f, 0.07f, 0.19f, 5),
                ObstacleRect(0.59f, 0.71f, 0.19f, 0.07f, 5),
                ObstacleRect(0.71f, 0.59f, 0.07f, 0.19f, 5)
            )
            1 -> listOf(
                // Horizontal nursery crib baffles with alternating center/side passes
                ObstacleRect(0.18f, 0.30f, 0.24f, 0.075f, 5),
                ObstacleRect(0.58f, 0.30f, 0.24f, 0.075f, 5),
                ObstacleRect(0.32f, 0.47f, 0.36f, 0.065f, 5),
                ObstacleRect(0.18f, 0.64f, 0.24f, 0.075f, 5),
                ObstacleRect(0.58f, 0.64f, 0.24f, 0.075f, 5)
            )
            else -> listOf(
                // Vertical shelf & toy chest corridors
                ObstacleRect(0.26f, 0.21f, 0.075f, 0.23f, 5),
                ObstacleRect(0.26f, 0.56f, 0.075f, 0.23f, 5),
                ObstacleRect(0.665f, 0.21f, 0.075f, 0.23f, 5),
                ObstacleRect(0.665f, 0.56f, 0.075f, 0.23f, 5),
                ObstacleRect(0.46f, 0.34f, 0.08f, 0.08f, 4),
                ObstacleRect(0.46f, 0.58f, 0.08f, 0.08f, 4)
            )
        }
    }

    private fun generateObstacles(type: RoomType, rng: Random): List<ObstacleRect> {
        if (type == RoomType.START || type == RoomType.SHOP || type == RoomType.REPAIR ||
            type == RoomType.SECRET || type == RoomType.EVENT || type == RoomType.MANUFACTURER
        ) {
            return emptyList()
        }
        if (type == RoomType.TREASURE) {
            // Place decorative vitrines only in far corners so center pedestals (0.38,0.5) and (0.62,0.5) are 100% accessible
            return listOf(
                ObstacleRect(0.14f, 0.16f, 0.08f, 0.08f, 1),
                ObstacleRect(0.78f, 0.16f, 0.08f, 0.08f, 1),
                ObstacleRect(0.14f, 0.76f, 0.08f, 0.08f, 1),
                ObstacleRect(0.78f, 0.76f, 0.08f, 0.08f, 1)
            )
        }
        if (type == RoomType.BOSS) {
            val bossKind = if (rng.nextBoolean()) 3 else 0
            return listOf(
                ObstacleRect(0.22f, 0.32f, 0.085f, 0.085f, bossKind),
                ObstacleRect(0.695f, 0.32f, 0.085f, 0.085f, bossKind),
                ObstacleRect(0.22f, 0.62f, 0.085f, 0.085f, bossKind),
                ObstacleRect(0.695f, 0.62f, 0.085f, 0.085f, bossKind)
            )
        }
        val pattern = rng.nextInt(7)
        return when (pattern) {
            0 -> listOf(
                ObstacleRect(0.27f, 0.30f, 0.10f, 0.10f, 0),
                ObstacleRect(0.63f, 0.30f, 0.10f, 0.10f, 0),
                ObstacleRect(0.27f, 0.60f, 0.10f, 0.10f, 1),
                ObstacleRect(0.63f, 0.60f, 0.10f, 0.10f, 1)
            )
            1 -> listOf(
                ObstacleRect(0.45f, 0.45f, 0.10f, 0.10f, 2),
                ObstacleRect(0.20f, 0.44f, 0.08f, 0.12f, 0),
                ObstacleRect(0.72f, 0.44f, 0.08f, 0.12f, 0)
            )
            2 -> listOf(
                ObstacleRect(0.30f, 0.28f, 0.13f, 0.08f, 1),
                ObstacleRect(0.57f, 0.64f, 0.13f, 0.08f, 1)
            )
            3 -> listOf(
                ObstacleRect(0.24f, 0.28f, 0.085f, 0.085f, 3),
                ObstacleRect(0.675f, 0.28f, 0.085f, 0.085f, 3),
                ObstacleRect(0.455f, 0.46f, 0.09f, 0.09f, 4)
            )
            4 -> listOf(
                ObstacleRect(0.32f, 0.45f, 0.09f, 0.11f, 4),
                ObstacleRect(0.59f, 0.45f, 0.09f, 0.11f, 4),
                ObstacleRect(0.45f, 0.24f, 0.10f, 0.08f, 1)
            )
            5 -> listOf(
                ObstacleRect(0.22f, 0.24f, 0.09f, 0.09f, 3),
                ObstacleRect(0.69f, 0.67f, 0.09f, 0.09f, 3),
                ObstacleRect(0.69f, 0.24f, 0.09f, 0.09f, 4),
                ObstacleRect(0.22f, 0.67f, 0.09f, 0.09f, 4)
            )
            else -> emptyList()
        }
    }

    private fun generateEnemies(
        floor: Int,
        count: Int,
        mechanic: RoomMechanic,
        obstacles: List<ObstacleRect>,
        rng: Random
    ): List<EnemySpawn> {
        val pool = mutableListOf(
            EnemyType.TIN_SOLDIER,
            EnemyType.CREEPY_DOLL,
            EnemyType.PLUSH_BEAR,
            EnemyType.JACK_IN_BOX,
            EnemyType.CLOCKWORK_SPIDER,
            EnemyType.WINDUP_CHOMPER,
            EnemyType.TIN_CAPTAIN,
            EnemyType.CLOCKWORK_COURIER
        )
        if (floor >= 2) {
            pool.add(EnemyType.MIRROR_DOLL)
            pool.add(EnemyType.THIEF_GREMLIN)
            pool.add(EnemyType.NEEDLE_MARIONETTE)
            pool.add(EnemyType.PORCELAIN_CLOWN)
            pool.add(EnemyType.BODYGUARD_BEAR)
            pool.add(EnemyType.STITCH_MECHANIC)
        }
        if (floor >= 3) {
            pool.add(EnemyType.MIMIC_CHEST)
            pool.add(EnemyType.SHADOW_PUPPET)
            pool.add(EnemyType.RAG_BOMBER)
        }
        if (floor >= 4) {
            pool.add(EnemyType.RUSTY_TIN_KNIGHT)
        }
        val list = mutableListOf<EnemySpawn>()
        for (i in 0 until count) {
            val type = when {
                mechanic == RoomMechanic.DONT_BLINK_STATUES && i < 2 -> EnemyType.WEEPING_TOY
                mechanic == RoomMechanic.MIRROR_COPIES && i < 2 -> EnemyType.MIRROR_COPY
                else -> pool.random(rng)
            }
            var nx: Float
            var ny: Float
            var tries = 0
            do {
                nx = 0.16f + rng.nextFloat() * 0.68f
                ny = 0.18f + rng.nextFloat() * 0.62f
                tries++
                val insideObs = obstacles.any { o ->
                    nx in (o.x - 0.04f)..(o.x + o.w + 0.04f) && ny in (o.y - 0.04f)..(o.y + o.h + 0.04f)
                }
            } while ((insideObs || (abs(nx - 0.5f) < 0.12f && abs(ny - 0.5f) < 0.12f)) && tries < 16)
            list.add(EnemySpawn(type, nx, ny))
        }
        return list
    }

    private fun buildShopStock(rng: Random): List<ShopOffer> {
        val part = GameCatalog.bodyParts.filter { it.rarity.ordinal >= 1 }.random(rng)
        val item = GameCatalog.passiveItems.random(rng)
        return listOf(
            ShopOffer("shop_heal", "Corazón de Algodón (+1 Vida)", price = 4, isHeal = true),
            ShopOffer("shop_kit", "Kit de Costura (+45 Durab.)", price = 4, isConsumable = true, itemId = "SEWING_KIT"),
            ShopOffer("shop_part", "${part.name} (${part.zone.shortLabel})", price = 8, isBodyPart = true, itemId = part.id),
            ShopOffer("shop_item", item.name, price = 10, itemId = item.id)
        )
    }
}
