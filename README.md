# Fokus – Reels-Block & ruhiger Homescreen (Android)

## Was die App macht
- **Reels-Block:** Öffnest du in Instagram Reels (Reels-Tab oder ein Reel aus Feed/DMs), wirst du sofort zurückgeschickt. Feed, Stories und Nachrichten funktionieren normal.
- **Ruhiger Homescreen:** Schwarz, Uhrzeit, Datum, max. 6 Apps als Text, keine Icons und keine roten Badges. Alle anderen Apps gibt's nur über „Alle Apps“ plus Suche.
- **Spiel:** XP, Level mit Fortschrittsbalken und eine 🔥-Serie für jeden Tag, an dem du etwas abhakst.
- **Tagesziele:** bis zu 3 Ziele auf dem Homescreen. Antippen = erledigt, lange drücken = löschen.
- **Challenge & Spruch des Tages:** jeden Tag neu.
- **Akzentfarbe:** 6 Farben, auf Wunsch täglich wechselnd, mit pulsierendem Leuchten hinter der Uhr.
- Zähler, wie oft du schon geblockt wurdest.

## APK bauen (kostenlos, ohne PC-Software)
1. Auf github.com einen Account anlegen und ein neues Repository erstellen (z. B. `fokus`, privat).
2. Den kompletten Inhalt dieses Ordners hochladen, **inklusive des Ordners `.github`**.
3. GitHub baut automatisch (Reiter **Actions**, dauert ca. 3–5 Min.).
4. Unter **Releases** liegt danach `Fokus.apk`. Direkt auf dem Handy herunterladen.

## Installieren & einrichten
1. `Fokus.apk` öffnen → „Installation aus unbekannten Quellen“ für Browser/Dateien erlauben.
2. App **Fokus Einstellungen** öffnen.
3. **Bedienungshilfen** → „Fokus Reels-Block“ einschalten.
   - Gesperrt/grau? → „App-Info öffnen“ → ⋮ oben rechts → „Eingeschränkte Einstellungen zulassen“ → nochmal.
4. **Homescreen-Apps auswählen** (max. 6, z. B. Telefon, Nachrichten, Kalender, Maps, Kamera).
5. **Fokus als Startbildschirm festlegen.**
6. Optional: Graustufen aktivieren (siehe Tipp in der App).

Zurück zu den Einstellungen: auf dem Homescreen lange drücken.

## Hinweise
- Abschalten geht nur über die Android-Bedienungshilfen. Das ist Absicht (harter Modus).
- Instagram ändert ab und zu seine App. Falls Reels wieder durchrutschen: die Erkennung steckt in `ReelsBlockerService.java` (`REELS_VIEW_IDS`).
- Die App sammelt und sendet keine Daten. Sie liest nur innerhalb von Instagram, was gerade angezeigt wird.
