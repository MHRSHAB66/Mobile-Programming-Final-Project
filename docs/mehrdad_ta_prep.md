# راهنمای دفاع پروژه — بخش مهرداد
**پخش موسیقی و دانلود آفلاین**

---

## فهرست مطالب
1. معماری پروژه (Clean Architecture)
2. پخش موسیقی — ExoPlayer و MusicService
3. پخش در پس‌زمینه — چرا Service؟
4. MiniPlayer و NowPlaying
5. Crossfade — ترکیب دو آهنگ
6. Audio Focus — رفتار هنگام تماس
7. Cache هوشمند — بدون مصرف مجدد اینترنت
8. دانلود آفلاین — WorkManager
9. پخش هوشمند — فایل لوکال اول
10. Audio Visualizer — کشیدن با Canvas
11. Sleep Timer
12. کنترل سرعت پخش
13. کتابخانه — Liked Songs و Recently Played
14. سوال‌های احتمالی TA و جواب‌ها

---

## ۱. معماری پروژه — Clean Architecture

پروژه به ۳ لایه تقسیم شده:

```
UI Layer       → Compose Screens + ViewModels
Domain Layer   → Use Cases + Repository Interfaces + Models
Data Layer     → Repository Implementations + Room + ExoPlayer
```

قانون اصلی: هر لایه فقط از لایه زیرش از طریق **interface** استفاده می‌کند.

> **Interface چیست؟** (اولین بار)
> یک قرارداد است — فقط می‌گوید "چه کاری انجام می‌شود" بدون اینکه بگوید "چطور". مثلاً `PlayerController` یک interface است که می‌گوید "باید بتوانی play و pause کنی." `PlayerControllerImpl` پیاده‌سازی واقعی آن با ExoPlayer است. UI فقط interface را می‌بیند، نه ExoPlayer را.

```
NowPlayingScreen (UI)
    → PlayerViewModel (UI)
        → PlayerController (Domain — interface)
            → PlayerControllerImpl (Data — پیاده‌سازی با ExoPlayer)
```

**چرا مهم است؟** اگر بخواهند ExoPlayer را عوض کنند، فقط `PlayerControllerImpl` تغییر می‌کند. هیچ کد UI یا Domain لازم نیست دست بخورد.

---

## ۲. پخش موسیقی — ExoPlayer و MediaSession

**ExoPlayer** کتابخانه گوگل برای پخش صدا و ویدیو است. نسبت به `MediaPlayer` قدیمی اندروید: از stream اینترنتی پشتیبانی می‌کند، صف آهنگ مدیریت می‌کند، cache داخلی دارد.

**MediaSession** یک "پل ارتباطی" بین اپ و سیستم اندروید است. از طریق آن:
- کنترل‌های notification (play/pause/next) کار می‌کنند
- هدفون بلوتوثی می‌تواند آهنگ را کنترل کند
- صفحه قفل وضعیت پخش را نشان می‌دهد

ارتباط این دو:

```
MusicService
    ├── ExoPlayer     ← صدا اینجا پخش می‌شود
    └── MediaSession  ← کنترل‌ها اینجا مدیریت می‌شوند

PlayerControllerImpl
    └── MediaController ← از طریق این به MusicService وصل می‌شود
```

---

## ۳. پخش در پس‌زمینه — چرا Service؟

**مشکل:** وقتی کاربر از اپ خارج می‌شود، اندروید ممکن است پروسه را kill کند و موسیقی قطع شود.

**راه حل:** `MediaSessionService` — نوع خاصی از Service اندروید که:
1. حتی بعد از بسته شدن اپ اجرا می‌ماند
2. یک notification دائمی نشان می‌دهد (Foreground Service)
3. اندروید نمی‌تواند آن را kill کند چون notification فعال دارد

```kotlin
class MusicService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus= */ true)
            .build()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession
}
```

`handleAudioFocus = true` یعنی ExoPlayer خودش مدیریت تمرکز صدا را انجام می‌دهد — جزئیاتش در بخش ۶ (Audio Focus).

---

## ۴. MiniPlayer و NowPlaying

### ارتباط این دو

> **StateFlow چیست؟** (اولین بار)
> یک جریان داده است که همیشه آخرین مقدار را نگه می‌دارد. هر کسی که subscribe کند فوری آخرین مقدار را می‌گیرد و بعدش هر بار که تغییر کند آپدیت می‌شود. مثل یک متغیر که وقتی عوض می‌شود همه بینندگانش خبردار می‌شوند.

