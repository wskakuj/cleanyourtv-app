# TV Toolkit (wersja rozwojowa)

Własny wariant mobilnej aplikacji Clean Your TV, rozwijany na podstawie kodu na licencji MIT: https://github.com/udhaykumarbala/cleanyourtv-app

## Co zawiera pierwsza nakładka na źródła

- Gotowy prompt po polsku generowany z modelu TV oraz pełnej listy pakietów. Nie dołącza IP, numeru seryjnego ani klucza ADB.
- Ekran kopiowania promptu i wklejania odpowiedzi AI w podglądzie raportu telewizora.
- Ścisły format: parser odrzuca komendy, obcy model, pakiety nieobecne, wcześniej wyłączone lub chronione, duplikaty oraz nieprawidłowe nazwy.
- Blokadę startu Setup bez bazowej kopii stanu; zmiana Home wymaga timera awaryjnego na TV i działającego fabrycznego launchera.
- Testy jednostkowe parsera i zabezpieczenia (jeszcze nieuruchomione w środowisku bez JDK 21/SDK).

**Ważne:** odpowiedź AI na razie służy tylko do podglądu. Nie ma jeszcze przycisku wykonania listy AI. Integrację z planowaniem i zmianami w TV dodamy po testach bezpieczeństwa.

## Zaplanowany pełny zakres

1. Osobne profile dla Sharp AQUOS_TVE19A i Philips 75PUS7354/12 z oryginalnych archiwów, ze sprawdzeniem listy pakietów i wersji Androida.
2. Wybór FLauncher albo Projectivy Launcher. Przed wyłączeniem stockowego Home: instalacja nowego, próba HOME, uzbrojenie watchdog i możliwość powrotu.
3. Wybór: stockowy YouTube albo opcjonalna aplikacja alternatywna; Plex, Wholphin lub oba. Automatyczne wykrywanie ABI 32/64-bit.
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
git clone --recurse-submodules https://github.com/udhaykumarbala/cleanyourtv-app.git TVToolkit
cd TVToolkit
git checkout 8ec0679f0a3b04cbf1a23d7762f68b35679f8b59
git submodule update --init --recursive
# Rozpakuj ZIP z nakładką TV Toolkit do folderu TVToolkit, ze scaleniem folderów.
.\gradlew.bat :core:flows:test :app:assembleFullDebug
```

APK po poprawnym buildzie: `app/build/outputs/apk/full/debug/`.

Na forku GitHub, po włączeniu Actions, workflow `pr` dodatkowo udostępnia testowy
APK w sekcji **Artifacts** udanego uruchomienia pod nazwą `tv-toolkit-debug-apk`.
To APK ma podpis debug, jest do testów, a nie do publikacji w sklepie.

Uwaga: obecne środowisko ma JDK 17 i nie ma Android SDK. Nie twierdzimy, że APK jest już skompilowany. Sprawdzono statycznie składnię plików XML i różnice Git.

Zachowaj oryginalne `LICENSE`, `THIRD-PARTY-NOTICES` i informacje o autorach projektu bazowego.
