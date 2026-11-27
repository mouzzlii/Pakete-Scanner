# TRAKUNA – Shop-Mitarbeitertest (interner Techniktest)

**Zweck:** Bedienung, Barcode-Erkennung und reine Verbindungsprüfungen im kleinen Kreis prüfen. Dieser Test ist keine Freigabe für den Produktivbetrieb.

## Vor dem Test

- Mit 2–3 freiwilligen Mitarbeitern und zunächst einem Android-Gerät pro Person testen.
- Nur den ausdrücklich bereitgestellten TRAKUNA-Testbuild verwenden.
- Keine echten Kundensendungen, Kundendaten, Sendungsnummern oder Zugangsdaten verwenden.
- Keine Anmeldung bei Carrier-Portalen und keine Eingabe oder Übermittlung von Codes an Carrier.
- Bestehende Shop-Abläufe nicht ersetzen oder verändern.
- Druckerverbindung nur an einem eigenen oder ausdrücklich freigegebenen Testgerät prüfen.
- Keine Etiketten, Druckaufträge oder Versandaktionen auslösen.

## Testablauf

### 1. Installation und Start
- APK aus dem bereitgestellten ZIP extrahieren und auf dem Testgerät installieren.
- App starten und prüfen, ob die Oberfläche vollständig angezeigt wird.
- Notieren, ob Android eine Berechtigung anfragt und ob die Erklärung verständlich ist.

### 2. Barcode-Erkennung
- Einen eigens für den Test erzeugten Fantasie-Barcode verwenden, der garantiert keiner echten Sendung zugeordnet ist.
- Kamera-Scanner öffnen, Code erfassen und prüfen, ob der erkannte Text korrekt angezeigt wird.
- Falls vorhanden, einen externen HID-/Tastatur-Scanner verwenden: Code scannen/eintippen und anschließend ausdrücklich **ANBIETER ERKENNEN & PORTAL ÖFFNEN** antippen.
- Prüfen, ob der Code in TRAKUNA sichtbar bleibt. Die Schaltfläche erkennt den Anbieter und öffnet bei eindeutiger Erkennung nur dessen Portal im externen Browser; sie fügt den Code nicht automatisch in das Portal ein.
- Wichtig: Ein HID-Scan wird nicht automatisch in ein Carrier-Portal eingefügt.

### 3. Carrier-Auswahl und Portal
- Automatische Erkennung nur als Vorschlag bewerten; bei Unsicherheit manuell den Carrier auswählen.
- Den Portal-Button höchstens zum Öffnen der offiziellen Website testen.
- Keine Zugangsdaten eingeben, keinen Code eintragen und keine Versand-/Paketaktion durchführen.
- Notieren, wenn ein Carrier falsch erkannt wird oder ein Link nicht mehr stimmt.

### 4. Lokale Paketverwaltungs-Demo – optional
- Nur synthetische Fantasie-Barcodes verwenden, die garantiert keiner echten Sendung zugeordnet sind.
- Optional den lokalen Demoablauf durchspielen: Paket annehmen, Lagerplatz vergeben, als abholbereit markieren und Übergabe bestätigen. Für Retouren den getrennten Retourenablauf prüfen.
- Prüfen, ob Status und Lagerplatz nach einem App-Neustart erhalten bleiben.
- Anschließend die Funktion „Lokale Demodaten löschen“ testen: Abbrechen darf nichts löschen; erst die ausdrückliche Bestätigung darf alle lokalen Demo-Einträge entfernen.
- Diese lokale Demo führt keine echte Paketannahme, Benachrichtigung, Carrier-Buchung oder Übergabe aus. Die Datenbank speichert Barcodes derzeit unverschlüsselt; niemals echte Kundendaten eingeben.

### 5. Drucker-Verbindung – optional
- Nur einen eigenen/freigegebenen Drucker verwenden.
- WLAN/LAN: ausschließlich die manuelle Erreichbarkeitsprüfung mit bekanntem Host und Port durchführen.
- Bluetooth: nur ein bereits gekoppeltes Gerät und den ausdrücklichen SPP-Verbindungstest verwenden.
- Keine Druckdaten senden. Ein erfolgreicher TCP- oder SPP-Verbindungstest beweist weder IPP-Unterstützung noch Druckfähigkeit.
- Keine unbekannten IP-Adressen oder fremden Geräte testen.

### 6. Fehlerfälle
Wenn gefahrlos möglich, jeweils nur einen Fall prüfen:
- Kamera-Berechtigung verweigert.
- Bluetooth ausgeschaltet oder Gerät nicht gekoppelt.
- Drucker ausgeschaltet.
- Falscher Host oder Port bei der manuellen Netzwerkprüfung.
- WLAN-Wechsel während/nach einem Verbindungstest.

Keine Einstellungen anderer Mitarbeiter oder produktiver Geräte verändern.

## Rückmeldung pro Testgerät

Bitte für jeden Punkt **OK / Fehler / nicht getestet** angeben und bei Fehlern die genaue Anzeige notieren:

| Bereich | Ergebnis | Beobachtung |
|---|---|---|
| Installation und Start | | |
| Oberfläche / Lesbarkeit | | |
| Kamera-Barcode-Scan | | |
| Externer HID-Scanner | | |
| Erkannter Code korrekt | | |
| Carrier-Erkennung | | |
| Manuelle Carrier-Auswahl | | |
| Portal öffnet sich | | |
| Lokale Paketverwaltung (optional) | | |
| Status bleibt nach Neustart erhalten (optional) | | |
| Demo-Löschdialog: Abbrechen / Bestätigen (optional) | | |
| Demo-Einträge nach Löschung entfernt (optional) | | |
| WLAN/LAN-Erreichbarkeit (optional) | | |
| Bluetooth-SPP-Verbindung (optional) | | |
| Fehlermeldungen verständlich | | |
| Bedienung ohne Erklärung | | |

Zusätzlich notieren:
- Android-Gerät und Android-Version:
- TRAKUNA-Build/Installationsdatum:
- Drucker-Hersteller und Modell (falls getestet):
- Was war unklar oder unnötig kompliziert?
- Was hat nicht funktioniert?
- Was sollte vor einem weiteren Test verbessert werden?

## Datenschutz und Screenshots

- Keine Screenshots mit echten Sendungsnummern, Namen, Adressen, Kundendaten oder Zugangsdaten weitergeben.
- Vor dem Teilen eines Screenshots alle sichtbaren Codes und persönlichen Daten vollständig unkenntlich machen.
- Fehlerberichte möglichst mit Fantasie-Testdaten erstellen.

## Grenzen des aktuellen Tests

TRAKUNA ist in dieser Phase ein Barcode-Scanner und Weiterleitungshelfer. Zusätzlich enthält die App eine klar getrennte lokale Paketverwaltungs-Demo. Diese Demo ist kein produktives Shop-System und führt keine Carrier-seitigen Paketannahmen, Ausgaben, Buchungen, Benachrichtigungen oder Versandaktionen aus. Labelbestellung/-erstellung und Druckaufträge sind nicht enthalten. Der erfolgreiche CI-Build ist kein Nachweis für reale Gerätekompatibilität; echte Android- und Hardwaretests müssen separat dokumentiert werden.

**Freigaberegel:** Erst nach Sichtung aller Rückmeldungen und Behebung kritischer Fehler wird entschieden, ob ein weiterer, größerer Test sinnvoll ist. Keine Produktivfreigabe aus diesem Mitarbeitertest ableiten.
