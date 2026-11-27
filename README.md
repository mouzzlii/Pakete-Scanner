# TRAKUNA – Android-Testversion

TRAKUNA ist in dieser Teststufe ein schlanker Barcode-Scanner und Weiterleitungshelfer für ausgewählte Geschäftskunden-Einstiege von Paketdiensten.

## Ablauf

1. Barcode mit dem Smartphone-Kamera-Scanner erfassen oder über einen externen HID-/Keyboard-Wedge-Scanner in das Codefeld eingeben.
2. TRAKUNA zeigt einen möglichen Anbieter an; der Kamera-Scan öffnet noch kein Portal.
3. **„ANBIETER ERKENNEN & PORTAL ÖFFNEN“** antippen, um den erkannten Anbieter-Einstieg ausdrücklich zu öffnen.
4. TRAKUNA ordnet nur eindeutig unterstützte Kennzeichen bzw. Barcode-Muster zu. Bei unbekannten oder mehrdeutigen Codes erfolgt keine automatische Zuordnung.
5. Bei eindeutiger Erkennung öffnet TRAKUNA den hinterlegten Anbieter-Einstieg im externen Android-Browser. Andernfalls kann der Anbieter manuell ausgewählt werden.
6. Anmeldung und alle weiteren Schritte erfolgen ausschließlich beim jeweiligen Anbieter.

**Wichtig:** TRAKUNA fügt den Barcode nicht automatisch in das Anbieterportal ein und überträgt ihn nicht an einen TRAKUNA-Server. Der Code bleibt in der App sichtbar.

## Unterstützte Anbieter-Einstiege

- DPD Business
- GLS / YourGLS
- Hermes Geschäftskundenbereich
- UPS Business / CampusShip
- DHL Geschäftskunden
- Österreichische Post / ELLA

Die Portaladressen können sich ändern. Vor einem produktiven Einsatz müssen sie aktuell geprüft und auf den jeweiligen Shop-Prozess abgestimmt werden.

## Grenzen der automatischen Erkennung

Die aktuelle Implementierung nutzt diese begrenzten Kandidaten:

- Hermes: `104` gefolgt von 19 Ziffern.
- DPD: `P` gefolgt von 12 Ziffern.
- UPS: `1Z` gefolgt von 16 alphanumerischen Zeichen.
- Klar erkennbare Anbieterkennzeichnungen im eingegebenen Text können ebenfalls zugeordnet werden.

Ein 13-stelliger Code mit dem Präfix `872` wird bewusst **nicht automatisch GLS zugeordnet**: Der bekannte Beispielcode `8721335813708` erfüllt zugleich die EAN-13-Prüfziffer. Er kann daher auch ein Produktbarcode sein. In diesem Fall muss GLS manuell ausgewählt werden.

Diese Muster decken nicht alle nationalen und internationalen Sendungsnummern ab und sind kein Beweis für die tatsächliche Zugehörigkeit eines Codes zu einem Paketdienst. DHL und Österreichische Post werden nicht anhand allgemeiner Nummernformate automatisch zugeordnet. Bei Unsicherheit bitte manuell auswählen.

## Eingabeprüfung beim Drucker-Verbindungstest

Vor dem TCP-Verbindungsversuch prüft TRAKUNA die manuelle Host-/Port-Eingabe. Unterstützt werden Hostnamen sowie IPv4- und IPv6-Adressen. URL-Schemata, Pfade, ungültige Zeichen und Ports außerhalb von 1 bis 65535 werden abgewiesen. Die Prüfung selbst stellt noch keine Verbindung her.

Die Netzwerkprüfung läuft außerhalb des UI-Threads und verwendet ein Verbindungszeitlimit von drei Sekunden. Aus Sicherheitsgründen werden ausschließlich lokale/private Zieladressen zugelassen; öffentliche IP-Adressen und Hostnamen, die auf öffentliche Adressen zeigen, werden abgewiesen. Die bereits geprüfte IP-Adresse wird direkt verbunden, damit keine zweite DNS-Auflösung dazwischenfunkt. Ein erfolgreicher TCP-Verbindungsaufbau bestätigt ausschließlich die Erreichbarkeit dieses Endpunkts – nicht IPP, IPPS oder die Druckbereitschaft des Geräts.

