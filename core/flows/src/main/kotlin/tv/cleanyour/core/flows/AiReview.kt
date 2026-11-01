package tv.cleanyour.core.flows

import tv.cleanyour.core.model.EnabledState
import tv.cleanyour.core.model.PACKAGE_RE
import tv.cleanyour.core.model.PackageName
import tv.cleanyour.core.model.Profile
import tv.cleanyour.core.model.Snapshot
import tv.cleanyour.core.ops.Denylist

/**
 * The AI never gets an ADB session. This is a deliberately narrow text interchange: the phone
 * exports package *names*, and only package *names* can come back. No shell text is executed.
 * Import is a preview, NOT an execution plan. The normal planner and live guards remain mandatory.
 */
object AiReview {
    const val HEADER = "TV_TOOLKIT_PLAN_V1"
    const val END = "END_TV_TOOLKIT_PLAN"
    private const val MAX_RESPONSE = 20_000
    private const val MAX_PACKAGES = 150

    data class Preview(val packages: List<PackageName>, val problems: List<String>) {
        val valid: Boolean get() = problems.isEmpty() && packages.isNotEmpty()
    }

    /** Only a voluntary clipboard copy, never an automatic submission to a model or a server. */
    fun prompt(snapshot: Snapshot): String = buildString {
        val brand = identity(snapshot.facts.brand)
        val model = identity(snapshot.facts.model)
        appendLine("Przeanalizuj ostroznie pakiety Android TV / Google TV. Nie wydawaj komend ADB.")
        appendLine("Nie proponuj wylaczania launchera, konfiguracji pierwszego uruchomienia, pilota,")
        appendLine("klawiatury, Wi-Fi, Bluetooth, wejsc HDMI, tunera, Cast, aktualizacji systemu,")
        appendLine("ulatwien dostepu, Google Play, uslug systemowych ani aplikacji multimedialnych.")
        appendLine("Gdy nie masz pewnosci co do pakietu, pomin go. Nie zakladaj, ze dwa modele tej")
        appendLine("samej marki maja identyczne uslugi. Zasugeruj tylko oczywisty bloat.")
        appendLine()
        appendLine("Marka: $brand")
        appendLine("Model: $model")
        appendLine("Android: ${identity(snapshot.facts.release)} (API ${snapshot.facts.sdk})")
        appendLine("Identyfikator platformy: ${identity(snapshot.facts.device)}")
        appendLine()
        appendLine("Pakiety na urzadzeniu (S = system, U = uzytkownika; ON / OFF):")
        snapshot.packages.entries.sortedBy { it.key.value }.forEach { (pkg, state) ->
            val status = if (state.enabled == EnabledState.DISABLED || state.enabled == EnabledState.DISABLED_USER) "OFF" else "ON"
            appendLine("${if (state.system) "S" else "U"} $status ${pkg.value}")
        }
        appendLine()
        appendLine("ZWROC TYLKO ponizszy format, bez markdown i bez komentarzy:")
        appendLine(HEADER)
        appendLine("BRAND=$brand")
        appendLine("MODEL=$model")
        appendLine("DISABLE=com.przyklad.zbedny")
        appendLine("DISABLE=com.drugi.zbedny")
        appendLine(END)
        appendLine("Zastap wiersze DISABLE rzeczywistymi nazwami z powyzszej listy.")
        appendLine("Jesli nie ma bezpiecznych kandydatow, nie dodawaj zadnego wiersza DISABLE.")
        appendLine("Nie dodawaj ENABLE, UNINSTALL, PM, SHELL ani innych polecen.")
        appendLine("Aplikacja ponownie sprawdzi liste lokalnie. Odpowiedz AI nie jest autoryzacja zmian.")
    }

    fun parse(text: String, snapshot: Snapshot, profile: Profile? = null): Preview {
        if (text.length > MAX_RESPONSE) return Preview(emptyList(), listOf("Odpowiedz jest za dluga."))
        val raw = text.trim().lines().map { it.trim() }.filter { it.isNotEmpty() }
        val lines = if (raw.size >= 2 && raw.first().startsWith("```") && raw.last() == "```") raw.drop(1).dropLast(1) else raw
        if (lines.firstOrNull() != HEADER || lines.lastOrNull() != END) {
            return Preview(emptyList(), listOf("Brak dokladnego naglowka $HEADER lub zakonczenia $END."))
        }
        val problems = mutableListOf<String>()
        var brand: String? = null
        var model: String? = null
        val requested = linkedSetOf<String>()
        for (line in lines.drop(1).dropLast(1)) {
            when {
                line.startsWith("BRAND=") -> if (brand != null) problems += "Powtorzona marka." else brand = line.removePrefix("BRAND=")
                line.startsWith("MODEL=") -> if (model != null) problems += "Powtorzony model." else model = line.removePrefix("MODEL=")
                line.startsWith("DISABLE=") -> {
                    val value = line.removePrefix("DISABLE=")
                    if (!PACKAGE_RE.matches(value) || value.length > 255) problems += "Niepoprawna nazwa pakietu: ${value.take(80)}"
                    else if (!requested.add(value)) problems += "Zduplikowany pakiet: $value"
                }
                else -> problems += "Niedozwolony wiersz: ${line.take(80)}"
            }
            if (problems.size >= 25) break
        }
        if (!brand.equals(identity(snapshot.facts.brand), ignoreCase = true) ||
            !model.equals(identity(snapshot.facts.model), ignoreCase = true)) problems += "Marka lub model nie zgadza sie z polaczonym telewizorem."
        if (requested.size > MAX_PACKAGES) problems += "Zbyt wiele pakietow w jednej odpowiedzi."
        val allowed = mutableListOf<PackageName>()
        requested.take(MAX_PACKAGES).forEach { name ->
            val pkg = PackageName(name)
            val state = snapshot.packages[pkg]
            val reason = when {
                state == null -> "pakiet nie istnieje na tym telewizorze"
                state.enabled == EnabledState.DISABLED || state.enabled == EnabledState.DISABLED_USER -> "pakiet jest juz wylaczony"
                pkg == snapshot.home.resolved?.pkg || snapshot.home.claimants.any { it.component.pkg == pkg } -> "pakiet moze obslugiwac ekran glowny"
                pkg.value in neverAiDisable -> "pakiet wymaga oddzielnej procedury lub zgody"
                else -> Denylist.reason(pkg, state, profile, snapshot)
            }
            if (reason != null) problems += "$name: $reason" else allowed += pkg
        }
        // All-or-nothing. The caller may display allowed candidates, but must not execute a partial
        // list when a model mismatch, malformed line or protected package is also present.
        return Preview(allowed, problems)
    }

    private fun identity(value: String): String = value.replace(Regex("[^A-Za-z0-9 _+./-]"), " ").trim().take(100)

    // Never let text generated by AI opt the user out of these functions. An explicit, guarded
    // wizard may offer selected optional features, but it cannot reuse AI-import authority.
    private val neverAiDisable = setOf(
        "com.google.android.tvlauncher", "com.google.android.apps.tv.launcherx",
        "com.google.android.tungsten.setupwraith", "com.google.android.apps.mediashell",
        "com.google.android.youtube.tv", "com.google.android.youtube.tvmusic",
        "org.droidtv.oad", "com.easy.oad", "org.droidtv.dlna",
        "com.google.android.katniss", "com.google.android.backdrop",
    )
}
