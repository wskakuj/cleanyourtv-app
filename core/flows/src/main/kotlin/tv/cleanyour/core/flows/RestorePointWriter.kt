package tv.cleanyour.core.flows

import java.io.ByteArrayInputStream
import java.security.MessageDigest
import tv.cleanyour.core.backup.TvRestoreFiles
import tv.cleanyour.core.engine.RunStatus
import tv.cleanyour.core.model.BlobRef
import tv.cleanyour.core.model.FileRoute
import tv.cleanyour.core.model.Op
import tv.cleanyour.core.model.RemoteDir
import tv.cleanyour.core.model.RemotePath
import tv.cleanyour.core.model.Profile
import tv.cleanyour.core.model.Sha256
import tv.cleanyour.core.model.Tv
import tv.cleanyour.core.planner.PlanRequest
import tv.cleanyour.core.planner.PlanResult
import tv.cleanyour.core.session.TvSession
import tv.cleanyour.core.store.BlobStore

fun sha256Of(bytes: ByteArray): Sha256 =
    Sha256(MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) })

suspend fun BlobStore.putBytes(bytes: ByteArray): BlobRef = put(sha256Of(bytes), ByteArrayInputStream(bytes), bytes.size.toLong())

// D44: after every plan, restore-point.json and restore.txt (W6's bytes) are pushed to
// /sdcard/cleanyourtv/ by a WriteRestorePoint plan (R0: 11). Best effort: a failure here never
// turns a finished plan into a stop; the phone's own restore point is the one that matters.
class RestorePointWriter(
    private val files: TvRestoreFiles,
    private val blobs: BlobStore,
    private val ledger: RunLedger,
    private val runner: PlanRunner,
) {
    // Strict pre-debloat version for the CANAL+ operator box: no skipped transfers and
    // no unexpected TV operations. A best-effort post-run restore point is not enough here.
    suspend fun writeVerified(tv: Tv, session: TvSession, profile: Profile?): Boolean = attempt {
        val changes = ledger.changes(tv.id)
        val json = blobs.putBytes(files.restorePointJson(tv, changes))
        val txt = blobs.putBytes(files.restoreTxt(tv, changes, profile))
        val snapshot = runner.snapshotFor(tv, session)
        val plan = (runner.plan(tv, PlanRequest.WriteRestorePoint(json, txt), snapshot) as? PlanResult.Ok)?.plan
            ?: return@attempt false
        val expected = mapOf(
            RemotePath(RemoteDir.RESTORE, listOf("restore-point.json")) to json,
            RemotePath(RemoteDir.RESTORE, listOf("restore.txt")) to txt,
        )
        val safe = plan.steps.size == 3 && plan.steps.count { it.op is Op.Mkdir } == 1 &&
            plan.steps.count { it.op is Op.PushAsset } == 2 &&
            plan.steps.all { step -> when (val op = step.op) {
                is Op.Mkdir -> op.dir == RemoteDir.RESTORE
                is Op.PushAsset -> expected[op.dest] == op.src &&
                    op.sha256 == op.src.sha256 && op.route == FileRoute.SYNC
                else -> false
            } }
        if (!safe) return@attempt false
        val finished = (runner.run(tv, plan, session) as? RunOutcome.Finished)?.state
            ?: return@attempt false
        finished.status == RunStatus.COMPLETED && finished.skipped.isEmpty()
    }.getOrDefault(false)

    suspend fun write(tv: Tv, session: TvSession, profile: Profile?): Boolean = attempt {
        val changes = ledger.changes(tv.id)
        val json = blobs.putBytes(files.restorePointJson(tv, changes))
        val txt = blobs.putBytes(files.restoreTxt(tv, changes, profile))
        val snapshot = runner.snapshotFor(tv, session)
        val plan = (runner.plan(tv, PlanRequest.WriteRestorePoint(json, txt), snapshot) as? PlanResult.Ok)?.plan ?: return@attempt false
        runner.run(tv, plan, session) is RunOutcome.Finished
    }.getOrDefault(false)
}
