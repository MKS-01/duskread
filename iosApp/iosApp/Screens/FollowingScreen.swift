import ComposeApp
import SwiftUI

/// Every blog followed, as one cloud of names in the order the shared side cuts — new,
/// caught up, not yet synced — so both phones order a long list alike. A search turns it
/// into a list of the posts that match.
struct FollowingScreen: View {
    let onOpenTopics: (Feed) -> Void

    @Environment(FeedsStore.self) private var feeds
    @Environment(BrowserRouter.self) private var browser
    @Environment(LinksStore.self) private var links
    @Environment(BarCollapse.self) private var collapse
    @Environment(\.dusk) private var dusk

    @State private var managing = false
    @State private var searching = false
    @State private var query = ""
    @State private var draft = ""
    // What the slider's cards need to reach the bar: measured in the stack's own space, so
    // scrolling never resizes them.
    @State private var viewportHeight: CGFloat = 0
    @State private var picksTop: CGFloat = 0
    @State private var picksHead: CGFloat = 0
    @State private var naturalCard: CGFloat = 0

    var body: some View {
        // Read through the bridge, which Observation cannot see into; touching the stores'
        // own copies is what re-renders this when a sync or a save lands.
        let _ = (feeds.feeds, feeds.postsByFeed, links.links)
        let groups = feeds.groups(query: query)

        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                controls.padding(.bottom, 18)

                if groups.isEmpty && !managing {
                    EmptyState(
                        title: feeds.feeds.isEmpty ? "No blogs followed" : "No matches",
                        message: feeds.feeds.isEmpty
                            ? "Paste a blog's address and DuskRead will find its feed."
                            : "Nothing here matches that."
                    )
                    .padding(.top, 40)
                } else if !managing {
                    following(groups.fresh + groups.caughtUp + groups.unsynced)
                    picks(groups.picks)
                }
            }
            .coordinateSpace(.named(Self.stackSpace))
            .padding(.horizontal, Layout.listGutter)
            .padding(.bottom, Layout.barClearance)
        }
        .onScrollGeometryChange(for: CGFloat.self) { geo in
            geo.containerSize.height - geo.contentInsets.top - geo.contentInsets.bottom
        } action: { _, height in
            viewportHeight = height
        }
        .tracksBarCollapse(collapse)
        .background(dusk.background)
        .refreshable { Task { await feeds.startSync() } }
    }

    /// A long list is exactly when search is wanted; never over Manage, where the one field
    /// that matters is the address.
    private var searchShown: Bool {
        !managing && (searching || feeds.feeds.count >= Int(FollowingGroupsKt.SearchAlwaysShownAt))
    }

    private var controls: some View {
        VStack(alignment: .leading, spacing: 14) {
            EyebrowHeader(
                label: feeds.feeds.isEmpty ? "Following" : "Following · \(feeds.feeds.count)",
                progress: feeds.syncProgress
            ) {
                HStack(spacing: 16) {
                    if feeds.feeds.count < Int(FollowingGroupsKt.SearchAlwaysShownAt) && !managing {
                        HeaderAction(label: searching ? "Done" : "Search") {
                            withAnimation(Motion.ease(Motion.chip)) {
                                searching.toggle()
                                if !searching { query = "" }
                            }
                        }
                    }
                    HeaderAction(label: feeds.syncing ? feeds.sync.label : "Sync now") {
                        Task { await feeds.startSync() }
                    }
                    HeaderAction(label: managing ? "Done" : "Manage") {
                        withAnimation(Motion.ease(Motion.chip)) {
                            managing.toggle()
                            if !managing { draft = "" }
                        }
                    }
                }
            }

            if searchShown {
                AppTextField(
                    placeholder: feeds.feeds.count >= Int(FollowingGroupsKt.SearchAlwaysShownAt)
                        ? "Search \(feeds.feeds.count) blogs, or their posts"
                        : "Search by blog, host or topic",
                    text: $query
                )
            }

            if managing {
                AppTextField(placeholder: "Paste a blog address", text: $draft, mono: true) {
                    let url = draft
                    draft = ""
                    Task { await feeds.follow(url) }
                }
                ForEach(feeds.feeds, id: \.id) { feed in
                    HStack {
                        Text(feed.url.replacingOccurrences(of: "https://", with: ""))
                            .dusk(.code)
                            .foregroundStyle(dusk.onSurface)
                            .lineLimit(1)
                        Spacer(minLength: 10)
                        HeaderAction(label: "Unfollow") { feeds.remove(feed) }
                    }
                }
            }
        }
    }

    /// One cloud of names, busiest first and largest, or a list of matching posts while
    /// searching. No group headers: size and brightness carry the split.
    @ViewBuilder
    private func following(_ rows: [DigestRow]) -> some View {
        if query.isEmpty {
            CloudLayout {
                ForEach(rows, id: \.feed.id) { row in cloudName(row) }
            }
            .padding(.top, 6)
        } else {
            ForEach(Array(rows.enumerated()), id: \.element.feed.id) { index, row in
                feedRow(row, last: index == rows.count - 1)
            }
        }
    }

    /// A few recent posts from different blogs, side by side, so the screen under a short
    /// cloud has something to read without turning the tab into a second feed.
    @ViewBuilder
    private func picks(_ items: [PostPick]) -> some View {
        if query.isEmpty && !items.isEmpty {
            VStack(alignment: .leading, spacing: 0) {
                EyebrowHeader(label: "From your blogs", tint: dusk.onSurfaceVariant)
                    .padding(.top, 22)
                    .padding(.bottom, 12)
                    .onGeometryChange(for: CGFloat.self) { $0.size.height } action: { picksHead = $0 }
                pickSlider(items)
            }
            .onGeometryChange(for: CGFloat.self) { $0.frame(in: .named(Self.stackSpace)).minY } action: { picksTop = $0 }
        }
    }

    /// Down to the bar when there is room for more than the card's own height; never less.
    private var pickCardHeight: CGFloat? {
        let room = viewportHeight - Layout.barClearance - picksTop - picksHead
        return naturalCard > 0 && room > naturalCard ? room : nil
    }

    private func pickSlider(_ items: [PostPick]) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            LazyHStack(spacing: Space.cardGap) {
                ForEach(items, id: \.post.url) { pick in
                    ArticleCard(
                        host: pick.feed.shortLabel,
                        title: pick.post.title,
                        text: pick.excerpt ?? "",
                        timeAgo: pick.post.publishedAt.map { links.savedAgo($0.int64Value) },
                        meta: [RowMetaItem(text: "\(pick.minutes) min")]
                            + (pick.feed.topic.map { [RowMetaItem(text: $0.lowercased())] } ?? []),
                        fixedHeight: pickCardHeight,
                        onTap: { open(pick.post) }
                    )
                    // Its own height is the floor, so it is read only while unset.
                    .onGeometryChange(for: CGFloat.self) { $0.size.height } action: { height in
                        if pickCardHeight == nil { naturalCard = max(naturalCard, height) }
                    }
                    // Short of the full width, so the next card shows at the edge.
                    .containerRelativeFrame(.horizontal) { width, _ in width * 0.86 }
                }
            }
            .scrollTargetLayout()
        }
        // One card at a time, with the next peeking in so the row reads as swipeable.
        .scrollTargetBehavior(.viewAligned)
    }

    private static let stackSpace = "following-stack"

    /// One blog in the cloud: its short name sized by `cloudTier`, with "14" or "3w" set
    /// small beside it. Grey, not the accent: the accent stays for what is playing.
    private func cloudName(_ row: DigestRow) -> some View {
        let fresh = row.newCount > 0
        let tag = fresh ? "\(row.newCount)" : row.lastPostAt.map { feeds.shortAgo($0.int64Value) } ?? "—"
        let size = CGFloat(truncating: DesignTokens.shared.CloudNameSizes[Int(FollowingGroupsKt.cloudTier(row: row))])
        let name = Text(row.feed.shortLabel)
            .font(.custom(DuskType.jost(weight: fresh ? 500 : 400), size: size))
            .foregroundStyle(fresh ? dusk.onSurface : dusk.onSurface.opacity(0.55))
        let mark = Text("\u{200A}\(tag)")
            .font(.custom(DuskType.inconsolata(weight: 400), size: 10.5))
            .baselineOffset(size * 0.4)
            .foregroundStyle(dusk.onSurfaceVariant)
        return Text("\(name)\(mark)")
            .lineLimit(1)
            .fixedSize()
            // Padding inside the tap target: a name is a small thing to hit one-handed.
            .padding(.leading, 2)
            .padding(.trailing, 14)
            .padding(.vertical, 6)
            .contentShape(RoundedRectangle(cornerRadius: Radius.chip))
            .onTapGesture { onOpenTopics(row.feed) }
    }

    /// One blog as a search result: its name, then "3 new" or how long since it last
    /// posted, with the matching posts underneath. All grey — the accent stays for what is
    /// playing.
    private func feedRow(_ row: DigestRow, last: Bool) -> some View {
        let feed = row.feed
        let posts = Array(row.posts.prefix(3))
        let fresh = row.newCount > 0

        return VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .center, spacing: 12) {
                Text(feed.label)
                    .dusk(.titleSmall)
                    .foregroundStyle(dusk.onSurface)
                    .lineLimit(1)
                Spacer(minLength: 8)
                Text(fresh ? "\(row.newCount) new" : (row.lastPostAt.map { feeds.shortAgo($0.int64Value) } ?? "—"))
                    .dusk(.code)
                    .fontWeight(fresh ? .semibold : .regular)
                    .foregroundStyle(fresh ? dusk.onSurface : dusk.onSurfaceVariant)
                DuskIcon(path: IconPaths.shared.Chevron, size: 16, tint: dusk.onSurfaceVariant)
            }
            .padding(.vertical, 13)
            .contentShape(Rectangle())
            .onTapGesture { onOpenTopics(feed) }

            // Every match shows its posts: the reader is looking for a post, not a blog.
            VStack(alignment: .leading, spacing: 0) {
                if posts.isEmpty {
                    Text("Nothing synced from this blog yet.")
                        .dusk(.bodyMedium)
                        .foregroundStyle(dusk.onSurfaceVariant)
                } else {
                    ForEach(posts, id: \.url) { post in
                        postRow(post)
                    }
                    HStack(spacing: 6) {
                        Text("All \(row.posts.count) posts")
                            .dusk(.sectionLabel)
                            .foregroundStyle(dusk.onSurface)
                        DuskIcon(path: IconPaths.shared.Chevron, size: 14, tint: dusk.onSurface)
                    }
                    .contentShape(Rectangle())
                    .onTapGesture { onOpenTopics(feed) }
                    .padding(.top, 4)
                }
            }
            .padding(.bottom, 12)

            ListRowDivider(last: last)
        }
    }

    private func postRow(_ post: FeedPost) -> some View {
        ListRow(
            title: post.title,
            meta: post.offline ? [RowMetaItem(text: "offline")] : [],
            last: false,
            titleLineLimit: 2,
            onTap: { open(post) }
        ) {
            let saved = links.links.contains { $0.url == post.url }
            RowToggle(
                path: saved ? IconPaths.shared.BookmarkFilled : IconPaths.shared.Bookmark,
                tint: saved ? dusk.primary : dusk.onSurfaceVariant
            ) {
                if let existing = links.links.first(where: { $0.url == post.url }) {
                    links.remove(existing)
                } else {
                    _ = links.save(post.url)
                }
            }
        }
    }

    private func open(_ post: FeedPost) {
        browser.open(post.url)
    }
}

