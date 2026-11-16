package com.example.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import com.example.data.model.BackpackItemKind
import com.example.data.model.BackpackTier
import com.example.data.model.LoreProp
import com.example.data.model.LorePropKind

object PixelArtAmpliacionSprites {

    fun forBackpackTier(tier: BackpackTier): ImageBitmap = when (tier) {
        BackpackTier.STITCHED_POUCH -> PixelArtSprites.sprite("pack_stitched", listOf(
            "....KKKKKK....",
            "...KTTTTTTK...",
            "..KTTTTTTTTK..",
            "..KTTGGGGTTK..",
            ".KTTTTTTTTTTK.",
            ".KTTbTTTTbTTK.",
            ".KTbTTTTTTbTK.",
            ".KTTTTTTTTTTK.",
            "..KTTTTTTTTK..",
            "...KKKKKKKK..."
        ))
        BackpackTier.KIDS_BACKPACK -> PixelArtSprites.sprite("pack_kids", listOf(
            "....KKKKKK....",
            "...KRRRRRRK...",
            "..KRRRRRRRRK..",
            ".KRRGGGGGGGRK.",
            ".KRRRRRRRRRRK.",
            ".KRLLLLLLLLRK.",
            ".KRLWWGGWWLRK.",
            ".KRLLLLLLLLRK.",
            ".KRRRRRRRRRRK.",
            "..KKKKKKKKKK.."
        ))
        BackpackTier.EXPLORER_PACK -> PixelArtSprites.sprite("pack_explorer", listOf(
            "....KKKKKK....",
            "...KMMMMMMK...",
            "..KMBBBBBBMK..",
            ".KMBGGGGGGbMK.",
            ".KMBBBBBBBBMK.",
            ".KMBBCCCCBBMK.",
            ".KMBBGGGGbbMK.",
            ".KMBBBBBBBBMK.",
            ".KMMBBBBBBMMK.",
            "..KKKKKKKKKK.."
        ))
        BackpackTier.MANUFACTURER_PACK -> PixelArtSprites.sprite("pack_manufacturer", listOf(
            "....KKKKKK....",
            "...KDDDDDDK...",
            "..KDDGGGGDDK..",
            ".KDDGPPPPPGDK.",
            ".KDDDPWWPDDDK.",
            ".KDDGGGGGGDDK.",
            ".KDDDVrrVDDDK.",
            ".KDDDVrrVDDDK.",
            ".KDDDDDDDDDDK.",
            "..KKKKKKKKKK.."
        ))
        BackpackTier.BOTTOMLESS_PACK -> PixelArtSprites.sprite("pack_bottomless", listOf(
            "....KKKKKK....",
            "...KVVVVVVK...",
            "..KVDDDDDDVK..",
            ".KVDDEEEEDDVk.".uppercase().replace('K', 'K'),
            ".KVDDYYYYDDVK.",
            ".KVDDVKKVDDVK.",
            ".KVDDDDDDDDVK.",
            ".KVVDDDDDDVVK.",
            ".KVVVVVVVVVVK.",
            "..KKKKKKKKKK.."
        ))
    }

