import ComposeApp
import SwiftUI

/// The big-timer mode: a full-screen destination for when the point is to
/// actually stare at the clock rather than glance at a corner.
///
/// Bottom-anchored, like every other surface in this app — it is used
/// one-handed, so the controls sit where the thumb already is.
struct FocusScreen: View {
    let onClose: () -> Void

    @Environment(PomodoroStore.self) private var pomodoro
    @Environment(\.dusk) private var dusk

    @State private var customising = false
    @State private var custom = ""

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                DuskIcon(path: IconPaths.shared.Close, size: 20, tint: dusk.onSurfaceVariant)
                    .contentShape(Rectangle())
                    .onTapGesture(perform: onClose)
                Spacer()
            }
            .padding(.top, 8)

            Spacer()

            if pomodoro.state.idle {
                idle
            } else {
                running
            }
        }
        .padding(.horizontal, Layout.readingGutter)
        .padding(.bottom, 40)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(dusk.background.ignoresSafeArea())
    }

    private var idle: some View {
        VStack(alignment: .leading, spacing: 16) {
            EyebrowHeader(label: "Focus")
            if customising {
                Text("How long?")
                    .dusk(.titleMedium)
                    .foregroundStyle(dusk.onSurface)
                AppTextField(placeholder: "Minutes", text: $custom, mono: true) { startCustom() }
                    .frame(maxWidth: 160)
                HStack(spacing: Space.chipGap) {
                    Pill(label: "Start", active: true) { startCustom() }
                    Pill(label: "Cancel", active: false) {
                        customising = false
                        custom = ""
                    }
                }
            } else {
                Text("Pick a length")
                    .dusk(.titleMedium)
                    .foregroundStyle(dusk.onSurface)
                HStack(spacing: Space.chipGap) {
                    ForEach(pomodoro.pickableMinutes, id: \.self) { minutes in
                        Pill(label: "\(minutes) min", active: false) { pomodoro.start(minutes) }
                    }
                    Pill(label: "Custom", active: false) { customising = true }
                }
            }
        }
    }

    private var running: some View {
        VStack(alignment: .leading, spacing: 20) {
            Text(pomodoro.clockLabel)
                .font(.custom("Inconsolata-Medium", size: 52))
                .foregroundStyle(dusk.onBackground)
                .monospacedDigit()

            WaveformMeter(progress: pomodoro.elapsedFraction, barCount: 28, height: 22)

            HStack(spacing: Space.chipGap) {
                Pill(label: pomodoro.state.running ? "Pause" : "Resume", active: true) {
                    if pomodoro.state.running { pomodoro.pause() } else { pomodoro.resume() }
                }
                Pill(label: "Reset", active: false) { pomodoro.reset() }
            }
        }
    }

    private func startCustom() {
        guard let minutes = Int(custom.trimmingCharacters(in: .whitespaces)), minutes > 0 else { return }
        pomodoro.start(minutes)
        customising = false
        custom = ""
    }
}
