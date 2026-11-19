package com.example.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import com.example.data.model.InteractableType
import com.example.data.model.LightSourceType

/**
 * Pixel-art sprites for the Master Expansion:
 * - 5 new enemies/bosses (Veiled Specter, Lamp Snuffer, Clockwork Sentinel, Boss Subsuelo, Boss Fabricante)
 * - 3 new interactables (Notice Board, Floor Lamp, Recharge Station)
 * - 6 Light Sources + Lost Toy Token
 * - Boss Arena Props (surprise_box, puppet_anchor, rail_switch, overheat_vent)
 */
object PixelArtMasterSprites {

    fun veiledSpecter(vulnerable: Boolean = false): ImageBitmap = PixelArtSprites.sprite(
        if (vulnerable) "enemy_veiled_vuln" else "enemy_veiled_lit",
        if (vulnerable) listOf(
            ".....KKKKKK.....",
            "....KVVVVVVK....",
            "...KVVWWWWVVK...",
            "...KVWKEEKWVK...",
            "...KVVWWWWVVK...",
            "..KKDDDDDDDDKK..",
            ".KDDKVVVVVVKDVK.",
            ".KD.KDDDDDDK.DK.",
            "....KDDVVDDK....",
            "...KDDK..KDDK...",
            "..KDDK....KDDK..",
            "..KKK......KKK.."
        ) else listOf(
            ".....KKKKKK.....",
            "....KWWWWWWK....",
            "...KWWPPPPWWK...",
            "...KWPWKKWPWK...",
            "...KWWPPPPWWK...",
            "..KKCCCCCCCCKK..",
            ".KCCKWWWWWWKCCK.",
            ".KC.KCCCCCCK.CK.",
            "....KCCWWCCK....",
            "...KCCK..KCCK...",
            "..KCCK....KCCK..",
            "..KKK......KKK.."
        )
    )

    fun lampSnuffer(): ImageBitmap = PixelArtSprites.sprite("enemy_lamp_snuffer", listOf(
        "....KKKKKKKK....",
        "...KDDDDDDDDK...",
        "..KDDYYKKYYDDK..",
        "..KDDDDDDDDDDK..",
        "...KKDDDDDDKK...",
        "..KKDDDDDDDDKK..",
        ".KDDKDDDDDDKGGK.",
        ".KD.KDDGGDDK.GK.",
        "....KDDDDDDK....",
        "...KDDK..KDDK...",
        "..KDDK....KDDK..",
        "..KKK......KKK.."
    ))

    fun clockworkSentinel(): ImageBitmap = PixelArtSprites.sprite("enemy_clockwork_sentinel", listOf(
        "....KKKKKKKK....",
        "...KGGGGGGGGK...",
        "..KGCGGGGGGCGK..",
        "..KCCKEKKKECCK..",
        "..KCCCCCCCCCCK..",
        ".KKGGGGGGGGGGKK.",
        "KCCKCCGGGGCCKCCK",
        "KCCKCCCCCCCCKCCK",
        "KCCKCCcKKcCCKCCK",
        ".KK.KCCCCCCK.KK.",
        "....KCCKKCCK....",
        "...KGGK..KGGK...",
        "...KKK....KKK..."
    ))

    fun bossSubsuelo(): ImageBitmap = PixelArtSprites.sprite("boss_subsuelo", listOf(
        "....KKK..KKKKKK....",
        "...KYYK.KCCCCCCK...",
        "....KK..KCCGGCCK...",
        "..KKKKKKKKKKKKKKK..",
        ".KCCGGGGGGGGGGGGCK.",
        ".KCcKWWKEKWWKEKWCcK",
        ".KCCCCCCCCCCCCCCCCK",
        "KKCCCRRRRRRRRRRCCCK",
        "KGGGGGGGGGGGGGGGGGK",
        ".KKKKKKKKKKKKKKKKK.",
        "..KDDK..KDDK..KDDK.",
        "...KK....KK....KK.."
    ))

