#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
FriendSim 声音克隆桥接（在电脑上跑，手机通过局域网访问）

链路：手机 --multipart--> 本脚本 --JSON(api_v2)--> GPT-SoVITS --wav--> 手机

用法：
  1) 先启动 GPT-SoVITS 的 api_v2 服务（默认 http://127.0.0.1:9880）
  2) python friendsim_bridge.py --listen 0.0.0.0 --port 9880
  3) 手机设置里把「克隆引擎地址」填成 http://<电脑局域网 IP>:9880

协议（与 App 内 VoiceEngine 一致）：
  POST /tts  multipart/form-data
    text         要朗读的文本
    prompt_text  参考音频对应的原文（可空）
    ref_audio    参考音频文件（mp3/wav/m4a）
    text_lang    语言，默认 zh
    prompt_lang  参考音频语言，默认 zh
  ← 200 audio/wav  失败 4xx/5xx + 纯文本原因
"""

import argparse
import json
import os
import tempfile
import urllib.request
from email.parser import BytesParser
from email.policy import default
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

UPSTREAM = "http://127.0.0.1:9880"


def parse_multipart(body: bytes, content_type: str):
    msg = BytesParser(policy=default).parsebytes(
        b"Content-Type: " + content_type.encode() + b"\r\n\r\n" + body
    )
    fields, files = {}, {}
    for part in msg.iter_parts():
        name = part.get_param("name", header="content-disposition")
        filename = part.get_param("filename", header="content-disposition")
        payload = part.get_payload(decode=True) or b""
        if filename:
            files[name] = (filename, payload)
        else:
            fields[name] = payload.decode("utf-8", "replace")
    return fields, files


def tts(text, ref_path, prompt_text, text_lang, prompt_lang) -> bytes:
    payload = {
        "text": text,
        "text_lang": text_lang,
        "ref_audio_path": ref_path,
        "prompt_text": prompt_text,
        "prompt_lang": prompt_lang,
        "text_split_method": "cut5",
        "media_type": "wav",
        "streaming_mode": False,
    }
    req = urllib.request.Request(
        UPSTREAM + "/tts",
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(req, timeout=120) as resp:
        return resp.read()


class Handler(BaseHTTPRequestHandler):
    server_version = "FriendSimBridge/1.0"

    def log_message(self, fmt, *args):
        print("[bridge]", fmt % args, flush=True)

    def _reply(self, code, body: bytes, ctype="text/plain; charset=utf-8"):
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if self.path.startswith("/health"):
            self._reply(200, b'{"ok":true}')
        else:
            self._reply(404, b"not found")

    def do_POST(self):
        if not self.path.startswith("/tts"):
            self._reply(404, b"not found")
            return
        length = int(self.headers.get("Content-Length", 0))
        raw = self.rfile.read(length)
        fields, files = parse_multipart(raw, self.headers.get("Content-Type", ""))
        text = (fields.get("text") or "").strip()
        if not text or "ref_audio" not in files:
            self._reply(400, b"need text and ref_audio")
            return
        filename, payload = files["ref_audio"]
        ext = os.path.splitext(filename)[1] or ".wav"
        with tempfile.NamedTemporaryFile(delete=False, suffix=ext) as tmp:
            tmp.write(payload)
            ref_path = tmp.name
        try:
            audio = tts(
                text,
                ref_path,
                fields.get("prompt_text", ""),
                fields.get("text_lang") or "zh",
                fields.get("prompt_lang") or "zh",
            )
            self._reply(200, audio, "audio/wav")
        except Exception as exc:  # noqa: BLE001
            self._reply(502, ("upstream error: %s" % exc).encode("utf-8"))
        finally:
            try:
                os.unlink(ref_path)
            except OSError:
                pass


def main():
    global UPSTREAM
    ap = argparse.ArgumentParser()
    ap.add_argument("--listen", default="0.0.0.0")
    ap.add_argument("--port", type=int, default=9880)
    ap.add_argument("--upstream", default=UPSTREAM, help="GPT-SoVITS api_v2 地址")
    args = ap.parse_args()
    UPSTREAM = args.upstream.rstrip("/")
    print("FriendSim bridge on http://%s:%d  ->  %s" % (args.listen, args.port, UPSTREAM))
    ThreadingHTTPServer((args.listen, args.port), Handler).serve_forever()


if __name__ == "__main__":
    main()
