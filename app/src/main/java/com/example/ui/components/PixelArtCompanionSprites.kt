package com.example.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import com.example.data.model.CompanionId

/**
 * Custom dark-outlined pixel-art sprites for all 8 conditional toy companions (including broken states),
 * betrayed/mirror/weeping enemies, nursery maze wooden blocks, and companion shrines.
 */
object PixelArtCompanionSprites {

    fun forCompanion(id: CompanionId, broken: Boolean = false): ImageBitmap {
        return when (id) {
            CompanionId.BUNNY -> PixelArtSprites.sprite("comp_bunny", listOf(
                "..KKK......KKK..",
                "..KWWK....KWWK..",
                "..KWPWK..KWPWK..",
                "..KWWWK..KWWWK..",
                "..KWWWKKKKWWWK..",
                ".KWWWWWWWWWWWWK.",
                ".KWWKKWWWWKKWWK.",
                ".KWWWWWRRWWWWWK.",
                "..KKKWWWWWWKKK..",
                "...KWKRRRRKWK...",
                "..KWWWKGGKWWWK..",
                "..KWWKWWWWKWWK..",
                "...KK.KWWK.KK...",
                ".....KWKKWK.....",
                "....KWWKKWWK....",
                "....KKKKKKKK...."
            ))

            CompanionId.FACELESS_DOLL -> PixelArtSprites.sprite("comp_faceless_doll", listOf(
                "...KKK....KKK...",
                "..KDDDK..KDDDK..",
                "..KDDKKKKKKDDK..",
                "..KDPPPPPPPPDK..",
                "..KDPPPPPPPPDK..",
                "..KDPPKKKKPPDK..",
                "..KDPPPPPPPPDK..",
                "...KKPPPPPPKK...",
                "..KKrrrrrrrrKK..",
                ".KPPKrrrrrrKPPK.",
                "..KKKrrrrrrKKK..",
                "....KrrrrrrK....",
                "....KPPKKPPK....",
                "....KKK..KKK...."
            ))

            CompanionId.BROKEN_SOLDIER -> if (broken) {
                PixelArtSprites.sprite("comp_soldier_broken", listOf(
                    ".....KKKKKK.....",
                    "....KccccccK....",
                    ".....KKccKK.....",
                    "....KPPPPPPK....",
                    "....KPKKKKPK....",
                    "....KPKDDKPK....",
                    "...KKrrrrrrKK...",
                    "..KccKrrGGrKccK.",
                    "..K.KrrrrrrK.K..",
                    "....KccKKccK....",
                    "...KDDK..KDDK...",
                    "...KKK....KKK..."
                ))
            } else {
                PixelArtSprites.sprite("comp_soldier_ok", listOf(
                    ".....KKKKKK.....",
                    "....KLLGGLLK....",
                    "....KKKKKKKK....",
                    "....KPPPPPPK....",
                    "....KPKEEKPK....",
                    "...KKLLLLLLKK...",
                    "..KCCKGGGGKCCK..",
                    ".KC.KLLLLLLK.CK.",
                    "....KLLKKLLK....",
                    "....KDDKKDDK....",
                    "....KKKKKKKK...."
                ))
            }

            CompanionId.FORGOTTEN_BEAR -> PixelArtSprites.sprite("comp_forgotten_bear", listOf(
                "..KKK......KKK..",
                ".KBBBK....KBBBK.",
                ".KBbBKKKKKKBbBK.",
                ".KBBBBBBBBBBBBK.",
                ".KBBKYKBBKYKBBK.",
                ".KBBBBPWWPBBBBK.",
                "..KKKKPKKPKKKK..",
                "..KKBKKKKKKBKK..",
                ".KBBKBBGGGBKBBK.",
                ".KBBKBBBBBBKBBK.",
                "..KK.KBBBBK.KK..",
                "....KBBKKBBK....",
                "....KKKKKKKK...."
            ))

            CompanionId.CLOCKWORK_CAT -> PixelArtSprites.sprite("comp_clockwork_cat", listOf(
                ".KKK........KKK.",
                ".KGGK......KGGK.",
                ".KGGKKKKKKKKGGK.",
                "..KGGGGGGGGGGK..",
                "..KGGKYKKYKGGK..",
                "..KGGGGPPGGGGK..",
                "...KKGGKKGGKK...",
                "..KCCKGGGGKCCK..",
                ".KCCKCCCCCCKCCK.",
                "..KK.KCCCCK.KKK.",
                "....KCCKKCCKGGK.",
                "....KKK..KKKKK.."
            ))

            CompanionId.PAPER_MOTH -> PixelArtSprites.sprite("comp_paper_moth", listOf(
                "....K......K....",
                "....WK....KW....",
                ".KKKKKWWWWKKKKK.",
                "KWWWWKYYYYKWWWWK",
                "KWWGGKWWWWKGGWWK",
                "KWWWWKWWWWKWWWWK",
                ".KWWWKWWWWKWWWK.",
                "..KKWKWWWWKWKK..",
                "...KWWKWWKWWK...",
                "....KK.KK.KK...."
            ))

            CompanionId.WHISPER_PUPPET -> PixelArtSprites.sprite("comp_whisper_puppet", listOf(
                ".....KKKKKK.....",
                "....KvvvvvvK....",
                "...KvPPPPPPvK...",
                "...KvPKEKEPvK...",
                "...KvPKKKKPvK...",
                "....KKPPPPKK....",
                "..KKDDDDDDDDKK..",
                ".KPKDDVVVVDDKPK.",
                "..K.KDDDDDDK.K..",
                "....KDDKKDDK....",
                "...KDDK..KDDK...",
                "...KKK....KKK..."
            ))

            CompanionId.RUSTY_BALLERINA -> if (broken) {
                PixelArtSprites.sprite("comp_ballerina_broken", listOf(
                    ".....KKKKKK.....",
                    "....KbbbbbbK....",
                    "....KPPPPPPK....",
                    "....KPKDDKPK....",
                    ".....KKPPKK.....",
                    "...KKccccccKK...",
                    "..KccKbbbbKccK..",
                    ".KccKKKKKKKKccK.",
                    "......KccK......",
                    ".....KccccK.....",
                    "......KKKK......"
                ))
            } else {
                PixelArtSprites.sprite("comp_ballerina_ok", listOf(
                    ".....KKKKKK.....",
                    "....KGGGGGGK....",
                    "....KPPPPPPK....",
                    "....KPKEEKPK....",
                    ".....KKPPKK.....",
                    "...KKWWWWWWKK...",
                    "..KPPKGGGGKPPK..",
                    ".KWWKKKKKKKKWWK.",
                    "......KCCK......",
                    ".....KCCCCK.....",
                    "......KKKK......"
                ))
            }
        }
    }

