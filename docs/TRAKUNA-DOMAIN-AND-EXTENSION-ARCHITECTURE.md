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

## Weitere Ausbaustufen
1. Persistenzschicht mit Migrationen, Integritätsschutz und explizitem Demo-Modus.
2. Paketannahme- und Lagerplatz-UI mit nachvollziehbaren Statusaktionen.
3. Ausgabeprozess mit geeigneter, datensparsamer Berechtigungsprüfung; keine selbst erfundenen Abhol-PIN-Sicherheitsversprechen.
4. Carrier-spezifische API-Adapter nur nach Prüfung offizieller Dokumentation, Vertrags-/Zugriffsvoraussetzungen und Testumgebung.
5. Druckeradapter getrennt nach Transport, Protokoll und Druckformat; Druckfunktion erst nach Modelltests.
6. Auditierbare Shop-Rollen, Rechte, Fehlerbehandlung, Backup-/Wiederherstellungs- und Löschkonzept vor Produktivbetrieb.

## Testgrenzen
Unit-Tests prüfen die reine Status- und Validierungslogik. Sie beweisen weder Persistenz noch Android-Geräteverhalten, Carrier-API-Zugriff, Etikettendruck oder Produktivfähigkeit. Jede dieser Ebenen benötigt eigene Tests und eine dokumentierte Freigabe.
