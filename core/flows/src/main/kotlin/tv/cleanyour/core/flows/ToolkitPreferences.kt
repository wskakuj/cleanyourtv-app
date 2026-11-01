package tv.cleanyour.core.flows

import tv.cleanyour.core.model.DeviceFacts

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
