# TV Toolkit: własna aplikacja na Androida

Status: projekt rozwojowy oparty na Clean Your TV (MIT). Ten dokument opisuje zakres; nie oznacza, że funkcje zostały już zaimplementowane.

## Wymagania

1. Telefon Android 9+ łączy się przez ADB po sieci lokalnej z Android TV/Google TV.
2. Rozpoznaje telewizor po marce, modelu, identyfikatorze urządzenia, Android SDK i pakietach.
3. Przed zmianami pobiera raport i dokładny stan pakietów, tworzy punkt przywracania i pokazuje plan.
4. Dla nierozpoznanego TV: generuje raport bez danych wrażliwych oraz prompt do AI, importuje listę do wyłączenia po walidacji.
5. Pozwala wybrać FLauncher lub Projectivy Launcher; fabryczny launcher wyłącza wyłącznie po udanym przełączeniu i z watchdogiem.
6. Daje wybór: zachować YouTube lub wybrać alternatywną aplikację; zainstalować Plex, Wholphin albo oba.
7. Narzędzia zależne od modelu: naprawa ghost volume, reset usługi pilota w telefonie, tapety i zrzuty ekranu.
8. Każdą operację umożliwia cofnąć do dokładnego stanu sprzed zmiany.

## Zasada bezpieczeństwa

- Import z AI jest tylko propozycją; aplikacja sama weryfikuje istnienie pakietu, listę chronioną i zależności sprzętowe.
- Nigdy nie uruchamia komend zwróconych przez AI. Importuje wyłącznie nazwy pakietów w ściśle zdefiniowanym formacie.
- Żadnego `pm uninstall` ani `pm clear` w debloat. `pm disable-user --user 0` dopiero po potwierdzeniu planu.
- Nieznany TV nie otrzymuje automatycznego wyłączenia launchera, aktualizacji, usług HDMI, Wi-Fi, Bluetooth, pilota, tunera, Cast lub dostępności.
- Funkcje naprawcze mogą zmieniać stan użytkownika; przed uruchomieniem wymagają odrębnego potwierdzenia i testów zgodności.

## Kolejne etapy

1. Parser i walidator odpowiedzi AI z testami. **Wprowadzono; wymaga jeszcze wykonania testów Gradle.**
2. UI do kopiowania promptu i podglądu importu listy. **Wprowadzono; celowo bez wykonywania zmian.**
3. Profile Sharp AQUOS_TVE19A i Philips TPM191E z dwóch oryginalnych zestawów.
4. Wybór launcherów i zewnętrznych APK po weryfikacji źródła oraz podpisu.
5. Funkcje serwisowe, tapety, testy realnych urządzeń i wydanie APK.

Wprowadzony mechanizm `RecoveryGate` blokuje Setup, gdy brakuje poprawnej kopii stanu, tożsamość telewizora jest niepewna lub zmiana Home nie ma uzbrojonego watchdoga i dostępnego fabrycznego launchera. Istniejące mechanizmy `DefaultEngine` nadal obowiązują.
