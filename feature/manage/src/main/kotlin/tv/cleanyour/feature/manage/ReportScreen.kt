package tv.cleanyour.feature.manage

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tv.cleanyour.core.flows.ReportIntent
import tv.cleanyour.core.flows.ReportKind
import tv.cleanyour.core.flows.SentReports
import tv.cleanyour.core.flows.ToolkitLauncher
import tv.cleanyour.core.flows.ToolkitMedia
import tv.cleanyour.core.flows.ToolkitPreferences
import tv.cleanyour.core.flows.ToolkitWallpaper
import tv.cleanyour.core.flows.ToolkitYouTube
import tv.cleanyour.ui.design.component.CytTextField
import tv.cleanyour.ui.design.component.LinkRow
import tv.cleanyour.ui.design.component.TertiaryButton
import tv.cleanyour.core.flows.ReportState
import tv.cleanyour.ui.design.component.ActionColumn
import tv.cleanyour.ui.design.component.Banner
import tv.cleanyour.ui.design.component.BannerTone
import tv.cleanyour.ui.design.component.CytTopBar
import tv.cleanyour.ui.design.component.Gap
import tv.cleanyour.ui.design.component.Lead
import tv.cleanyour.ui.design.component.MonospaceBlock
import tv.cleanyour.ui.design.component.PrimaryButton
import tv.cleanyour.ui.design.component.ScreenScaffold
import tv.cleanyour.ui.design.component.ScreenTitle
import tv.cleanyour.ui.design.component.CytBottomSheet
import tv.cleanyour.ui.design.component.SheetHeader
import tv.cleanyour.ui.design.component.SwitchRow
import tv.cleanyour.ui.design.component.SecondaryButton
import tv.cleanyour.ui.design.icon.CytIcons
import tv.cleanyour.ui.design.theme.Glow

