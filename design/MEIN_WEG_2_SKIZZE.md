# Mein Weg 2.0 – Oberflächenskizze

## Designrichtung
Drei klare Ansichten wie in der abgestimmten visuellen Skizze: **Start**, **Verbindungen**, **Meine Fahrt**.

- **Start:** nächster Kalendertermin als Karte, Zieladresse, Ankunftspuffer als Slider (5–60 Minuten), kompakte Filterchips für Linien und ein prominenter Suchknopf. Weitere Termine und Gehgeschwindigkeit nur bei Bedarf aufklappen.
- **Verbindungen:** höchstens drei Vorschläge, nach passender Ankunft sortiert. Abfahrt und Ankunft groß, Linie und Fußweg als kompaktes Informationsraster. Ein Knopf öffnet die Fahrt.
- **Meine Fahrt:** Countdown zur Busabfahrt, Einstiegshaltestelle, geschätzter Fußweg und Knopf zur echten Fußweg-Navigation.

## Umsetzung und Grenzen
Die erste Compose-Umsetzung verwendet Material 3, Card, FilterChip, Slider und reduzierte Textblöcke. Eine eigenständige Kartenansicht mit Abbiegehinweisen und laufend aktualisierten Metern ist noch nicht implementiert; dafür werden echte Weggeometrie und eine bewusst gestartete Positionsführung benötigt. Es dürfen keine scheinbar präzisen Navigationshinweise erfunden werden.

## Gestaltungsfarben
Violett als primäre Akzentfarbe, helle Flächen, großzügige Rundungen und gut lesbare Zahlen. Das Bild aus dem Chat ist die visuelle Referenz; diese Datei dokumentiert die Skizze und die technische Abgrenzung.
