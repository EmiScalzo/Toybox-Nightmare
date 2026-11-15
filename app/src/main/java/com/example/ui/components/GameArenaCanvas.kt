package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.data.catalog.GameCatalog
import com.example.data.model.*
import com.example.engine.*
import com.example.ui.theme.*
import kotlin.math.*
import kotlin.random.Random

@Composable
fun GameArenaCanvas(
    state: GameRunState,
    vignetteStrength: Float,
    screenShakeEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val scale = min(size.width / ARENA_W, size.height / ARENA_H)
        val arenaPxW = ARENA_W * scale
        val arenaPxH = ARENA_H * scale
        val baseOffX = (size.width - arenaPxW) / 2f
        val baseOffY = (size.height - arenaPxH) / 2f

        val shakeAmp = if (screenShakeEnabled) state.screenShake else 0f
        val shakeX = if (shakeAmp > 0.2f) (Random.nextFloat() - 0.5f) * shakeAmp else 0f
        val shakeY = if (shakeAmp > 0.2f) (Random.nextFloat() - 0.5f) * shakeAmp else 0f
        val ox = baseOffX + shakeX
        val oy = baseOffY + shakeY

        fun ax(x: Float) = ox + x * scale
        fun ay(y: Float) = oy + y * scale
        fun asz(v: Float) = v * scale

        drawRect(color = CharcoalBlack)

        val floorColor = Color(state.floorTheme.floorColorHex)
        val wallColor = Color(state.floorTheme.wallColorHex)
        drawRect(color = floorColor, topLeft = Offset(ox, oy), size = Size(arenaPxW, arenaPxH))

        val wallPx = asz(WALL_THICKNESS)
        val innerLeft = ox + wallPx
        val innerTop = oy + wallPx
        val innerRight = ox + arenaPxW - wallPx
        val innerBottom = oy + arenaPxH - wallPx
        val room = state.currentRoom
        val roomSeed = room.coord.x * 73856093 xor room.coord.y * 19349663 xor (state.floorNumber * 83492791)

        // Varied Floor Tilesets per Room Visual Variant & Floor
        drawRoomFloorPattern(
            variant = (room.visualVariant + state.floorNumber) % 4,
            roomType = room.type,
            roomSeed = roomSeed,
            innerLeft = innerLeft,
            innerTop = innerTop,
            innerRight = innerRight,
            innerBottom = innerBottom,
            tileSize = asz(72f),
            scale = scale
        )

        // Room-specific ambient & decorative storytelling props (Sewing Room, Curse, Shop, Boss, etc.)
        drawRoomDecorations(
            room = room,
            elapsed = state.elapsedSeconds,
            ox = ox,
            oy = oy,
            scale = scale,
            innerLeft = innerLeft,
            innerTop = innerTop,
            innerRight = innerRight,
            innerBottom = innerBottom
        )

        // Dynamic Room Mechanic Floor Overlays (Vanishing Sectors, Tilted Floor, Mirrors, Carousel, Gaze Cone)
        drawRoomMechanicFloor(
            state = state,
            ox = ox,
            oy = oy,
            scale = scale,
            innerLeft = innerLeft,
            innerTop = innerTop,
            innerRight = innerRight,
            innerBottom = innerBottom
        )

        // Walls & Trim
        drawRect(wallColor, Offset(ox, oy), Size(arenaPxW, wallPx))
        drawRect(wallColor, Offset(ox, oy + arenaPxH - wallPx), Size(arenaPxW, wallPx))
        drawRect(wallColor, Offset(ox, oy), Size(wallPx, arenaPxH))
        drawRect(wallColor, Offset(ox + arenaPxW - wallPx, oy), Size(wallPx, arenaPxH))

        val trimColor = when {
            room.mechanic == RoomMechanic.MIRROR_COPIES -> Color(0xFFB7C9D6).copy(alpha = 0.75f)
            room.type == RoomType.REPAIR -> MutedMoss.copy(alpha = 0.72f)
            room.type == RoomType.CURSE || room.type == RoomType.BOSS || room.type == RoomType.FORBIDDEN -> CrimsonBright.copy(alpha = 0.65f)
            room.type == RoomType.TREASURE || room.type == RoomType.SECRET || room.type == RoomType.MANUFACTURER -> TarnishedBrass.copy(alpha = 0.75f)
            else -> TarnishedBrassDim.copy(alpha = 0.55f)
        }
        drawRect(
            color = trimColor,
            topLeft = Offset(innerLeft, innerTop),
            size = Size(arenaPxW - wallPx * 2f, arenaPxH - wallPx * 2f),
            style = Stroke(3f)
        )

        // Corner Cobwebs (except warm tidy repair room)
        if (room.type != RoomType.REPAIR) {
            drawCobweb(innerLeft, innerTop, asz(65f), 1f, 1f)
            drawCobweb(innerRight, innerTop, asz(65f), -1f, 1f)
            drawCobweb(innerLeft, innerBottom, asz(65f), 1f, -1f)
            drawCobweb(innerRight, innerBottom, asz(65f), -1f, -1f)
        }

        // Doors (respecting SHIFTING_DOORS relocated physical slots)
        val cleared = room.cleared
        val isShiftingDoors = room.mechanic == RoomMechanic.SHIFTING_DOORS
        val shiftImminent = isShiftingDoors && state.doorShiftTimer < 1.35f && ((state.elapsedSeconds * 10f).toInt() % 2 == 0)
        val doorCol = when {
            shiftImminent -> AmberWarning
            cleared -> TarnishedBrass
            else -> CrimsonBlood
        }
        val doorFill = if (cleared) Color(0xFF0A0808) else Color(0xFF3E241C)
        val doorW = asz(112f)
        val doorH = asz(46f)
        val activePhysicalDoors = if (isShiftingDoors) {
            state.doorMapping.filterValues { it in room.doors }.keys
        } else {
            room.doors
        }
        for (dir in activePhysicalDoors) {
            when (dir) {
                Direction.NORTH -> {
                    drawRect(doorFill, Offset(ax(ARENA_W / 2f) - doorW / 2f, oy + asz(14f)), Size(doorW, doorH))
                    drawRect(doorCol, Offset(ax(ARENA_W / 2f) - doorW / 2f, oy + asz(14f)), Size(doorW, doorH), style = Stroke(3.5f))
                }
                Direction.SOUTH -> {
                    drawRect(doorFill, Offset(ax(ARENA_W / 2f) - doorW / 2f, oy + arenaPxH - asz(60f)), Size(doorW, doorH))
                    drawRect(doorCol, Offset(ax(ARENA_W / 2f) - doorW / 2f, oy + arenaPxH - asz(60f)), Size(doorW, doorH), style = Stroke(3.5f))
                }
                Direction.WEST -> {
                    drawRect(doorFill, Offset(ox + asz(14f), ay(ARENA_H / 2f) - doorW / 2f), Size(doorH, doorW))
                    drawRect(doorCol, Offset(ox + asz(14f), ay(ARENA_H / 2f) - doorW / 2f), Size(doorH, doorW), style = Stroke(3.5f))
                }
                Direction.EAST -> {
                    drawRect(doorFill, Offset(ox + arenaPxW - asz(60f), ay(ARENA_H / 2f) - doorW / 2f), Size(doorH, doorW))
                    drawRect(doorCol, Offset(ox + arenaPxW - asz(60f), ay(ARENA_H / 2f) - doorW / 2f), Size(doorH, doorW), style = Stroke(3.5f))
                }
            }
        }

        // Secret wall crack
        for (sDir in room.secretDoors) {
            val sx = when (sDir) {
                Direction.WEST -> ax(WALL_THICKNESS - 12f)
                Direction.EAST -> ax(ARENA_W - WALL_THICKNESS + 6f)
                else -> ax(ARENA_W / 2f)
            }
            val sy = when (sDir) {
                Direction.NORTH -> ay(WALL_THICKNESS - 12f)
                Direction.SOUTH -> ay(ARENA_H - WALL_THICKNESS + 6f)
                else -> ay(ARENA_H / 2f)
            }
            drawLine(TarnishedBrass.copy(alpha = 0.65f), Offset(sx - 12f, sy - 8f), Offset(sx + 10f, sy + 10f), 3f)
        }

        // Exit Trapdoor
        if (state.exitTrapdoorOpen) {
            val tx = ax(ARENA_W * 0.5f)
            val ty = ay(ARENA_H * 0.24f)
            drawCircle(CharcoalBlack, asz(34f), Offset(tx, ty))
            drawCircle(TarnishedBrass, asz(34f), Offset(tx, ty), style = Stroke(4f))
            drawCircle(TarnishedBrass.copy(alpha = 0.4f), asz(20f), Offset(tx, ty))
        }

        // Distinct Pixel-Art Obstacles (Crates, Vitrines, Sewing Tables, Pillars, Toy Piles)
        for (obs in room.obstacles) {
            val rLeft = ax(obs.x * ARENA_W)
            val rTop = ay(obs.y * ARENA_H)
            val rW = asz(obs.w * ARENA_W)
            val rH = asz(obs.h * ARENA_H)
            val obsBmp = when (obs.kind) {
                1 -> PixelArtSprites.vitrineObstacle()
                2 -> PixelArtSprites.sewingTableProp()
                3 -> PixelArtSprites.pillarObstacle()
                4 -> PixelArtSprites.plushPileObstacle()
                5 -> PixelArtSprites.nurseryBlockObstacle()
                else -> PixelArtSprites.crateObstacle()
            }
            drawRect(Color.Black.copy(alpha = 0.42f), Offset(rLeft + 5f, rTop + 7f), Size(rW, rH))
            drawImage(
                image = obsBmp,
                dstOffset = IntOffset(rLeft.toInt(), rTop.toInt()),
                dstSize = IntSize(rW.toInt().coerceAtLeast(1), rH.toInt().coerceAtLeast(1)),
                filterQuality = FilterQuality.None
            )
        }

        // Physical Secret Anomalies (crooked frames, gazing dolls, irregular clocks, knocking walls, wrong shadows)
        for (anom in state.roomAnomalies) {
            val axPos = ax(anom.x)
            val ayPos = ay(anom.y)
            if (!anom.resolved) {
                val pulse = 0.45f + 0.35f * sin(state.elapsedSeconds * 5f + anom.id)
                val ringCol = if (anom.inspected) TarnishedBrass else TarnishedBrassDim.copy(alpha = 0.45f * pulse)
                drawCircle(ringCol, asz(26f), Offset(axPos, ayPos), style = Stroke(2f))
            }
            val anomBmp = PixelArtSprites.forSecretClue(anom.clueType)
            val aSz = asz(36f)
            drawImage(
                image = anomBmp,
                dstOffset = IntOffset((axPos - aSz / 2f).toInt(), (ayPos - aSz / 2f).toInt()),
                dstSize = IntSize(aSz.toInt().coerceAtLeast(1), aSz.toInt().coerceAtLeast(1)),
                alpha = if (anom.resolved) 0.5f else 1f,
                filterQuality = FilterQuality.None
            )
        }

        // Alarm Bell Prop (used by Coward enemies)
        state.alarmBellPos?.let { (bx, by) ->
            val bellBmp = PixelArtSprites.forObjectiveProp("alarm_bell")
            val bSz = asz(30f)
            drawCircle(AmberWarning.copy(alpha = 0.2f), asz(20f), Offset(ax(bx), ay(by)))
            drawImage(
                image = bellBmp,
                dstOffset = IntOffset((ax(bx) - bSz / 2f).toInt(), (ay(by) - bSz / 2f).toInt()),
                dstSize = IntSize(bSz.toInt().coerceAtLeast(1), bSz.toInt().coerceAtLeast(1)),
                filterQuality = FilterQuality.None
            )
        }

        // Interactive Room Elements (Movables, Activables, Destructibles, Ambient Hazards)
        for (inter in state.roomInteractables) {
            val ix = ax(inter.x)
            val iy = ay(inter.y)
            val ir = asz(inter.radius)

            // Draw train track oval before train sprite
            if (inter.type == InteractableType.ELECTRIC_TRAIN) {
                drawOval(
                    color = TarnishedBrassDim.copy(alpha = 0.55f),
                    topLeft = Offset(ax(ARENA_W * 0.5f - 235f), ay(ARENA_H * 0.5f - 210f)),
                    size = Size(asz(470f), asz(420f)),
                    style = Stroke(4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f))
                )
            }

            if (inter.type == InteractableType.TOY_CAROUSEL && inter.active && inter.activeTimer > 0f) {
                val pulse = 0.55f + 0.45f * sin(state.elapsedSeconds * 10f)
                drawCircle(CrimsonBright.copy(alpha = 0.16f * pulse), asz(145f), Offset(ix, iy))
                drawCircle(TarnishedBrass.copy(alpha = 0.55f), asz(145f), Offset(ix, iy), style = Stroke(2.5f))
            }

            if ((inter.type == InteractableType.LIVE_WIRE && inter.active && inter.activeTimer > 0f) ||
                inter.electrifiedTimer > 0f
            ) {
                val sparkAlpha = 0.4f + 0.4f * abs(sin(state.elapsedSeconds * 16f))
                drawCircle(Color(0xFF4CC9F0).copy(alpha = 0.25f * sparkAlpha), ir * 1.6f, Offset(ix, iy))
                drawCircle(Color(0xFF90E0EF).copy(alpha = sparkAlpha), ir * 1.4f, Offset(ix, iy), style = Stroke(2.5f))
            }

            if (inter.type == InteractableType.HANGING_SHELF && !inter.triggered && !inter.destroyed) {
                // Target drop shadow under hanging shelf
                drawOval(Color.Black.copy(alpha = 0.35f), Offset(ix - ir, iy + ir * 0.4f), Size(ir * 2f, ir * 0.7f))
                drawCircle(CrimsonBright.copy(alpha = 0.35f), ir * 1.25f, Offset(ix, iy), style = Stroke(2f))
            }

            if (inter.type == InteractableType.STEAM_VENT && inter.active) {
                drawCircle(CottonWhite.copy(alpha = 0.24f), ir * 1.7f, Offset(ix, iy))
            }

            val bmp = PixelArtSprites.forInteractable(inter.type, active = inter.active, destroyed = inter.destroyed)
            val spriteSz = ir * 2.15f
            val rotDeg = if (inter.type == InteractableType.TOY_CAROUSEL && inter.active) state.elapsedSeconds * 180f else 0f
            rotate(rotDeg, Offset(ix, iy)) {
                drawImage(
                    image = bmp,
                    dstOffset = IntOffset((ix - spriteSz / 2f).toInt(), (iy - spriteSz / 2f).toInt()),
                    dstSize = IntSize(spriteSz.toInt().coerceAtLeast(1), spriteSz.toInt().coerceAtLeast(1)),
                    alpha = if (inter.destroyed) 0.58f else 1f,
                    filterQuality = FilterQuality.None
                )
            }
        }

        // Alternative Room Objective Visuals (Crying Box, Lost Toy, Escort Parcel, 3 Windups, Last Chair Safe Zone)
        state.roomObjective?.let { obj ->
            if (!obj.completed && !obj.failed) {
                when (obj.type) {
                    ObjectiveType.LAST_CHAIR_SAFE_ZONE -> {
                        val zx = ax(obj.safeZoneX)
                        val zy = ay(obj.safeZoneY)
                        val r = asz(84f)
                        val alpha = if (obj.safeZoneActive) 0.34f + 0.2f * abs(sin(state.elapsedSeconds * 8f)) else 0.14f
                        val col = if (obj.safeZoneActive) TarnishedBrass else MutedMoss
                        drawCircle(col.copy(alpha = alpha), r, Offset(zx, zy))
                        drawCircle(col.copy(alpha = 0.85f), r, Offset(zx, zy), style = Stroke(if (obj.safeZoneActive) 4f else 2f))
                    }
                    ObjectiveType.PROTECT_CRYING_BOX -> {
                        val cx = ax(obj.entityX)
                        val cy = ay(obj.entityY)
                        val sz = asz(46f)
                        drawCircle(CrimsonBright.copy(alpha = 0.2f), asz(34f), Offset(cx, cy), style = Stroke(2f))
                        drawImage(
                            PixelArtSprites.forObjectiveProp("crying_box"),
                            dstOffset = IntOffset((cx - sz / 2f).toInt(), (cy - sz / 2f).toInt()),
                            dstSize = IntSize(sz.toInt().coerceAtLeast(1), sz.toInt().coerceAtLeast(1)),
                            filterQuality = FilterQuality.None
                        )
                        val ratio = (obj.entityHp / obj.entityMaxHp.coerceAtLeast(1f)).coerceIn(0f, 1f)
                        drawRect(CharcoalBlack, Offset(cx - asz(26f), cy - asz(32f)), Size(asz(52f), 5f))
                        drawRect(MutedMoss, Offset(cx - asz(26f), cy - asz(32f)), Size(asz(52f) * ratio, 5f))
                    }
                    ObjectiveType.CATCH_LOST_TOY -> {
                        val lx = ax(obj.entityX)
                        val ly = ay(obj.entityY)
                        val sz = asz(38f)
                        drawCircle(TarnishedBrass.copy(alpha = 0.28f), asz(26f), Offset(lx, ly))
                        drawImage(
                            PixelArtSprites.forCompanion(CompanionId.BUNNY),
                            dstOffset = IntOffset((lx - sz / 2f).toInt(), (ly - sz / 2f).toInt()),
                            dstSize = IntSize(sz.toInt().coerceAtLeast(1), sz.toInt().coerceAtLeast(1)),
                            filterQuality = FilterQuality.None
                        )
                    }
                    ObjectiveType.ESCORT_PACKAGE -> {
                        val dx = ax(obj.deliveryX)
                        val dy = ay(obj.deliveryY)
                        drawCircle(MutedMoss.copy(alpha = 0.25f), asz(44f), Offset(dx, dy))
                        drawCircle(MutedMoss, asz(44f), Offset(dx, dy), style = Stroke(3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)))

                        val pxPos = ax(obj.entityX)
                        val pyPos = ay(obj.entityY)
                        val sz = asz(42f)
                        drawImage(
                            PixelArtSprites.forObjectiveProp("delivery_box"),
                            dstOffset = IntOffset((pxPos - sz / 2f).toInt(), (pyPos - sz / 2f).toInt()),
                            dstSize = IntSize(sz.toInt().coerceAtLeast(1), sz.toInt().coerceAtLeast(1)),
                            filterQuality = FilterQuality.None
                        )
                        val ratio = (obj.entityHp / obj.entityMaxHp.coerceAtLeast(1f)).coerceIn(0f, 1f)
                        drawRect(CharcoalBlack, Offset(pxPos - asz(24f), pyPos - asz(30f)), Size(asz(48f), 5f))
                        drawRect(TarnishedBrass, Offset(pxPos - asz(24f), pyPos - asz(30f)), Size(asz(48f) * ratio, 5f))
                    }
                    ObjectiveType.WIND_THREE_MECHANISMS -> {
                        obj.windupPositions.forEachIndexed { idx, (wx, wy) ->
                            val wound = idx in obj.woundIndices
                            val wpx = ax(wx)
                            val wpy = ay(wy)
                            drawCircle(if (wound) MutedMoss.copy(alpha = 0.3f) else TarnishedBrass.copy(alpha = 0.28f), asz(26f), Offset(wpx, wpy))
                            drawCircle(if (wound) MutedMoss else TarnishedBrass, asz(22f), Offset(wpx, wpy), style = Stroke(2.5f))
                            val sz = asz(28f)
                            drawImage(
                                PixelArtSprites.forInteractable(InteractableType.WINDUP_JACK_TRAP, active = wound),
                                dstOffset = IntOffset((wpx - sz / 2f).toInt(), (wpy - sz / 2f).toInt()),
                                dstSize = IntSize(sz.toInt().coerceAtLeast(1), sz.toInt().coerceAtLeast(1)),
                                filterQuality = FilterQuality.None
                            )
                        }
                    }
                    else -> Unit
                }
            }
        }

        // El Fabricante NPC
        state.manufacturerNpc?.let { npc ->
            if (npc.present) {
                val nx = ax(npc.x)
                val ny = ay(npc.y)
                val aura = 0.5f + 0.35f * sin(state.elapsedSeconds * 3f)
                drawCircle(TarnishedBrass.copy(alpha = 0.22f * aura), asz(44f), Offset(nx, ny))
                drawCircle(CrimsonBright.copy(alpha = 0.45f), asz(36f), Offset(nx, ny), style = Stroke(2.5f))
                val bmp = PixelArtSprites.manufacturerNpc()
                val sz = asz(58f)
                drawImage(
                    image = bmp,
                    dstOffset = IntOffset((nx - sz / 2f).toInt(), (ny - sz / 2f).toInt()),
                    dstSize = IntSize(sz.toInt().coerceAtLeast(1), sz.toInt().coerceAtLeast(1)),
                    alpha = if (npc.interacted) 0.65f else 1f,
                    filterQuality = FilterQuality.None
                )
            }
        }

        // Ground Pickups & Pedestals with Representative Pixel Icons
        val bobFloat = sin(state.elapsedSeconds * 4.5f) * asz(4f)
        for (p in state.pickups) {
            val px = ax(p.x)
            val py = ay(p.y)
            when (p.kind) {
                PickupKind.BUTTON, PickupKind.GOLD_BUTTON -> {
                    val bmp = PixelArtSprites.pickupButton(p.kind == PickupKind.GOLD_BUTTON)
                    val sz = asz(if (p.kind == PickupKind.GOLD_BUTTON) 30f else 24f)
                    drawImage(
                        bmp,
                        dstOffset = IntOffset((px - sz / 2f).toInt(), (py - sz / 2f).toInt()),
                        dstSize = IntSize(sz.toInt(), sz.toInt()),
                        filterQuality = FilterQuality.None
                    )
                }
                PickupKind.HEART -> {
                    val bmp = PixelArtSprites.pickupHeart()
                    val sz = asz(26f)
                    drawImage(
                        bmp,
                        dstOffset = IntOffset((px - sz / 2f).toInt(), (py - sz / 2f).toInt()),
                        dstSize = IntSize(sz.toInt(), sz.toInt()),
                        filterQuality = FilterQuality.None
                    )
                }
                PickupKind.CONSUMABLE -> {
                    val cons = runCatching { ConsumableType.valueOf(p.valueStr) }.getOrNull() ?: ConsumableType.BOMB
                    val bmp = PixelArtSprites.forConsumable(cons)
                    val sz = asz(34f)
                    drawCircle(TarnishedBrass.copy(alpha = 0.22f), asz(18f), Offset(px, py))
                    drawImage(
                        bmp,
                        dstOffset = IntOffset((px - sz / 2f).toInt(), (py - sz / 2f).toInt()),
                        dstSize = IntSize(sz.toInt().coerceAtLeast(1), sz.toInt().coerceAtLeast(1)),
                        filterQuality = FilterQuality.None
                    )
                }
                PickupKind.DETACHED_PART -> {
                    val pulse = 0.7f + 0.3f * sin(state.elapsedSeconds * 10f)
                    drawCircle(CrimsonBright.copy(alpha = 0.45f * pulse), asz(25f), Offset(px, py))
                    val dp = p.detachedPart
                    if (dp != null) {
                        val bmp = PixelArtSprites.forBodyPart(dp.def.id, dp.def.zone)
                        val sz = asz(32f)
                        drawImage(
                            bmp,
                            dstOffset = IntOffset((px - sz / 2f).toInt(), (py - sz / 2f).toInt()),
                            dstSize = IntSize(sz.toInt().coerceAtLeast(1), sz.toInt().coerceAtLeast(1)),
                            filterQuality = FilterQuality.None
                        )
                    } else {
                        drawCircle(TarnishedBrass, asz(13f), Offset(px, py))
                    }
                }
                PickupKind.NEW_PART_PEDESTAL -> {
                    val part = GameCatalog.partById(p.valueStr)
                    drawRect(AgedWoodLight, Offset(px - asz(22f), py - asz(4f)), Size(asz(44f), asz(24f)))
                    drawRect(CharcoalBlack, Offset(px - asz(22f), py - asz(4f)), Size(asz(44f), asz(24f)), style = Stroke(2.5f))
                    drawCircle(MutedMoss.copy(alpha = 0.30f), asz(26f), Offset(px, py - asz(18f)))
                    val bmp = PixelArtSprites.forBodyPart(part.id, part.zone)
                    val sz = asz(38f)
                    drawImage(
                        bmp,
                        dstOffset = IntOffset((px - sz / 2f).toInt(), (py - asz(20f) - sz / 2f + bobFloat).toInt()),
                        dstSize = IntSize(sz.toInt().coerceAtLeast(1), sz.toInt().coerceAtLeast(1)),
                        filterQuality = FilterQuality.None
                    )
                }
                PickupKind.PASSIVE_ITEM_PEDESTAL -> {
                    drawRect(AgedWoodLight, Offset(px - asz(22f), py - asz(4f)), Size(asz(44f), asz(24f)))
                    drawRect(TarnishedBrass, Offset(px - asz(22f), py - asz(4f)), Size(asz(44f), asz(24f)), style = Stroke(2.5f))
                    drawCircle(TarnishedBrass.copy(alpha = 0.32f), asz(28f), Offset(px, py - asz(18f)))
                    val bmp = PixelArtSprites.forPassiveItem(p.valueStr)
                    val sz = asz(40f)
                    drawImage(
                        bmp,
                        dstOffset = IntOffset((px - sz / 2f).toInt(), (py - asz(20f) - sz / 2f + bobFloat).toInt()),
                        dstSize = IntSize(sz.toInt().coerceAtLeast(1), sz.toInt().coerceAtLeast(1)),
                        filterQuality = FilterQuality.None
                    )
                }
                PickupKind.COMPANION_SHRINE -> {
                    val cid = runCatching { CompanionId.valueOf(p.valueStr) }.getOrNull()
                    drawCircle(MutedMoss.copy(alpha = 0.28f), asz(30f), Offset(px, py))
                    drawCircle(TarnishedBrass.copy(alpha = 0.65f), asz(24f), Offset(px, py), style = Stroke(2.5f))
                    val bmp = cid?.let { PixelArtSprites.forCompanion(it) } ?: PixelArtSprites.companionBellProp()
                    val sz = asz(38f)
                    drawImage(
                        bmp,
                        dstOffset = IntOffset((px - sz / 2f).toInt(), (py - sz / 2f + bobFloat).toInt()),
                        dstSize = IntSize(sz.toInt().coerceAtLeast(1), sz.toInt().coerceAtLeast(1)),
                        filterQuality = FilterQuality.None
                    )
                }
                PickupKind.MATERIAL -> {
                    val mat = runCatching { RepairMaterial.valueOf(p.valueStr) }.getOrNull() ?: RepairMaterial.THREAD
                    val bmp = PixelArtSprites.forMaterial(mat)
                    val sz = asz(30f)
                    drawCircle(MutedMoss.copy(alpha = 0.26f), asz(17f), Offset(px, py))
                    drawImage(
                        bmp,
                        dstOffset = IntOffset((px - sz / 2f).toInt(), (py - sz / 2f + bobFloat).toInt()),
                        dstSize = IntSize(sz.toInt().coerceAtLeast(1), sz.toInt().coerceAtLeast(1)),
                        filterQuality = FilterQuality.None
                    )
                }
                PickupKind.GEAR_TOKEN -> {
                    val bmp = PixelArtSprites.forObjectiveProp("gear_token")
                    val sz = asz(34f)
                    drawCircle(TarnishedBrass.copy(alpha = 0.35f), asz(20f), Offset(px, py))
                    drawImage(
                        bmp,
                        dstOffset = IntOffset((px - sz / 2f).toInt(), (py - sz / 2f + bobFloat).toInt()),
                        dstSize = IntSize(sz.toInt().coerceAtLeast(1), sz.toInt().coerceAtLeast(1)),
                        filterQuality = FilterQuality.None
                    )
                }
            }
        }

        // Bombs
        for (b in state.bombs) {
            val bx = ax(b.x)
            val by = ay(b.y)
            val flash = sin(b.fuseSec * 22f) > 0f
            if (!b.fromPlayer) {
                drawCircle(CrimsonBright.copy(alpha = 0.22f), asz(b.radius), Offset(bx, by))
                drawCircle(CrimsonBright.copy(alpha = 0.55f), asz(b.radius), Offset(bx, by), style = Stroke(2f))
            }
            drawCircle(if (flash) CrimsonBright else CharcoalBlack, asz(14f), Offset(bx, by))
            drawCircle(TarnishedBrass, asz(14f), Offset(bx, by), style = Stroke(2.5f))
        }

        // Decoys
        for (d in state.decoys) {
            val dx = ax(d.x)
            val dy = ay(d.y)
            if (state.activeSynergies.any { it.effectId == "decoy_pulse" }) {
                drawCircle(CrimsonBlood.copy(alpha = 0.16f), asz(135f), Offset(dx, dy))
            }
            drawImage(
                PixelArtSprites.forPassiveItem("broken_doll"),
                dstOffset = IntOffset((dx - asz(18f)).toInt(), (dy - asz(18f)).toInt()),
                dstSize = IntSize(asz(36f).toInt().coerceAtLeast(1), asz(36f).toInt().coerceAtLeast(1)),
                alpha = 0.85f,
                filterQuality = FilterQuality.None
            )
        }

        // Pixel-Art Enemies & Bosses
        for (e in state.enemies) {
            val ex = ax(e.x)
            val ey = ay(e.y)
            val er = asz(e.radius)
            val bobY = if (e.disguised) 0f else sin(state.elapsedSeconds * 8f + e.id) * asz(3.5f)

            drawOval(
                Color.Black.copy(alpha = 0.45f),
                Offset(ex - er * 0.9f, ey + er * 0.62f),
                Size(er * 1.8f, er * 0.6f)
            )

            if (e.telegraphTimer > 0f) {
                drawCircle(CrimsonBright.copy(alpha = 0.32f), er * 1.55f, Offset(ex, ey))
            }

            if (state.toyboxMemory.enemiesAdaptDodge && !e.disguised && e.type == EnemyType.TIN_SOLDIER) {
                drawLine(
                    color = TarnishedBrass.copy(alpha = 0.25f),
                    start = Offset(ex, ey),
                    end = Offset(ax(state.playerX), ay(state.playerY)),
                    strokeWidth = 2f
                )
            }

            val bmp = PixelArtSprites.forEnemy(e.type, e.disguised)
            val spriteW = er * 2.5f
            val spriteH = spriteW * (bmp.height.toFloat() / bmp.width.toFloat())
            val tintFilter = when {
                e.hitFlashTimer > 0f -> ColorFilter.tint(CottonWhite)
                e.stunnedTimer > 0f -> ColorFilter.tint(SlateBlue.copy(alpha = 0.7f))
                else -> null
            }

            drawImage(
                image = bmp,
                dstOffset = IntOffset((ex - spriteW / 2f).toInt(), (ey - spriteH / 2f + bobY).toInt()),
                dstSize = IntSize(spriteW.toInt().coerceAtLeast(1), spriteH.toInt().coerceAtLeast(1)),
                colorFilter = tintFilter,
                filterQuality = FilterQuality.None
            )

            if (!e.type.isBoss && e.hp < e.maxHp) {
                val ratio = (e.hp / e.maxHp).coerceIn(0f, 1f)
                drawRect(CharcoalBlack, Offset(ex - er, ey - spriteH * 0.55f - 8f), Size(er * 2f, 5f))
                drawRect(CrimsonBright, Offset(ex - er, ey - spriteH * 0.55f - 8f), Size(er * 2f * ratio, 5f))
            }

            // Enemy Relationship Badges (Leader, Protector, Coward, Repairer, Disorganized, Stolen Part)
            val badgeY = ey - spriteH * 0.55f - 14f
            when {
                e.disorganized -> {
                    drawCircle(AmberWarning, asz(4.5f), Offset(ex - asz(6f), badgeY))
                    drawCircle(AmberWarning, asz(4.5f), Offset(ex + asz(6f), badgeY))
                }
                e.enraged -> {
                    drawCircle(CrimsonBright, asz(6f), Offset(ex, badgeY))
                }
                e.role == EnemyRole.LEADER -> {
                    drawCircle(TarnishedBrass, asz(6f), Offset(ex, badgeY))
                }
                e.role == EnemyRole.PROTECTOR && e.type == EnemyType.BODYGUARD_BEAR -> {
                    drawCircle(SlateBlue, asz(5.5f), Offset(ex, badgeY))
                }
                e.role == EnemyRole.COWARD -> {
                    drawCircle(AmberWarning, asz(5.5f), Offset(ex, badgeY))
                }
                e.role == EnemyRole.REPAIRER -> {
                    drawCircle(MutedMoss, asz(5.5f), Offset(ex, badgeY))
                }
            }
            if (e.carriedPart != null) {
                drawCircle(CrimsonBright, asz(5f), Offset(ex + er * 0.7f, ey - er * 0.6f))
            }
        }

        // Active Conditional Companions
        for (comp in state.activeCompanions) {
            val cx = ax(comp.x)
            val cy = ay(comp.y)
            val compBob = sin(state.elapsedSeconds * 7.5f + comp.id.ordinal) * asz(3f)
            drawOval(Color.Black.copy(alpha = 0.38f), Offset(cx - asz(13f), cy + asz(12f)), Size(asz(26f), asz(9f)))
            if (comp.id == CompanionId.RUSTY_BALLERINA && !comp.broken) {
                drawCircle(SlateBlue.copy(alpha = 0.14f), asz(170f), Offset(cx, cy))
                drawCircle(SlateBlue.copy(alpha = 0.32f), asz(170f), Offset(cx, cy), style = Stroke(2f))
            } else if (comp.id == CompanionId.FORGOTTEN_BEAR) {
                drawCircle(TarnishedBrass.copy(alpha = 0.22f), asz(22f), Offset(cx, cy), style = Stroke(2f))
            }
            val cbmp = PixelArtSprites.forCompanion(comp.id, broken = comp.broken)
            val csz = asz(34f)
            drawImage(
                image = cbmp,
                dstOffset = IntOffset((cx - csz / 2f).toInt(), (cy - csz / 2f + compBob).toInt()),
                dstSize = IntSize(csz.toInt().coerceAtLeast(1), csz.toInt().coerceAtLeast(1)),
                colorFilter = if (comp.broken) ColorFilter.tint(AmberWarning.copy(alpha = 0.45f)) else null,
                filterQuality = FilterQuality.None
            )
            if (comp.broken || comp.needsAttention) {
                drawCircle(CrimsonBright, asz(5.5f), Offset(cx + asz(13f), cy - asz(14f)))
            }
        }

        // Pixel-Art Player Character
        drawPixelPlayer(
            cx = ax(state.playerX),
            cy = ay(state.playerY),
            scale = scale,
            state = state
        )

        // Projectiles
        for (p in state.projectiles) {
            val px = ax(p.x)
            val py = ay(p.y)
            val pr = asz(p.radius)
            drawCircle(Color(p.colorHex).copy(alpha = 0.28f), pr * 1.7f, Offset(px, py))
            drawCircle(Color(p.colorHex), pr, Offset(px, py))
            drawCircle(CharcoalBlack, pr, Offset(px, py), style = Stroke(2f))
            if (!p.fromPlayer) {
                drawCircle(CottonWhite, pr * 0.35f, Offset(px, py))
            }
        }

        // Particles
        for (pt in state.particles) {
            val alpha = (pt.life / pt.maxLife).coerceIn(0f, 1f)
            drawCircle(
                color = Color(pt.colorHex).copy(alpha = alpha),
                radius = asz(pt.radius),
                center = Offset(ax(pt.x), ay(pt.y))
            )
        }

        // Localized Lantern Lighting & Defect Perception Effects
        val pxPx = ax(state.playerX)
        val pyPx = ay(state.playerY)
        val eyeWear = state.equippedParts[BodyZone.EYES]?.wearState
        var visionScale = if (eyeWear == WearState.DAMAGED) 0.85f else if (eyeWear == WearState.CRITICAL || eyeWear == WearState.DETACHED) 0.72f else 1f
        state.activeDefects.forEach { visionScale *= it.effects.visionScale }
        state.passiveItems.forEach { visionScale *= it.effects.visionBoost }
        if (state.activeCompanions.any { it.id == CompanionId.PAPER_MOTH }) visionScale *= 1.28f
        val lightR = asz(470f) * visionScale
        val roomDarkBias = if (room.type == RoomType.REPAIR) 0.45f else 0.76f
        val darkAlpha = (roomDarkBias * vignetteStrength).coerceIn(0.20f, 0.92f)

        drawRect(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to Color.Transparent,
                    0.48f to Color.Black.copy(alpha = darkAlpha * 0.25f),
                    1.0f to CharcoalBlack.copy(alpha = darkAlpha)
                ),
                center = Offset(pxPx, pyPx),
                radius = lightR
            ),
            topLeft = Offset(ox, oy),
            size = Size(arenaPxW, arenaPxH)
        )

        if (state.blackoutAlpha > 0.02f) {
            drawRect(
                color = CharcoalBlack.copy(alpha = state.blackoutAlpha),
                topLeft = Offset(ox, oy),
                size = Size(arenaPxW, arenaPxH)
            )
        }

        if (state.activeDefects.any { it.effects.leftBlindness }) {
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(CharcoalBlack.copy(alpha = 0.78f), Color.Transparent),
                    startX = ox,
                    endX = ox + arenaPxW * 0.42f
                ),
                topLeft = Offset(ox, oy),
                size = Size(arenaPxW * 0.45f, arenaPxH)
            )
        }

        if (state.activeDefects.any { it.effects.blurPulses }) {
            val pulseAlpha = (sin(state.blurPulseTimer * 2.5f) * 0.5f + 0.5f) * 0.25f
            drawRect(
                color = SlateBlue.copy(alpha = pulseAlpha),
                topLeft = Offset(ox, oy),
                size = Size(arenaPxW, arenaPxH)
            )
        }

        if (state.lightFlashTimer > 0f) {
            drawRect(
                color = AgedIvory.copy(alpha = (state.lightFlashTimer * 0.6f).coerceIn(0f, 0.45f)),
                topLeft = Offset(ox, oy),
                size = Size(arenaPxW, arenaPxH)
            )
        }
    }
}

