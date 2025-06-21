import Foundation
import Combine
import CoreLocation
import MapKit // For MKMapItem

class MapsViewModel: ObservableObject {
    // MARK: - Published Properties (UI State)
    @Published var isSheetOpen: Bool = false
    @Published var farmaciasCercanas: [Farmacia] = []
    @Published var actualUserLocation: CLLocationCoordinate2D?
    @Published var actualUserLocality: String = ""

    @Published var isLoadingInitialData: Bool = false
    @Published var isFetchingPharmacies: Bool = false
    @Published var isProcessingData: Bool = false

    @Published var mapReady: Bool = false
    @Published var selectedPharmacy: Farmacia?
    @Published var allPermissionGranted: Bool = false
    @Published var errorState: ErrorState?

    // Use constant for search radius
    let searchRadiusMeters: Double = Constants.pharmacySearchRadiusMeters

    // MARK: - Dependencies
    private let networkService: NetworkServiceProtocol
    private let locationService: LocationServiceProtocol
    private var cancellables = Set<AnyCancellable>()

    private var allFetchedPharmacies: [Farmacia] = []

    init(networkService: NetworkServiceProtocol, locationService: LocationServiceProtocol) {
        self.networkService = networkService
        self.locationService = locationService

        subscribeToLocationUpdates()
        subscribeToAuthorizationStatus()

        if locationService.currentAuthorizationStatus == .authorizedWhenInUse || locationService.currentAuthorizationStatus == .authorizedAlways {
            self.allPermissionGranted = true
            self.locationService.startUpdatingLocation()
        } else if locationService.currentAuthorizationStatus == .notDetermined {
            // Waiting for SplashScreenView to call locationService.requestLocationPermission()
        } else {
            self.allPermissionGranted = false
            self.errorState = ErrorState(message: "Se requiere permiso de ubicación para encontrar farmacias.")
        }
    }

    private func subscribeToLocationUpdates() {
        locationService.userLocationPublisher
            .receive(on: DispatchQueue.main)
            .sink { [weak self] location in
                guard let self = self else { return }
                let oldLocation = self.actualUserLocation
                self.actualUserLocation = location
                if let loc = location {
                    Task {
                        // Fetch locality only if location significantly changed or locality is empty
                        if self.actualUserLocality.isEmpty || oldLocation == nil || (oldLocation != nil && CLLocation(latitude: oldLocation!.latitude, longitude: oldLocation!.longitude).distance(from: CLLocation(latitude: loc.latitude, longitude: loc.longitude)) > 1000) { // e.g. > 1km change
                            await self.fetchUserLocality(location: loc)
                        }
                        if !self.allFetchedPharmacies.isEmpty { // Process pharmacies if data is already loaded
                             self.processAndFilterPharmacies()
                        }
                    }
                }
            }
            .store(in: &cancellables)
    }

    private func subscribeToAuthorizationStatus() {
        locationService.authorizationStatusPublisher
            .receive(on: DispatchQueue.main)
            .sink { [weak self] status in
                guard let self = self else { return }
                switch status {
                case .authorizedWhenInUse, .authorizedAlways:
                    if !self.allPermissionGranted { // Only react if status truly changed to granted
                        self.allPermissionGranted = true
                        self.errorState = nil
                        self.locationService.startUpdatingLocation()
                        if self.allFetchedPharmacies.isEmpty && !self.isFetchingPharmacies {
                            self.loadInitialPharmacyData()
                        }
                    } else { // Already granted, ensure location updates are on if not.
                        self.allPermissionGranted = true // ensure it's true
                        self.locationService.startUpdatingLocation()
                    }
                case .denied, .restricted:
                    self.allPermissionGranted = false
                    self.actualUserLocation = nil
                    self.farmaciasCercanas = []
                    self.errorState = ErrorState(message: "El permiso de ubicación fue denegado. Habilítalo en Ajustes para usar esta función.")
                case .notDetermined:
                    self.allPermissionGranted = false
                @unknown default:
                    self.allPermissionGranted = false
                    self.errorState = ErrorState(message: "Estado de permiso de ubicación desconocido.")
                }
            }
            .store(in: &cancellables)
    }

    // MARK: - Data Loading and Processing
    func loadInitialPharmacyData() {
        guard allPermissionGranted else {
            if locationService.currentAuthorizationStatus == .notDetermined {
                locationService.requestLocationPermission()
            }
            return
        }

        guard !isFetchingPharmacies else { return }

        isFetchingPharmacies = true
        isLoadingInitialData = true
        errorState = nil

        Task {
            do {
                let pharmacies = try await networkService.fetchPharmacyData()
                DispatchQueue.main.async {
                    self.allFetchedPharmacies = pharmacies
                    self.isFetchingPharmacies = false
                    self.processAndFilterPharmacies()
                }
            } catch {
                DispatchQueue.main.async {
                    self.isFetchingPharmacies = false
                    self.isLoadingInitialData = false
                    self.errorState = ErrorState(message: "No se pudieron cargar las farmacias. Verifica tu conexión.")
                }
            }
        }
    }

    private func fetchUserLocality(location: CLLocationCoordinate2D) async {
        do {
            let localityName = try await locationService.reverseGeocode(location: location)
            DispatchQueue.main.async {
                self.actualUserLocality = localityName ?? ""
                print("MapsViewModel: User locality updated to: \(self.actualUserLocality)")
                // If pharmacies were waiting for locality, re-process
                if !self.allFetchedPharmacies.isEmpty {
                    self.processAndFilterPharmacies()
                }
            }
        } catch {
            DispatchQueue.main.async {
                print("MapsViewModel: Error fetching user locality: \(error)")
                self.actualUserLocality = "" // Clear or set to a default/error state
            }
        }
    }

