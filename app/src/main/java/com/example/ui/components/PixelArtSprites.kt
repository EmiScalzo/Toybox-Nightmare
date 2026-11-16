package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.data.model.BackpackItemKind
import com.example.data.model.BackpackTier
import com.example.data.model.BodyZone
import com.example.data.model.CharacterId
import com.example.data.model.CompanionId
import com.example.data.model.ConsumableType
import com.example.data.model.EnemyType
import com.example.data.model.InteractableType
import com.example.data.model.LoreProp
import com.example.data.model.RepairMaterial
import com.example.data.model.SecretClueType

/**
 * Handcrafted dark-outlined pixel-art sprite matrices matching the reference sprite sheet aesthetic:
 * bold #181014 outlines, shaded vintage fabrics, glowing horror eyes, stitches, bow ties, and wind-up keys.
 * Cached once as crisp ImageBitmaps for FilterQuality.None rendering in both Combat Canvas and Album Cards.
 */
object PixelArtSprites {

    private val palette: Map<Char, Int> = mapOf(
        '.' to 0x00000000, // Transparent
        'K' to 0xFF160F13.toInt(), // Dark Pixel Outline
        'W' to 0xFFF0E6D2.toInt(), // Cream / Ivory Highlight
        'P' to 0xFFD2C0A4.toInt(), // Pale Porcelain / Bone
        'S' to 0xFFA6907A.toInt(), // Shaded Cloth / Stone
        'B' to 0xFF8C6244.toInt(), // Teddy Brown Mid
        'b' to 0xFF5C3B28.toInt(), // Teddy Brown Dark
        'T' to 0xFFC2956B.toInt(), // Tan Burlap
        'R' to 0xFFB82932.toInt(), // Crimson Cloth / Blood
        'r' to 0xFF75161D.toInt(), // Dark Crimson
        'G' to 0xFFE5A93B.toInt(), // Brass / Gold
        'g' to 0xFF9E7022.toInt(), // Tarnished Brass
        'E' to 0xFFFF3842.toInt(), // Glowing Red Eye
        'Y' to 0xFFFFD15C.toInt(), // Glowing Amber/Yellow Eye
        'L' to 0xFF4D6A80.toInt(), // Slate Blue Uniform
        'l' to 0xFF2D404F.toInt(), // Dark Slate Blue
        'M' to 0xFF63805B.toInt(), // Moss / Stitch Green
        'V' to 0xFF7D5C78.toInt(), // Haunted Violet Plush
        'v' to 0xFF4B3448.toInt(), // Dark Violet Shadow
        'H' to 0xFFE0B866.toInt(), // Blonde Hair / Straw
        'C' to 0xFF7D8C96.toInt(), // Cold Steel / Tin
        'c' to 0xFF4B565E.toInt(), // Dark Steel
        'D' to 0xFF2B2026.toInt()  // Deep Shadow / Void Body
    )

    private val cache = mutableMapOf<String, ImageBitmap>()

    internal fun sprite(key: String, rows: List<String>): ImageBitmap {
        return cache.getOrPut(key) { buildBitmap(rows) }
    }

