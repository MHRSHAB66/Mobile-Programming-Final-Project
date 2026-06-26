# Melodify — Issue Tracker

هر عضو تیم می‌تونه مشکلی که پیدا می‌کنه رو اینجا اضافه کنه.
برای هر مشکل این اطلاعات رو بنویس:

```
## #XXX — عنوان مشکل

- **شدت:** Critical / High / Medium / Low
- **پیدا کرده:** [اسم]
- **باید حل کنه:** [اسم] — `[برنچ]`
- **فایل‌های مرتبط:** `...`
- **وضعیت:** Open

**توضیح:**
...
```

### وضعیت‌های ممکن

| وضعیت | معنی |
|---|---|
| `Open` | مشکل پیدا شده، هنوز کسی روش کار نکرده |
| `In Progress` | داره کار می‌شه روش |
| `✅ Fixed — [تاریخ] by [اسم]` | حل شده — commit یا branch رو بنویس |
| `⏭ Skipped — [دلیل]` | عمداً رد شد (مثلاً کم‌اهمیته یا وقت نداریم) |

---

## #001 — تپ روی نوتیفیکیشن اپ رو باز نمی‌کنه

- **شدت:** High
- **پیدا کرده:** Mehrdad
- **باید حل کنه:** Mehrdad — `mehrdad/playback-download`
- **فایل‌های مرتبط:** `data/player/MusicService.kt`
- **وضعیت:** Open

**توضیح:**
وقتی موسیقی در پس‌زمینه داره پخش می‌شه و از نوتیفیکیشن روش می‌زنی، اپ باز نمی‌شه.
فقط دکمه‌های play/pause/next/prev کار می‌کنن.

**ریشه مشکل:**
در Media3، برای اینکه تپ روی نوتیفیکیشن اپ رو باز کنه، باید یک `sessionActivity` با `PendingIntent` روی `MediaSession` ست بشه.
این احتمالاً در `MusicService.kt` داخل `onGetSession()` یا `onCreate()` جا داره.

**راه حل:**
```kotlin
// در MusicService.onCreate()
val intent = packageManager.getLaunchIntentForPackage(packageName)
val pendingIntent = PendingIntent.getActivity(
    this, 0, intent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
)
// پاس دادن pendingIntent به MediaSession builder
// mediaSession = MediaSession.Builder(this, player)
//     .setSessionActivity(pendingIntent)
//     .build()
```

---

## #002 — دکمه‌های Shuffle و Repeat در نوتیفیکیشن وجود ندارند

- **شدت:** Medium
- **پیدا کرده:** Mehrdad
- **باید حل کنه:** Mehrdad — `mehrdad/playback-download`
- **فایل‌های مرتبط:** `data/player/MusicService.kt`
- **وضعیت:** Open

**توضیح:**
نوتیفیکیشن سیستم فقط prev/pause/next دارد.
دکمه‌های shuffle و repeat که در `NowPlayingScreen` پیاده‌سازی می‌شن (تسک‌های M-1 و M-4 از Week 2)، در نوتیفیکیشن نیستن.

**نکته:**
Media3 به صورت پیش‌فرض فقط prev/play/next رو نمایش می‌ده.
اضافه کردن shuffle/repeat به نوتیفیکیشن نیاز به تعریف `CommandButton` سفارشی در `MusicService` داره.
این تسک رو بعد از اینکه shuffle و repeat توی اپ کار کردن انجام بده.

---

## #003 — تأخیر در seek کردن روی progress bar

- **شدت:** Medium
- **پیدا کرده:** Mehrdad
- **باید حل کنه:** Mehrdad — `mehrdad/playback-download`
- **فایل‌های مرتبط:** `ui/player/PlayerViewModel.kt`، `data/player/PlayerControllerImpl.kt`
- **وضعیت:** Open

**توضیح:**
وقتی روی نوار پیشرفت (slider) در NowPlayingScreen انگشت می‌کشی تا جلو یا عقب بری،
یه تأخیر وجود داره و فوری جابجا نمی‌شه.

**احتمالات:**
1. `PlayerViewModel` وضعیت پیشرفت رو با یه interval جمع می‌کنه — ممکنه درخواست seek فوری ارسال نشه.
2. ExoPlayer باید buffer جدید رو دانلود کنه (که برای استریم طبیعیه).
3. ممکنه `Slider` در Compose debounce داشته باشه یا `onValueChange` و `onValueChangeFinished` به درستی کنترل نشده باشن.

