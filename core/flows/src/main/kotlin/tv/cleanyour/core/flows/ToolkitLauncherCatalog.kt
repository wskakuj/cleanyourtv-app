package tv.cleanyour.core.flows

import tv.cleanyour.core.model.ApkInfo
import tv.cleanyour.core.model.ArtifactRef
import tv.cleanyour.core.model.DeviceFacts
import tv.cleanyour.core.model.EnabledState
import tv.cleanyour.core.model.Module
import tv.cleanyour.core.model.ModuleVersion
import tv.cleanyour.core.model.PackageName
import tv.cleanyour.core.model.PackageState
import tv.cleanyour.core.model.Sha256
import tv.cleanyour.core.model.Tier

/**
 * Exact, immutable official release pins for the user's optional launchers. These APKs were
 * downloaded from the publisher's release pages and independently checked for size, SHA-256
 * and signing certificate. No URL, signer or hash is provided by the TV or by an AI response.
 */
data class ToolkitLauncherPin(
    val choice: ToolkitLauncher,
    val title: String,
    val pkg: PackageName,
    val release: String,
    val published: String,
    val minSdk: Int,
    val artifact: ArtifactRef,
    val abis: Set<String> = setOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"),
) {
    /** Refuse first if compatibility or an existing install cannot be checked safely. */
    fun refusal(facts: DeviceFacts, existing: PackageState?): String? = when {
        facts.installRefusal -> "This TV refuses app installation."
        facts.sdk < minSdk -> "$title needs Android SDK $minSdk or newer."
        facts.abis.isEmpty() || facts.abis.none { it in abis } -> "The TV processor is not supported or could not be read."
        existing?.system == true -> "An existing system app cannot be replaced this way."
        existing != null && existing.enabled in setOf(EnabledState.DISABLED, EnabledState.DISABLED_USER) -> "The app is installed but disabled. Restore it explicitly instead of installing over it."
        existing != null && existing.signer == null -> "The installed app's signing key could not be read."
        existing != null && existing.signer != artifact.signer -> "This app is installed with a different signing key. It will not be replaced."
        else -> null
    }

    fun accepts(info: ApkInfo?): Boolean = info != null &&
        info.pkg == pkg && info.sha256 == artifact.sha256 && info.signer == artifact.signer &&
        info.size == artifact.size && info.versionCode > 0

    fun module(info: ApkInfo): Pair<Module, ModuleVersion> {
        require(accepts(info)) { "Only the pinned and inspected APK may be planned." }
        val version = ModuleVersion(
            versionCode = info.versionCode, versionName = info.versionName, releasedAt = published,
            artifact = artifact, permissions = emptyList(), networkStatement = "Official launcher APK",
            minSdk = minSdk, abis = abis.toList(), postInstall = emptyList(),
        )
        val module = Module(
            id = "tvtoolkit-${choice.name.lowercase()}", name = title,
            oneLine = "Optional TV home screen", description = "Install only; Home is not switched automatically.",
            pkg = pkg, type = "app", tier = Tier.VERIFIED, publisher = "Original project",
            sourceUrl = artifact.url, licence = if (choice == ToolkitLauncher.FLAUNCHER) "GPL-3.0" else "Publisher terms",
            accent = null, artwork = null, versions = listOf(version),
        )
        return module to version
    }
}

object ToolkitLauncherCatalog {
    private val ABI = setOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")

    val flauncher = ToolkitLauncherPin(
        ToolkitLauncher.FLAUNCHER, "FLauncher", PackageName("me.efesser.flauncher"),
        "0.18.0", "2023-03-29", 26,
        ArtifactRef(
            url = "https://gitlab.com/flauncher/flauncher/-/releases/0.18.0/downloads/flauncher-0.18.0.apk",
            size = 26729298L,
            sha256 = Sha256("376fc528c880cdf7c7c04b2d3708c69f911144498218babb6300808b772af32e"),
            signer = Sha256("e017f5b75829ded190219825ab7c1b27d21c1f4ec8d7dbd10f5c790348528046"),
        ),
        ABI,
    )

    val projectivy = ToolkitLauncherPin(
        ToolkitLauncher.PROJECTIVY, "Projectivy Launcher", PackageName("com.spocky.projengmenu"),
        "4.73", "2026-10-31", 29,
        ArtifactRef(
            url = "https://github.com/spocky/miproja1/releases/download/4.73/ProjectivyLauncher-4.73-c98-xda-release.apk",
            size = 11437133L,
            sha256 = Sha256("74e33af1e5e13831a69a0669d2b68b02150bf9df1ae1df437dc67e3f8d52c790"),
            signer = Sha256("f6697bf4082ee97511e4de07863193884a015b7ab5860430321bda1042b0aadd"),
        ),
        ABI,
    )

    fun forChoice(choice: ToolkitLauncher): ToolkitLauncherPin? = when (choice) {
        ToolkitLauncher.KEEP_STOCK -> null
        ToolkitLauncher.FLAUNCHER -> flauncher
        ToolkitLauncher.PROJECTIVY -> projectivy
    }
}
