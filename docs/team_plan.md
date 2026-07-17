# Melodify Team Plan

**Course:** Mobile Programming Final Project — AUT, Spring 1405
**Repo:** https://github.com/MHRSHAB66/Mobile-Programming-Final-Project.git

---

## 0. TL;DR

- The project already **builds and produces a working APK**. We are not rebuilding from zero.
- A **baseline commit and tag** (`baseline-working-apk`) exist on `main`. This is our safe starting point.
- Each person works on **their own branch** only — never directly on `main`.
- Each person owns a clear area: Mehrdad → playback/downloads, Hadi → data/auth/chat, Mahyar → UI/navigation/theme.
- We will preserve the baseline and improve the project incrementally through small, tested commits.
- **Week 1** is mostly reading, tracing flows, and testing existing features.
- **Week 2** is small commits — polish, improvements, and optional new features done carefully.
- **Week 3** is merge, final APK, demo video, and Q&A prep.

---

## 1. Current Status

| Item | State |
|---|---|
| Build | ✅ `BUILD SUCCESSFUL` verified |
| Baseline commit | ✅ `baseline: working APK before team split` on `main` |
| Baseline tag | ✅ `baseline-working-apk` pushed to GitHub |
| GitHub remote | ✅ configured and pushed |
| Branches | ✅ `main`, `mehrdad/playback-download`, `hadi/data-auth-chat`, `mahyar/ui-navigation-theme` |
| Spec gap check | ✅ done — see Section 4 |

**The `main` branch is our safe baseline. All work happens on personal branches. Changes go into `main` via pull request in Week 3.**

---

## 2. What Each Person Owns

---

### Mehrdad — `mehrdad/playback-download`

**Main responsibility:**
Everything related to playing music and downloading it. This includes the ExoPlayer service running in the background, the MiniPlayer and NowPlaying UI, download management via WorkManager, and the library screens (Liked Songs, Recently Played).

**Key folders/files:**
- `data/player/` — `MusicService`, `PlayerControllerImpl`, `PlaybackCache`
- `data/download/DownloadWorker.kt`
- `data/repository/DownloadRepositoryImpl.kt`, `LibraryRepositoryImpl.kt`
- `domain/player/PlayerController.kt`
- `domain/usecase/PlaySongsUseCase.kt`, `DownloadSongUseCase.kt`, `ToggleLikeUseCase.kt`
- `domain/model/PlaybackState.kt`, `Download.kt`
- `ui/player/PlayerViewModel.kt`
- `ui/nowplaying/` — `NowPlayingScreen`, `AudioVisualizer`, `DominantColor`
- `ui/downloads/` — `DownloadsScreen`, `DownloadsViewModel`
- `ui/library/` — `LikedSongsScreen`, `RecentlyPlayedScreen`, `LibraryViewModels`, `LibraryHeader`
- `ui/components/MiniPlayer.kt`

**First things to test in Week 1:**
- [ ] Play a song → minimize → confirm music keeps playing in background
- [ ] Tap MiniPlayer → NowPlaying opens → rotating cover, gradient, visualizer visible
- [ ] Test play/pause, next/previous, seek, speed (1x→1.5x→2x), sleep timer
- [ ] Upgrade to Premium → download a song → confirm progress in Downloads tab
- [ ] Play the downloaded song → confirm it plays from local file (no streaming)

**What Mehrdad should understand for TA:**
- How `MusicService` runs ExoPlayer as a background service and generates the system notification
- What `PlaybackCache` (`SimpleCache`) is and why it must be a singleton
- How `PlayerControllerImpl` connects via `MediaController` and handles stream fallback URLs
- How `PlaySongsUseCase` checks for a local file before using the streaming URL
- What `PlayerViewModel` does that `MusicService` does not (sleep timer, speed, like, download triggers)

---

### Hadi — `hadi/data-auth-chat`

**Main responsibility:**
Everything behind the scenes: the Room database, DataStore settings, mock music catalog, optional Jamendo API, repository implementations, chat simulation via FakeChatSocket, auth/session flow, and all Koin DI modules. Hadi also owns the data/repository/usecase side of search — while `SearchScreen` and `SearchViewModel` UI live in Mahyar's area, the underlying `SearchRepositoryImpl` and `SearchUseCase` are Hadi's.