    fun forBackpackItem(kind: BackpackItemKind): ImageBitmap = when (kind) {
        BackpackItemKind.EXTRA_HEART -> PixelArtSprites.pickupHeart()
        BackpackItemKind.BOMB_PACK -> PixelArtSprites.sprite("bp_bomb", listOf(
            ".....KKYY.....",
            "....KGG.......",
            "...KKKKKK.....",
            "..KDDDDDDK....",
            ".KDDWWWWDDK...",
            ".KDDDDDDDDK...",
            ".KDDDDDDDDK...",
            "..KDDDDDDK....",
            "...KKKKKK....."
        ))
        BackpackItemKind.BRASS_KEY -> PixelArtSprites.sprite("bp_key", listOf(
            "..KKKK........",
            ".KGGGGK.......",
            ".KGGKGGK......",
            ".KGGGGKKKKKKKK",
            "..KKKKGGGGGGGK",
            "..........KGKK",
            "..........KGK."
        ))
        BackpackItemKind.SEWING_KIT_ITEM -> PixelArtSprites.sprite("bp_sewing_kit", listOf(
            "..KKKKKKKKKK..",
            ".KRRRRRRRRRRK.",
            ".KRWWWWWWWWRK.",
            ".KRRCCCCCCRRK.",
            ".KRRGGWWGGRRK.",
            ".KRRRRRRRRRRK.",
            "..KKKKKKKKKK.."
        ))
        BackpackItemKind.OFFENSIVE_SPRING -> PixelArtSprites.sprite("bp_spring", listOf(
            "..KKKKKKKK....",
            ".KCCCCCCCCK...",
            "..KKGGGGKK....",
            ".KCCCCCCCCK...",
            "..KKGGGGKK....",
            ".KCCCCCCCCK...",
            "..KKKKKKKK...."
        ))
        BackpackItemKind.SHIELD_CHARM -> PixelArtSprites.sprite("bp_shield", listOf(
            "..KKKKKKKKKK..",
            ".KLLGGGGGGLLK.",
            ".KLLGWWWWGLLK.",
            ".KLLGGGGGGLLK.",
            "..KLLLLLLLLK..",
            "...KLLLLLLK...",
            "....KLLLLK....",
            ".....KKKK....."
        ))
        BackpackItemKind.CRAFTING_BUNDLE -> PixelArtSprites.sprite("bp_bundle", listOf(
            "..KKKKKKKKKK..",
            ".KTTTTTTTTTTK.",
            "KTTKRRRRRRKTTK",
            "KTTKLLLLLLKTTK",
            "KTTTTTTTTTTTTK",
            ".KGGGGGGGGGGK.",
            "..KKKKKKKKKK.."
        ))
        BackpackItemKind.SMALL_PART_SPARE -> PixelArtSprites.sprite("bp_part_spare", listOf(
            "..KKKKKKKKKK..",
            ".KPPPPPPPPPPK.",
            ".KPPCCCCCCPPK.",
            ".KPPCGGGGCPPK.",
            ".KPPCCCCCCPPK.",
            ".KPPPPPPPPPPK.",
            "..KKKKKKKKKK.."
        ))
        BackpackItemKind.LARGE_RELIC_BOX -> PixelArtSprites.sprite("bp_relic_box", listOf(
            ".KKKKKKKKKKKK.",
            "KGGGGGGGGGGGGK",
            "KBBBBBBBBBBBBK",
            "KBGGKKKKKKGGBK",
            "KBBBBKEEKBBBBK",
            "KBBBBKKKKBBBBK",
            "KGGGGGGGGGGGGK",
            ".KKKKKKKKKKKK."
        ))
        BackpackItemKind.CONSUMABLE_MEMORY -> PixelArtSprites.sprite("bp_memory_jar", listOf(
            "....KKKKKK....",
            "....KBBBBK....",
            "...KCCCCCCK...",
            "..KCWWWWWWCK..",
            ".KCVVRRRRVVCK.",
            ".KCVRRRRRRVCK.",
            "..KCCCCCCCCK..",
            "...KKKKKKKK..."
        ))
    }

