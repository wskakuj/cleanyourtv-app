package tv.cleanyour.core.flows

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import tv.cleanyour.core.model.EnabledState
import tv.cleanyour.core.model.Op
import tv.cleanyour.core.model.RunToken
import tv.cleanyour.testing.fixtures.Samples

class RecoveryGateTest {
    private val current = Samples.miTvSnapshot()

    @Test fun `no baseline means no writes`() {
        assertNotNull(RecoveryGate.refusal(null, current, listOf(Op.Disable(current.packages.keys.first()))))
    }

    @Test fun `regular cleanup with baseline does not need a home watchdog`() {
        val candidate = current.packages.keys.first()
        assertNull(RecoveryGate.refusal(current, current, listOf(Op.Disable(candidate))))
    }

    @Test fun `home switch is denied without a watchdog`() {
        val home = current.home.resolved ?: error("Fixture needs home")
        assertNotNull(RecoveryGate.refusal(current, current, listOf(Op.SetHome(home))))
    }

    @Test fun `armed watchdog allows preflight when stock launcher is enabled`() {
        val home = current.home.resolved ?: error("Fixture needs home")
        val enabled = current.copy(packages = current.packages + (home.pkg to current.packages[home.pkg]!!.copy(enabled = EnabledState.ENABLED)))
        val ops = listOf(Op.ArmHomeWatchdog(home, 150, RunToken("0123456789abcdef")), Op.SetHome(home))
        assertNull(RecoveryGate.refusal(enabled, enabled, ops))
    }

    @Test fun `watchdog is not enough if the stock home is already disabled`() {
        val home = current.home.resolved ?: error("Fixture needs home")
        val disabled = current.copy(packages = current.packages + (home.pkg to current.packages[home.pkg]!!.copy(enabled = EnabledState.DISABLED_USER)))
        val ops = listOf(Op.ArmHomeWatchdog(home, 150, RunToken("0123456789abcdef")), Op.SetHome(home))
        assertNotNull(RecoveryGate.refusal(disabled, disabled, ops))
    }
}
