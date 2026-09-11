using System;
using System.IO;
using System.Windows;

namespace ClaudeVoiceOverlay.Services
{
    /// <summary>
    /// Tiefe Kopie der Zwischenablage vor dem Diktat-Einfuegen.
    /// Clipboard.GetDataObject() liefert nur einen Live-Verweis auf die
    /// OLE-Zwischenablage. Nach Clipboard.SetDataObject zeigt dieser Verweis auf
    /// den neuen Inhalt bzw. ist ungueltig — ein Zurueckschreiben hinterliess eine
    /// leere Zwischenablage, vorher kopierter Text war weg (Strg+V tat nichts).
    /// Deshalb alle Formate sofort in ein eigenes DataObject kopieren.
    /// Schwester-Datei: TerminalVoiceOverlay-Windows/Services/ClipboardSnapshot.cs
    /// </summary>
    internal static class ClipboardSnapshot
    {
        /// <summary>
        /// Muss auf einem STA-Thread laufen. <paramref name="hadContent"/> ist true,
        /// wenn die Zwischenablage etwas enthielt — auch wenn nichts kopierbar war
        /// (dann null zurueck und der Aufrufer darf NICHT leeren).
        /// </summary>
        public static IDataObject? Capture(out bool hadContent)
        {
            hadContent = false;
            var source = Clipboard.GetDataObject();
            if (source is null)
                return null;

            string[] formats;
            try { formats = source.GetFormats(false); }
            catch { return null; }
            hadContent = formats.Length > 0;

            var copy = new DataObject();
            int copied = 0;
            foreach (var format in formats)
            {
                try
                {
                    var data = source.GetData(format, false);
                    if (data is null)
                        continue;
                    if (data is Stream stream)
                    {
                        var buffer = new MemoryStream();
                        if (stream.CanSeek)
                            stream.Position = 0;
                        stream.CopyTo(buffer);
                        buffer.Position = 0;
                        data = buffer;
                    }
                    copy.SetData(format, data, false);
                    copied++;
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"[ClipboardSnapshot] Format '{format}' nicht kopierbar: {ex.GetType().Name}");
                }
            }
            return copied > 0 ? copy : null;
        }
    }
}
