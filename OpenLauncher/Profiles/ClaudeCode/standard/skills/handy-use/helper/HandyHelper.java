import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.UiAutomation;
import android.content.ClipData;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Base64;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Laeuft per app_process in der Shell-Rolle auf dem Handy und haelt eine UiAutomation-Verbindung offen.
 * Der Baum wird ohne waitForIdle gelesen: Darum haengt er nicht bei Apps mit Dauer-Animation (uiautomator dump: ~11 s bei Jarvis).
 * Zugriff nur mit dem Token aus einer 0600-Datei, weil jede App den Abstract-Socket erreichen kann. Beendet sich nach Leerlauf selbst.
 * Jede Verbindung hat einen eigenen Thread, damit ein Ping auch waehrend einer langen Aktion antwortet; Aktionen laufen nacheinander.
 */
public final class HandyHelper {
    static final String VERSION = "5";
    static final String SOCKET = "handy-helper";
    static final String DIR = "/data/local/tmp/handy";
    static final long LEERLAUF_MS = 20 * 60 * 1000;
    /** So lange gilt ein Baum als Grundlage fuer Aktionen. Danach kann auf dem Bildschirm alles anders sein. */
    static final long BAUM_GILT_MS = 2 * 60 * 1000;
    static final int MAX_KNOTEN = 500;
    static final int MAX_VERBINDUNGEN = 16;
    static final int LESE_LIMIT_MS = 5000;
    /** Eingaben ueber sh wuerden den Vordergrund-Schutz umgehen. Das ist eine Sperre gegen Versehen, keine Sicherheitsgrenze. */
    static final Pattern EINGABE_IN_SH = Pattern.compile(
            "(^|[;&|(]\\s*)(input|monkey|am\\s+start|am\\s+broadcast|cmd\\s+input|cmd\\s+activity\\s+(start|broadcast))\\b");

    static UiAutomation ua;
    static volatile long zuletzt = SystemClock.uptimeMillis();
    static final Object SPERRE = new Object();
    static final AtomicInteger offen = new AtomicInteger();

    /** Der zuletzt gezeigte Baum: Nummern beziehen sich nur hierauf; wait und die Textsuche aendern ihn nicht. */
    static final List<AccessibilityNodeInfo> knoten = new ArrayList<>();
    static final List<int[]> punkte = new ArrayList<>();
    static String erwartet;
    static long baumZeit;

    public static void main(String[] args) {
        // AccessibilityInteractionClient braucht den Haupt-Looper, ohne ihn stuerzt der Prozess beim Verbinden ab.
        Looper.prepareMainLooper();
        new Thread(() -> {
            try { dienen(); } catch (Throwable t) { t.printStackTrace(); System.exit(2); }
        }, "handy-dienst").start();
        Looper.loop();
    }

    static void dienen() throws Exception {
        LocalServerSocket server = new LocalServerSocket(SOCKET);
        verbinden();
        byte[] soll = tokenSchreiben().getBytes(StandardCharsets.UTF_8);
        Thread w = new Thread(() -> {
            while (true) {
                SystemClock.sleep(30000);
                if (SystemClock.uptimeMillis() - zuletzt > LEERLAUF_MS) {
                    try { ua.getClass().getMethod("disconnect").invoke(ua); } catch (Throwable ignored) { }
                    System.exit(0);
                }
            }
        });
        w.setDaemon(true);
        w.start();
        while (true) {
            LocalSocket s = server.accept();
            if (offen.incrementAndGet() > MAX_VERBINDUNGEN) {
                offen.decrementAndGet();
                try { s.close(); } catch (Exception ignored) { }
                continue;
            }
            Thread t = new Thread(() -> {
                try (LocalSocket sk = s) { sitzung(sk, soll); } catch (Throwable x) { x.printStackTrace(); } finally { offen.decrementAndGet(); }
            });
            t.setDaemon(true);
            t.start();
        }
    }

