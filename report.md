# گزارش پروژه نهایی برنامه‌نویسی دستگاه‌های سیار

## 1. معرفی برنامه و هدف پروژه

این پروژه یک اپلیکیشن اندروید برای استریم موسیقی همراه با امکانات کوچک شبکه اجتماعی است. ایده کلی برنامه شبیه Spotify/Melodify است: کاربر می‌تواند آهنگ‌ها و پلی‌لیست‌ها را مرور کند، جست‌وجو انجام دهد، آهنگ‌ها را لایک کند، سابقه پخش را ببیند، با کاربران دیگر گفت‌وگو کند و در صورت داشتن حساب Premium دانلود آفلاین انجام دهد.

هدف پروژه نمایش پیاده‌سازی مفاهیم اصلی درس شامل Jetpack Compose، معماری تمیز، MVVM/UDF، مدیریت وضعیت با Flow، پخش رسانه، ذخیره‌سازی محلی، پس‌زمینه، چندزبانه بودن و شبیه‌سازی ارتباط بلادرنگ است.

## 2. تکنولوژی‌های استفاده‌شده

- Kotlin و Jetpack Compose برای رابط کاربری
- Material 3، Navigation Compose و Compose animation
- Koin برای Dependency Injection
- StateFlow و Channel/SharedFlow-style effects برای مدیریت وضعیت و پیام‌های یک‌باره
- Room برای کش محلی لایک‌ها، دانلودها، تاریخچه جست‌وجو، پخش اخیر و پیام‌های چت
- DataStore Preferences برای تنظیمات کاربر
- WorkManager برای شبیه‌سازی دانلود پس‌زمینه
- Media3/ExoPlayer و MediaSessionService برای پخش موسیقی
- Paging 3 و PagingSource آماده برای لیست‌های طولانی/چت
- Coil برای بارگذاری تصویر
- Palette برای رنگ پس‌زمینه پویا در صفحه Now Playing

## 3. معماری پروژه و ساختار پوشه‌ها

پروژه از ساختار سه‌لایه استفاده می‌کند:

- `ui`: صفحات Compose، ViewModelها، کامپوننت‌های مشترک، ناوبری و تم
- `domain`: مدل‌های دامنه، Repository interfaceها، UseCaseها و قرارداد PlayerController
- `data`: پیاده‌سازی Repositoryها، Room، DataStore، mock data، WebSocket جعلی، WorkManager و Media3
- `di`: ماژول‌های Koin برای data، domain و presentation
- `core`: ابزارهای مشترک مثل متن‌های قابل ترجمه، فرمت زمان و مدیریت locale

این ساختار باعث شده UI مستقیما به دیتابیس، Media3 یا WorkManager وابسته نباشد و از طریق ViewModel/UseCase/Repository کار کند.

## 4. Clean Architecture، MVVM و UDF

الگوی اصلی پروژه MVVM با جریان داده یک‌طرفه است. هر صفحه وضعیت خود را از ViewModel به صورت `StateFlow` دریافت می‌کند. تعامل کاربر از صفحه به ViewModel منتقل می‌شود و ViewModel با Repository یا UseCase کار می‌کند.

برای پیام‌های یک‌باره مثل خطای Premium یا Snackbar، از Channel/Flow در `PlayerViewModel` و `ProfileViewModel` استفاده شده است تا این پیام‌ها داخل state پایدار صفحه باقی نمانند.

## 5. Room و DataStore

Room در `AppDatabase` تعریف شده و این جدول‌ها را پوشش می‌دهد:

- `liked_songs`
- `recently_played`
- `downloads`
- `search_history`
- `chat_messages`

DAOها برای مشاهده داده با Flow، درج/حذف، به‌روزرسانی وضعیت دانلود و کش پیام‌های چت استفاده می‌شوند. نسخه Room پروژه به `2.7.2` به‌روزرسانی شده تا با Kotlin 2 و KSP درست build شود.

