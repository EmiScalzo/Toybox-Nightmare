package com.example.engine

import com.example.data.catalog.AmpliacionCatalog
import com.example.data.model.ArchiveCategory
import com.example.data.model.ArchiveEntryDef
import com.example.data.model.PendingArchiveDiscovery

object WonderlandArchiveSystem {

    fun entriesForCategory(category: ArchiveCategory?): List<ArchiveEntryDef> {
        return if (category == null) AmpliacionCatalog.archiveEntries
        else AmpliacionCatalog.archiveEntries.filter { it.category == category }
    }

    fun isUnlocked(entry: ArchiveEntryDef, discoveredKeys: Set<String>): Boolean {
        return "archive:${entry.id}" in discoveredKeys
    }

    fun isExtraFragmentUnlocked(entry: ArchiveEntryDef, discoveredKeys: Set<String>): Boolean {
        if (!isUnlocked(entry, discoveredKeys)) return false
        val req = entry.requiredAdditionalKey ?: return true
        return req in discoveredKeys
    }

    fun countUnlocked(discoveredKeys: Set<String>): Int {
        return AmpliacionCatalog.archiveEntries.count { isUnlocked(it, discoveredKeys) }
    }

    fun unlockArchiveEntry(
        state: GameRunState,
        archiveId: String,
        bonusText: String? = null,
        onDiscovery: (String, String) -> Unit,
        onAchievement: (String) -> Unit
    ): GameRunState {
        val entry = AmpliacionCatalog.archiveById(archiveId) ?: return state
        val already = archiveId in state.unlockedArchiveIdsThisRun
        onDiscovery("archive", archiveId)
        val nextIds = state.unlockedArchiveIdsThisRun + archiveId
        if (entry.category == ArchiveCategory.MAIN_STORY || archiveId == "arch_manufacturer_file") {
            onAchievement("ach_forbidden_doc")
        }
        return state.copy(
            unlockedArchiveIdsThisRun = nextIds,
            pendingArchiveDiscovery = PendingArchiveDiscovery(
                entry = entry,
                isNewlyUnlocked = !already,
                bonusText = bonusText
            )
        )
    }
}