private fun DrawScope.drawRoomFloorPattern(
    variant: Int,
    roomType: RoomType,
    roomSeed: Int,
    innerLeft: Float,
    innerTop: Float,
    innerRight: Float,
    innerBottom: Float,
    tileSize: Float,
    scale: Float
) {
    val activeVariant = if (roomType == RoomType.REPAIR) 0 else variant
    when (activeVariant) {
        0 -> {
            // Staggered wooden floorboards
            var rowIdx = 0
            var py = innerTop
            while (py < innerBottom) {
                drawLine(Color.Black.copy(alpha = 0.28f), Offset(innerLeft, py), Offset(innerRight, py), 2f)
                var px = innerLeft + (if (rowIdx % 2 == 0) tileSize * 0.55f else tileSize * 1.15f)
                while (px < innerRight) {
                    drawLine(Color.Black.copy(alpha = 0.18f), Offset(px, py), Offset(px, (py + tileSize).coerceAtMost(innerBottom)), 2f)
                    // Nail dots
                    drawCircle(Color.Black.copy(alpha = 0.24f), 2f * scale, Offset(px + 6f * scale, py + 12f * scale))
                    px += tileSize * 1.75f
                }
                py += tileSize * 0.7f
                rowIdx++
            }
        }
        1 -> {
            // Checkered vintage nursery / shop tiles
            var row = 0
            var py = innerTop
            while (py < innerBottom) {
                var col = 0
                var px = innerLeft
                while (px < innerRight) {
                    if ((row + col) % 2 == 0) {
                        val tw = min(tileSize, innerRight - px)
                        val th = min(tileSize, innerBottom - py)
                        drawRect(Color.Black.copy(alpha = 0.16f), Offset(px, py), Size(tw, th))
                    }
                    px += tileSize
                    col++
                }
                py += tileSize
                row++
            }
        }
        2 -> {
            // Diagonal workshop parquet / diamond grid
            val step = tileSize * 1.25f
            var x = innerLeft - (innerBottom - innerTop)
            while (x < innerRight) {
                drawLine(Color.Black.copy(alpha = 0.18f), Offset(x, innerTop), Offset(x + (innerBottom - innerTop), innerBottom), 2f)
                drawLine(Color.Black.copy(alpha = 0.14f), Offset(x + (innerBottom - innerTop), innerTop), Offset(x, innerBottom), 1.5f)
                x += step
            }
        }
        else -> {
            // Stone / slate slabs with brass corner rivets
            var py = innerTop
            while (py < innerBottom) {
                var px = innerLeft
                while (px < innerRight) {
                    val tw = min(tileSize * 1.3f, innerRight - px)
                    val th = min(tileSize * 1.1f, innerBottom - py)
                    drawRect(Color.Black.copy(alpha = 0.22f), Offset(px, py), Size(tw, th), style = Stroke(2f))
                    drawCircle(TarnishedBrassDim.copy(alpha = 0.18f), 2.5f * scale, Offset(px + 8f * scale, py + 8f * scale))
                    px += tileSize * 1.3f
                }
                py += tileSize * 1.1f
            }
        }
    }

    // Deterministic subtle floor scuffs / wear patches so every room feels handcrafted
    val rng = Random(roomSeed)
    val w = innerRight - innerLeft
    val h = innerBottom - innerTop
    repeat(5) {
        val sx = innerLeft + rng.nextFloat() * w * 0.85f + w * 0.07f
        val sy = innerTop + rng.nextFloat() * h * 0.85f + h * 0.07f
        val r = (14f + rng.nextFloat() * 24f) * scale
        drawOval(Color.Black.copy(alpha = 0.10f), Offset(sx - r, sy - r * 0.5f), Size(r * 2f, r))
    }
}