DataStore تنظیمات کاربر را نگه می‌دارد: زبان، حالت تم، اندازه فونت، وضعیت Premium و تنظیمات پخش. `MainActivity` زبان ذخیره‌شده را قبل از ساخت UI اعمال می‌کند تا RTL/LTR درست تنظیم شود.

## 6. Media Playback، Media3/ExoPlayer و سرویس پس‌زمینه

پخش موسیقی با Media3 انجام می‌شود. `MusicService` به عنوان `MediaSessionService` در Manifest ثبت شده و `PlayerControllerImpl` از طریق `MediaController` به آن وصل می‌شود. UI فقط با `PlayerViewModel` و `PlayerController` کار می‌کند.

MiniPlayer در صفحات اصلی نمایش داده می‌شود و صفحه Now Playing شامل کاور چرخان هنگام پخش، play/pause، next/previous، slider پیشرفت، seek، کنترل سرعت 1x/1.5x/2x، sleep timer، visualizer با Canvas و پس‌زمینه گرادیانی پویا یا fallback است.

برای پایداری، `PlayerControllerImpl` اگر MediaController هنوز آماده نباشد عملیات را در صف نگه می‌دارد و بعد از اتصال اجرا می‌کند. اعلان و MediaSession از قابلیت‌های Media3 service استفاده می‌کنند. کش هوشمند استریم با `CacheDataSource` روی `SimpleCache` (در `PlaybackCache`) پیاده شده تا جلو/عقب کردن و پخش مجدد، اینترنت را دوباره مصرف نکند؛ و Audio Focus با `setAudioAttributes(..., handleAudioFocus = true)` به‌همراه واکنش به قطع‌شدن صدا (`setHandleAudioBecomingNoisy(true)`) مدیریت می‌شود.

**منبع موسیقی (معماری منبع داده):** برای جداسازی محل آمدن داده‌ها از بقیهٔ برنامه، یک انتزاع
`RemoteMusicDataSource` تعریف شده است با دو پیاده‌سازی:

* `JamendoMusicDataSource` — موسیقی **واقعیِ Creative Commons** را از **Jamendo API** می‌گیرد
  (صدای این آثار به‌صورت قانونی قابل استریم است). این منبع فقط زمانی فعال می‌شود که یک
  `client_id` تنظیم شده باشد (`BuildConfig.JAMENDO_CLIENT_ID` از طریق پراپرتی گریدل
  `jamendoClientId`). در صورت هر خطا/نبود کلید، برنامه به‌صورت خودکار به منبع ماک برمی‌گردد.
* `MockMusicDataSource` — کاتالوگ درون‌حافظه‌ای (`MockData`) با **۵۰ آهنگ معروفِ واقعی**
  (ایرانی/انگلیسی/جهانی). عنوان و خواننده واقعی‌اند، اما چون **نسخهٔ اصلی این آثار کپی‌رایت دارد و
  قابل استریم قانونی نیست**، صدای پخش از نمونه‌های **پایدار و آزاد (royalty-free، SoundHelix)** استفاده می‌شود.

> **چرا YouTube Music استفاده نشد؟** یوتیوب/یوتیوب‌موزیک هیچ **API رسمی برای استریم مستقیم صدا**
> ارائه نمی‌دهد و روش‌های استخراج/دانلود غیررسمی هم ناقض شرایط استفاده‌اند؛ بنابراین طبق الزام،
> از آن‌ها استفاده نشد و به‌جای آن از Jamendo (موسیقی آزاد) یا نمونه‌های پایدار عمومی بهره گرفتیم.

**پایداری پخش:** هر آهنگ یک URL اصلی و یک **URL پشتیبان مطمئن** (`DEFAULT_FALLBACK_AUDIO_URL`)
دارد. اگر منبعی خراب باشد، در `PlayerControllerImpl` رویداد `onPlayerError` ابتدا با URL پشتیبان
دوباره تلاش و سپس در صورت نیاز به آهنگ بعدی پرش می‌کند؛ پلیر هرگز کرش نمی‌کند یا گیر نمی‌کند.
اگر آهنگ **دانلود** شده باشد، به‌جای استریم از **فایل محلی** پخش می‌شود. لمس هر آهنگ از
Home/Search/Playlist/Liked/Recent پخش را آغاز و مینی‌پلیر/Now Playing را به‌روزرسانی می‌کند.

