package com.example.engine

import androidx.annotation.DrawableRes
import com.example.R
import com.example.data.catalog.AmpliacionCatalog
import com.example.data.model.ArchiveEntryDef

/**
 * Resolves pre-generated offline AI narrative artwork stored in res/drawable/
 * and maintains visual consistency metadata across characters and eras.
 */
object AIImageArchiveSystem {

    @DrawableRes
    fun drawableForArchive(entry: ArchiveEntryDef): Int = drawableByName(entry.drawableResName)

    @DrawableRes
    fun drawableByName(name: String): Int = when (name) {
        "img_archive_first_shop" -> R.drawable.img_archive_first_shop
        "img_archive_best_friend" -> R.drawable.img_archive_best_friend
        "img_archive_bear_portrait" -> R.drawable.img_archive_bear_portrait
        "img_archive_last_night" -> R.drawable.img_archive_last_night
        "img_archive_forbidden_blueprint" -> R.drawable.img_archive_forbidden_blueprint
        "img_lore_workshop" -> R.drawable.img_lore_workshop
        "img_lore_manufacturer" -> R.drawable.img_lore_manufacturer
        "img_patch_portrait" -> R.drawable.img_patch_portrait
        "img_achievement_book" -> R.drawable.img_achievement_book
        else -> R.drawable.img_menu_toyshop
    }

    fun visualConsistencySummary(entryId: String): String {
        val entry = AmpliacionCatalog.archiveById(entryId) ?: return "Archivo offline listo."
        val tags = if (entry.visualCharacterTags.isEmpty()) "estética_wonderland" else entry.visualCharacterTags.joinToString(" • ")
        return "Recurso local en caché • Referencia visual: $tags"
    }
}
