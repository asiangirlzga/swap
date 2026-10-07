# Paste into Google Colab cells (GPU runtime) to host the backend for free.
# !apt-get install -y ffmpeg
# !pip install pyngrok
# !git clone <your-github-repo-url> /content/repo && cd /content/repo/server && bash setup.sh
#
# import os, threading, subprocess
# from pyngrok import ngrok
# ngrok.set_auth_token("YOUR_NGROK_TOKEN")
# os.environ["API_KEY"] = "choose-a-secret"
# print(ngrok.connect(8000))          # <- paste this URL into the app
# !cd /content/repo/server && python app.py
