package com.example.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import com.example.data.model.BodyZone
import com.example.data.model.ConsumableType

/**
 * Representative dark-outlined pixel-art icons for all Passive Items, Consumables,
 * Body Parts, Obstacles, and Sewing-Room Props.
 */
internal object PixelArtObjectSprites {

    fun forPassiveItem(itemId: String): ImageBitmap = when (itemId) {
        "eyeless_bear" -> PixelArtSprites.sprite("item_eyeless_bear", listOf(
            "..KKK......KKK..",
            ".KBBBK....KBBBK.",
            ".KBbBKKKKKKBbBK.",
            ".KBBBBBBBBBBBBK.",
            ".KBKKBBBBBBKKBK.",
            ".KBKKBBBBBBKKBK.",
            ".KBBBBPWWPBBBBK.",
            "..KBBBPKKPBBBK..",
            "...KKKKRRKKKK...",
            "..KBBbKRRKbBBK..",
            "..KBBBBBBBBBBK..",
            "...KKKKKKKKKK..."
        ))
        "music_box_item" -> PixelArtSprites.sprite("item_music_box", listOf(
            ".....KKKKKK.....",
            "....KGGGGGGK....",
            "...KGGGGGGGGK...",
            "..KKKKKKKKKKKK..",
            ".KBBBBBBBBBBBBK.",
            ".KBGGGGGGGGGGbK.",
            ".KBBKKBWWBKKBBK.",
            ".KBBGGBWWBGGBBK.",
            ".KBBBBBBBBBBBBK.",
            ".KBGGGGGGGGGGbK.",
            "..KKKKKKKKKKKK.."
        ))
        "rusty_key" -> PixelArtSprites.sprite("item_rusty_key", listOf(
            "....KKKKKK......",
            "...KbbbbbbK.....",
            "..KbbKKKKbbK....",
            "..KbK....KbK....",
            "..KbbKKKKbbK....",
            "...KbbbbbbK.....",
            ".....KbbK.......",
            ".....KbbKKK.....",
            ".....KbbK.......",
            ".....KbbKKKK....",
            ".....KbbKbbK....",
            ".....KKKKKK....."
        ))
        "porcelain_tooth" -> PixelArtSprites.sprite("item_tooth", listOf(
            "...KKKKKKKKKK...",
            "..KWWWWWWWWWWK..",
            ".KWWWWPPPPWWWWK.",
            ".KWWWWPPPPWWWWK.",
            ".KPPPPPPPPPPPPK.",
            "..KPPPPKKPPPPK..",
            "..KPPPK..KPPPK..",
            "..KPPK....KPPK..",
            "...KK......KK..."
        ))
        "handless_watch" -> PixelArtSprites.sprite("item_watch", listOf(
            ".....KKKKKK.....",
            "....KGGGGGGK....",
            "...KKKKKKKKKK...",
            "..KGGWWWWWWGGK..",
            ".KGGWWWWWWWWGGK.",
            ".KGWKKKKKKKKWGK.",
            ".KGWKWWWWWWKWGK.",
            ".KGGWWWWWWWWGGK.",
            "..KGGWWWWWWGGK..",
            "...KKKKKKKKKK..."
        ))
        "broken_doll" -> PixelArtSprites.sprite("item_broken_doll", listOf(
            "...KKK....KKK...",
            "..KHHHK..KHHHK..",
            "..KHHKKKKKKHHK..",
            "..KPPPPKKPPPPHK.",
            "..KPKEKPKPPPKHK.",
            "...KKPPPPPPKK...",
            "...KKRRRRRRKK...",
            "..KRRRRKKRRRRK..",
            "..KPRRRKKRRPK...",
            "...KPPK..KPPK...",
            "...KKK....KKK..."
        ))
        "cursed_button" -> PixelArtSprites.sprite("item_cursed_btn", listOf(
            "...KKKKKKKK...",
            "..KRRRRRRRRK..",
            ".KRRrRRRRrRRK.",
            "KRRKGRRRRGKRRK",
            "KRRRRRRRRRRRRK",
            "KRRKGRRRRGKRRK",
            ".KRRrRRRRrRRK.",
            "..KRRRRRRRRK..",
            "...KKKKKKKK..."
        ))
        "red_thread" -> PixelArtSprites.sprite("item_red_thread", listOf(
            "..KKKKKKKKKK..",
            ".KBBBBBBBBBBK.",
            "..KKKKKKKKKK..",
            "..KRRRRRRRRK..",
            "..KrrRRRRrrK..",
            "..KRRRRRRRRK..",
            "..KrrRRRRrrK..",
            "..KRRRRRRRRK..",
            "..KKKKKKKKKK..",
            ".KBBBBBBBBBBK.",
            "..KKKKKKKKKK..R",
            "..........RRRR"
        ))
        "needle_cushion" -> PixelArtSprites.sprite("item_needle_cushion", listOf(
            "..C........C..",
            "...C..CC..C...",
            "....CKKKKC....",
            "..KKRRRRRRKK..",
            ".KRRRRWWRRRRK.",
            "KRRRRRRRRRRRRK",
            "KrrrrRRRRrrrrK",
            ".KGGGGGGGGGGK.",
            "..KKKKKKKKKK.."
        ))
        "rubber_marble" -> PixelArtSprites.sprite("item_marble", listOf(
            "...KKKKKKKK...",
            "..KLLWWWWLLK..",
            ".KLLWWLLLLLLK.",
            "KLLLLGGGGLLLLK",
            "KLLLGGLLGGLLLK",
            "KLLLLLLLLLLLLK",
            ".KllLLLLLLllK.",
            "..KllllllllK..",
            "...KKKKKKKK..."
        ))
        "stuffed_heart" -> PixelArtSprites.sprite("item_stuffed_heart", listOf(
            ".KKKK....KKKK.",
            "KRRRRK..KRRRRK",
            "KRWWRRKKRRRRRK",
            "KRWRRRRRKRRRRK",
            "KRRRRRRKRRRRRK",
            ".KRRRRKRRRRRK.",
            "..KRRRKRRRRK..",
            "...KRRKRRRK...",
            "....KRRRRK....",
            ".....KKKK....."
        ))
        "magnet_coil" -> PixelArtSprites.sprite("item_magnet", listOf(
            "..KKKK..KKKK..",
            ".KCCCCKKCCCCK.",
            ".KCCCCKKCCCCK.",
            ".KRRRR..RRRRK.",
            ".KRRRR..RRRRK.",
            ".KRRRR..RRRRK.",
            ".KRRRRRRRRRRK.",
            "..KRRRRRRRRK..",
            "...KKKKKKKK..."
        ))
        "brass_thimble" -> PixelArtSprites.sprite("item_thimble", listOf(
            "....KKKKKK....",
            "...KGGGGGGK...",
            "..KGGgGGgGGK..",
            "..KGGGGGGGGK..",
            "..KGgGGGGgGK..",
            ".KGGGGGGGGGGK.",
            ".KGgGGgGGgGGK.",
            ".KGGGGGGGGGGK.",
            ".KKKKKKKKKKKK."
        ))
        "velvet_ribbon" -> PixelArtSprites.sprite("item_ribbon", listOf(
            ".KKK......KKK.",
            "KRRRK....KRRRK",
            "KRrRRKKKKRRrRK",
            ".KRRRRRRRRRRK.",
            "..KKKRRRRKKK..",
            "...KRRKKRRK...",
            "..KRRK..KRRK..",
            ".KRRK....KRRK.",
            ".KKK......KKK."
        ))
        "broken_music_key" -> PixelArtSprites.sprite("item_music_key", listOf(
            "..KKKK..KKKK..",
            ".KGGWWKKWWGGK.",
            ".KGK..KK..KGK.",
            "..KKKKKKKKKK..",
            ".....KGGK.....",
            ".....KGGK.....",
            ".....KGGK.....",
            "....KGGGGK....",
            ".....KKKK....."
        ))
        "silver_scissors" -> PixelArtSprites.sprite("item_scissors", listOf(
            "....K....K....",
            "...KCK..KCK...",
            "...KCK..KCK...",
            "....KCKKCK....",
            ".....KKKK.....",
            "....KGGKGGK...",
            "..KKGKKGKKGK..",
            ".KGGK..K..KGK.",
            "..KK......KK.."
        ))
        "marionette_cross" -> PixelArtSprites.sprite("item_marionette", listOf(
            ".....KKKK.....",
            ".....KBBK.....",
            ".KKKKKKKKKKKK.",
            "KBBBBBBBBBBBBK",
            ".KKKKKKKKKKKK.",
            ".....KBBK.....",
            "....K.KK.K....",
            "...K..KK..K...",
            "..K...KK...K..",
            ".W....W.....W."
        ))
        "glass_marble_bag" -> PixelArtSprites.sprite("item_marble_bag", listOf(
            "....KKKKKK....",
            "...KTTTTTTK...",
            "....KKGGKK....",
            "..KKTTTTTTKK..",
            ".KTTLLTTRRTTK.",
            ".KTTLLTTGGTTK.",
            ".KTTTTTTTTTTK.",
            "..KTTTbbTTTK..",
            "...KKKKKKKK..."
        ))
        "moth_lantern" -> PixelArtSprites.sprite("item_lantern", listOf(
            "....KKKKKK....",
            "...KGGGGGGK...",
            "....KYYYYK....",
            "..KKKKKKKKKK..",
            ".KGGYYYYYYGGK.",
            ".KGGYYWWYYGGK.",
            ".KGGYYYYYYGGK.",
            ".KGGYYYYYYGGK.",
            "..KKKKKKKKKK.."
        ))
        else -> PixelArtSprites.sprite("item_bloody_pin", listOf(
            ".....KKKK.....",
            "....KRRRRK....",
            "....KRRRRK....",
            ".....KKKK.....",
            "......KK......",
            "......CC......",
            "......CC......",
            "......CC......",
            "......CK......",
            ".......r......"
        ))
    }

