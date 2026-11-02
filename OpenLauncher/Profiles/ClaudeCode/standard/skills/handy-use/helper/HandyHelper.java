import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.UiAutomation;
import android.content.ClipData;
import android.graphics.Bitmap;
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
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Laeuft per app_process in der Shell-Rolle auf dem Handy und haelt eine UiAutomation-Verbindung offen.
 * Der Baum wird ohne waitForIdle gelesen: Darum haengt er nicht bei Apps mit Dauer-Animation (uiautomator dump: ~11 s bei Jarvis).
 * Zugriff nur mit dem Token aus einer 0600-Datei, weil jede App den Abstract-Socket erreichen kann. Beendet sich nach Leerlauf selbst.
 */
public final class HandyHelper {
    static final String VERSION = "4";
    static final String SOCKET = "handy-helper";
    static final String DIR = "/data/local/tmp/handy";
    static final long LEERLAUF_MS = 20 * 60 * 1000;
    static final int MAX_KNOTEN = 500;

    static UiAutomation ua;
    static volatile long zuletzt = SystemClock.uptimeMillis();
    static final List<int[]> punkte = new ArrayList<>();
    static final List<AccessibilityNodeInfo> knoten = new ArrayList<>();
    static String erwartet;

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
        String token = tokenSchreiben();
        verbinden();
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
        byte[] soll = token.getBytes(StandardCharsets.UTF_8);
        while (true) {
            try (LocalSocket s = server.accept()) { sitzung(s, soll); } catch (Throwable t) { t.printStackTrace(); }
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
        BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
        OutputStream o = s.getOutputStream();
        String zeile = r.readLine();
        if (zeile == null) return;
        JSONObject antwort = new JSONObject();
        JSONObject req = new JSONObject(zeile);
        if (!MessageDigest.isEqual(soll, req.optString("token").getBytes(StandardCharsets.UTF_8))) {
            antwort.put("ok", false).put("fehler", "token");
        } else {
            zuletzt = SystemClock.uptimeMillis();
            JSONArray cmds = req.getJSONArray("cmds");
            JSONArray erg = new JSONArray();
            boolean ok = true;
            for (int i = 0; i < cmds.length() && ok; i++) {
                JSONObject c = cmds.getJSONObject(i);
                JSONObject e = new JSONObject();
                long t0 = SystemClock.uptimeMillis();
                try {
                    ausfuehren(c, e, req.optBoolean("frei"));
                } catch (Throwable t) {
                    Throwable u = t.getCause() != null ? t.getCause() : t;
                    e.put("fehler", u.getClass().getSimpleName() + ": " + u.getMessage());
                }
                if (e.has("fehler")) ok = false;
                e.put("c", c.optString("c")).put("ms", SystemClock.uptimeMillis() - t0);
                erg.put(e);
            }
            antwort.put("ok", ok).put("ergebnisse", erg);
            zuletzt = SystemClock.uptimeMillis();
        }
        o.write((antwort.toString() + "\n").getBytes(StandardCharsets.UTF_8));
        o.flush();
    }

