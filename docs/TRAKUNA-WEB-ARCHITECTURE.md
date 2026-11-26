# TRAKUNA – Weiterentwicklung des bestehenden Paketshop-Systems

## Leitentscheidung
Die Web-Oberfläche ersetzt TRAKUNA nicht und darf die bestehende Android-App nicht entkernen. Die ursprünglichen Paketshop-Abläufe bleiben maßgeblich: Annahme, Abholung, Retoure, Versandvormerkung, PIN, Kunden-/Sendungssuche und Sendungsübersicht. Die Web-Version ist ein ergänzender mobiler Ausbau und übernimmt dieselbe Produktlogik.

## Neue Erweiterungen
- Kamera-Barcode-Scanner (Browser BarcodeDetector, abhängig von Android-/Chrome-Unterstützung) plus manuelle Eingabe.
- Anbietererkennung anhand konservativer Barcode-/Sendungsnummernregeln. Eindeutige Muster können automatisch zum hinterlegten Anbieterportal führen; unsichere Formate verlangen Bestätigung.
- Erkennt der Scanner einen Anbieter, wird dieser zugleich im TRAKUNA-Erfassungsformular vorbelegt.
- Portaladressen werden pro Anbieter konfiguriert und nur lokal im Browser gespeichert. Keine Zugangsdaten oder Cookies in TRAKUNA.
- Lokale Etikettenauswahl PDF/PNG/JPG mit Vorschau und Übergabe an den Android-Druckdialog.

## Grenzen
- Barcode-Formate sind nicht global eindeutig. Die Regeln sind ein Startbestand, keine offiziell bestätigte vollständige Carrier-Datenbank. Falschzuordnungen müssen mit echten, zulässig verwendbaren Labels ausgeschlossen werden.
- Automatische Portalweiterleitung funktioniert nur, wenn für den erkannten Anbieter eine gültige HTTPS-Adresse hinterlegt ist. Anbieter können Anmeldung, MFA oder spezielle Geschäftskundenportale verlangen.
- Fremde Portalseiten werden nicht eingebettet, ausgelesen oder automatisiert bedient. Offizielle APIs/Partnerintegrationen müssen pro Anbieter geprüft werden.
- Der aktuelle Web-Druckdialog ist kein Zebra-Direktdruck und garantiert keine Etikettenmaßstäbe oder ZPL-Übertragung.
- Keine Daten-Synchronisation zwischen Web und Android-Prototyp; die lokale Demo speichert im jeweiligen Browser.

## Nächste technische Schritte
1. Gemeinsames Daten-/Funktionsmodell für Android und Web definieren, damit keine doppelte oder widersprüchliche Paketlogik entsteht.
2. Carrier-Erkennung anhand repräsentativer Labels für DHL, Österreichische Post, DPD, GLS, UPS und weitere relevante Anbieter validieren; Confidence-Stufen und manuelle Korrektur vorsehen.
3. Anbieterportal-URLs und zulässige Integrationswege anhand offizieller Anbieterquellen verifizieren.
4. Zebra-Druckpfad mit konkretem Druckermodell und Verbindung (Bluetooth Classic/WLAN/USB) praktisch testen; bei Bedarf native Druckbrücke statt Browser-only.
5. Erst danach Android-/Web-Synchronisierung und produktive Datenhaltung planen.

## Datenschutz und Sicherheit
- Keine Anbieterpasswörter, Session-Cookies oder Kundendaten an einen TRAKUNA-Server übertragen.
- Nur HTTPS-Portaladressen akzeptieren.
- Lokale Demo-Daten bleiben auf dem Gerät/Browser; keine Behauptung einer Cloud-Sicherung oder Mehrgeräte-Synchronisierung.
- Originaletiketten und insbesondere Frankierung/Barcodes nicht verändern.

## Status
Der Web-Prototyp enthält wieder die ursprünglichen Paketshop-Kernabläufe als lokale Demo sowie Barcode-Erkennung, Anbieter-Routing und Etikettenübergabe. Er ist nicht mit der Android-Datenhaltung synchronisiert. Kamera, reale Carrier-Barcodes, Portaladressen und Zebra-Direktdruck sind noch nicht auf dem Android-Gerät praktisch getestet.