هر دو صفحه از یک `PlayerViewModel` مشترک می‌خوانند:

```
PlayerViewModel
    └── StateFlow<PlaybackState>  ← شامل isPlaying، title، cover، position
          ├── MiniPlayer           ← کاور کوچک + دکمه play/pause
          └── NowPlayingScreen     ← همه کنترل‌ها
```

### SharedElement Transition — انیمیشن کاور

وقتی روی MiniPlayer tap می‌کنی، کاور آهنگ به صورت نرم از جای کوچک به وسط NowPlaying می‌رود. این با `SharedTransitionLayout` پیاده شده:

```kotlin
// هر دو کاور (MiniPlayer و NowPlaying) از همین کلید استفاده می‌کنند
const val PLAYER_COVER_KEY = "player-cover"

// Compose می‌فهمد این دو المان یکی هستند و بینشان انیمیشن می‌سازد
Modifier.sharedBounds(
    sharedContentState = rememberSharedContentState(key = PLAYER_COVER_KEY),
    animatedVisibilityScope = scope,
)
```

### Rotating Cover — چرخش کاور

```kotlin
val rotation by animateFloatAsState(
    targetValue = if (isPlaying) rotationState + 360f else rotationState,
    animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing))
)
Image(modifier = Modifier.rotate(rotation))
```

### Dynamic Color — رنگ پس‌زمینه از کاور

از `Palette API` رنگ غالب کاور استخراج می‌شود و به عنوان gradient پس‌زمینه NowPlaying استفاده می‌شود (`DominantColor.kt`).

---

## ۵. Crossfade — ترکیب دو آهنگ

۵ ثانیه قبل از تمام شدن آهنگ، صدای آهنگ بعدی شروع می‌شود و به تدریج صدای آهنگ اول کم می‌شود.

**پیاده‌سازی (`CrossfadeManager.kt`):**
- دو ExoPlayer داریم: **Main** (آهنگ فعلی) و **Secondary** (آهنگ بعدی)
- Secondary با `handleAudioFocus = false` است تا با Main در Audio Focus رقابت نکند

> **Coroutine چیست؟** (اولین بار)
> یک "کار سبک‌وزن" است که می‌تواند بدون مسدود کردن thread اصلی، منتظر بماند. `delay(100)` در یک Coroutine یعنی "۱۰۰ms صبر کن" — ولی thread آزاد می‌ماند و UI فریز نمی‌شود.

یک Coroutine هر ۱۰۰ms چک می‌کند چقدر از آهنگ مانده:

```kotlin
// وقتی ۵ ثانیه مانده، crossfade شروع می‌شود
while (remaining > 0) {
    val volume = remaining / DURATION_MS.toFloat()
    mainPlayer.volume = volume          // آهنگ اول کم می‌شود
    secondaryPlayer.volume = 1 - volume // آهنگ دوم زیاد می‌شود
    delay(100)
    remaining -= 100
}
```

یک نکته مهم — اگر تماس بیاید و Main پاز کند:

```kotlin
override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
    if (!playWhenReady && fading) abort() // crossfade فوری لغو می‌شود
}
```

بدون این خط، Secondary ادامه پخش می‌داد حتی وقتی Main پاز بود.

---

## ۶. Audio Focus — رفتار هنگام تماس

سیستم اندروید یک مکانیزم دارد به نام Audio Focus که تعیین می‌کند در هر لحظه کدام اپ صدا پخش کند.

**سه حالت مهم:**
- `GAIN` — ما می‌توانیم پخش کنیم
- `LOSS_TRANSIENT` — موقتاً focus از دست رفت (تماس ورودی) → باید pause کرد
- `LOSS` — کاملاً از دست رفت → باید stop کرد

چون در MusicService از `handleAudioFocus = true` استفاده کردیم (بخش ۳)، ExoPlayer این موارد را خودکار مدیریت می‌کند. کار اضافه‌ای که ما انجام دادیم فقط abort کردن crossfade است (بخش ۵).

---

## ۷. Cache هوشمند — بدون مصرف مجدد اینترنت

**مشکل:** هر بار که کاربر seek می‌کند یا آهنگ را replay می‌کند، ExoPlayer باید دوباره از اینترنت دانلود کند.

**راه حل:** `SimpleCache` از کتابخانه Media3 — داده‌هایی که از اینترنت می‌آیند روی دیسک ذخیره می‌شوند.

