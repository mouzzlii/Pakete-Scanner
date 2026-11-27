# TRAKUNA – Paketshop-Domänenmodell und Erweiterungsarchitektur

## Ziel
Eine gemeinsame Paketshop-Oberfläche soll mehrere Paketdienste und später weitere Anbieter unterstützen. Die Paketverwaltung darf nicht von einem einzelnen Carrier oder dessen Sendungsnummernformat abhängig sein.

## Bereits angelegt: gemeinsamer Fachkern
`ParcelRecord` enthält ausschließlich:
- interne Vorgangs-ID
- Barcode / Sendungskennung
- stabile Carrier-ID
- Status
- Erstellungs- und Änderungszeitpunkt
- optionalen internen Lagerplatz

Empfängername, Anschrift, Telefonnummer, Abhol-PIN und andere Kundendaten gehören bewusst nicht in dieses Kernmodell. Wenn ein späterer realer Shop-Prozess zusätzliche Daten benötigt, muss deren Notwendigkeit, Zugriffsschutz, Aufbewahrung und Löschung gesondert festgelegt werden.

## Statusmodell
- `RECEIVED`: Paket angenommen
- `STORED`: an einem Lagerplatz eingelagert
- `READY_FOR_PICKUP`: zur Abholung bereit
- `HANDED_OVER`: Paket ausgegeben
- `RETURN_RECEIVED`: Retoure angenommen
- `RETURNED_TO_CARRIER`: Retoure an den Dienstleister übergeben

Erlaubte Übergänge:
- `RECEIVED → STORED → READY_FOR_PICKUP → HANDED_OVER`
- `RETURN_RECEIVED → RETURNED_TO_CARRIER`

Direkte Sprünge, Wiederöffnung abgeschlossener Vorgänge und rückwärts laufende Zeitstempel werden abgewiesen. Vor dem Einlagern muss ein Lagerplatz angegeben werden. Ein offener Barcode darf nicht doppelt erfasst werden; nach Abschluss kann dieselbe Kennung für einen neuen Vorgang erneut verwendet werden.

Das Modell ist aktuell eine getestete Domänenlogik. Eine persistente Speicherung und eine sichtbare Paketshop-Oberfläche sind noch nicht angeschlossen. Es darf daher noch nicht als nutzbare Paketannahme oder echte Lagerverwaltung bezeichnet werden.

## Carrier-Erweiterung
Jeder Anbieter implementiert `CarrierAdapter` mit:
- stabiler, kleingeschriebener ID
- Anzeigename
- offiziellem Portal-Einstieg

`CarrierRegistry` ist die zentrale Registrierung. Neue Anbieter sollen als Adapter ergänzt werden; das neutrale Paketmodell muss dafür nicht verändert werden. Die derzeitigen sechs Einträge sind Portal-Einstiege, keine API-Integrationen. Barcode-Erkennung bleibt vorsichtig und ist kein Identitätsnachweis für eine Sendung.

## Zusätzlich implementiert: lokale Demo-Verwaltung
- `ParcelDatabase` speichert Paketvorgänge lokal in einer SQLite-Datenbank auf dem Android-Gerät.
- Ein eindeutiger Datenbankindex verhindert doppelte offene Barcodes auch bei parallelen Schreibversuchen.
- `ParcelShopSection` bietet eine einfache Oberfläche zur Paket-/Retourenannahme, Auswahl eines registrierten Paketdienstes, Eingabe eines Lagerplatzes und Statusfortschaltung bis zur Übergabe.
- Die UI kennzeichnet sich ausdrücklich als lokale Demo ohne Cloud-Synchronisierung und ohne Carrier-API.
- Die Datenbank speichert keine Empfängernamen, Anschriften, Telefonnummern oder Abhol-PINs.

## Weitere Ausbaustufen
1. Persistenztests auf Android-Instrumentierungsebene, Datenbankmigrationen und dokumentiertes Backup-/Löschverhalten.
2. Ausgabeprozess mit geeigneter, datensparsamer Berechtigungsprüfung; keine selbst erfundenen Abhol-PIN-Sicherheitsversprechen.
3. Carrier-spezifische API-Adapter nur nach Prüfung offizieller Dokumentation, Vertrags-/Zugriffsvoraussetzungen und Testumgebung.
4. Druckeradapter getrennt nach Transport, Protokoll und Druckformat; Druckfunktion erst nach Modelltests.
5. Auditierbare Shop-Rollen, Rechte, Fehlerbehandlung, Wiederherstellungs- und Löschkonzept vor Produktivbetrieb.

## Testgrenzen
Unit-Tests prüfen die reine Status- und Validierungslogik. Sie beweisen weder Persistenz noch Android-Geräteverhalten, Carrier-API-Zugriff, Etikettendruck oder Produktivfähigkeit. Jede dieser Ebenen benötigt eigene Tests und eine dokumentierte Freigabe.


## Datenbank-Härtung (Teststand)

- SQLite bleibt ausschließlich lokal auf dem Android-Gerät; es gibt keine Cloud-Synchronisierung.
- Statusänderungen verwenden einen Compare-and-set-Updatefilter (`id`, bisheriger Status und bisheriger Zeitstempel). Dadurch kann eine zweite Datenbankinstanz keine zwischenzeitlich erfolgte Änderung still überschreiben.
- Nicht implementierte Schema-Upgrades und Downgrades brechen bewusst ab, statt eine unbekannte Datenbankstruktur scheinbar erfolgreich zu öffnen. Vor jeder künftigen Versionsanhebung ist eine getestete Migration erforderlich.
- Robolectric-Tests prüfen Persistenz nach erneutem Öffnen, doppelte offene Barcodes, Wiederverwendung nach abgeschlossener Übergabe, unzulässige Statuswechsel und fehlende Lagerplätze.
- Diese Tests sind automatisierte JVM-/Robolectric-Tests und ersetzen keine Tests auf realen Android-Geräten. Verschlüsselung, Backup/Wiederherstellung, Lösch- und Aufbewahrungsregeln sowie Mehrbenutzerbetrieb bleiben vor Echtdatenbetrieb offen.
