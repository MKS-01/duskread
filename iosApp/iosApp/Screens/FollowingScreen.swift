import ComposeApp
import SwiftUI

/// Every blog followed, one line each, under NEW (posted this week, unsaved), CAUGHT UP
/// and NO POSTS YET — the split the shared side cuts, so both phones order a long list
/// alike. Expanding a row shows its three latest without leaving the list.
struct FollowingScreen: View {
    let onOpenTopics: (Feed) -> Void

    @Environment(FeedsStore.self) private var feeds
    @Environment(BrowserRouter.self) private var browser
    @Environment(LinksStore.self) private var links
    @Environment(BarCollapse.self) private var collapse
    @Environment(\.dusk) private var dusk

    @State private var expanded: Set<String> = []
    @State private var managing = false
    @State private var searching = false
    @State private var query = ""
    @State private var draft = ""

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
                    group("New", groups.fresh)
                    group("Caught up", groups.caughtUp)
                    group("No posts yet", groups.unsynced)
                }
            }
            .padding(.horizontal, Layout.listGutter)
            .padding(.bottom, Layout.barClearance)
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

    @ViewBuilder
    private func group(_ label: String, _ rows: [DigestRow]) -> some View {
        if !rows.isEmpty {
            // Muted: these sit under Following, which keeps the section's own tint.
            EyebrowHeader(label: "\(label) · \(rows.count)", tint: dusk.onSurfaceVariant)
                .padding(.top, 18)
                .padding(.bottom, 4)
            ForEach(Array(rows.enumerated()), id: \.element.feed.id) { index, row in
                feedRow(row, last: index == rows.count - 1)
            }
        }
    }

    /// One blog on one line — its name, then "3 new · 4h" in grey. The accent stays for
    /// what is playing.
    private func feedRow(_ row: DigestRow, last: Bool) -> some View {
        let feed = row.feed
        // A search opens every match: the reader is looking for a post, not a blog.
        let isOpen = expanded.contains(feed.id) || !query.isEmpty
        let posts = Array(row.posts.prefix(3))

        return VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .center, spacing: 10) {
                Text(feed.label)
                    .dusk(.titleSmall)
                    .foregroundStyle(dusk.onSurface)
                    .lineLimit(1)
                Spacer(minLength: 8)
                HStack(spacing: 0) {
                    if row.newCount > 0 {
                        Text("\(row.newCount) new").dusk(.code).fontWeight(.semibold).foregroundStyle(dusk.onSurface)
                        Text(" · ").dusk(.code).foregroundStyle(dusk.onSurfaceVariant)
                    }
                    Text(row.lastPostAt.map { feeds.shortAgo($0.int64Value) } ?? "—")
                        .dusk(.code)
                        .foregroundStyle(dusk.onSurfaceVariant)
                }
                DuskIcon(path: IconPaths.shared.Chevron, size: 16, tint: dusk.onSurfaceVariant)
                    .rotationEffect(.degrees(isOpen ? 90 : 0))
            }
            .padding(.vertical, 12)
            .contentShape(Rectangle())
            .onTapGesture {
                withAnimation(Motion.ease(Motion.chip)) {
                    if expanded.contains(feed.id) { expanded.remove(feed.id) } else { expanded.insert(feed.id) }
                }
            }

            if isOpen {
                VStack(alignment: .leading, spacing: 0) {
                    if posts.isEmpty {
                        Text("Nothing synced from this blog yet.")
                            .dusk(.bodyMedium)
                            .foregroundStyle(dusk.onSurfaceVariant)
                    } else {
                        ForEach(posts, id: \.url) { post in
                            postRow(post, host: feed.host)
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
            }

            ListRowDivider(last: last)
        }
    }

    private func postRow(_ post: FeedPost, host: String) -> some View {
        ListRow(
            host: host,
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
                        host: feed.host,
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
