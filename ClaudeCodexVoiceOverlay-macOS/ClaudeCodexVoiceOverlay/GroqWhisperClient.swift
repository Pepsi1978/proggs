import Foundation

final class GroqWhisperClient {
    private let apiKey: String
    private let endpoint = URL(string: "https://api.groq.com/openai/v1/audio/transcriptions")!
    private let retryableStatusCodes: Set<Int> = [429, 500, 503]
    private let maxRetries = 3
    private let delays: [TimeInterval] = [2, 4, 8]

    init(apiKey: String) {
        self.apiKey = apiKey
    }

    func transcribe(fileURL: URL, completion: @escaping (Result<String, Error>) -> Void) {
        DispatchQueue.global(qos: .userInitiated).async { [self] in
            self.transcribeSync(fileURL: fileURL, attempt: 0, completion: completion)
        }
    }

    private func transcribeSync(fileURL: URL, attempt: Int, completion: @escaping (Result<String, Error>) -> Void) {
        guard let audioData = try? Data(contentsOf: fileURL) else {
            completion(.failure(APIError.fileReadError))
            return
        }

        let boundary = UUID().uuidString
        var request = URLRequest(url: endpoint)
        request.httpMethod = "POST"
        request.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
        request.timeoutInterval = 180

        var body = Data()
        let fields: [(String, String)] = [
            ("model", "whisper-large-v3"),
            ("language", "de"),
            ("response_format", "text"),
            ("temperature", "0"),
        ]
        for (key, value) in fields {
            body.append("--\(boundary)\r\n".data(using: .utf8)!)
            body.append("Content-Disposition: form-data; name=\"\(key)\"\r\n\r\n".data(using: .utf8)!)
            body.append("\(value)\r\n".data(using: .utf8)!)
        }
        // File part
        body.append("--\(boundary)\r\n".data(using: .utf8)!)
        body.append("Content-Disposition: form-data; name=\"file\"; filename=\"recording.wav\"\r\n".data(using: .utf8)!)
        body.append("Content-Type: audio/wav\r\n\r\n".data(using: .utf8)!)
        body.append(audioData)
        body.append("\r\n--\(boundary)--\r\n".data(using: .utf8)!)

        request.httpBody = body

        let task = URLSession.shared.dataTask(with: request) { [self] data, response, error in
            if let error = error {
                completion(.failure(error))
                return
            }

            let statusCode = (response as? HTTPURLResponse)?.statusCode ?? 0

            if (200...299).contains(statusCode), let data = data,
               let text = String(data: data, encoding: .utf8)?.trimmingCharacters(in: .whitespacesAndNewlines),
               !text.isEmpty {
                completion(.success(text))
                return
            }

            if self.retryableStatusCodes.contains(statusCode) && attempt < self.maxRetries {
                let delay = self.delays[attempt]
                NSLog("Groq %d - Versuch %d/%d, warte %.0fs...", statusCode, attempt + 1, self.maxRetries, delay)
                Thread.sleep(forTimeInterval: delay)
                self.transcribeSync(fileURL: fileURL, attempt: attempt + 1, completion: completion)
                return
            }

            let responseText = data.flatMap { String(data: $0, encoding: .utf8) } ?? "no response"
            completion(.failure(APIError.httpError(statusCode, responseText)))
        }
        task.resume()
    }

    enum APIError: Error, LocalizedError {
        case fileReadError
        case httpError(Int, String)

        var errorDescription: String? {
            switch self {
            case .fileReadError: return "Audio-Datei konnte nicht gelesen werden"
            case .httpError(let code, let msg): return "Groq API Fehler \(code): \(msg)"
            }
        }
    }
}
