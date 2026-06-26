package com.example.project.data.mock

import com.example.project.domain.model.Artist
import com.example.project.domain.model.Conversation
import com.example.project.domain.model.Playlist
import com.example.project.domain.model.PlaylistType
import com.example.project.domain.model.Song
import com.example.project.domain.model.User
import kotlin.math.abs

/**
 * In-memory MOCK BACKEND and fallback catalogue. There is NO real backend/API here; when a
 * Jamendo client id is configured the app instead loads real Creative-Commons tracks via
 * [com.example.project.data.remote.music.JamendoMusicDataSource]. Swapping sources only touches
 * the data-source layer — the rest of the app is unchanged.
 *
 * SONGS: titles and artists are REAL, well-known tracks (Iranian / English / world) so the
 * catalogue feels authentic. Their original recordings are copyrighted and cannot be legally
 * streamed, so PLAYBACK uses verified, stable, royalty-free sample audio ([audioPool], SoundHelix)
 * plus a reliable fallback. Cover art uses deterministic picsum.photos seeds (stable, distinct).
 */
object MockData {

    private fun cover(seed: String, size: Int = 300) =
        "https://picsum.photos/seed/$seed/$size/$size"

    // Verified, stable, royalty-free sample audio used for playback (real recordings are copyrighted).
    private val audioPool = (1..16).map {
        "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-$it.mp3"
    }

    private fun audio(i: Int) = audioPool[i % audioPool.size]

    private data class Seed(val title: String, val artistId: String, val artist: String, val genre: String)

    private val seeds = listOf(
        // --- Iranian (16) ---
        Seed("سلطان قلب‌ها", "aref", "عارف", "Classic Persian"),
        Seed("گل سنگم", "hayedeh", "هایده", "Classic Persian"),
        Seed("مرد تنها", "dariush", "داریوش", "Persian Pop"),
        Seed("بوی عیدی", "farhad", "فرهاد مهراد", "Persian Folk"),
        Seed("کجایی", "chavoshi", "محسن چاوشی", "Persian Pop"),
        Seed("دیوونه", "shadmehr", "شادمهر عقیلی", "Persian Pop"),
        Seed("ساقی", "ebi", "ابی", "Persian Pop"),
        Seed("مرغ سحر", "shajarian", "محمدرضا شجریان", "Traditional Persian"),
        Seed("جان مریم", "iraj", "ایرج", "Classic Persian"),
        Seed("کوچه", "foroughi", "فریدون فروغی", "Persian Rock"),
        Seed("شبانه", "dariush", "داریوش", "Persian Pop"),
        Seed("خونه", "farhad", "فرهاد مهراد", "Persian Folk"),
        Seed("گل بی‌گلدون", "habib", "حبیب", "Persian Pop"),
        Seed("سنگ صبور", "hayedeh", "هایده", "Classic Persian"),
        Seed("یار دبستانی", "farhad", "فرهاد مهراد", "Persian Folk"),
        Seed("نیمه گمشده", "ghomeyshi", "سیاوش قمیشی", "Persian Pop"),
        // --- English / Western (17) ---
        Seed("Bohemian Rhapsody", "queen", "Queen", "Rock"),
        Seed("Imagine", "lennon", "John Lennon", "Soft Rock"),
        Seed("Hotel California", "eagles", "Eagles", "Rock"),
        Seed("Shape of You", "edsheeran", "Ed Sheeran", "Pop"),
        Seed("Billie Jean", "mj", "Michael Jackson", "Pop"),
        Seed("Rolling in the Deep", "adele", "Adele", "Soul"),
        Seed("Smells Like Teen Spirit", "nirvana", "Nirvana", "Grunge"),
        Seed("Hey Jude", "beatles", "The Beatles", "Rock"),
        Seed("Sweet Child o' Mine", "gnr", "Guns N' Roses", "Rock"),
        Seed("Wonderwall", "oasis", "Oasis", "Britpop"),
        Seed("Yesterday", "beatles", "The Beatles", "Rock"),
        Seed("Stairway to Heaven", "ledzep", "Led Zeppelin", "Rock"),
        Seed("Thriller", "mj", "Michael Jackson", "Pop"),
        Seed("Someone Like You", "adele", "Adele", "Soul"),
        Seed("Believer", "imaginedragons", "Imagine Dragons", "Pop Rock"),
        Seed("Viva la Vida", "coldplay", "Coldplay", "Alt Rock"),
        Seed("Bad Guy", "billie", "Billie Eilish", "Pop"),
        // --- World (17) ---
        Seed("Despacito", "fonsi", "Luis Fonsi", "Reggaeton"),
        Seed("Gangnam Style", "psy", "PSY", "K-Pop"),
        Seed("La Vie en rose", "piaf", "Édith Piaf", "Chanson"),
        Seed("Waka Waka", "shakira", "Shakira", "Latin Pop"),
        Seed("Bella Ciao", "mcr", "Modena City Ramblers", "Folk"),
        Seed("Jai Ho", "rahman", "A. R. Rahman", "Soundtrack"),
        Seed("Volare", "modugno", "Domenico Modugno", "Italian"),
        Seed("Sukiyaki", "sakamoto", "Kyu Sakamoto", "J-Pop"),
        Seed("Macarena", "losdelrio", "Los del Río", "Latin"),
        Seed("Bamboleo", "gipsykings", "Gipsy Kings", "Flamenco"),
        Seed("Lambada", "kaoma", "Kaoma", "Latin"),
        Seed("99 Luftballons", "nena", "Nena", "Pop"),
        Seed("Ai Se Eu Te Pego", "telo", "Michel Teló", "Sertanejo"),
        Seed("Dragostea Din Tei", "ozone", "O-Zone", "Eurodance"),
        Seed("Hips Don't Lie", "shakira", "Shakira", "Latin Pop"),
        Seed("Gasolina", "daddyyankee", "Daddy Yankee", "Reggaeton"),
        Seed("Tum Hi Ho", "arijit", "Arijit Singh", "Bollywood"),
    )