    fun betrayedBunny(): ImageBitmap = PixelArtSprites.sprite("enemy_betrayed_bunny", listOf(
        "..KKK......KKK..",
        "..KSSK....KSSK..",
        "..KSRK....KRSK..",
        "..KSSSKKKKSSSK..",
        ".KSSrSSSSSSrSSK.",
        ".KSSKEKSSKEKSSK.",
        ".KSSSWWWWWWWSSK.",
        ".KSSrKrKrKrKrSK.",
        "..KKKSSSSSSKKK..",
        "..KSSKrrrrKSSK..",
        ".KSSSKrrrrKSSSK.",
        "..KK.KSSSSK.KK..",
        "....KSSKKSSK....",
        "...KSSK..KSSK...",
        "...KKK....KKK..."
    ))

    fun mirrorCopy(): ImageBitmap = PixelArtSprites.sprite("enemy_mirror_copy", listOf(
        "....KKKKKKKK....",
        "...KCCCCCCCCK...",
        "..KCCKKCCCKCCK..",
        "..KCCKECCCECCK..",
        "..KCCCCCCCCCCK..",
        "...KCKKKKKKCK...",
        "..KKCCKLLKCCKK..",
        ".KCCKCCLLCCKCCK.",
        ".KCCKCCCCCCKCCK.",
        "..KK.KCCCCK.KK..",
        ".....KCKKCK.....",
        "....KccKKccK....",
        "....KKKKKKKK...."
    ))

    fun weepingToy(frozen: Boolean): ImageBitmap = if (frozen) {
        PixelArtSprites.sprite("enemy_weeping_frozen", listOf(
            "....KKKKKKKK....",
            "...KSSSSSSSSK...",
            "...KSPPPPPPSK...",
            "...KSPKKKKPSK...",
            "...KSPPPPPPSK...",
            "....KKSSSSKK....",
            "..KKSSSSSSSSKK..",
            ".KSSKSSSSSSKSSK.",
            "..KK.KSSSSK.KK..",
            "....KSSKKSSK....",
            "....KKKKKKKK...."
        ))
    } else {
        PixelArtSprites.sprite("enemy_weeping_awake", listOf(
            "....KKKKKKKK....",
            "...KPPPPPPPPK...",
            "...KPKEKKKEPK...",
            "...KPPWWWWPPK...",
            "...KPKKKKKKPK...",
            "....KKPPPPKK....",
            "..KKrrrrrrrrKK..",
            ".KPKrrPPPPrrKPK.",
            "..K.KrrrrrrK.K..",
            "....KPPKKPPK....",
            "...KPPK..KPPK...",
            "...KKK....KKK..."
        ))
    }

    fun nurseryBlockObstacle(): ImageBitmap = PixelArtSprites.sprite("obs_nursery_block", listOf(
        "KKKKKKKKKKKK",
        "KRRRRRRRRRRK",
        "KRGGGGGGGGRK",
        "KRGWWWWWWGRK",
        "KRGWKWWKWGRK",
        "KRGWKKKKWGRK",
        "KRGWKWWKWGRK",
        "KRGWWWWWWGRK",
        "KRGGGGGGGGRK",
        "KLLLLLLLLLLK",
        "KLLLLLLLLLLK",
        "KKKKKKKKKKKK"
    ))

    fun companionBellProp(): ImageBitmap = PixelArtSprites.sprite("pickup_companion_shrine", listOf(
        ".....KKKK.....",
        "....KGGGGK....",
        "...KGGWWGGK...",
        "...KGGGGGGK...",
        "..KGGGGGGGGK..",
        "..KGGGGGGGGK..",
        ".KGGGGGGGGGGK.",
        ".KKKKKKKKKKKK.",
        ".....KrrK.....",
        "......KK......"
    ))
}
