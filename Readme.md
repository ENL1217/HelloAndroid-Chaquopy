# Android Chaquopy 影像處理專案

這是一個結合 Android 與 Python 的影像處理示範專案。透過 **Chaquopy** 框架，在 Android 中直接呼叫 Python 的 **OpenCV** 進行影像運算。

## 🌟 功能亮點
- **多種圖片來源**：支援從 `res/raw`、手機相簿 (Picker) 或 Python 本地讀取圖片。
- **異步處理**：使用 Java Thread 執行 Python 耗時運算，避免 UI 卡頓。
- **Python OpenCV 整合**：示範如何在 Python 端接收 `byte[]`，執行 Canny 邊緣檢測後回傳給 Java 顯示。

## 🛠️ 技術棧
- **Android**: Java, Activity Result API
- **Python**: OpenCV (cv2), Numpy
- **Bridge**: Chaquopy (v15.0+)

## 🚀 如何執行
1. 確保已安裝 Android Studio 與 Python 環境。
2. Clone 專案後，Gradle 會自動下載 Chaquopy 插件。
3. 在 `app/build.gradle` 的 `pip` 區塊中已配置 `opencv-python-headless`。

## 📂 核心程式碼
- **Java**: `MainActivity.java` 負責影像選取與 UI 顯示。
- **Python**: `opencv_process.py` 負責 `canny_from_image_bytes` 的處理邏輯。