**Key folders/files:**
- `data/local/db/` — `AppDatabase`, `Entities`, `Daos`, `Mappers`
- `data/local/datastore/SettingsDataStore.kt`
- `data/mock/MockData.kt`
- `data/remote/music/` — `RemoteMusicDataSource`, `MockMusicDataSource`, `JamendoMusicDataSource`
- `data/remote/socket/` — `ChatSocket`, `FakeChatSocket`
- `data/paging/ListPagingSource.kt`
- `data/repository/` — `ChatRepositoryImpl`, `MusicRepositoryImpl`, `PlaylistRepositoryImpl`, `SearchRepositoryImpl`, `SettingsRepositoryImpl`, `SocialRepositoryImpl`
- `domain/repository/` — all 6 interfaces above
- `domain/model/` — `Song`, `Artist`, `User`, `ChatModels`, `HomeFeed`, `Settings`, `SearchFilter`, `Playlist`
- `domain/usecase/` — `GetHomeFeedUseCase`, `SearchUseCase`, `UpgradeToPremiumUseCase`
- `di/` — `DataModule`, `DomainModule`, `PresentationModule`, `Modules`
- `ui/auth/` — `AuthScreen`, `AuthViewModel`
- `ui/chat/` — `ChatListScreen`, `ChatListViewModel`, `ChatDetailScreen`, `ChatDetailViewModel`

**First things to test in Week 1:**
- [ ] Read Room entities and DAOs — know all 5 tables (`liked_songs`, `recently_played`, `downloads`, `search_history`, `chat_messages`)
- [ ] Trace auth: login → DataStore write → `MainActivity` switches to `MainScreen` → logout → back to `AuthScreen`
- [ ] Trace search: user types → debounce → `SearchUseCase` → Room history saved → results shown
- [ ] Trace chat: send message → `FakeChatSocket` → `ChatRepositoryImpl` → Room → Paging 3 → UI updates
- [ ] Read all 4 DI modules — understand what is a singleton vs factory

**What Hadi should understand for TA:**
- What all 5 Room tables store and why
- How `RemoteMusicDataSource` is an interface — Koin picks `MockMusicDataSource` or `JamendoMusicDataSource` at startup based on `BuildConfig.JAMENDO_CLIENT_ID`
- How `FakeChatSocket` simulates real-time push with `SharedFlow` (not polling)
- How auth is simulated: one atomic DataStore write on login, one atomic write on logout
- How Koin injects parameterised ViewModels (e.g. `PlaylistDetailViewModel(playlistId)`)

---

### Mahyar — `mahyar/ui-navigation-theme`

**Main responsibility:**
All Compose screens (except auth, chat, nowplaying, downloads, library), the design system (theme/colors/typography/spacing), navigation, localization (FA/EN, RTL/LTR), shared UI components, and app entry points. Mahyar owns the `SearchScreen` and `SearchViewModel` UI; the search data layer belongs to Hadi.

**Key folders/files:**
- `MainActivity.kt`, `MelodyApp.kt`
- `MainScreen.kt`, `MainViewModel.kt`
- `ui/theme/` — `Color`, `Type`, `Shape`, `Dimens`, `Theme`
- `ui/navigation/` — `AppNavHost`, `BottomBar`, `Destinations`, `TopBarActions`
- `ui/components/` — `AppTopBar`, `ContentCards`, `CoverImage`, `DetailTopBar`, `LikeButton`, `Modifiers`, `SectionHeader`, `Shimmer`, `SongRow`, `StateViews`, `AnimatedGradient`
- `ui/home/`, `ui/search/`, `ui/playlists/`, `ui/profile/`, `ui/userprofile/`, `ui/artist/`
- `ui/settings/`, `ui/followed/`, `ui/notifications/`
- `core/locale/LocaleManager.kt`, `core/util/`
- `res/values/strings.xml`, `res/values-fa/strings.xml`, `res/values/colors.xml`, `res/drawable/`

**First things to test in Week 1:**
- [ ] Walk through every screen — Home, Search, Playlists, Profile, Settings, NowPlaying, Chat, Notifications
- [ ] Switch language FA → EN → confirm RTL/LTR and all strings change
- [ ] Switch Dark → Light → System theme — confirm all screens look correct
- [ ] Trigger shimmer loading on Home (works on cold launch before data loads)
- [ ] Check empty states: search with no results, Downloads tab if empty

