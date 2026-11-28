# TRAKUNA 2.0 – Druckpfad

## Aktueller Stand

- Die App bietet unter „Sendungen suchen / Abholung“ den Einstieg „Drucker einrichten“.
- Dieser öffnet die Android-Systemseite für Druckeinstellungen (Settings.ACTION_PRINT_SETTINGS). Dort kann ein kompatibler Android-Druckdienst aktiviert und der vorgesehene einzelne Drucker eingerichtet werden.
- Ein PDF-Versandetikett kann über den Android-Dateiauswahldialog ausgewählt werden.
- Der aktuelle Adapter unterstützt genau eine PDF-Seite und bereitet sie auf eine Ausgabegröße von 100 × 150 mm vor.
- Der Android-Druckdialog wird bewusst sichtbar geöffnet. Die App behauptet nicht, dass der Drucker bereits dauerhaft gespeichert ist oder dass ohne weitere Auswahl gedruckt wird.
- Es gibt keine automatische Wiederholung bei unklarem Druckstatus.
- Temporäre PDF-Dateien werden nach Abschluss des Druckvorgangs oder bei einem Einrichtungsfehler gelöscht.

## Grenzen und Risiken

- Die App kann ohne konkretes Druckermodell und passenden Android-Druckdienst keine universelle Direktverbindung zu Bluetooth-, USB- oder WLAN-Etikettendruckern garantieren.
- Die Android-Druckeinstellungen können einen Dienst und Drucker konfigurieren; TRAKUNA speichert derzeit aber keine eigene, verbindliche Drucker-ID und erzwingt nicht, dass ausschließlich ein physischer Drucker verwendet wird.
- Android kann im Druckdialog weiterhin eine Druckerauswahl oder weitere Bestätigung verlangen. Eine vollständig automatische Ein-Drucker-Übergabe ist nicht implementiert.
- Die PDF-Seite wird derzeit gerastert. Seitenverhältnis und vollständiger Seiteninhalt bleiben erhalten; dadurch kann ein Etikett mit viel Weißraum jedoch kleiner als erwartet ausfallen.
- Die Zielauflösung 300 dpi ist eine Aufbereitungsgröße, keine Garantie für die tatsächliche Druckauflösung des Geräts.
- Barcode-/QR-Lesbarkeit, Ruhezonen, Druckkontrast und Druckqualität müssen mit echten Etiketten und dem später eingesetzten Drucker praktisch geprüft werden. Bis dahin darf keine produktive Lesbarkeit behauptet werden.

## Nächste technische Prüfschritte

1. Android-Gerät: „Drucker einrichten“ öffnen und einen kompatiblen Druckdienst aktivieren.
2. Mit einem realen 100 × 150-mm-Etikettendrucker prüfen, ob der Druckdialog das Format korrekt anbietet.
3. Ein- und mehrseitige PDFs testen; mehrseitige PDFs müssen derzeit mit einer verständlichen Fehlermeldung abgewiesen werden.
4. Reale Testetiketten mit Barcode-/QR-Scanner gegenprüfen; insbesondere Ruhezone und Modulbreite.
5. Druckauftrag abbrechen, Druckdienst trennen und Verbindungsfehler testen. Niemals bei unklarem Ergebnis automatisch erneut drucken.

## Sicherheits- und Produktgrenzen

- Kein automatischer Versandlabelabruf und keine Carrier-API.
- Keine automatische Datenübertragung an Paketdienste.
- Kein stiller Druck und keine automatische Wiederholung.
- Keine Mehrdruckerverwaltung.
- main bleibt unverändert; Umsetzung erfolgt ausschließlich im Entwurfszweig und Draft-PR.


## Statische Codeprüfung – 28.11.2026

Der Druckpfad wurde zusätzlich gegen die Android-API-Referenz und die im Workflow generierte Kotlin-Implementierung geprüft:

- `MediaSize(..., 3937, 5906)` entspricht gerundet 100 × 150 mm in Android-Mils.
- Die Raster-Zielgröße 1181 × 1772 Pixel entspricht gerundet 300 dpi für 100 × 150 mm.
- Die Ausgabe-PDF-Seite nutzt 283 × 425 PDF-Punkte. Das ist die nächstliegende ganzzahlige Punktgröße zu 100 × 150 mm und weicht geringfügig nach unten ab (unter 0,2 %).
- Der Code verwendet eine einheitliche `minOf`-Skalierung und zentriert den gesamten Seiteninhalt. Es wird nicht absichtlich gestreckt oder beschnitten; vorhandene Ruhebereiche werden zusammen mit dem Etiketteninhalt skaliert.
- `PdfRenderer.Page.render(..., null, null, RENDER_MODE_FOR_PRINT)` passt laut Android-API-Referenz die gesamte Seite in die Ziel-Bitmap ein, wenn keine Transformationsmatrix angegeben ist: https://developer.android.com/reference/android/graphics/pdf/PdfRenderer.Page#render(android.graphics.Bitmap,android.graphics.Rect,android.graphics.Matrix,int).

**Grenze:** Dies ist eine statische Code- und Maßprüfung. Sie beweist weder, dass ein bestimmter Druckdienst die Attribute übernimmt, noch dass Barcode-/QR-Codes nach dem Druck mit ausreichender Modulbreite, Ruhezone und Kontrast tatsächlich lesbar sind. Dafür bleibt ein Test mit dem späteren physischen Etikettendrucker und einem unabhängigen Scanner zwingend. Es wurde keine Codeänderung vorgenommen.
