// Beendet Kotlin-Compile-Daemons, deren JDK gelöscht ist (Aufruf: aufraeumen.sh, als Single-File-Programm).
//
// Erkennung über den Pfad der tatsächlich laufenden Java-Datei (macOS: proc_pidpath, also hinter allen
// Symlinks): Existiert sie nicht mehr, hat ein JDK-Update ihren Ordner gelöscht. Ein solcher Kotlin-Daemon
// bricht jede Kompilierung mit „no class roots are found in the JDK path“ ab; ihn zu beenden kostet nichts.
// Gradle-Daemons bleiben unberührt: Gradle verwendet sie nach einem JDK-Wechsel ohnehin nicht wieder.
public class VeralteteDaemons {
    public static void main(String[] args) {
        int beendet = 0;
        for (ProcessHandle p : (Iterable<ProcessHandle>) ProcessHandle.allProcesses()::iterator) {
            if (!veraltet(p)) continue;
            if (p.destroy()) {
                beendet++;
                System.out.println("JDK-Schutz: veralteten Kotlin-Daemon " + p.pid() + " beendet ("
                    + p.info().command().orElse("?") + " existiert nicht mehr).");
            }
        }
        if (beendet == 0) System.out.println("JDK-Schutz: keine veralteten Kotlin-Daemons.");
    }

    static boolean veraltet(ProcessHandle p) {
        ProcessHandle.Info info = p.info();
        String java = info.command().orElse("");
        if (!java.endsWith("/bin/java") && !java.endsWith("\\bin\\java.exe")) return false;
        if (!info.commandLine().orElse("").contains("KotlinCompileDaemon")) return false;
        return !new java.io.File(java).exists();
    }
}