    fun forConsumable(type: ConsumableType): ImageBitmap = when (type) {
        ConsumableType.BOMB -> PixelArtSprites.sprite("cons_bomb", listOf(
            ".......KKY....",
            "......KGG.....",
            "....KKKKKK....",
            "..KKDDDDDDKK..",
            ".KDDDDWWDDDDK.",
            ".KDDDDDDDDDDK.",
            ".KDDccDDDDDDK.",
            "..KDDDDDDDDK..",
            "...KKKKKKKK..."
        ))
        ConsumableType.SEWING_KIT -> PixelArtSprites.sprite("cons_kit", listOf(
            "..KKKKKKKKKK..",
            ".KRRRRRRRRRRK.",
            ".KRWWKKKKWWRK.",
            ".KRRWWKKWWRRK.",
            ".KRWWKKKKWWRK.",
            ".KRRRRRRRRRRK.",
            ".KGGGGGGGGGGK.",
            "..KKKKKKKKKK.."
        ))
        ConsumableType.KEY -> PixelArtSprites.sprite("cons_key", listOf(
            "...KKKKKK.....",
            "..KGGGGGGK....",
            ".KGGKKKKGGK...",
            ".KGGKKKKGGK...",
            "..KGGGGGGK....",
            "....KGGK......",
            "....KGGKKKK...",
            "....KGGKGGK...",
            "....KKKKKK...."
        ))
        ConsumableType.WINDUP_STRING -> PixelArtSprites.sprite("cons_windup", listOf(
            "..KKKK..KKKK..",
            ".KGGGGKKGGGGK.",
            ".KGK..KK..KGK.",
            "..KKKKKKKKKK..",
            ".....KGGK.....",
            "....KGGGGK....",
            ".....KKKK....."
        ))
        ConsumableType.SURPRISE_BOX -> PixelArtSprites.sprite("cons_surprise", listOf(
            "....KKKKKK....",
            "...KGGGGGGK...",
            "..KKKKKKKKKK..",
            ".KRRRRLLRRRRK.",
            ".KRRGGGGGGLLK.",
            ".KLLLLRRRRLLK.",
            ".KRRRRLLLLRRK.",
            "..KKKKKKKKKK.."
        ))
        ConsumableType.EXPLOSIVE_TOY -> PixelArtSprites.sprite("cons_exptoy", listOf(
            ".......YY.....",
            "......KK......",
            "..KKKKKKKKKK..",
            ".KRRRRRRRRRRK.",
            ".KRKEKRRKEKRK.",
            ".KRRWWWWWWWRK.",
            ".KRRRRRRRRRRK.",
            "..KKKKKKKKKK.."
        ))
        ConsumableType.SPRING_COIL -> PixelArtSprites.sprite("cons_spring", listOf(
            "...KKKKKKKK...",
            "..KCCCCCCCCK..",
            "...KKKKKKKK...",
            "..KCCCCCCCCK..",
            "...KKKKKKKK...",
            "..KCCCCCCCCK..",
            "...KKKKKKKK...",
            "..KCCCCCCCCK..",
            "...KKKKKKKK..."
        ))
        ConsumableType.REPAIR_PATCH -> PixelArtSprites.sprite("cons_patch", listOf(
            ".KKKKKKKKKKKK.",
            "KTTTTTTTTTTTTK",
            "KTKTTKTTKTTKTK",
            "KTTTMMMMMMTTTK",
            "KTKTTMMMMTTKTK",
            "KTTTTTTTTTTTTK",
            ".KKKKKKKKKKKK."
        ))
    }