**What Mahyar should understand for TA:**
- How `Dimens.kt` + `LocalDimens.current` centralizes the design system (hardcoded values are minimized)
- How `LocaleManager.applyLocale()` + `recreate()` switches RTL/LTR at runtime
- How `AppNavHost` handles route arguments (e.g. `"artist/{artistId}"` → `SavedStateHandle`)
- How `Shimmer.kt` works: `rememberInfiniteTransition` animates a gradient offset across placeholder shapes
- How the Home `HorizontalPager` carousel works with page-dot indicators

---

## 3. Git Workflow

### One-time setup
```bash
git clone https://github.com/MHRSHAB66/Mobile-Programming-Final-Project.git
cd Mobile-Programming-Final-Project
```

### Checkout your branch
```bash
# Mehrdad:
git checkout mehrdad/playback-download

# Hadi:
git checkout hadi/data-auth-chat

# Mahyar:
git checkout mahyar/ui-navigation-theme
```

### Before starting work each day
```bash
git pull origin <your-branch-name>
```

### Build to verify everything works
```powershell
# Windows:
.\gradlew.bat :app:assembleDebug
```
```bash
# macOS/Linux:
./gradlew :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL`

### Commit and push your work
```bash
git add <file1> <file2>
git commit -m "feat(area): short description"
git push origin <your-branch-name>
```

**Commit message format:**
- `feat(player): add shuffle toggle` — new feature
- `fix(chat): improve auto-reply timing` — bug fix or polish
- `docs(player): annotate fallback URL logic` — comments only

### Rules
- ❌ **Never push directly to `main`**
- ✅ Build must pass before every commit
- ✅ Keep commits small — one change per commit
- ✅ If your task touches someone else's file, coordinate first

---

## 4. What We Verified from the Spec

### Implemented ✅

| Feature | Notes |
|---|---|
| Home carousel | `HorizontalPager` with swipe + page dots |
| Home quick actions | 4 buttons (Liked, Recent, Playlists, Top Artists) |
| Home LazyRow sections | Popular, New, Global Playlists, Local Playlists, Artists |
| Home shimmer | `HomeShimmer()` composable |
| Playlists LazyVerticalGrid | `GridCells.Fixed(2)` — 2-column grid with 3 sections |
| Profile avatar change | Camera button overlay → `changeAvatar()` |
| Premium badge + upgrade flow | `PremiumBadge` + `UpgradeButton` with loading state |
| Chat (FakeChatSocket) | Flow-based, not polling — typing indicator, read receipts |
| Downloads | WorkManager, progress, swipe-to-dismiss, local playback |
| Theme (Dark/Light/System) | Material3 with two full color palettes |
| Localization (FA/EN + RTL/LTR) | `LocaleManager` + `recreate()` |
| Notifications screen | Polished empty state with icon and string |

### Gaps / Risks ⚠️

| Item | Status | Notes |
|---|---|---|
| SharedElement transition (MiniPlayer → NowPlaying) | Not implemented | Do not demo or mention until implemented and verified. See backlog below. |
| Carousel auto-scroll | Missing — pager is manual swipe only | The swipeable pager satisfies the spec. Auto-scroll is optional. |
| Notifications content | Only a polished empty state | Safe to show briefly. Can be improved — see backlog below. |
| Backend | Mock/local only | Partial but defensible — see Section 5. |

### Optional Improvements / Backlog

These features are not removed from the project. They are improvements that can be implemented on a personal branch if the team decides they are useful and they can be properly tested before the demo.

- **SharedElement transition** — animating the cover image from MiniPlayer to NowPlaying on expand
- **Carousel auto-scroll** — auto-advancing the `HorizontalPager` every few seconds
- **Richer Notifications screen** — real or placeholder notification items
- **Real backend integration** — Firebase, Supabase, Node.js, or Ktor behind the existing `RemoteMusicDataSource` interface
- **Real WebSocket** — replacing `FakeChatSocket` with an OkHttp WebSocket implementation behind the existing `ChatSocket` interface
- **Jamendo API demo setup** — configuring `jamendoClientId` in `gradle.properties` for the live demo

If any of these are implemented: test thoroughly, build successfully on your branch, and coordinate with the group before merging.

---

## 5. Backend Explanation for TA

**The current backend/data implementation is partial but defensible.**

