# TRAKUNA – Drucker-Kompatibilitäts- und Abnahmetestplan

## Ziel

Herstellerunabhängige Druckerverwaltung für WLAN/LAN und Bluetooth vorbereiten. Dieser Testplan trennt Transport-Verbindung, Gerätefähigkeiten und spätere Druckfunktion.

## Testmatrix

| Verbindung | Basisweg | Was als erfolgreich gilt | Was nicht daraus folgt |
|---|---|---|---|
| WLAN/LAN | IPP/IPPS | IPP-Endpunkt antwortet und liefert lesbare Geräteattribute | Kein Beweis, dass ein Etikettenformat unterstützt wird |
| WLAN/LAN | DNS-SD/mDNS | Gerät kündigt passenden Druckdienst im lokalen Netz an | Kein Beweis, dass Verbindung oder Druckauftrag funktioniert |
| WLAN/LAN | Manuelle IP/Hostname | Host ist über den konfigurierten Dienst erreichbar | Kein Beweis, dass Port oder Protokoll korrekt ist |
| WLAN/LAN | RAW TCP/LPR | Nur mit explizitem Geräteprofil und erfolgreichem Protokolltest | Kein universeller Standard und keine automatische Formatwahl |
| Bluetooth Classic | Gekoppeltes SPP-Gerät | RFCOMM-Verbindung mit bekanntem UUID/Profil gelingt | Kein universeller Bluetooth-Druckstandard |
| Bluetooth BLE | Dokumentiertes GATT-Profil | Dienst und Characteristics stimmen mit Herstellerdokumentation überein | Nicht ohne Adapter auf andere Geräte übertragbar |

## Pflichtprüfungen vor Freigabe einer Geräteintegration

- Hersteller, Modell und Firmware dokumentieren.
- Verbindungstyp und verwendetes Protokoll dokumentieren.
- Erforderliche Android-Version und Berechtigungen prüfen.
- Gerät finden/konfigurieren, verbinden, Status abfragen und sauber trennen.
- Negativtests: Gerät ausgeschaltet, falsche IP, falscher Port, Bluetooth nicht gekoppelt, Berechtigung verweigert, WLAN-Wechsel.
- Prüfen, dass beim Verbinden oder Statusabfragen kein Druckauftrag ausgelöst wird.
- Keine Kennwörter oder Nutzdaten in Logs schreiben.
- Ergebnis pro Gerätemodell als getestet / teilweise getestet / nicht unterstützt kennzeichnen.

## Erste Implementierungsstufe (ohne Druck)

Abnahmekriterien:
1. Manuelles WLAN-Gerät hinzufügen.
2. Gekoppelte Bluetooth-Geräte anzeigen, ohne unaufgefordert zu koppeln.
3. Verbindung auf ausdrückliche Nutzeraktion testen.
4. Verbindungsstatus und Fehler verständlich anzeigen.
5. Keine Druckdaten senden, keine Labels erzeugen, keine Paketaktion ausführen.
6. Berechtigungen nur bei Nutzung anfragen.

## Nicht Teil dieser Freigabe

- Versandlabel erstellen, bestellen oder drucken.
- Paketannahme, Versand, Abholung oder sonstige Carrier-Aktion.
- Native Zebra-SDK- oder herstellerspezifische Integration ohne konkreten, freigegebenen Modell-/Protokollnachweis.
- Behauptung einer universellen Kompatibilität ohne reale Modelltests.

## Teststatus (Stand 27.11.2026)

- **CI-Build, Commit `b6b39282e163cfe71372817ef6465847ca2fcae2`:** GitHub Actions-Lauf [#46006540110](https://github.com/mouzzlii/Pakete-Scanner/actions/runs/46006540110) vollständig erfolgreich. Unit-Tests, Android Lint, Debug-APK-Build und Artefakt-Upload wurden erfolgreich abgeschlossen.
- **Unit-Tests:** 11 Tests im aktuellen Lauf erfolgreich. Zusätzlich wurden die reinen Kotlin-Komponenten lokal kompiliert und ein separater 10-Fälle-Smoke-Test erfolgreich ausgeführt; das sind keine Android-Geräte- oder Hardwaretests.
- **APK-Artefakt:** `TRAKUNA-2.0-Android`, GitHub-Artefakt-ID `15201352884`, ca. 9,25 MB; laut GitHub bis 25.02.2027 verfügbar. Der Download ist über den Workflow-Lauf erreichbar.
- **Im Android-Testbuild enthalten:** manuelle WLAN/LAN-Host-/Port-Prüfung per TCP mit Zeitlimit; die Anzeige weist ausdrücklich darauf hin, dass Erreichbarkeit keine Drucker- oder IPP-Kompatibilität bestätigt.
- **Im Android-Testbuild enthalten:** Anzeige bereits gekoppelter Bluetooth-Geräte und ausdrücklicher Verbindungstest über Bluetooth Classic SPP/RFCOMM mit Standard-SPP-UUID; es werden keine Druckdaten gesendet.
- **Build:** Den aktuellen Build-Status und das zugehörige Artefakt zeigt der [GitHub-Actions-Workflow](https://github.com/mouzzlii/Pakete-Scanner/actions/workflows/trakuna-2.0.yml). Ein erfolgreicher CI-Lauf bestätigt Kompilierung und Artefakterstellung, nicht die reale Gerätefunktion.
- **Noch nicht implementiert:** IPP/IPPS-Aushandlung und Geräteattribute, mDNS/DNS-SD-Suche, LPR, RAW-TCP-Geräteprofile, BLE/GATT sowie Druckformatadapter.
- **Hardwaretests:** nicht durchgeführt. Der erfolgreiche CI-Build bestätigt nur die Kompilierung und Artefakterstellung, nicht die Funktion mit einem realen Druckermodell.


## Zusätzliche Grenzen der aktuellen App

- Die Schaltfläche heißt **„ANBIETER ERKENNEN & PORTAL ÖFFNEN“**. Bei einem erkannten Muster wird nur das offizielle Portal im externen Browser geöffnet; der Barcode wird nicht automatisch in das Portal eingefügt oder an einen TRAKUNA-Server übertragen.
- Ein externer HID-/Tastatur-Scanner gibt Zeichen in das Eingabefeld ein. Die App erkennt bzw. öffnet den Anbieter erst nach ausdrücklichem Antippen der Schaltfläche.
- Der 13-stellige `872…`-Beispielcode wird wegen der möglichen EAN-13-Mehrdeutigkeit nicht automatisch GLS zugeordnet; GLS muss in diesem Fall manuell gewählt werden.
- Ein erfolgreicher Build ist kein Laufzeit-, Kamera-, Browser-, Netzwerk- oder Bluetooth-Hardwaretest.

Die Matrix bleibt die Abnahmegrundlage für die weiteren Integrationsstufen.
