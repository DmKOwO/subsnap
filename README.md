# SubSnap 📸✨

> **Умный Sentence Mining на Android**: захват субтитров поверх любых приложений в 1 касание, мгновенная генерация flash-карточек через бесплатный Gemini Flash API, встроенный тренажер с алгоритмом SM-2, озвучка Text-to-Speech и экспорт в Anki.

[![Android CI](https://github.com/DmKOwO/subsnap/actions/workflows/release.yml/badge.svg)](https://github.com/DmKOwO/subsnap/actions)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2026.03-4285F4.svg?logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Gemini API](https://img.shields.io/badge/Google%20Gemini-3.8%20Flash-34A853.svg?logo=google&logoColor=white)](https://aistudio.google.com)

---

## 🌟 Возможности

1. **Мгновенный захват в 1 касание (1-Tap Instant Capture)**:
   - Плавающий кружок поверх всех приложений (`SYSTEM_ALERT_WINDOW`).
   - Быстрое касание (< 400 мс) сразу делает снимок экрана и сохраняет его в WebP без промежуточных меню.
   - Долгое нажатие открывает быстрые настройки: авто-захват (интервал 6–14 сек), переход в карточки, остановка службы.
   - Свободный drag-and-drop с магнитным прилипанием к краю экрана.

2. **Полноэкранный режим без вылетов (Android 14 / 15 / 16)**:
   - Динамический `virtualDisplay.resize(...)` на лету при повороте в Landscape (YouTube, плееры, игры).
   - Токен MediaProjection сохраняется активным на протяжении всей работы.

3. **100% бесплатный ИИ (Google AI Studio)**:
   - Поддержка актуального поколения моделей Gemini 3.x (`gemini-3.8-flash`, `gemini-3.5-flash`, `gemini-3.1-flash-lite`).
   - Автоматическое извлечение субтитров, ключевого слова, транскрипции IPA, перевода на русский и контекстного объяснения.
   - Встроенный оффлайн-фильтр **Google ML Kit OCR**: проверка наличия английских субтитров на устройстве перед отправкой в сеть (экономия квот и батареи).

4. **Встроенный тренажер карточек (SM-2 Spaced Repetition)**:
   - Изучайте слова прямо в приложении без сторонних программ.
   - Двусторонние карточки с переворотом и кнопками оценки («Снова», «Хорошо», «Легко»).
   - Озвучивание слова и предложения через встроенный синтезатор речи **Text-To-Speech (TTS)**.

5. **Экспорт в Anki**:
   - Экспорт в `.txt` (TSV) формат с разметкой картинок, готовый для импорта в AnkiDroid и Anki Desktop.

6. **OTA-обновления по воздуху через GitHub Releases**:
   - Автоматическая проверка новых версий при старте.
   - Загрузка и установка обновления прямо из приложения в один клик.

---

## 🚀 Установка

Скачайте готовый APK из раздела [GitHub Releases](https://github.com/DmKOwO/subsnap/releases) или соберите проект из исходников:

```bash
git clone https://github.com/DmKOwO/subsnap.git
cd subsnap
./gradlew assembleDebug
```

Готовый файл: `app/build/outputs/apk/debug/app-debug.apk`

---

## 🛠️ Стек технологий

- **Язык**: Kotlin 2.3.20
- **UI**: Jetpack Compose (Material 3)
- **Службы**: Android Foreground Service, MediaProjection API
- **OCR**: Google ML Kit Text Recognition
- **ИИ**: Google Gemini Multimodal REST API
- **Аудио**: Android TextToSpeech API
- **Алгоритм**: SuperMemo SM-2 Spaced Repetition
- **CI/CD**: GitHub Actions (автоматическая сборка релизов при пуше тегов `v*`)