| Area | Implementation |
|---|---|
| Music catalog | `MockMusicDataSource` (50 songs in-memory) or `JamendoMusicDataSource` (Creative Commons API, if configured) |
| Audio streaming | Real internet URLs — SoundHelix royalty-free samples |
| Auth / login | Simulated locally with DataStore — no server |
| Chat | `FakeChatSocket` — Flow/Channel simulation, not polling |
| Downloads | WorkManager writing real audio files to internal storage |
| User data | Room + DataStore — fully local |

A real backend can be added by implementing the existing `RemoteMusicDataSource` and `ChatSocket` interfaces and changing the DI bindings in `DataModule` — no ViewModels or UI code would need to change. If the team decides to add Firebase, Supabase, Node.js, Ktor, or a real WebSocket before the deadline, the architecture is ready for it.

### What to say if TA asks about the backend

> "We designed the data layer behind interfaces — `RemoteMusicDataSource` for the music catalog and `ChatSocket` for real-time messaging. The current implementation uses mock/local sources and a fake socket for demo stability, but the architecture is ready for a real backend. Replacing the mock source with Firebase or a real WebSocket only requires writing a new implementation of those two interfaces and updating the DI binding in `DataModule` — no ViewModels or UI code change."

### If TA asks about chat specifically

> "The spec says polling is not acceptable. We do not use polling. Messages arrive via `SharedFlow` that the repository collects — identical consumption pattern to a real WebSocket. Only the transport layer is simulated."

---

## 6. Week 1 Plan — Read, Build, Understand

Goal: understand your area deeply before making changes.

### Mehrdad — Week 1 Checklist
- [ ] Read `MusicService`, `PlayerControllerImpl`, `PlaybackCache`
- [ ] Read `PlayerViewModel`, `NowPlayingScreen`, `AudioVisualizer`
- [ ] Read `DownloadWorker`, `DownloadSongUseCase`, `PlaySongsUseCase`
- [ ] Trace: tap song → `PlaySongsUseCase` → `PlayerController.play()` → ExoPlayer
- [ ] Test all NowPlaying controls: play/pause, next/prev, seek, speed, sleep timer
- [ ] Test download → local playback flow

### Hadi — Week 1 Checklist
- [ ] Read `AppDatabase`, `Entities`, `Daos`, `Mappers` — know all 5 tables
- [ ] Read `SettingsDataStore` — know every DataStore key
- [ ] Read `MockData`, `MockMusicDataSource`, `JamendoMusicDataSource`
- [ ] Trace: auth login → DataStore → `MainActivity` routing
- [ ] Trace: chat send → `FakeChatSocket` → `ChatRepositoryImpl` → Room → Paging 3 → UI
- [ ] Read all 4 Koin DI modules

### Mahyar — Week 1 Checklist
- [ ] Read `Color.kt`, `Type.kt`, `Dimens.kt`, `Shape.kt`, `Theme.kt`
- [ ] Read `AppNavHost`, `BottomBar`, `Destinations`
- [ ] Walk through every screen in the running app
- [ ] Test: FA/EN switch → RTL/LTR changes → strings change
- [ ] Test: Dark/Light/System theme switch
- [ ] Check all empty states (Search, Downloads, Liked Songs if empty)

---

## 7. Week 2 Plan — Small Safe Commits

**Guidelines:** build must pass before and after every task. One task = one commit. Make changes carefully through your own branch. Improvements to missing features (from the backlog) are welcome if implemented and tested properly.

---

### Mehrdad — Week 2 Tasks

| # | Task | Files | Commit message |
|---|---|---|---|
| M-1 | Add shuffle toggle to NowPlaying (ExoPlayer already supports it — UI only) | `PlayerViewModel`, `NowPlayingScreen` | `feat(player): add shuffle toggle to now playing` |
| M-2 | Show download progress percentage in DownloadsScreen | `DownloadsScreen`, `DownloadsViewModel` | `feat(downloads): show real-time download progress bar` |
| M-3 | Add Play All / Shuffle All to Liked Songs header | `LikedSongsScreen`, `LibraryViewModels`, `LibraryHeader` | `feat(library): add play all and shuffle all to liked songs` |
| M-4 | Add repeat-one toggle (ExoPlayer native — UI only) | `PlayerViewModel`, `NowPlayingScreen` | `feat(player): add repeat-one toggle` |
| M-5 | Annotate fallback URL logic in `PlayerControllerImpl` for Q&A clarity | `PlayerControllerImpl` | `docs(player): annotate fallback URL and queue handling` |

