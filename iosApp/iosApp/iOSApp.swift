import SwiftUI

@main
struct iOSApp: App {
    /// Built here so the Kotlin graph is created once, before any view asks
    /// for it, and torn down with the app rather than with a screen.
    @StateObject private var host = DuskReadHost()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(host)
                .ignoresSafeArea(.all)
        }
    }
}
