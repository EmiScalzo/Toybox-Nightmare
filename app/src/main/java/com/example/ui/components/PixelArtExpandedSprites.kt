package com.example.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import com.example.data.model.InteractableType
import com.example.data.model.RepairMaterial
import com.example.data.model.SecretClueType

/**
 * Handcrafted dark-outlined pixel-art sprites for the 6 expanded mechanics:
 * - 4 new tactical relationship enemies (Tin Captain, Bodyguard Bear, Clockwork Courier, Stitch Mechanic)
 * - El Fabricante NPC portrait/sprite
 * - All 23 interactive room elements (movables, activables, destructibles, ambient)
 * - All 5 physical secret clues & anomalies
 * - All 7 repair materials
 * - Objective props (crying box, delivery parcel, gear token, alarm bell)
 */
object PixelArtExpandedSprites {

    fun tinCaptain(): ImageBitmap = PixelArtSprites.sprite("enemy_tin_captain", listOf(
        "....KKKKKKKK....",
        "...KGGGGGGGGK...",
        "...KRRRRRRRRK...",
        "...KGGDDDDGGK...",
        "....KKKKKKKK....",
        "....KPPPPPPK....",
        "....KPKKKKPK....",
        "....KPKEEKPK....",
        "..KKRRRRRRRRKK..",
        ".KLLRGGGGGGGRLLK",
        ".KLLRRRRRRRRRLLK",
        ".KCLLRRGGRRLLCCK",
        "..K.KLLLLLLK.CK.",
        "....KLLKKLLK....",
        "....KDDKKDDK....",
        "....KKKKKKKK...."
    ))

    fun bodyguardBear(): ImageBitmap = PixelArtSprites.sprite("enemy_bodyguard_bear", listOf(
        "..KKK......KKK..",
        ".KBBBK....KBBBK.",
        ".KBbBKKKKKKBbBK.",
        ".KBBBBBBBBBBBBK.",
        "KBBCCCCCCCCCCBBK",
        "KBBKEKBBBBKEKBBK",
        "KBBBBBPWWPBBBBBK",
        ".KBBBBPKKPBBBBK.",
        "..KKKKGGGGKKKK..",
        "KCCbKLLLLLLKbCCK",
        "KCCCKLLGGLLKCCCK",
        "KCCbKLLLLLLKbCCK",
        ".KK.KBBBBBBK.KK.",
        "....KBBBBBBK....",
        "...KBBbKKbBBK...",
        "...KKKK..KKKK..."
    ))

    fun clockworkCourier(): ImageBitmap = PixelArtSprites.sprite("enemy_clockwork_courier", listOf(
        "..KKK......KKK..",
        "..KGGK....KGGK..",
        "..KCCK....KCCK..",
        "..KCCKKKKKKCCK..",
        "..KCCCCCCCCCCK..",
        ".KCCKYKCCKYKCCK.",
        ".KCCCCPRRPCCCCK.",
        "..KKKCCCCCCKKK..",
        "...KCKGGGGKCK...",
        "..KLLCKGGKCLLK..",
        "..KCCKCCCCKCCK..",
        "...KK.KCCK.KK...",
        ".....KCKKCK.....",
        "....KGGKKGGK....",
        "....KKKKKKKK...."
    ))

    fun stitchMechanic(): ImageBitmap = PixelArtSprites.sprite("enemy_stitch_mechanic", listOf(
        "....KKKKKKKK....",
        "...KMMMMMMMMK...",
        "..KMMGGGGGGMMK..",
        "..KMMMMMMMMMMK..",
        "..KMKKKKKKKKMK..",
        "..KMPKEKKKEPMK..",
        "...KKPPPPPPKK...",
        "..KKMMKKKKMMKK..",
        ".KCCKMMWWMMKGGK.",
        ".KCK.KMMMMK.KCK.",
        ".....KMMMMK.....",
        "....KMMKKMMK....",
        "....KDDKKDDK....",
        "....KKKKKKKK...."
    ))

