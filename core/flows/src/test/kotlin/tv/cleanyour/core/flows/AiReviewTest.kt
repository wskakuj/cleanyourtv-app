package tv.cleanyour.core.flows

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.cleanyour.core.model.Component
import tv.cleanyour.core.model.EnabledState
import tv.cleanyour.core.model.HomeClaimant
import tv.cleanyour.core.model.HomeResolution
import tv.cleanyour.core.model.PackageName
import tv.cleanyour.core.model.PackageState
import tv.cleanyour.testing.fixtures.Samples

class AiReviewTest {
    private val safe = PackageName("com.example.retaildemo")
    private val stock = PackageName("com.google.android.tvlauncher")
    private val settings = PackageName("com.android.settings")
    private val alreadyOff = PackageName("com.example.oldapp")

    private fun state(enabled: EnabledState = EnabledState.ENABLED) =
        PackageState(enabled, true, 1, null, false, false, null, false)

    private val snapshot = Samples.miTvSnapshot().let { s ->
        val home = Component(stock, ".MainActivity")
        s.copy(
            facts = s.facts.copy(brand = "SHARP", model = "AQUOS_TVE19A"),
            packages = mapOf(safe to state(), stock to state(), settings to state(), alreadyOff to state(EnabledState.DISABLED_USER)),
            home = HomeResolution(home, listOf(HomeClaimant(home, 1))),
        )
    }

    private fun answer(vararg packages: String) = buildString {
        appendLine(AiReview.HEADER)
        appendLine("BRAND=SHARP")
        appendLine("MODEL=AQUOS_TVE19A")
        packages.forEach { appendLine("DISABLE=$it") }
        append(AiReview.END)
    }

    @Test fun `only known unprotected package names can be previewed`() {
        val preview = AiReview.parse(answer(safe.value), snapshot)
        assertTrue(preview.valid)
        assertTrue(preview.packages == listOf(safe))
        assertTrue(preview.problems.isEmpty())
    }

    @Test fun `wrong television identity is rejected`() {
        val preview = AiReview.parse(answer(safe.value).replace("AQUOS_TVE19A", "OTHER_TV"), snapshot)
        assertFalse(preview.valid)
        assertTrue(preview.problems.any { "model" in it.lowercase() })
    }

    @Test fun `stock home and system settings cannot be imported`() {
        val preview = AiReview.parse(answer(safe.value, stock.value, settings.value), snapshot)
        assertFalse(preview.valid)
        assertTrue(preview.problems.any { stock.value in it })
        assertTrue(preview.problems.any { settings.value in it })
    }

    @Test fun `shell injection or ADB commands are rejected`() {
        val preview = AiReview.parse(answer("com.example.demo; pm clear com.android.settings"), snapshot)
        assertFalse(preview.valid)
        assertTrue(preview.problems.any { "Niepoprawna" in it })
    }

    @Test fun `not-present and previously disabled packages are rejected`() {
        val preview = AiReview.parse(answer("com.example.unknown", alreadyOff.value), snapshot)
        assertFalse(preview.valid)
        assertTrue(preview.problems.size == 2)
    }

    @Test fun `duplicate or arbitrary lines are rejected as a whole`() {
        val preview = AiReview.parse(answer(safe.value, safe.value).replace(AiReview.END, "adb shell reboot\n${AiReview.END}"), snapshot)
        assertFalse(preview.valid)
        assertTrue(preview.problems.any { "Zduplikowany" in it })
        assertTrue(preview.problems.any { "Niedozwolony" in it })
    }

    @Test fun `prompt includes complete inventory but not fingerprint or serial`() {
        val prompt = AiReview.prompt(snapshot)
        assertTrue("MODEL=AQUOS_TVE19A" in prompt)
        assertTrue("S ON ${safe.value}" in prompt)
        assertTrue("S OFF ${alreadyOff.value}" in prompt)
        assertFalse(snapshot.facts.fingerprint in prompt)
        assertFalse(snapshot.facts.serialLast4 in prompt)
    }
}
