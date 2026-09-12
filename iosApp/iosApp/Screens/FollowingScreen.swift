import ComposeApp
import SwiftUI

/// Every blog followed, with its host, how far behind you are, and its newest
/// post. Expanding a row shows the three latest without leaving the list.
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
    @State private var sortNewest = true

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                controls.padding(.bottom, 18)

                if visible.isEmpty {
                    EmptyState(
                        title: feeds.feeds.isEmpty ? "No blogs followed" : "No matches",
                        message: feeds.feeds.isEmpty
                            ? "Paste a blog's address and DuskRead will find its feed."
                            : "Nothing here matches that."
                    )
                    .padding(.top, 40)
                } else {
                    ForEach(Array(visible.enumerated()), id: \.element.id) { index, feed in
                        feedRow(feed, last: index == visible.count - 1)
                    }
                }
            }
            .padding(.horizontal, Layout.listGutter)
            .padding(.bottom, Layout.barClearance)
        }
        .tracksBarCollapse(collapse)
        .background(dusk.background)
        .refreshable { await feeds.sync() }
    }

    private var controls: some View {
        VStack(alignment: .leading, spacing: 14) {
            EyebrowHeader(label: "Following") {
                HStack(spacing: 16) {
                    HeaderAction(label: searching ? "Done" : "Search") {
                        withAnimation(Motion.ease(Motion.chip)) {
                            searching.toggle()
                            if !searching { query = "" }
                        }
                    }
                    HeaderAction(label: feeds.syncing ? "Syncing…" : "Sync now") {
                        Task { await feeds.sync() }
                    }
                    HeaderAction(label: managing ? "Done" : "Manage") {
                        withAnimation(Motion.ease(Motion.chip)) {
                            managing.toggle()
                            if !managing { draft = "" }
                        }
                    }
                }
            }

            HStack(spacing: Space.chipGap) {
                Pill(label: "Newest", active: sortNewest) { sortNewest = true }
                Pill(label: "A–Z", active: !sortNewest) { sortNewest = false }
            }

            if searching {
                AppTextField(placeholder: "Search blogs", text: $query)
            }

            if managing {
                AppTextField(placeholder: "Paste a blog address", text: $draft, mono: true) {
                    let url = draft
                    draft = ""
                    Task { await feeds.follow(url) }
                }
            }
        }
    }

    private func feedRow(_ feed: Feed, last: Bool) -> some View {
        let isOpen = expanded.contains(feed.id)
        let posts = Array(feeds.posts(for: feed).prefix(3))

        return VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .center, spacing: 10) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(feed.label)
                        .dusk(.titleSmall)
                        .foregroundStyle(dusk.onSurface)
                        .lineLimit(1)
                    if !isOpen, let newest = feeds.posts(for: feed).first {
                        Text(newest.title)
                            .dusk(.bodyMedium)
                            .foregroundStyle(dusk.onSurfaceVariant)
                            .lineLimit(1)
                    }
                }
                Spacer(minLength: 8)
                Text("\(feeds.newCount(for: feed, saved: links.links)) new")
                    .dusk(.code)
                    .foregroundStyle(dusk.primary)
                DuskIcon(path: IconPaths.shared.Chevron, size: 16, tint: dusk.onSurfaceVariant)
                    .rotationEffect(.degrees(isOpen ? 90 : 0))
            }
            .contentShape(Rectangle())
            .onTapGesture {
                withAnimation(Motion.ease(Motion.chip)) {
                    if isOpen { expanded.remove(feed.id) } else { expanded.insert(feed.id) }
                }
            }
            // Only offered while managing — an empty context menu still
            // swallows the long press, which makes the row feel stuck.
            .unfollowMenu(managing) { feeds.remove(feed) }

            if isOpen {
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(posts, id: \.url) { post in
                        postRow(post, host: feed.host)
                    }
                    HStack(spacing: 6) {
                        Text("All \(feeds.posts(for: feed).count) posts")
                            .dusk(.sectionLabel)
                            .foregroundStyle(dusk.primary)
                        DuskIcon(path: IconPaths.shared.Chevron, size: 14, tint: dusk.primary)
                    }
                    .contentShape(Rectangle())
                    .onTapGesture { onOpenTopics(feed) }
                    .padding(.top, 4)
                }
                .padding(.top, 16)
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

    private var visible: [Feed] {
        var pool = feeds.feeds
        if !query.isEmpty {
            pool = pool.filter { $0.label.localizedCaseInsensitiveContains(query) }
        }
        return sortNewest
            ? pool.sorted { $0.addedAt > $1.addedAt }
            : pool.sorted { $0.label.localizedCaseInsensitiveCompare($1.label) == .orderedAscending }
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

private extension View {
    @ViewBuilder
    func unfollowMenu(_ managing: Bool, remove: @escaping () -> Void) -> some View {
        if managing {
            contextMenu { Button("Unfollow", role: .destructive, action: remove) }
        } else {
            self
        }
    }
}