**راه بررسی:**
در `NowPlayingScreen`، چک کن که seek فقط در `onValueChangeFinished` فرستاده می‌شه یا در هر `onValueChange`.
برای UI smooth بهتره که slider position رو locally آپدیت کنی و seek رو فقط موقع رها کردن بفرستی.

---

## #004 — موسیقی‌ها واقعی نیستند — توضیح و راه‌حل‌ها

- **شدت:** Medium (برای دمو مهمه)
- **پیدا کرده:** Mehrdad
- **باید حل کنه:** Hadi — `hadi/data-auth-chat`
- **فایل‌های مرتبط:** `data/mock/MockData.kt`
- **وضعیت:** Open

**توضیح:**
اسم و خواننده آهنگ‌ها واقعی هستن (۵۰ آهنگ ایرانی/انگلیسی/جهانی)،
اما صدایی که پخش می‌شه از URLهای SoundHelix (نمونه‌های رایگان) میاد، نه آهنگ اصلی.
این به خاطر کپی‌رایت و نبود API استریم مستقیم یوتیوب بوده.

**موسیقی‌ها از کجا میان؟**
- پوشه محلی نیست. همه چیز از اینترنت stream می‌شه.
- URLها داخل `MockData.kt` هستن — هر آهنگ یک `audioUrl` داره که به SoundHelix اشاره می‌کنه.
- اگه آهنگی دانلود بشه، از فایل محلی پخش می‌شه (تو پوشه داخلی اپ).

**گزینه‌های موجود برای بهبود:**

**گزینه ۱ — Jamendo API (توصیه می‌شه):**
Jamendo موسیقی Creative Commons رایگان و قانونی داره.
برای فعال کردن:
1. ثبت‌نام رایگان در https://developer.jamendo.com
2. یه `client_id` بگیر
3. توی `gradle.properties` بذار:
   ```properties
   jamendoClientId=YOUR_CLIENT_ID_HERE
   ```
4. اپ به صورت خودکار از Jamendo موسیقی واقعی میاره.

**گزینه ۲ — جایگزینی URLها در MockData:**
می‌تونی `audioUrl` هر آهنگ رو در `MockData.kt` با لینک مستقیم MP3 از منابع رایگان جایگزین کنی.
منابع مناسب:
- Archive.org (موسیقی Public Domain)
- Free Music Archive (freemusicarchive.org)
- ccMixter

**گزینه ۳ — دانلود فایل MP3 و اضافه کردن به assets:**
اگه می‌خوای چند آهنگ واقعی توی اپ داشته باشی بدون نیاز به اینترنت:
1. چند فایل MP3 رایگان (Creative Commons) دانلود کن
2. تو پوشه `app/src/main/assets/` بذارشون
3. URL رو به `file:///android_asset/song_name.mp3` تغییر بده در MockData

> **نکته:** این گزینه‌ها نیاز به دسترسی و دانلود دستی دارن — Claude نمی‌تونه فایل MP3 دانلود کنه.

---

## #005 — MiniPlayer در PlaylistDetailScreen نمایش داده نمی‌شه

- **شدت:** High
- **پیدا کرده:** Mehrdad
- **باید حل کنه:** Mahyar (ناوبری/MainScreen) — `mahyar/ui-navigation-theme`، هماهنگی با Mehrdad
- **فایل‌های مرتبط:** `ui/MainScreen.kt`، `ui/navigation/AppNavHost.kt`، `ui/playlistdetail/PlaylistDetailScreen.kt`
- **وضعیت:** Open

**توضیح:**
وقتی داری داخل PlaylistDetailScreen هستی و یه آهنگ پخش می‌شه،
نوار MiniPlayer پایین صفحه نمایش داده نمی‌شه.
فقط وقتی از Playlist خارج می‌شی می‌تونی MiniPlayer رو ببینی.

**ریشه احتمالی:**
در `MainScreen.kt`، MiniPlayer احتمالاً فقط برای تب‌های اصلی (bottom nav destinations) نمایش داده می‌شه.
`PlaylistDetailScreen` یک صفحه detail هست که NavHost بدون bottom nav باز می‌کنه،
و ممکنه MiniPlayer از layout اون خارج باشه.

**راه بررسی:**
در `MainScreen.kt` چک کن که MiniPlayer کجا قرار گرفته — اگه داخل `Scaffold` و بالای `BottomBar` هست ولی فقط وقتی `currentDestination` یک tab اصلی باشه نمایش می‌ده، باید شرط رو اصلاح کنی تا در همه صفحات نمایش داده بشه.