private fun DrawScope.drawRoomDecorations(
    room: RoomBlueprint,
    elapsed: Float,
    ox: Float,
    oy: Float,
    scale: Float,
    innerLeft: Float,
    innerTop: Float,
    innerRight: Float,
    innerBottom: Float
) {
    fun ax(x: Float) = ox + x * scale
    fun ay(y: Float) = oy + y * scale
    fun asz(v: Float) = v * scale
    val dashStroke = Stroke(3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f))

    when (room.type) {
        RoomType.REPAIR -> {
            // Warm Tailor's Overhead Lamp Spotlight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(TarnishedBrass.copy(alpha = 0.22f), AmberWarning.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(ax(500f), ay(440f)),
                    radius = asz(360f)
                ),
                radius = asz(360f),
                center = Offset(ax(500f), ay(440f))
            )
            // Patchwork crimson & olive tailor rug in center
            drawRoundRect(
                color = CrimsonBlood.copy(alpha = 0.28f),
                topLeft = Offset(ax(250f), ay(300f)),
                size = Size(asz(500f), asz(390f)),
                cornerRadius = CornerRadius(10f, 10f)
            )
            drawRoundRect(
                color = TarnishedBrass.copy(alpha = 0.55f),
                topLeft = Offset(ax(250f), ay(300f)),
                size = Size(asz(500f), asz(390f)),
                cornerRadius = CornerRadius(10f, 10f),
                style = dashStroke
            )
            // Centerpiece Mesa de Costura
            val tableBmp = PixelArtSprites.sewingTableProp()
            drawImage(
                tableBmp,
                dstOffset = IntOffset(ax(420f).toInt(), ay(295f).toInt()),
                dstSize = IntSize(asz(160f).toInt().coerceAtLeast(1), asz(95f).toInt().coerceAtLeast(1)),
                filterQuality = FilterQuality.None
            )
            // Thread spools, scissors, and tailor mannequins in workshop corners
            drawImage(
                PixelArtSprites.mannequinProp(),
                dstOffset = IntOffset(ax(115f).toInt(), ay(108f).toInt()),
                dstSize = IntSize(asz(68f).toInt().coerceAtLeast(1), asz(78f).toInt().coerceAtLeast(1)),
                filterQuality = FilterQuality.None
            )
            drawImage(
                PixelArtSprites.mannequinProp(),
                dstOffset = IntOffset(ax(815f).toInt(), ay(108f).toInt()),
                dstSize = IntSize(asz(68f).toInt().coerceAtLeast(1), asz(78f).toInt().coerceAtLeast(1)),
                filterQuality = FilterQuality.None
            )
            drawImage(
                PixelArtSprites.threadSpoolsProp(),
                dstOffset = IntOffset(ax(195f).toInt(), ay(128f).toInt()),
                dstSize = IntSize(asz(66f).toInt().coerceAtLeast(1), asz(40f).toInt().coerceAtLeast(1)),
                filterQuality = FilterQuality.None
            )
            drawImage(
                PixelArtSprites.scissorsProp(),
                dstOffset = IntOffset(ax(735f).toInt(), ay(128f).toInt()),
                dstSize = IntSize(asz(62f).toInt().coerceAtLeast(1), asz(38f).toInt().coerceAtLeast(1)),
                filterQuality = FilterQuality.None
            )
            // Hanging crimson bunting threads near top trim
            for (i in 0 until 6) {
                val bx = ax(130f + i * 145f)
                drawArc(
                    color = if (i % 2 == 0) CrimsonBright.copy(alpha = 0.55f) else TarnishedBrass.copy(alpha = 0.55f),
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(bx, innerTop - asz(12f)),
                    size = Size(asz(90f), asz(32f)),
                    style = Stroke(3f)
                )
            }
        }
        RoomType.START -> {
            drawRoundRect(
                color = AgedWoodLight.copy(alpha = 0.25f),
                topLeft = Offset(ax(330f), ay(350f)),
                size = Size(asz(340f), asz(300f)),
                cornerRadius = CornerRadius(6f, 6f),
                style = dashStroke
            )
        }
        RoomType.TREASURE -> {
            drawRoundRect(
                color = TarnishedBrass.copy(alpha = 0.16f),
                topLeft = Offset(ax(260f), ay(320f)),
                size = Size(asz(480f), asz(380f)),
                cornerRadius = CornerRadius(12f, 12f)
            )
            drawRoundRect(
                color = TarnishedBrass.copy(alpha = 0.50f),
                topLeft = Offset(ax(260f), ay(320f)),
                size = Size(asz(480f), asz(380f)),
                cornerRadius = CornerRadius(12f, 12f),
                style = Stroke(3f)
            )
        }
        RoomType.SHOP -> {
            drawRoundRect(
                color = MutedMoss.copy(alpha = 0.18f),
                topLeft = Offset(ax(200f), ay(250f)),
                size = Size(asz(600f), asz(110f)),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawRoundRect(
                color = TarnishedBrass.copy(alpha = 0.45f),
                topLeft = Offset(ax(200f), ay(250f)),
                size = Size(asz(600f), asz(110f)),
                cornerRadius = CornerRadius(8f, 8f),
                style = Stroke(3f)
            )
        }
        RoomType.CURSE -> {
            val cx = ax(500f)
            val cy = ay(520f)
            val pulse = 0.8f + 0.2f * sin(elapsed * 3f)
            drawCircle(CrimsonBright.copy(alpha = 0.26f * pulse), asz(180f), Offset(cx, cy), style = Stroke(3f))
            drawCircle(CrimsonBlood.copy(alpha = 0.18f), asz(135f), Offset(cx, cy), style = dashStroke)
        }
        RoomType.CHALLENGE -> {
            val cx = ax(500f)
            val cy = ay(520f)
            drawCircle(SlateBlue.copy(alpha = 0.28f), asz(190f), Offset(cx, cy), style = Stroke(4f))
        }
        RoomType.BOSS -> {
            drawRoundRect(
                color = CrimsonBlood.copy(alpha = 0.24f),
                topLeft = Offset(ax(240f), ay(290f)),
                size = Size(asz(520f), asz(470f)),
                cornerRadius = CornerRadius(10f, 10f)
            )
            drawRoundRect(
                color = TarnishedBrass.copy(alpha = 0.42f),
                topLeft = Offset(ax(240f), ay(290f)),
                size = Size(asz(520f), asz(470f)),
                cornerRadius = CornerRadius(10f, 10f),
                style = Stroke(3.5f)
            )
            drawCircle(CrimsonBright.copy(alpha = 0.22f), asz(120f), Offset(ax(500f), ay(525f)), style = Stroke(3f))
        }
        RoomType.MANUFACTURER -> {
            drawCircle(TarnishedBrass.copy(alpha = 0.2f), asz(190f), Offset(ax(500f), ay(440f)))
            drawCircle(TarnishedBrass.copy(alpha = 0.55f), asz(190f), Offset(ax(500f), ay(440f)), style = Stroke(3f))
            drawRoundRect(
                color = AgedWoodLight.copy(alpha = 0.35f),
                topLeft = Offset(ax(360f), ay(410f)),
                size = Size(asz(280f), asz(90f)),
                cornerRadius = CornerRadius(6f, 6f)
            )
        }
        RoomType.FORBIDDEN -> {
            drawCircle(CrimsonBright.copy(alpha = 0.26f), asz(170f), Offset(ax(500f), ay(520f)), style = Stroke(4f))
            drawCircle(CrimsonBlood.copy(alpha = 0.22f), asz(115f), Offset(ax(500f), ay(520f)), style = dashStroke)
        }
        else -> {
            // Subtle room border stitching on some combat/event/secret rooms
            if (room.visualVariant % 2 == 1) {
                drawRect(
                    color = Color.Black.copy(alpha = 0.15f),
                    topLeft = Offset(innerLeft + asz(40f), innerTop + asz(40f)),
                    size = Size(innerRight - innerLeft - asz(80f), innerBottom - innerTop - asz(80f)),
                    style = dashStroke
                )
            }
        }
    }
}

