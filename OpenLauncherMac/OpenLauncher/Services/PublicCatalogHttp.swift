import Foundation

/// Only public catalogs enter the native fallback. Authenticated requests never use curl.
actor PublicCatalogHttp {
    static let shared = PublicCatalogHttp()
    private var cache: [String: (Date, Data)] = [:]
    private var pending: [String: Task<Data, Error>] = [:]

    func get(_ url: String, force: Bool = false) async throws -> Data {
        guard ["https://models.dev/api.json", "https://openrouter.ai/api/v1/models"].contains(url) else {
            throw URLError(.unsupportedURL)
        }
        try Task.checkCancellation()
        if !force, let (at, data) = cache[url], Date().timeIntervalSince(at) < 600 { return data }
        if let task = pending[url] {
            let data = try await task.value
            try Task.checkCancellation()
            return data
        }
        // The shared download outlives a model switch, but has its own strict timeout.
        let task = Task.detached { try await Self.download(url) }
        pending[url] = task
        defer { pending[url] = nil }
        let data = try await task.value
        cache[url] = (Date(), data)
        try Task.checkCancellation()
        return data
    }

    private static func validate(_ data: Data) throws -> Data {
        guard data.count <= 16_777_216,
              try JSONSerialization.jsonObject(with: data) is [String: Any] else {
            throw URLError(.cannotParseResponse)
        }
        return data
    }

    private static func download(_ url: String) async throws -> Data {
        do {
            var request = URLRequest(url: URL(string: url)!)
            request.timeoutInterval = 8
            request.cachePolicy = .reloadIgnoringLocalCacheData
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
                throw URLError(.badServerResponse)
            }
            return try validate(data)
        } catch {
            Logger.shared.warn("PublicCatalogHttp", "download", "Öffentlicher Katalog: nativer HTTPS-Fallback")
        }
        return try await Task.detached {
            let process = Process()
            let output = Pipe()
            process.executableURL = URL(fileURLWithPath: "/usr/bin/curl")
            process.arguments = ["--disable", "--fail", "--silent", "--compressed",
                                 "--proto", "=https", "--connect-timeout", "6", "--max-time", "20",
                                 "--max-filesize", "16777216", url]
            process.standardOutput = output
            process.standardError = FileHandle.nullDevice
            try process.run()
            // Drain while curl runs; waiting before reading can deadlock on a full pipe.
            let data = output.fileHandleForReading.readDataToEndOfFile()
            process.waitUntilExit()
            guard process.terminationStatus == 0 else { throw URLError(.cannotLoadFromNetwork) }
            return try validate(data)
        }.value
    }
}