    val songs: List<Song> = seeds.mapIndexed { i, seed ->
        Song(
            id = "s${i + 1}",
            title = seed.title,
            artistId = seed.artistId,
            artistName = seed.artist,
            album = "Single",
            coverImageUrl = cover("song${i + 1}"),
            audioUrl = audio(i),
            durationMs = 180_000L + (i % 9) * 18_000L,
            genre = seed.genre,
        )
    }

    val artists: List<Artist> = seeds
        .map { it.artistId to it.artist }
        .distinct()
        .map { (id, name) ->
            Artist(
                id = id,
                name = name,
                imageUrl = cover("artist_$id"),
                followers = 200_000 + abs(id.hashCode()) % 9_000_000,
            )
        }

    private fun songIds(vararg idx: Int) = idx.map { "s$it" }

    val playlists: List<Playlist> = listOf(
        Playlist("pl1", "Legends — Top Hits", "The songs everyone knows.",
            cover("pltop"), PlaylistType.GLOBAL, "Melodify",
            songIds(17, 21, 18, 19, 22, 34, 20, 29, 32)),
        Playlist("pl2", "Pop Anthems", "Chart-topping pop classics.",
            cover("plpop"), PlaylistType.GLOBAL, "Melodify",
            songIds(20, 21, 22, 33, 37, 34, 30, 31)),
        Playlist("pl3", "Rock Classics", "Timeless rock & roll.",
            cover("plrock"), PlaylistType.GLOBAL, "Melodify",
            songIds(17, 19, 24, 25, 26, 28, 23)),
        Playlist("pl4", "World Tour", "Global hits across languages.",
            cover("plworld"), PlaylistType.GLOBAL, "Melodify",
            songIds(34, 35, 36, 37, 42, 43, 47, 49)),
        Playlist("pl5", "بهترین‌های ایران", "محبوب‌ترین آهنگ‌های ایرانی.",
            cover("pliran"), PlaylistType.LOCAL, "Melodify",
            songIds(1, 2, 3, 4, 5, 6, 7, 8)),
        Playlist("pl6", "پاپ ایرانی امروز", "صدای امروز موسیقی ایران.",
            cover("plirpop"), PlaylistType.LOCAL, "Melodify",
            songIds(5, 6, 7, 3, 13, 16)),
        Playlist("pl7", "کلاسیک‌های ایرانی", "خاطره‌انگیزترین‌ها.",
            cover("plclassic"), PlaylistType.LOCAL, "Melodify",
            songIds(1, 2, 8, 9, 14, 11)),
        Playlist("pl8", "My Road Trip", "Songs for the open road.",
            cover("plroad"), PlaylistType.USER, "You",
            songIds(17, 20, 34, 37, 19, 25, 31)),
        Playlist("pl9", "Coding Sessions", "What I build to.",
            cover("plcode"), PlaylistType.USER, "You",
            songIds(18, 32, 36, 28, 50, 39)),
        Playlist("pl10", "Throwback Mix", "Old favourites.",
            cover("plthrow"), PlaylistType.USER, "You",
            songIds(1, 2, 21, 17, 24, 40, 42)),
    )

