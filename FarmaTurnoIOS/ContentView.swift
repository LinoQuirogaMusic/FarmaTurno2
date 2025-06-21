import SwiftUI

struct ContentView: View {
    // State to manage which view is currently shown
    @State private var showSplashScreen = true

    // Initialize services and view models here as StateObjects
    // so they persist for the lifecycle of ContentView.
    @StateObject private var locationService = LocationService()
    @StateObject private var networkService = NetworkService() // If it had published properties, otherwise can be plain

    // MapsViewModel depends on LocationService and NetworkService
    // We need a way to initialize it once they are ready, or pass them into its init.
    // For simplicity, initialize it here.
    @StateObject private var mapsViewModel: MapsViewModel

    init() {
        // Initialize MapsViewModel with its dependencies.
        // This ensures that the same instances of services are used.
        let locService = LocationService()
        let netService = NetworkService() // Assuming NetworkService doesn't need to be @StateObject itself
        _locationService = StateObject(wrappedValue: locService)
        _networkService = StateObject(wrappedValue: netService) // Only if it were an ObservableObject
        _mapsViewModel = StateObject(wrappedValue: MapsViewModel(networkService: netService, locationService: locService))
    }

    var body: some View {
        Group {
            if showSplashScreen {
                SplashScreenView(
                    mapsViewModel: mapsViewModel,
                    locationService: locationService, // Pass LocationService for permission requests
                    onFinishedLoading: {
                        // This callback is triggered by SplashScreenView when it's done.
                        withAnimation { // Optional animation for transition
                            showSplashScreen = false
                        }
                    }
                )
            } else {
                MapsScreenView(mapsViewModel: mapsViewModel)
            }
        }
        .environmentObject(locationService) // Make locationService available to MapsScreenView if needed (e.g., for settings button)
        // .environmentObject(mapsViewModel) // Alternative way to pass ViewModel if many nested views need it
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