/// Every post from one blog.
struct TopicsScreen: View {
    let feed: Feed
    let onClose: () -> Void

    @Environment(FeedsStore.self) private var feeds
    @Environment(BrowserRouter.self) private var browser
    @Environment(LinksStore.self) private var links
    @Environment(\.dusk) private var dusk

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                HStack {
                    DuskIcon(path: IconPaths.shared.Back, size: 20, tint: dusk.onSurfaceVariant)
                        .contentShape(Rectangle())
                        .onTapGesture(perform: onClose)
                    Spacer()
                }
                .padding(.bottom, 18)

                EyebrowHeader(label: feed.label) {
                    Text("\(posts.count) posts")
                        .dusk(.sectionLabel)
                        .foregroundStyle(dusk.onSurfaceVariant)
                }
                .padding(.bottom, 16)

                ForEach(Array(posts.enumerated()), id: \.element.url) { index, post in
                    ListRow(
                        title: post.title,
                        meta: post.offline ? [RowMetaItem(text: "offline")] : [],
                        last: index == posts.count - 1,
                        onTap: { open(post) }
                    ) {
                        let saved = links.links.contains { $0.url == post.url }
                        RowToggle(
                            path: saved ? IconPaths.shared.BookmarkFilled : IconPaths.shared.Bookmark,
                            tint: saved ? dusk.primary : dusk.onSurfaceVariant
                        ) {
                            if let existing = links.links.first(where: { $0.url == post.url }) {
                                links.remove(existing)
                            } else {
                                _ = links.save(post.url)
                            }
                        }
                    }
                }
            }
            .padding(.horizontal, Layout.listGutter)
            .padding(.bottom, Layout.barClearance)
        }
        .background(dusk.background.ignoresSafeArea())
    }

    private var posts: [FeedPost] { feeds.posts(for: feed) }

    private func open(_ post: FeedPost) {
        browser.open(post.url)
    }
}