private fun DrawScope.drawCobweb(x: Float, y: Float, size: Float, dx: Float, dy: Float) {
    val c = AgedIvory.copy(alpha = 0.14f)
    drawLine(c, Offset(x, y), Offset(x + size * dx, y + size * 0.35f * dy), 1.5f)
    drawLine(c, Offset(x, y), Offset(x + size * 0.7f * dx, y + size * 0.7f * dy), 1.5f)
    drawLine(c, Offset(x, y), Offset(x + size * 0.35f * dx, y + size * dy), 1.5f)
}

private fun DrawScope.drawPixelPlayer(
    cx: Float,
    cy: Float,
    scale: Float,
    state: GameRunState
) {
    val s = scale
    val blink = state.invulnTimer > 0f && ((state.invulnTimer * 18f).toInt() % 2 == 0)
    if (blink) return

    val alpha = if (state.stealthTimer > 0f) 0.45f else 1f
    val legPart = state.equippedParts[BodyZone.LEGS]
    val armPart = state.equippedParts[BodyZone.ARMS]
    val backPart = state.equippedParts[BodyZone.BACK]
    val moving = hypot(state.playerVx, state.playerVy) > 15f
    val bobY = if (moving) abs(sin(state.walkCycle * 2.5f)) * -4f * s else 0f
    val limpTilt = if (legPart?.isDetached == true || legPart?.wearState == WearState.CRITICAL) {
        sin(state.walkCycle * 3f) * 8f
    } else 0f

    drawOval(Color.Black.copy(alpha = 0.48f), Offset(cx - 18f * s, cy + 18f * s), Size(36f * s, 13f * s))

    if (backPart != null && !backPart.isDetached) {
        if (backPart.def.id == "paper_wings") {
            drawOval(AgedIvory.copy(alpha = 0.7f * alpha), Offset(cx - 28f * s, cy - 8f * s + bobY), Size(14f * s, 20f * s))
            drawOval(AgedIvory.copy(alpha = 0.7f * alpha), Offset(cx + 14f * s, cy - 8f * s + bobY), Size(14f * s, 20f * s))
        } else if (backPart.def.id == "windup_key_back") {
            rotate(state.elapsedSeconds * 120f, Offset(cx, cy - 22f * s + bobY)) {
                drawRect(TarnishedBrass, Offset(cx - 3f * s, cy - 33f * s + bobY), Size(6f * s, 12f * s))
            }
        }
    }

    val bmp = PixelArtSprites.forCharacter(state.character.id)
    val sprSize = 52f * s
    rotate(limpTilt, Offset(cx, cy)) {
        drawImage(
            image = bmp,
            dstOffset = IntOffset((cx - sprSize / 2f).toInt(), (cy - sprSize / 2f + bobY).toInt()),
            dstSize = IntSize(sprSize.toInt().coerceAtLeast(1), sprSize.toInt().coerceAtLeast(1)),
            alpha = alpha,
            filterQuality = FilterQuality.None
        )
    }

    if (armPart?.isDetached == true) {
        drawCircle(CrimsonBright, 4.5f * s, Offset(cx + 14f * s, cy + bobY))
    }

    if (hypot(state.aimX, state.aimY) > 0.25f) {
        val ang = atan2(state.aimY, state.aimX)
        val rx = cx + cos(ang) * 44f * s
        val ry = cy + sin(ang) * 44f * s
        drawCircle(TarnishedBrass.copy(alpha = 0.78f), 5.5f * s, Offset(rx, ry), style = Stroke(2f))
    }
}

