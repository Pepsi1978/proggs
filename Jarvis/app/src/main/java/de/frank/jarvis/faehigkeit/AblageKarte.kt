package de.frank.jarvis.faehigkeit

import org.json.JSONArray
import org.json.JSONObject

/**
 * Die Karte, mit der ChatGPT eine Datei aus der Ablage im Gespräch anzeigt (MCP Apps: eine HTML-Ressource unter
 * `ui://`, die das Werkzeug `ablage_zeigen` als Ausgabevorlage nennt). ChatGPT zeigt Bilder aus Werkzeug-Ergebnissen
 * nur auf diesem Weg verlässlich an; reine MCP-Bildblöcke und Bildlinks verwirft oder sperrt es (Recherche 08.10.2026,
 * best-practices/server/chatgpt-plugin-dateien.md).
 *
 * Das Bild kommt als data:-Adresse im Feld `_meta` des Ergebnisses: Das sieht nur die Karte, nicht das Modell, und es
 * braucht keine öffentliche Adresse für die Datei. Die Karte lädt nichts aus dem Netz (leere Freigabeliste).
 */
object AblageKarte {
    /** Bei jeder Änderung am HTML hochzählen: ChatGPT merkt sich die Ressource unter ihrer Adresse. */
    const val URI = "ui://jarvis/ablage-v1.html"
    private const val MIME = "text/html;profile=mcp-app"

    val werkzeugMeta: JSONObject get() = JSONObject().put("ui", JSONObject().put("resourceUri", URI)).put("openai/outputTemplate", URI)

    private fun meta() = JSONObject()
        .put("ui", JSONObject().put("prefersBorder", true).put("csp", JSONObject().put("resourceDomains", JSONArray()).put("connectDomains", JSONArray())))
        .put("openai/widgetDescription", "Zeigt ein Bild, eine PDF-Seite oder einen Text aus der Ablage von Jarvis.")

    fun liste(): JSONObject = JSONObject().put("resources", JSONArray().put(
        JSONObject().put("uri", URI).put("name", "jarvis-ablage").put("title", "Jarvis-Ablage").put("mimeType", MIME).put("_meta", meta())))

    fun lies(uri: String): JSONObject? = if (uri != URI) null else JSONObject().put("contents", JSONArray().put(
        JSONObject().put("uri", URI).put("mimeType", MIME).put("text", HTML).put("_meta", meta())))

    // Versteht beide Wege, auf denen ChatGPT das Ergebnis übergibt: window.openai (ChatGPT) und die Nachrichten des MCP-Apps-Standards.
    private val HTML = """
<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<style>
:root{color-scheme:light dark}
body{margin:0;font:15px/1.45 system-ui,sans-serif;color:CanvasText;background:transparent}
#kopf{padding:10px 12px 2px;font-weight:600}
#unter{padding:0 12px 8px;font-size:12px;opacity:.7}
img{display:block;width:100%;height:auto;border-radius:10px}
pre{margin:0;padding:6px 12px 12px;white-space:pre-wrap;overflow-wrap:anywhere;font:14px/1.5 system-ui,sans-serif}
#leer{padding:14px 12px;opacity:.7}
</style></head><body>
<div id="kopf"></div><div id="unter"></div><div id="inhalt"><div id="leer">Jarvis lädt …</div></div>
<script>
(function(){
  var daten=null, meta=null;
  function sende(m){ try{ window.parent.postMessage(m,'*'); }catch(e){} }
  function groesse(){ sende({jsonrpc:'2.0',method:'ui/notifications/size-changed',params:{width:document.documentElement.scrollWidth,height:document.documentElement.scrollHeight}}); }
  function zeige(){
    var d=daten||{}, m=meta||{}, box=document.getElementById('inhalt');
    document.getElementById('kopf').textContent=d.titel||'';
    document.getElementById('unter').textContent=d.hinweis||'';
    if(m.bild){ var i=new Image(); i.alt=d.titel||''; i.onload=groesse; i.src=m.bild; box.replaceChildren(i); }
    else if(d.text){ var p=document.createElement('pre'); p.textContent=d.text; box.replaceChildren(p); }
    else if(daten){ var l=document.createElement('div'); l.id='leer'; l.textContent='Die Datei wurde nicht mitgeliefert. Du findest sie in Jarvis unter Ablage.'; box.replaceChildren(l); }
    groesse();
  }
  function nimm(sc,mt){ if(sc) daten=sc; if(mt) meta=mt; if(sc||mt) zeige(); }
  function ausOpenai(){ var o=window.openai; if(o) nimm(o.toolOutput, o.toolResponseMetadata); }
  window.addEventListener('openai:set_globals', ausOpenai);
  window.addEventListener('message', function(e){
    var m=e.data; if(!m||m.jsonrpc!=='2.0') return;
    if(m.method==='ui/notifications/tool-result'&&m.params) nimm(m.params.structuredContent, m.params._meta);
    if(m.id===1&&m.result) sende({jsonrpc:'2.0',method:'ui/notifications/initialized'});
  });
  sende({jsonrpc:'2.0',id:1,method:'ui/initialize',params:{protocolVersion:'2026-01-26',appInfo:{name:'Jarvis-Ablage',version:'1'},appCapabilities:{}}});
  ausOpenai();
})();
</script></body></html>
""".trim()
}
