package tv.cleanyour.core.spec

import tv.cleanyour.core.model.Component
import tv.cleanyour.core.model.DeviceFacts
import tv.cleanyour.core.model.GroupDefault
import tv.cleanyour.core.model.HomeResolution
import tv.cleanyour.core.model.PackageEntry
import tv.cleanyour.core.model.PackageGroup
import tv.cleanyour.core.model.PackageName
import tv.cleanyour.core.model.PackageState
import tv.cleanyour.core.model.Platform
import tv.cleanyour.core.model.Profile
import tv.cleanyour.core.model.ProfileMatch
import tv.cleanyour.core.model.Quirk
import tv.cleanyour.core.model.Sha256

/**
 * Owner-supplied, model-specific TV Toolkit presets. They deliberately do not contribute to
 * upstream brand sure-sets: a Philips or Sharp sharing the brand is not necessarily compatible.
 *
 * Every suggested package is ASK, never automatic. The TV's stock launcher stays enabled until
 * an independently tested, TV-side recovery flow is available for third-party launchers.
 * These are imported observations, not a certification of other firmware revisions.
 */
object TvToolkitProfiles {
    const val SHARP_ID = "tvtoolkit-sharp-aquos-tve19a"
    const val PHILIPS_ID = "tvtoolkit-philips-tpm191e"
    const val CANAL_ID = "tvtoolkit-canal-plus-hy40a2-c308"

    private fun pkg(value: String) = PackageName(value)
    private fun group(id: String, title: String, vararg entries: String): PackageGroup =
        PackageGroup(id, title, GroupDefault.ASK, entries.map { PackageEntry(pkg(it), null) })

    private val mustKeep = setOf(
        "com.google.android.tvlauncher",
        "com.google.android.tungsten.setupwraith",
        "com.google.android.apps.mediashell",
        "com.google.android.tv.remote.service",
        "com.google.android.youtube.tv",
        "com.plexapp.android",
        "com.netflix.ninja",
        "com.amazon.amazonvideo.livingroom",
        "com.android.tv",
        "com.google.android.gms",
        "com.google.android.gsf",
    ).map(::pkg).toSet()

    val sharp: Profile = Profile(
        id = SHARP_ID,
        match = ProfileMatch(listOf("AQUOS_TVE19A"), listOf("sindang"), "Sharp", 30, 30, "SHARP/tcbrae_a/sindang:11/"),
        platform = Platform.ANDROID_TV,
        stockLaunchers = listOf(KnownLaunchers.TVLAUNCHER),
        liveTv = pkg("com.android.tv"),
        setupWraith = KnownLaunchers.SETUPWRAITH,
        quirks = setOf(Quirk.STOCK_LAUNCHER_KEEP_ENABLED),
        groups = listOf(
            group("ads-optional", "Reklamy i promocje (wybór)", "com.google.android.tvrecommendations",
                "tv.anoki.acr.controller", "com.cltv.fast", "fusion.android.tv.demo"),
            group("tracking-optional", "Telemetria (wybór)", "com.google.android.feedback"),
            group("preloads-optional", "Dodatkowe aplikacje (wybór)", "com.google.android.videos",
                "com.google.android.play.games", "com.google.android.youtube.tvmusic",
                "com.android.printspooler", "com.google.android.syncadapters.calendar",
                "com.android.providers.calendar", "com.android.deskclock", "umc.com.emanual"),
            group("voice", "Funkcje głosowe (wybór)", "com.google.android.katniss", "com.google.android.tts"),
            group("screensaver", "Wygaszacze (wybór)", "com.google.android.backdrop", "com.android.dreams.basic"),
            group("brand-optional", "Freeview Play i kanały UK (tylko jeśli nie używasz)", "uk.co.freeview.mdsclient",
                "uk.co.freeview.amc_catchup", "uk.co.freeview.onnow", "uk.co.freeview.uktv",
                "uk.co.freeview.fvpconfigauth", "uk.co.freeview.explore", "uk.co.freeview.bbc",
                "uk.co.freeview.ch5", "uk.co.freeview.itv", "uk.co.freeview.stv",
                "uk.co.freeview.amc_horror", "uk.co.freeview.ch4_vod"),
        ),
        neverDisable = mustKeep + listOf(
            "uk.co.freeview.tifbridge", "com.mediatek.dmp.mediaplayer",
            "com.google.android.apps.nbu.smartconnect.tv",
        ).map(::pkg),
        suggestedDock = emptyList(),
        tested = null,
        sourceSha256 = Sha256("6ec3498bb04b81093c2b2d186265fddfdd139c41b45c827472736c1fd540dfb3"),
    )