    static String tokenSchreiben() throws Exception {
        byte[] b = new byte[24];
        new SecureRandom().nextBytes(b);
        String t = Base64.encodeToString(b, Base64.NO_WRAP | Base64.URL_SAFE);
        File f = new File(DIR, "token");
        f.delete();
        try (FileOutputStream o = new FileOutputStream(f)) {
            f.setReadable(false, false); f.setWritable(false, false);
            f.setReadable(true, true); f.setWritable(true, true);
            o.write(t.getBytes(StandardCharsets.UTF_8));
        }
        return t;
    }

    static void verbinden() throws Exception {
        HandlerThread ht = new HandlerThread("handy-ua");
        ht.start();
        Object conn = Class.forName("android.app.UiAutomationConnection").getDeclaredConstructor().newInstance();
        Constructor<?> k = null;
        for (Constructor<?> c : UiAutomation.class.getDeclaredConstructors()) {
            Class<?>[] p = c.getParameterTypes();
            if (p.length == 2 && p[0] == Looper.class) k = c;
        }
        if (k == null) throw new IllegalStateException("UiAutomation(Looper, Connection) fehlt");
        k.setAccessible(true);
        ua = (UiAutomation) k.newInstance(ht.getLooper(), conn);
        Method m = UiAutomation.class.getDeclaredMethod("connect", int.class);
        m.setAccessible(true);
        m.invoke(ua, UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
        AccessibilityServiceInfo i = ua.getServiceInfo();
        i.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                | AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
                | AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        ua.setServiceInfo(i);
    }

    static void sitzung(LocalSocket s, byte[] soll) throws Exception {
        s.setSoTimeout(LESE_LIMIT_MS);
        BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
        OutputStream o = s.getOutputStream();
        String zeile = r.readLine();
        if (zeile == null) return;
        JSONObject antwort = new JSONObject();
        JSONObject req = new JSONObject(zeile);
        if (!MessageDigest.isEqual(soll, req.optString("token").getBytes(StandardCharsets.UTF_8))) {
            antwort.put("ok", false).put("fehler", "token");
        } else {
            JSONArray cmds = req.getJSONArray("cmds");
            zuletzt = SystemClock.uptimeMillis();
            if (cmds.length() == 1 && "ping".equals(cmds.getJSONObject(0).optString("c"))) {
                antwort.put("ok", true).put("ergebnisse", new JSONArray().put(new JSONObject().put("c", "ping").put("version", VERSION)));
            } else {
                synchronized (SPERRE) { abarbeiten(cmds, req.optBoolean("frei"), antwort); }
            }
            zuletzt = SystemClock.uptimeMillis();
        }
        o.write((antwort.toString() + "\n").getBytes(StandardCharsets.UTF_8));
        o.flush();
    }

    static void abarbeiten(JSONArray cmds, boolean frei, JSONObject antwort) throws Exception {
        JSONArray erg = new JSONArray();
        boolean ok = true;
        for (int i = 0; i < cmds.length() && ok; i++) {
            JSONObject c = cmds.getJSONObject(i);
            JSONObject e = new JSONObject();
            long t0 = SystemClock.uptimeMillis();
            try {
                ausfuehren(c, e, frei);
            } catch (Throwable t) {
                Throwable u = t.getCause() != null ? t.getCause() : t;
                e.put("fehler", u.getMessage() != null ? u.getMessage() : u.getClass().getSimpleName());
            }
            if (e.has("fehler")) ok = false;
            e.put("c", c.optString("c")).put("ms", SystemClock.uptimeMillis() - t0);
            erg.put(e);
        }
        antwort.put("ok", ok).put("ergebnisse", erg);
    }

    static void ausfuehren(JSONObject c, JSONObject e, boolean frei) throws Exception {
        String art = c.getString("c");
        switch (art) {
            case "tree": baum(c, e); return;
            case "shot": bild(c, e); return;
            case "wait": warten(c, e); return;
            case "wake": taste(KeyEvent.KEYCODE_WAKEUP); return; // Absichtlich ohne Schutz: Das Display ist dann ja aus.
            case "sh": lesen(c, e, frei); return;
            default: break;
        }
        vordergrundPruefen(frei);
        switch (art) {
            case "pruefe": break;
            case "start": starten(c, e); break;
            case "scroll": rollen(c.getString("richtung"), c.optDouble("anteil", 0.6)); break;
            case "tap": tippen(c, 0); break;
            case "long": tippen(c, c.optLong("dauer", 700)); break;
            case "swipe": wischen(c.getInt("x1"), c.getInt("y1"), c.getInt("x2"), c.getInt("y2"), c.optLong("dauer", 300)); break;
            case "pinch": kneifen(c.getInt("x"), c.getInt("y"), c.getInt("von"), c.getInt("bis"), c.optLong("dauer", 400)); break;
            case "key": taste(c.getInt("code")); break;
            case "text": text(c, e); break;
            default: throw new IllegalArgumentException("unbekannt: " + art);
        }
    }

    /** Stoppt, wenn nicht mehr die App vorn ist, die der letzte Baum gezeigt hat: Dann bedient gerade jemand anderes das Handy. */
    static void vordergrundPruefen(boolean frei) {
        if (frei) return;
        if (erwartet == null) throw new IllegalStateException("erst tree aufrufen: Vordergrund unbekannt oder nicht lesbar");
        if (SystemClock.uptimeMillis() - baumZeit > BAUM_GILT_MS) throw new IllegalStateException("BAUM_ZU_ALT: erst tree aufrufen");
        String jetzt = paket(wurzel());
        if (!erwartet.equals(jetzt)) throw new IllegalStateException("VORDERGRUND_GEWECHSELT erwartet=" + erwartet + " jetzt=" + jetzt + " (erst tree aufrufen)");
    }

    static AccessibilityNodeInfo wurzel() {
        for (int i = 0; i < 6; i++) {
            AccessibilityNodeInfo r = ua.getRootInActiveWindow();
            if (r != null) return r;
            SystemClock.sleep(40);
        }
        return null;
    }

    static String paket(AccessibilityNodeInfo r) {
        return r == null || r.getPackageName() == null ? "?" : r.getPackageName().toString();
    }

    static void baum(JSONObject c, JSONObject e) throws Exception {
        List<AccessibilityNodeInfo> neu = new ArrayList<>();
        List<int[]> pkt = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        AccessibilityNodeInfo r = wurzel();
        if (c.optBoolean("alle")) {
            for (AccessibilityWindowInfo w : ua.getWindows()) {
                AccessibilityNodeInfo fr = w.getRoot();
                if (fr == null) continue;
                sb.append("# Fenster ").append(fr.getPackageName()).append(w.isActive() ? " (aktiv)" : "").append('\n');
                ablaufen(fr, neu, pkt, sb);
            }
        } else if (r != null) {
            ablaufen(r, neu, pkt, sb);
        }
        knoten.clear(); knoten.addAll(neu);
        punkte.clear(); punkte.addAll(pkt);
        String fg = paket(r);
        erwartet = "?".equals(fg) ? null : fg;
        baumZeit = SystemClock.uptimeMillis();
        e.put("paket", fg).put("baum", sb.toString());
    }

    /** Sammelt die relevanten Knoten unter [wurzel]; [sb] darf null sein. Das Limit gilt je Fenster, eine Kuerzung wird angezeigt. */
    static void ablaufen(AccessibilityNodeInfo wurzel, List<AccessibilityNodeInfo> liste, List<int[]> pkt, StringBuilder sb) {
        ArrayDeque<AccessibilityNodeInfo> stapel = new ArrayDeque<>();
        stapel.push(wurzel);
        Rect b = new Rect();
        int ende = liste.size() + MAX_KNOTEN;
        while (!stapel.isEmpty() && liste.size() < ende) {
            AccessibilityNodeInfo n = stapel.pop();
            for (int i = n.getChildCount() - 1; i >= 0; i--) {
                AccessibilityNodeInfo k = n.getChild(i);
                if (k != null) stapel.push(k);
            }
            if (!n.isVisibleToUser()) continue;
            CharSequence t = n.isPassword() ? (n.getText() == null ? null : "●●●") : n.getText();
            CharSequence d = n.getContentDescription();
            boolean bedienbar = n.isClickable() || n.isLongClickable() || n.isEditable() || n.isScrollable() || n.isCheckable();
            if (!bedienbar && leer(t) && leer(d)) continue;
            n.getBoundsInScreen(b);
            if (b.width() <= 0 || b.height() <= 0) continue;
            int nr = liste.size();
            liste.add(n);
            pkt.add(new int[] { b.centerX(), b.centerY() });
            if (sb == null) continue;
            sb.append(nr).append(' ').append(kurz(n.getClassName()));
            if (!leer(t)) sb.append(" \"").append(eineZeile(t)).append('"');
            if (!leer(d) && (leer(t) || !d.toString().contentEquals(t))) sb.append(" d=\"").append(eineZeile(d)).append('"');
            String id = n.getViewIdResourceName();
            if (id != null) sb.append(" id=").append(id.substring(id.indexOf('/') + 1));
            sb.append(" @").append(b.centerX()).append(',').append(b.centerY());
            StringBuilder f = new StringBuilder();
            if (n.isClickable()) f.append('k');
            if (n.isLongClickable()) f.append('l');
            if (n.isEditable()) f.append('e');
            if (n.isScrollable()) f.append('s');
            if (n.isCheckable()) f.append(n.isChecked() ? "[an]" : "[aus]");
            if (n.isFocused()) f.append('f');
            if (n.isSelected()) f.append('*');
            if (!n.isEnabled()) f.append('x');
            if (f.length() > 0) sb.append(' ').append(f);
            sb.append('\n');
        }
        if (sb != null && !stapel.isEmpty()) sb.append("… gekürzt nach ").append(MAX_KNOTEN).append(" Elementen\n");
    }

    static boolean leer(CharSequence s) { return s == null || s.toString().trim().isEmpty(); }
    static String kurz(CharSequence k) { if (k == null) return "?"; String s = k.toString(); return s.substring(s.lastIndexOf('.') + 1); }
    static String eineZeile(CharSequence s) { String z = s.toString().replace('\n', ' ').replace("\"", "'"); return z.length() > 160 ? z.substring(0, 160) + "…" : z; }
    static String kleinText(AccessibilityNodeInfo n) { return n.isPassword() || n.getText() == null ? "" : n.getText().toString().toLowerCase(); }
    static String kleinBeschr(AccessibilityNodeInfo n) { return n.getContentDescription() == null ? "" : n.getContentDescription().toString().toLowerCase(); }

    static void bild(JSONObject c, JSONObject e) throws Exception {
        Bitmap b = ua.takeScreenshot();
        if (b == null) throw new IllegalStateException("kein Bild (Display aus oder geschützt)");
        Bitmap sw = b.copy(Bitmap.Config.ARGB_8888, false);
        int kante = Math.max(64, c.optInt("kante", 1024));
        int lang = Math.max(sw.getWidth(), sw.getHeight());
        double f = Math.min(1.0, kante / (double) lang);
        Bitmap kl = f < 1 ? Bitmap.createScaledBitmap(sw, Math.max(1, (int) (sw.getWidth() * f)), Math.max(1, (int) (sw.getHeight() * f)), true) : sw;
        boolean schwarz = true;
        int sx = Math.max(1, kl.getWidth() / 8), sy = Math.max(1, kl.getHeight() / 8);
        for (int y = sy / 2; y < kl.getHeight() && schwarz; y += sy) {
            for (int x = sx / 2; x < kl.getWidth(); x += sx) {
                int p = kl.getPixel(x, y);
                if (Color.red(p) > 16 || Color.green(p) > 16 || Color.blue(p) > 16) { schwarz = false; break; }
            }
        }
        ByteArrayOutputStream o = new ByteArrayOutputStream(128 * 1024);
        kl.compress(Bitmap.CompressFormat.JPEG, c.optInt("q", 70), o);
        e.put("breite", sw.getWidth()).put("hoehe", sw.getHeight()).put("faktor", f).put("schwarz", schwarz)
                .put("jpg", Base64.encodeToString(o.toByteArray(), Base64.NO_WRAP));
    }

    /** Bis der Baum zweimal gleich ist oder die Obergrenze erreicht ist; ersetzt feste Pausen. Laesst den gezeigten Baum und seine Nummern unberuehrt. */
    static void warten(JSONObject c, JSONObject e) throws Exception {
        long grenze = SystemClock.uptimeMillis() + c.optLong("max", 2000);
        if (c.has("paket")) {
            String soll = c.getString("paket");
            while (SystemClock.uptimeMillis() < grenze) {
                if (soll.equals(paket(wurzel()))) { erwartet = soll; baumZeit = SystemClock.uptimeMillis(); return; }
                SystemClock.sleep(50);
            }
            e.put("fehler", "App kam nicht nach vorn: " + soll + ", vorn ist " + paket(wurzel()));
            return;
        }
        String gesucht = c.optString("text", "").toLowerCase();
        int vorher = Integer.MIN_VALUE;
        SystemClock.sleep(c.optLong("min", 80));
        while (SystemClock.uptimeMillis() < grenze) {
            StringBuilder sb = new StringBuilder();
            List<AccessibilityNodeInfo> liste = new ArrayList<>();
            AccessibilityNodeInfo r = wurzel();
            if (r != null) ablaufen(r, liste, new ArrayList<>(), sb);
            if (!gesucht.isEmpty()) {
                for (AccessibilityNodeInfo n : liste) {
                    if (kleinText(n).contains(gesucht) || kleinBeschr(n).contains(gesucht)) { e.put("gefunden", true); return; }
                }
            } else {
                int h = sb.toString().hashCode();
                // Ein leerer Baum zaehlt nie als ruhig: Direkt nach dem App-Start ist das Fenster noch nicht gefuellt.
                if (h == vorher && sb.length() > 0) { e.put("ruhig", true); return; }
                vorher = h;
            }
            SystemClock.sleep(90);
        }
        if (!gesucht.isEmpty()) e.put("fehler", "nicht gefunden: " + c.optString("text")); else e.put("ruhig", false);
    }

    /** Ziel: x+y, Nummer aus dem letzten Baum oder Text (frisch gesucht). */
    static int[] ziel(JSONObject c) throws Exception {
        if (c.has("x")) return new int[] { c.getInt("x"), c.getInt("y") };
        if (c.has("nr")) return nummer(c.getInt("nr"));
        return sucheText(c.getString("suche"));
    }

    /** Eine Nummer gilt nur, solange das Element noch an seiner Stelle liegt: In derselben App kann sich der Bildschirm geaendert haben. */
    static int[] nummer(int n) {
        if (n < 0 || n >= punkte.size()) throw new IllegalArgumentException("Nummer " + n + " nicht im letzten Baum");
        AccessibilityNodeInfo k = knoten.get(n);
        Rect b = new Rect();
        if (!k.refresh() || !k.isVisibleToUser()) throw new IllegalStateException("ELEMENT_VERAENDERT: Nummer " + n + " ist weg (erst tree aufrufen)");
        k.getBoundsInScreen(b);
        int[] p = punkte.get(n);
        if (Math.abs(b.centerX() - p[0]) > 8 || Math.abs(b.centerY() - p[1]) > 8) {
            throw new IllegalStateException("ELEMENT_VERAENDERT: Nummer " + n + " liegt woanders (erst tree aufrufen)");
        }
        return p;
    }

    static boolean bedienbar(AccessibilityNodeInfo n) {
        AccessibilityNodeInfo a = n;
        for (int i = 0; i < 4 && a != null; i++) {
            if (a.isClickable() || a.isCheckable() || a.isEditable()) return true;
            a = a.getParent();
        }
        return false;
    }

    /**
     * Sucht in allen Fenstern. Genauer Treffer vor Teiltreffer, bedienbar vor nur sichtbar.
     * Mehrere verschiedene Teiltreffer sind ein Fehler: Aus „Aus“ darf nie still „Ausschalten“ werden.
     */
    static int[] sucheText(String s) {
        List<AccessibilityNodeInfo> liste = new ArrayList<>();
        List<int[]> pkt = new ArrayList<>();
        AccessibilityNodeInfo aktiv = wurzel();
        if (aktiv != null) ablaufen(aktiv, liste, pkt, null);
        for (AccessibilityWindowInfo w : ua.getWindows()) {
            AccessibilityNodeInfo r = w.isActive() ? null : w.getRoot();
            if (r != null) ablaufen(r, liste, pkt, null);
        }
        String t = s.toLowerCase();
        int[] genau = null, genauBedienbar = null, teilBedienbar = null, teil = null;
        Set<String> teile = new LinkedHashSet<>();
        for (int i = 0; i < liste.size(); i++) {
            AccessibilityNodeInfo n = liste.get(i);
            String a = kleinText(n), b = kleinBeschr(n);
            boolean bd = bedienbar(n);
            if (a.equals(t) || b.equals(t)) {
                if (genau == null) genau = pkt.get(i);
                if (bd && genauBedienbar == null) genauBedienbar = pkt.get(i);
            } else if (a.contains(t) || b.contains(t)) {
                teile.add(a.contains(t) ? a : b);
                if (teil == null) teil = pkt.get(i);
                if (bd && teilBedienbar == null) teilBedienbar = pkt.get(i);
            }
        }
        if (genauBedienbar != null) return genauBedienbar;
        if (genau != null) return genau;
        if (teile.size() > 1) {
            List<String> kurzliste = new ArrayList<>(teile);
            throw new IllegalStateException("mehrdeutig: " + s + " passt auf " + kurzliste.subList(0, Math.min(5, kurzliste.size())) + " (genauer Text oder Nummer)");
        }
        if (teilBedienbar != null) return teilBedienbar;
        if (teil != null) return teil;
        throw new IllegalStateException("nicht gefunden: " + s);
    }

    static void tippen(JSONObject c, long dauer) throws Exception {
        int[] p = ziel(c);
        long t = SystemClock.uptimeMillis();
        beruehren(MotionEvent.ACTION_DOWN, t, t, p[0], p[1]);
        if (dauer > 0) SystemClock.sleep(dauer);
        beruehren(MotionEvent.ACTION_UP, t, SystemClock.uptimeMillis(), p[0], p[1]);
    }

    /** Mitte der aktiven Wurzel, damit es zugeklappt wie aufgeklappt stimmt. */
    static void rollen(String r, double anteil) {
        AccessibilityNodeInfo w = wurzel();
        if (w == null) throw new IllegalStateException("kein Fenster");
        Rect b = new Rect();
        w.getBoundsInScreen(b);
        int mx = b.centerX(), my = b.centerY(), dy = (int) (b.height() * anteil / 2), dx = (int) (b.width() * anteil / 2);
        switch (r) {
            case "down": wischen(mx, my + dy, mx, my - dy, 250); break;
            case "up": wischen(mx, my - dy, mx, my + dy, 250); break;
            case "left": wischen(mx + dx, my, mx - dx, my, 250); break;
            case "right": wischen(mx - dx, my, mx + dx, my, 250); break;
            default: throw new IllegalArgumentException("scroll up|down|left|right");
        }
    }

    static void wischen(int x1, int y1, int x2, int y2, long dauer) {
        long t = SystemClock.uptimeMillis();
        beruehren(MotionEvent.ACTION_DOWN, t, t, x1, y1);
        int schritte = Math.max(8, (int) (dauer / 8));
        for (int i = 1; i <= schritte; i++) {
            float a = i / (float) schritte;
            SystemClock.sleep(dauer / schritte);
            beruehren(MotionEvent.ACTION_MOVE, t, SystemClock.uptimeMillis(), x1 + (x2 - x1) * a, y1 + (y2 - y1) * a);
        }
        beruehren(MotionEvent.ACTION_UP, t, SystemClock.uptimeMillis(), x2, y2);
    }

    static MotionEvent.PointerProperties[] finger(int n) {
        MotionEvent.PointerProperties[] pp = new MotionEvent.PointerProperties[n];
        for (int i = 0; i < n; i++) {
            pp[i] = new MotionEvent.PointerProperties();
            pp[i].id = i;
            pp[i].toolType = MotionEvent.TOOL_TYPE_FINGER;
        }
        return pp;
    }

    static MotionEvent.PointerCoords punkt() {
        MotionEvent.PointerCoords p = new MotionEvent.PointerCoords();
        p.pressure = 1;
        p.size = 1;
        return p;
    }

    static void beruehren(int aktion, long down, long jetzt, float x, float y) {
        MotionEvent.PointerCoords p = punkt();
        p.x = x;
        p.y = y;
        ereignis(down, jetzt, aktion, 1, finger(1), new MotionEvent.PointerCoords[] { p });
    }

    static void kneifen(int x, int y, int von, int bis, long dauer) {
        MotionEvent.PointerProperties[] pp = finger(2);
        MotionEvent.PointerCoords[] pc = { punkt(), punkt() };
        long t = SystemClock.uptimeMillis();
        setzen(pc, x, y, von);
        ereignis(t, t, MotionEvent.ACTION_DOWN, 1, pp, pc);
        ereignis(t, t, MotionEvent.ACTION_POINTER_DOWN | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT), 2, pp, pc);
        int schritte = Math.max(8, (int) (dauer / 10));
        for (int i = 1; i <= schritte; i++) {
            SystemClock.sleep(dauer / schritte);
            setzen(pc, x, y, von + (bis - von) * i / schritte);
            ereignis(t, SystemClock.uptimeMillis(), MotionEvent.ACTION_MOVE, 2, pp, pc);
        }
        long e = SystemClock.uptimeMillis();
        ereignis(t, e, MotionEvent.ACTION_POINTER_UP | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT), 2, pp, pc);
        ereignis(t, e, MotionEvent.ACTION_UP, 1, pp, pc);
    }

    static void setzen(MotionEvent.PointerCoords[] pc, int x, int y, int r) {
        pc[0].x = x - r; pc[0].y = y;
        pc[1].x = x + r; pc[1].y = y;
    }

    static void ereignis(long down, long jetzt, int aktion, int n, MotionEvent.PointerProperties[] pp, MotionEvent.PointerCoords[] pc) {
        MotionEvent m = MotionEvent.obtain(down, jetzt, aktion, n, pp, pc, 0, 0, 1, 1, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
        spritzen(m);
        m.recycle();
    }

    static void taste(int code) {
        long t = SystemClock.uptimeMillis();
        KeyEvent a = new KeyEvent(t, t, KeyEvent.ACTION_DOWN, code, 0);
        KeyEvent b = new KeyEvent(t, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, code, 0);
        a.setSource(InputDevice.SOURCE_KEYBOARD);
        b.setSource(InputDevice.SOURCE_KEYBOARD);
        spritzen(a);
        spritzen(b);
    }

    static void spritzen(InputEvent ev) {
        // false: nicht je Ereignis auf die App warten, sonst wird aus einem 250-ms-Wischer im Emulator 1,6 s. Die Reihenfolge bleibt erhalten.
        if (!ua.injectInputEvent(ev, false)) throw new IllegalStateException("Eingabe abgelehnt");
    }

    /**
     * Umlaute und Emojis: direkt ins Feld (ACTION_SET_TEXT). Ohne genanntes Feld ist die Zwischenablage der Rueckfall, etwa bei WebViews.
     * Ist ein Feld genannt und passt es nicht, ist das ein Fehler: Sonst landete der Text in irgendeinem anderen Feld.
     */
    static void text(JSONObject c, JSONObject e) throws Exception {
        String s = c.getString("s");
        boolean genannt = c.has("nr");
        AccessibilityNodeInfo feld = null;
        if (genannt) {
            int n = c.getInt("nr");
            nummer(n);
            feld = knoten.get(n);
            if (!feld.isEditable()) throw new IllegalStateException("Nummer " + n + " ist kein Eingabefeld");
            feld.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        } else {
            AccessibilityNodeInfo r = wurzel();
            if (r != null) feld = r.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        }
        if (feld != null && feld.isEditable()) {
            String neu = s;
            if (c.optBoolean("anhaengen") && feld.getText() != null && !feld.isShowingHintText()) neu = feld.getText() + s;
            Bundle b = new Bundle();
            b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, neu);
            if (feld.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b)) { e.put("weg", "feld"); return; }
            if (genannt) throw new IllegalStateException("Das Feld hat den Text abgelehnt");
        }
        zwischenablage(s);
        taste(KeyEvent.KEYCODE_PASTE);
        e.put("weg", "zwischenablage");
    }

    static void zwischenablage(String s) throws Exception {
        Class<?> sm = Class.forName("android.os.ServiceManager");
        IBinder bin = (IBinder) sm.getMethod("getService", String.class).invoke(null, "clipboard");
        Object cb = Class.forName("android.content.IClipboard$Stub").getMethod("asInterface", IBinder.class).invoke(null, bin);
        ClipData clip = ClipData.newPlainText("handy-use", s);
        for (Method m : cb.getClass().getMethods()) {
            if (!m.getName().equals("setPrimaryClip")) continue;
            Class<?>[] p = m.getParameterTypes();
            Object[] a = new Object[p.length];
            boolean ersterString = true;
            for (int i = 0; i < p.length; i++) {
                if (p[i] == ClipData.class) a[i] = clip;
                else if (p[i] == String.class) { a[i] = ersterString ? "com.android.shell" : null; ersterString = false; }
                else if (p[i] == int.class) a[i] = 0;
            }
            m.invoke(cb, a);
            return;
        }
        throw new IllegalStateException("setPrimaryClip fehlt");
    }

    /** sh: zum Lesen und fuer Einstellungen. Eingaben laufen ueber die geschuetzten Befehle. */
    static void lesen(JSONObject c, JSONObject e, boolean frei) throws Exception {
        String cmd = c.getString("cmd");
        if (!frei && EINGABE_IN_SH.matcher(cmd).find()) {
            e.put("fehler", "Eingaben nicht über sh: tap, text, key, open, url oder start nehmen, dort greift der Vordergrund-Schutz");
            return;
        }
        Ergebnis r = shell(cmd, c.optLong("timeout", 30000));
        e.put("aus", r.aus);
        if (r.zeitlimit) e.put("fehler", "Zeitlimit: Befehl abgebrochen, Ausgabe unvollständig");
        else if (r.rc != 0) e.put("rc", r.rc);
    }

    /** Geschuetzter App- oder Seitenstart. */
    static void starten(JSONObject c, JSONObject e) throws Exception {
        Ergebnis r = shell(c.getString("cmd"), 15000);
        if (r.zeitlimit || r.rc != 0 || r.aus.contains("Error:") || r.aus.contains("No activities found")) {
            e.put("fehler", "Start fehlgeschlagen: " + r.aus.trim());
        }
    }

    static final class Ergebnis {
        String aus = "";
        int rc = -1;
        boolean zeitlimit;
    }

    static Ergebnis shell(String cmd, long timeout) throws Exception {
        Process p = new ProcessBuilder("sh", "-c", cmd).redirectErrorStream(true).start();
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        Thread leser = new Thread(() -> {
            try (InputStream in = p.getInputStream()) {
                byte[] b = new byte[8192];
                int n;
                while ((n = in.read(b)) > 0) o.write(b, 0, n);
            } catch (Exception ignored) { }
        });
        leser.setDaemon(true);
        leser.start();
        Ergebnis r = new Ergebnis();
        if (p.waitFor(timeout, TimeUnit.MILLISECONDS)) {
            r.rc = p.exitValue();
        } else {
            r.zeitlimit = true;
            p.destroyForcibly();
        }
        leser.join(300);
        r.aus = o.toString("UTF-8");
        return r;
    }
}
