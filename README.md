# 🌲 Leśne Punkty

Aplikacja na Androida w klimacie bajkowego lasu. Dziecko zbiera punkty (🌰 żołędzie) za dobre zachowanie, wymienia je na nagrody, zdobywa poziomy i odznaki, a za złe zachowanie traci punkty.

<p>
<img src="screenshots/1_las.png" width="200"> <img src="screenshots/2_zachowania.png" width="200">
<img src="screenshots/3_nagrody.png" width="200"> <img src="screenshots/4_odznaki.png" width="200">
</p>

## Co potrafi

- **Zachowania** – dobre i złe, każde z ikonką (zestaw ponad 60 emoji albo własne emoji), punktami i licznikiem serii 🔥. Jedno dotknięcie przyznaje lub odejmuje punkty; pomyłkę można cofnąć z paska na dole ekranu albo z historii.
- **Nagrody** – Leśny Skarbiec z cenami, paskiem postępu i potwierdzeniem odbioru.
- **Poziomy** – 12 leśnych poziomów od 🌱 Nasionka do 👑 Króla Lasu. Liczą się zdobyte punkty, więc odbieranie nagród nie cofa poziomu.
- **Kreator odznak** – cztery rodzaje warunków:
  - 🎯 **Combo dnia** – kilka zachowań tego samego dnia (z liczbą powtórzeń każdego),
  - 🔥 **Seria dni** – zachowania codziennie przez N dni z rzędu,
  - 🧮 **Licznik** – zachowania łącznie N razy,
  - 🛡️ **Bez wpadki** – N dni z rzędu bez wybranych złych zachowań.

  Za odznakę można ustawić bonusowe punkty.
- **PIN rodzica** (opcjonalny) – blokuje przyznawanie punktów, odbieranie nagród i edycję. Aplikacja sama blokuje się po wyjściu z niej.
- Wszystkie dane są zapisywane lokalnie na telefonie (`data.json`, objęte kopią zapasową Androida).

## Budowanie

```bash
./gradlew assembleRelease   # APK: app/build/outputs/apk/release/app-release.apk
```

Wymaga JDK 17+ i Android SDK (platforma 35). Testy (`./gradlew testDebugUnitTest`) sprawdzają logikę odznak i poziomów, przechodzą przez kreator odznak oraz odświeżają zrzuty ekranu w `screenshots/`. GitHub Actions buduje APK przy każdym pushu (zakładka *Actions* → artefakt `lesne-punkty-apk`).

> APK jest podpisywany kluczem debug. Aktualizacja z innego komputera (inny klucz) wymaga odinstalowania poprzedniej wersji, co usuwa dane.