## Drucker – aktueller Umfang

- WLAN/LAN: manuelle TCP-Erreichbarkeitsprüfung eines selbst eingegebenen Hosts und Ports.
- Bluetooth: Anzeige bereits gekoppelter Geräte und ein ausdrücklicher Bluetooth-Classic-SPP-Verbindungsversuch.
- Es werden keine Druckdaten oder Etiketten gesendet oder erzeugt.
- Eine erreichbare TCP-Adresse oder erfolgreiche SPP-Verbindung beweist weder IPP-Unterstützung noch Druckbereitschaft.
- IPP/IPPS, mDNS/DNS-SD, LPR, BLE/GATT und Druckformatadapter sind noch nicht implementiert.

## Sicherheits- und Funktionsgrenzen

- Keine Passwörter oder Zugangsdaten in TRAKUNA.
- Keine Paketannahme, Abholung, Buchung, Versandlabel-Erstellung, Etikettenbestellung oder Carrier-Aktion.
- Keine Speicherung gescannter Paketdaten auf einem TRAKUNA-Server.
- Die Android-App besitzt die INTERNET-Berechtigung ausschließlich für den vom Nutzer gestarteten TCP-Verbindungstest zum manuell eingegebenen Netzwerk-Endpunkt.
- Der externe HID-Scanner wird als Tastatureingabe unterstützt; es gibt keine native Zebra-SDK-Integration.
- Kein Produktivbetrieb und keine Produktivfreigabe durch den erfolgreichen Build.

## Android-Projekt lokal bauen

Der Android-Quellcode liegt jetzt als normales Gradle-Projekt im Repository und wird nicht mehr bei jedem CI-Lauf aus einem Workflow-Heredoc erzeugt.

Voraussetzungen: Android Studio (empfohlen), JDK 17 und Android SDK 35.

- Repository in Android Studio öffnen.
- Gradle-Synchronisierung abwarten.
- Für lokale Tests: `gradle testDebugUnitTest`.
- Für statische Android-Prüfungen: `gradle lintDebug`.
- Für eine Debug-APK: `gradle assembleDebug`.

GitHub Actions führt diese drei Schritte ebenfalls getrennt aus und veröffentlicht die Debug-APK als Build-Artefakt. Die Unit-Tests prüfen ausschließlich die lokale Erkennungslogik; sie ersetzen keine Kamera-, Browser-, Bluetooth-, Netzwerk- oder Gerätetests.

## Teststatus

- GitHub Actions Debug-APK: Build erfolgreich für den zuletzt dokumentierten Workflow-Lauf; siehe [GitHub Actions](https://github.com/mouzzlii/Pakete-Scanner/actions/workflows/trakuna-2.0.yml).
- Der Nutzer hat am 27.11.2026 einen realen Android-Gerätetest im Paketshop durchgeführt. Die konkreten Beobachtungen sind im derzeit zugänglichen Projektprotokoll nicht vollständig erfasst; daraus wird kein pauschales „bestanden“ abgeleitet.
- Weitere Kamera-/HID-Tests und konkrete Drucker-Hardwaretests müssen separat dokumentiert werden.
- Der CI-Build belegt nur Kompilierung und Artefakterstellung, nicht die Funktion auf einem konkreten Android-Gerät oder mit einem konkreten Druckermodell.
- Für einen kleinen internen Test gibt es die [Mitarbeiter-Testanleitung](docs/TRAKUNA-SHOP-MITARBEITERTEST.md).

## Rechte und Vertraulichkeit

Siehe [PROPRIETARY-NOTICE.md](PROPRIETARY-NOTICE.md). Keine unveröffentlichten Erfindungsdetails, Kundendaten, Passwörter, API-Schlüssel oder privaten Signaturschlüssel in dieses Repository eintragen.
