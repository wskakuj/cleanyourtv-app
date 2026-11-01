package tv.cleanyour.core.planner

import org.junit.Test
import tv.cleanyour.core.model.Component
import tv.cleanyour.core.model.EnabledState
import tv.cleanyour.core.model.HomeClaimant
import tv.cleanyour.core.model.HomeResolution
import tv.cleanyour.core.model.Op
import tv.cleanyour.core.model.PackageName
import tv.cleanyour.core.model.PackageState
import tv.cleanyour.core.planner.PlanRequest
import tv.cleanyour.core.planner.PlanResult
import tv.cleanyour.core.spec.BundledProfileSource
import tv.cleanyour.core.spec.TvToolkitProfileSource
import tv.cleanyour.core.store.SnapshotCodec
import tv.cleanyour.testing.fixtures.Samples
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Safety checks through the actual planner and profile resolution, not only a UI list. */
class CanalBoxSafetyTest {
    private fun pkg(value: String) = PackageName(value)
    private val canalHome = Component(pkg("com.canal.box"), ".ui.boot.BoxMainActivity")
    private val fallback = Component(pkg("com.android.tv.settings"), ".system.FallbackHome")
    private val state = PackageState(EnabledState.DEFAULT, true, 1L, null, false, false, null, false)
    private val facts = Samples.miTvFacts().copy(
        brand = "CANAL-PLUS", manufacturer = "SkyworthDigital", model = "CANAL PLUS BOX",
        device = "HY40A2", sdk = 29,
        fingerprint = "CANAL-PLUS/HY4001/HY40A2:10/QTT8.201201.002/C3.0.8_20220628:user/release-keys")
    private val snap = Samples.miTvSnapshot().copy(
        facts = facts,
        home = HomeResolution(canalHome, listOf(HomeClaimant(canalHome, 2), HomeClaimant(fallback, -1000))),
        packages = Samples.miTvPackages() + mapOf(
            pkg("com.canal.box") to state,
            pkg("com.skyworthdigital.ncplusbtservice") to state,
            pkg("com.google.android.play.games") to state,
        ),
    ).let { it.copy(contentHash = SnapshotCodec.hash(it)) }
    private val planner = PlannerFixtures.planner(profiles = TvToolkitProfileSource(BundledProfileSource()))

    @Test fun criticalOperatorPackagesCannotBeDisabledEvenInManage() {
        for (name in listOf("com.canal.box", "com.skyworthdigital.ncplusbtservice")) {
            val result = planner.plan(PlanRequest.SwitchOff(setOf(pkg(name))), snap, PlannerFixtures.facts, emptyList())
            assertIs<PlanResult.Refused>(result, name)
        }
    }

    @Test fun oneOptionalGoogleAppCanBeDisabledOnlyByExplicitRequest() {
        val result = planner.plan(PlanRequest.SwitchOff(setOf(pkg("com.google.android.play.games"))),
            snap, PlannerFixtures.facts, emptyList())
        val steps = assertIs<PlanResult.Ok>(result).plan.steps
        assertTrue(steps.any { (it.op as? Op.Disable)?.pkg == pkg("com.google.android.play.games") })
        assertTrue(steps.none { it.op is Op.SetHome || (it.op as? Op.Disable)?.pkg == pkg("com.canal.box") })
    }

    @Test fun homeTakeoverIsRejectedByTheOperatorRiskRule() {
        val request = PlannerFixtures.setup(
            profile = tv.cleanyour.core.spec.TvToolkitProfiles.canal,
            choices = PlannerFixtures.choices(installLauncher = true, takeover = true),
        )
        val result = planner.plan(request, snap, PlannerFixtures.facts, emptyList())
        val refused = assertIs<PlanResult.Refused>(result)
        assertTrue(refused.reasons.any { "CANAL+ BOX uses its operator's home" in it }, refused.reasons.toString())
    }
}