    fun forBodyPart(partId: String, zone: BodyZone): ImageBitmap {
        val key = "part_${partId}_${zone.name}"
        return when (zone) {
            BodyZone.HEAD -> when (partId) {
                "porcelain_head" -> PixelArtSprites.sprite(key, listOf(
                    "...KKKKKKKK...",
                    "..KPPPPPPPPK..",
                    ".KPPKPPPPKPPK.",
                    ".KPPPPPPPPPPK.",
                    ".KPPPRRRRPPPK.",
                    "..KPPPPPPPPK..",
                    "...KKKKKKKK..."
                ))
                "clown_head" -> PixelArtSprites.sprite(key, listOf(
                    "..KKK....KKK..",
                    ".KRRKKKKKKLLK.",
                    "..KWWWWWWWWK..",
                    "..KWKEKWKEWK..",
                    "..KWWWRRWWWK..",
                    "...KKKKKKKK..."
                ))
                "clockwork_head" -> PixelArtSprites.sprite(key, listOf(
                    ".....KGGK.....",
                    "...KKKKKKKK...",
                    "..KCCCCCCCCK..",
                    "..KCYCCCCYCK..",
                    "..KCcKKKKcCK..",
                    "...KKKKKKKK..."
                ))
                else -> PixelArtSprites.sprite(key, listOf(
                    "...KKKKKKKK...",
                    "..KTTTTTTTTK..",
                    ".KTTKKTTTGTTK.",
                    ".KTTTTTTTTTTK.",
                    "..KTKKKKKKTK..",
                    "...KKKKKKKK..."
                ))
            }
            BodyZone.EYES -> when (partId) {
                "crimson_eye" -> PixelArtSprites.sprite(key, listOf(
                    "...KKKKKKKK...",
                    "..KRRRRRRRRK..",
                    ".KRREEEEEERRK.",
                    ".KRREKWWKERRK.",
                    "..KRRRRRRRRK..",
                    "...KKKKKKKK..."
                ))
                "mechanical_eye" -> PixelArtSprites.sprite(key, listOf(
                    "...KKKKKKKK...",
                    "..KCCCCCCCCK..",
                    ".KCCYYYYYYCCK.",
                    ".KCCYKKKKYCCK.",
                    "..KCCCCCCCCK..",
                    "...KKKKKKKK..."
                ))
                "button_eye_part" -> PixelArtSprites.sprite(key, listOf(
                    "...KKKKKKKK...",
                    "..KggggggggK..",
                    ".KggKggggKggK.",
                    ".KggKggggKggK.",
                    "..KggggggggK..",
                    "...KKKKKKKK..."
                ))
                else -> PixelArtSprites.sprite(key, listOf(
                    "...KKKKKKKK...",
                    "..KWWWWWWWWK..",
                    ".KWLLLLLLLLWK.",
                    ".KWLKKWWKKLWK.",
                    "..KWWWWWWWWK..",
                    "...KKKKKKKK..."
                ))
            }
            BodyZone.ARMS -> when (partId) {
                "scissor_arm" -> PixelArtSprites.sprite(key, listOf(
                    "....K....K....",
                    "...KCK..KCK...",
                    "....KCKKCK....",
                    "....KGGGGK....",
                    "...KGGKKGGK...",
                    "....KK..KK...."
                ))
                "spring_arm" -> PixelArtSprites.sprite(key, listOf(
                    "..KKKKKKKK....",
                    ".KCCCCCCCCK...",
                    "..KKKKKKKK....",
                    ".KCCCCCCCCK...",
                    "..KGGGGGGK....",
                    "...KKKKKK....."
                ))
                "explosive_arm" -> PixelArtSprites.sprite(key, listOf(
                    "......KKY.....",
                    "...KKKKKKK....",
                    "..KRRRRRRRK...",
                    ".KRRWWWRRRRK..",
                    "..KRRRRRRRK...",
                    "...KKKKKKK...."
                ))
                else -> PixelArtSprites.sprite(key, listOf(
                    "...KKKKKK.....",
                    "..KTTTTTTK....",
                    "..KTTKKTTK....",
                    "..KTTTTTTKK...",
                    "...KTTTTTTK...",
                    "....KKKKKK...."
                ))
            }
            BodyZone.TORSO -> when (partId) {
                "musical_torso" -> PixelArtSprites.sprite(key, listOf(
                    "..KKKKKKKKKK..",
                    ".KBBBBBBBBBBK.",
                    ".KBBGGGGGGbBK.",
                    ".KBBGWWWWGbBK.",
                    ".KBBBBBBBBBBK.",
                    "..KKKKKKKKKK.."
                ))
                "armored_torso" -> PixelArtSprites.sprite(key, listOf(
                    "..KKKKKKKKKK..",
                    ".KCCCCCCCCCCK.",
                    ".KCCcGGGGcCCK.",
                    ".KCCCCCCCCCCK.",
                    ".KCcCCCCCCcCK.",
                    "..KKKKKKKKKK.."
                ))
                "clock_torso" -> PixelArtSprites.sprite(key, listOf(
                    "..KKKKKKKKKK..",
                    ".KGGGGGGGGGGK.",
                    ".KGGWWWWWWGGK.",
                    ".KGGWKKWWWGGK.",
                    ".KGGGGGGGGGGK.",
                    "..KKKKKKKKKK.."
                ))
                else -> PixelArtSprites.sprite(key, listOf(
                    "..KKKKKKKKKK..",
                    ".KTTTTTTTTTTK.",
                    ".KTTTTGGTTTTK.",
                    ".KTTTTGGTTTTK.",
                    ".KTTTTTTTTTTK.",
                    "..KKKKKKKKKK.."
                ))
            }
            BodyZone.LEGS -> when (partId) {
                "wheel_legs" -> PixelArtSprites.sprite(key, listOf(
                    "...KK....KK...",
                    "..KCCK..KCCK..",
                    ".KGGGGKKGGGGK.",
                    ".KGKKGKKGKKGK.",
                    "..KKKK..KKKK.."
                ))
                "spider_legs" -> PixelArtSprites.sprite(key, listOf(
                    "K..KKKKKK..K..",
                    "KK.KDDDDK.KK..",
                    ".KKDDDDDDKK...",
                    "KK.KDDDDK.KK..",
                    "K..K....K..K.."
                ))
                else -> PixelArtSprites.sprite(key, listOf(
                    "..KKKK..KKKK..",
                    "..KTTK..KTTK..",
                    "..KTTK..KTTK..",
                    "..KbbK..KbbK..",
                    "..KKKK..KKKK.."
                ))
            }
            BodyZone.BACK -> when (partId) {
                "paper_wings" -> PixelArtSprites.sprite(key, listOf(
                    ".KKK....KKK...",
                    "KWWWK..KWWWK..",
                    "KWWPPKKPPWWK..",
                    ".KWWWWWWWWK...",
                    "..KK....KK...."
                ))
                "windup_key_back" -> PixelArtSprites.sprite(key, listOf(
                    "..KKKK..KKKK..",
                    ".KGGGGKKGGGGK.",
                    "..KKKKKKKKKK..",
                    ".....KGGK.....",
                    ".....KGGK.....",
                    ".....KKKK....."
                ))
                else -> PixelArtSprites.sprite(key, listOf(
                    "..KKKKKKKKKK..",
                    ".KTTTTTTTTTTK.",
                    ".KTMRRRRRRMTK.",
                    ".KTTTTTTTTTTK.",
                    "..KKKKKKKKKK.."
                ))
            }
        }
    }

