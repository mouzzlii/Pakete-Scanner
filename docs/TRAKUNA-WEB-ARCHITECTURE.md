# TRAKUNA Web-Hub – Architektur und Grenzen

## Ziel
Mobile-first Web-Oberfläche als zentrale Start- und Druckdrehscheibe. Fachliche Paketprozesse laufen im Originalportal des jeweiligen Dienstleisters.

## MVP-Stand
- Kamera-Barcode-Scanner über die Browser BarcodeDetector API, sofern Android/Chrome sie unterstützt; manuelle Eingabe als Fallback.
- Eindeutige Muster (z. B. UPS 1Z und UPU-S10 mit AT) können zum hinterlegten Anbieterportal routen. Mehrdeutige Muster verlangen Bestätigung.
- Erkennungsregeln sind ein konservativer Startbestand, keine vollständige oder offiziell bestätigte Carrier-Datenbank. Vor Produktivbetrieb mit echten Labels aller Zielanbieter validieren.
- Anbieterportale werden extern geöffnet; Login und Paketvorgänge bleiben beim Anbieter.
- Lokale Etikettenauswahl PDF/PNG/JPG und Druckdialog. Keine direkte ZPL-/Zebra-Steuerung.
- Keine Datenübertragung an einen TRAKUNA-Server; Portalkonfiguration liegt in localStorage.

## Grenzen und nächste Schritte
1. Carrier-Regeln anhand repräsentativer, rechtmäßig verfügbarer Etiketten je Anbieter testen und falsche Treffer vermeiden.
2. Browser-/Android-Kompatibilität des BarcodeDetector und Kamera-Berechtigungen auf realen Geräten testen.
3. Offizielle Anbieter-APIs/Partnerintegrationen und Bedingungen je Anbieter prüfen; fremde Portale nicht automatisiert auslesen oder fernsteuern.
4. Zebra-Verbindung mit konkretem Druckermodell, Android-Version und Verbindung (Bluetooth Classic/WLAN/USB) validieren; bei Bedarf native Druckbrücke.
5. Originaletiketten unverändert drucken; zusätzliche Kennzeichnung nur separat oder mit zulässiger Anbieter-Vorlage.

## Datenschutz
Keine Zugangsdaten, Session-Cookies oder Sendungsdaten an TRAKUNA übertragen. Nur HTTPS-Portaladressen zulassen. Etiketten bleiben lokal im Browser.

## Status
Lokaler Web-Prototyp; keine Live-Anbieterintegration und kein nachgewiesener Zebra-Direktdruck. Kamera und reale Etiketten sind noch nicht auf dem Android-Gerät getestet.
