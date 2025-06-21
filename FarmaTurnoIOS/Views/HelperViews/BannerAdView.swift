import SwiftUI
import GoogleMobileAds // Will need to add Google Mobile Ads SDK package
import UIKit

// This struct will wrap the GADBannerView from Google Mobile Ads SDK
struct BannerAdView: UIViewRepresentable {

    let adUnitID: String

    func makeUIView(context: Context) -> GADBannerView {
        let bannerView = GADBannerView(adSize: GADAdSizeBanner)
        bannerView.adUnitID = adUnitID
        // Attempt to find the root view controller more safely
        bannerView.rootViewController = findRootViewController()
        bannerView.load(GADRequest())
        return bannerView
    }

    func updateUIView(_ uiView: GADBannerView, context: Context) {
        // No updates needed for a static banner ad after initial load.
    }

    private func findRootViewController() -> UIViewController? {
        // Get the first key window scene
        guard let windowScene = UIApplication.shared.connectedScenes.first(where: {
            $0.activationState == .foregroundActive && $0 is UIWindowScene
        }) as? UIWindowScene else { return nil }

        // Get the key window from the scene
        return windowScene.windows.first(where: { $0.isKeyWindow })?.rootViewController
    }
}

struct BannerAdView_Previews: PreviewProvider {
    static var previews: some View {
        BannerAdView(adUnitID: Constants.adMobBannerTestID) // Using constant
            .frame(width: GADAdSizeBanner.size.width, height: GADAdSizeBanner.size.height)
            .previewLayout(.sizeThatFits)
    }
}
