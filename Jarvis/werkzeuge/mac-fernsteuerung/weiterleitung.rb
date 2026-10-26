# Leitet den Fernsteuerungs-Port aus dem Tunnel (0.0.0.0:47800) an den Dienst auf 127.0.0.1:47801 weiter.
# Läuft mit /usr/bin/ruby, weil die macOS-Firewall eingebaute, von Apple signierte Programme durchlässt; den selbst
# gebauten Dienst würde sie für Verbindungen von außen sperren (Rückfrage am Bildschirm, den niemand sieht).
require 'socket'

AUSSEN = 47800
INNEN = 47801

def nodelay(s)
  s.setsockopt(Socket::IPPROTO_TCP, Socket::TCP_NODELAY, 1)
rescue StandardError
  nil
end

server = TCPServer.new('0.0.0.0', AUSSEN)
$stderr.puts "Weiterleitung #{AUSSEN} -> 127.0.0.1:#{INNEN}"
# Nur der eigene Tunnel (10.8.0.x) und der Mac selbst dürfen herein, nicht das WLAN, in dem der Mac gerade hängt.
def erlaubt?(a)
  ip = a.remote_address.ip_address
  ip.start_with?('10.8.0.') || ip == '127.0.0.1'
rescue StandardError
  false
end

loop do
  aussen = server.accept
  unless erlaubt?(aussen)
    aussen.close rescue nil
    next
  end
  Thread.new(aussen) do |a|
    i = nil
    begin
      i = TCPSocket.new('127.0.0.1', INNEN)
      nodelay(a); nodelay(i)
      t = Thread.new { begin; loop { i.write(a.readpartial(65536)) }; rescue StandardError; end; i.close_write rescue nil }
      begin; loop { a.write(i.readpartial(1 << 20)) }; rescue StandardError; end
      t.kill
    rescue StandardError => e
      $stderr.puts "Weiterleitung: #{e.message}"
    ensure
      a.close rescue nil
      i.close if i rescue nil
    end
  end
end