    val currentUser = User(
        id = "u0",
        displayName = "Leila",
        handle = "@leila",
        avatarUrl = cover("me"),
        isPremium = false,
        followers = 128,
        publicPlaylistIds = listOf("pl8", "pl9", "pl10"),
    )

    val users: List<User> = listOf(
        User("u1", "Sara Ahmadi", "@sara", cover("u1"), true, 540, isFollowed = true, isOnline = true,
            publicPlaylistIds = listOf("pl1", "pl8")),
        User("u2", "Omid R.", "@omid", cover("u2"), false, 210, isFollowed = true, isOnline = false,
            publicPlaylistIds = listOf("pl2")),
        User("u3", "Niloofar", "@niloo", cover("u3"), true, 980, isFollowed = true, isOnline = true,
            publicPlaylistIds = listOf("pl3", "pl9")),
        User("u4", "Arman K.", "@arman", cover("u4"), false, 75, isFollowed = false, isOnline = false,
            publicPlaylistIds = listOf("pl5")),
        User("u5", "Tara", "@tara", cover("u5"), true, 1340, isFollowed = true, isOnline = false,
            publicPlaylistIds = listOf("pl4", "pl10")),
        User("u6", "Reza Pour", "@reza", cover("u6"), false, 310, isFollowed = false, isOnline = true,
            publicPlaylistIds = listOf("pl7")),
        User("u7", "Mina", "@mina", cover("u7"), true, 620, isFollowed = true, isOnline = true,
            publicPlaylistIds = listOf("pl1", "pl6")),
        User("u8", "Kaveh", "@kaveh", cover("u8"), false, 145, isFollowed = false, isOnline = false,
            publicPlaylistIds = listOf("pl3")),
    )

    /** Seed conversations for the chat list (with the users we follow). */
    val seedConversations: List<Conversation> = listOf(
        Conversation("c1", users[0], "Did you hear Bohemian Rhapsody again? 🎸", nowMinus(4), 2),
        Conversation("c2", users[2], "Sharing a playlist with you 🎧", nowMinus(40), 0),
        Conversation("c3", users[4], "آهنگی که فرستادی رو گذاشتم تو ریپیت!", nowMinus(180), 0),
        Conversation("c4", users[6], "let's make a collab playlist", nowMinus(1440), 1),
    )

    private fun nowMinus(minutes: Long) = System.currentTimeMillis() - minutes * 60_000L

    fun songById(id: String): Song? = songs.firstOrNull { it.id == id }
    fun userById(id: String): User? = (users + currentUser).firstOrNull { it.id == id }
    fun playlistById(id: String): Playlist? = playlists.firstOrNull { it.id == id }
}
