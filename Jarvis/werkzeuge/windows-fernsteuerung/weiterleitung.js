// Leitet den Fernsteuerungs-Port aus dem Tunnel (0.0.0.0:47800) an den Dienst auf 127.0.0.1:47801 weiter.
// Läuft mit Node.js, weil die Windows-Firewall dafür schon eine Freigabe hat; eine eigene Regel für Python bräuchte
// Administratorrechte (wie am Mac, dort übernimmt /usr/bin/ruby diese Rolle).
const net = require('net');

const AUSSEN = 47800;
const INNEN = 47801;

// Nur der eigene Tunnel (10.8.0.x) und der PC selbst dürfen herein, nicht das WLAN, in dem der Laptop gerade hängt.
function erlaubt(ip) {
  const a = (ip || '').replace(/^::ffff:/, '');
  return a.startsWith('10.8.0.') || a === '127.0.0.1';
}

const server = net.createServer((aussen) => {
  if (!erlaubt(aussen.remoteAddress)) { aussen.destroy(); return; }
  aussen.setNoDelay(true);
  const innen = net.connect(INNEN, '127.0.0.1');
  innen.setNoDelay(true);
  aussen.pipe(innen);
  innen.pipe(aussen);
  const zu = () => { aussen.destroy(); innen.destroy(); };
  aussen.on('error', zu); innen.on('error', zu);
  aussen.on('close', zu); innen.on('close', zu);
});
server.on('error', (e) => { console.error(`Weiterleitung: ${e.message}`); process.exit(1); });
server.listen(AUSSEN, '0.0.0.0', () => console.error(`Weiterleitung ${AUSSEN} -> 127.0.0.1:${INNEN}`));
