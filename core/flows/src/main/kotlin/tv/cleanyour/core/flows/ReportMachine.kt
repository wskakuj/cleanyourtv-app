package tv.cleanyour.core.flows

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import tv.cleanyour.core.model.Profile
import tv.cleanyour.core.engine.RunStatus
import tv.cleanyour.core.model.Op
import tv.cleanyour.core.model.PackageName
import tv.cleanyour.core.planner.PlanRequest
import tv.cleanyour.core.planner.PlanResult
import tv.cleanyour.core.spec.resolveFor
import tv.cleanyour.core.model.Snapshot
import tv.cleanyour.core.model.TvId
import tv.cleanyour.core.spec.ProfileResolution

// 0.3.2 B1 and B2: the report preview. "Help add this TV" (TV) is the TV report for a TV with no exact
// profile; "Report a problem" (PROBLEM) is the redacted diagnostics log with a short TV summary. Both
// are made from what this phone stored, so the TV need not be on. The screen shows the exact text,
// then copies it and opens the GitHub form, or hands it to the share sheet. Nothing is sent by the
// app: the clipboard, the browser and the share sheet are the person's.
// 0.4.8: CONFIRM ("confirm") is "Tell us it worked", the confirmation of a TV whose restarts this phone counted
// (ItWorked); it needs a TV, like TV.
enum class ReportKind(val key: String) {
    TV("tv"), PROBLEM("problem"), CONFIRM("confirm");

    companion object { fun of(key: String): ReportKind? = entries.firstOrNull { it.key == key } }
}

data class ReportState(
    val kind: ReportKind,
    val loading: Boolean = true,
    // What the preview shows and the GitHub form gets (in the link, or from the clipboard).
    val text: String = "",
    val url: String = "",
    // False when there is nothing to report from (TV: this phone has no snapshot of the TV).
    val available: Boolean = true,
    // 0.3.3 D1: the report rides in the link, so the form opens with it filled in. False: too long
    // for the link, the person pastes it, and the screen says so before GitHub opens.
    val inLink: Boolean = false,
    // What Share sends: the TV report with one package per line (TvReport.text); the problem report
    // as shown. Empty means the shown text.
    val shareText: String = "",
    // 0.4.6: the link holds a shorter problem report (ProblemReports.fit: fewer log lines); the
    // whole one is shown, copied and shared.
    val shortened: Boolean = false,
    // 0.4.7 (must 4): "Help add this TV" remembers the report it sent (SentReports): when, and the
    // issue once the person pasted its link. Null before the first send. `issueError`: the pasted
    // text is not a link to an issue of this app.
    val sent: SentReport? = null,
    val issueError: Boolean = false,
    // TV Toolkit: opt-in, offline AI-assisted review. A suggestion cannot itself write to a TV.
    val aiPrompt: String = "",
    val aiPreview: AiReview.Preview? = null,
    val toolkitProfileName: String? = null,
    val aiExample: String = "",
    val aiApplying: Boolean = false,
    val aiOutcome: String? = null,
    val aiApplied: Boolean = false,
)

sealed interface ReportIntent {
    data object CopyAndOpen : ReportIntent
    data object Share : ReportIntent
    // 0.4.7: the issue the TV report went to, pasted by the person; empty forgets it.
    data class SaveIssue(val url: String) : ReportIntent
    data object OpenIssue : ReportIntent
    data object CopyAiPrompt : ReportIntent
    data class PreviewAi(val text: String) : ReportIntent
    data class ApplyAi(val selected: Set<PackageName>) : ReportIntent
    data class CopyToolkitPreferences(val preferences: ToolkitPreferences) : ReportIntent
}

