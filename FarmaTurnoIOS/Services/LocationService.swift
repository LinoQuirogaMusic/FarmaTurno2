import Foundation
import CoreLocation
import Combine

enum LocationError: Error {
    case authorizationDenied
    case authorizationRestricted
    case reverseGeocodingFailed(Error?)
    case unknown
}

protocol LocationServiceProtocol {
    var authorizationStatusPublisher: AnyPublisher<CLAuthorizationStatus, Never> { get }
    var userLocationPublisher: AnyPublisher<CLLocationCoordinate2D?, Never> { get }

    var currentAuthorizationStatus: CLAuthorizationStatus { get }

    func requestLocationPermission()
    func startUpdatingLocation()
    func stopUpdatingLocation()
    func reverseGeocode(location: CLLocationCoordinate2D) async throws -> String?
}

class LocationService: NSObject, LocationServiceProtocol, CLLocationManagerDelegate {
    private let locationManager = CLLocationManager()

    private let _authorizationStatusSubject = CurrentValueSubject<CLAuthorizationStatus, Never>(.notDetermined)
    var authorizationStatusPublisher: AnyPublisher<CLAuthorizationStatus, Never> {
        _authorizationStatusSubject.eraseToAnyPublisher()
    }
    var currentAuthorizationStatus: CLAuthorizationStatus {
        return _authorizationStatusSubject.value
    }

    private let _userLocationSubject = CurrentValueSubject<CLLocationCoordinate2D?, Never>(nil)
    var userLocationPublisher: AnyPublisher<CLLocationCoordinate2D?, Never> {
        _userLocationSubject.eraseToAnyPublisher()
    }

    override init() {
        super.init()
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyNearestTenMeters
        _authorizationStatusSubject.send(locationManager.authorizationStatus) // Send initial status
    }

    func requestLocationPermission() {
        if locationManager.authorizationStatus == .notDetermined {
            locationManager.requestWhenInUseAuthorization()
        }
    }

    func startUpdatingLocation() {
        let status = locationManager.authorizationStatus
        if status == .authorizedWhenInUse || status == .authorizedAlways {
            locationManager.startUpdatingLocation()
        } else {
            print("LocationService: Permission not granted (\(status.rawValue)), cannot start updating location. Requesting permission.")
            requestLocationPermission() // Attempt to request if not determined, or user can go to settings
        }
    }

    func stopUpdatingLocation() {
        locationManager.stopUpdatingLocation()
    }

    // MARK: - CLLocationManagerDelegate Methods

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        _authorizationStatusSubject.send(manager.authorizationStatus)
        print("LocationService: Authorization status changed to: \(manager.authorizationStatus.rawValue)")

        switch manager.authorizationStatus {
        case .authorizedWhenInUse, .authorizedAlways:
            // If permission is granted, can start location updates if needed immediately
            // locationManager.startUpdatingLocation() // Or let the caller decide
            print("LocationService: Permission granted.")
        case .denied:
            print("LocationService: Permission denied.")
            _userLocationSubject.send(nil) // Clear location if permission is denied
            // Notify ViewModel or UI to handle denial (e.g., show alert)
        case .restricted:
            print("LocationService: Permission restricted.")
            _userLocationSubject.send(nil)
            // Notify ViewModel or UI
        case .notDetermined:
            print("LocationService: Permission not determined.")
        @unknown default:
            print("LocationService: Unknown authorization status.")
        }
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }
        _userLocationSubject.send(location.coordinate)
        // print("LocationService: Location updated to \(location.coordinate)")
        // Decide if you want to stop after first update or keep it running
        // For continuous update, leave it. For one-time, call stopUpdatingLocation()
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        print("LocationService: Failed to get user location: \(error.localizedDescription)")
        _userLocationSubject.send(nil) // Publish nil on error
        // Notify ViewModel or UI
    }

    // MARK: - Geocoding

    func reverseGeocode(location: CLLocationCoordinate2D) async throws -> String? {
        let geocoder = CLGeocoder()
        let clLocation = CLLocation(latitude: location.latitude, longitude: location.longitude)

        do {
            let placemarks = try await geocoder.reverseGeocodeLocation(clLocation)
            // .locality is often the city name
            // .subLocality can be neighborhood
            // .administrativeArea can be state/province
            // .country for country
            let locality = placemarks.first?.locality
            print("LocationService: Reverse geocoded locality: \(locality ?? "N/A") for location \(location)")
            return locality
        } catch {
            print("LocationService: Reverse geocoding failed with error: \(error.localizedDescription)")
            throw LocationError.reverseGeocodingFailed(error)
        }
    }
}
