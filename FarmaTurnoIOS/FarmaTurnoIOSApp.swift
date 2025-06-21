import SwiftUI
import GoogleMobileAds // For AdMob initialization
// import Firebase // If using Firebase

@main
struct FarmaTurnoIOSApp: App {

    init() {
        // Configure AdMob
        GADMobileAds.sharedInstance().start(completionHandler: nil)
        print("AdMob SDK Initialized.")

        // Configure Firebase (if used)
        // FirebaseApp.configure()
        // print("Firebase Configured (if enabled).")

        print("FarmaTurnoIOSApp Initialized.")
    }

    var body: some Scene {
        WindowGroup {
            // ContentView will now manage the splash screen vs. main app view logic
            ContentView()
        }
    }
}
