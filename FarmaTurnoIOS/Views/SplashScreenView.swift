import SwiftUI

struct SplashScreenView: View {
    @ObservedObject var mapsViewModel: MapsViewModel
    @ObservedObject var locationService: LocationService // Changed to ObservedObject if passed from App level

    var onFinishedLoading: () -> Void

    @State private var loadingProgress: Float = 0.0
    @State private var statusText: String = "Iniciando..."

    // Simpler timer for progress animation, less granular than before
    @State private var progressTimer = Timer.publish(every: 0.1, on: .main, in: .common).autoconnect()

    var body: some View {
        VStack(spacing: 20) {
            Spacer()
            Image("logo")
                .resizable()
                .scaledToFit()
                .frame(height: 150)
                .padding(.bottom, 20)

            if let error = mapsViewModel.errorState {
                Text(error.message)
                    .font(.headline)
                    .foregroundColor(.red)
                    .multilineTextAlignment(.center)
                    .padding()
                // Optionally, add a button to retry or go to settings
            } else {
                Text(statusText)
                    .font(.headline)
                ProgressView(value: loadingProgress, total: 1.0)
                    .progressViewStyle(LinearProgressViewStyle())
                    .frame(height: 8)
                    .padding(.horizontal, 40)
            }

            Spacer()
            Text("FarmaTurno Chile")
                .font(.caption)
                .padding(.bottom)
        }
        .onAppear(perform: startInitialChecks)
        .onReceive(mapsViewModel.$allPermissionGranted) { granted in
            updateStatusText()
            if granted && !mapsViewModel.isLoadingInitialData && mapsViewModel.farmaciasCercanas.isEmpty {
                 // Permissions were just granted, or re-confirmed, and data isn't loading yet.
                 // MapsViewModel's subscription to LocationService should trigger loadInitialPharmacyData.
                 // This is more of a reactive update.
            }
        }
        .onReceive(mapsViewModel.$isLoadingInitialData) { isLoading in
            updateStatusText()
            if !isLoading && mapsViewModel.isReadyToNavigateToMap {
                loadingProgress = 1.0 // Ensure progress is full
                onFinishedLoading()
            }
        }
        .onReceive(mapsViewModel.$errorState) { error in
            if error != nil {
                progressTimer.upstream.connect().cancel() // Stop progress animation on error
            }
        }
        .onReceive(progressTimer) { _ in
            if mapsViewModel.isLoadingInitialData && loadingProgress < 0.95 { // Don't let timer complete it fully
                loadingProgress += 0.02 // Adjust speed as needed
            } else if !mapsViewModel.isLoadingInitialData && mapsViewModel.isReadyToNavigateToMap {
                // This case should be caught by onReceive for isLoadingInitialData
            } else if !mapsViewModel.isLoadingInitialData && mapsViewModel.errorState != nil {
                // Error state, timer should be stopped.
            }
             else if !mapsViewModel.isLoadingInitialData && !mapsViewModel.isReadyToNavigateToMap && mapsViewModel.allPermissionGranted {
                // It's possible data loading finished but something else is not ready (e.g. locality pending)
                // Keep progress somewhat high but not full.
                if loadingProgress < 0.85 { loadingProgress = 0.85 }
            }
        }
    }

    private func startInitialChecks() {
        loadingProgress = 0.05
        updateStatusText() // Set initial status text

        if locationService.currentAuthorizationStatus == .notDetermined {
            locationService.requestLocationPermission()
        } else if locationService.currentAuthorizationStatus == .authorizedWhenInUse || locationService.currentAuthorizationStatus == .authorizedAlways {
            // MapsViewModel will handle data loading via its subscriptions
            // Ensure location updates are started if not already
            locationService.startUpdatingLocation()
        } else {
            // Denied or restricted, MapsViewModel's subscription will set errorState
        }
        // Start progress timer
        progressTimer = Timer.publish(every: 0.1, on: .main, in: .common).autoconnect()
    }

    private func updateStatusText() {
        if mapsViewModel.errorState != nil {
            // Error text is handled by the error display in body
            statusText = "Error" // Fallback, actual error shown in view
            return
        }

        if !mapsViewModel.allPermissionGranted {
            if locationService.currentAuthorizationStatus == .notDetermined {
                statusText = "Solicitando permiso de ubicación..."
            } else {
                statusText = "Se requiere permiso de ubicación." // Should be covered by errorState
            }
        } else if mapsViewModel.isFetchingPharmacies {
            statusText = "Cargando farmacias de turno..."
        } else if mapsViewModel.isProcessingData {
            statusText = "Procesando datos..."
        } else if mapsViewModel.isLoadingInitialData { // General loading state
             statusText = "Preparando todo..."
        }
         else if mapsViewModel.isReadyToNavigateToMap {
            statusText = "¡Listo!"
        } else {
            statusText = "Esperando ubicación..." // Default if permission granted but location not yet available
        }
    }
}

// Preview (Optional)
// struct SplashScreenView_Previews: PreviewProvider {
//     static var previews: some View {
//         let networkService = NetworkService() // Or a mock
//         let locationService = LocationService() // Or a mock
//         let mapsVM = MapsViewModel(networkService: networkService, locationService: locationService)
//         SplashScreenView(mapsViewModel: mapsVM, locationService: locationService, onFinishedLoading: {})
//     }
// }