    fun manufacturerNpc(): ImageBitmap = PixelArtSprites.sprite("npc_manufacturer", listOf(
        ".....KKKKKK.....",
        "....KDDDDDDK....",
        "...KDDGGGGDDK...",
        "...KDDDDDDDDK...",
        "..KKKPPPPPPKKK..",
        "...KPWWWWWWPK...",
        "...KPWKKKKWPK...",
        "...KPWKDDKWPK...",
        "...KPPPPPPPPK...",
        "....KKDDDDKK....",
        "..KKDDGGGGDDKK..",
        ".KDDKDBBBBDKDDK.",
        ".KD.KDBGGbDK.DK.",
        "....KDBBBBDK....",
        "....KDDKKDDK....",
        "....KDDKKDDK....",
        "....KKKKKKKK...."
    ))

    fun forInteractable(
        type: InteractableType,
        active: Boolean = false,
        destroyed: Boolean = false
    ): ImageBitmap = when (type) {
        InteractableType.WOOD_BLOCK -> PixelArtSprites.sprite("int_wood_block", listOf(
            "KKKKKKKKKKKK",
            "KBBBBBBBBBBK",
            "KBTTTTTTTbBK",
            "KBTKKBBKKTBK",
            "KBTKBBBBKTBK",
            "KBTKKKKKKTBK",
            "KBTKBBBBKTBK",
            "KBTTTTTTTbBK",
            "KBBbbbbbbBBK",
            "KKKKKKKKKKKK"
        ))

        InteractableType.CRATE_COVER -> if (destroyed) {
            PixelArtSprites.sprite("int_crate_broken", listOf(
                "............",
                "............",
                "..KK....KK..",
                ".KBBK..KbBK.",
                "..KKbKKBBK..",
                ".KBbK..KBBK.",
                "KKKK....KKKK"
            ))
        } else {
            PixelArtSprites.crateObstacle()
        }

        InteractableType.TOY_CART -> PixelArtSprites.sprite("int_toy_cart", listOf(
            "....KKKKKK....",
            "...KLLWWLLK...",
            "..KLLLLLLLLK..",
            ".KRRRRRRRRRRK.",
            ".KRRGGGGGGrrK.",
            ".KKKKKKKKKKKK.",
            "..KDDK..KDDK..",
            "..KGGK..KGGK..",
            "...KK....KK..."
        ))

        InteractableType.PARTS_BARREL -> PixelArtSprites.sprite("int_parts_barrel", listOf(
            "..KKKKKKKK..",
            ".KCCCCCCCCK.",
            ".KBBBBBBBBK.",
            "KBBbGGGGbBBK",
            "KBBBBCCBBBBK",
            "KBBbGGGGbBBK",
            ".KBBBBBBBBK.",
            ".KCCCCCCCCK.",
            "..KKKKKKKK.."
        ))

        InteractableType.GIANT_DOLL_PROP -> PixelArtSprites.sprite("int_giant_doll", listOf(
            "...KKKKKKKK...",
            "..KHHHHHHHHK..",
            "..KHPPPPPPPK..",
            "..KHPKKKKPPK..",
            "..KHPPPPPPPK..",
            "..KKKRRRRKKK..",
            ".KPPKRRRRKPPK.",
            ".KPPKRRRRKPPK.",
            "..KKKRRRRKKK..",
            "....KPPKKPPK..",
            "....KKKKKKKK.."
        ))

        InteractableType.BATTERY_CUBE -> PixelArtSprites.sprite(
            if (active) "int_battery_on" else "int_battery_off",
            if (active) listOf(
                "..KK....KK..",
                ".KGGK..KGGK.",
                "KKKKKKKKKKKK",
                "KLLLLYYLLLLK",
                "KLLYWWWWYLLK",
                "KLLYYYYYYLLK",
                "KLLLLYYLLLLK",
                "KCCCCCCCCCCK",
                "KKKKKKKKKKKK"
            ) else listOf(
                "..KK....KK..",
                ".KCCK..KCCK.",
                "KKKKKKKKKKKK",
                "KLLLLLLLLLLK",
                "KLLLGGGGLLLK",
                "KLLLGllGLLLK",
                "KLLLGGGGLLLK",
                "KCCCCCCCCCCK",
                "KKKKKKKKKKKK"
            )
        )

        InteractableType.ELECTRIC_TRAIN -> PixelArtSprites.sprite(
            if (active) "int_train_on" else "int_train_off",
            listOf(
                "..KK....KKKKKK..",
                ".KYYK..KRRRRRRK.",
                ".KCCKKKKRRRRRRK.",
                "KCCCCCCCRRRRRRRK",
                "KCCGGGCCRRWWWRRK",
                "KCCCCCCCCRRRRRRK",
                "KKKKKKKKKKKKKKKK",
                ".KDDK.KDDK.KDDK.",
                "..KK...KK...KK.."
            )
        )

        InteractableType.TOY_CAROUSEL -> PixelArtSprites.sprite("int_carousel", listOf(
            "......KKKK......",
            "....KKGGGGKK....",
            "..KKRRRRWWWWKK..",
            ".KRRWWWWWWWWrrK.",
            "KKKKKKKKKKKKKKKK",
            "..KGG.KWWK.GGK..",
            "..KWW.KGGK.WWK..",
            "..KPP.KRRK.PPK..",
            ".KKKKKKKKKKKKKK.",
            ".KGGGGGGGGGGGGK.",
            "..KKKKKKKKKKKK.."
        ))

        InteractableType.WINDUP_JACK_TRAP -> PixelArtSprites.sprite(
            if (active) "int_jack_open" else "int_jack_closed",
            if (active) listOf(
                "....KKK..KKK....",
                "...KWKEKKKEWK...",
                "....KWWRRWWK....",
                ".....KKCCKK.....",
                "..KKKKKKKKKKKK..",
                ".KLLLLRRRRLLLLK.",
                ".KRRRRGGGGLLLLK.",
                "..KKKKKKKKKKKK.."
            ) else listOf(
                ".....KGGGGK.....",
                "......KGGK......",
                "..KKKKKKKKKKKK..",
                ".KLLLLRRRRLLLLK.",
                ".KLLRRGGGGLLRRK.",
                ".KRRRRLLLLRRRRK.",
                ".KLLLLRRRRLLLLK.",
                "..KKKKKKKKKKKK.."
            )
        )

        InteractableType.SPRING_LAUNCHER -> PixelArtSprites.sprite("int_spring_launcher", listOf(
            "....KKKKKKKK....",
            "...KCCCCCCCCK...",
            "..KCCGGGGGGCCK..",
            "..KCCWWCCWWCCK..",
            ".KKKKCCCCCCKKKK.",
            "KBBBBBBBBBBBBBBK",
            "KBggggggggggggBK",
            "KKKKKKKKKKKKKKKK"
        ))

        InteractableType.WALL_LEVER -> PixelArtSprites.sprite(
            if (active) "int_lever_on" else "int_lever_off",
            if (active) listOf(
                "........KKK.",
                ".......KRRRK",
                "......KCCKK.",
                ".....KCCK...",
                "..KKKKKKKK..",
                ".KCCCCCCCCK.",
                ".KCCGGGGCCK.",
                "..KKKKKKKK.."
            ) else listOf(
                ".KKK........",
                "KRRRK.......",
                ".KKCCK......",
                "...KCCK.....",
                "..KKKKKKKK..",
                ".KCCCCCCCCK.",
                ".KCCGGGGCCK.",
                "..KKKKKKKK.."
            )
        )

        InteractableType.CHRONO_CLOCK -> PixelArtSprites.sprite("int_chrono_clock", listOf(
            "....KKKKKKKK....",
            "...KBBBBBBBBK...",
            "..KBBGGGGGGbBK..",
            "..KBKWWWWWWKBK..",
            "..KBKWWKKWWKBK..",
            "..KBKWWKGGWKBK..",
            "..KBKWWWWWWKBK..",
            "..KBBGGGGGGbBK..",
            "...KBBbKKbBBK...",
            "...KBBKGGKBBK...",
            "...KKKKKKKKKK..."
        ))

        InteractableType.PRIZE_MACHINE -> PixelArtSprites.sprite("int_prize_machine", listOf(
            "...KKKKKKKKKK...",
            "..KRRRRRRRRRRK..",
            "..KRRGGGGGGrrK..",
            "..KCCCCCCCCCCK..",
            "..KCWWWWWWWWCK..",
            "..KCWRRGGLLWCK..",
            "..KCCCCCCCCCCK..",
            "..KRRRRRRRRRRK..",
            "..KRRKDDDDKrrK..",
            "..KRRRRRRRRRRK..",
            "...KKKKKKKKKK..."
        ))

        InteractableType.HANGING_SHELF -> if (destroyed) {
            PixelArtSprites.sprite("int_shelf_fallen", listOf(
                "................",
                ".KKKKKKKKKKKKKK.",
                "KBBBBbBBBBbBBBBK",
                "KBbBBBBBBbBBBBbK",
                ".KKKKKKKKKKKKKK."
            ))
        } else {
            PixelArtSprites.sprite("int_shelf_hanging", listOf(
                "..K..........K..",
                "..K..........K..",
                ".KKKKKKKKKKKKKK.",
                "KBBBBBBBBBBBBBBK",
                "KBGGKBBBBBBKRRBK",
                ".KKKKKKKKKKKKKK."
            ))
        }

        InteractableType.GLASS_LAMP -> if (destroyed) {
            PixelArtSprites.sprite("int_lamp_broken", listOf(
                "................",
                "...KK......KK...",
                "..KWWK....KWWK..",
                ".KWKK..KK..KKWK.",
                "..KK..KGGK..KK.."
            ))
        } else {
            PixelArtSprites.sprite("int_lamp_lit", listOf(
                "....KKKKKKKK....",
                "...KYYYYYYYYK...",
                "..KYYWWWWWWYYK..",
                "..KYYWWWWWWYYK..",
                "...KYYYYYYYYK...",
                "....KKGGGGKK....",
                ".....KGGGGK.....",
                "....KKKKKKKK...."
            ))
        }

        InteractableType.GLASS_CASE -> if (destroyed) {
            PixelArtSprites.sprite("int_glass_shards", listOf(
                "................",
                ".KK....KK....KK.",
                "KWWK..KLLK..KWWK",
                ".KK....KK....KK.",
                "...KK......KK..."
            ))
        } else {
            PixelArtSprites.vitrineObstacle()
        }

        InteractableType.LOOT_CRATE -> if (destroyed) {
            PixelArtSprites.sprite("int_loot_open", listOf(
                "................",
                ".KKKKKKKKKKKKKK.",
                "KBBGGGGGGGGGGbBK",
                "KBBBBBBBBBBBBBBK",
                ".KKKKKKKKKKKKKK."
            ))
        } else {
            PixelArtSprites.sprite("int_loot_crate", listOf(
                "KKKKKKKKKKKK",
                "KBBBBBBBBBBK",
                "KBGGGGGGGGbK",
                "KBBGGKKGGBBK",
                "KBBGGKKGGBBK",
                "KBGGGGGGGGbK",
                "KBBBBBBBBBBK",
                "KKKKKKKKKKKK"
            ))
        }

        InteractableType.CRACKED_WALL -> if (destroyed) {
            PixelArtSprites.sprite("int_wall_broken", listOf(
                "................",
                ".KK........KK...",
                "KSSK......KSSK..",
                ".KK..KKKK..KK...",
                ".....KSSK......."
            ))
        } else {
            PixelArtSprites.sprite("int_cracked_wall", listOf(
                "KKKKKKKKKKKK",
                "KSSSSSSSSSSK",
                "KSSKKSSSSSSK",
                "KSSSKKKSSSSK",
                "KSSSSSSKKSSK",
                "KSSSSKKSSSSK",
                "KSSSSSSSSSSK",
                "KKKKKKKKKKKK"
            ))
        }

        InteractableType.OIL_PUDDLE -> PixelArtSprites.sprite("int_oil_puddle", listOf(
            "....KKKKKKKK....",
            "..KKDDDDDDDDKK..",
            ".KDDDDLLLLDDDDK.",
            "KDDDLDDDDDDLDDDK",
            "KDDDDDDDDDDDDDDK",
            ".KDDDDLLLLDDDDK.",
            "..KKDDDDDDDDKK..",
            "....KKKKKKKK...."
        ))

        InteractableType.LIVE_WIRE -> PixelArtSprites.sprite(
            if (active) "int_wire_on" else "int_wire_off",
            if (active) listOf(
                "..KK......YY....",
                ".KGGK...KKYYKK..",
                "KGGCCKKKYYWWYYK.",
                ".KK..KKCCYYYYK..",
                "......KCCKKYY...",
                ".......KK......."
            ) else listOf(
                "..KK............",
                ".KGGK...KKKKK...",
                "KGGCCKKKCCCCCK..",
                ".KK..KKCCKKCC...",
                "......KCCK......",
                ".......KK......."
            )
        )

        InteractableType.STEAM_VENT -> PixelArtSprites.sprite("int_steam_vent", listOf(
            "...WW....WW...",
            "..KSSKKKKSSK..",
            ".KCCCCCCCCCCK.",
            ".KCKCCKCCKCCK.",
            ".KCKCCKCCKCCK.",
            ".KCCCCCCCCCCK.",
            "..KKKKKKKKKK.."
        ))

        InteractableType.PRESSURE_TILE -> PixelArtSprites.sprite(
            if (active) "int_tile_on" else "int_tile_off",
            if (active) listOf(
                "KKKKKKKKKKKK",
                "KMMMMMMMMMMK",
                "KMMGGGGGGMMK",
                "KMMGWWWWGMMK",
                "KMMGGGGGGMMK",
                "KMMMMMMMMMMK",
                "KKKKKKKKKKKK"
            ) else listOf(
                "KKKKKKKKKKKK",
                "KCCCCCCCCCCK",
                "KCCRRRRRRCCK",
                "KCCRGGGGGCCK",
                "KCCRRRRRRCCK",
                "KCCCCCCCCCCK",
                "KKKKKKKKKKKK"
            )
        )

        InteractableType.FAN_BLOWER -> PixelArtSprites.sprite("int_fan_blower", listOf(
            "..KKKKKKKKKK..",
            ".KCCCCCCCCCCK.",
            "KCCWWKCCKWWCCK",
            "KCCCKKGGKKCCCK",
            "KCCCCGGGGCCCCK",
            "KCCCKKGGKKCCCK",
            "KCCWWKCCKWWCCK",
            ".KCCCCCCCCCCK.",
            "..KKKKKKKKKK.."
        ))
    }

