# SpideyCode PWA (Next.js + iOS Safari Support)

Progressive Web App (PWA) edition of **SpideyCode** built for **Encode 5.0 x ZenithCup** (KIIT, KSAC, IoT Lab, AlgoZenith).

This web application brings the full Spider-Verse photobooth and AR mask experience to **iOS Safari (iPhone/iPad)**, Android browsers, and desktop devices without requiring native APK installation.

---

## ⚡ Tech Stack & Capabilities

- **Framework**: Next.js 16 (App Router) + React 19 + TypeScript
- **Styling**: Tailwind CSS v4 (Neo-Brutalist Spider-Verse Comic Aesthetics)
- **AR Face Tracking**: Google MediaPipe Face Landmarker (WASM + WebGL GPU acceleration) with LERP landmark smoothing
- **Compositing**: HTML5 2D Canvas high-resolution poster renderer with dynamic window detection & branding layers
- **Animations**: Lottie Web (spider animations) + Canvas Confetti
- **PWA & Offline**: Service Worker (`public/sw.js`) with cache-first asset strategy for offline venue reliability

---

## 🚀 Running Locally

```bash
# 1. Install dependencies
npm install

# 2. Run local dev server
npm run dev

# 3. Open in your browser
# http://localhost:3000
```

---

## 📱 Testing on iOS Safari / iPhone

1. Start the development server on your host machine:
   ```bash
   npm run dev -- -H 0.0.0.0
   ```
2. Connect your iPhone to the same local Wi-Fi network and open `http://<your-computer-ip>:3000` in Safari.
3. Tap the **Share** button at the bottom of Safari, then choose **"Add to Home Screen"**.
4. Launch **SpideyCode** directly from your iOS Home Screen for a 100% standalone, full-screen AR camera experience!

