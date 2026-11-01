package tv.cleanyour.core.flows

import org.junit.Test
import tv.cleanyour.core.model.ApkInfo
import tv.cleanyour.core.model.EnabledState
import tv.cleanyour.core.model.PackageName
import tv.cleanyour.core.model.PackageState
import tv.cleanyour.core.model.Sha256
import tv.cleanyour.testing.fixtures.Samples
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ToolkitLauncherCatalogTest {
    private val facts = Samples.miTvFacts().copy(sdk = 30, abis = listOf("arm64-v8a"), installRefusal = false)

    @Test fun noChangeIsNotAnInstallation() {
        assertNull(ToolkitLauncherCatalog.forChoice(ToolkitLauncher.KEEP_STOCK))
    }

    @Test fun bothOfficialPinsUseExactPackagesAndUniqueArtifacts() {
        val a = ToolkitLauncherCatalog.flauncher
        val b = ToolkitLauncherCatalog.projectivy
        assertEquals(PackageName("me.efesser.flauncher"), a.pkg)
        assertEquals(PackageName("com.spocky.projengmenu"), b.pkg)
        assertTrue(a.artifact.url.startsWith("https://gitlab.com/flauncher/flauncher/"))
        assertTrue(b.artifact.url.startsWith("https://github.com/spocky/miproja1/releases/"))
        assertTrue(a.artifact.sha256 != b.artifact.sha256)
        assertTrue(a.artifact.signer != b.artifact.signer)
        assertTrue(a.artifact.size > 0 && b.artifact.size > 0)
    }

    @Test fun aDifferentSigningKeyOrMissingAbiIsRefused() {
        val pin = ToolkitLauncherCatalog.projectivy
        val wrong = PackageState(EnabledState.DEFAULT, false, 1L, Sha256("a".repeat(64)),
            false, false, null, false)
        assertNotNull(pin.refusal(facts, wrong))
        assertNotNull(pin.refusal(facts.copy(abis = emptyList()), null))
        assertNotNull(pin.refusal(facts.copy(sdk = 28), null))
        assertNotNull(pin.refusal(facts.copy(installRefusal = true), null))
        assertNull(pin.refusal(facts, null))
    }

    @Test fun onlyAnExactInspectedApkMayReachThePlanner() {
        val pin = ToolkitLauncherCatalog.flauncher
        val good = ApkInfo(pin.pkg, 18, pin.release, pin.artifact.signer,
            pin.artifact.sha256, pin.artifact.size, "GET_SIGNING_CERTIFICATES")
        assertTrue(pin.accepts(good))
        assertFalse(pin.accepts(good.copy(pkg = PackageName("com.example.imposter"))))
        assertFalse(pin.accepts(good.copy(signer = Sha256("b".repeat(64)))))
        assertFalse(pin.accepts(good.copy(size = pin.artifact.size - 1)))
        assertFalse(pin.accepts(null))
        val (module, version) = pin.module(good)
        assertEquals(pin.pkg, module.pkg)
        assertEquals(pin.artifact, version.artifact)
        assertEquals(18L, version.versionCode)
    }
}