    fun bossFabricante(): ImageBitmap = PixelArtSprites.sprite("boss_fabricante", listOf(
        ".....KKKKKKKKKK.....",
        "....KDDDDDDDDDDK....",
        "...KDDGGGGGGGGDDK...",
        "...KDDDDDDDDDDDDK...",
        "..KKKPPPPPPPPPPKKK..",
        "...KPWWWWWWWWWWPK...",
        "...KPWKEKKKKKEWPK...",
        "...KPWKKDDDDKKWPK...",
        "....KPPPPPPPPPPK....",
        "..KKKGGDDDDDDGGKKK..",
        ".KDDKDDGGGGGGDDKDDK.",
        ".KGGKDBBRRRRBBbKGGK.",
        ".KDD.KDBBGGbbDK.DDK.",
        ".....KDBBBBBBDK.....",
        "....KDDDK..KDDDK....",
        "....KKKK....KKKK...."
    ))

    fun forMasterInteractable(
        type: InteractableType,
        active: Boolean = false
    ): ImageBitmap = when (type) {
        InteractableType.NOTICE_BOARD -> PixelArtSprites.sprite("int_notice_board", listOf(
            "KKKKKKKKKKKKKKKK",
            "KBBBBBBBBBBBBBBK",
            "KBTWWWTWWWTGGTBK",
            "KBTWPWTWRWTWWTBK",
            "KBTWWWTWWWTGGTBK",
            "KBBBBBBBBBBBBBBK",
            ".KBBK......KBBK.",
            ".KBBK......KBBK.",
            ".KKKK......KKKK."
        ))

        InteractableType.FLOOR_LAMP -> PixelArtSprites.sprite(
            if (active) "int_floor_lamp_on" else "int_floor_lamp_off",
            if (active) listOf(
                "...KKKKKKKK...",
                "..KYYYYYYYYK..",
                ".KYYWWWWWWYYK.",
                ".KKKKKKKKKKKK.",
                ".....KGGK.....",
                ".....KGGK.....",
                ".....KGGK.....",
                "...KKGGGGKK...",
                "...KKKKKKKK..."
            ) else listOf(
                "...KKKKKKKK...",
                "..KSSSSSSSSK..",
                ".KSSCCCCCCSSK.",
                ".KKKKKKKKKKKK.",
                ".....KccK.....",
                ".....KccK.....",
                ".....KccK.....",
                "...KKccccKK...",
                "...KKKKKKKK..."
            )
        )

        InteractableType.RECHARGE_STATION -> PixelArtSprites.sprite(
            if (active) "int_recharge_on" else "int_recharge_off",
            if (active) listOf(
                "..KKKKKKKKKK..",
                ".KCCCCCCCCCCK.",
                ".KCYYGGGGYYCK.",
                ".KCYWWWWWWYCK.",
                ".KCYYGGGGYYCK.",
                ".KCCCCCCCCCCK.",
                "KGGGGGGGGGGGGK",
                "KKKKKKKKKKKKKK"
            ) else listOf(
                "..KKKKKKKKKK..",
                ".KCCCCCCCCCCK.",
                ".KCCccccccCCK.",
                ".KCCcWWWWcCCK.",
                ".KCCccccccCCK.",
                ".KCCCCCCCCCCK.",
                "KccccccccccccK",
                "KKKKKKKKKKKKKK"
            )
        )

        else -> PixelArtExpandedSprites.forObjectiveProp("alarm_bell")
    }

