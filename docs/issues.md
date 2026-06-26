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
- **وضعیت:** ✅ Fixed — 2026-06-27 by Mehrdad (commit `408deed` on `mehrdad/playback-download`)

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
- **فایل‌های مرتبط:** `data/player/MusicService.kt`، `ui/nowplaying/NowPlayingScreen.kt`
- **وضعیت:** ⚠️ Partial — shuffle/repeat داخل NowPlaying اضافه شد (commit `408deed`)، ولی هنوز در خودِ نوتیفیکیشن سیستم نیست.

**توضیح:**
نوتیفیکیشن سیستم فقط prev/pause/next دارد.
دکمه‌های shuffle و repeat حالا در `NowPlayingScreen` کار می‌کنن (commit `408deed`)، ولی در نوتیفیکیشن نیستن.

**نتیجه‌ی تست (Mehrdad):**
داخل اپ اوکی شد. Mehrdad گفت بد نیست که توی نوتیفیکیشن هم باشن.
اضافه کردنشون به نوتیفیکیشن نیاز به `CommandButton` سفارشی + `setCustomLayout` در `MediaSession` و
هندل کردن custom command در `MediaSession.Callback` داره — یه کار جدا و کمی ظریف. منتظر تصمیم.

---

## #003 — تأخیر در seek کردن روی progress bar

- **شدت:** Medium
- **پیدا کرده:** Mehrdad
- **باید حل کنه:** Mehrdad — `mehrdad/playback-download`
- **فایل‌های مرتبط:** `ui/player/PlayerViewModel.kt`، `data/player/PlayerControllerImpl.kt`
- **وضعیت:** ⚠️ Partial — 2026-06-27 by Mehrdad (commit `408deed`). نوار (slider) حالا فوری می‌پره، ولی خود صدا هنوز چند لحظه طول می‌کشه — وابسته به #004.

**توضیح:**
وقتی روی نوار پیشرفت (slider) در NowPlayingScreen انگشت می‌کشی تا جلو یا عقب بری،
یه تأخیر وجود داره و فوری جابجا نمی‌شه.

