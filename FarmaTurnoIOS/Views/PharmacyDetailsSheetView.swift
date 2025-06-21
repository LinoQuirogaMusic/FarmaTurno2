import SwiftUI

struct PharmacyDetailsSheetView: View {
    let pharmacy: Farmacia
    @ObservedObject var mapsViewModel: MapsViewModel

    // Colors from Android theme (approximations)
    // Titulos: Not specified, using .primary for now
    // TextSecondary: Color.gray
    // TextPrimary: .primary
    // Health-related text color (0xFF004D40): Color(red: 0, green: 0.30, blue: 0.25)

    var body: some View {
        VStack(alignment: .center, spacing: 12) {
            Text(pharmacy.nombre.replacingOccurrences(of: "FARMACIA EXTERNA", with: "").trimmingCharacters(in: .whitespacesAndNewlines))
                .font(.title2) // headlineMedium equivalent
                .fontWeight(.bold)
                .foregroundColor(.primary) // Titulos color
                .multilineTextAlignment(.center)
                .lineLimit(2)

            Text(pharmacy.comuna)
                .font(.body) // bodyLarge equivalent
                .foregroundColor(.gray) // TextSecondary
                .multilineTextAlignment(.center)

            Text("Apertura: \(pharmacy.hApertura)")
                .font(.body)
                .foregroundColor(.primary) // TextPrimary

            Text("Cierre: \(pharmacy.hCierre)")
                .font(.body)
                .foregroundColor(Color(red: 0, green: 0.30, blue: 0.25)) // Health-related color from Android

            Spacer() // Pushes action buttons towards the bottom of their allocated space if sheet is larger

            HStack(spacing: 40) { // Increased spacing for larger tap targets
                Button {
                    mapsViewModel.callSelectedPharmacy()
                } label: {
                    VStack(spacing: 4) {
                        Image(systemName: "phone.fill")
                            .font(.system(size: 28)) // Slightly smaller than .title, similar to Android's 64dp icon with padding
                        Text("Llamar")
                            .font(.caption)
                    }
                }
                .foregroundColor(.red) // Tint from Android

                Button {
                    mapsViewModel.openDirectionsForSelectedPharmacy()
                } label: {
                    VStack(spacing: 4) {
                        Image(systemName: "location.fill")
                            .font(.system(size: 28))
                        Text("Navegar")
                            .font(.caption)
                    }
                }
                .foregroundColor(.red) // Tint from Android
            }
            .padding(.vertical, 10) // Add some vertical padding around buttons

            // If implementing Pager, this view would be wrapped or this would be one page.
            // For now, this shows details for the single `pharmacy` passed in.
            // The Android version had HorizontalPager here.
            // A simple way to indicate more pharmacies if list is > 1:
            if mapsViewModel.farmaciasCercanas.count > 1 {
                 Text("Desliza para ver otras farmacias cercanas") // Placeholder for pager hint
                    .font(.caption2)
                    .foregroundColor(.gray)
                    .padding(.top, 5)
            }
             Spacer().frame(height: 10) // Ensure some padding at the bottom
        }
        .padding(.horizontal, 20)
        .padding(.top, 20) // Padding at the top of the sheet content
        .frame(maxWidth: .infinity, maxHeight: .infinity) // Allow it to fill the sheet
    }
}

// Preview (Optional)
// struct PharmacyDetailsSheetView_Previews: PreviewProvider {
//     static var previews: some View {
//         let previewPharmacy = Farmacia(id: "1", nombre: "Farmacia Ejemplo FARMACIA EXTERNA", direccion: "Calle Falsa 123", lat: 0.0, long: 0.0, comuna: "Providencia", hApertura: "09:00", hCierre: "21:00", telefono: "123456789")
//         let mapsVM = MapsViewModel()
//         mapsVM.farmaciasCercanas = [previewPharmacy] // For the pager hint logic
//         return PharmacyDetailsSheetView(pharmacy: previewPharmacy, mapsViewModel: mapsVM)
//             .previewLayout(.fixed(width: 375, height: 250)) // Simulate sheet height
//     }
// }
