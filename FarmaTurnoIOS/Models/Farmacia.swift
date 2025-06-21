import Foundation

struct Farmacia: Identifiable, Codable {
    let id: String // From local_id
    let nombre: String // From local_nombre
    let direccion: String // From local_direccion
    let lat: Double // From local_lat
    let long: Double // From local_lng
    let comuna: String // From comuna_nombre
    let hApertura: String // From funcionamiento_hora_apertura
    let hCierre: String // From funcionamiento_hora_cierre
    let telefono: String // From local_telefono
    let patrocinada: Bool? // This field was in Android model, make optional if not in JSON

    enum CodingKeys: String, CodingKey {
        case id = "local_id"
        case nombre = "local_nombre"
        case direccion = "local_direccion"
        case lat = "local_lat"
        case long = "local_lng"
        case comuna = "comuna_nombre"
        case hApertura = "funcionamiento_hora_apertura"
        case hCierre = "funcionamiento_hora_cierre"
        case telefono = "local_telefono"
        case patrocinada // Assuming JSON key is "patrocinada" if it exists
    }

    // Initializer for easier creation, though Codable provides one
    init(id: String, nombre: String, direccion: String, lat: Double, long: Double, comuna: String, hApertura: String, hCierre: String, telefono: String, patrocinada: Bool? = false) {
        self.id = id
        self.nombre = nombre
        self.direccion = direccion
        self.lat = lat
        self.long = long
        self.comuna = comuna
        self.hApertura = hApertura
        self.hCierre = hCierre
        self.telefono = telefono
        self.patrocinada = patrocinada
    }

    // Default initializer for Codable
    // If any property in JSON might be missing or null, they should be Optional in Swift.
    // For `lat` and `long`, if they can be empty strings in JSON but need to be Doubles,
    // custom decoding logic or a String type in the struct might be needed first, then conversion.
    // The Android repo geocoded if lat/lng was missing.
    // For now, assuming lat/long are valid doubles or parsing will fail.
}

// Example of how it might be decoded if lat/long are sometimes empty strings
/*
extension Farmacia {
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(String.self, forKey: .id)
        nombre = try container.decode(String.self, forKey: .nombre)
        direccion = try container.decode(String.self, forKey: .direccion)
        comuna = try container.decode(String.self, forKey: .comuna)
        hApertura = try container.decode(String.self, forKey: .hApertura)
        hCierre = try container.decode(String.self, forKey: .hCierre)
        telefono = try container.decode(String.self, forKey: .telefono)
        patrocinada = try container.decodeIfPresent(Bool.self, forKey: .patrocinada) ?? false

        // Handle potentially problematic lat/long
        let latString = try container.decode(String.self, forKey: .lat)
        lat = Double(latString) ?? 0.0 // Default to 0.0 if conversion fails, or handle error

        let longString = try container.decode(String.self, forKey: .long)
        long = Double(longString) ?? 0.0 // Default to 0.0 if conversion fails, or handle error
    }
}
*/