**راه حل احتمالی:**
MiniPlayer باید همیشه (در همه route‌ها) نمایش داده بشه، نه فقط در تب‌های اصلی.
فقط وقتی که صفحه NowPlaying باز باشه باید پنهان بشه.

---

---

## #006 — MiniPlayer دکمه prev ندارد

- **شدت:** High
- **پیدا کرده:** Mehrdad
- **باید حل کنه:** Mehrdad — `mehrdad/playback-download`
- **فایل‌های مرتبط:** `ui/components/MiniPlayer.kt`
- **وضعیت:** ✅ Fixed — 2026-06-26 by Mehrdad (commit `43f0e28` on `mehrdad/playback-download`)

**توضیح:**
MiniPlayer فقط دکمه play/pause و next دارد. دکمه prev (قبلی) ندارد.
در `NowPlayingScreen` prev هست ولی در `MiniPlayer.kt` باید اضافه بشه.

---

## #007 — Sleep Timer فقط مقادیر ثابت دارد — باید custom input داشته باشد

- **شدت:** Low
- **پیدا کرده:** Mehrdad
- **باید حل کنه:** Mehrdad — `mehrdad/playback-download`
- **فایل‌های مرتبط:** `ui/player/PlayerViewModel.kt`، `ui/nowplaying/NowPlayingScreen.kt`
- **وضعیت:** ✅ Fixed — 2026-06-26 by Mehrdad (commit `43f0e28` on `mehrdad/playback-download`)

**توضیح:**
Sleep timer فعلاً احتمالاً با مقادیر ثابت (۱۵/۳۰/۶۰ دقیقه) کار می‌کنه.
بهتره کاربر بتونه هر عددی بذاره — مثلاً با یه `TextField` عددی یا `Slider`.

**راه حل پیشنهادی:**
یه dialog با ورودی دقیقه (TextField یا NumberPicker) اضافه کن که بعد از تأیید، timer ست بشه.

---

## #008 — نمی‌شود وضعیت Premium رو برای تست برگرداند

- **شدت:** Medium (فقط برای تست مهمه)
- **پیدا کرده:** Mehrdad
- **باید حل کنه:** Hadi — `hadi/data-auth-chat`
- **فایل‌های مرتبط:** `ui/profile/ProfileScreen.kt`، `ui/profile/ProfileViewModel.kt`، `data/local/datastore/SettingsDataStore.kt`
- **وضعیت:** Open

**توضیح:**
وقتی کاربر Premium می‌شه، راهی برای برگشت به حالت عادی وجود نداره.
در اپ‌های واقعی این منطقی هست، ولی ما برای تست (مثلاً نشون دادن پیام "نیاز به Premium داری") باید بتونیم حالت رو عوض کنیم.

**راه حل پیشنهادی:**
یه دکمه "Revoke Premium" فقط در debug build با `BuildConfig.DEBUG` چک بذار.
یا روش ساده‌تر: long-press روی premium badge → confirmation dialog → reset to free.

---

## #009 — معلوم نیست آهنگ دانلود‌شده از فایل محلی پخش می‌شه یا استریم

- **شدت:** Medium (برای دمو مهمه)
- **پیدا کرده:** Mehrdad
- **باید حل کنه:** Mehrdad — `mehrdad/playback-download`
- **فایل‌های مرتبط:** `ui/nowplaying/NowPlayingScreen.kt`، `ui/downloads/DownloadsScreen.kt`
- **وضعیت:** ✅ Fixed — 2026-06-26 by Mehrdad (commit `43f0e28` on `mehrdad/playback-download`)

**توضیح:**
وقتی آهنگی دانلود شده و از فایل محلی پخش می‌شه، هیچ نشانه بصری در NowPlayingScreen یا MiniPlayer وجود نداره.
برای TA و دمو خیلی مهمه که این تفاوت مشخص باشه.

**راه حل پیشنهادی:**
در `NowPlayingScreen` یه آیکون کوچک (مثل download/offline icon) نمایش بده وقتی `song.localPath != null`.

**چطور بفهمیم آهنگ local عه:**
در `PlayerViewModel`، `playbackState.currentSong?.localPath` رو چک کن — اگه null نباشه، از فایل محلی داره پخش می‌شه.

---

## #010 — تأخیر در شروع پخش وقتی روی آهنگ می‌زنی