class ReportMachine(
    private val d: ManageDeps,
    scope: CoroutineScope,
    kind: ReportKind,
    private val tvId: TvId?,
) : Machine<ReportState, ReportIntent, ManageEffect>(ReportState(kind), scope) {

    private data class Loaded(val text: String, val url: String, val available: Boolean, val inLink: Boolean = false, val share: String = "", val shortened: Boolean = false,
                              val sent: SentReport? = null, val aiPrompt: String = "", val aiExample: String = "", val toolkitProfileName: String? = null)
    private data class Sent(val r: SentReport?, val error: Boolean = false)
    private data class AiPreviewed(val preview: AiReview.Preview)
    private data class AiApplyUpdate(val busy: Boolean, val status: String?, val succeeded: Boolean = false)

    private var aiSnapshot: Snapshot? = null
    private var aiProfile: Profile? = null
    private var reviewedText: String? = null
    private var aiRunActive: Boolean = false

    init { scope.launch { send(build(kind)) } }

    override fun reduce(state: ReportState, event: Any): ReportState = when (event) {
        is Loaded -> state.copy(loading = false, text = event.text, url = event.url, available = event.available, inLink = event.inLink, shareText = event.share,
            shortened = event.shortened, sent = event.sent, aiPrompt = event.aiPrompt, aiExample = event.aiExample, toolkitProfileName = event.toolkitProfileName)
        is Sent -> state.copy(sent = event.r ?: state.sent, issueError = event.error)
        is AiPreviewed -> state.copy(aiPreview = event.preview, aiOutcome = null, aiApplied = false)
        is AiApplyUpdate -> state.copy(aiApplying = event.busy, aiOutcome = event.status, aiApplied = event.succeeded)
        else -> state
    }

    override fun handle(event: Any, before: ReportState, after: ReportState) {
        if (after.loading || !after.available) return
        when (event) {
            // 0.3.3 D1: copied also when the report is in the link, in case the form opens without it
            // (a sign-in on the way can drop it). The screen says which way it goes.
            ReportIntent.CopyAndOpen -> { effect(ManageEffect.Copy(after.text)); effect(ManageEffect.OpenUrl(after.url)); remember(); told() }
            ReportIntent.Share -> { effect(ManageEffect.Share(after.shareText.ifEmpty { after.text })); remember(); told() }
            is ReportIntent.SaveIssue -> scope.launch { saveIssue(event.url) }
            ReportIntent.OpenIssue -> after.sent?.issueUrl?.let { effect(ManageEffect.OpenUrl(it)) }
            ReportIntent.CopyAiPrompt -> if (after.kind == ReportKind.TV && after.aiPrompt.isNotEmpty()) effect(ManageEffect.Copy(after.aiPrompt))
            is ReportIntent.CopyToolkitPreferences -> if (after.kind == ReportKind.TV) {
                aiSnapshot?.facts?.let { effect(ManageEffect.Copy(event.preferences.export(it))) }
            }
            is ReportIntent.PreviewAi -> if (after.kind == ReportKind.TV && !after.aiApplying) {
                reviewedText = event.text
                val snap = aiSnapshot
                if (snap != null) scope.launch { send(AiPreviewed(AiReview.parse(event.text, snap, aiProfile))) }
            }
            is ReportIntent.ApplyAi -> if (after.kind == ReportKind.TV && !after.aiApplying && !aiRunActive && after.aiPreview?.valid == true && event.selected.isNotEmpty()) {
                aiRunActive = true
                scope.launch { try { applyAi(event.selected) } finally { aiRunActive = false } }
            }
            else -> {}
        }
    }

    /** Explicitly opted-in AI suggestions: fresh live read, exact reparse, pre-write restore point, guarded ADB plan. */
    private suspend fun applyAi(selected: Set<PackageName>) {
        send(AiApplyUpdate(true, null))
        val result = attempt {
            val id = tvId ?: return@attempt "Nie wybrano telewizora."
            val tv = d.tv(id) ?: return@attempt "Nie znaleziono zapisanego telewizora."
            val text = reviewedText ?: return@attempt "Najpierw zweryfikuj odpowiedz AI."
            val session = (d.access.session(id) as? AccessResult.Ok)?.session
                ?: return@attempt "Telewizor jest offline. Polacz ponownie i sprawdz liste."
            val fresh = d.runner.freshSnapshot(session)
            val profile = (d.profiles.resolveFor(fresh.facts, fresh.home, fresh.packages) as? ProfileResolution.Exact)?.profile
            val checked = AiReview.parse(text, fresh, profile)
            if (!checked.valid || selected.any { it !in checked.packages })
                return@attempt "Lista lub stan TV zmienily sie. Przeprowadz analize AI ponownie."
            val baseline = d.snapshots.baseline(id)
            val refusal = RecoveryGate.refusal(baseline, fresh, selected.map { Op.Disable(it) })
            if (refusal != null) return@attempt refusal
            val dryPlan = (d.runner.plan(tv, PlanRequest.SwitchOff(selected), fresh) as? PlanResult.Ok)?.plan
                ?: return@attempt "Zabezpieczenia odmowily utworzenia planu. Nic nie zmieniono."
            if (dryPlan.steps.size != selected.size || dryPlan.steps.any { it.op !is Op.Disable } ||
                dryPlan.steps.map { (it.op as Op.Disable).pkg }.toSet() != selected)
                return@attempt "Plan zawiera inne operacje niz zatwierdzone pakiety. Przerwano."
            d.sessionEnd.touched(id)
            if (!d.restorePoint.writeVerified(tv, session, profile))
                return@attempt "Nie udalo sie zapisac punktu przywracania na TV. Niczego nie wylaczono."
            when (val write = TvWriter(d).write(id, PlanRequest.SwitchOff(selected), fresh = true)) {
                is WriteResult.Done -> if (write.finished.status == RunStatus.COMPLETED && write.finished.skipped.isEmpty())
                    return@attempt "OK: wylaczono ${selected.size} wybranych pakietow. Mozesz je przywrocic w Switched off."
                    else return@attempt "Czesc operacji zostala pominieta. Sprawdz status TV i liste wylaczonych."
                WriteResult.GateCancelled -> return@attempt "Anulowano. Niczego nie zmieniono."
                WriteResult.NoTv -> return@attempt "Utracono polaczenie z TV. Sprawdz stan przed ponowieniem."
                is WriteResult.Paused -> return@attempt "Plan wstrzymany. Wroc do aplikacji i uzyj mechanizmu przywracania."
                is WriteResult.Refused -> return@attempt "Zabezpieczenia odrzucily plan: ${write.reasons.joinToString("; ").take(280)}"
                else -> return@attempt "Nie potwierdzono wykonania planu. Odswiez stan telewizora."
            }
        }.getOrElse { "Nie mozna bezpiecznie ukonczyc zmian. Sprawdz polaczenie i stan TV." }
        send(AiApplyUpdate(false, result, result.startsWith("OK:")))
    }
    // 0.4.7: a TV report that left the phone (copied and opened, or shared) is remembered for the
    // TV, with today's date; the issue link it went to is kept once the person pastes it. Sending
    // again keeps the link and moves the date.
    private fun remember() {
        val id = tvId ?: return
        if (state.value.kind != ReportKind.TV) return
        scope.launch {
            val r = SentReport(d.clock.nowMs(), state.value.sent?.issueUrl)
            attempt { d.sentReports.save(id, r) }.onSuccess { send(Sent(r)) }
        }
    }

    // 0.4.8: a confirmation that left the phone is remembered with the count it carried, so Manage says so.
    private fun told() {
        val id = tvId ?: return
        if (state.value.kind != ReportKind.CONFIRM) return
        scope.launch { attempt { d.restarts.told(id, d.clock.nowMs()) } }
    }

    private suspend fun saveIssue(text: String) {
        val id = tvId ?: return
        val current = state.value.sent ?: return
        if (text.isBlank()) {
            val r = current.copy(issueUrl = null)
            attempt { d.sentReports.save(id, r) }.onSuccess { send(Sent(r)) }
            return
        }
        val url = SentReports.issueUrl(text) ?: return send(Sent(null, error = true))
        val r = current.copy(issueUrl = url)
        attempt { d.sentReports.save(id, r) }.onSuccess { send(Sent(r)) }
    }

    private suspend fun build(kind: ReportKind): Loaded = when (kind) {
        ReportKind.TV -> when (val input = ProblemReports.input(d, tvId)) {
            null -> Loaded("", "", available = false)
            else -> {
                val compact = TvReport.compact(input)
                val link = IssueLinks.tvSupport(input, compact)
                aiSnapshot = input.snapshot
                aiProfile = (input.profile as? ProfileResolution.Exact)?.profile
                Loaded(compact, link.url, true, link.reportInLink, share = TvReport.text(input), sent = tvId?.let { id -> attempt { d.sentReports.load(id) }.getOrNull() },
                    aiPrompt = AiReview.prompt(input.snapshot), aiExample = AiReview.example(input.snapshot),
                    toolkitProfileName = aiProfile?.id)
            }
        }
        // 0.4.8: the confirmation block (TvReport.confirmation) in the confirmation form; none before a restart counted.
        ReportKind.CONFIRM -> {
            val input = ProblemReports.input(d, tvId)
            val facts = input?.restarts?.takeIf { it.tally.held > 0 }
            if (input == null || facts == null) Loaded("", "", available = false) else {
                val text = TvReport.confirmation(d.about, input, facts)
                val link = IssueLinks.confirmation(input, text)
                Loaded(text, link.url, true, link.reportInLink, share = text)
            }
        }
        // Settings asks without a TV: the TV used last, when there is one (ProblemReports.build).
        // 0.4.6: the link always holds the report, shortened to fit when it must.
        ReportKind.PROBLEM -> {
            val r = ProblemReports.build(d, tvId, null)
            Loaded(r.full, r.link.url, true, r.link.reportInLink, share = r.full, shortened = r.shortened)
        }
    }

    companion object {
        // The log can hold a few megabytes (five 1 MB files); a GitHub issue takes 65,536 characters
        // and the clipboard and the share sheet about a megabyte. The report keeps the log's first line
        // (the app and the phone) and its newest lines, at most MAX_LOG_LINES and MAX_LOG_CHARS.
        const val MAX_LOG_LINES = 300
        const val MAX_LOG_CHARS = 40_000

        fun tail(log: String): String {
            val lines = log.trimEnd().lines()
            if (lines.size <= 1) return log.trimEnd()
            val head = lines.first()
            val kept = ArrayDeque<String>()
            var chars = head.length
            for (line in lines.drop(1).asReversed()) {
                if (kept.size >= MAX_LOG_LINES || chars + line.length + 1 > MAX_LOG_CHARS) break
                kept.addFirst(line)
                chars += line.length + 1
            }
            val dropped = lines.size - 1 - kept.size
            return (listOf(head) + (if (dropped > 0) listOf("($dropped older lines left out)") else emptyList()) + kept).joinToString("\n")
        }
    }
}
