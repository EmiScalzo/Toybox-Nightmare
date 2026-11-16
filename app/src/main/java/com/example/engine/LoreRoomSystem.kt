package com.example.engine

import com.example.data.catalog.AmpliacionCatalog
import com.example.data.catalog.LoreRoomTemplate
import com.example.data.model.*
import kotlin.random.Random

object LoreRoomSystem {

    fun generateForRoom(
        floorNumber: Int,
        coord: GridCoord,
        obstacles: List<ObstacleRect>,
        allocId: () -> Long,
        rng: Random = Random.Default,
        forcedTemplate: LoreRoomTemplate? = null
    ): LoreRoomState {
        val templates = AmpliacionCatalog.loreTemplates
        val pickIdx = (kotlin.math.abs(coord.x * 5 + coord.y * 11 + floorNumber * 3) + rng.nextInt(templates.size)) % templates.size
        val tmpl = forcedTemplate ?: templates[pickIdx]

        fun safeSpot(nx: Float, ny: Float): Pair<Float, Float> {
            val rawX = (nx * ARENA_W).coerceIn(WALL_THICKNESS + 52f, ARENA_W - WALL_THICKNESS - 52f)
            val rawY = (ny * ARENA_H).coerceIn(WALL_THICKNESS + 52f, ARENA_H - WALL_THICKNESS - 52f)
            return RoomMechanicSystem.clampToObstacles(rawX, rawY, 36f, obstacles)
        }

        val props = mutableListOf<LoreProp>()
        when (tmpl.puzzleKind) {
            LorePuzzleKind.CHAIR_AND_BUTTON -> {
                val (cx, cy) = safeSpot(0.30f, 0.56f)
                val (sx, sy) = safeSpot(0.68f, 0.42f)
                val (bx, by) = safeSpot(0.50f, 0.24f)
                props += LoreProp(allocId(), LorePropKind.DRAGGABLE_CHAIR, cx, cy, 24f, "Silla Antigua", draggable = true)
                props += LoreProp(allocId(), LorePropKind.FLOOR_SWITCH, sx, sy, 28f, "Interruptor")
                props += LoreProp(allocId(), LorePropKind.SECRET_COMPARTMENT, bx, by, 26f, "Regalo Sellado")
            }

            LorePuzzleKind.STUCK_BOOKSHELF -> {
                val (tx, ty) = safeSpot(0.24f, 0.72f)
                val (shx, shy) = safeSpot(0.50f, 0.34f)
                val (cx, cy) = safeSpot(0.50f, 0.20f)
                props += LoreProp(allocId(), LorePropKind.TOOL_CROWBAR, tx, ty, 20f, "Palanca de Hierro")
                props += LoreProp(
                    allocId(), LorePropKind.STUCK_SHELF, shx, shy, 32f,
                    "Estantería Trabada", stateInt = 0, draggable = false, destructible = true, hp = 18f
                )
                props += LoreProp(allocId(), LorePropKind.SECRET_COMPARTMENT, cx, cy, 24f, "Hueco en la Pared")
            }

            LorePuzzleKind.CROOKED_PAINTING -> {
                val (px, py) = safeSpot(0.32f, 0.22f)
                val (dx, dy) = safeSpot(0.68f, 0.50f)
                props += LoreProp(allocId(), LorePropKind.ROTATABLE_PAINTING, px, py, 28f, "Retrato Torcido", stateInt = 2, targetInt = 0)
                props += LoreProp(allocId(), LorePropKind.SECRET_COMPARTMENT, dx, dy, 26f, "Cajón Cerrado")
            }

            LorePuzzleKind.MUSIC_BOX_REPAIR -> {
                val (mx, my) = safeSpot(0.50f, 0.38f)
                val (kx, ky) = safeSpot(0.24f, 0.68f)
                val (gx, gy) = safeSpot(0.76f, 0.68f)
                props += LoreProp(allocId(), LorePropKind.BROKEN_MUSIC_BOX, mx, my, 28f, "Caja Musical", stateInt = 0, targetInt = 2)
                props += LoreProp(allocId(), LorePropKind.TOOL_CRANK, kx, ky, 20f, "Manivela")
                props += LoreProp(allocId(), LorePropKind.TOOL_GEAR, gx, gy, 20f, "Engranaje")
            }

            LorePuzzleKind.FOUR_CLOCKS -> {
                val (hx, hy) = safeSpot(0.50f, 0.20f)
                props += LoreProp(
                    allocId(), LorePropKind.WALL_HINT_PANEL, hx, hy, 24f,
                    "Nota Horaria", hintText = "Nota grabada: «Cuando las 4 agujas marquen las 3:00, el armario cederá»."
                )
                val coords = listOf(0.28f to 0.38f, 0.72f to 0.38f, 0.28f to 0.68f, 0.72f to 0.68f)
                val initials = listOf(12, 6, 9, 12)
                coords.forEachIndexed { idx, (nx, ny) ->
                    val (clx, cly) = safeSpot(nx, ny)
                    props += LoreProp(allocId(), LorePropKind.ADJUSTABLE_CLOCK, clx, cly, 26f, "Reloj ${idx + 1}", stateInt = initials[idx], targetInt = 3)
                }
            }

            LorePuzzleKind.TRAPPED_TOY -> {
                val (cx, cy) = safeSpot(0.50f, 0.38f)
                val (kx, ky) = safeSpot(0.78f, 0.74f)
                props += LoreProp(allocId(), LorePropKind.TRAPPED_DOLL_CAGE, cx, cy, 32f, "Jaula del Osito", destructible = true, hp = 24f)
                props += LoreProp(allocId(), LorePropKind.CAGE_KEY, kx, ky, 20f, "Llave Oxidada")
            }

            LorePuzzleKind.FLOODED_FLOOR_PATH -> {
                val (lx, ly) = safeSpot(0.76f, 0.50f)
                val (ex1, ey1) = safeSpot(0.54f, 0.40f)
                val (ex2, ey2) = safeSpot(0.54f, 0.60f)
                val (cx1, cy1) = safeSpot(0.28f, 0.36f)
                val (cx2, cy2) = safeSpot(0.28f, 0.64f)
                props += LoreProp(allocId(), LorePropKind.ELECTRIFIED_ZONE, ex1, ey1, 36f, "Cable Chispeante")
                props += LoreProp(allocId(), LorePropKind.ELECTRIFIED_ZONE, ex2, ey2, 36f, "Cable Chispeante")
                props += LoreProp(allocId(), LorePropKind.DRAGGABLE_CRATE, cx1, cy1, 24f, "Caja Aislante", draggable = true)
                props += LoreProp(allocId(), LorePropKind.DRAGGABLE_CRATE, cx2, cy2, 24f, "Caja Aislante", draggable = true)
                props += LoreProp(allocId(), LorePropKind.POWER_LEVER, lx, ly, 26f, "Palanca de Corte")
            }

            LorePuzzleKind.THREE_DOLLS_DIRECTION -> {
                val (hx, hy) = safeSpot(0.50f, 0.20f)
                props += LoreProp(
                    allocId(), LorePropKind.WALL_HINT_PANEL, hx, hy, 24f,
                    "Boceto Mural", hintText = "Mural: Izquierda mira al NORTE (↑), Centro al ESTE (→), Derecha al OESTE (←)."
                )
                val targets = listOf(0, 1, 3)
                val starts = listOf(2, 3, 1)
                val xs = listOf(0.28f, 0.50f, 0.72f)
                xs.forEachIndexed { idx, nx ->
                    val (dx, dy) = safeSpot(nx, 0.50f)
                    props += LoreProp(allocId(), LorePropKind.ROTATABLE_DOLL, dx, dy, 26f, "Muñeca ${idx + 1}", stateInt = starts[idx], targetInt = targets[idx])
                }
            }

            LorePuzzleKind.HOLLOW_FLOOR -> {
                val hollowIdx = rng.nextInt(3)
                val positions = listOf(0.30f to 0.52f, 0.50f to 0.62f, 0.70f to 0.52f)
                positions.forEachIndexed { idx, (nx, ny) ->
                    val (tx, ty) = safeSpot(nx, ny)
                    val kind = if (idx == hollowIdx) LorePropKind.FLOOR_TILE_HOLLOW else LorePropKind.FLOOR_TILE_TRAP
                    props += LoreProp(
                        allocId(),
                        kind,
                        tx,
                        ty,
                        30f,
                        "Baldosa ${idx + 1}",
                        destructible = (kind == LorePropKind.FLOOR_TILE_HOLLOW),
                        hp = 14f
                    )
                }
            }

            LorePuzzleKind.THREE_TOYS_PEDESTALS -> {
                val (hx, hy) = safeSpot(0.50f, 0.19f)
                props += LoreProp(
                    allocId(), LorePropKind.WALL_HINT_PANEL, hx, hy, 24f,
                    "Plano de Vitrina", hintText = "Orden sagrado: 1. Soldado • 2. Muñeca • 3. Conejo."
                )
                val pedXs = listOf(0.28f, 0.50f, 0.72f)
                pedXs.forEachIndexed { idx, nx ->
                    val (px, py) = safeSpot(nx, 0.38f)
                    props += LoreProp(allocId(), LorePropKind.PEDESTAL_SLOT, px, py, 26f, "Pedestal ${idx + 1}", targetInt = idx + 1)
                }
                val (sx, sy) = safeSpot(0.74f, 0.72f)
                val (dx, dy) = safeSpot(0.26f, 0.72f)
                val (bx, by) = safeSpot(0.50f, 0.76f)
                props += LoreProp(allocId(), LorePropKind.TOY_FIGURE_SOLDIER, sx, sy, 20f, "Figura: Soldado", figureToken = "SOLDIER")
                props += LoreProp(allocId(), LorePropKind.TOY_FIGURE_DOLL, dx, dy, 20f, "Figura: Muñeca", figureToken = "DOLL")
                props += LoreProp(allocId(), LorePropKind.TOY_FIGURE_BUNNY, bx, by, 20f, "Figura: Conejo", figureToken = "BUNNY")
            }
        }

        // Environmental diary desk + destructible crate (may contain a fragile photograph destroyed if bombed!)
        val (deskX, deskY) = safeSpot(0.18f, 0.24f)
        val (crateX, crateY) = safeSpot(0.82f, 0.24f)
        props += LoreProp(
            allocId(), LorePropKind.DIARY_DESK, deskX, deskY, 24f,
            "Escritorio de Archivo",
            hintText = when (tmpl.loreRoomType) {
                LoreRoomType.MEMORY -> "Diario antiguo: «Ningún regalo debía abrirse antes de medianoche»."
                LoreRoomType.HISTORICAL -> "Albarán de 1958: «Enviar 40 carretes de hilo dorado al sótano»."
                LoreRoomType.CHARACTER -> "Nota de calidad: «El modelo olvidado no dejaba de mirar la puerta»."
                LoreRoomType.ANOMALOUS -> "Registro anómalo: «Las manecillas retrocedieron solas al cerrar la tienda»."
                LoreRoomType.DOCUMENTAL -> "Memorándum: «No usar explosivos cerca de las cajas con fotografías»."
                LoreRoomType.FORBIDDEN -> "Sello de El Fabricante: «Todo recuerdo pesa; guárdalo bien o se deshilacha»."
            }
        )
        props += LoreProp(
            allocId(),
            LorePropKind.DESTRUCTIBLE_CRATE,
            crateX,
            crateY,
            24f,
            "Caja de Archivo",
            destructible = true,
            fragilePhotoInside = true,
            hp = 18f
        )

        return LoreRoomState(
            loreRoomType = tmpl.loreRoomType,
            templateId = tmpl.id,
            title = tmpl.title,
            subtitle = tmpl.subtitle,
            difficulty = tmpl.difficulty,
            puzzleKind = tmpl.puzzleKind,
            hintSummary = tmpl.hintSummary,
            props = props,
            rewardArchiveId = tmpl.rewardArchiveId
        )
    }
}
