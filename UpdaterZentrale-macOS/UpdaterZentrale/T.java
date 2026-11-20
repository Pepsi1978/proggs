public class T { public static void main(String[] a) {
  ProcessHandle.allProcesses().forEach(p -> { var i = p.info(); String cl = i.commandLine().orElse(""); 
    if (cl.contains("KotlinCompileDaemon") || cl.contains("GradleDaemon")) System.out.println(p.pid()+" cmd="+i.command().orElse("<leer>")+" exists="+i.command().map(c->new java.io.File(c).exists()).orElse(null)+" first="+cl.split(" ")[0]); });
}}