    val philips: Profile = Profile(
        id = PHILIPS_ID,
        match = ProfileMatch(listOf("TPM191E"), listOf("PH9M_EA_5599"), "Philips", 30, 30, null),
        platform = Platform.ANDROID_TV,
        stockLaunchers = listOf(KnownLaunchers.TVLAUNCHER),
        liveTv = pkg("com.android.tv"),
        setupWraith = KnownLaunchers.SETUPWRAITH,
        quirks = setOf(Quirk.STOCK_LAUNCHER_KEEP_ENABLED),
        groups = listOf(
            group("demo-optional", "Tryb demonstracyjny Philips (wybór)", "org.droidtv.demome"),
            group("ads-optional", "Rekomendacje (wybór)", "org.droidtv.nettvrecommender",
                "org.droidtv.nettv_launcherapp", "org.droidtv.recommendation",
                "com.google.android.tvrecommendations"),
            group("tracking-optional", "Raportowanie użycia (wybór)", "org.droidtv.usagelogger",
                "org.droidtv.tv.frameworklogger", "org.droidtv.candeebug", "org.droidtv.gel",
                "com.google.android.feedback"),
            group("preloads-optional", "Zbędne aplikacje (wybór)", "org.droidtv.sunrise",
                "org.droidtv.sticker", "com.google.android.play.games",
                "com.google.android.videos", "com.google.android.youtube.tvmusic",
                "com.android.printspooler", "com.google.android.syncadapters.calendar",
                "com.android.providers.calendar"),
            group("voice", "Asystenci (wybór)", "com.google.android.katniss", "org.droidtv.amazonalexa"),
            group("brand-optional", "Ambilight + Hue (tylko jeśli nie używasz)", "org.droidtv.alhuewizard"),
        ),
        neverDisable = mustKeep + listOf(
            "org.droidtv.oad", "com.easy.oad", "org.droidtv.dlna",
            "org.droidtv.teletext", "org.droidtv.usbbreakin",
            "org.droidtv.leanbackcustomizer", "org.droidtv.nettv.market",
        ).map(::pkg),
        suggestedDock = emptyList(),
        tested = null,
        sourceSha256 = Sha256("956482e47803e539c98699f66bed819de60cb642ed029217af53ef937b41f47d"),
    )