**دکمهٔ دانلود:** در صفحهٔ Now Playing کنار دکمهٔ لایک یک آیکون **دانلود** قرار دارد؛ برای کاربر
Premium دانلود (WorkManager) آغاز می‌شود و برای کاربر عادی پیام «نیاز به Premium» نمایش داده می‌شود.

**نرمی اجرا (Performance):** اندازهٔ تصاویر کاور برای رمزگشایی سریع‌تر کاهش یافت و بارگذاری با Coil
کش می‌شود. توجه: خروجی **Debug** ذاتاً کندتر از **Release** است؛ برای ارزیابی نرمی واقعی، نسخهٔ
Release (`assembleRelease`) را روی دستگاه نصب کنید. تغییر نسخهٔ SDK تأثیری بر نرمی ندارد
(compileSdk/targetSdk از قبل ۳۶ است و با Android دستگاه‌های جدید مثل Galaxy A55 سازگار است).

## 7. Premium logic و WorkManager download

در این پروژه کاربران عادی می‌توانند موسیقی را کامل گوش دهند. Premium فقط برای دانلود آفلاین لازم است. هنگام درخواست دانلود، `DownloadSongUseCase` وضعیت Premium را از تنظیمات می‌خواند و در صورت نبود Premium پیام مناسب به UI ارسال می‌شود.

دانلودها با `DownloadWorker` و **WorkManager** در پس‌زمینه انجام می‌شوند؛ Worker فایل صوتی را به‌صورت واقعی از URL استریم می‌خواند و در حافظهٔ داخلی برنامه ذخیره می‌کند و وضعیت/پیشرفت/مسیر فایل را در Room به‌روزرسانی می‌کند. **پخش از مسیر محلی:** پس از دانلود، `PlaySongsUseCase`/`Song.playbackUri` تشخیص می‌دهد که فایل آفلاین موجود است و به‌جای استریم، آهنگ مستقیماً از فایل محلی پخش می‌شود. صفحه Downloads مرتب‌سازی و حذف با کشیدن (Swipe) را پشتیبانی می‌کند.

## 8. Search debounce و Paging/Paging-ready

صفحه Search از Flow و debounce استفاده می‌کند تا با هر تایپ کاربر بلافاصله جست‌وجوی سنگین انجام نشود. تاریخچه جست‌وجو در Room ذخیره می‌شود.

برای Paging، وابستگی Paging 3 اضافه شده است. صفحات Playlist Detail و Chat Detail به‌صورت کامل با `LazyPagingItems` (از طریق `collectAsLazyPagingItems`) رندر می‌شوند؛ منبع‌ها به‌ترتیب `ListPagingSource` (برای داده‌های درون‌حافظه‌ای) و `PagingSource` مبتنی بر Room هستند. سایر لیست‌ها نیز با همین الگو برای اتصال به Paging آماده‌اند.

## 9. Chat realtime، fake WebSocket و offline cache

برای چت، یک انتزاع `ChatSocket` و پیاده‌سازی `FakeChatSocket` وجود دارد. این بخش polling نیست و پیام‌ها از مسیر Flow/WebSocket جعلی وارد Repository می‌شوند. `MelodyApp` هنگام شروع برنامه اتصال چت را باز می‌کند.

پیام‌ها در Room ذخیره می‌شوند تا تاریخچه چت آفلاین باقی بماند. صفحه Chat List مکالمه‌ها و unread badge را نشان می‌دهد. صفحه Chat Detail لیست پیام‌ها، ورودی متن، ارسال پیام، typing indicator، وضعیت sending/sent/read و کارت آهنگ اشتراکی را پشتیبانی می‌کند.