    private fun buildBitmap(rows: List<String>): ImageBitmap {
        val h = rows.size
        val w = rows.maxOf { it.length }
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            val row = rows[y]
            for (x in 0 until row.length) {
                val ch = row[x]
                pixels[y * w + x] = palette[ch] ?: 0x00000000
            }
        }
        val bmp = Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
        return bmp.asImageBitmap()
    }

    fun forEnemy(type: EnemyType, disguised: Boolean = false): ImageBitmap {
        return when (type) {
            EnemyType.PLUSH_BEAR -> sprite("bear", listOf(
                "..KKK......KKK..",
                ".KBBBK....KBBBK.",
                ".KBbBKKKKKKBbBK.",
                ".KBBBBBBBBBBBBK.",
                "KBBWWBBBBBBWWBBK",
                "KBBKEKBBBBKEKBBK",
                "KBBBBBPWWPBBBBBK",
                ".KBBBBPKKPBBBBK.",
                "..KKKKRRRRKKKK..",
                ".KBBbKRRRRKbBBK.",
                "KBBBBKBBBBKBBBBK",
                "KBBbKBBWWBBKbBBK",
                ".KK.KBBrrBBK.KK.",
                "....KBBBBBBK....",
                "...KBBbKKbBBK...",
                "...KKKK..KKKK..."
            ))

            EnemyType.TIN_SOLDIER -> sprite("soldier", listOf(
                ".....KKKKKK.....",
                "....KDDDDDDK....",
                "....KDDGGDDK....",
                "....KDDDDDDK....",
                "....KKKKKKKK....",
                "....KPPPPPPK....",
                "....KPKKKKPK....",
                "....KPKEEKPK....",
                "...KKRRRRRRKK...",
                "..KLLRRGGRRLLK..",
                ".KCLLRRRRRRLLCK.",
                ".KCLLRRGGRRLLCK.",
                "..K.KLLLLLLK.K..",
                "....KLLKKLLK....",
                "....KDDKKDDK....",
                "....KKKKKKKK...."
            ))

            EnemyType.CREEPY_DOLL -> if (disguised) {
                sprite("doll_still", listOf(
                    "...KKK....KKK...",
                    "..KHHHK..KHHHK..",
                    ".KHKKKKKKKHHK...",
                    ".KHKKPPPPPKKHK..",
                    ".KHKPPKPPKPPKHK.",
                    ".KHKPPPPPPPPKHK.",
                    "..KKKPRRRPKKK...",
                    "....KWRRRWK.....",
                    "...KRRRRRRRK....",
                    "..KPRRRRRRRPK...",
                    "..KPKRRRRRKPK...",
                    "...KKRRRRRKK....",
                    "....KPPKKPPK....",
                    "....KPPKKPPK....",
                    "....KKK..KKK...."
                ))
            } else {
                sprite("doll_awake", listOf(
                    "...KKK....KKK...",
                    "..KHHHK..KHHHK..",
                    ".KHKRKKKKKKRKHK.",
                    ".KHKPPPPPPPPKHK.",
                    ".KHKPEKPPKEPKHK.",
                    ".KHKPPPPPPPPKHK.",
                    "..KKKPKKKPKKK...",
                    "...KKWRRRWKK....",
                    "..KPKRRRRRRKPK..",
                    ".KPKRRRRRRRRKPK.",
                    "..K.KRRRRRRK.K..",
                    "....KRRRRRRK....",
                    "....KPPKKPPK....",
                    "...KPPK..KPPK...",
                    "...KKK....KKK..."
                ))
            }

            EnemyType.JACK_IN_BOX -> sprite("jack", listOf(
                "..KKK......KKK..",
                ".KRRRK....KLLLK.",
                "..KRRKKKKKKLLK..",
                "...KWWWWWWWWK...",
                "...KWKEKWKEWK...",
                "...KWWWRRWWWK...",
                "....KWKKKKWK....",
                ".....KKCCKK.....",
                "......KCCK......",
                "..KKKKKKKKKKKK..",
                ".KLLLLRRRRLLLLK.",
                ".KLLRRGGGGLLRRK.",
                ".KLLLLGGGGLLLLK.",
                ".KRRRRLLLLRRRRK.",
                ".KLLLLRRRRLLLLK.",
                "..KKKKKKKKKKKK.."
            ))

            EnemyType.CLOCKWORK_SPIDER -> sprite("spider", listOf(
                "......KGGK......",
                ".......KK.......",
                "K...KKKKKKKK...K",
                "KK.KPPSSSSPPK.KK",
                ".KKPSEKPPEKSPKK.",
                ".KKPSSSSSSSSPKK.",
                "KK.KSSKKKKSSK.KK",
                "K..KCCCCCCCCK..K",
                "..KKcCCCCCCcKK..",
                ".KK.KCCKKCCK.KK.",
                "KK..KCK..KCK..KK",
                "K...KK....KK...K"
            ))

            EnemyType.MIRROR_DOLL -> sprite("mirror", listOf(
                ".....KKKKKK.....",
                "....KDDDDDDK....",
                "...KDDvvvvDDK...",
                "...KDVWWWWVDK...",
                "...KDVWKKWVDK...",
                "...KDDDDDDDDK...",
                "....KKvvvvKK....",
                "...KDDvvvvDDK...",
                "..KDvDDDDDDvDK..",
                "..KDKDDDDDDKDK..",
                "...K.KDDDDK.K...",
                "....KDDKKDDK....",
                "...KDDK..KDDK...",
                "...KKK....KKK..."
            ))

            EnemyType.THIEF_GREMLIN -> sprite("gremlin", listOf(
                ".KKK........KKK.",
                ".KVVKK....KKVVK.",
                "..KVvKKKKKKvVK..",
                "...KVVVVVVVVK...",
                "...KVKYKKYVKV...",
                "...KVVKKKKVVK...",
                "..KKVvVVVVvVKK..",
                ".KVVKVVVVVVKBK..",
                "KVVVKVVVVVVKBBK.",
                ".KK.KVVKKVVKBBK.",
                "....KVVK.KVKKKK.",
                "...KVvK...KvVK..",
                "...KKK.....KKK.."
            ))

            EnemyType.MIMIC_CHEST -> if (disguised) {
                sprite("chest_still", listOf(
                    "................",
                    "..KKKKKKKKKKKK..",
                    ".KBBBBBBBBBBBBK.",
                    ".KBGGGGGGGGGGbK.",
                    ".KBBBBBBBBBBBBK.",
                    ".KGGGGKKKKGGGGK.",
                    ".KBBBBKGGKBBBBK.",
                    ".KBBbBKGGKBbBBK.",
                    ".KBBBBBBBBBBBBK.",
                    ".KGGGGGGGGGGGGK.",
                    "..KKKKKKKKKKKK.."
                ))
            } else {
                sprite("chest_awake", listOf(
                    "..KKKKKKKKKKKK..",
                    ".KBBBBBBBBBBBBK.",
                    ".KBBEEBBBBEEBBK.",
                    ".KWWWWWWWWWWWWK.",
                    "KrrrrrrrrrrrrrrK",
                    "KrWKrWKrWKrWKrWK",
                    "KrrrrrrrrrrrrrrK",
                    ".KWWWWWWWWWWWWK.",
                    ".KBBBBKGGKBBBBK.",
                    ".KGGGGGGGGGGGGK.",
                    "..KKKKKKKKKKKK.."
                ))
            }

            EnemyType.PORCELAIN_CLOWN -> sprite("porcelain_clown", listOf(
                "..KKK......KKK..",
                ".KRRRK....KLLLK.",
                "..KRRKKKKKKLLK..",
                "...KPPPPPPPPK...",
                "..KPPKEKKKEPPK..",
                "..KPPRRRRRRPPK..",
                "...KPPKKKKPPK...",
                "..KKWWGGGGWWKK..",
                ".KLLKRRRRRRKLLK.",
                ".KLLKLLGGLLKLLK.",
                "..KK.KRRRRK.KK..",
                "....KLLKKLLK....",
                "...KDDK..KDDK...",
                "...KKK....KKK..."
            ))

            EnemyType.WINDUP_CHOMPER -> sprite("windup_chomper", listOf(
                ".....KGGGGK.....",
                "......KGGK......",
                "...KKKKKKKKKK...",
                "..KRRRRRRRRRRK..",
                ".KRRRRRRRRRRRRK.",
                ".KWWWWWWWWWWWWK.",
                ".KWKWKWKWKWKWKK.",
                ".KDDDDDDDDDDDDK.",
                ".KWKWKWKWKWKWKK.",
                ".KWWWWWWWWWWWWK.",
                "..KRRRRRRRRRRK..",
                "...KKKKKKKKKK..."
            ))

            EnemyType.NEEDLE_MARIONETTE -> sprite("needle_marionette", listOf(
                "....W..KK..W....",
                "....WKPPPPKW....",
                "...KKPKKKKPKK...",
                "...KPPKEEKPPK...",
                "....KPPPPPPK....",
                "..KKVCCCCCCVKK..",
                ".KCCKVVVVVVKCCK.",
                "CK..KVVRRVVK..KC",
                "....KVVVVVVK....",
                "....KVVKKVVK....",
                "...KPPK..KPPK...",
                "...KKK....KKK..."
            ))

            EnemyType.SHADOW_PUPPET -> sprite("shadow_puppet", listOf(
                "...KKK....KKK...",
                "..KDDDK..KDDDK..",
                "..KDDKKKKKKDDK..",
                "..KDDDDDDDDDDK..",
                "..KDDKEKKKEKDK..",
                "..KDDDDDDDDDDK..",
                "...KKDDDDDDKK...",
                "..KDDDDVVDDDDK..",
                ".KDDKDDDDDDKDDK.",
                "..KK.KDDDDK.KK..",
                "....KDDKKDDK....",
                "...KDDK..KDDK..."
            ))

            EnemyType.RUSTY_TIN_KNIGHT -> sprite("rusty_tin_knight", listOf(
                ".....KKKKKK.....",
                "....KCCCCCCK....",
                "...KCCcKKcCCK...",
                "...KCCKEEKCCK...",
                "...KCCCCCCCCK...",
                "..KKCCbbbbCCKK..",
                ".KCCKCCCCCCKCCK.",
                ".KCbKCCGGCCKbCK.",
                ".KCCKCCCCCCKCCK.",
                "..KK.KCCCCK.KK..",
                "....KccKKccK....",
                "....KKKKKKKK...."
            ))

            EnemyType.RAG_BOMBER -> sprite("rag_bomber", listOf(
                "......KKYY......",
                ".....KGG........",
                "...KKKKKKKKKK...",
                "..KTTTTTTTTTTK..",
                ".KTKEKTTTTKEKTK.",
                ".KTTTTKKKKTTTTK.",
                "..KKRRRRRRRRKK..",
                ".KTTKRRDDRRKTTK.",
                ".KTTKRRDDRRKTTK.",
                "..KK.KTTTTK.KK..",
                "....KTTKKTTK....",
                "....KKKKKKKK...."
            ))

            EnemyType.BETRAYED_BUNNY -> PixelArtCompanionSprites.betrayedBunny()
            EnemyType.MIRROR_COPY -> PixelArtCompanionSprites.mirrorCopy()
            EnemyType.WEEPING_TOY -> PixelArtCompanionSprites.weepingToy(frozen = disguised)
            EnemyType.TIN_CAPTAIN -> PixelArtExpandedSprites.tinCaptain()
            EnemyType.BODYGUARD_BEAR -> PixelArtExpandedSprites.bodyguardBear()
            EnemyType.CLOCKWORK_COURIER -> PixelArtExpandedSprites.clockworkCourier()
            EnemyType.STITCH_MECHANIC -> PixelArtExpandedSprites.stitchMechanic()

            EnemyType.BOSS_VENDEDOR -> sprite("boss_vendedor", listOf(
                "......KKKKKKKK......",
                ".....KDDDDDDDDK.....",
                ".....KDDGGGGDDK.....",
                "...KKKKKKKKKKKKKK...",
                "....KBBBBBBBBBBK....",
                "...KBBKEKBBKEKBBK...",
                "...KBBBBWWWWBBBBK...",
                "...KBBBBKKKKBBBBK...",
                "....KKKKRRRRKKKK....",
                "..KKLLbKRRRRKbLLKK..",
                ".KLLLLLKGGGGKLLLLLK.",
                "KLLbLLKBBBBBBKLLbLLK",
                "KLLK.KBBBBBBBBK.KLLK",
                ".KK..KBBbKKbBBK..KK.",
                "....KBBBBKKBBBBK....",
                "....KDDDK..KDDDK....",
                "....KKKK....KKKK...."
            ))

            EnemyType.BOSS_BOTONERO -> sprite("boss_botonero", listOf(
                ".....KKKKKKKKKK.....",
                "....KGGGGGGGGGGK....",
                "...KGGKGGGGGGKGGK...",
                "...KGGKGGGGGGKGGK...",
                "...KGGGGGGGGGGGGK...",
                "....KKGGGGGGGGKK....",
                "..KKLLKRRRRRRKLLKK..",
                ".KLLLLKGGGGGGKLLLLK.",
                "KLLK.KLLLLLLLLK.KLLK",
                ".KK..KLLGKKGLLK..KK.",
                "....KLLLLKKLLLLK....",
                "...KDDDDK..KDDDDK...",
                "...KKKKK....KKKKK..."
            ))

            EnemyType.BOSS_COSTURERA -> sprite("boss_costurera", listOf(
                "......KKKKKKKK......",
                ".....KvvvvvvvvK.....",
                "....KvPPPPPPPPvK....",
                "....KvPKEKKKEPvK....",
                "....KvPPPPPPPPvK....",
                ".....KKPRRRPKK......",
                "...KKrrrrrrrrrrKK...",
                "..KrrrrRRRRRRrrrrK..",
                ".KrrKrrrrrrrrrrKCCK.",
                ".KrK.KrrrrrrrrK.CCK.",
                ".....KrrRRRRrrK.CCK.",
                "....KrrrrrrrrrrKCK..",
                "....KrrrrrrrrrrKK...",
                "...KrrrrKKKKrrrrK...",
                "...KKKK......KKKK..."
            ))

            EnemyType.BOSS_TITIRITERO -> sprite("boss_titiritero", listOf(
                "..KKKKKKKKKKKKKKKK..",
                ".KBBBBBBBBBBBBBBBBK.",
                "..KKKKKvvvvvvKKKKK..",
                ".....KvPPPPPPvK.....",
                "....KvPKEKKKEPvK....",
                "....KvPPPPPPPPvK....",
                "..KKDDDDGGGGDDDDKK..",
                ".KDDKDDvvvvvvDDKDDK.",
                ".K.W.KDDDDDDDDK.W.K.",
                ".W...KDDDKKDDDK...W.",
                "....KDDDK..KDDDK....",
                "....KKKK....KKKK...."
            ))

            EnemyType.BOSS_NINERA -> sprite("boss_ninera", listOf(
                ".....KKKKKKKKKK.....",
                "....KWWWWWWWWWWK....",
                "....KWPPPPPPPPWK....",
                "....KWPKEKKKEPWK....",
                "....KWPPPPPPPPWK....",
                ".....KKPKKKKPKK.....",
                "...KKLLLLLLLLLLKK...",
                "..KLLLLWWWWWWLLLLK..",
                ".KLLKLLLLLLLLLLKLLK.",
                ".KK..KLLLLLLLLK..KK.",
                "....KLLLLLLLLLLK....",
                "...KLLLLLKKLLLLLK...",
                "...KKKKK....KKKKK..."
            ))

            EnemyType.BOSS_RELOJERO -> sprite("boss_relojero", listOf(
                ".....KKKKKKKKKK.....",
                "....KGGGGGGGGGGK....",
                "...KGGWWWWWWWWGGK...",
                "...KGWKEKWWEKEWGK...",
                "...KGGWWKKKKWWGGK...",
                "...KKGGGGGGGGGGKK...",
                "..KCCKGGGGGGGGKCCK..",
                ".KCCcKCCCCCCCCKcCCK.",
                ".KCC.KCCCCCCCCK.CCK.",
                ".....KCCCCKKCCK.....",
                "....KCCCCK..KCCCCK..",
                "....KKKKK....KKKKK.."
            ))

            EnemyType.BOSS_MAESTRO -> sprite("boss_maestro", listOf(
                "....KKK......KKK....",
                "...KRRRK....KGGGK...",
                "....KRRKKKKKKGGK....",
                "....KWWWWWWWWWWK....",
                "....KWKEKWWKEKWK....",
                "....KWWWRRRRWWWK....",
                "...KKGGGGGGGGGGKK...",
                "..KRRRRLLLLLLRRRRK..",
                ".KRRKRRRRRRRRRRKGGK.",
                ".KK..KRRRRRRRRK..KK.",
                "....KRRRRKKRRRRK....",
                "...KDDDK....KDDDK...",
                "...KKKK......KKKK..."
            ))

            EnemyType.BOSS_BUHONERO -> sprite("boss_buhonero", listOf(
                "....KKKKKKKKKKKK....",
                "...KDDDDDDDDDDDDK...",
                "..KDDVVVVVVVVVVDDK..",
                "..KDVKYKKKKKKYKVDDK.",
                "..KDDDDDDDDDDDDDDK..",
                "..KKRRRRRRRRRRRRKK..",
                ".KRRKDDDDGGDDDDKRRK.",
                "KRRK.KDDDDDDDDK.KRRK",
                ".KK..KDDDDDDDDK..KK.",
                "....KDDDK..KDDDK....",
                "...KDDDK....KDDDK...",
                "...KKKK......KKKK..."
            ))

            EnemyType.BOSS_COLECCIONISTA -> sprite("boss_coleccionista", listOf(
                "....KKKKKKKKKKKK....",
                "...KCCGGGGGGGGCCK...",
                "...KCCKEKCCKEKCCK...",
                "...KCCCCCCCCCCCCK...",
                "..KKBBBBBBBBBBBBKK..",
                ".KBBKCCGGGGGGCCKBBK.",
                "KBBK.KVVRRRRVVK.KBBK",
                ".KK..KCCCCCCCCK..KK.",
                "....KCCCCKKCCCCK....",
                "...KBBBK....KCCCK...",
                "...KKKK......KKKK..."
            ))

            EnemyType.BOSS_MADRE -> sprite("boss_madre", listOf(
                "....KK..KKKK..KK....",
                "...KGGKKGGGGKKGGK...",
                "....KDDDDDDDDDDK....",
                "...KDDWWWWWWWWDDK...",
                "...KDWKEKDDKEKWDK...",
                "...KDDWWWWWWWWDDK...",
                "..KKKrrrrrrrrrrKKK..",
                ".KrrrrRRRRRRRRrrrrK.",
                "KrrKrrrrrrrrrrrrKrrK",
                "KK..KrrrrrrrrrrK..KK",
                "...KrrrrrrrrrrrrK...",
                "..KrrrrrKKKKrrrrrK..",
                "..KKKKK......KKKKK.."
            ))
        }
    }

    fun forCharacter(id: CharacterId): ImageBitmap {
        return when (id) {
            CharacterId.PATCH -> sprite("char_patch", listOf(
                "....KKKKKKKK....",
                "...KTTTTTTTTK...",
                "..KTTKKTTTGTTK..",
                "..KTTKKTTTGGTK..",
                "..KTTTTTTTTTTK..",
                "...KTKKKKKKTK...",
                "..KKTTKGGKTTKK..",
                ".KTTKTTGGTTKTTK.",
                ".KTTKTTTTTTKTTK.",
                ".KTT.KTTTTK..KK.",
                ".KK..KTKKTK.....",
                ".....KTKKTK.....",
                "....KbK..KbK....",
                "....KKK..KKK...."
            ))
            CharacterId.TINNY -> sprite("char_tinny", listOf(
                ".......KK.......",
                "......KEEK......",
                ".......KK.......",
                "...KKKKKKKKKK...",
                "..KCCCCCCCCCCK..",
                "..KCCYCCCCYCCK..",
                "..KCCcKKKKcCCK..",
                "..KKKKKKKKKKKK..",
                ".KCCKCCGGCCKCCK.",
                ".KCCKCCCCCCKCCK.",
                "..KK.KCCCCK.KK..",
                ".....KCKKCK.....",
                "....KccKKccK....",
                "....KKKKKKKK...."
            ))
            CharacterId.DOLLY -> sprite("char_dolly", listOf(
                "...KKK....KKK...",
                "..KHHHK..KHHHK..",
                "..KHHKKKKKKHHK..",
                "..KHPPPPPPPPHK..",
                "..KHPKEKPKEPHK..",
                "..KHPPPPPPPPHK..",
                "...KKPRRRRPKK...",
                "..KRRRRRRRRRRK..",
                ".KPKRRRRRRRRKPK.",
                "..K.KRRRRRRK.K..",
                "....KPPKKPPK....",
                "....KKK..KKK...."
            ))
            CharacterId.HOPS -> sprite("char_hops", listOf(
                "..KKK......KKK..",
                "..KSSK....KSSK..",
                "..KSSK....KSSK..",
                "..KSSKKKKKKSSK..",
                "..KSSSSSSSSSSK..",
                "..KSSKEKKSKEK...",
                "..KSSSSRRSSSSK..",
                "...KKMMMMMMKK...",
                "..KSSKMMMMKSSK..",
                "..KK.KMMMMK.KK..",
                ".....KSKKSK.....",
                "....KSSKKSSK....",
                "....KKKKKKKK...."
            ))
            CharacterId.JACK -> sprite("char_jack", listOf(
                ".KKK........KKK.",
                ".KRRKK....KKGGK.",
                "..KRRKKKKKKGGK..",
                "...KWWWWWWWWK...",
                "...KWKEKWKEWK...",
                "...KWWWRRWWWK...",
                "..KKRRRRRRRRKK..",
                ".KGGKRRRRRRKGGK.",
                "..KK.KLLLLK.KK..",
                ".....KLKKLK.....",
                "....KLLKKLLK....",
                "....KKKKKKKK...."
            ))
            CharacterId.STITCH -> sprite("char_stitch", listOf(
                "....KKKKKKKK....",
                "...KMMMMMMMMK...",
                "..KMMKKMMKKEK...",
                "..KMMMMMMMMMMK..",
                "...KMKKKKKKMK...",
                "..KKMMKRRKMMKK..",
                ".KMMKMMRRMMKMMK.",
                "..KK.KMMMMK.KK..",
                ".....KMKKMK.....",
                "....KMMKKMMK....",
                "....KKKKKKKK...."
            ))
        }
    }

    fun crateObstacle(): ImageBitmap = sprite("obs_crate", listOf(
        "KKKKKKKKKKKK",
        "KBBBBBBBBBBK",
        "KBbBBBBBBbBK",
        "KBBbBBBBbBBK",
        "KBBBbBBbBBBK",
        "KBBBBbbBBBBK",
        "KBBBBbbBBBBK",
        "KBBBbBBbBBBK",
        "KBBbBBBBbBBK",
        "KBbBBBBBBbBK",
        "KBBBBBBBBBBK",
        "KKKKKKKKKKKK"
    ))

    fun pickupButton(gold: Boolean): ImageBitmap = sprite(if (gold) "btn_gold" else "btn_brass", if (gold) listOf(
        "..KKKKKK..",
        ".KGGGGGGK.",
        "KGWWGGWWGK",
        "KGGKGGKGGK",
        "KGGGGGGGGK",
        "KGGKGGKGGK",
        "KGggGGggGK",
        ".KGGGGGGK.",
        "..KKKKKK.."
    ) else listOf(
        "..KKKKKK..",
        ".KggggggK.",
        "KgGGggGGgK",
        "KggKggKggK",
        "KggggggggK",
        "KggKggKggK",
        "KgbbggbbgK",
        ".KggggggK.",
        "..KKKKKK.."
    ))

    fun pickupHeart(): ImageBitmap = sprite("pickup_heart", listOf(
        ".KKK..KKK.",
        "KRRRKRRRKK",
        "KRWRRRRRRK",
        "KRRRRRRRRK",
        ".KRRRRRRK.",
        "..KRRRRK..",
        "...KRRK...",
        "....KK...."
    ))

    fun forPassiveItem(itemId: String): ImageBitmap = PixelArtObjectSprites.forPassiveItem(itemId)
    fun forConsumable(type: ConsumableType): ImageBitmap = PixelArtObjectSprites.forConsumable(type)
    fun forBodyPart(partId: String, zone: BodyZone): ImageBitmap = PixelArtObjectSprites.forBodyPart(partId, zone)
    fun vitrineObstacle(): ImageBitmap = PixelArtObjectSprites.vitrineObstacle()
    fun pillarObstacle(): ImageBitmap = PixelArtObjectSprites.pillarObstacle()
    fun plushPileObstacle(): ImageBitmap = PixelArtObjectSprites.plushPileObstacle()
    fun sewingTableProp(): ImageBitmap = PixelArtObjectSprites.sewingTableProp()
    fun threadSpoolsProp(): ImageBitmap = PixelArtObjectSprites.threadSpoolsProp()
    fun mannequinProp(): ImageBitmap = PixelArtObjectSprites.mannequinProp()
    fun scissorsProp(): ImageBitmap = PixelArtObjectSprites.scissorsProp()
    fun forCompanion(id: CompanionId, broken: Boolean = false): ImageBitmap =
        PixelArtCompanionSprites.forCompanion(id, broken)
    fun nurseryBlockObstacle(): ImageBitmap = PixelArtCompanionSprites.nurseryBlockObstacle()
    fun companionBellProp(): ImageBitmap = PixelArtCompanionSprites.companionBellProp()
    fun manufacturerNpc(): ImageBitmap = PixelArtExpandedSprites.manufacturerNpc()
    fun forInteractable(type: InteractableType, active: Boolean = false, destroyed: Boolean = false): ImageBitmap =
        PixelArtExpandedSprites.forInteractable(type, active, destroyed)
    fun forSecretClue(type: SecretClueType): ImageBitmap = PixelArtExpandedSprites.forSecretClue(type)
    fun forMaterial(mat: RepairMaterial): ImageBitmap = PixelArtExpandedSprites.forMaterial(mat)
    fun forObjectiveProp(kind: String): ImageBitmap = PixelArtExpandedSprites.forObjectiveProp(kind)
    fun forBackpackTier(tier: BackpackTier): ImageBitmap = PixelArtAmpliacionSprites.forBackpackTier(tier)
    fun forBackpackItem(kind: BackpackItemKind): ImageBitmap = PixelArtAmpliacionSprites.forBackpackItem(kind)
    fun forLoreProp(prop: LoreProp): ImageBitmap = PixelArtAmpliacionSprites.forLoreProp(prop)
}