    private func processAndFilterPharmacies() {
        guard let currentUserLocation = actualUserLocation, !allFetchedPharmacies.isEmpty else {
            // Can't process without user location or fetched data
            // If allFetchedPharmacies is empty but not fetching, means initial load failed or not started
            if allFetchedPharmacies.isEmpty && !isFetchingPharmacies && allPermissionGranted {
                // This might be a good place to retry or ensure loadInitialPharmacyData was called
                print("MapsViewModel: Attempting to process pharmacies, but no raw data and not fetching. Triggering load.")
                loadInitialPharmacyData()
            } else if currentUserLocation == nil && allPermissionGranted {
                print("MapsViewModel: Waiting for user location to process pharmacies.")
                locationService.startUpdatingLocation() // Ensure it's running
            }
            return
        }

        print("MapsViewModel: Processing and filtering pharmacies. User Loc: \(currentUserLocation), User Locality: \(actualUserLocality)")
        isProcessingData = true

        let filtered = allFetchedPharmacies.filter { farmacia in
            let pharmacyLocation = CLLocation(latitude: farmacia.lat, longitude: farmacia.long)
            let userCLLocation = CLLocation(latitude: currentUserLocation.latitude, longitude: currentUserLocation.longitude)

            let distance = userCLLocation.distance(from: pharmacyLocation) // in meters

            let isInRadius = distance <= searchRadiusMeters

            // Check if comuna matches user's locality if locality is known
            var isInLocality = false
            if !actualUserLocality.isEmpty {
                // Case-insensitive comparison and remove accents might be good here for robustness
                isInLocality = farmacia.comuna.localizedCaseInsensitiveCompare(actualUserLocality) == .orderedSame
            }

            return isInRadius || isInLocality
        }

        DispatchQueue.main.async {
            self.farmaciasCercanas = filtered
            self.isProcessingData = false
            self.isLoadingInitialData = false // All initial loading steps are complete
            // isDataExtracted equivalent
            print("MapsViewModel: Found \(filtered.count) nearby pharmacies.")
        }
    }

    // MARK: - UI Interaction Methods
    func onMapReady() {
        self.mapReady = true
        print("MapsViewModel: Map component is ready.")
    }

    func selectPharmacy(pharmacy: Farmacia?) { // Allow nil to deselect
        self.selectedPharmacy = pharmacy
        if pharmacy == nil {
            self.isSheetOpen = false // Ensure sheet closes if pharmacy is deselected programmatically
        }
    }

    // MARK: - Actions for Pharmacy Details
    func openDirectionsForSelectedPharmacy() {
        guard let pharmacy = selectedPharmacy else { return }

        let coordinate = CLLocationCoordinate2D(latitude: pharmacy.lat, longitude: pharmacy.long)
        let mapItem = MKMapItem(placemark: MKPlacemark(coordinate: coordinate))
        mapItem.name = pharmacy.nombre
        // Open in Apple Maps with driving directions
        mapItem.openInMaps(launchOptions: [MKLaunchOptionsDirectionsModeKey: MKLaunchOptionsDirectionsModeDriving])
        print("MapsViewModel: Opening directions for \(pharmacy.nombre)")
    }

    func callSelectedPharmacy() {
        guard let pharmacy = selectedPharmacy,
              let url = URL(string: "tel:\(pharmacy.telefono.filter("0123456789".contains))") else { // Ensure phone number is clean
            print("MapsViewModel: Could not create phone call URL for \(selectedPharmacy?.telefono ?? "N/A")")
            return
        }

        // This requires UIApplication, which is tricky to call directly from ViewModel in pure SwiftUI.
        // Typically, this action would be passed to the View, or a dedicated service would handle it.
        // For now, logging the intent. The View will need to handle the UIApplication.shared.open call.
        print("MapsViewModel: Attempting to call \(url). View should handle UIApplication.shared.open().")

        // In a real app, you might have a dedicated URLOpeningService or pass a closure to the View.
        // Example: `UIApplication.shared.open(url, options: [:], completionHandler: nil)` from View context.
        // For now, the view will need to implement the actual call to UIApplication.shared.open
        NotificationCenter.default.post(name: .callPhoneNumber, object: url)

    }

    // Helper for SplashScreen to know when it can navigate
    var isReadyToNavigateToMap: Bool {
        return allPermissionGranted && !allFetchedPharmacies.isEmpty && !isLoadingInitialData && !isProcessingData
    }

    // Called from SplashScreenView when location permission is granted by user action there
    // This is a bit redundant if observing publisher, but can be used for direct trigger.
    // func userDidGrantPermissionFromSplash() {
    //    if locationService.currentAuthorizationStatus == .authorizedWhenInUse || locationService.currentAuthorizationStatus == .authorizedAlways {
    //        self.allPermissionGranted = true
    //        loadInitialPharmacyData() // Load data now that permission is confirmed
    //    }
    // }
}

// For error display
struct ErrorState: Identifiable {
    let id = UUID()
    let message: String
}

// Notification name for initiating a call
extension Notification.Name {
    static let callPhoneNumber = Notification.Name("callPhoneNumberNotification")
}
