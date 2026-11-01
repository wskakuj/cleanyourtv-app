# TV Toolkit (wersja rozwojowa)

Własny wariant mobilnej aplikacji Clean Your TV, rozwijany na podstawie kodu na licencji MIT: https://github.com/udhaykumarbala/cleanyourtv-app

## Co zawiera pierwsza nakładka na źródła

- Gotowy prompt po polsku generowany z modelu TV oraz pełnej listy pakietów. Nie dołącza IP, numeru seryjnego ani klucza ADB.
- Ekran kopiowania promptu i wklejania odpowiedzi AI w podglądzie raportu telewizora.
- Ścisły format: parser odrzuca komendy, obcy model, pakiety nieobecne, wcześniej wyłączone lub chronione, duplikaty oraz nieprawidłowe nazwy.
- Blokadę startu Setup bez bazowej kopii stanu; zmiana Home wymaga timera awaryjnego na TV i działającego fabrycznego launchera.
- Testy jednostkowe parsera i zabezpieczenia (pierwszy etap przeszedł GitHub Actions #7).
- Dwa odrębne, ściśle dopasowane profile Sharp AQUOS_TVE19A i Philips TPM191E. Wszystkie kandydatury do wyłączenia wymagają decyzji użytkownika, a stockowy Home, aktualizacje firmware i pilot pozostają chronione.
- Wybór FLauncher / Projectivy, stockowy YouTube / alternatywa, Plex / Wholphin / oba i tapety. Na razie można skopiować projekt tych wyborów — bez instalowania zewnętrznych APK.

**Ważne:** odpowiedź AI na razie służy tylko do podglądu. Nie ma jeszcze przycisku wykonania listy AI. Integrację z planowaniem i zmianami w TV dodamy po testach bezpieczeństwa.

## Zaplanowany pełny zakres

1. Osobne profile dla Sharp AQUOS_TVE19A i Philips 75PUS7354/12 z oryginalnych archiwów, ze sprawdzeniem modelu, platformy i wersji Androida. **Wprowadzono jako profile ostrożne, bez automatycznego debloatu.**
2. Wybór FLauncher albo Projectivy Launcher. **UI wyboru wprowadzono, instalator i procedura zmiany Home nadal do wykonania.** Przed wyłączeniem stockowego Home: instalacja nowego, próba HOME, uzbrojenie watchdog i możliwość powrotu.
3. Wybór: stockowy YouTube albo opcjonalna aplikacja alternatywna; Plex, Wholphin lub oba. **UI wyboru wprowadzono; automatyczne pobieranie, kontrola podpisów i wykrywanie ABI wymagają dalszej implementacji.**
4. Narzędzia: tapety, zrzuty ekranu, diagnostyka pamięci, „ghost volume”, reset pilota w aplikacji telefonu.
5. Zastosowanie listy AI wyłącznie przez zwykły planner, po świeżej analizie TV, utworzeniu backupu, pokazaniu każdego pakietu i oddzielnej zgodzie.

## Zasady odzyskiwania

- 100% odzysku po każdej awarii sprzętu lub firmware nie jest technicznie możliwe. Watchdog i kopia stanu minimalizują ryzyko.
- Wklejona odpowiedź AI nigdy nie może sama wyłączyć launchera ani wykonywać shell/ADB.
- `pm clear` (reset pilota) usuwa zapisane dane usługi i wymaga osobnej zgody.
- `sm forget` nie może uruchomić się bez inspekcji wolumenów oraz potwierdzenia, że nie są na nich ważne dane.
- Pakiety APK nie są dołączone w tym prototypie. Należy zweryfikować ich pochodzenie, architekturę, SHA-256 i podpis. FLauncher ma licencję GPL-3.0, którą trzeba uwzględnić przy dystrybucji.

## Jak zbudować na Windows 11

Źródła bazowe wymagają JDK 21, Android SDK (compile SDK 37) i submodułu `third_party/cleanyourtv`.

```powershell
git clone --recurse-submodules https://github.com/wskakuj/cleanyourtv-app.git TVToolkit
cd TVToolkit
git checkout feature/tv-toolkit-ai-recovery-v01
git submodule update --init --recursive
.\gradlew.bat :core:flows:test :app:assembleFullDebug
```

APK po poprawnym buildzie: `app/build/outputs/apk/full/debug/`.

Na forku GitHub, po włączeniu Actions, workflow `pr` dodatkowo udostępnia testowy
APK w sekcji **Artifacts** udanego uruchomienia pod nazwą `tv-toolkit-debug-apk`.
To APK ma podpis debug, jest do testów, a nie do publikacji w sklepie.

Pierwszy etap (commit 3ccdad3) miał zielony build GitHub Actions i APK testowy. Kolejne zmiany mają osobną weryfikację CI; wynik poprzedniego buildu nie potwierdza automatycznie poprawności nowej wersji.

Zachowaj oryginalne `LICENSE`, `THIRD-PARTY-NOTICES` i informacje o autorach projektu bazowego.
