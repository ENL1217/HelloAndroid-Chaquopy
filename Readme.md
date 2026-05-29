# Android CameraX + Python (OpenCV) 影像處理器

這個專案展示了如何結合 **Android CameraX** 與 **Chaquopy (Python)**，實現即時拍照並透過 Python 的 OpenCV 進行影像邊緣檢測（Canny Edge Detection）。

## 功能特點
- **CameraX 整合**：實作相機預覽與靜態影像擷取 (ImageCapture)。
- **Python 運算**：透過 Chaquopy 框架在 Android 中執行 Python 腳本。
- **影像處理**：將相機擷取的 ImageProxy 轉換為 Byte 陣列，傳遞給 Python 進行 Canny 濾波處理。
- **異步處理**：使用背景執行緒進行 Python 運算，確保 UI 介面流暢不卡頓。

## 系統需求
- Android Studio Ladybug 或更新版本
- Python 3.8+ (透過 Chaquopy 整合)
- 權限：需要 Camera 攝影機權限

## 核心代碼流程
1. **初始化 Python**：在 `onCreate` 啟動 Python 虛擬環境。
2. **啟動相機**：使用 CameraX 綁定 `Preview` 與 `ImageCapture`。
3. **拍照與處理**：
    - 點擊按鈕觸發 `takePicture`。
    - `onCaptureSuccess` 獲取影像數據。
    - 轉換為 `byte[]` 並由 Python 模組 `opencv_process.canny_from_image_bytes` 進行運算。
    - 將回傳的影像顯示於 `resultView`。

## Python 依賴 (requirements.txt)