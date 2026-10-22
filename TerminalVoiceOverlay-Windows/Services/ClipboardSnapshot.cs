using System;
using System.IO;
using System.Windows;

namespace TerminalVoiceOverlay.Services
{
    /// <summary>
    /// Tiefe Kopie der Zwischenablage vor dem Diktat-Einfuegen.
    /// Clipboard.GetDataObject() liefert nur einen Live-Verweis auf die
    /// OLE-Zwischenablage. Nach Clipboard.SetText zeigt dieser Verweis auf den
    /// neuen Inhalt bzw. ist ungueltig — ein Zurueckschreiben hinterliess eine
    /// leere Zwischenablage, vorher kopierter Text war weg (Strg+V tat nichts).
    /// Deshalb alle Formate sofort in ein eigenes DataObject kopieren.
    /// Schwester-Datei: ClaudeVoiceOverlay-Windows/Services/ClipboardSnapshot.cs
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
                    Console.WriteLine($"ClipboardSnapshot: Format '{format}' nicht kopierbar: {ex.GetType().Name}");
                }
            }
            if (copied == 0)
                return null;
            // The restore re-writes the user's content; keep that re-write out of
            // clipboard history / cloud sync so it does not create a duplicate entry.
            MarkExcludedFromHistory(copy);
            return copy;
        }

        /// <summary>
        /// Builds the clipboard payload for a dictation: Unicode text (Text is
        /// synthesized via autoConvert, like Clipboard.SetText) plus the formats that
        /// keep it out of clipboard history (Win+V), cloud clipboard and monitors.
        /// </summary>
        public static DataObject CreateTextData(string text)
        {
            var data = new DataObject();
            data.SetData(DataFormats.UnicodeText, text, true);
            MarkExcludedFromHistory(data);
            return data;
        }

        /// <summary>
        /// Adds the documented opt-out formats: ExcludeClipboardContentFromMonitorProcessing
        /// (presence is enough), CanIncludeInClipboardHistory = 0, CanUploadToCloudClipboard = 0.
        /// </summary>
        public static void MarkExcludedFromHistory(DataObject data)
        {
            try
            {
                data.SetData("ExcludeClipboardContentFromMonitorProcessing", new MemoryStream(new byte[4]), false);
                data.SetData("CanIncludeInClipboardHistory", new MemoryStream(new byte[4]), false);
                data.SetData("CanUploadToCloudClipboard", new MemoryStream(new byte[4]), false);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"ClipboardSnapshot: Ausschluss-Formate nicht setzbar: {ex.GetType().Name}");
            }
        }
    }
}
