package tv.cleanyour.core.spec

import kotlinx.coroutines.runBlocking
import org.junit.Test
import tv.cleanyour.core.model.Component
import tv.cleanyour.core.model.GroupDefault
import tv.cleanyour.core.model.HomeClaimant
import tv.cleanyour.core.model.HomeResolution
import tv.cleanyour.core.model.PackageName
import tv.cleanyour.core.model.Quirk
import tv.cleanyour.testing.fixtures.Samples
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TvToolkitProfilesTest {
    private val source = TvToolkitProfileSource(BundledProfileSource())
    private val sample = Samples.miTvFacts()

    @Test fun sharpRequiresModelAndDeviceAndBrand() = runBlocking {
        val real = sample.copy(brand = "Sharp", manufacturer = "SHARP", model = "AQUOS_TVE19A",
            device = "sindang", sdk = 30, fingerprint = "SHARP/tcbrae_a/sindang:11/RTOB.250721.001/13808904:user/release-keys")
        assertEquals(TvToolkitProfiles.SHARP_ID, assertIs<ProfileResolution.Exact>(source.resolve(real)).profile.id)
        assertNull(TvToolkitProfiles.exact(real.copy(device = "another_tv")))
        assertNull(TvToolkitProfiles.exact(real.copy(model = "unknown_sharp")))
        assertNull(TvToolkitProfiles.exact(real.copy(brand = "Another", manufacturer = "Another")))
        assertNull(TvToolkitProfiles.exact(real.copy(sdk = 29)))
        assertNull(TvToolkitProfiles.exact(real.copy(sdk = 31)))
        assertNull(TvToolkitProfiles.exact(real.copy(fingerprint = "SHARP/other/sindang:11/build")))
        assertNull(TvToolkitProfiles.exact(real.copy(sdk = 34)))
    }

    @Test fun sharpWithThirdPartyHomeStillMatchesWithoutChangingHome() = runBlocking {
        val real = sample.copy(brand = "SHARP", manufacturer = "SHARP", model = "AQUOS_TVE19A",
            device = "sindang", sdk = 30, fingerprint = "SHARP/tcbrae_a/sindang:11/RTOB.250721.001/13808904:user/release-keys")
        val alternativeHome = Component(PackageName("com.example.launcher"), ".Home")
        val fallback = Component(PackageName("com.android.tv.settings"), ".system.FallbackHome")
        val home = HomeResolution(alternativeHome, listOf(HomeClaimant(alternativeHome, 0), HomeClaimant(fallback, -1000)))
        val resolved = assertIs<ProfileResolution.Exact>(source.resolve(real, home))
        assertEquals(TvToolkitProfiles.SHARP_ID, resolved.profile.id)
        assertTrue(Quirk.STOCK_LAUNCHER_KEEP_ENABLED in resolved.profile.quirks)
        assertTrue(resolved.profile.groups.all { it.default == GroupDefault.ASK })
    }

    @Test fun philipsMatchesObservedFirmwareOnly() = runBlocking {
        val real = sample.copy(brand = "Philips", manufacturer = "Philips", model = "TPM191E",
            device = "PH9M_EA_5599", sdk = 30)
        assertEquals(TvToolkitProfiles.PHILIPS_ID, assertIs<ProfileResolution.Exact>(source.resolve(real)).profile.id)
        assertNull(TvToolkitProfiles.exact(real.copy(device = "different_platform")))
        assertNull(TvToolkitProfiles.exact(real.copy(sdk = 29)))
        assertNull(TvToolkitProfiles.exact(real.copy(model = "TPM191F")))
    }

    @Test fun aGoogleTvHomeNeverGetsOurAndroidTvSpecificProfile() = runBlocking {
        val real = sample.copy(brand = "Sharp", manufacturer = "SHARP", model = "AQUOS_TVE19A",
            device = "sindang", sdk = 30, fingerprint = "SHARP/tcbrae_a/sindang:11/RTOB.250721.001/13808904:user/release-keys")
        val googleHome = KnownLaunchers.LAUNCHERX
        val home = HomeResolution(googleHome, listOf(HomeClaimant(googleHome, 2)))
        val resolved = source.resolve(real, home)
        assertFalse(resolved is ProfileResolution.Exact && resolved.profile.id == TvToolkitProfiles.SHARP_ID)
    }

    @Test fun allCuratedPackagesRequireExplicitChoiceAndExcludeCriticalServices() {
        assertEquals(2, TvToolkitProfiles.all.size)
        TvToolkitProfiles.all.forEach { profile ->
            assertTrue(profile.groups.isNotEmpty())
            assertTrue(profile.groups.all { it.default == GroupDefault.ASK }, profile.id)
            val suggestions = profile.groups.flatMap { it.packages.map { entry -> entry.name } }
            assertEquals(suggestions.size, suggestions.toSet().size, profile.id)
            assertTrue(suggestions.none { it in profile.neverDisable }, profile.id)
            assertTrue(Quirk.STOCK_LAUNCHER_KEEP_ENABLED in profile.quirks, profile.id)
            assertTrue(profile.stockLaunchers.all { it.pkg in profile.neverDisable }, profile.id)
        }
        assertTrue(PackageName("org.droidtv.oad") in TvToolkitProfiles.philips.neverDisable)
        assertTrue(PackageName("com.easy.oad") in TvToolkitProfiles.philips.neverDisable)
        assertTrue(PackageName("uk.co.freeview.tifbridge") in TvToolkitProfiles.sharp.neverDisable)
        assertNotNull(TvToolkitProfiles.all.firstOrNull { it.id == TvToolkitProfiles.SHARP_ID })
    }
}