    fun forLightSource(type: LightSourceType): ImageBitmap = when (type) {
        LightSourceType.NONE -> PixelArtSprites.sprite("light_none", listOf(
            "..KKKK..",
            ".KDDDDK.",
            "KDDWWDDK",
            "KDDWWDDK",
            ".KDDDDK.",
            "..KKKK.."
        ))
        LightSourceType.CANDLE -> PixelArtSprites.sprite("light_candle", listOf(
            "...YY...",
            "..YWWY..",
            "...YY...",
            "..KKKK..",
            ".KWWWWK.",
            ".KWWWWK.",
            ".KPPPPK.",
            "KGGGGGGK",
            "KKKKKKKK"
        ))
        LightSourceType.FLASHLIGHT -> PixelArtSprites.sprite("light_flashlight", listOf(
            ".....KKKKKKK..",
            "....KCCCCCCCYY",
            "KKKKKCCGGCCCYY",
            "KCCCCCCCKKKKYY",
            "KKKKKKKK......"
        ))
        LightSourceType.PORTABLE_LAMP -> PixelArtSprites.sprite("light_portable", listOf(
            "..KKKKKK..",
            ".KCCCCCCK.",
            ".KCYYYYCK.",
            ".KCYWWYCK.",
            ".KCYYYYCK.",
            "KGGGGGGGGK",
            "KKKKKKKKKK"
        ))
        LightSourceType.MECHANICAL_LANTERN -> PixelArtSprites.sprite("light_lantern", listOf(
            "...KKKK...",
            "..KGGGGK..",
            ".KGWWWWGK.",
            ".KGYYYYGK.",
            ".KGWWWWGK.",
            "KGGGGGGGGK",
            "KKKKKKKKKK"
        ))
        LightSourceType.GLOWING_TOY -> PixelArtSprites.sprite("light_glowing_toy", listOf(
            "..KK..KK..",
            ".KYYKKYYK.",
            ".KYYYYYYK.",
            "KYYWKKWYYK",
            "KYYYYYYYYK",
            ".KYYMMYYK.",
            "..KKKKKK.."
        ))
        LightSourceType.LUMINOUS_EYE -> PixelArtSprites.sprite("light_luminous_eye", listOf(
            "..KKKKKK..",
            ".KWWWWWWK.",
            "KWWYYYYWWK",
            "KWYYKKYYWK",
            "KWWYYYYWWK",
            ".KWWWWWWK.",
            "..KKKKKK.."
        ))
    }

    fun lostToyToken(): ImageBitmap = PixelArtSprites.sprite("pickup_lost_toy", listOf(
        "..KK..KK..",
        ".KBBKKBBK.",
        ".KBBBBBBK.",
        "KBBWKKWBBK",
        ".KBBGGbBK.",
        ".KRRRRRRK.",
        "..KBBBBK..",
        "..KK..KK.."
    ))

    fun forBossProp(kind: String, active: Boolean = true): ImageBitmap = when (kind) {
        "surprise_box" -> PixelArtSprites.sprite("boss_prop_box", listOf(
            "....KKKKKK....",
            "...KRRRRRRK...",
            "..KKKKKKKKKK..",
            ".KLLGGGGGGLLK.",
            ".KLLRRWWRRLLK.",
            ".KLLGGGGGGLLK.",
            ".KRRLLLLLLRRK.",
            "..KKKKKKKKKK.."
        ))
        "puppet_anchor" -> PixelArtSprites.sprite(
            if (active) "boss_prop_anchor_on" else "boss_prop_anchor_off",
            if (active) listOf(
                "....W....W....",
                "...KKWWWWKK...",
                "..KVVPPPPVVk..",
                "..KVPKKKKPVK..",
                "..KVVPPPPVVk..",
                "...KKKKKKKK...",
                "....KCCCCK....",
                "...KKKKKKKK..."
            ) else listOf(
                "..............",
                "..............",
                "..KK......KK..",
                ".KVVk....KCCK.",
                "..KKKCCCCKKK..",
                "....KCCCCK...."
            )
        )
        "rail_switch" -> PixelArtExpandedSprites.forInteractable(InteractableType.WALL_LEVER, active = !active)
        else -> PixelArtExpandedSprites.forInteractable(InteractableType.STEAM_VENT, active = active)
    }
}
