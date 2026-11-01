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
    const val HEADER = "TV_TOOLKIT_PLAN_V2"
    const val END = "END_TV_TOOLKIT_PLAN"
    private const val MAX_RESPONSE = 20_000
    private const val MAX_PACKAGES = 30
    private const val MAX_KEEP = 40

    data class Preview(
        val packages: List<PackageName>, val problems: List<String>,
        val why: Map<PackageName, String> = emptyMap(),
        val keep: Map<PackageName, String> = emptyMap(),
        val noCandidates: Boolean = false,
    ) {
        val valid: Boolean get() = problems.isEmpty() && (packages.isNotEmpty() || noCandidates)
    }

    /** No serial, network address, Wi-Fi name, token or ADB key leaves the phone in this text. */
    fun prompt(snapshot: Snapshot): String = buildString {
        val facts = snapshot.facts
        val brand = identity(facts.brand)
        val model = identity(facts.model)
        val device = identity(facts.device)
        val buildId = buildId(snapshot)
        val home = snapshot.home.resolved?.pkg?.value ?: "UNRESOLVED"
        appendLine("AUDYT BEZPIECZENSTWA ANDROID TV / GOOGLE TV / PRZYSTAWKI — WYLACZNIE ANALIZA.")
        appendLine("Oceniaj TYLKO wymienione pakiety na konkretnym modelu i wersji Androida.")
        appendLine("Jesli nie mozesz potwierdzic przeznaczenia pakietu, oznacz go KEEP albo pomin.")
        appendLine("Nie zgaduj na podstawie samej marki. Zakladaj, ze dekoder operatora moze")
        appendLine("wymagac swoich aplikacji do logowania, DRM, kanalow, pilota, tunera i pilota glosowego.")
        appendLine()
        appendLine("BEZWZGLEDNIE ZACHOWAJ: aktywny i zapasowy Home, SystemUI, Settings,")
        appendLine("PackageInstaller, PermissionController, uslugi Google/Play, DRM/Widevine,")
        appendLine("OTA/aktualizacje, konfiguracje pierwszego uruchomienia, pilot/BT/Wi-Fi,")
        appendLine("wejscia HDMI, tuner/LiveTV, Cast, audio/wideo, konta operatora,")
        appendLine("aplikacje do streamingu, uzywane launchery i nieznane komponenty producenta.")
        appendLine("Nie proponuj odinstalowania, usuwania danych, roota ani zadnych komend ADB.")
        appendLine("Pakiet juz OFF pozostaw w spokoju, NIE sugeruj jego ponownego wylaczenia.")
        appendLine("Do DISABLE wybieraj jedynie niezalezne i zbedne pozycje o wysokiej pewnosci;")
        appendLine("maksymalnie 30. Brak kandydatow jest poprawnym, bezpiecznym wynikiem.")
        appendLine()
        appendLine("DANE TELEWIZORA (skopiuj je bez zmian do odpowiedzi):")
        appendLine("BRAND=$brand")
        appendLine("MODEL=$model")
        appendLine("DEVICE=$device")
        appendLine("SDK=${facts.sdk}")
        appendLine("BUILD_SHA256=$buildId")
        appendLine("Producent: ${identity(facts.manufacturer)}")
        appendLine("Android: ${identity(facts.release)}")
        appendLine("Aktywny Home: $home")
        appendLine("Pozostale odbiorniki Home: ${snapshot.home.claimants.map { it.component.pkg.value }.distinct().joinToString(", ")}")
        appendLine()
        appendLine("INWENTARYZACJA: S=system, U=uzytkownika, ON=wlaczony, OFF=juz wylaczony:")
        snapshot.packages.entries.sortedBy { it.key.value }.forEach { (pkg, state) ->
            val status = if (state.enabled == EnabledState.DISABLED || state.enabled == EnabledState.DISABLED_USER) "OFF" else "ON"
            appendLine("${if (state.system) "S" else "U"} $status ${pkg.value}")
        }
        appendLine()
        appendLine("ODPOWIEDZ TYLKO w PONIZSZYM FORMACIE: bez Markdown, numeracji, opisow")
        appendLine("poza wierszami i bez blokow kodu. Kazdy wiersz na osobnej linii.")
        appendLine("Identyfikatory BRAND/MODEL/DEVICE/SDK/BUILD_SHA256 przepisz DOSLOWNIE.")
        appendLine("Wiersze DISABLE i KEEP zawieraja dokladna nazwe z inwentaryzacji, znak |")
        appendLine("i krotki, rzeczowy powod. Powod nie moze zawierac znaku |.")
        appendLine("KEEP to przyklady waznych pakietow, ktorych nie wolno wylaczac.")
        appendLine("Kazdy DISABLE bedzie osobno zweryfikowany w aplikacji, a potem")
        appendLine("wymagal zatwierdzenia przez uzytkownika i kopii przywracania.")
        appendLine()
        appendLine(HEADER)
        appendLine("BRAND=$brand")
        appendLine("MODEL=$model")
        appendLine("DEVICE=$device")
        appendLine("SDK=${facts.sdk}")
        appendLine("BUILD_SHA256=$buildId")
        appendLine("DISABLE=com.przyklad.zbedna_aplikacja|Samodzielna aplikacja promocyjna")
        appendLine("KEEP=com.przyklad.usluga_systemowa|Wymagana do dzialania systemu")
        appendLine(END)
        appendLine()
        appendLine("Powyzej tylko PRZYKLAD skladni: zastap oba przykladowe pakiety rzeczywistymi")
        appendLine("nazwami z listy. Gdy NIE MA bezpiecznych kandydatow, zamiast DISABLE")
        appendLine("wpisz wiersz NONE=YES. Mozesz dodac do 40 wierszy KEEP z powodami.")
        appendLine("Nigdy nie dodawaj ENABLE, UNINSTALL, PM, SHELL ani polecen systemowych.")
    }

    /** A form sample with this exact device identity, so a person need not edit fake TV names. */
    fun example(snapshot: Snapshot): String = listOf(
        HEADER,
        "BRAND=${identity(snapshot.facts.brand)}",
        "MODEL=${identity(snapshot.facts.model)}",
        "DEVICE=${identity(snapshot.facts.device)}",
        "SDK=${snapshot.facts.sdk}",
        "BUILD_SHA256=${buildId(snapshot)}",
        "DISABLE=com.example.optional|A confirmed optional app",
        "KEEP=com.example.critical|A required service",
        END,
    ).joinToString("\n")

    fun parse(text: String, snapshot: Snapshot, profile: Profile? = null): Preview {
        if (text.length > MAX_RESPONSE) return Preview(emptyList(), listOf("Odpowiedz jest za dluga."))
        val raw = text.trim().lines().map { it.trim() }.filter { it.isNotEmpty() }
        val lines = if (raw.size >= 2 && raw.first().startsWith("```") && raw.last() == "```") raw.drop(1).dropLast(1) else raw
        if (lines.firstOrNull() != HEADER || lines.lastOrNull() != END) {
            return Preview(emptyList(), listOf("Brak dokladnego naglowka $HEADER lub zakonczenia $END. Skopiuj NOWY prompt."))
        }
        val problems = mutableListOf<String>()
        val fields = mutableMapOf<String, String>()
        val requested = linkedMapOf<String, String>()
        val kept = linkedMapOf<String, String>()
        var none = false
        for (line in lines.drop(1).dropLast(1)) {
            val name = line.substringBefore("=")
            val value = line.substringAfter("=", missingDelimiterValue = "")
            when (name) {
                "BRAND", "MODEL", "DEVICE", "SDK", "BUILD_SHA256" ->
                    if (name in fields) problems += "Powtorzone pole $name." else fields[name] = value
                "DISABLE", "KEEP" -> {
                    val pkg = value.substringBefore("|")
                    val why = value.substringAfter("|", missingDelimiterValue = "")
                    if (!PACKAGE_RE.matches(pkg) || pkg.length > 255 || why.length !in 8..180 || "|" in why) {
                        problems += "Niepoprawny wiersz $name: ${value.take(80)}"
                    } else {
                        val target = if (name == "DISABLE") requested else kept
                        if (target.putIfAbsent(pkg, why) != null) problems += "Zduplikowany pakiet $pkg ($name)."
                    }
                }
                "NONE" -> if (none || value != "YES") problems += "Niedozwolone NONE." else none = true
                else -> problems += "Niedozwolony wiersz: ${line.take(80)}"
            }
            if (problems.size >= 25) break
        }
        val expected = mapOf(
            "BRAND" to identity(snapshot.facts.brand),
            "MODEL" to identity(snapshot.facts.model),
            "DEVICE" to identity(snapshot.facts.device),
            "SDK" to snapshot.facts.sdk.toString(),
            "BUILD_SHA256" to buildId(snapshot),
        )
        expected.forEach { (key, value) ->
            if (!fields[key].equals(value, ignoreCase = key in setOf("BRAND", "MODEL", "DEVICE"))) {
                problems += "$key nie zgadza sie z polaczonym telewizorem lub aktualnym firmware."
            }
        }
        if (requested.isNotEmpty() && none) problems += "NONE=YES nie moze wystepowac razem z DISABLE."
        if (requested.isEmpty() && !none) problems += "Wpisz przynajmniej jedno DISABLE albo NONE=YES."
        if (requested.size > MAX_PACKAGES) problems += "Zbyt wiele propozycji DISABLE (maks. $MAX_PACKAGES)."
        if (kept.size > MAX_KEEP) problems += "Zbyt wiele wpisow KEEP (maks. $MAX_KEEP)."
        if ((requested.keys intersect kept.keys).isNotEmpty()) problems += "Ten sam pakiet jest w DISABLE i KEEP."
        val allowed = mutableListOf<PackageName>()
        requested.keys.take(MAX_PACKAGES).forEach { name ->
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
        kept.keys.take(MAX_KEEP).forEach { name ->
            if (!PACKAGE_RE.matches(name) || PackageName(name) !in snapshot.packages) problems += "KEEP odnosi sie do nieobecnego pakietu $name."
        }
        return Preview(allowed, problems, requested.mapKeys { PackageName(it.key) },
            kept.mapKeys { PackageName(it.key) }, noCandidates = none)
    }

    private fun buildId(snapshot: Snapshot): String = sha256Of(snapshot.facts.fingerprint.toByteArray(Charsets.UTF_8)).hex
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