```kotlin
// PlaybackCache.kt — Singleton
object PlaybackCache {
    private var cache: SimpleCache? = null

    fun get(context: Context): SimpleCache {
        if (cache == null) {
            cache = SimpleCache(
                File(context.cacheDir, "exoplayer"),
                LeastRecentlyUsedCacheEvictor(200 * 1024 * 1024), // ۲۰۰ مگابایت حداکثر
                StandaloneDatabaseProvider(context),
            )
        }
        return cache!!
    }
}
```

> **Singleton چیست؟** (اولین بار)
> یعنی در کل اپ فقط یک instance از این شیء وجود دارد. اگر دو instance از Cache داشتیم، فایل‌های cache با هم تداخل پیدا می‌کردند و خراب می‌شدند.

**نتیجه:** seek کردن در قسمت‌هایی که قبلاً لود شده فوری است. replay آهنگ اینترنت نمی‌خورد.

---

## ۸. دانلود آفلاین — WorkManager

**چرا WorkManager و نه Coroutine ساده؟**

Coroutine (که قبلاً توضیح دادیم) وقتی اپ بسته شود متوقف می‌شود. WorkManager برای کارهایی است که باید **تضمینی** انجام شوند — حتی اگر اپ بسته شود، گوشی ریستارت شود، یا شبکه موقتاً قطع شود.

### `DownloadWorker.kt` — کار اصلی

```kotlin
class DownloadWorker(context: Context, params: WorkerParameters)
    : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val songId = inputData.getString(KEY_SONG_ID) ?: return Result.failure()
        val userId = inputData.getString(KEY_USER_ID) ?: return Result.failure()
        val url   = inputData.getString(KEY_AUDIO_URL) ?: return Result.failure()

        downloadDao.updateStatus(songId, userId, "DOWNLOADING", 0, null)

        val outFile = File(context.filesDir, "downloads/${userId}_${songId}.mp3")

        // فایل را stream کن و هر ۵٪ پیشرفت را در Room ثبت کن
        URL(url).openStream().use { input ->
            outFile.outputStream().use { output ->
                // ...
            }
        }

        downloadDao.updateStatus(songId, userId, "COMPLETED", 100, outFile.absolutePath)
        return Result.success()
    }
}
```

**چرا `userId` در نام فایل؟** هر اکانت دانلودهای جداگانه دارد. اگر اکانت A آهنگ X را دانلود کند، اکانت B نباید به فایل دسترسی داشته باشد.

### شروع دانلود از `DownloadRepositoryImpl.kt`

```kotlin
val request = OneTimeWorkRequestBuilder<DownloadWorker>()
    .setConstraints(
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // فقط با اینترنت شروع شود
            .build()
    )
    .setInputData(
        workDataOf(
            KEY_SONG_ID  to song.id,
            KEY_USER_ID  to userId,
            KEY_AUDIO_URL to song.audioUrl,
        )
    )
    .build()

workManager.enqueueUniqueWork(
    "download_${userId}_${song.id}", // نام یونیک — دوباره شروع نمی‌شود
    ExistingWorkPolicy.KEEP,
    request,
)
```

### دانلودها به userId وابسته‌اند

> **Room چیست؟** (اولین بار)
> یک wrapper روی SQLite (پایگاه داده محلی اندروید) است. به جای SQL خام از annotation (`@Entity`, `@Dao`, `@Query`) استفاده می‌کنیم. Room اطلاعات را روی گوشی ذخیره می‌کند و حتی بدون اینترنت در دسترس است.

```kotlin
// کلید ترکیبی — هر ردیف با (songId + userId) یونیک است
@Entity(tableName = "downloads", primaryKeys = ["songId", "userId"])
data class DownloadEntity(
    val songId: String,
    val userId: String,
    // ...
)

// فقط دانلودهای این کاربر
@Query("SELECT * FROM downloads WHERE userId = :userId ORDER BY addedAt DESC")
fun observeAllByRecent(userId: String): Flow<List<DownloadEntity>>
```

> **Flow در اینجا** (StateFlow را قبلاً توضیح دادیم — Flow مشابه است ولی آخرین مقدار را نگه نمی‌دارد): Room می‌تواند وقتی دیتابیس تغییر می‌کند، فوری UI را آپدیت کند. اگر دانلود تمام شود، لیست Downloads بدون هیچ کار اضافه‌ای آپدیت می‌شود.

