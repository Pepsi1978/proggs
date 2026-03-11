import Foundation

/// User-friendly error descriptions for API errors (extracted from AppDelegate for clarity)
enum ErrorDescriptions {

    static func describeTranscriptionError(_ error: Error) -> String {
        let nsError = error as NSError

        if nsError.domain == NSURLErrorDomain {
            switch nsError.code {
            case NSURLErrorNotConnectedToInternet, NSURLErrorNetworkConnectionLost:
                return "Kein Internet — Spracheingabe konnte nicht transkribiert werden"
            case NSURLErrorTimedOut:
                return "Timeout — Groq API antwortet nicht (Internet-Problem?)"
            case NSURLErrorCannotFindHost, NSURLErrorCannotConnectToHost, NSURLErrorDNSLookupFailed:
                return "Groq API nicht erreichbar — DNS oder Server-Problem"
            case NSURLErrorSecureConnectionFailed:
                return "SSL-Fehler — sichere Verbindung zu Groq fehlgeschlagen"
            default:
                return "Netzwerkfehler — \(error.localizedDescription)"
            }
        }

        if let apiError = error as? GroqWhisperClient.APIError {
            switch apiError {
            case .fileReadError:
                return "Audio-Datei konnte nicht gelesen werden"
            case .httpError(let code, _):
                if code == 429 {
                    return "Groq API Rate-Limit erreicht — zu viele Anfragen, kurz warten"
                } else if (500...599).contains(code) {
                    return "Groq API Server-Fehler (\(code)) — spaeter nochmal versuchen"
                } else {
                    return "Groq API Fehler \(code) — \(error.localizedDescription)"
                }
            }
        }

        return "Transkription fehlgeschlagen — \(error.localizedDescription)"
    }

    static func describeGeminiError(_ error: Error) -> String {
        let nsError = error as NSError

        if nsError.domain == NSURLErrorDomain {
            switch nsError.code {
            case NSURLErrorNotConnectedToInternet, NSURLErrorNetworkConnectionLost:
                return "Gemini offline (kein Internet) — Rohtext verwendet"
            case NSURLErrorTimedOut:
                return "Gemini Timeout — Rohtext verwendet"
            case NSURLErrorCannotFindHost, NSURLErrorCannotConnectToHost, NSURLErrorDNSLookupFailed:
                return "Gemini nicht erreichbar — Rohtext verwendet"
            default:
                return "Gemini Netzwerkfehler — Rohtext verwendet"
            }
        }

        if let apiError = error as? GeminiClient.APIError {
            switch apiError {
            case .httpError(let code, _):
                if code == 429 {
                    return "Gemini Rate-Limit — Rohtext verwendet"
                } else if (500...599).contains(code) {
                    return "Gemini Server-Fehler (\(code)) — Rohtext verwendet"
                } else {
                    return "Gemini Fehler \(code) — Rohtext verwendet"
                }
            case .noData, .unexpectedResponse, .noTextInResponse:
                return "Gemini lieferte keine Antwort — Rohtext verwendet"
            }
        }

        return "Gemini-Korrektur fehlgeschlagen — Rohtext verwendet"
    }
}