## 10. Localization، RTL/LTR و Theme

برنامه منابع متنی انگلیسی و فارسی دارد (`values/strings.xml` و `values-fa/strings.xml`). Manifest با `supportsRtl=true` تنظیم شده و `LocaleManager` زبان انتخابی را روی context اعمال می‌کند.

تم برنامه با Material 3 ساخته شده و حالت Light/Dark/System، اندازه فونت و رنگ‌های اختصاصی را پشتیبانی می‌کند. آیکون adaptive اختصاصی در منابع launcher وجود دارد.

## 11. صفحات اصلی و فرعی

صفحات اصلی:

- Home
- Search
- Downloads
- Playlists
- Profile

صفحات فرعی:

- Settings
- Now Playing
- Playlist Detail
- Liked Songs
- Recently Played
- Followed Users/Artists
- Chat List
- Chat Detail
- User Profile
- Artist Profile
- Notifications

ناوبری با `AppNavHost` و `Routes` انجام شده است. Bottom Navigation فقط در تب‌های اصلی دیده می‌شود و MiniPlayer بالای Bottom Navigation قرار می‌گیرد.

**احراز هویت شبیه‌سازی‌شده (ورود/خروج/ساخت حساب):** ریشهٔ برنامه در `MainActivity` بر اساس مقدار `isLoggedIn` در DataStore تصمیم می‌گیرد: اگر کاربر وارد نشده باشد صفحهٔ `AuthScreen` و در غیر این‌صورت برنامهٔ اصلی نمایش داده می‌شود. صفحهٔ ورود شامل لوگو و نام برنامه، ورودی نام و نام‌کاربری و دکمهٔ «ساخت حساب / ورود» (به‌علاوهٔ گزینهٔ «ادامه به‌عنوان کاربر نمونه») است. هنگام ورود، نام/نام‌کاربری/آواتار و `isLoggedIn=true` به‌صورت **یک نوشتن اتمیک** در DataStore ذخیره می‌شوند و `SocialRepository` کاربر جاری را بر همین اساس برمی‌گرداند (نام و آواتار در پروفایل و TopBar نمایش داده می‌شود). **خروج** نیز یک نوشتن اتمیک است که `isLoggedIn=false` را تنظیم، Premium را بازنشانی و هویت را پاک می‌کند؛ بنابراین خروج هیچ‌وقت گیر نمی‌کند و برنامه به‌طور خودکار به صفحهٔ ورود بازمی‌گردد (دکمهٔ Back هم کاربر را به برنامهٔ اصلی برنمی‌گرداند). داده‌های Room هنگام خروج پاک نمی‌شوند. این جریان کاملاً شبیه‌سازی‌شده و محلی است (بدون سرور احراز هویت واقعی).

## 12. نحوه اجرای پروژه در Android Studio

1. پروژه را در Android Studio باز کنید.
2. اجازه دهید Gradle Sync کامل شود.
3. JDK سازگار با Gradle/Android Studio را انتخاب کنید.
4. یک emulator یا دستگاه واقعی با حداقل Android 7.0/API 24 آماده کنید.
5. از نوار بالا کانفیگ `app` را انتخاب کنید.
6. روی Run بزنید.

## 13. نحوه گرفتن APK

از ترمینال ریشه پروژه اجرا کنید:

```powershell
.\gradlew.bat :app:assembleDebug
```

در صورت موفقیت، APK در مسیر زیر ساخته می‌شود:

```text
app/build/outputs/apk/debug/app-debug.apk
```

در Android Studio نیز می‌توان از مسیر `Build > Build Bundle(s) / APK(s) > Build APK(s)` استفاده کرد.

## 14. سناریوی پیشنهادی ویدیو