**نتیجه‌ی تست (Mehrdad):**
اون optimistic update باعث شد نشانگر slider بلافاصله بپره، ولی صدا هنوز کند جابجا می‌شه.
علتش اینه که ExoPlayer باید buffer جدید رو از روی استریم دانلود کنه. این ذاتیِ stream هست و
با عوض شدن منبع موسیقی (#004 — مثلاً فایل لوکال یا Jamendo) خودش حل می‌شه. کد سمت Mehrdad
کاری که می‌تونست بکنه رو کرده.

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
- **وضعیت:** ✅ Fixed — 2026-06-27 by Mehrdad (custom input در `43f0e28`، گزینه‌ی ۱۵ ثانیه در `8d73822`)

**نتیجه‌ی تست (Mehrdad):**
custom input کار کرد. درخواست شد یه گزینه‌ی کوتاه ۱۵ ثانیه‌ای هم باشه که توی تست/دمو راحت نشون داده بشه.
حالا (commit `8d73822`) sleep timer به **ثانیه** کار می‌کنه و یه گزینه‌ی «۱۵ ثانیه (test)» اضافه شد.
مشکل صفحه‌ی خالی بعد از اتمام تایمر هم به #017 منتقل و حل شد.

**توضیح:**
Sleep timer فعلاً احتمالاً با مقادیر ثابت (۱۵/۳۰/۶۰ دقیقه) کار می‌کنه.
بهتره کاربر بتونه هر عددی بذاره — مثلاً با یه `TextField` عددی یا `Slider`.

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
- **وضعیت:** ✅ Fixed — 2026-06-27 by Mehrdad (commit `8d73822`؛ آیکون اولیه در `43f0e28`)

**نتیجه‌ی تست (Mehrdad):**
آیکون اولیه (commit `43f0e28`) کم‌رنگ و نامحسوس بود — Mehrdad گفت فرقی حس نمی‌شه.
حالا (commit `8d73822`) زیر اسم خواننده یه برچسب واضحِ متنی «Playing from device» با آیکون نمایش
داده می‌شه وقتی `song.isDownloaded == true`. این روشن‌تره و توی دمو معلومه.

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
- **فایل‌های مرتبط:** `data/player/PlayerControllerImpl.kt`، `data/player/MusicService.kt`، `domain/model/Song.kt`
- **وضعیت:** ✅ Fixed — 2026-06-27 by Mehrdad (commit `8d73822`؛ بهبود buffer در `43f0e28`)

**ریشه‌ی واقعی (که Mehrdad توی تست پیدا کرد):**
آهنگ «کجایی» (محسن چاوشی) ۵ ثانیه طول می‌کشید و بعد یه **صدای آقا درباره‌ی Google Play** پخش می‌شد.
اون صدا همون `DEFAULT_FALLBACK_AUDIO_URL` قدیمی بود (فایل تستِ گوگل `play.mp3` که حرف می‌زنه).
یعنی URL اصلیِ اون آهنگ (SoundHelix) fail می‌شد و player بعد از ~۵ ثانیه انتظار به fallback سوییچ می‌کرد.

**کاری که شد (commit `8d73822`):**
1. `DEFAULT_FALLBACK_AUDIO_URL` از `play.mp3` (صدای حرف‌زدن) عوض شد به `Jazz_In_Paris.mp3`
   (موسیقی جاز بی‌کلام، روی همون CDN معتبر گوگل) — حالا اگه fallback هم بشه، موسیقیه نه حرف‌زدن.
2. `connectTimeout` و `readTimeout` در `MusicService` از ۸ ثانیه به ۴ ثانیه کم شد، تا اگه URL اصلی
   مرده باشه، خیلی سریع‌تر (به جای ~۸ ثانیه) به fallback سوییچ کنه.

**نکته:** علت اصلیِ fail شدنِ URLها، بی‌ثباتیِ منابع نمونه‌ی رایگانه — حل کاملش با #004 (عوض کردن منبع موسیقی).

**توضیح:**
وقتی روی یه آهنگ می‌زنی، بعضی وقت‌ها چند ثانیه طول می‌کشه تا پخش شروع بشه.
این با مشکل #003 (تأخیر seek) فرق داره — اینجا مشکل buffering اولیه هست نه seek.

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
- **وضعیت:** ⚠️ Partial — قشنگ‌تر شد (commit `43f0e28`) ولی هنوز به صدای واقعی واکنش نمی‌ده. منتظر تصمیم.

**نتیجه‌ی تست (Mehrdad):**
Visualizer قشنگ‌تر شد (سه موج bass/mid/treble)، ولی اونجوری که باید نیست: وقتی صدا بلندتر/تندتر
یا آروم‌تر می‌شه، Visualizer فرقی نمی‌کنه — یعنی هنوز یه انیمیشن تزئینیه، نه واقعاً وصل به صدا.

**برای واقعی شدن دو راه هست (هر دو نیاز به تصمیم):**
1. **`android.media.audiofx.Visualizer` API:** session id ExoPlayer رو می‌گیره و amplitude/FFT واقعی می‌ده.
   ولی از Android 9 به permission `RECORD_AUDIO` نیاز داره (یه permission prompt موقع دمو — کمی اذیت‌کننده).
2. **`TeeAudioProcessor` در ExoPlayer:** داده‌ی PCM رو tap می‌کنه و RMS amplitude حساب می‌کنه — بدون
   permission، ولی پیاده‌سازیش پیچیده‌تره (RenderersFactory سفارشی + flow به UI).

**توضیح:**
Visualizer فعلی یه انیمیشن ثابت است که فقط بالا و پایین می‌رود.
به frequency یا amplitude واقعی صدای آهنگ وصل نیست.

---

## #013 — آهنگ دانلودشده رو نمی‌شه حذف کرد

- **شدت:** High
- **پیدا کرده:** Mehrdad
- **باید حل کنه:** Mehrdad — `mehrdad/playback-download`
- **فایل‌های مرتبط:** `ui/downloads/DownloadsScreen.kt`
- **وضعیت:** ✅ Fixed — 2026-06-27 by Mehrdad (commit `8d73822`)

**توضیح:**
Mehrdad توی تست دید که نمی‌تونه آهنگ دانلودشده رو حذف کنه.

**ریشه:**
منطق حذف (`removeDownload` → cancel work + delete file + delete row) درست بود، ولی تنها راه حذف
یه swipe (کشیدن) بود که (الف) معلوم نبود وجود داره، (ب) در حالت RTL جهتش برعکس می‌شد.

**کاری که شد:**
یه دکمه‌ی حذف صریح (آیکون سطل) به هر ردیف اضافه شد، کنار وضعیت دانلود. swipe هم سر جاش هست.

---

## #014 — آپدیت آواتار در نوار بالا منعکس نمی‌شه و بعد از باز/بسته شدن اپ برمی‌گرده به دیفالت

- **شدت:** High
- **پیدا کرده:** Mehrdad
- **باید حل کنه:** Hadi (persistence در DataStore) + Mahyar (نوار بالا) — `hadi/data-auth-chat` / `mahyar/ui-navigation-theme`
- **فایل‌های مرتبط:** `ui/profile/ProfileViewModel.kt`، `data/local/datastore/SettingsDataStore.kt`، `ui/MainViewModel.kt`، `ui/components/AppTopBar.kt`
- **وضعیت:** Open

**توضیح:**
وقتی آواتار رو عوض می‌کنی:
1. عکس پروفایلِ گوشه‌ی بالا (TopBar) آپدیت نمی‌شه.
2. اپ رو که می‌بندی و باز می‌کنی، آواتار برمی‌گرده به همون دیفالت قبلی.

**ریشه‌ی احتمالی:**
آواتار جدید فقط در state موقتِ `ProfileViewModel` ذخیره می‌شه — نه در DataStore (پس persist نمی‌شه)
و `MainViewModel` (که TopBar رو تغذیه می‌کنه) اون رو observe نمی‌کنه. این بخشی از یه الگوی کلیه:
mutationهای کاربر persist نمی‌شن (مثل #008 و #015).

---

## #015 — فالو کردن artist بعد از باز/بسته شدن اپ نمی‌مونه

- **شدت:** Medium
- **پیدا کرده:** Mehrdad
- **باید حل کنه:** Hadi — `hadi/data-auth-chat`
- **فایل‌های مرتبط:** `data/repository/SocialRepositoryImpl.kt`، `data/local/` (نیاز به persist)
- **وضعیت:** Open

**توضیح:**
وقتی یه artist رو فالو می‌کنی و بعد اپ رو می‌بندی و دوباره باز می‌کنی، فالو از بین می‌ره.

**ریشه‌ی احتمالی:**
وضعیت فالو فقط in-memory (در mock/repository) نگه داشته می‌شه و در Room/DataStore ذخیره نمی‌شه.
همون الگوی persist نشدنِ #008 و #014.

---

## #016 — پیش‌نمایش چت با محتوای واقعی داخل گفتگو فرق داره (هم محتوا هم فارسی/انگلیسی)

- **شدت:** Medium
- **پیدا کرده:** Mehrdad
- **باید حل کنه:** Hadi — `hadi/data-auth-chat`
- **فایل‌های مرتبط:** `ui/chat/ChatListScreen.kt`، `ui/chat/ChatListViewModel.kt`، `data/repository/ChatRepositoryImpl.kt`، `data/mock/MockData.kt`
- **وضعیت:** Open

**توضیح:**
چیزی که در لیست پیام‌ها به عنوان پیش‌نمایش آخرین پیام نشون داده می‌شه، با چیزی که موقع باز کردن
گفتگو می‌بینی یکی نیست — هم از نظر محتوا و هم از نظر زبان (یکی فارسی، یکی انگلیسی).

**ریشه‌ی احتمالی:**
متنِ پیش‌نمایش (`Conversation.lastMessage` در `MockData.seedConversations`) جدا از پیام‌های واقعیِ
داخل گفتگوئه و با هم هماهنگ نیستن.

---

## #017 — بعد از اتمام Sleep Timer یه صفحه‌ی خالی عجیب می‌مونه

- **شدت:** Medium
- **پیدا کرده:** Mehrdad
- **باید حل کنه:** Mehrdad — `mehrdad/playback-download`
- **فایل‌های مرتبط:** `ui/player/PlayerViewModel.kt`، `ui/nowplaying/NowPlayingScreen.kt`
- **وضعیت:** ✅ Fixed — 2026-06-27 by Mehrdad (commit `8d73822`)

**توضیح:**
وقتی sleep timer تموم می‌شه و پخش متوقف می‌شه، صفحه‌ی NowPlaying خالی می‌مونه (چون `player.stop()`
صف رو پاک می‌کنه و آهنگ null می‌شه) و یه صفحه‌ی بی‌معنی نشون داده می‌شه.

**کاری که شد:**
1. موقع اتمام تایمر یه پیام «Sleep timer ended — playback stopped» (snackbar) نشون داده می‌شه.
2. NowPlaying وقتی آهنگ null بشه خودش به‌صورت خودکار بسته می‌شه و برمی‌گرده به صفحه‌ی قبل.

---

## چطور مشکل جدید اضافه کنیم

کپی کن و پر کن:

```markdown
## #018 — عنوان مشکل

- **شدت:** Critical / High / Medium / Low
- **پیدا کرده:** [اسم]
- **باید حل کنه:** [اسم] — `[برنچ]`
- **فایل‌های مرتبط:** `...`
- **وضعیت:** Open

**توضیح:**
...
```
