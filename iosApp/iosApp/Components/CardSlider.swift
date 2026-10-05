import SwiftUI

/// Cards side by side, one at a time, with the next peeking in so the row reads as
/// swipeable. Shared by Following and Saved so the two sliders cannot drift apart.
struct CardSlider<Item, ID: Hashable, Card: View>: View {
    let items: [Item]
    let id: KeyPath<Item, ID>
    @ViewBuilder let card: (Item) -> Card

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            LazyHStack(spacing: Space.cardGap) {
                ForEach(items, id: id) { item in
                    card(item)
                        // Short of the full width, so the next card shows at the edge.
                        .containerRelativeFrame(.horizontal) { width, _ in width * 0.86 }
                }
            }
            .scrollTargetLayout()
        }
        .scrollTargetBehavior(.viewAligned)
    }
}