    fun vitrineObstacle(): ImageBitmap = PixelArtSprites.sprite("obs_vitrine", listOf(
        "KKKKKKKKKKKK",
        "KLLLLLLLLLLK",
        "KLWWWWLLLLLK",
        "KLLWWLLLLLLK",
        "KLCCKKKKCCLK",
        "KLLCCCCCCLLK",
        "KLLCCCCCCLLK",
        "KLCCKKKKCCLK",
        "KBBBBBBBBBBK",
        "KBGGGGGGGGBK",
        "KBBBBBBBBBBK",
        "KKKKKKKKKKKK"
    ))

    fun pillarObstacle(): ImageBitmap = PixelArtSprites.sprite("obs_pillar", listOf(
        ".KKKKKKKKKK.",
        "KCCCCCCCCCCK",
        "KccCCCCCCccK",
        ".KCCCCCCCCK.",
        ".KCcCCCCcCK.",
        ".KCcGGGGcCK.",
        ".KCcCCCCcCK.",
        ".KCcCCCCcCK.",
        ".KCCCCCCCCK.",
        "KccCCCCCCccK",
        "KCCCCCCCCCCK",
        ".KKKKKKKKKK."
    ))

    fun plushPileObstacle(): ImageBitmap = PixelArtSprites.sprite("obs_plush_pile", listOf(
        "...KKKKKK...",
        "..KVVVVVVK..",
        ".KVKEKKVKVK.",
        ".KKKKKKKKKK.",
        "KRRKKBBBBKKK",
        "KRRRKBBBBBBK",
        "KRRRKBBWWBBK",
        "KKKKKKKKKKKK",
        "KLLGGGGGGGGK",
        "KLLLLRRRRLLK",
        "KKKKKKKKKKKK"
    ))