    static void ausfuehren(JSONObject c, JSONObject e, boolean frei) throws Exception {
        String art = c.getString("c");
        switch (art) {
            case "ping": e.put("version", VERSION); return;
            case "tree": baum(c, e); return;
            case "shot": bild(c, e); return;
            case "wait": warten(c, e); return;
            case "sh": e.put("aus", shell(c.getString("cmd"), c.optLong("timeout", 30000))); return;
            default: break;
        }
        vordergrundPruefen(frei);
        switch (art) {
            case "pruefe": break;
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
        if (erwartet == null) throw new IllegalStateException("erst tree aufrufen: Vordergrund unbekannt");
        String jetzt = vordergrund();
        if (!erwartet.equals(jetzt)) throw new IllegalStateException("VORDERGRUND_GEWECHSELT erwartet=" + erwartet + " jetzt=" + jetzt);
    }

    static AccessibilityNodeInfo wurzel() {
        for (int i = 0; i < 6; i++) {
            AccessibilityNodeInfo r = ua.getRootInActiveWindow();
            if (r != null) return r;
            SystemClock.sleep(40);
        }
        return null;
    }

    static String vordergrund() {
        AccessibilityNodeInfo r = wurzel();
        return r == null || r.getPackageName() == null ? "?" : r.getPackageName().toString();
    }

    static void baum(JSONObject c, JSONObject e) throws Exception {
        punkte.clear();
        knoten.clear();
        StringBuilder sb = new StringBuilder();
        String fg = vordergrund();
        if (c.optBoolean("alle")) {
            for (AccessibilityWindowInfo w : ua.getWindows()) {
                AccessibilityNodeInfo r = w.getRoot();
                if (r == null) continue;
                sb.append("# Fenster ").append(r.getPackageName()).append(w.isActive() ? " (aktiv)" : "").append('\n');
                ablaufen(r, sb);
            }
        } else {
            AccessibilityNodeInfo r = wurzel();
            if (r != null) ablaufen(r, sb);
        }
        erwartet = fg;
        e.put("paket", fg).put("baum", sb.toString());
    }

    static void ablaufen(AccessibilityNodeInfo wurzel, StringBuilder sb) {
        ArrayDeque<AccessibilityNodeInfo> stapel = new ArrayDeque<>();
        stapel.push(wurzel);
        Rect b = new Rect();
        while (!stapel.isEmpty() && knoten.size() < MAX_KNOTEN) {
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
            int nr = knoten.size();
            knoten.add(n);
            punkte.add(new int[] { b.centerX(), b.centerY() });
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
    }

    static boolean leer(CharSequence s) { return s == null || s.toString().trim().isEmpty(); }
    static String kurz(CharSequence k) { if (k == null) return "?"; String s = k.toString(); return s.substring(s.lastIndexOf('.') + 1); }
    static String eineZeile(CharSequence s) { String z = s.toString().replace('\n', ' ').replace("\"", "'"); return z.length() > 160 ? z.substring(0, 160) + "…" : z; }

    static void bild(JSONObject c, JSONObject e) throws Exception {
        Bitmap b = ua.takeScreenshot();
        if (b == null) throw new IllegalStateException("kein Bild (Display aus oder geschuetzt)");
        Bitmap sw = b.copy(Bitmap.Config.ARGB_8888, false);
        int kante = c.optInt("kante", 1024);
        int lang = Math.max(sw.getWidth(), sw.getHeight());
        double f = Math.min(1.0, kante / (double) lang);
        Bitmap kl = f < 1 ? Bitmap.createScaledBitmap(sw, (int) (sw.getWidth() * f), (int) (sw.getHeight() * f), true) : sw;
        boolean schwarz = true;
        for (int y = kl.getHeight() / 8; y < kl.getHeight() && schwarz; y += kl.getHeight() / 8) {
            for (int x = kl.getWidth() / 8; x < kl.getWidth(); x += kl.getWidth() / 8) {
                if ((kl.getPixel(x, y) & 0xFFFFFF) > 0x101010) { schwarz = false; break; }
            }
        }
        ByteArrayOutputStream o = new ByteArrayOutputStream(128 * 1024);
        kl.compress(Bitmap.CompressFormat.JPEG, c.optInt("q", 70), o);
        e.put("breite", sw.getWidth()).put("hoehe", sw.getHeight()).put("faktor", f).put("schwarz", schwarz)
                .put("jpg", Base64.encodeToString(o.toByteArray(), Base64.NO_WRAP));
    }

    /** Bis der Baum zweimal gleich ist oder die Obergrenze erreicht ist; ersetzt feste Pausen. */
    static void warten(JSONObject c, JSONObject e) throws Exception {
        long grenze = SystemClock.uptimeMillis() + c.optLong("max", 2000);
        if (c.has("paket")) {
            String soll = c.getString("paket");
            while (SystemClock.uptimeMillis() < grenze) {
                if (soll.equals(vordergrund())) { erwartet = soll; return; }
                SystemClock.sleep(50);
            }
            e.put("fehler", "App kam nicht nach vorn: " + soll + ", vorn ist " + vordergrund());
            return;
        }
        String gesucht = c.optString("text", "");
        int vorher = Integer.MIN_VALUE;
        int gleich = 0;
        SystemClock.sleep(c.optLong("min", 80));
        while (SystemClock.uptimeMillis() < grenze) {
            StringBuilder sb = new StringBuilder();
            punkte.clear();
            knoten.clear();
            AccessibilityNodeInfo r = wurzel();
            if (r != null) ablaufen(r, sb);
            if (!gesucht.isEmpty()) {
                if (sb.toString().toLowerCase().contains(gesucht.toLowerCase())) { e.put("gefunden", true); return; }
            } else {
                int h = sb.toString().hashCode();
                // Ein leerer Baum zaehlt nie als ruhig: Direkt nach dem App-Start ist das Fenster noch nicht gefuellt.
                if (h == vorher && sb.length() > 0 && ++gleich >= 1) { e.put("ruhig", true); return; }
                if (h != vorher) gleich = 0;
                vorher = h;
            }
            SystemClock.sleep(90);
        }
        if (!gesucht.isEmpty()) e.put("fehler", "nicht gefunden: " + gesucht); else e.put("ruhig", false);
    }

    /** Ziel: x+y, nr aus dem letzten Baum oder Text (frisch gesucht, erster Treffer, bevorzugt bedienbar). */
    static int[] ziel(JSONObject c) throws Exception {
        if (c.has("x")) return new int[] { c.getInt("x"), c.getInt("y") };
        if (c.has("nr")) {
            int n = c.getInt("nr");
            if (n < 0 || n >= punkte.size()) throw new IllegalArgumentException("Nummer " + n + " nicht im letzten Baum");
            return punkte.get(n);
        }
        int[] p = sucheText(c.getString("suche"));
        if (p == null) throw new IllegalStateException("nicht gefunden: " + c.getString("suche"));
        return p;
    }

    static int[] sucheText(String s) {
        AccessibilityNodeInfo r = wurzel();
        if (r == null) return null;
        StringBuilder egal = new StringBuilder();
        punkte.clear();
        knoten.clear();
        ablaufen(r, egal);
        String t = s.toLowerCase();
        int[] genau = null, teil = null;
        for (int i = 0; i < knoten.size(); i++) {
            AccessibilityNodeInfo n = knoten.get(i);
            String a = n.getText() == null ? "" : n.getText().toString().toLowerCase();
            String b = n.getContentDescription() == null ? "" : n.getContentDescription().toString().toLowerCase();
            if (a.equals(t) || b.equals(t)) { if (genau == null) genau = punkte.get(i); }
            else if ((a.contains(t) || b.contains(t)) && teil == null) teil = punkte.get(i);
        }
        return genau != null ? genau : teil;
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

    static void beruehren(int aktion, long down, long jetzt, float x, float y) {
        MotionEvent m = MotionEvent.obtain(down, jetzt, aktion, x, y, 0);
        m.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        spritzen(m);
        m.recycle();
    }

    static void kneifen(int x, int y, int von, int bis, long dauer) {
        MotionEvent.PointerProperties[] pp = new MotionEvent.PointerProperties[2];
        MotionEvent.PointerCoords[] pc = new MotionEvent.PointerCoords[2];
        for (int i = 0; i < 2; i++) {
            pp[i] = new MotionEvent.PointerProperties();
            pp[i].id = i;
            pp[i].toolType = MotionEvent.TOOL_TYPE_FINGER;
            pc[i] = new MotionEvent.PointerCoords();
            pc[i].pressure = 1;
            pc[i].size = 1;
        }
        long t = SystemClock.uptimeMillis();
        setzen(pc, x, y, von);
        zwei(t, t, MotionEvent.ACTION_DOWN, 1, pp, pc);
        zwei(t, t, MotionEvent.ACTION_POINTER_DOWN | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT), 2, pp, pc);
        int schritte = Math.max(8, (int) (dauer / 10));
        for (int i = 1; i <= schritte; i++) {
            SystemClock.sleep(dauer / schritte);
            setzen(pc, x, y, von + (bis - von) * i / schritte);
            zwei(t, SystemClock.uptimeMillis(), MotionEvent.ACTION_MOVE, 2, pp, pc);
        }
        long e = SystemClock.uptimeMillis();
        zwei(t, e, MotionEvent.ACTION_POINTER_UP | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT), 2, pp, pc);
        zwei(t, e, MotionEvent.ACTION_UP, 1, pp, pc);
    }

    static void setzen(MotionEvent.PointerCoords[] pc, int x, int y, int r) {
        pc[0].x = x - r; pc[0].y = y;
        pc[1].x = x + r; pc[1].y = y;
    }

    static void zwei(long down, long jetzt, int aktion, int n, MotionEvent.PointerProperties[] pp, MotionEvent.PointerCoords[] pc) {
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

    /** Umlaute und Emojis: direkt ins Feld (ACTION_SET_TEXT), sonst Zwischenablage + Einfuegen. */
    static void text(JSONObject c, JSONObject e) throws Exception {
        String s = c.getString("s");
        AccessibilityNodeInfo r = wurzel();
        AccessibilityNodeInfo feld = null;
        if (c.has("nr")) {
            int n = c.getInt("nr");
            if (n >= 0 && n < knoten.size()) feld = knoten.get(n);
            if (feld != null) feld.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        } else if (r != null) {
            feld = r.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        }
        if (feld != null && feld.isEditable()) {
            String neu = s;
            if (c.optBoolean("anhaengen") && feld.getText() != null && !feld.isShowingHintText()) neu = feld.getText() + s;
            Bundle b = new Bundle();
            b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, neu);
            if (feld.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b)) { e.put("weg", "feld"); return; }
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

    static String shell(String cmd, long timeout) throws Exception {
        Process p = new ProcessBuilder("sh", "-c", cmd).redirectErrorStream(true).start();
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        Thread leser = new Thread(() -> {
            try (InputStream in = p.getInputStream()) {
                byte[] b = new byte[8192];
                int n;
                while ((n = in.read(b)) > 0) o.write(b, 0, n);
            } catch (Exception ignored) { }
        });
        leser.start();
        long ende = SystemClock.uptimeMillis() + timeout;
        while (p.isAlive() && SystemClock.uptimeMillis() < ende) SystemClock.sleep(5);
        if (p.isAlive()) p.destroyForcibly();
        leser.join(300);
        return o.toString("UTF-8");
    }
}