// 0.3.2 B1 and B2: the report preview, "Help add this TV" and "Report a problem". It shows exactly
// the text that would be copied or shared, says plainly that a GitHub post is public and that the
// app sends nothing itself, then offers the two ways out: copy it and open the GitHub form (the
// browser opens the link; the app makes no request), or the share sheet. The same in both builds.
// 0.3.3 D1: when the report fits in the link the form opens with it filled in and the button says
// "Open GitHub"; when it does not, a line above the report says so before GitHub opens, and the
// person pastes it as in 0.3.2. 0.4.6: a problem report always fits (ProblemReports.fit).
// 0.4.7 (must 4): a TV report sent before says when, and keeps the issue's link once pasted, so the
// same TV is not reported twice by mistake; sending again stays allowed.
@Composable
fun ReportScreen(state: ReportState, onIntent: (ReportIntent) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val tvReport = state.kind == ReportKind.TV
    val confirm = state.kind == ReportKind.CONFIRM   // 0.4.8: "Tell us it worked"
    val ready = !state.loading && state.available
    var aiSelected by remember { mutableStateOf(setOf<String>()) }
    var aiConfirm by remember { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    ScreenScaffold(
        Glow.Tv, modifier,
        topBar = { CytTopBar(onBack = onBack) },
        bottomBar = if (!ready || (tvReport && state.aiPrompt.isNotEmpty())) null else ({
            ActionColumn {
                // Neither button writes to the TV: copying, the browser and the share sheet are the phone's.
                PrimaryButton(stringResource(if (state.inLink) R.string.report_open else R.string.report_copy_open), { onIntent(ReportIntent.CopyAndOpen) }, guardObscured = false)
                SecondaryButton(stringResource(R.string.report_share), { onIntent(ReportIntent.Share) })
            }
        }),
    ) {
        ScreenTitle(stringResource(when { confirm -> R.string.report_confirm_title; tvReport && state.aiPrompt.isNotEmpty() -> R.string.tvtoolkit_ai_title; tvReport -> R.string.report_tv_title; else -> R.string.report_problem_title }), Modifier.padding(top = 20.dp))
        when {
            state.loading -> LiveLine(stringResource(R.string.report_loading))
            !state.available -> Lead(stringResource(if (confirm) R.string.report_confirm_unavailable else R.string.report_unavailable), Modifier.padding(top = 12.dp))
            else -> {
                Lead(stringResource(when { confirm -> R.string.report_confirm_body; tvReport && state.aiPrompt.isNotEmpty() -> R.string.tvtoolkit_manage_ai_sub; tvReport -> R.string.report_tv_body; else -> R.string.report_problem_body }), Modifier.padding(top = 12.dp))
                // The universal toolkit is the main action: keep the legacy GitHub issue report below it.
                if (tvReport && state.aiPrompt.isNotEmpty()) {
                    Gap(20.dp)
                    state.toolkitProfileName?.let { name ->
                        Banner("${stringResource(R.string.tvtoolkit_matched_profile)}: $name. ${stringResource(R.string.tvtoolkit_profile_caution)}",
                            BannerTone.INFO, Modifier.padding(bottom = 12.dp), icon = CytIcons.Info)
                    }
                    SectionLabel(stringResource(R.string.tvtoolkit_ai_title))
                    Body(stringResource(R.string.tvtoolkit_ai_intro), Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp))
                    Banner(stringResource(R.string.tvtoolkit_ai_privacy), BannerTone.INFO, Modifier.padding(top = 12.dp), icon = CytIcons.Info)
                    SecondaryButton(stringResource(R.string.tvtoolkit_ai_copy_prompt), { onIntent(ReportIntent.CopyAiPrompt) }, Modifier.padding(top = 12.dp))
                    Body(stringResource(R.string.tvtoolkit_ai_format), Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp))
                    MonospaceBlock(state.aiExample.lines(), wrap = true)
                    var aiAnswer by rememberSaveable { mutableStateOf("") }
                    CytTextField(aiAnswer, { aiAnswer = it.take(20_000); aiSelected = emptySet() }, stringResource(R.string.tvtoolkit_ai_paste), Modifier.padding(top = 12.dp),
                        singleLine = false, supporting = stringResource(R.string.tvtoolkit_ai_only_preview))
                    SecondaryButton(stringResource(R.string.tvtoolkit_ai_check), { aiSelected = emptySet(); onIntent(ReportIntent.PreviewAi(aiAnswer)) }, Modifier.padding(top = 8.dp),
                        enabled = aiAnswer.isNotBlank())
                    state.aiPreview?.let { preview ->
                        Banner(stringResource(if (preview.valid) R.string.tvtoolkit_ai_valid else R.string.tvtoolkit_ai_invalid),
                            if (preview.valid) BannerTone.SUCCESS else BannerTone.INFO, Modifier.padding(top = 12.dp))
                        if (preview.noCandidates && preview.valid) Body(stringResource(R.string.tvtoolkit_ai_none), Modifier.padding(top = 8.dp))
                        if (preview.packages.isNotEmpty()) {
                            Body(stringResource(R.string.tvtoolkit_ai_choose), Modifier.padding(top = 10.dp))
                            preview.packages.forEach { pkg ->
                                SwitchRow(pkg.value, pkg.value in aiSelected && !state.aiApplied,
                                    { selected -> aiSelected = if (selected) aiSelected + pkg.value else aiSelected - pkg.value },
                                    subtitle = preview.why[pkg], enabled = !state.aiApplying && !state.aiApplied)
                            }
                            if (aiSelected.isNotEmpty() && preview.valid && !state.aiApplied) {
                                PrimaryButton(stringResource(R.string.tvtoolkit_ai_apply), { aiConfirm = true },
                                    Modifier.padding(top = 16.dp), enabled = !state.aiApplying)
                            }
                        }
                        if (preview.keep.isNotEmpty()) {
                            Body(stringResource(R.string.tvtoolkit_ai_keep), Modifier.padding(top = 12.dp))
                            MonospaceBlock(preview.keep.map { (pkg, why) -> "${pkg.value} — $why" }, wrap = true)
                        }
                        if (preview.problems.isNotEmpty()) MonospaceBlock(preview.problems.take(25), wrap = true)
                    }
                    if (state.aiApplying) LiveLine(stringResource(R.string.tvtoolkit_ai_working))
                    state.aiOutcome?.let { Banner(it, if (state.aiApplied) BannerTone.SUCCESS else BannerTone.INFO, Modifier.padding(top = 12.dp)) }
                    Gap(20.dp)
                    SectionLabel(stringResource(R.string.tvtoolkit_choices_title))
                    Body(stringResource(R.string.tvtoolkit_choices_intro), Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp))
                    var launcher by rememberSaveable { mutableStateOf(0) }
                    var youtube by rememberSaveable { mutableStateOf(0) }
                    var media by rememberSaveable { mutableStateOf(0) }
                    var wallpaper by rememberSaveable { mutableStateOf(0) }
                    val launcherLabels = listOf(stringResource(R.string.tvtoolkit_keep_stock),
                        stringResource(R.string.tvtoolkit_flauncher), stringResource(R.string.tvtoolkit_projectivy))
                    val youtubeLabels = listOf(stringResource(R.string.tvtoolkit_keep_stock),
                        stringResource(R.string.tvtoolkit_youtube_alternative))
                    val mediaLabels = listOf(stringResource(R.string.tvtoolkit_none),
                        stringResource(R.string.tvtoolkit_plex), stringResource(R.string.tvtoolkit_wholphin),
                        stringResource(R.string.tvtoolkit_both))
                    val wallpaperLabels = listOf(stringResource(R.string.tvtoolkit_keep_stock),
                        stringResource(R.string.tvtoolkit_aurora), stringResource(R.string.tvtoolkit_graphite))
                    SecondaryButton("${stringResource(R.string.tvtoolkit_launcher)}: ${launcherLabels[launcher]}",
                        { launcher = (launcher + 1) % launcherLabels.size }, Modifier.padding(top = 12.dp))
                    SecondaryButton("${stringResource(R.string.tvtoolkit_youtube)}: ${youtubeLabels[youtube]}",
                        { youtube = (youtube + 1) % youtubeLabels.size }, Modifier.padding(top = 8.dp))
                    SecondaryButton("${stringResource(R.string.tvtoolkit_media)}: ${mediaLabels[media]}",
                        { media = (media + 1) % mediaLabels.size }, Modifier.padding(top = 8.dp))
                    SecondaryButton("${stringResource(R.string.tvtoolkit_wallpaper)}: ${wallpaperLabels[wallpaper]}",
                        { wallpaper = (wallpaper + 1) % wallpaperLabels.size }, Modifier.padding(top = 8.dp))
                    Banner(stringResource(R.string.tvtoolkit_choices_preview_only), BannerTone.INFO,
                        Modifier.padding(top = 12.dp), icon = CytIcons.Info)
                    SecondaryButton(stringResource(R.string.tvtoolkit_choices_copy), {
                        onIntent(ReportIntent.CopyToolkitPreferences(ToolkitPreferences(
                            ToolkitLauncher.entries[launcher], ToolkitYouTube.entries[youtube],
                            ToolkitMedia.entries[media], ToolkitWallpaper.entries[wallpaper],
                        )))
                    }, Modifier.padding(top = 12.dp))
                }

                if (tvReport && state.aiPrompt.isNotEmpty()) SectionLabel(stringResource(R.string.tvtoolkit_report_technical))
                val sent = state.sent
                if (tvReport && sent != null) {
                    val issue = sent.issueUrl?.let { SentReports.issueNumber(it) }
                    Banner(
                        if (issue != null) msg(R.string.report_sent_issue, "issue" to issue) + "\n" + msg(R.string.report_sent_line, "date" to shortDate(sent.atMs, locale))
                        else msg(R.string.report_sent_line, "date" to shortDate(sent.atMs, locale)),
                        BannerTone.SUCCESS, Modifier.padding(top = 16.dp),
                    )
                    if (issue != null) LinkRow(stringResource(R.string.report_issue_open), { onIntent(ReportIntent.OpenIssue) }, Modifier.padding(top = 4.dp))
                    else {
                        var link by rememberSaveable { mutableStateOf("") }
                        CytTextField(link, { link = it.take(200) }, stringResource(R.string.report_issue_label), Modifier.padding(top = 12.dp),
                            error = if (state.issueError) stringResource(R.string.report_issue_error) else null, supporting = stringResource(R.string.report_issue_hint))
                        if (link.isNotBlank()) TertiaryButton(stringResource(R.string.report_issue_save), { onIntent(ReportIntent.SaveIssue(link)) }, Modifier.padding(top = 4.dp))
                    }
                }
                Banner(stringResource(R.string.report_public), BannerTone.INFO, Modifier.padding(top = 16.dp), icon = CytIcons.Info)
                // Before the report, so it is read before GitHub opens however long the report is.
                if (!state.inLink) Body(stringResource(R.string.report_too_long), Modifier.padding(top = 16.dp, start = 4.dp, end = 4.dp))
                SectionLabel(stringResource(if (confirm) R.string.report_confirm_label else R.string.report_preview_label))
                // Wrapped and one text node: no sideways scrolling on a phone, and TalkBack reads it as one block.
                MonospaceBlock(state.text.trimEnd().lines(), wrap = true)
                // In the scrolling content, not the bottom bar, so at large font sizes the report keeps the room.
                // 0.4.6: a long problem report goes into the form shorter (its newest log lines), so it says so.
                val hint = when {
                    !state.inLink -> R.string.report_paste_hint
                    state.shortened -> R.string.report_in_link_short_hint
                    else -> R.string.report_in_link_hint
                }
                Body(stringResource(hint), Modifier.padding(top = 16.dp, start = 4.dp, end = 4.dp))
                if (tvReport && state.aiPrompt.isNotEmpty()) {
                    SecondaryButton(stringResource(if (state.inLink) R.string.report_open else R.string.report_copy_open),
                        { onIntent(ReportIntent.CopyAndOpen) }, Modifier.padding(top = 16.dp))
                    TertiaryButton(stringResource(R.string.report_share), { onIntent(ReportIntent.Share) }, Modifier.padding(top = 8.dp))
                }
                Gap(16.dp)
            }
        }
    }
    if (aiConfirm && state.aiPreview?.valid == true && aiSelected.isNotEmpty()) {
        CytBottomSheet({ aiConfirm = false }) {
            SheetHeader(stringResource(R.string.tvtoolkit_ai_confirm_title))
            Body(stringResource(R.string.tvtoolkit_ai_confirm_body), Modifier.padding(top = 12.dp))
            MonospaceBlock(aiSelected.sorted(), wrap = true)
            ActionColumn(Modifier.padding(top = 20.dp)) {
                PrimaryButton(stringResource(R.string.tvtoolkit_ai_confirm_button), {
                    aiConfirm = false
                    onIntent(ReportIntent.ApplyAi(aiSelected.map { tv.cleanyour.core.model.PackageName(it) }.toSet()))
                }, enabled = !state.aiApplying)
                TertiaryButton(stringResource(R.string.action_cancel), { aiConfirm = false })
            }
        }
    }
}
