package tv.cleanyour.core.flows

import tv.cleanyour.core.model.EnabledState
import tv.cleanyour.core.model.Op
import tv.cleanyour.core.model.Snapshot

/** Fail closed before any Setup write. The engine still owns the stronger live and watchdog guards. */
object RecoveryGate {
    fun refusal(baseline: Snapshot?, current: Snapshot?, operations: List<Op>): String? {
        if (baseline == null || baseline.packages.isEmpty()) return "Brak kompletnej kopii stanu pakietow. Nic nie zmieniono."
        if (current == null || current.packages.isEmpty() || current.tvId != baseline.tvId) {
            return "Nie mozna potwierdzic tozsamosci TV i aktualnego stanu. Nic nie zmieniono."
        }
        val homePackages = current.home.claimants.map { it.component.pkg }.toSet() + listOfNotNull(current.home.resolved?.pkg)
        val touchesHome = operations.any { op -> op is Op.SetHome || op is Op.Disable && op.pkg in homePackages }
        if (!touchesHome) return null
        val watchdog = operations.filterIsInstance<Op.ArmHomeWatchdog>().firstOrNull()
            ?: return "Przelaczenie ekranu glownego bez timera awaryjnego jest zabronione."
        val stockState = current.packages[watchdog.stock.pkg]
            ?: return "Nie znaleziono fabrycznego launchera do awaryjnego przywrocenia."
        if (stockState.enabled == EnabledState.DISABLED_USER || stockState.enabled == EnabledState.DISABLED) {
            return "Fabryczny launcher jest juz wylaczony. Najpierw trzeba go przywrocic."
        }
        return null
    }
}
