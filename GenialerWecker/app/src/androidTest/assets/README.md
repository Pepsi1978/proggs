# Audio-Testdatei

`test-song.mp3` ist ein eigens erzeugter, 30 Sekunden langer und stark abgesenkter
Sinuston (523 Hz). Er prüft, dass eine echte MP3 vollständig abgespielt wird und
der Weckablauf erst danach zum nächsten Schritt wechselt.

Erzeugung: `ffmpeg -f lavfi -i sine=frequency=523:duration=30 -af volume=0.05 -codec:a libmp3lame -q:a 8 test-song.mp3`

Keine Aufnahme, kein fremdes Musikstück, keine Lizenz eines Dritten erforderlich.