---

### Hadi — Week 2 Tasks

| # | Task | Files | Commit message |
|---|---|---|---|
| H-1 | Add/verify unread badge count on ChatListScreen | `ChatListViewModel`, `ChatRepositoryImpl` | `feat(chat): show unread badge on conversations` |
| H-2 | Polish FakeChatSocket auto-reply timing (vary delay 1–3s) | `FakeChatSocket` | `fix(chat): vary auto-reply timing for realistic feel` |
| H-3 | Add/verify "clear all search history" button *(if UI changes needed in `SearchScreen`, coordinate with Mahyar)* | `SearchRepositoryImpl`, optionally `SearchScreen` | `feat(search): add clear all history button` |
| H-4 | Annotate `FakeChatSocket` and `ChatRepositoryImpl` for Q&A clarity | `FakeChatSocket`, `ChatRepositoryImpl` | `docs(chat): annotate fake socket event flow` |
| H-5 | Add comment clarifying mock source vs Jamendo path | `MockMusicDataSource` | `docs(data): clarify mock source and Jamendo fallback` |

---

### Mahyar — Week 2 Tasks

| # | Task | Files | Commit message |
|---|---|---|---|
| U-1 | Add placeholder notification items to Notifications screen | `NotificationsScreen` | `feat(notifications): add placeholder notification items` |
| U-2 | Improve empty state on Followed screen | `FollowedScreen` | `feat(social): improve empty state for no followed artists` |
| U-3 | Add font size live preview in Settings | `SettingsScreen` | `feat(settings): add live font size preview` |
| U-4 | Small Home hero banner polish (test in both themes and both languages) | `HomeScreen` | `feat(home): polish animated hero banner` |
| U-5 | Add Play All / Shuffle All header to RecentlyPlayed *(coordinate with Mehrdad first — `RecentlyPlayedScreen` is in his library area)* | `RecentlyPlayedScreen` (shared) | `feat(library): add play all to recently played header` |

**SharedElement transition** is in the backlog (Section 4). It is not forbidden — if someone wants to implement it on their branch and test it, great. Just do not demo it unless it is verified and working.

---

## 8. Week 3 Plan — Merge, Demo, Q&A

- [ ] Each person: open a Pull Request from their branch → `main` on GitHub
- [ ] Merge all three branches (resolve conflicts carefully)
- [ ] Build final APK from `main`:
  ```powershell
  git checkout main
  git pull origin main
  .\gradlew.bat :app:assembleDebug
  ```
- [ ] Test full demo flow (Section 9) on device or emulator
- [ ] Each person writes their section of `README.md`
- [ ] Record demo video following Section 9
- [ ] Each person reviews their Q&A sheet (Section 10)

---

## 9. Demo Script

Run on a device or emulator with internet. Keep Wi-Fi on during recording.

1. **Login** — cold start → Auth screen → enter name + handle → tap Login
2. **Home** — shimmer loads → carousel appears → scroll through sections
3. **Play song** — tap any song on Home → MiniPlayer appears at bottom
4. **MiniPlayer → NowPlaying** — tap MiniPlayer → full screen opens (rotating cover, gradient, visualizer)
5. **Player controls** — play/pause, next, prev, seek slider, speed (1x → 1.5x → 2x), sleep timer (set to 1 min)
6. **Search** — type a query → pause to show debounce → results appear
7. **Like** — like a song → go to Library → Liked Songs → confirm it appears
8. **Playlists** — Playlists tab → 2-column grid visible → open a playlist → Play All
9. **Premium upgrade** — Profile tab → tap Upgrade → badge changes to Premium
10. **Download** — tap download on a song → go to Downloads tab → show progress → wait for completion
11. **Local playback** — play the downloaded song → mention it plays from local file, not streaming
12. **Chat** — Chat List → open conversation → send message → typing indicator → auto-reply arrives
13. **Language switch** — Settings → switch to Persian → RTL layout, Farsi strings visible
14. **Theme switch** — switch Dark → Light → confirm both look correct

