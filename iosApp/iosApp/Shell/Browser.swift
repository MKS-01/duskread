import SafariServices
import SwiftUI

/// Where a tapped link opens.
///
/// In the app, not out of it. Leaving for Safari loses the reader's place —
/// they come back to a cold launch, on whichever tab they were not on — and it
/// makes reading an article feel like abandoning the app rather than using it.
/// `SFSafariViewController` keeps Safari's session, reader mode and share
/// sheet, with a Done button back to exactly where they were.
///
/// Presented through UIKit rather than a `fullScreenCover`, for two reasons.
/// `SFSafariViewController` is a controller meant to be *presented*, and
/// wrapping it in a representable inside a cover does not reliably show at all.
/// And a link can be tapped from inside a full-screen cover already — Topics is
/// one — where a second SwiftUI presentation on the same view would conflict;
/// walking to the frontmost controller has neither problem. This is also what
/// the Compose iOS build does, so the two behave the same.
@Observable
final class BrowserRouter {
    /// Set by the root each render so the sheet matches whichever scheme is
    /// showing. The reader can change theme while an article is open.
    @ObservationIgnored var barTint: Color = .black
    @ObservationIgnored var controlTint: Color = .white

    func open(_ raw: String) {
        guard let url = URL(string: raw) else { return }

        // Non-web schemes (`mailto:`, App Store links) go to the system, which
        // Safari View Controller cannot show anyway.
        guard let scheme = url.scheme?.lowercased(), scheme == "http" || scheme == "https",
              let host = Self.frontmostController()
        else {
            UIApplication.shared.open(url)
            return
        }

        let configuration = SFSafariViewController.Configuration()
        // Reader where the page offers it, matching what the Android in-app
        // browser does with its own reader view — this app is for reading the
        // article, not the page around it.
        configuration.entersReaderIfAvailable = true

        let controller = SFSafariViewController(url: url, configuration: configuration)
        controller.preferredBarTintColor = UIColor(barTint)
        controller.preferredControlTintColor = UIColor(controlTint)
        controller.dismissButtonStyle = .done
        host.present(controller, animated: true)
    }

    /// Whatever is actually on screen, which is not necessarily the root: a
    /// cover is presented over it, and presenting on a controller that is
    /// itself covered does nothing.
    private static func frontmostController() -> UIViewController? {
        let scene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }
        var controller = scene?.windows.first { $0.isKeyWindow }?.rootViewController
        while let presented = controller?.presentedViewController {
            controller = presented
        }
        return controller
    }
}
