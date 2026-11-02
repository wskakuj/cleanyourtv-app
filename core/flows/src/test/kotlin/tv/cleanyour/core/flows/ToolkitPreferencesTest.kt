package tv.cleanyour.core.flows

import org.junit.Test
import kotlinx.coroutines.test.runTest
import tv.cleanyour.testing.fixtures.MemoryDocumentStore
import tv.cleanyour.core.model.TvId
import tv.cleanyour.testing.fixtures.Samples
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToolkitPreferencesTest {
    @Test fun launcherChoiceIsKeptPerTvAndDefaultsToNoChange() = runTest {
        val documents = MemoryDocumentStore()
        val store = ToolkitPreferencesStore(documents)
        val sharp = TvId("aabbccdd11223344")
        val philips = TvId("aabbccdd55667788")
        assertEquals(null, store.loadLauncher(sharp))
        store.saveLauncher(sharp, ToolkitLauncher.PROJECTIVY)
        store.saveLauncher(philips, ToolkitLauncher.FLAUNCHER)
        assertEquals(ToolkitLauncher.PROJECTIVY, store.loadLauncher(sharp))
        assertEquals(ToolkitLauncher.FLAUNCHER, store.loadLauncher(philips))
        store.saveLauncher(sharp, ToolkitLauncher.KEEP_STOCK)
        assertEquals(ToolkitLauncher.KEEP_STOCK, store.loadLauncher(sharp))
        assertEquals(ToolkitLauncher.FLAUNCHER, store.loadLauncher(philips))
    }

    @Test fun defaultsKeepStockAndNeverInstallAnything() {
        val text = ToolkitPreferences().export(Samples.miTvFacts())
        assertTrue("LAUNCHER=KEEP_STOCK" in text)
        assertTrue("YOUTUBE=KEEP_STOCK" in text)
        assertTrue("MEDIA=NONE" in text)
        assertTrue("WALLPAPER=KEEP" in text)
        assertTrue("STATUS=PREVIEW_ONLY" in text)
        assertFalse("pm disable" in text)
        assertFalse("adb shell" in text)
    }

    @Test fun choicesCanCombineProjectivyAdFreeYouTubeAndBothMediaApps() {
        val preferences = ToolkitPreferences(
            launcher = ToolkitLauncher.PROJECTIVY,
            youtube = ToolkitYouTube.ALTERNATIVE,
            media = ToolkitMedia.BOTH,
            wallpaper = ToolkitWallpaper.GRAPHITE,
        )
        val text = preferences.export(Samples.miTvFacts().copy(brand = "Philips", model = "TPM191E"))
        assertEquals("TV_TOOLKIT_PREFERENCES_V1", text.lineSequence().first())
        assertTrue("BRAND=Philips" in text)
        assertTrue("MODEL=TPM191E" in text)
        assertTrue("LAUNCHER=PROJECTIVY" in text)
        assertTrue("YOUTUBE=ALTERNATIVE" in text)
        assertTrue("MEDIA=BOTH" in text)
        assertTrue("WALLPAPER=GRAPHITE" in text)
        assertEquals("END_TV_TOOLKIT_PREFERENCES", text.trim().lines().last())
    }

    @Test fun generatedPlanDoesNotLeakSerialOrFingerprint() {
        val facts = Samples.miTvFacts().copy(serialLast4 = "1234", fingerprint = "top-secret-fingerprint")
        val text = ToolkitPreferences().export(facts)
        assertFalse("1234" in text)
        assertFalse("top-secret-fingerprint" in text)
    }
}