---

## ۹. پخش هوشمند — فایل لوکال اول

قبل از هر بار پخش، `PlaySongsUseCase` چک می‌کند فایل دانلود شده روی گوشی هست یا نه:

```kotlin
val enriched = songs.map { song ->
    val localPath = downloadRepository.localPathFor(song.id)
    if (localPath != null && File(localPath).exists()) {
        song.copy(audioUrl = "file://$localPath") // از روی گوشی
    } else {
        song // از اینترنت stream
    }
}
playerController.play(enriched, startIndex)
```

ExoPlayer هر دو نوع URL (`https://` و `file://`) را پشتیبانی می‌کند — برایش فرقی نمی‌کند.

---

## ۱۰. Audio Visualizer — کشیدن با Canvas

**Canvas در Compose** ابزاری است برای کشیدن شکل‌های سفارشی (`drawRoundRect`, `drawCircle`, ...). Spec صراحتاً گفته از Lottie یا GIF استفاده نشود — چون می‌خواهند بدانی Canvas را بلدی.

**دو حالت:**

**حالت ۱ — Real FFT (با permission `RECORD_AUDIO`):**

```kotlin
visualizer = Visualizer(audioSessionId)
// FFT = Fast Fourier Transform — صدا را به فرکانس‌های مختلف تجزیه می‌کند
// هر فرکانس یک bar در visualizer می‌شود که با شدت صدا بالا و پایین می‌رود
```

**حالت ۲ — Synthetic (بدون permission):**

```kotlin
Canvas(modifier) {
    repeat(barCount) { i ->
        val height = abs(sin(phase1 + i * 0.3f)) * size.height
        drawRoundRect(...)  // هر bar یک مستطیل گرد
    }
}
```

ترکیب سه sine wave با سرعت‌های مختلف یک انیمیشن طبیعی می‌سازد.

---

## ۱۱. Sleep Timer

کاربر می‌گوید "۱۵ دقیقه دیگر پخش را متوقف کن." پیاده‌سازی در `PlayerViewModel.kt`:

```kotlin
private var sleepTimerJob: Job? = null

fun setSleepTimer(seconds: Int) {
    sleepTimerJob?.cancel()  // تایمر قبلی لغو شود
    if (seconds <= 0) return

    sleepTimerJob = viewModelScope.launch {
        delay(seconds * 1000L) // Coroutine (که قبلاً توضیح دادیم) صبر می‌کند
        player.pause()
    }
}
```

`Job` را نگه می‌داریم تا بتوانیم آن را هر وقت خواستیم با `cancel()` لغو کنیم.

---

## ۱۲. کنترل سرعت پخش

```kotlin
fun setSpeed(speed: Float) {
    player.setPlaybackParameters(PlaybackParameters(speed))
}
```

ExoPlayer الگوریتم‌هایی دارد که صدا را با سرعت‌های مختلف پخش کند بدون اینکه pitch (زیر و بمی صدا) تغییر کند. ما ۶ گزینه داریم: `0.5x, 0.75x, 1x, 1.25x, 1.5x, 2x`.

---

## ۱۳. کتابخانه — Liked Songs و Recently Played

Room (که در بخش ۸ توضیح دادیم) پنج جدول دارد. جدول‌های مربوط به بخش من:

| جدول | محتوا |
|------|-------|
| `liked_songs` | آهنگ‌های like شده |
| `recently_played` | تاریخچه پخش |
| `downloads` | دانلودها با userId |

```kotlin
// LikedSongDao — نمونه
@Dao
interface LikedSongDao {
    @Query("SELECT * FROM liked_songs ORDER BY likedAt DESC")
    fun observeAll(): Flow<List<LikedSongEntity>>  // Flow: هر بار like تغییر کند UI آپدیت می‌شود

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LikedSongEntity)

    @Query("DELETE FROM liked_songs WHERE songId = :songId")
    suspend fun delete(songId: String)
}
```

**Liked Songs و Recently Played هیچ userId ندارند.** این یک نقطه ضعف بالقوه است که در همین جلسه scope نبود — اگر TA پرسید می‌گویی "جدول downloads را scope کردیم، liked و recent در roadmap هستند."

---

## ۱۴. سوال‌های احتمالی TA

---

**پخش در پس‌زمینه چطور کار می‌کند؟**

