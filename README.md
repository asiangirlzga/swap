# Face Swap: Android app + Colab server

```
Android app  ──HTTPS──>  Cloudflare tunnel  ──>  server.py (FastAPI + InsightFace) on Colab GPU
```

## 1. Start the server (Colab)
1. Open Google Colab, then File → Upload notebook → `colab_server/FaceSwap_Server.ipynb`.
2. Runtime → Change runtime type → **GPU (T4)**.
3. Run all cells. The last one prints a **Server URL** (`https://xxxx.trycloudflare.com`) and a **Token**.
4. Keep the tab open. The URL and token change every time you restart.

## 2. Build the app
1. Open `android_app/` in Android Studio (Koala or newer) and let Gradle sync.
2. Run on a phone (Android 10+).
3. Paste the Server URL and Token, pick a source face, then either up to 50 images or one video, tick the consent box and tap Swap.
4. Results are saved to `Pictures/FaceSwap` and `Movies/FaceSwap`.

## Behaviour
- Images are processed one by one; photos with no detectable face are skipped and reported.
- All faces in each target image are replaced with the source face.
- Video: every frame is swapped, then re-encoded to H.264 with the original audio.
- "Resume last job" re-fetches a result if the app was closed while the server was working.
- Jobs are queued: one runs on the GPU at a time.

## Limits and caveats
- Colab free GPU sessions are time-limited (~12 h max, often less) and Google may restrict deepfake-style workloads; the notebook is plain Python, so the same `server.py` also runs on any GPU machine.
- The `inswapper_128` model is for non-commercial research use only.
- Speed on a T4: roughly 5–10 frames/s, so a 1-minute 30fps video takes a few minutes. 50 photos take about a minute plus upload time.
- The tunnel is public but every request needs the token.
- Use only with the consent of the people shown.
