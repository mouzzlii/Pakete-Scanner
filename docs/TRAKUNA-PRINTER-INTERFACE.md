# TRAKUNA – offene, herstellerunabhängige Druckerschnittstelle

## Verbindliche Produktanforderung

TRAKUNA soll Drucker verschiedener Hersteller über WLAN/LAN oder Bluetooth anbinden können. Es gibt keine Festlegung auf Zebra. Die gemeinsame Geräteverwaltung soll Geräte finden bzw. manuell hinzufügen, verbinden, trennen, Fähigkeiten anzeigen und den Verbindungs-/Gerätestatus verständlich darstellen.

**Kompatibilitätsversprechen:** „Offen für alle Geräte“ ist ein Architekturziel, keine Garantie, dass jeder Drucker ohne Adapter funktioniert. Eine Integration ist nur möglich, wenn das Gerät ein dokumentiertes und erreichbares Protokoll bzw. Profil anbietet. Geschlossene Herstellerprotokolle brauchen einen eigenen Adapter oder einen installierten Android-Druckdienst.

## Architekturprinzip: Transport ≠ Druckersprache

Die Verbindung (WLAN/Bluetooth) und das Datenformat (z. B. PDF/ZPL/ESC-POS) werden getrennt behandelt. Ein Gerät gilt nicht allein deshalb als druckfähig, weil eine Funkverbindung besteht.

### 1. WLAN/LAN

Priorität:
1. **IPP/IPPS** als bevorzugter offener Netzwerkstandard, wenn vom Drucker unterstützt. IPP kann Gerätefähigkeiten und Status abfragen; IPPS ergänzt Transportverschlüsselung.
2. **Android-Druckdienste / Systemdruck-Framework** als Kompatibilitätsweg für Geräte, deren Hersteller oder Mopria/AirPrint-kompatible Druckdienste bereits Android-seitig eingebunden sind.
3. **DNS-SD/mDNS** zur optionalen lokalen Erkennung kompatibler Druckdienste.
4. **RAW TCP (z. B. Port 9100) und LPR/LPD** nur als optionale, explizit konfigurierte Legacy-Adapter. Keine automatische Übertragung unbekannter Daten.

IP-Adresse/Hostname kann manuell eingetragen werden. Lokale Netzwerkerkennung erfolgt ausschließlich nach einer Nutzeraktion. Netzwerkverbindung und tatsächliche Druckerbereitschaft sind getrennte Zustände.

### 2. Bluetooth

- **Bluetooth Classic (SPP/RFCOMM):** nur für Geräte, die ein passendes serielles Profil und ein bekanntes Protokoll bereitstellen.
- **Bluetooth Low Energy (BLE):** nur mit dokumentiertem GATT-Service/Characteristic-Profil des Geräts.
- Es gibt kein universelles Bluetooth-Druckprotokoll, das jedes Modell gleich ansprechen würde. Deshalb sind Geräte-/Herstelleradapter nötig.
- Gekoppelte Geräte können als erster sicherer Schritt angezeigt werden. Scannen/Koppeln/Verbinden erst nach verständlicher Nutzeraktion und den erforderlichen Android-Berechtigungen.

### 3. Druckformat-Adapter

Mögliche Adapter – nur wenn das konkrete Gerät das Format bestätigt:
- PDF / PWG Raster / JPEG für standardisierte Dokumentausgabe
- ZPL / EPL / CPCL für bestimmte Etikettendrucker
- ESC/POS für bestimmte Belegdrucker
- PCL und weitere Herstellerformate nur bei nachgewiesener Unterstützung

Nicht aus dem Markennamen oder einer erfolgreichen Bluetooth-/WLAN-Verbindung auf unterstützte Formate schließen.

## Gemeinsamer Schnittstellenvertrag

Jeder Adapter implementiert dieselben konzeptionellen Operationen:
- `discover(userInitiated)` – kompatible Geräte suchen
- `addManual(connectionDetails)` – Gerät manuell hinzufügen
- `connect(device)` / `disconnect(device)`
- `getConnectionState(device)`
- `getCapabilities(device)`
- `getStatus(device)`
- `sendTestPayload(device, payload)` – erst in einer späteren, ausdrücklich freigegebenen Druckphase

