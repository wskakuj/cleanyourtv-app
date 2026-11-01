# CANAL+ BOX HY40A2 — zachowaj pełną obsługę operatora

Profil: `tvtoolkit-canal-plus-hy40a2-c308`.

## Źródło i dopasowanie

Raport użytkownika z 2026-11-01, SHA-256:
`d6206c1899e584619350b1f5698e77d9ea6eae10fa3afc1a6a411c7652590fc5`.

- Marka: `CANAL-PLUS`; producent: `SkyworthDigital`
- Model: `CANAL PLUS BOX`; urządzenie: `HY40A2`
- Android: 10, SDK 29
- Odcisk kompilacji musi rozpoczynać się od
  `CANAL-PLUS/HY4001/HY40A2:10/QTT8.201201.002/C3.0.8_20220628:`
- HOME: `com.canal.box/.ui.boot.BoxMainActivity`, z zapasowym
  `com.android.tv.settings/.system.FallbackHome`.

**Inny firmware lub model nie dostaje tego profilu**. Dopasowanie wyłącznie po marce CANAL+ albo producencie Skyworth nie wystarcza.

## Zasady tego profilu

1. Wszystkie 93 pakiety systemowe z raportu zostały sklasyfikowane. 90 z nich jest chronionych przez `neverDisable`, w tym oprogramowanie CANAL+, telewizja, pilot, Bluetooth, aktualizacje, ustawienia, usługi Google i fabryczny Home.
2. Tylko poniższe trzy aplikacje są potencjalnym wyborem do wyłączenia. `GroupDefault.ASK` oznacza brak automatycznego debloatu:

   - `com.google.android.play.games` — Gry Google Play;
   - `com.google.android.youtube.tvmusic` — YouTube Music;
   - `com.google.android.videos` — filmy Google.

   Jeśli którejś aplikacji używasz, zostaw ją włączoną. Nawet te trzy nie są jeszcze potwierdzone testem na fizycznym HY40A2.
3. Dla tego profilu planner odmawia przejęcia Home. Nie wyłącza `com.canal.box` ani nie zmienia przycisku Home. Instalacja innego launchera jest osobną czynnością i nie przełącza automatycznie systemu.
4. Przed pierwszą zmianą Setup wymaga zapisanej bazowej migawki stanu na telefonie oraz zapisu punktu przywracania na TV. Jeśli nie można zapisać plików w `/sdcard/cleanyourtv/`, plan nie wyłącza żadnego pakietu.
5. Każde wyłączenie przechodzi przez standardowy planner, silnik ADB i dziennik zmian. Nie używamy odinstalowania z `--user 0`, `pm clear` ani resetu danych.

## Powrót po wyłączeniu opcjonalnej aplikacji

**W aplikacji telefonu:** połącz się z tym samym urządzeniem → otwórz listę wyłączonych pakietów w Manage → wybierz pakiet → **Przywróć / Put back**. Dziennik zmian i migawka telefonu identyfikują wcześniejszy stan.

**Awaryjnie przez komputer z autoryzowanym ADB** (tylko gdy odpowiedni pakiet był przez nas wyłączony):

```sh
adb shell pm enable --user 0 com.google.android.play.games
adb shell pm enable --user 0 com.google.android.youtube.tvmusic
adb shell pm enable --user 0 com.google.android.videos
```

Zwykle wystarczy wykonać jedną właściwą komendę, nie wszystkie. `pm enable` jest odwróceniem `pm disable-user`; nie usuwa danych aplikacji. Gdy debugowanie ADB jest wyłączone, należy je najpierw legalnie ponownie włączyć w ustawieniach urządzenia. Kopia i watchdog ograniczają ryzyko, ale nie gwarantują odzyskania po awarii sprzętu ani uszkodzeniu firmware.

## Status

Przygotowany i testowany w kodzie, **bez potwierdzenia na fizycznym CANAL+ BOX**. Pierwszy test powinien obejmować analizę, widok listy, jedno dobrowolne wyłączenie, restart urządzenia, test kanałów CANAL+, pilota i przywrócenie pakietu.
