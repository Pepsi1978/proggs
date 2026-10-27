# Meldet Frank am Mac ohne Passwort-Eingabe an: Steht der Anmeldebildschirm (nach dem Einschalten) oder der
# Sperrbildschirm, tippt dieser Dienst das gespeicherte Passwort über Apples Bildschirmfreigabe (VNC auf 127.0.0.1:5900)
# ein. Künstliche Tastendrücke anderer Programme nimmt der Sperrbildschirm nicht an, die der Bildschirmfreigabe schon.
# Apples "automatische Anmeldung" bleibt dabei aus, deshalb bleibt Touch ID an.
# Läuft als root (LaunchDaemon de.frank.mac-auto-anmelden), Passwort aus authrestart.plist (nur root lesbar).
require 'socket'
require 'openssl'
require 'json'
require 'securerandom'
require 'timeout'

ORDNER = '/Library/Application Support/MacOhnePasswort'
ZUGANG = "#{ORDNER}/authrestart.plist"
LOG = '/Library/Logs/MacAutoAnmelden.log'

def log(text)
  File.open(LOG, 'a') { |f| f.puts "[#{Time.now.strftime('%d.%m. %H:%M:%S')}] #{text}" }
rescue StandardError
  nil
end

def zugang
  j = JSON.parse(IO.popen(['plutil', '-convert', 'json', '-o', '-', ZUGANG], err: File::NULL, &:read))
  j['Username'] && j['Password'] ? [j['Username'], j['Password']] : nil
rescue StandardError
  nil
end

# :anmeldung (niemand angemeldet), :gesperrt, :offen
def zustand(benutzer)
  roh = IO.popen(['/bin/sh', '-c', 'ioreg -n Root -d1 -a | plutil -extract IOConsoleUsers json -o - -'], err: File::NULL, &:read)
  nutzer = JSON.parse(roh)
  ich = nutzer.find { |u| u['kCGSSessionUserNameKey'] == benutzer && u['kCGSSessionOnConsoleKey'] }
  return :anmeldung unless ich
  return :anmeldung unless ich['kCGSessionLoginDoneKey']
  ich['CGSSessionScreenIsLocked'] ? :gesperrt : :offen
rescue StandardError
  # Ohne angemeldete Sitzung fehlt IOConsoleUsers ganz.
  :anmeldung
end

def lesen(s, n)
  daten = s.read(n)
  raise 'Verbindung zu' if daten.nil? || daten.bytesize < n
  daten
end

# RFB-Sitzung mit Apple-Anmeldung (Sicherheitstyp 30, Diffie-Hellman + AES-128).
def verbinden(benutzer, passwort)
  s = TCPSocket.new('127.0.0.1', 5900)
  lesen(s, 12)
  s.write("RFB 003.889\n")
  typen = lesen(s, lesen(s, 1).ord).bytes
  raise "Bildschirmfreigabe bietet keine Apple-Anmeldung (#{typen})" unless typen.include?(30)
  s.write([30].pack('C'))
  g, laenge = lesen(s, 4).unpack('nn')
  p = OpenSSL::BN.new(lesen(s, laenge), 2)
  server = OpenSSL::BN.new(lesen(s, laenge), 2)
  geheim = OpenSSL::BN.new(SecureRandom.random_bytes(laenge), 2) % p
  oeffentlich = OpenSSL::BN.new(g.to_s).mod_exp(geheim, p)
  gemeinsam = server.mod_exp(geheim, p).to_s(2).rjust(laenge, "\0")
  feld = ->(t) { (t.b + "\0").ljust(64, "\0")[0, 64].tap { |f| (t.bytesize + 1...64).each { |i| f.setbyte(i, rand(256)) } } }
  c = OpenSSL::Cipher.new('aes-128-ecb').encrypt
  c.key = OpenSSL::Digest::MD5.digest(gemeinsam)
  c.padding = 0
  s.write(c.update(feld.call(benutzer) + feld.call(passwort)) + c.final + oeffentlich.to_s(2).rjust(laenge, "\0"))
  ergebnis = lesen(s, 4).unpack1('N')
  unless ergebnis.zero?
    grund = (lesen(s, lesen(s, 4).unpack1('N')) rescue '')
    raise "Anmeldung an der Bildschirmfreigabe abgelehnt: #{grund}"
  end
  s.write([1].pack('C')) # ClientInit, geteilt
  w, h = lesen(s, 4).unpack('nn')
  lesen(s, 16)
  lesen(s, lesen(s, 4).unpack1('N'))
  [s, w, h]
end

def taste(s, keysym)
  [1, 0].each { |runter| s.write([4, runter, 0, keysym].pack('CCnN')); sleep 0.04 }
end

def maus(s, x, y)
  s.write([5, 0, x, y].pack('CCnn'))
end

def eintippen(benutzer, passwort)
  # Ohne Zeitlimit könnte eine hängende Bildschirmfreigabe den Dienst für immer anhalten.
  s, w, h = Timeout.timeout(30) { verbinden(benutzer, passwort) }
  # Bildschirm wecken, Anmelde-/Sperrfenster Zeit geben.
  3.times { |i| maus(s, w / 2 + i * 5, h / 2); sleep 0.2 }
  sleep 1.5
  taste(s, 0xffe1) # Umschalt: holt das Passwortfeld hervor, tippt nichts
  sleep 1.0
  passwort.each_char { |z| taste(s, z.ord) }
  sleep 0.3
  taste(s, 0xff0d)
  sleep 1
  s.close
end

if ARGV[0] == '--pruefen'
  z = zugang or abort 'Kein Passwort gespeichert.'
  s, w, h = Timeout.timeout(30) { verbinden(*z) }
  s.close
  puts "Bildschirmfreigabe: Anmeldung ok (#{w}x#{h}), Zustand: #{zustand(z[0])}"
  exit 0
end

log 'gestartet'
sleep 10 # nach dem Einschalten: Bildschirmfreigabe und Anmeldefenster hochkommen lassen
fehlversuche = 0
loop do
  z = zugang
  if z.nil?
    sleep 30
    next
  end
  benutzer, passwort = z
  lage = zustand(benutzer)
  if lage == :offen
    fehlversuche = 0
    sleep 3
    next
  end
  begin
    log "#{lage == :anmeldung ? 'Anmeldebildschirm' : 'Sperrbildschirm'}: Passwort wird eingetippt"
    eintippen(benutzer, passwort)
    sleep 6
    neu = zustand(benutzer)
    log "danach: #{neu}"
    fehlversuche = neu == :offen ? 0 : fehlversuche + 1
  rescue StandardError => e
    log "Fehler: #{e.message}"
    fehlversuche += 1
  end
  # Wiederholte Fehlschläge würden macOS das Konto sperren lassen: dann lange Pause.
  sleep(fehlversuche >= 3 ? 600 : 10)
end
