# TRAKUNA – offene Druckerschnittstelle

## Produktanforderung

TRAKUNA soll Drucker möglichst herstellerunabhängig über WLAN/LAN und Bluetooth anbinden können. Die App darf nicht auf Zebra oder einen einzelnen Druckertyp festgelegt werden. Unterstützte Geräte sollen über eine gemeinsame Drucker-Verwaltung gefunden, verbunden, geprüft und später über passende Protokollmodule angesprochen werden.

„Alle Geräte“ ist ein Kompatibilitätsziel, keine pauschale technische Garantie: Ein Drucker ist nur integrierbar, wenn er über eine dokumentierte, erreichbare Schnittstelle und ein unterstütztes Druck-/Datenformat verfügt. Proprietäre Apps, geschlossene Pairing-Verfahren und herstellerspezifische Protokolle benötigen gegebenenfalls eigene Adapter.

## Architektur

### 1. Transportebene
- WLAN/LAN: Geräte im lokalen Netzwerk erkennen, sofern sie Discovery erlauben; direkte Verbindung über IP/Hostname.
- IPP/IPPS: bevorzugter standardisierter Netzwerk-Druckweg, wenn vom Gerät unterstützt.
- RAW TCP (häufig Port 9100) und LPR nur als optionale, explizit konfigurierte Kompatibilitätswege.
- Bluetooth Classic (SPP/RFCOMM) und BLE nur dort, wo das Gerät ein geeignetes Profil bzw. GATT-Protokoll bereitstellt.
- USB ist nicht Teil der aktuellen WLAN/Bluetooth-Anforderung; kann später als separater Adapter ergänzt werden.

### 2. Druckersprachen-/Formatadapter
Transport und Druckersprache strikt trennen. Adapter bei Bedarf für ZPL, EPL, CPCL, ESC/POS, PDF bzw. standardisierte IPP-Dokumentformate. Nicht annehmen, dass jeder Drucker jedes Format versteht. Fähigkeiten pro Gerät speichern/anzeigen, nicht durch bloße Herstellererkennung erraten.

### 3. Gemeinsame Drucker-Schnittstelle
Einheitliche Funktionen für:
- Geräte suchen/hinzufügen und manuell per IP/Verbindungsart konfigurieren
- verbinden/trennen und Verbindung prüfen
- unterstützte Funktionen/Formate anzeigen
- Status und verständliche Fehler melden
- später einen ausdrücklich freigegebenen Test-/Druckauftrag über einen passenden Adapter senden

## Android-Berechtigungen und Sicherheit
- WLAN-Erkennung und Bluetooth benötigen versionsabhängige Android-Berechtigungen; nur bei tatsächlicher Nutzung anfordern.
- Bluetooth-Pairing und Netzwerkzugriff transparent durch den Nutzer bestätigen lassen.
- Keine Druckerpasswörter oder Zugangsdaten protokollieren.
- Keine Druckaufträge automatisch beim Verbinden senden.
- Netzwerkgeräte nicht unkontrolliert scannen; lokale Netzwerkerkennung nur nach Nutzeraktion.
- Verbindungsstatus und tatsächliche Druckfähigkeit unterscheiden.

## Stufenweise Umsetzung und Abnahme

1. **Bestandsaufnahme:** Android-Zielversionen, vorhandene Bibliotheken, Berechtigungen und Build-Workflow prüfen.
2. **Geräteverwaltung:** manuelles Hinzufügen, WLAN-/Bluetooth-Verbindung und Statusanzeige, zunächst ohne Druckauftrag.
3. **Standardprotokolle:** IPP/IPPS priorisieren; weitere Transportwege nur mit dokumentiertem Geräteprofil.
4. **Formatadapter:** erst nach verifizierter Gerätefähigkeit.
5. **Tests:** simulierte Geräte/Protokolle, danach reale Hardware je Modell und Transportweg; Ergebnisse pro Modell dokumentieren.

## Aktueller Projektstand / Grenze

Die bestehende TRAKUNA-Test-App hat derzeit keine implementierte Druckerverbindung. Diese Datei dokumentiert die Zielarchitektur; sie ist keine Behauptung, dass WLAN, Bluetooth oder Drucken bereits funktionieren. Bis zu einer ausdrücklichen Freigabe bleibt die App ein Scanner-/Weiterleitungs-Test. Es werden keine echten Paketetiketten erzeugt oder gedruckt und keine Paketvorgänge ausgelöst.