    fun forSecretClue(type: SecretClueType): ImageBitmap = when (type) {
        SecretClueType.CROOKED_FRAME -> PixelArtSprites.sprite("clue_crooked_frame", listOf(
            "....KKKKKKKK....",
            "...KGGGGGGGGK...",
            "..KGGBBBBBBGK...",
            "..KGBBPPPPBBGK..",
            ".KGBBPKEEKPBBGK.",
            ".KGBBPPPPPPBBGK.",
            ".KGGGGGGGGGGGK..",
            "..KKKKKKKKKKK..."
        ))

        SecretClueType.WALL_GAZING_DOLL -> PixelArtSprites.sprite("clue_gazing_doll", listOf(
            "...KKKKKKKK...",
            "..KHHHHHHHHK..",
            ".KHDDDDDDDDHK.",
            ".KHDDDDDDDDHK.",
            "..KKDDDDDDKK..",
            "..KRRRRRRRRK..",
            "..KRRRRRRRRK..",
            "...KPPKKPPK...",
            "...KKK..KKK..."
        ))

        SecretClueType.TICKING_CLOCK_PUZZLE -> PixelArtSprites.sprite("clue_ticking_clock", listOf(
            "...KKKKKKKK...",
            "..KGGGGGGGGK..",
            ".KGWWWWWWWWGK.",
            ".KGWKKKKWWWGK.",
            ".KGWWWKGGGGWK.",
            ".KGWWWWWWWWGK.",
            "..KGGGGGGGGK..",
            "...KKKKKKKK..."
        ))

        SecretClueType.KNOCKING_WALL -> PixelArtSprites.sprite("clue_knocking_wall", listOf(
            "KKKKKKKKKKKK",
            "KSSSSSSSSSSK",
            "KSSKKSSKKSSK",
            "KSSKKWWWWSSK",
            "KSSKWWPPWSSK",
            "KSSSSKKSSSSK",
            "KSSSSSSSSSSK",
            "KKKKKKKKKKKK"
        ))

        SecretClueType.WRONG_SHADOW_PROP -> PixelArtSprites.sprite("clue_wrong_shadow", listOf(
            "..KKKK....KK..",
            ".KGGGGK..KDDK.",
            ".KGGWWK.KDDDK.",
            "..KGGK..KDDK..",
            "..KGGKKKKDDK..",
            "..KGGK..KDDK..",
            "...KK....KK..."
        ))
    }