/// Wraps names like a magazine index, each line on one baseline so mixed sizes read as a
/// line of type rather than a ragged row.
private struct CloudLayout: SwiftUI.Layout {
    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let lines = lines(width: proposal.width ?? .infinity, subviews: subviews)
        return CGSize(width: proposal.width ?? lines.map(\.width).max() ?? 0, height: lines.reduce(0) { $0 + $1.height })
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var y = bounds.minY
        for line in lines(width: bounds.width, subviews: subviews) {
            var x = bounds.minX
            for index in line.indices {
                let d = subviews[index].dimensions(in: .unspecified)
                subviews[index].place(at: CGPoint(x: x, y: y + line.ascent - d[.firstTextBaseline]), proposal: .unspecified)
                x += d.width
            }
            y += line.height
        }
    }

    private struct Line { var indices: [Int] = []; var width: CGFloat = 0; var ascent: CGFloat = 0; var descent: CGFloat = 0
        var height: CGFloat { ascent + descent }
    }

    private func lines(width: CGFloat, subviews: Subviews) -> [Line] {
        var lines: [Line] = []
        var line = Line()
        for (index, view) in subviews.enumerated() {
            let d = view.dimensions(in: .unspecified)
            if !line.indices.isEmpty && line.width + d.width > width {
                lines.append(line)
                line = Line()
            }
            line.indices.append(index)
            line.width += d.width
            line.ascent = max(line.ascent, d[.firstTextBaseline])
            line.descent = max(line.descent, d.height - d[.firstTextBaseline])
        }
        if !line.indices.isEmpty { lines.append(line) }
        return lines
    }
}