1. اجرای برنامه و نمایش Home، اسلایدر/لیست‌ها و MiniPlayer
2. پخش یک آهنگ و رفتن به Now Playing
3. نمایش play/pause، next/previous، slider، speed و sleep timer
4. جست‌وجوی یک آهنگ و توضیح debounce
5. لایک کردن آهنگ و نمایش در Liked Songs
6. باز کردن Playlist Detail و پخش Play All/Shuffle
7. نمایش Downloads و توضیح اینکه دانلود نیازمند Premium است
8. ارتقا به Premium از Profile و تلاش دوباره برای دانلود
9. نمایش Chat List و Chat Detail، ارسال پیام و typing/status
10. تغییر زبان/تم از Settings و نمایش RTL/LTR
11. اشاره به Room، DataStore، WorkManager، Media3 service و Koin در ساختار پروژه

## 15. محدودیت‌های صادقانه

- منبع داده با انتزاع `RemoteMusicDataSource` پیاده شده: **Jamendo** (موسیقی واقعی Creative Commons، در صورت تنظیم `client_id`) یا منبع ماک با **۵۰ آهنگ معروفِ واقعی** (ایرانی/انگلیسی/جهانی). بدون کلید، به‌صورت پیش‌فرض از منبع ماک استفاده می‌شود.
- **منبع صوتی و کپی‌رایت (مهم):** در حالت ماک، عنوان و خوانندهٔ آهنگ‌ها واقعی‌اند اما نسخهٔ اصلی این آثار کپی‌رایت دارد و قابل استریم قانونی نیست؛ پس صدای **پخش** از نمونه‌های آزاد (royalty-free، SoundHelix) با URL پشتیبان است و کاورها تصاویر پایدار‌اند (نه کاور اصلی آلبوم). با فعال‌کردن Jamendo، صدا و کاورِ واقعیِ آثارِ آزاد بارگذاری می‌شوند.
- **YouTube/YouTube Music استفاده نشد** چون API رسمی استریم مستقیم صدا ندارد و روش‌های غیررسمی ناقض شرایط‌اند.
- **احراز هویت/خروج شبیه‌سازی‌شده است:** ساخت حساب/ورود/خروج فقط با DataStore انجام می‌شود و سرور احراز هویت واقعی ندارد؛ هویت کاربر (نام/نام‌کاربری/آواتار) محلی ذخیره می‌شود.
- چت بلادرنگ با `FakeChatSocket` شبیه‌سازی شده و WebSocket واقعی شبکه‌ای نیست.
- Crossfade به صورت کامل و حرفه‌ای پیاده‌سازی نشده و در نسخه فعلی ساده/محدود است.
- اعلان/MediaSession بر پایه Media3 service وجود دارد، اما رفتار دقیق اعلان ممکن است به نسخه اندروید و permission اعلان (`POST_NOTIFICATIONS`) وابسته باشد.

## 16. وضعیت build نهایی

برای آماده‌سازی build، تنظیمات Gradle با AGP 9/Kotlin 2 هماهنگ شد، نسخه‌های ناسازگار AndroidX Core/Activity/Lifecycle اصلاح شدند، Room به نسخه سازگارتر به‌روزرسانی شد و چند خطای import/API در Compose رفع شد. علاوه بر این، در آخرین مرحله، پایداری پخش (URL پشتیبان + بازیابی خطا) و یک جریان احراز هویت/خروج شبیه‌سازی‌شده اضافه شد.

> **توجه دربارهٔ نسخهٔ Java:** بیلد باید با **JDK 17+** اجرا شود (Android Studio از JBR داخلی خود استفاده می‌کند). اگر `java` خط فرمان نسخهٔ ۸ باشد، ابتدا `JAVA_HOME` را روی JBR همراه Android Studio تنظیم کنید.

دستور زیر با موفقیت و بدون هشدار اجرا شده است (با JDK 21 از JBR اندروید استودیو):

```powershell
.\gradlew.bat :app:assembleDebug
```

خروجی (BUILD SUCCESSFUL) در این مسیر ساخته می‌شود:

```text
app/build/outputs/apk/debug/app-debug.apk
```
