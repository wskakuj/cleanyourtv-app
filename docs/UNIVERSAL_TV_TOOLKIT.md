# Universal TV Toolkit — Android TV / Google TV / przystawki

Cel: jedna aplikacja na telefon do bezpiecznej inwentaryzacji, opt-in debloatu i przywracania **niezależnie od marki**. Gotowe profile Sharp, Philips i CANAL+ są jedynie pomocniczymi regułami ochronnymi; pozostałe urządzenia korzystają z ostrożnego przepływu AI i lokalnych zabezpieczeń.

## Nowy przebieg

1. Połącz telewizor przez autoryzowane ADB; aplikacja pokazuje faktycznie aktywny pakiet Home, a nie tylko preferencję użytkownika.
2. Otwórz „Switched off”: zobacz pakiety wyłączone przez tę aplikację oraz osobno pakiety wyłączone innym narzędziem. Odśwież stan TV i przywracaj pojedynczo.
3. Otwórz „Analyse packages with AI”, skopiuj prompt ze spisem systemowych/użytkownika oraz stanem ON/OFF. Prompt nie zawiera numeru seryjnego, IP ani klucza ADB.
4. Wklej odpowiedź wyłącznie w schemacie `TV_TOOLKIT_PLAN_V2`. Sprawdzamy dokładnie markę, model, urządzenie, API, SHA-256 identyfikatora firmware oraz każdą nazwę pakietu.
5. Aplikacja odrzuca nieznane/już wyłączone/chronione pakiety, błędne linie, duplikaty i niespójne odpowiedzi w całości. `NONE=YES` to prawidłowa odpowiedź.
6. Wszystkie proponowane pakiety są **domyślnie niezaznaczone**. Użytkownik wybiera, widzi powody i potwierdza listę.
7. Przed zmianą wymagamy bazowej migawki, aktualnego odczytu TV, bezpiecznego planu z dokładnie wybranymi `Op.Disable` oraz udanego zapisu punktu przywracania na urządzeniu. Dopiero wtedy standardowy silnik ADB z własnymi kontrolami może wykonać plan.

## Format odpowiedzi AI

```text
TV_TOOLKIT_PLAN_V2
BRAND=...
MODEL=...
DEVICE=...
SDK=...
BUILD_SHA256=...
DISABLE=com.example.optional|Zweryfikowana zbędna aplikacja
KEEP=com.example.core|Krytyczna usługa
END_TV_TOOLKIT_PLAN
```

Wiersz `KEEP` jest informacyjny. Gdy nie ma bezpiecznych kandydatów, zamiast `DISABLE` użyj `NONE=YES`. Każdy identyfikator w odpowiedzi musi pochodzić z inwentaryzacji. Niedozwolone są polecenia ADB i tekst poza blokiem. Program nie wykonuje odpowiedzi AI bezpośrednio.

## Launcher

- Pokazujemy **faktyczny** pakiet obsługujący Home, odczytany z urządzenia.
- Ekran konfiguracji pozwala wybrać: nie zmieniaj / FLauncher / Projectivy (instalacja osobna, z kontrolą APK).
- Na nieznanym urządzeniu nie wyłączamy fabrycznego Home automatycznie. Instalacja launchera to nie to samo co przejęcie klawisza Home. Gdzie Android TV oferuje ustawienie domyślnej aplikacji Home, użytkownik dokonuje tego świadomie na urządzeniu.
- Profil operatora może blokować zmianę Home nawet gdy pakiet alternatywny jest zainstalowany.

## Przywracanie i ograniczenia

- „Put back” wykonuje operację `pm enable --user 0` przez sprawdzony planner. Dla pakietów wyłączonych innym narzędziem jest dodatkowe potwierdzenie; brak automatycznego Undo, bo nie wiemy, dlaczego zostały wyłączone.
- W razie problemu przez komputer można włączyć **tylko dokładnie znany pakiet**: `adb shell pm enable --user 0 NAZWA.PAKIETU`.
- Nie ma technicznej gwarancji obsługi **każdego** telewizora: wymagane jest dostępne i autoryzowane ADB; producent może zablokować część funkcji.
- Nie obiecujemy odzyskania urządzenia w razie uszkodzenia firmware czy utraty dostępu do ADB.

Status: wymagane testy CI i testy na rzeczywistych urządzeniach przed uznaniem zmiany za stabilną.
