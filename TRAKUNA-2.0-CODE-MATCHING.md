# TRAKUNA 2.0 – Code-Erkennung und Zuordnung

## Aktueller Implementierungsstand

- Arbeitszweig: `trakuna-2.0-hardening`
- Umsetzung im PR #6; der PR bleibt Draft und wird nicht automatisch zusammengeführt.
- Die Android-App wird derzeit aus `.github/workflows/trakuna-2.0.yml` generiert. Diese Workflow-Datei ist für diesen Zweig die maßgebliche Implementierungsquelle.
- `Build TRAKUNA 2.0.` ist ein älterer, nicht synchronisierter Build-Snapshot. Er enthält eine frühere App-Version und darf nicht als aktuelle Implementierung behandelt werden.
- `main` wurde nicht geändert.

## Was die Erkennung jetzt unterscheidet

1. **Barcode-Erkennung:** ML Kit liefert den gelesenen Inhalt und das Barcodeformat.
2. **Code-Normalisierung:** Groß-/Kleinschreibung wird vereinheitlicht; Leerzeichen und Bindestriche werden für Vergleiche entfernt. Der GS1-Scannerpräfix `]C1` und die ausgeschriebene GS1-AI `(00)` werden berücksichtigt.
3. **Identifikator-/Paketdiensterkennung:** Nur ausreichend belegte Muster dürfen automatisch vorbelegt werden.
4. **Sendungszuordnung:** Bei Abholung wird der normalisierte Code exakt mit gespeicherten Sendungen verglichen. Mehrere Treffer führen zu keiner automatischen Statusänderung.

## Unterstützte Erkennungsregeln

| Regel | Verhalten | Begründung |
|---|---|---|
| Österreichische Post | Automatische Vorbelegung nur bei 22 Ziffern, Partnerkennung `10`, bekanntem Produkt-Prozess-Kennzeichen und gültiger 3/1-Modulo-10-Prüfziffer | Offizielle Post-Spezifikation, Version 6.2 |
| UPS | Automatische Vorbelegung bei `1Z` plus 16 alphanumerischen Zeichen | Offizielle UPS-Trackinghilfe dokumentiert dieses 18-stellige Format |
| DPD | 14 Ziffern werden nur als möglicher DPD-Kandidat angezeigt; manuelle Bestätigung bleibt erforderlich | DPD Österreich nennt 14-stellige Paketnummern; die Länge allein ist kein eindeutiger Identitätsbeweis |
| DHL | Keine automatische Zuordnung | DHL beschreibt Tracking-IDs als Zahlen-/Buchstabenkombinationen und mehrere Geschäftsbereiche; kein universelles Muster für diese App verifiziert |
| GLS | 8 alphanumerische Zeichen oder 11/12 Ziffern werden nur als Kandidat angezeigt; manuelle Bestätigung bleibt erforderlich | GLS ShipIT dokumentiert TrackID mit 8 alphanumerischen Zeichen sowie ParcelNumber mit 11 oder 12 Ziffern. Diese Muster können auch mit anderen Nummern kollidieren |
| GS1 SSCC | Wird als 18-stellige Logistikeinheiten-ID erkannt, aber keinem Paketdienst zugeordnet | SSCC identifiziert eine Logistikeinheit, nicht den Carrier |

**Wichtig:** Eine Format-Erkennung bestätigt nicht, dass eine Sendung tatsächlich existiert oder beim jeweiligen Dienst aktiv ist. Unbekannte und unsichere Codes bleiben unzugeordnet. DHL und GLS bleiben in der festen Auswahlliste verfügbar.

## Abholung und Duplikate

- In der Sendungsübersicht kann nun direkt ein Barcode für die Abholung gescannt werden.
- Ein einzelner normalisierter Treffer wird mit Paketdienst, optionalem Kundennamen, Vorgang und Status angezeigt.
- Die Abholung wird erst nach dem bewussten Antippen von „Speichern“ bestätigt.
- Kein Treffer: keine Statusänderung.
- Mehrere Treffer: keine automatische Abholung.
- Doppelte aktive Sendungsnummern werden nach normalisiertem Vergleich verhindert.
- Fuzzy-Matching und automatische Verknüpfung verschiedener, voneinander abweichender Sendungsnummern sind bewusst nicht enthalten. Dafür wäre eine bestätigte Alias-/Referenznummern-Funktion nötig.

## Tests und offene Verifikation

- 12 eigenständige Kotlin-Assertions für Normalisierung, Post-Prüfziffer, UPS-Format, DPD-Kandidat, SSCC und unbekannte Codes lokal mit `kotlinc` ausgeführt: **bestanden**.
- 10 JUnit-Tests sind im Android-Buildworkflow ergänzt und werden dort über `testDebugUnitTest` aufgerufen.
- Der App-Code-Commit `f14d00dfe3e4af8325f93420bc9c85ec88e44cb1` wurde in GitHub Actions mit Run #215 erfolgreich gebaut. Der Job führte `testDebugUnitTest assembleDebug` aus; APK-Prüfung, Signaturprüfung und Artefakt-Upload waren ebenfalls erfolgreich.
- Die 10 JUnit-Tests liefen im erfolgreichen CI-Job mit. Das bestätigt die automatisierten Resolver-Tests, nicht die Erkennung echter Paketetiketten.
- Installation auf einem realen Android-Gerät, Kameratests und Druckertests sind noch nicht bestätigt.
- Der Scan- und Zuordnungsablauf muss mit echten, anonymisierten Testetiketten der fünf tatsächlich eingesetzten Paketdienste geprüft werden.

## Quellen

- Österreichische Post, *Belabelungs- und Avisodatenfibel*, Version 6.2 (Juni 2025), Kapitel 4.1 und 4.3: https://assets.post.at/-/media/Dokumente/De/Geschaeftlich/Versenden/Paket/Belabelungs-und-Avisodatenfibel-V6-2.pdf
- DPD Österreich, Support – Paketnummer: https://www.dpd.com/at/en/support/
- GLS ShipIT, Tracking REST API – TrackID/ParcelNumber-Felder: https://shipit.gls-group.com/documentation/webservices/5_2_5/WS-REST-API/rest_tracking.html
- UPS, Tracking Support – Beispiele für Trackingnummern: https://www.ups.com/us/en/support/tracking-support
- DHL Österreich, Sendungsverfolgung – allgemeine Tracking-ID-Erklärung: https://www.dhl.com/at-de/home/tracking.html
- GS1 Austria, SSCC-Identifikation und GS1-Logistiklabel: https://www.gs1.at/

## Sicherheitsgrenzen

- Kein Merge nach `main`.
- Keine Produktionsbereitstellung, keine externen Carrier-APIs und keine automatische Datenübertragung.
- Keine automatische Abholung bei mehrdeutigen Treffern.
- Keine erfundenen Carrier-Muster.