Statusmodell:
- Nicht eingerichtet
- Gefunden / gekoppelt
- Verbindung wird hergestellt
- Verbunden
- Druckerstatus unbekannt
- Bereit / Papier fehlt / Fehler (nur wenn das Protokoll diesen Status tatsächlich liefert)
- Nicht erreichbar / Berechtigung fehlt

**Keine Scheinerfolge:** „Verbunden“ bedeutet nicht „druckbereit“. Ein Testpayload ist nicht Teil der ersten Verbindungsstufe.

## Android-Sicherheit und Berechtigungen

- Bluetooth ab Android 12: `BLUETOOTH_SCAN` für Suche und `BLUETOOTH_CONNECT` für Kommunikation mit gekoppelten Geräten; nur die tatsächlich benötigten Berechtigungen anfordern. `BLUETOOTH_ADVERTISE` ist für diese App nicht vorgesehen.
- Ältere Android-Versionen benötigen die jeweils passenden Legacy-Berechtigungen; die Implementierung muss SDK-abhängig sein.
- WLAN/LAN-Zugriff benötigt passende Netzwerkberechtigungen. Ab Android 17 / Target SDK 37 ist zusätzlich `ACCESS_LOCAL_NETWORK` als Laufzeitberechtigung relevant. Die aktuelle Test-App besitzt die INTERNET-Berechtigung ausschließlich für den vom Nutzer gestarteten TCP-Erreichbarkeitstest eines manuell eingegebenen Endpunkts; sie führt keine automatische Netzwerkerkennung oder Portsuche durch.
- Keine Hintergrundsuche und kein breitflächiger Portscan.
- Keine Speicherung von WLAN-Schlüsseln, Druckerpasswörtern oder Zugangsdaten im Klartext.
- Verbindungen werden sichtbar und auf Nutzerwunsch hergestellt; keine automatischen Druckaufträge beim Verbinden.

## Stufenplan

1. **Spezifikation und Kompatibilitätsmatrix** – abgeschlossen mit diesem Dokument und dem Testplan.
2. **Geräteverwaltung ohne Druckaufträge** – erste begrenzte Testimplementierung vorhanden: manuelle TCP-Erreichbarkeitsprüfung und Anzeige gekoppelter Bluetooth-Geräte mit SPP-Verbindungsversuch. Vollständige Geräteprofile, IPP-Status und Hardwarevalidierung stehen noch aus.
3. **WLAN-Standardadapter** – IPP/IPPS und unterstützte Discovery.
4. **Bluetooth-Adapter** – zuerst SPP für nachweislich kompatible Geräte; BLE pro dokumentiertem Gerätemodell.
5. **Formatadapter** – erst nach Gerätefähigkeitsprüfung.
6. **Druckfunktion** – separates Freigabe-/Abnahmetor; nicht Teil der aktuellen Teststufe.

## Projektgrenze und aktueller Status

Die vorhandene TRAKUNA-Test-App enthält bereits begrenzte Verbindungstests: manuelle TCP-Erreichbarkeitsprüfung eines eingegebenen WLAN-/LAN-Endpunkts sowie Anzeige gekoppelter Bluetooth-Geräte und einen ausdrücklichen Bluetooth-Classic-SPP-Verbindungsversuch. Diese Tests senden keine Druckdaten und liefern keine IPP-Geräteattribute. Sie beweisen weder Druckerkompatibilität noch Druckbereitschaft. IPP/IPPS, mDNS/DNS-SD, LPR, RAW-TCP-Geräteprofile, BLE/GATT und Druckformatadapter sind noch nicht implementiert. Hardwaretests stehen aus.

Die laufende Teststufe bleibt Scanner und Weiterleitung zu externen Anbieterportalen. Es werden keine echten Paketetiketten erstellt oder gedruckt und keine Paketvorgänge ausgelöst. Die erste spätere Drucker-Ausbaustufe darf ausschließlich Geräteverwaltung und Verbindungstests enthalten, bis die Druckfunktion separat freigegeben wird.