private fun DrawScope.drawRoomMechanicFloor(
    state: GameRunState,
    ox: Float,
    oy: Float,
    scale: Float,
    innerLeft: Float,
    innerTop: Float,
    innerRight: Float,
    innerBottom: Float
) {
    fun ax(x: Float) = ox + x * scale
    fun ay(y: Float) = oy + y * scale
    fun asz(v: Float) = v * scale
    val mech = state.currentRoom.mechanic

    when (mech) {
        RoomMechanic.VANISHING_FLOOR -> {
            for (sec in state.vanishingSectors) {
                val b = RoomMechanicSystem.sectorBounds(sec)
                val sx = ax(b[0])
                val sy = ay(b[1])
                val sw = asz(b[2])
                val sh = asz(b[3])
                when (sec.state) {
                    SectorState.STABLE -> {
                        drawRect(TarnishedBrassDim.copy(alpha = 0.18f), Offset(sx, sy), Size(sw, sh), style = Stroke(1.5f))
                    }
                    SectorState.WARNING -> {
                        val pulse = 0.35f + 0.35f * abs(sin(state.elapsedSeconds * 12f))
                        drawRect(AmberWarning.copy(alpha = pulse * 0.35f), Offset(sx, sy), Size(sw, sh))
                        drawRect(CrimsonBright.copy(alpha = 0.85f), Offset(sx, sy), Size(sw, sh), style = Stroke(3f))
                        drawLine(CrimsonBright.copy(alpha = 0.7f), Offset(sx + 8f, sy + 8f), Offset(sx + sw - 8f, sy + sh - 8f), 2f)
                    }
                    SectorState.GONE -> {
                        drawRect(Color(0xFF060405), Offset(sx, sy), Size(sw, sh))
                        drawRect(CrimsonBlood.copy(alpha = 0.55f), Offset(sx, sy), Size(sw, sh), style = Stroke(2.5f))
                    }
                }
            }
        }
        RoomMechanic.TILTED_FLOOR -> {
            val w = innerRight - innerLeft
            val h = innerBottom - innerTop
            val cx = (innerLeft + innerRight) / 2f
            val cy = (innerTop + innerBottom) / 2f
            val tx = state.tiltX
            val ty = state.tiltY
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, CrimsonBlood.copy(alpha = 0.22f)),
                    start = Offset(cx - tx * w * 0.45f, cy - ty * h * 0.45f),
                    end = Offset(cx + tx * w * 0.45f, cy + ty * h * 0.45f)
                ),
                topLeft = Offset(innerLeft, innerTop),
                size = Size(w, h)
            )
            // Subtle directional slope arrows on floor center
            val arrowLen = asz(65f)
            val tipX = cx + tx * arrowLen
            val tipY = cy + ty * arrowLen
            drawLine(TarnishedBrass.copy(alpha = 0.52f), Offset(cx - tx * arrowLen * 0.5f, cy - ty * arrowLen * 0.5f), Offset(tipX, tipY), 4f)
            drawCircle(TarnishedBrass.copy(alpha = 0.65f), asz(7f), Offset(tipX, tipY))
        }
        RoomMechanic.MIRROR_COPIES -> {
            val shimmer = 0.12f + 0.06f * sin(state.elapsedSeconds * 2.5f)
            drawRect(
                color = Color(0xFF9EC4DB).copy(alpha = shimmer),
                topLeft = Offset(innerLeft, innerTop),
                size = Size(innerRight - innerLeft, innerBottom - innerTop)
            )
            drawLine(
                color = Color(0xFFD9ECF7).copy(alpha = 0.30f),
                start = Offset(innerLeft, innerTop + (innerBottom - innerTop) * 0.25f),
                end = Offset(innerLeft + (innerRight - innerLeft) * 0.35f, innerTop),
                strokeWidth = 6f
            )
        }
        RoomMechanic.DONT_BLINK_STATUES -> {
            // Subtle gaze cone indicator from player along facingAngle
            val px = ax(state.playerX)
            val py = ay(state.playerY)
            val range = asz(260f)
            val a1 = state.facingAngle - 0.56f
            val a2 = state.facingAngle + 0.56f
            drawLine(TarnishedBrass.copy(alpha = 0.25f), Offset(px, py), Offset(px + cos(a1) * range, py + sin(a1) * range), 2f)
            drawLine(TarnishedBrass.copy(alpha = 0.25f), Offset(px, py), Offset(px + cos(a2) * range, py + sin(a2) * range), 2f)
        }
        RoomMechanic.SPINNING_CAROUSEL -> {
            val cx = ax(ARENA_W * 0.5f)
            val cy = ay(ARENA_H * 0.5f)
            rotate(state.elapsedSeconds * 28f, Offset(cx, cy)) {
                drawCircle(TarnishedBrass.copy(alpha = 0.28f), asz(260f), Offset(cx, cy), style = Stroke(3f))
                drawCircle(CrimsonBright.copy(alpha = 0.24f), asz(150f), Offset(cx, cy), style = Stroke(3f))
                for (i in 0 until 6) {
                    val a = (i / 6f) * (2f * PI.toFloat())
                    drawLine(TarnishedBrass.copy(alpha = 0.22f), Offset(cx, cy), Offset(cx + cos(a) * asz(260f), cy + sin(a) * asz(260f)), 2f)
                }
            }
        }
        RoomMechanic.NURSERY_MAZE -> {
            // Playful faded nursery rug stripe underneath maze blocks
            drawRect(
                color = SlateBlue.copy(alpha = 0.14f),
                topLeft = Offset(ax(160f), ay(170f)),
                size = Size(asz(680f), asz(710f)),
                style = Stroke(4f)
            )
        }
        else -> Unit
    }
}