از `MediaSessionService` استفاده می‌کنیم — یک Foreground Service که notification دائمی دارد و اندروید نمی‌تواند آن را kill کند. ExoPlayer داخل این Service اجرا می‌شود. حتی بعد از بستن اپ موسیقی ادامه می‌دهد.

---

**Audio Focus چیست و چطور پیاده کردید؟**

سیستم اندروید که مدیریت می‌کند کدام اپ در لحظه صدا پخش کند. چون ExoPlayer را با `handleAudioFocus = true` ساختیم، خودکار pause می‌کند وقتی تماس می‌آید. یک کار اضافه هم داریم: در `CrossfadeManager` وقتی پلیر اصلی pause می‌کند، `onPlayWhenReadyChanged` فوری crossfade را abort می‌کند تا پلیر ثانویه هم ساکت شود.

---

**دانلود چطور کار می‌کند؟**

از `WorkManager` استفاده می‌کنیم — برخلاف Coroutine ساده که با بستن اپ متوقف می‌شود، WorkManager تضمین می‌کند حتی بعد از ریستارت گوشی هم ادامه پیدا کند. `DownloadWorker` فایل را stream کرده و روی storage داخلی ذخیره می‌کند. هر ۵٪ پیشرفت را در Room ثبت می‌کند تا DownloadsScreen بلادرنگ progress bar نشان دهد.

---

**چطور از stream به فایل لوکال switch می‌کنید؟**

قبل از پخش، `PlaySongsUseCase` از `DownloadRepository` می‌پرسد این آهنگ `localPath` دارد یا نه. اگر فایل در آن آدرس وجود داشت، URL آهنگ را به `file://` تغییر می‌دهد. ExoPlayer هر دو نوع URL را پشتیبانی می‌کند.

---

**Cache هوشمند چطور کار می‌کند؟**

از `SimpleCache` (Media3) استفاده می‌کنیم — Singleton است چون باید دقیقاً یک instance وجود داشته باشد. وقتی ExoPlayer از اینترنت لود می‌کند، داده‌ها روی دیسک کش می‌شوند. seek به قسمت‌هایی که قبلاً لود شده فوری است.

---

**Crossfade چطور کار می‌کند؟**

دو ExoPlayer داریم. یک Coroutine هر ۱۰۰ms چک می‌کند چقدر از آهنگ مانده. وقتی ۵ ثانیه مانده، پلیر ثانویه شروع می‌کند و به تدریج volume اولی کم و دومی زیاد می‌شود. پلیر ثانویه `handleAudioFocus = false` دارد تا با اصلی تداخل نداشته باشد.

---

**چرا دانلودها به userId وابسته شدند؟**

قبلاً کلید اصلی جدول downloads فقط `songId` بود — یعنی اکانت B دانلودهای اکانت A را می‌دید. کلید را به `(songId, userId)` تغییر دادیم. حالا تمام queryهای DAO یک `userId` می‌گیرند و هر اکانت فقط دانلودهای خودش را می‌بیند.

---

**Audio Visualizer چطور کار می‌کند؟**

دو حالت دارد. اگر permission `RECORD_AUDIO` باشد از `android.media.audiofx.Visualizer` استفاده می‌کنیم که FFT صدا را در real-time می‌دهد — هر bar یک باند فرکانسی است. بدون permission، ترکیب چند sine wave با `Canvas` رسم می‌کنیم که انیمیشن طبیعی می‌سازد.

---

**PlayerViewModel چه کاری می‌کند که MusicService نمی‌کند؟**

`MusicService` فقط ExoPlayer را مدیریت می‌کند. `PlayerViewModel` پل بین Service و UI است: وضعیت پخش را به StateFlow تبدیل می‌کند که Compose می‌تواند observe کند، Sleep Timer را با Coroutines مدیریت می‌کند، سرعت پخش را تنظیم می‌کند و trigger دانلود و like را هماهنگ می‌کند.

---

**چرا از Clean Architecture استفاده کردید؟**

هر لایه مستقل است. UI فقط با `PlayerController` (interface) کار می‌کند — نمی‌داند پشت صحنه ExoPlayer است. اگر بخواهیم ExoPlayer را عوض کنیم، فقط `PlayerControllerImpl` تغییر می‌کند. کد قابل تست و قابل تغییر است.

---

*این فایل را به ChatGPT بده و از آن بخواه هر بخشی که نفهمیدی را بیشتر توضیح دهد یا سوال‌های احتمالی بیشتری بسازد.*
