# Roop Face Swap – Android app

Android client + Python backend built from your `roop.ipynb` notebook.
Roop needs a GPU and ONNX models, so it can't realistically run on a phone.
The app uploads a face image and a video to your server, the server runs roop,
and the app plays and saves the result.

```
Android app (Kotlin)  ──HTTP──▶  server/app.py (FastAPI)  ──▶  roop/run.py
```

## Responsible use
Only use faces of people who consented. Do not use the output to deceive,
harass or impersonate. Label results as AI-edited. The app asks for a consent
tick before every run.

## 1. Put it on GitHub and build the APK
```bash
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/<you>/<repo>.git
git push -u origin main
```
GitHub Actions (`.github/workflows/build-apk.yml`) builds automatically.
Open the **Actions** tab → latest run → download **RoopSwap-debug-apk** → unzip → install `app-debug.apk`
(allow "install unknown apps").

## 2. Run the backend (needs NVIDIA GPU, or set PROVIDER=cpu – very slow)
```bash
cd server
bash setup.sh            # clones roop, installs deps, downloads inswapper_128.onnx
API_KEY=mysecret PROVIDER=cuda python app.py
```
Needs `ffmpeg` installed. For GPU use `pip install onnxruntime-gpu` as in roop's README.
To expose it to your phone use ngrok (`ngrok http 8000`) or run it on a cloud VM.
See `server/colab_server.py` for a Colab recipe.

## 3. Use the app
Enter the server URL (and API key), pick a face image and a video, tick the
consent box, press **Start swap**.

## Project layout
```
.github/workflows/build-apk.yml   CI that builds the APK
app/                              Android app (Kotlin, views)
server/app.py                     FastAPI wrapper for roop
server/setup.sh                   notebook steps as a script
```
