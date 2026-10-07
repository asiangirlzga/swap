#!/usr/bin/env bash
# One-time server setup (same steps as your roop.ipynb). Needs ffmpeg + python3.10/3.11
set -e
cd "$(dirname "$0")"

[ -d roop ] || git clone https://github.com/s0md3v/roop.git
pip install -r roop/requirements.txt
pip install -r requirements.txt

mkdir -p roop/models
if [ ! -f roop/models/inswapper_128.onnx ]; then
  wget https://huggingface.co/ezioruan/inswapper_128.onnx/resolve/main/inswapper_128.onnx \
       -O roop/models/inswapper_128.onnx
fi
echo "Setup complete. Run: python app.py"