    /**
     * CANAL+ BOX HY40A2, exactly the Android 10 C3.0.8_20220628 firmware in the owner's
     * 2026-11-01 report. The operator's Home, playback, tuner, DRM, pairing, remote, OTA and
     * Google infrastructure are protected. The other 90 observed system packages are
     * protected as well: only three nonessential entertainment apps are offered, one by one.
     * Any other firmware falls back to upstream safety handling, never to this preset.
     */
    internal val canalObservedSystemPackages: Set<String> = setOf(
        "android", "android.autoinstalls.config.skyworthdigital.hy4001", "com.amlogic.multimediaplayer",
        "com.android.backupconfirm", "com.android.bluetooth", "com.android.bluetoothmidiservice",
        "com.android.camera2", "com.android.captiveportallogin", "com.android.certinstaller",
        "com.android.companiondevicemanager", "com.android.cts.ctsshim", "com.android.cts.priv.ctsshim",
        "com.android.dreams.basic", "com.android.dynsystem", "com.android.externalstorage",
        "com.android.gallery3d", "com.android.htmlviewer", "com.android.inputdevices",
        "com.android.keychain", "com.android.localtransport", "com.android.location.fused",
        "com.android.modulemetadata", "com.android.networkstack.inprocess", "com.android.networkstack.permissionconfig",
        "com.android.pacprocessor", "com.android.printspooler", "com.android.providers.calendar",
        "com.android.providers.contacts", "com.android.providers.downloads", "com.android.providers.media",
        "com.android.providers.settings", "com.android.providers.tv", "com.android.providers.userdictionary",
        "com.android.proxyhandler", "com.android.se", "com.android.settings.intelligence",
        "com.android.sharedstoragebackup", "com.android.shell", "com.android.statementservice",
        "com.android.systemui", "com.android.tv.settings", "com.android.vending",
        "com.android.vpndialogs", "com.android.wallpaperbackup", "com.canal.box",
        "com.ctc", "com.droidlogic", "com.droidlogic.ble",
        "com.droidlogic.droidtvsettingsoverlay", "com.droidlogic.dvbaudioservice", "com.droidlogic.imageplayer",
        "com.droidlogic.overlay", "com.droidlogic.readlog", "com.droidlogic.tv.settings",
        "com.google.android.apps.mediashell", "com.google.android.backdrop", "com.google.android.ext.services",
        "com.google.android.ext.shared", "com.google.android.feedback", "com.google.android.gms",
        "com.google.android.gsf", "com.google.android.inputmethod.latin", "com.google.android.katniss",
        "com.google.android.marvin.talkback", "com.google.android.onetimeinitializer", "com.google.android.packageinstaller",
        "com.google.android.partnersetup", "com.google.android.permissioncontroller", "com.google.android.play.games",
        "com.google.android.sss.authbridge", "com.google.android.syncadapters.calendar", "com.google.android.tts",
        "com.google.android.tungsten.setupwraith", "com.google.android.tv.bugreportsender", "com.google.android.tv.frameworkpackagestubs",
        "com.google.android.tv.remote.service", "com.google.android.tvrecommendations", "com.google.android.videos",
        "com.google.android.webview", "com.google.android.youtube.tv", "com.google.android.youtube.tvmusic",
        "com.netflix.ninja", "com.rock.stb", "com.skyworthdigital.ncplus.frontpanelledsservice",
        "com.skyworthdigital.ncplus.rcu.upgrade.skyboxactivity", "com.skyworthdigital.ncplus.setupwraith", "com.skyworthdigital.ncplusbtservice",
        "com.skyworthdigital.ncplusrcubatteryservice", "com.skyworthdigital.ncplusrcuota", "com.skyworthdigital.producttest",
        "com.skyworthdigital.stb.ncplus", "com.skyworthdigital.system.server", "pl.canalplus.tvod.atv"
    )
    internal val canalOptional: Set<String> = setOf(
        "com.google.android.play.games", "com.google.android.youtube.tvmusic", "com.google.android.videos"
    )
    val canal: Profile = Profile(
        id = CANAL_ID,
        match = ProfileMatch(listOf("CANAL PLUS BOX"), listOf("HY40A2"), "CANAL-PLUS",
            29, 29, "CANAL-PLUS/HY4001/HY40A2:10/QTT8.201201.002/C3.0.8_20220628:"),
        platform = Platform.ANDROID_TV,
        stockLaunchers = listOf(Component(pkg("com.canal.box"), ".ui.boot.BoxMainActivity")),
        liveTv = null, // Do not guess which operator component implements Live TV.
        setupWraith = null, // Never touch first-boot or operator activation services.
        quirks = setOf(Quirk.STOCK_LAUNCHER_KEEP_ENABLED),
        groups = listOf(
            group("canal-optional-google", "Opcjonalne aplikacje Google (wybór)",
                "com.google.android.play.games", "com.google.android.youtube.tvmusic",
                "com.google.android.videos"),
        ),
        neverDisable = mustKeep + (canalObservedSystemPackages - canalOptional).map(::pkg),
        suggestedDock = emptyList(),
        tested = null, // Report-only: no tested disable or successful reboot has been observed.
        sourceSha256 = Sha256("d6206c1899e584619350b1f5698e77d9ea6eae10fa3afc1a6a411c7652590fc5"),
    )

    val all: List<Profile> = listOf(sharp, philips, canal)

    /** Both observed model and device must match; matching by brand or platform alone is unsafe. */
    fun exact(facts: DeviceFacts): Profile? = all.firstOrNull { profile ->
        val match = profile.match
        (facts.brand.equals(match.brand, ignoreCase = true) ||
            facts.manufacturer.equals(match.brand, ignoreCase = true)) &&
            match.models.any { it.equals(facts.model, ignoreCase = true) } &&
            match.devices.any { it.equals(facts.device, ignoreCase = true) } &&
            facts.sdk >= (match.sdkMin ?: Int.MIN_VALUE) &&
            facts.sdk <= (match.sdkMax ?: Int.MAX_VALUE) &&
            (match.fingerprintPrefix?.let { facts.fingerprint.startsWith(it) } ?: true)
    }
}

/** Adds three strictly matched, opt-in presets without changing existing upstream TV profiles. */
class TvToolkitProfileSource(private val upstream: BundledProfileSource) : HomeAwareProfileSource {
    override suspend fun all(): List<Profile> = upstream.all() + TvToolkitProfiles.all

    /** Preserve the upstream brand sure-sets; our three exact presets never extend them. */
    fun sureSets(): List<SureSet> = upstream.sureSets()

    override suspend fun resolve(facts: DeviceFacts): ProfileResolution =
        resolve(facts, null, null)

    override suspend fun resolve(
        facts: DeviceFacts,
        home: HomeResolution?,
        packages: Map<PackageName, PackageState>?,
    ): ProfileResolution {
        val local = TvToolkitProfiles.exact(facts)?.takeIf { HomeFit.fits(it, home, packages) }
        return if (local != null) ProfileResolution.Exact(local) else upstream.resolve(facts, home, packages)
    }
}