    fun sewingTableProp(): ImageBitmap = PixelArtSprites.sprite("prop_sewing_table", listOf(
        "....KK..C...........",
        "...KGGK.CC..........",
        "..KKKKKKKKKKKKKKKK..",
        ".KBBBBBBBBBBBBBBBBK.",
        "KBBrrrrrrBBBBGGGGGBK",
        "KBrrrrrrrrBBGGWWWWBK",
        "KBBrrrrrrBBBBBBBBBBK",
        ".KKKKKKKKKKKKKKKKKK.",
        ".KBBK..........KBBK.",
        ".KbbK..........KbbK.",
        ".KKKK..........KKKK."
    ))

    fun threadSpoolsProp(): ImageBitmap = PixelArtSprites.sprite("prop_spools", listOf(
        ".KKKKK..KKKKK.",
        "KBBBBBK.KGGGGG",
        "KRRRRRK.KMMMMM",
        "KrrRRrK.KmMMmM",
        "KRRRRRK.KMMMMM",
        "KBBBBBK.KGGGGG",
        ".KKKKK..KKKKK."
    ))

    fun mannequinProp(): ImageBitmap = PixelArtSprites.sprite("prop_mannequin", listOf(
        "....KKKK....",
        "...KBBBBK...",
        "....KKKK....",
        "..KKTTTTKK..",
        ".KTTTTTTTTK.",
        ".KTTKGGKTTK.",
        "..KTTTTTTK..",
        "...KTTTTK...",
        "....KKKK....",
        ".....KK.....",
        "...KKKKKK...",
        "...KbbbbK..."
    ))

    fun scissorsProp(): ImageBitmap = PixelArtSprites.sprite("prop_scissors", listOf(
        "....K....K....",
        "...KCK..KCK...",
        "....KCKKCK....",
        ".....KKKK.....",
        "....KGGKGGK...",
        "..KKGKKGKKGK..",
        "..KK......KK.."
    ))
}