- **شدت:** Medium
- **پیدا کرده:** Mehrdad
- **باید حل کنه:** Mehrdad — `mehrdad/playback-download`
- **فایل‌های مرتبط:** `data/player/PlayerControllerImpl.kt`، `data/player/MusicService.kt`
- **وضعیت:** ✅ Fixed — 2026-06-26 by Mehrdad (commit `43f0e28` on `mehrdad/playback-download`)

**توضیح:**
وقتی روی یه آهنگ می‌زنی، بعضی وقت‌ها چند ثانیه طول می‌کشه تا پخش شروع بشه.
این با مشکل #003 (تأخیر seek) فرق داره — اینجا مشکل buffering اولیه هست نه seek.

**علت احتمالی:**
ExoPlayer باید ابتدا به URL وصل بشه و buffer اولیه رو بگیره.
برای دمو، بهتره روی آهنگ‌های دانلودشده (local file) تست کنی — باید فوری شروع کنه.

---

## #011 — پس‌زمینه NowPlaying فقط یک رنگ dominant دارد — باید animated multi-color gradient باشد

- **شدت:** Low (زیبایی‌شناختی)
- **پیدا کرده:** Mehrdad
- **باید حل کنه:** Mehrdad — `mehrdad/playback-download`
- **فایل‌های مرتبط:** `ui/nowplaying/DominantColor.kt`، `ui/nowplaying/NowPlayingScreen.kt`
- **وضعیت:** ✅ Fixed — 2026-06-26 by Mehrdad (commit `43f0e28` on `mehrdad/playback-download`)

**توضیح:**
فعلاً `DominantColor.kt` فقط یه رنگ dominant از کاور استخراج می‌کنه و پس‌زمینه رو با اون رنگ (ثابت) پر می‌کنه.
هدف: همه رنگ‌های اصلی کاور با هم استخراج بشن و یه gradient انیمیشن‌دار بسازن که آروم بین اون رنگ‌ها شناور باشه — شبیه کاری که `AnimatedGradient.kt` توی Home Hero انجام می‌ده.

**راه حل پیشنهادی:**
1. از Palette چند رنگ بگیر (نه فقط dominant — vibrant، muted، darkVibrant هم):
   ```kotlin
   val colors = listOf(
       palette.vibrantSwatch?.rgb,
       palette.mutedSwatch?.rgb,
       palette.darkVibrantSwatch?.rgb,
       palette.lightMutedSwatch?.rgb,
   ).filterNotNull().map { Color(it) }
   ```
2. با `rememberAnimatedBrandGradient(colors)` (همون تابعی که Home Hero استفاده می‌کنه) یه gradient متحرک بساز.
3. این gradient رو به عنوان پس‌زمینه NowPlayingScreen استفاده کن.

**نکته:** `rememberAnimatedBrandGradient` قبلاً در `ui/components/AnimatedGradient.kt` وجود داره — نیازی به نوشتن از صفر نیست.

---

## #012 — AudioVisualizer به صدای واقعی واکنش نشان نمی‌دهد

- **شدت:** Low (زیبایی‌شناختی)
- **پیدا کرده:** Mehrdad
- **باید حل کنه:** Mehrdad — `mehrdad/playback-download`
- **فایل‌های مرتبط:** `ui/nowplaying/AudioVisualizer.kt`
- **وضعیت:** ✅ Fixed — 2026-06-26 by Mehrdad (commit `43f0e28` on `mehrdad/playback-download`)

**توضیح:**
Visualizer فعلی یه انیمیشن ثابت است که فقط بالا و پایین می‌رود.
به frequency یا amplitude واقعی صدای آهنگ وصل نیست.

**محدودیت تکنیکی مهم:**
برای Visualizer واقعی در Android، باید از `android.media.audiofx.Visualizer` API استفاده کرد.
این API نیاز به permission `RECORD_AUDIO` دارد و در Compose باید با Canvas هندل بشه.

**گزینه‌ها:**
1. **ساده (توصیه‌شده):** تعداد bar بیشتر، ارتفاع‌های random متنوع‌تر، و سرعت‌های مختلف برای هر bar
2. **پیشرفته:** `Visualizer` Android API رو به ExoPlayer وصل کن — نتیجه واقعی ولی پیچیده‌تر

---

## چطور مشکل جدید اضافه کنیم

کپی کن و پر کن:

```markdown
## #013 — عنوان مشکل

- **شدت:** Critical / High / Medium / Low
- **پیدا کرده:** [اسم]
- **باید حل کنه:** [اسم] — `[برنچ]`
- **فایل‌های مرتبط:** `...`
- **وضعیت:** Open

**توضیح:**
...
```
