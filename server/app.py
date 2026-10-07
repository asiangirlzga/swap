"""
Backend for the Android app. Wraps roop (https://github.com/s0md3v/roop).

Env vars:
  API_KEY    optional shared secret (app sends it as X-API-Key)
  PROVIDER   roop execution provider: cuda | cpu | coreml (default cuda)
  PORT       default 8000
"""
import os, shutil, subprocess, sys, threading, uuid
from pathlib import Path

from fastapi import FastAPI, File, Form, Header, HTTPException, UploadFile
from fastapi.responses import FileResponse
import uvicorn

ROOT = Path(__file__).parent.resolve()
ROOP_DIR = ROOT / "roop"
WORK = ROOT / "work"
WORK.mkdir(exist_ok=True)

API_KEY = os.getenv("API_KEY", "")
PROVIDER = os.getenv("PROVIDER", "cuda")

app = FastAPI(title="Roop backend")
jobs: dict[str, dict] = {}
lock = threading.Semaphore(1)  # one swap at a time


def check(key: str | None):
    if API_KEY and key != API_KEY:
        raise HTTPException(401, "Bad API key")


def run_job(job_id: str, enhance: bool):
    d = WORK / job_id
    out = d / "swapped.mp4"
    with lock:
        jobs[job_id].update(status="running", message="")
        processors = ["face_swapper"] + (["face_enhancer"] if enhance else [])
        cmd = [
            sys.executable, "run.py",
            "--target", str(d / "target.mp4"),
            "--source", str(d / "source.jpg"),
            "-o", str(out),
            "--output-video-quality", "80",
            "--execution-provider", PROVIDER,
            "--frame-processor", *processors,
        ]
        try:
            p = subprocess.run(cmd, cwd=ROOP_DIR, capture_output=True, text=True)
            if p.returncode == 0 and out.exists():
                jobs[job_id].update(status="done")
            else:
                jobs[job_id].update(status="error", message=(p.stderr or p.stdout)[-400:])
        except Exception as e:
            jobs[job_id].update(status="error", message=str(e))


@app.post("/swap")
async def swap(
    source: UploadFile = File(...),
    target: UploadFile = File(...),
    enhance: bool = Form(True),
    x_api_key: str | None = Header(default=None),
):
    check(x_api_key)
    job_id = uuid.uuid4().hex[:12]
    d = WORK / job_id
    d.mkdir()
    with open(d / "source.jpg", "wb") as f:
        shutil.copyfileobj(source.file, f)
    with open(d / "target.mp4", "wb") as f:
        shutil.copyfileobj(target.file, f)
    jobs[job_id] = {"status": "queued", "message": ""}
    threading.Thread(target=run_job, args=(job_id, enhance), daemon=True).start()
    return {"job_id": job_id}


@app.get("/status/{job_id}")
def status(job_id: str, x_api_key: str | None = Header(default=None)):
    check(x_api_key)
    if job_id not in jobs:
        raise HTTPException(404, "Unknown job")
    return jobs[job_id]


@app.get("/result/{job_id}")
def result(job_id: str, x_api_key: str | None = Header(default=None)):
    check(x_api_key)
    f = WORK / job_id / "swapped.mp4"
    if not f.exists():
        raise HTTPException(404, "Not ready")
    return FileResponse(f, media_type="video/mp4", filename="swapped.mp4")


@app.get("/")
def health():
    return {"ok": True}


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=int(os.getenv("PORT", 8000)))
