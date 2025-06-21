import Foundation

enum Constants {
    // AdMob Test Banner ID. Replace with actual ID for production.
    static let adMobBannerTestID = "ca-app-pub-3940256099942544/2934735716"

    // Search Radius from Android app (MainActivity.radius)
    static let pharmacySearchRadiusMeters: Double = 12000.0

    // Notification Names
    enum Notifications {
        static let callPhoneNumber = Notification.Name("callPhoneNumberNotification")
    }

    // Other app-wide constants can be added here
    // e.g. API keys if they were not embedded, default zoom levels, etc.
    // static let initialMapLatitude = -33.45694 // Santiago
    // static let initialMapLongitude = -70.64827
    // static let defaultAreaZoomSpan = 0.1 // For MKCoordinateSpan
    // static let defaultPharmacyZoomSpan = 0.01 // For MKCoordinateSpan
}