    fun forLoreProp(prop: LoreProp): ImageBitmap = when (prop.kind) {
        LorePropKind.DRAGGABLE_CHAIR -> PixelArtSprites.sprite("lore_chair", listOf(
            "..KK......KK..",
            "..KBK....KBK..",
            "..KBBKKKKBBK..",
            "..KBBBBBBBBK..",
            "..KKKKKKKKKK..",
            "..KBBBBBBBBK..",
            "..KBK....KBK..",
            "..KBK....KBK..",
            "..KK......KK.."
        ))
        LorePropKind.FLOOR_SWITCH -> PixelArtSprites.sprite(
            if (prop.activated) "lore_switch_on" else "lore_switch_off",
            if (prop.activated) listOf(
                "KKKKKKKKKKKK",
                "KCCCCCCCCCCK",
                "KCGGGGGGGGCK",
                "KCGMMMMMMGCK",
                "KCGMMMMMMGCK",
                "KCGGGGGGGGCK",
                "KCCCCCCCCCCK",
                "KKKKKKKKKKKK"
            ) else listOf(
                "KKKKKKKKKKKK",
                "KCCCCCCCCCCK",
                "KCRRRRRRRRCK",
                "KCRWWWWWWRcK".replace('c', 'C'),
                "KCRRRRRRRRCK",
                "KCCCCCCCCCCK",
                "KKKKKKKKKKKK"
            )
        )
        LorePropKind.STUCK_SHELF -> PixelArtSprites.sprite(
            if (prop.stateInt > 0) "lore_shelf_loose" else "lore_shelf_stuck",
            listOf(
                "KKKKKKKKKKKKKK",
                "KBBBBBBBBBBBBK",
                "KBRRLLMMGGBBBK",
                "KBBBBBBBBBBBBK",
                "KBLLRRGGGGMMBK",
                "KBBBBBBBBBBBBK",
                if (prop.stateInt > 0) "KGG........GGK" else "KCCKKKKKKKKCCK",
                "KKKKKKKKKKKKKK"
            )
        )
        LorePropKind.TOOL_CROWBAR -> PixelArtSprites.sprite("lore_crowbar", listOf(
            ".......KKKK...",
            "......KCCCRK..",
            ".....KCCCK....",
            "....KCCCK.....",
            "...KCCCK......",
            "..KCCCK.......",
            ".KCCCK........",
            ".KKKK........."
        ))
        LorePropKind.ROTATABLE_PAINTING -> PixelArtSprites.sprite("lore_painting", listOf(
            "KKKKKKKKKKKKKK",
            "KGGGGGGGGGGGGK",
            "KGPPPPPPPPPPGK",
            "KGPBBBBBBBBPGK",
            "KGPBKEEBKEBPGK",
            "KGPBRRRRRRBPGK",
            "KGGGGGGGGGGGGK",
            "KKKKKKKKKKKKKK"
        ))
        LorePropKind.BROKEN_MUSIC_BOX -> PixelArtSprites.sprite("lore_music_box", listOf(
            "..KKKKKKKKKK..",
            ".KGGGGGGGGGGK.",
            "KBBBBBBBBBBBBK",
            "KBWWGGGGGGWWBK",
            "KBWGGGGGGGGWBK",
            "KBBBBBBBBBBBBK",
            ".KKKKKKKKKKKK."
        ))
        LorePropKind.TOOL_CRANK -> PixelArtSprites.sprite("lore_crank", listOf(
            "....KKKKKK....",
            "....KGGGGK....",
            "....KGKK......",
            "..KKKGK.......",
            ".KGGGGK.......",
            ".KKKKK........"
        ))
        LorePropKind.TOOL_GEAR -> PixelArtSprites.sprite("lore_gear", listOf(
            "...KK..KK...",
            ".KKGGGGGGKK.",
            "KGGKKGGKKGGK",
            ".KGGK..KGGK.",
            "KGGKKGGKKGGK",
            ".KKGGGGGGKK.",
            "...KK..KK..."
        ))
        LorePropKind.ADJUSTABLE_CLOCK -> PixelArtSprites.sprite("lore_clock", listOf(
            "...KKKKKKKK...",
            "..KGGGGGGGGK..",
            ".KGWWWWWWWWGK.",
            ".KGWKKKKWWWGK.",
            ".KGWKWWWKWWGK.",
            ".KGWWWWWWWWGK.",
            "..KGGGGGGGGK..",
            "...KKKKKKKK..."
        ))
        LorePropKind.TRAPPED_DOLL_CAGE -> PixelArtSprites.sprite(
            if (prop.solved) "lore_cage_open" else "lore_cage_locked",
            listOf(
                "..KKKKKKKKKK..",
                ".KCCCCCCCCCCK.",
                "KCKPPKPPKPPKCK",
                "KCKPEKPPKEPKCK",
                "KCKRRRRRRRRKCK",
                "KCCCCCCCCCCCCK",
                ".KKKKKKKKKKKK."
            )
        )
        LorePropKind.CAGE_KEY -> PixelArtSprites.sprite("lore_cage_key", listOf(
            "..KKKK......",
            ".KGGGGK.....",
            ".KGGGGKKKKKK",
            "..KKKKGGGGGK",
            "........KGKK"
        ))
        LorePropKind.DRAGGABLE_CRATE -> PixelArtSprites.crateObstacle()
        LorePropKind.POWER_LEVER -> PixelArtSprites.sprite(
            if (prop.activated) "lore_lever_on" else "lore_lever_off",
            listOf(
                if (prop.activated) "......KKKK.." else "..KKKK......",
                if (prop.activated) ".....KRRRRK." else ".KRRRRK.....",
                "....KKCCKK..",
                "..KKKKKKKK..",
                ".KCCCCCCCCK.",
                ".KKKKKKKKKK."
            )
        )
        LorePropKind.ELECTRIFIED_ZONE -> PixelArtSprites.sprite(
            if (prop.activated) "lore_elec_safe" else "lore_elec_live",
            if (prop.activated) listOf(
                "KKKKKKKKKKKK",
                "KMMMMMMMMMMK",
                "KMCCCCCCCCMK",
                "KMMMMMMMMMMK",
                "KKKKKKKKKKKK"
            ) else listOf(
                "KKKKKKKKKKKK",
                "KYYYYYYYYYYK",
                "KYLLYYYYLLYK",
                "KYYYYYYYYYYK",
                "KKKKKKKKKKKK"
            )
        )
        LorePropKind.ROTATABLE_DOLL -> PixelArtSprites.sprite("lore_rot_doll", listOf(
            "...KKKKKKKK...",
            "..KHHHHHHHHK..",
            "..KPPPPPPPPK..",
            "..KPKEKKKEPK..",
            "..KPRRRRRRPK..",
            "...KRRRRRRK...",
            "..KPPKKKKPPK..",
            "..KKK....KKK.."
        ))
        LorePropKind.WALL_HINT_PANEL -> PixelArtSprites.sprite("lore_wall_hint", listOf(
            "KKKKKKKKKKKKKK",
            "KWWWWWWWWWWWWK",
            "KWKKKKKKWWWWWK",
            "KWWWWKKKKWWWWK",
            "KWKKKKKKKKWWWK",
            "KWWWWWWWWWWWWK",
            "KKKKKKKKKKKKKK"
        ))
        LorePropKind.FLOOR_TILE_HOLLOW -> PixelArtSprites.sprite(
            if (prop.stateInt > 0) "lore_tile_hollow_found" else "lore_tile_wood",
            listOf(
                "KKKKKKKKKKKK",
                "KBBBBBBBBBBK",
                if (prop.stateInt > 0) "KBGGGGGGGGBK" else "KBBBBBBBBBBK",
                "KBKKKKKKKKBK",
                "KBBBBBBBBBBK",
                "KKKKKKKKKKKK"
            )
        )
        LorePropKind.FLOOR_TILE_TRAP -> PixelArtSprites.sprite("lore_tile_trap", listOf(
            "KKKKKKKKKKKK",
            "KBBBBBBBBBBK",
            "KBBbbBBbbBBK",
            "KBBBBBBBBBBK",
            "KKKKKKKKKKKK"
        ))
        LorePropKind.TOY_FIGURE_SOLDIER -> PixelArtSprites.forEnemy(com.example.data.model.EnemyType.TIN_SOLDIER)
        LorePropKind.TOY_FIGURE_DOLL -> PixelArtSprites.forEnemy(com.example.data.model.EnemyType.CREEPY_DOLL, disguised = true)
        LorePropKind.TOY_FIGURE_BUNNY -> PixelArtSprites.forCompanion(com.example.data.model.CompanionId.BUNNY)
        LorePropKind.PEDESTAL_SLOT -> PixelArtSprites.sprite(
            if (prop.stateInt > 0) "lore_pedestal_full" else "lore_pedestal_empty",
            listOf(
                "..KKKKKKKK..",
                ".KGGGGGGGGK.",
                "KBBBBBBBBBBK",
                "KBBGGGGGGbBK".replace('b', 'B'),
                "KBBBBBBBBBBK",
                ".KKKKKKKKKK."
            )
        )
        LorePropKind.DESTRUCTIBLE_CRATE -> PixelArtSprites.crateObstacle()
        LorePropKind.SECRET_COMPARTMENT -> PixelArtSprites.sprite(
            if (prop.solved) "lore_comp_open" else "lore_comp_closed",
            listOf(
                "KKKKKKKKKKKK",
                "KGGGGGGGGGGK",
                if (prop.solved) "KGWWWWWWWWGK" else "KBBBBKGGKBBK",
                if (prop.solved) "KGWWWWWWWWGK" else "KBBBBBBBBBBK",
                "KGGGGGGGGGGK",
                "KKKKKKKKKKKK"
            )
        )
        LorePropKind.DIARY_DESK -> PixelArtSprites.sprite("lore_diary_desk", listOf(
            "KKKKKKKKKKKKKK",
            "KBBBBBBBBBBBBK",
            "KBBWWWWWWWWBBK",
            "KBBWKKKKKKWBBK",
            "KBBBBBBBBBBBBK",
            "KKKKKKKKKKKKKK"
        ))
    }
}
