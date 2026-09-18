# Realtime Screen Processing & OCR Guidelines

When implementing real-time screen capture, subtitle mining, or OCR pipelines on Android:

1. **Two-Stage Cascade Architecture**:
   - Never run heavy ML Kit OCR or neural models continuously on raw frames.
   - Always implement a Stage 1 zero-allocation luminance diff detector sampled directly from `Image.planes[0]` buffers downscaled to a tiny matrix (e.g. 48x16).
   - Only trigger Stage 2 (OCR / processing) when a significant positive luminance delta (text appearance) is detected in the target region.

2. **Disappearance vs Appearance Separation**:
   - Track directional delta (`brightened` vs `darkened` pixel fractions).
   - Subtitle disappearance (darkening back to background) must silently adapt the baseline frame without triggering OCR or disk writes.

3. **Debounce & Settle Time**:
   - Subtitle renders and UI transitions often include a 150-300ms transition.
   - Always debounce ~250–350ms before grabbing the final settled frame for OCR to ensure full text clarity.

4. **Self-App Suppression**:
   - Always track application foreground state (`MainActivity.isAppInForeground`).
   - Suppress background auto-capture while the user is actively interacting with the app's own UI.

5. **Configuration & Rotation Safety**:
   - Always reset baseline matrices when `VirtualDisplay` is resized or screen orientation changes to avoid false diff spikes.