**Important:** Only demo and claim features that have been implemented and verified. If a feature from the backlog (SharedElement, auto-scroll, real notifications, etc.) is added and tested before the demo, it can be shown. If not, skip it without mentioning it.

**If streaming fails during recording:** download a song on Wi-Fi beforehand and demo local offline playback instead.

---

## 10. Q&A Cheat Sheet

### Mehrdad

**Q: How does background playback work?**
`MusicService` extends `MediaSessionService`. ExoPlayer runs inside it. Even when the app is minimised, the service keeps playing and shows a system notification with play/pause/next controls.

**Q: How does download → local playback work?**
`DownloadWorker` streams the audio URL and saves it to internal storage, then updates Room with the local path. Next time `PlaySongsUseCase` runs for that song, it reads the local path from Room and passes a `file://` URI to ExoPlayer instead of the stream URL.

**Q: What does PlayerViewModel do that MusicService doesn't?**
`MusicService` manages ExoPlayer. `PlayerViewModel` is the bridge to the UI: it observes `PlayerController.playbackState` as a `StateFlow`, runs the sleep timer as a Coroutines job, handles speed changes, triggers downloads, and coordinates likes.

---

### Hadi

**Q: How does the chat work without a real server? Is it polling?**
Not polling. `FakeChatSocket` uses a `SharedFlow` to push events and schedules delayed auto-replies. `ChatRepositoryImpl` collects these and writes to Room. The UI observes via Paging 3 — new inserts appear automatically. Same consumption pattern as a real WebSocket.

**Q: What are the 5 Room tables?**
`liked_songs`, `recently_played`, `downloads`, `search_history`, `chat_messages`. All defined in `Entities.kt`, accessed via DAOs in `Daos.kt`.

**Q: How does authentication work?**
Simulated with DataStore. Login writes `isLoggedIn=true`, name, handle, and avatar atomically. `MainActivity` reads this before layout. Logout writes `isLoggedIn=false` atomically — the app returns to `AuthScreen`.

---

### Mahyar

**Q: How does RTL/LTR switching work at runtime?**
`SettingsViewModel` saves the new language to DataStore. `MainActivity` reads it in `onCreate`, calls `LocaleManager.applyLocale(context, lang)`, then `recreate()`. The new instance starts with the correct locale and Compose picks up RTL automatically.

**Q: How does the design system work?**
Colors are in `Color.kt`, typography in `Type.kt`, spacing/sizing in `Dimens.kt` (accessed via `LocalDimens.current`), and shapes in `Shape.kt`. Hardcoded values are minimized; reusable sizes, colors, typography, and shapes are centralized in the design system. `Theme.kt` assembles them into `MaterialTheme`.

**Q: How does the Shimmer loading work?**
`Shimmer.kt` uses `rememberInfiniteTransition()` to animate a float that offsets a `LinearGradient` horizontally. Placeholder `ShimmerBox` composables of the same shape as real content are shown while data loads.

---

## 11. Rules

- ❌ Never push directly to `main`
- ✅ Build the app before and after every change (`.\gradlew.bat :app:assembleDebug`)
- ✅ Keep commits small — one logical change per commit
- ✅ Stay on your own branch
- ✅ Coordinate before touching a file owned by someone else
- ✅ Each person must understand their own files for the TA presentation
- ✅ Only claim and demo features after they are implemented, tested, and build successfully
- ❌ Do not accidentally rewrite the existing architecture — the three-layer Clean Architecture (Data/Domain/UI) is already correct and working

---

## Appendix — Full File Ownership Reference

<details>
<summary>Click to expand — complete file list per member</summary>

### Mehrdad — Full File List

```
data/player/MusicService.kt
data/player/PlayerControllerImpl.kt
data/player/PlaybackCache.kt
data/download/DownloadWorker.kt
data/repository/DownloadRepositoryImpl.kt
data/repository/LibraryRepositoryImpl.kt
domain/player/PlayerController.kt
domain/repository/DownloadRepository.kt
domain/repository/LibraryRepository.kt
domain/usecase/PlaySongsUseCase.kt
domain/usecase/DownloadSongUseCase.kt
domain/usecase/ToggleLikeUseCase.kt
domain/model/PlaybackState.kt
domain/model/Download.kt
ui/player/PlayerViewModel.kt
ui/nowplaying/NowPlayingScreen.kt
ui/nowplaying/AudioVisualizer.kt
ui/nowplaying/DominantColor.kt
ui/downloads/DownloadsScreen.kt
ui/downloads/DownloadsViewModel.kt
ui/library/LikedSongsScreen.kt
ui/library/RecentlyPlayedScreen.kt
ui/library/LibraryViewModels.kt
ui/library/LibraryHeader.kt
ui/components/MiniPlayer.kt
```

