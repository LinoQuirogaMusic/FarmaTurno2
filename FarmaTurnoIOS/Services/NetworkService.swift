import Foundation

// Custom Error type for more specific error handling
enum NetworkError: Error {
    case badURL
    case requestFailed(Error)
    case badServerResponse(statusCode: Int)
    case decodingError(Error)
    case unknown
}

protocol NetworkServiceProtocol {
    // Using async/await for modern Swift concurrency
    func fetchPharmacyData() async throws -> [Farmacia]
}

class NetworkService: NetworkServiceProtocol {
    private let baseURL = "https://midas.minsal.cl/farmacia_v2/WS/"
    private let pharmacyEndpoint = "getLocalesTurnos.php"

    private let userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36" // Updated User-Agent slightly
    private let referer = "https://midas.minsal.cl/farmacia_v2/WS/getLocalesTurnos.php"

    func fetchPharmacyData() async throws -> [Farmacia] {
        guard let url = URL(string: baseURL + pharmacyEndpoint) else {
            throw NetworkError.badURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue(userAgent, forHTTPHeaderField: "User-Agent")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("es-CL,es;q=0.9,en-US;q=0.8,en;q=0.7", forHTTPHeaderField: "Accept-Language")
        request.setValue(referer, forHTTPHeaderField: "Referer")

        // Standard URLSession SSL validation will be used.
        // If midas.minsal.cl has an invalid certificate (e.g., self-signed, expired, wrong host),
        // this request will fail. This is the secure default.
        // The Android app's SSL bypass is a security risk not replicated here.

        let data: Data
        let response: URLResponse

        do {
            (data, response) = try await URLSession.shared.data(for: request)
        } catch {
            // Handle underlying URLSession errors (e.g., no internet connection)
            throw NetworkError.requestFailed(error)
        }

        guard let httpResponse = response as? HTTPURLResponse else {
            throw NetworkError.unknown // Should not happen if dataTask succeeds
        }

        guard httpResponse.statusCode == 200 else {
            // Log detailed error if possible (e.g., response body for non-200)
            print("NetworkService Error: HTTP Status Code: \(httpResponse.statusCode)")
            // You could try to decode error message from `data` if API provides one
            throw NetworkError.badServerResponse(statusCode: httpResponse.statusCode)
        }

        // The Android app received a JSON String and then parsed it.
        // The Farmacia struct now uses CodingKeys to map from snake_case JSON.
        do {
            let decoder = JSONDecoder()
            // No need to set .keyDecodingStrategy = .convertFromSnakeCase if CodingKeys are defined in Farmacia
            let pharmacies = try decoder.decode([Farmacia].self, from: data)
            return pharmacies
        } catch {
            print("NetworkService Error: JSON Decoding Error: \(error)")
            // For debugging, try to print the data as a string
            // if let jsonString = String(data: data, encoding: .utf8) {
            //     print("Failed to decode JSON: \(jsonString)")
            // }
            throw NetworkError.decodingError(error)
        }
    }

    // The completion handler version can be removed if only async/await is used.
    // For brevity, I'm focusing on the async/await version as per modern Swift practices.
}
