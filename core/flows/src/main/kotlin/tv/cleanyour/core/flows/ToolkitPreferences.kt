package tv.cleanyour.core.flows

import tv.cleanyour.core.model.DeviceFacts
import tv.cleanyour.core.model.TvId
import tv.cleanyour.core.store.DocumentStore

/** Explicit choices made on the phone. These are an exportable draft, never executable ADB text. */
enum class ToolkitLauncher { KEEP_STOCK, FLAUNCHER, PROJECTIVY }
enum class ToolkitYouTube { KEEP_STOCK, ALTERNATIVE }
enum class ToolkitMedia { NONE, PLEX, WHOLPHIN, BOTH }
enum class ToolkitWallpaper { KEEP, AURORA, GRAPHITE }

data class ToolkitPreferences(
    val launcher: ToolkitLauncher = ToolkitLauncher.KEEP_STOCK,
    val youtube: ToolkitYouTube = ToolkitYouTube.KEEP_STOCK,
    val media: ToolkitMedia = ToolkitMedia.NONE,
    val wallpaper: ToolkitWallpaper = ToolkitWallpaper.KEEP,
) {
    /**
     * A plan for the human to review, not a package download instruction. External APKs must be
     * independently identified, pinned by hash + signer, checked for ABI and explicitly approved.
     */
    fun export(facts: DeviceFacts): String = buildString {
        fun safe(value: String) = value.replace(Regex("[^A-Za-z0-9 _+./-]"), " ").trim().take(100)
        appendLine("TV_TOOLKIT_PREFERENCES_V1")
        appendLine("BRAND=${safe(facts.brand)}")
        appendLine("MODEL=${safe(facts.model)}")
        appendLine("DEVICE=${safe(facts.device)}")
        appendLine("LAUNCHER=${launcher.name}")
        appendLine("YOUTUBE=${youtube.name}")
        appendLine("MEDIA=${media.name}")
        appendLine("WALLPAPER=${wallpaper.name}")
        appendLine("STATUS=PREVIEW_ONLY")
        appendLine("END_TV_TOOLKIT_PREFERENCES")
    }
}

/** A per-TV local preference; never interprets a selected launcher as an ADB command. */
class ToolkitPreferencesStore(private val documents: DocumentStore) {
    private fun path(tvId: TvId) = "tvs/${tvId.value}/toolkit-launcher.txt"

    suspend fun loadLauncher(tvId: TvId): ToolkitLauncher? {
        val content = documents.read(path(tvId)) { it.decodeToString() } ?: return null
        if (!content.startsWith("TV_TOOLKIT_LAUNCHER_V1\n")) return null
        val value = content.lineSequence().firstOrNull { it.startsWith("LAUNCHER=") }?.removePrefix("LAUNCHER=")
        return ToolkitLauncher.entries.firstOrNull { it.name == value }
    }

    suspend fun saveLauncher(tvId: TvId, launcher: ToolkitLauncher) {
        documents.write(path(tvId), "TV_TOOLKIT_LAUNCHER_V1\nLAUNCHER=${launcher.name}\n".encodeToByteArray())
    }
}