    fun forMaterial(mat: RepairMaterial): ImageBitmap = when (mat) {
        RepairMaterial.THREAD -> PixelArtSprites.sprite("mat_thread", listOf(
            "..KKKKKKKK..",
            ".KBBBBBBBBK.",
            "..KRRRRRRK..",
            "..KRRRRRRK..",
            "..KRRWWRRK..",
            "..KRRRRRRK..",
            ".KBBBBBBBBK.",
            "..KKKKKKKK.."
        ))

        RepairMaterial.NEEDLE -> PixelArtSprites.sprite("mat_needle", listOf(
            "..........KK",
            "........KWWK",
            ".......KCCK.",
            ".....KCCK...",
            "...KCCK.....",
            ".KCCK.......",
            "KCK.........",
            "KK.........."
        ))

        RepairMaterial.GEAR -> PixelArtSprites.sprite("mat_gear", listOf(
            "...KK..KK...",
            ".KKGGKKGGKK.",
            ".KGGGGGGGGK.",
            "KGGGKKKKGGGK",
            "KGGGKKKKGGGK",
            ".KGGGGGGGGK.",
            ".KKGGKKGGKK.",
            "...KK..KK..."
        ))

        RepairMaterial.SCREW -> PixelArtSprites.sprite("mat_screw", listOf(
            "..KKKKKKKK..",
            ".KCCCCCCCCK.",
            ".KCCKKKKCCK.",
            "..KKKKKKKK..",
            "....KCCK....",
            "....KcCK....",
            "....KCCK....",
            ".....KK....."
        ))

        RepairMaterial.GLUE -> PixelArtSprites.sprite("mat_glue", listOf(
            "....KKKK....",
            "....KRRK....",
            "...KWWWWK...",
            "..KWWWWWWK..",
            "..KWGGGGWK..",
            "..KWWWWWWK..",
            "..KWWWWWWK..",
            "...KKKKKK..."
        ))

        RepairMaterial.PORCELAIN_SHARD -> PixelArtSprites.sprite("mat_porcelain", listOf(
            ".....KK.....",
            "...KPPPPK...",
            "..KPWWWWPK..",
            ".KPPWLLWPPK.",
            "..KPPWWPPK..",
            "...KPPPPK...",
            "....KKKK...."
        ))

        RepairMaterial.SPRING -> PixelArtSprites.sprite("mat_spring", listOf(
            "..KKKKKKKK..",
            ".KCCCCCCCCK.",
            "..KKKKKKKK..",
            ".KGGGGGGGGK.",
            "..KKKKKKKK..",
            ".KCCCCCCCCK.",
            "..KKKKKKKK.."
        ))
    }

