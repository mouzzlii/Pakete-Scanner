# TRAKUNA – BB-Testversion

TRAKUNA ist in dieser Teststufe eine schlanke Weiterleitung zu den Geschäftskunden-Einstiegen der Paketdienste.

## Ablauf

1. Barcode mit der Smartphone-Kamera scannen oder über einen externen Scanner im Tastaturmodus (HID/Keyboard-Wedge) in das Codefeld einlesen.
2. TRAKUNA erkennt den Paketdienst nur anhand eindeutiger Kennzeichen bzw. bekannter Formate.
3. Bei unklaren Codes wird nichts automatisch zugeordnet; der Anbieter kann manuell gewählt werden.
4. Der passende Anbieter-Einstieg wird im externen Android-Browser geöffnet. Anmeldung und alle weiteren Schritte erfolgen ausschließlich beim Anbieter.

## Sicherheits- und Funktionsgrenzen

- TRAKUNA speichert keine Anbieterpasswörter und übermittelt keine Zugangsdaten.
- Die Android-App hat keine eigene Internetberechtigung; Webseiten werden mit einer externen Browser-App geöffnet.
- Keine Paketannahme, Abholung, Buchung, Versandlabel-Erstellung, Etikettenbestellung oder Druckaktion in TRAKUNA.
- Keine Speicherung gescannter Paketdaten auf einem Server.
- Die Hermes-Auswahl öffnet den Hermes-Geschäftskundenbereich. Ein separater, öffentlich zugänglicher Login für das Geschäftskundenportal konnte nicht eindeutig verifiziert werden.
- Der externe Zebra-Scanner wird in dieser Teststufe nur unterstützt, wenn er Barcodes als Tastatureingabe (HID/Keyboard-Wedge) sendet. Es ist keine native Zebra-SDK-Integration.
- Zebra-/Etikettendrucker sind in dieser Version noch nicht angebunden; es wird nichts gedruckt.

## Grenzen der automatischen Erkennung

Die Erkennung nutzt nur eindeutige Textkennzeichen und bekannte Formate:
- Hermes: 14 Ziffern oder H gefolgt von 19 Ziffern.
- GLS YourGLS Track-ID: Z gefolgt von 7 alphanumerischen Zeichen.

Diese Muster sind keine vollständige Erkennung aller Paketlabel. Unbekannte oder mehrdeutige Formate bleiben bewusst unerkannt. Insbesondere wird eine beliebige 11-stellige Nummer nicht automatisch GLS zugeordnet. Für die beiden zuvor nicht erkannten GLS- und Hermes-Testpakete liegen in diesem Repository keine originalen Barcode-Inhalte vor; die Erkennung dieser konkreten Labels ist daher noch nicht nachgewiesen.

## Teststatus

- GitHub Actions Debug-APK-Build: erfolgreich für Commit `f62ad27aa13b082f166e83296eb98ed0256fdce5`.
- APK: Debug-/Testversion, nicht produktionsreif.
- Android-Gerätetest, Kamera-Scan mit echten GLS-/Hermes-Labels, Zebra-HID-Test und Druckertest: noch nicht durchgeführt.
- Der erfolgreiche Build belegt nur, dass das Android-Projekt kompiliert und die APK erstellt wurde; er belegt nicht, dass alle Anbieterportale auf jedem Gerät korrekt öffnen.

## Rechte und Vertraulichkeit

Siehe [PROPRIETARY-NOTICE.md](PROPRIETARY-NOTICE.md). Keine unveröffentlichten Erfindungsdetails, Kundendaten, Passwörter, API-Schlüssel oder privaten Signaturschlüssel in dieses Repository eintragen.
