# Smart Lecture Notes 🎓 📓
### تطبيق تسجيل وتلخيص المحاضرات الجامعية الذكي المدعوم بـ Gemini 3.8 Flash

[![Android CI & Build APK](https://github.com/actions/workflows/android.yml/badge.svg)](https://github.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-M3-green.svg?logo=android)](https://developer.android.com/jetpack/compose)
[![Gemini 3.8 Flash](https://img.shields.io/badge/AI-Gemini%203.8%20Flash-orange.svg)](https://ai.google.dev)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 📖 نبذة عن التطبيق (Overview)
**Smart Lecture Notes** هو رفيق الطالب الجامعي المتكامل. يقوم التطبيق بالاستماع إلى صوت الدكتور أو المحاضر في القاعة الدراسية، وتسجيله وتفريغه، ثم كتابته تلقائياً على **ورقة دفترية جامعية مسطرة (Notebook Paper View)**، مع تلخيصه واستخراج النقاط الأساسية، وتوضيح المصطلحات الصعبة، وصياغة أسئلة اختبار متوقعة، وترجمة المحاضرة إلى أي لغة عبر أحدث نماذج الذكاء الاصطناعي **Gemini 3.8 Flash**.

---

## ✨ المميزات الرئيسية (Features)

- 🎙️ **تسجيل وتفريغ صوتي دقيق**:
  - تسجيل المحاضرات الصوتية بأعلى جودة وتفريغ الكلام العربي والأجنبي بدقة متناهية.
- 📓 **ورقة المحاضرة الدفترية (Notebook Paper View)**:
  - تصميم يحاكي دفتر الكشكول الجامعي الحقيقي بسطور زرقاء/وردية وهامش جانبي.
  - تفريغ سطور الأستاذ مباشرة على الورقة مع خيارات للنسخ، والمشاركة، والتكبير.
- ⚡ **التلخيص والشرح الذكي عبر Gemini 3.8 Flash**:
  - تلخيص فوري للمحاضرات الطويلة.
  - استخراج أهم النقاط والمصطلحات المعقدة.
  - اقتراح أسئلة اختبار جامعية متوقعة للتدريب قبل الامتحان.
- 🌐 **ترجمة فورية للمحاضرات**:
  - ترجمة ورقة المحاضرة بالكامل لأي لغة (عربية، إنجليزية، فرنسية، وغيرها) مع الحفاظ على الصياغة الأكاديمية.
- 🎨 **تخصيص الهوية والألوان (ذكر / فتاة) + وضع ليلي ونهاري**:
  - **طالب جامعي (ذكر) 👨‍🎓**: يتحول مظهر التطبيق بالكامل للون **الأزرق الملكي 💙**.
  - **طالبة جامعية (فتاة) 👩‍🎓**: يتحول مظهر التطبيق بالكامل للون **الوردي الأنيق 💖**.
  - **وضع نهاري ☀️ & وضع ليلي 🌙**: خلفيات مريحة للعين أثناء الدراسة الليلية والمراجعة.
- 🗣️ **قراءة صوتية ومحادثة حية (Live Voice & TTS)**:
  - إمكانية الاستماع لنص المحاضرة والورقة صوتياً.
  - غرفة محادثة صوتية مباشرة لسؤال المساعد الأكاديمي والحصول على إجابات فورية.
- 📁 **تنظيم المواد والمجلدات (Folders & Offline Storage)**:
  - تصنيف المحاضرات بحسب المواد الدراسية.
  - حفظ كامل محلي عبر **Room Database** للوصول للمحاضرات حتى بدون اتصال بالإنترنت.

---

## 🛠️ البنية التقنية (Tech Stack)

- **لغة البرمجة**: Kotlin
- **واجهة المستخدم**: Jetpack Compose & Material Design 3 (M3)
- **قاعدة البيانات**: Room Database (SQLite)
- **الذكاء الاصطناعي**: Google Gemini REST API (`gemini-3.8-flash`)
- **الصوتيات**: Android MediaRecorder + TextToSpeech (TTS) + SpeechRecognizer
- **المعمارية البرمجية**: Clean Architecture / MVVM (Model-View-ViewModel) + Kotlin Coroutines & StateFlow

---

## 🚀 كيفية تشغيل وبناء المشروع (Build & Run)

### 1. المتطلبات الأساسية
- **Android Studio Ladybug (أو أحدث)**
- **JDK 17** أو **JDK 21**
- **Android SDK API 34+**

### 2. استنساخ المستودع (Clone)
```bash
git clone https://github.com/your-username/smart-lecture-notes.git
cd smart-lecture-notes
```

### 3. إعداد مفتاح Gemini API Key
يمكنك إدخال مفتاح Gemini الخاص بك بطريقتين:
1. **من داخل التطبيق مباشرة**: اضغط على أيقونة الإعدادات ⚙️ في الشريط العلوي وألصق المفتاح.
2. أو من خلال ملف `.env` في المجلد الرئيسي:
```properties
GEMINI_API_KEY=your_actual_gemini_api_key_here
```

### 4. البناء والتشغيل عبر سطر الأوامر (Gradle)
```bash
# منح الصلاحية لـ Gradle Wrapper (Linux / macOS)
chmod +x gradlew

# بناء نسخة الـ APK
./gradlew assembleDebug

# تشغيل الاختبارات
./gradlew testDebugUnitTest
```
سيكون ملف الـ APK الناتج متوفراً في المسار:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🤖 البناء التلقائي عبر GitHub Actions (CI/CD)

يحتوي هذا المستودع على إعداد جاهز لـ **GitHub Actions** في المسار `.github/workflows/android.yml`.
عند رفع المشروع على GitHub:
1. سيتم تشغيل البناء التلقائي عند كل `push` أو `pull_request`.
2. يمكنك تحميل ملف الـ APK الجاهز مباشرة من تبويب **Actions** -> اختر آخر تشغيل -> حمل الـ **Artifact (SmartLectureNotes-APK)**.

---

## 📄 الترخيص (License)
هذا المشروع مرخص تحت رخصة [MIT License](LICENSE).