    fun forObjectiveProp(kind: String): ImageBitmap = when (kind) {
        "crying_box" -> PixelArtSprites.sprite("obj_crying_box", listOf(
            "..KKKKKKKKKK..",
            ".KBBBBBBBBBBK.",
            ".KBBLLBBLLBBK.",
            ".KBBLWKKWLBBK.",
            ".KBBLLKKLLBBK.",
            ".KBBBBBBBBBBK.",
            ".KGGGGGGGGGGK.",
            "..KKKKKKKKKK.."
        ))

        "delivery_box" -> PixelArtSprites.sprite("obj_delivery_box", listOf(
            "..KKKKKKKKKK..",
            ".KTTTTTTTTTTK.",
            ".KTTGGGGGGTTK.",
            ".KTWWGGGGWWTK.",
            ".KTTTTTTTTTTK.",
            ".KTTTGGGGTTTK.",
            "..KKKKKKKKKK.."
        ))

        "gear_token" -> forMaterial(RepairMaterial.GEAR)

        else -> PixelArtSprites.sprite("obj_alarm_bell", listOf(
            "....KKKK....",
            "...KGGGGK...",
            "..KGGWWGGK..",
            "..KGGGGGGK..",
            ".KGGGGGGGGK.",
            ".KKKKKKKKKK.",
            "....KGGK....",
            ".....KK....."
        ))
    }
}
