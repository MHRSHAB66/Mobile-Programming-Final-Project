package com.example.project.data.remote.music

import android.util.Log
import com.example.project.domain.model.Artist
import com.example.project.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Real catalogue from the Jamendo API — free, Creative-Commons-licensed music whose audio is
 * legally streamable (unlike copyrighted commercial tracks, which is why YouTube Music / unofficial
 * extraction are NOT used: there is no official direct-audio streaming API for those).
 *
 * Enabled only when a Jamendo client id is configured (BuildConfig.JAMENDO_CLIENT_ID, supplied via
 * a `jamendoClientId` Gradle property). On any failure the repository falls back to the mock source,
 * so the app always works.
 */
class JamendoMusicDataSource(
    private val clientId: String,
) : RemoteMusicDataSource {

    @Volatile
    private var cache: List<Song>? = null

    override suspend fun getSongs(): List<Song> = cache ?: fetchTracks().also { cache = it }

    override suspend fun getArtists(): List<Artist> {
        val songs = getSongs()
        return songs
            .map { it.artistId to it.artistName }
            .distinct()
            .map { (id, name) ->
                val cover = songs.first { it.artistId == id }.coverImageUrl
                Artist(id = id, name = name, imageUrl = cover, followers = 0)
            }
    }

    private suspend fun fetchTracks(): List<Song> = withContext(Dispatchers.IO) {
        val url = "https://api.jamendo.com/v3.0/tracks/?" +
            "client_id=${URLEncoder.encode(clientId, "UTF-8")}" +
            "&format=json&limit=50&audioformat=mp32&order=popularity_total&include=musicinfo"

        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
            requestMethod = "GET"
        }
        try {
            if (connection.responseCode !in 200..299) {
                Log.w(TAG, "Jamendo returned ${connection.responseCode}; falling back to mock.")
                return@withContext emptyList()
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            parse(body)
        } catch (e: Exception) {
            Log.w(TAG, "Jamendo request failed: ${e.message}; falling back to mock.")
            emptyList()
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(body: String): List<Song> {
        val results = JSONObject(body).optJSONArray("results") ?: return emptyList()
        val out = ArrayList<Song>(results.length())
        for (i in 0 until results.length()) {
            val t = results.optJSONObject(i) ?: continue
            val audio = t.optString("audio")
            if (audio.isBlank()) continue
            val cover = t.optString("album_image").ifBlank { t.optString("image") }
            out += Song(
                id = "jamendo_${t.optString("id")}",
                title = t.optString("name").ifBlank { "Untitled" },
                artistId = "jamendo_artist_${t.optString("artist_id")}",
                artistName = t.optString("artist_name").ifBlank { "Unknown Artist" },
                album = t.optString("album_name"),
                coverImageUrl = cover,
                audioUrl = audio,
                durationMs = t.optLong("duration") * 1000L,
                genre = "Creative Commons",
            )
        }
        return out
    }

    private companion object {
        const val TAG = "JamendoDataSource"
    }
}
