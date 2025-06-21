import SwiftUI
import MapKit // For Map View
import UIKit // For UIApplication

struct MapsScreenView: View {
    @StateObject var mapsViewModel: MapsViewModel

    @State private var region = MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: -33.45694, longitude: -70.64827),
        span: MKCoordinateSpan(latitudeDelta: 0.1, longitudeDelta: 0.1)
    )

    private let searchRadius: Double = 12000.0
    private let circleOverlayColor = Color(red: 0.2, green: 0.6, blue: 0.8, opacity: 0.3)

    var body: some View {
        Group {
            if mapsViewModel.allPermissionGranted {
                ZStack {
                    Map(coordinateRegion: $region,
                        interactionModes: .all,
                        showsUserLocation: true,
                        annotationItems: mapsViewModel.farmaciasCercanas) { pharmacy in

                        MapAnnotation(coordinate: CLLocationCoordinate2D(latitude: pharmacy.lat, longitude: pharmacy.long)) {
                            Image("farm_marker")
                                .resizable()
                                .frame(width: 28, height: 40)
                                .scaleEffect(mapsViewModel.selectedPharmacy?.id == pharmacy.id ? 1.2 : 1.0)
                                .animation(.easeInOut, value: mapsViewModel.selectedPharmacy?.id)
                                .onTapGesture {
                                    mapsViewModel.selectPharmacy(pharmacy: pharmacy)
                                    // mapsViewModel.isSheetOpen = true // ViewModel will set this if pharmacy is selected
                                    withAnimation {
                                        region.center = CLLocationCoordinate2D(latitude: pharmacy.lat, longitude: pharmacy.long)
                                    }
                                }
                        }
                    }
                    .edgesIgnoringSafeArea(.top)
                    .onAppear {
                        setupInitialRegion()
                        mapsViewModel.onMapReady()
                    }
                    .onChange(of: mapsViewModel.actualUserLocation) { newLocation in
                        if let loc = newLocation, region.center.latitude == -33.45694 { // Update only if still default
                            withAnimation {
                                region.center = loc
                                region.span = MKCoordinateSpan(latitudeDelta: 0.1, longitudeDelta: 0.1)
                            }
                        }
                    }
                    .onChange(of: mapsViewModel.selectedPharmacy) { pharmacy in
                        if let pharm = pharmacy {
                            withAnimation(.easeInOut(duration: 0.5)) {
                                region.center = CLLocationCoordinate2D(latitude: pharm.lat, longitude: pharm.long)
                                // Adjust span for pharmacyZoom from Android if needed
                                // region.span = MKCoordinateSpan(latitudeDelta: 0.01, longitudeDelta: 0.01)
                            }
                        } else if mapsViewModel.isSheetOpen == false { // Only pan back if sheet is also closed
                            if let userLoc = mapsViewModel.actualUserLocation {
                                 withAnimation(.easeInOut(duration: 0.5)) {
                                    region.center = userLoc
                                    // Adjust span for areaZoom from Android if needed
                                    // region.span = MKCoordinateSpan(latitudeDelta: 0.1, longitudeDelta: 0.1)
                                 }
                            }
                        }
                    }
                    .onChange(of: mapsViewModel.isSheetOpen) { isOpen in
                        if !isOpen && mapsViewModel.selectedPharmacy != nil {
                            // If sheet is closed programmatically or by swipe, deselect pharmacy
                            // mapsViewModel.selectPharmacy(pharmacy: nil) // This will trigger the above .onChange
                        }
                    }

                    // Ad Banner Placeholder
                    VStack {
                        Spacer()
                        // BannerAdView(adUnitID: "your_admob_banner_id") // Replace with actual ID
                        Text("Ad Banner Placeholder")
                             .frame(maxWidth: .infinity)
                             .frame(height: 50)
                             .background(Color.gray.opacity(0.3))
                             .padding(.bottom, UIApplication.shared.windows.first(where: { $0.isKeyWindow })?.safeAreaInsets.bottom ?? 0)
                    }
                    .edgesIgnoringSafeArea(.bottom)
                }
                .sheet(isPresented: $mapsViewModel.isSheetOpen, onDismiss: {
                    if mapsViewModel.selectedPharmacy != nil { // If sheet dismissed by swipe
                        mapsViewModel.selectPharmacy(pharmacy: nil)
                    }
                }) {
                    if let pharmacy = mapsViewModel.selectedPharmacy {
                        PharmacyDetailsSheetView(pharmacy: pharmacy, mapsViewModel: mapsViewModel)
                            .presentationDetents([.fraction(0.32), .medium])
                            .presentationDragIndicator(.visible)
                    } else {
                        // This state should ideally not be reached if isSheetOpen is true
                        Text("Cargando detalles...")
                            .onAppear { mapsViewModel.isSheetOpen = false }
                    }
                }
                .onReceive(NotificationCenter.default.publisher(for: .callPhoneNumber)) { notification in
                    if let url = notification.object as? URL {
                        if UIApplication.shared.canOpenURL(url) {
                            UIApplication.shared.open(url, options: [:], completionHandler: nil)
                        } else {
                            print("Cannot open URL: \(url)")
                            // Show an alert to the user if the call cannot be made
                        }
                    }
                }
            } else {
                VStack {
                    Text(mapsViewModel.errorState?.message ?? "Se requiere permiso de ubicación para mostrar el mapa.")
                        .multilineTextAlignment(.center)
                        .padding()
                    if mapsViewModel.errorState == nil { ProgressView() } // Show progress if no specific error
                    // Button to open settings if permission denied
                    if locationService.currentAuthorizationStatus == .denied || locationService.currentAuthorizationStatus == .restricted {
                        Button("Abrir Ajustes") {
                            if let url = URL(string: UIApplication.openSettingsURLString), UIApplication.shared.canOpenURL(url) {
                                UIApplication.shared.open(url)
                            }
                        }
                        .padding()
                    }
                }
            }
        }
        // Inject LocationService for the permission denied button
        .environmentObject(locationService)
    }

    // Make locationService accessible for the button
    @EnvironmentObject var locationService: LocationService


    private func setupInitialRegion() {
        if let userLocation = mapsViewModel.actualUserLocation {
            region = MKCoordinateRegion(
                center: userLocation,
                span: MKCoordinateSpan(latitudeDelta: 0.1, longitudeDelta: 0.1)
            )
        }
    }
}


// Preview
// struct MapsScreenView_Previews: PreviewProvider {
//     static var previews: some View {
//         let networkService = NetworkService()
//         let locationService = LocationService()
//         let mapsVM = MapsViewModel(networkService: networkService, locationService: locationService)
//         mapsVM.allPermissionGranted = true
//         mapsVM.actualUserLocation = CLLocationCoordinate2D(latitude: -33.45, longitude: -70.65)
//         mapsVM.farmaciasCercanas = [
//             Farmacia(id: "1", nombre: "Farmacia Central", direccion: "Av. Principal 123", lat: -33.45, long: -70.65, comuna: "Santiago", hApertura: "09:00", hCierre: "21:00", telefono: "123456789"),
//             Farmacia(id: "2", nombre: "Farmacia Cruz Verde", direccion: "Calle Secundaria 456", lat: -33.455, long: -70.655, comuna: "Providencia", hApertura: "08:00", hCierre: "20:00", telefono: "987654321")
//         ]
//        return MapsScreenView(mapsViewModel: mapsVM).environmentObject(locationService)
//     }
// }