### Hadi — Full File List

```
data/local/db/AppDatabase.kt
data/local/db/Entities.kt
data/local/db/Daos.kt
data/local/db/Mappers.kt
data/local/datastore/SettingsDataStore.kt
data/mock/MockData.kt
data/remote/music/RemoteMusicDataSource.kt
data/remote/music/MockMusicDataSource.kt
data/remote/music/JamendoMusicDataSource.kt
data/remote/socket/ChatSocket.kt
data/remote/socket/FakeChatSocket.kt
data/paging/ListPagingSource.kt
data/repository/ChatRepositoryImpl.kt
data/repository/MusicRepositoryImpl.kt
data/repository/PlaylistRepositoryImpl.kt
data/repository/SearchRepositoryImpl.kt
data/repository/SettingsRepositoryImpl.kt
data/repository/SocialRepositoryImpl.kt
domain/repository/ChatRepository.kt
domain/repository/MusicRepository.kt
domain/repository/PlaylistRepository.kt
domain/repository/SearchRepository.kt
domain/repository/SettingsRepository.kt
domain/repository/SocialRepository.kt
domain/model/Song.kt
domain/model/Artist.kt
domain/model/User.kt
domain/model/ChatModels.kt
domain/model/HomeFeed.kt
domain/model/Settings.kt
domain/model/SearchFilter.kt
domain/model/Playlist.kt
domain/usecase/GetHomeFeedUseCase.kt
domain/usecase/SearchUseCase.kt
domain/usecase/UpgradeToPremiumUseCase.kt
di/DataModule.kt
di/DomainModule.kt
di/PresentationModule.kt
di/Modules.kt
ui/auth/AuthScreen.kt
ui/auth/AuthViewModel.kt
ui/chat/ChatListScreen.kt
ui/chat/ChatListViewModel.kt
ui/chat/ChatDetailScreen.kt
ui/chat/ChatDetailViewModel.kt
```

### Mahyar — Full File List

```
MainActivity.kt
MelodyApp.kt
MainScreen.kt
MainViewModel.kt
core/locale/LocaleManager.kt
core/util/Resource.kt
core/util/UiText.kt
core/util/Format.kt
ui/theme/Color.kt
ui/theme/Type.kt
ui/theme/Shape.kt
ui/theme/Dimens.kt
ui/theme/Theme.kt
ui/navigation/AppNavHost.kt
ui/navigation/BottomBar.kt
ui/navigation/Destinations.kt
ui/navigation/TopBarActions.kt
ui/components/AppTopBar.kt
ui/components/ContentCards.kt
ui/components/CoverImage.kt
ui/components/DetailTopBar.kt
ui/components/LikeButton.kt
ui/components/Modifiers.kt
ui/components/SectionHeader.kt
ui/components/Shimmer.kt
ui/components/SongRow.kt
ui/components/StateViews.kt
ui/components/AnimatedGradient.kt
ui/home/HomeScreen.kt
ui/home/HomeViewModel.kt
ui/search/SearchScreen.kt
ui/search/SearchViewModel.kt
ui/playlists/PlaylistsScreen.kt
ui/playlists/PlaylistsViewModel.kt
ui/playlistdetail/PlaylistDetailScreen.kt
ui/playlistdetail/PlaylistDetailViewModel.kt
ui/profile/ProfileScreen.kt
ui/profile/ProfileViewModel.kt
ui/userprofile/UserProfileScreen.kt
ui/userprofile/UserProfileViewModel.kt
ui/artist/ArtistScreen.kt
ui/artist/ArtistViewModel.kt
ui/settings/SettingsScreen.kt
ui/settings/SettingsViewModel.kt
ui/followed/FollowedScreen.kt
ui/followed/FollowedViewModel.kt
ui/notifications/NotificationsScreen.kt
res/values/strings.xml
res/values-fa/strings.xml
res/values/colors.xml
res/values/themes.xml
res/drawable/
```

</details>
