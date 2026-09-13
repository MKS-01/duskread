import SafariServices
import SwiftUI

/// Where a tapped link opens. In the app, not out of it.
@Observable
final class BrowserRouter {
    /// Set by the root each render so the sheet matches whichever scheme is showing. The
    /// reader can change theme while an article is open.
    @ObservationIgnored var barTint: Color = .black
    @ObservationIgnored var controlTint: Color = .white

    func open(_ raw: String) {
        guard let url = URL(string: raw) else { return }

        // Non-web schemes (`mailto:`, App Store links) go to the system, which Safari
        // View Controller cannot show anyway.
        guard let scheme = url.scheme?.lowercased(), scheme == "http" || scheme == "https",
              let host = Self.frontmostController()
        else {
            UIApplication.shared.open(url)
            return
        }

        let configuration = SFSafariViewController.Configuration()
        // Reader where the page offers it, matching what the Android in-app browser does
        // with its own reader view — this app is for reading the article.
        configuration.entersReaderIfAvailable = true

        let controller = SFSafariViewController(url: url, configuration: configuration)
        controller.preferredBarTintColor = UIColor(barTint)
        controller.preferredControlTintColor = UIColor(controlTint)
        controller.dismissButtonStyle = .done
        host.present(controller, animated: true)
    }

    /// Whatever is actually on screen, which is not necessarily the root: a cover is
    /// presented over it.
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
