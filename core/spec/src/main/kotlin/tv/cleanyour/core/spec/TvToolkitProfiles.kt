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
        match = ProfileMatch(listOf("AQUOS_TVE19A", "65BL2EA"), listOf("Aquos_tve19a"), "Sharp", 26, 32, null),
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

    val all: List<Profile> = listOf(sharp, philips)

    /** Both observed model and device must match; matching by brand or platform alone is unsafe. */
    fun exact(facts: DeviceFacts): Profile? = all.firstOrNull { profile ->
        val match = profile.match
        (facts.brand.equals(match.brand, ignoreCase = true) ||
            facts.manufacturer.equals(match.brand, ignoreCase = true)) &&
            match.models.any { it.equals(facts.model, ignoreCase = true) } &&
            match.devices.any { it.equals(facts.device, ignoreCase = true) } &&
            facts.sdk >= (match.sdkMin ?: Int.MIN_VALUE) &&
            facts.sdk <= (match.sdkMax ?: Int.MAX_VALUE)
    }
}

/** Adds two strictly matched, opt-in presets without changing existing upstream TV profiles. */
class TvToolkitProfileSource(private val upstream: HomeAwareProfileSource) : HomeAwareProfileSource {
    override suspend fun all(): List<Profile> = upstream.all() + TvToolkitProfiles.all

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
