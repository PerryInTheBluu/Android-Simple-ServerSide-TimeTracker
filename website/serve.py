#!/usr/bin/env python3
"""Local launcher for the analysis website.

Serves the static analysis page and proxies /api requests to the time
tracker server, so the browser page talks to the same origin and no
CORS configuration on the server is needed. Standard library only.

Usage:
    python3 serve.py [--port 8765] [--server https://time.example.com]

Then open http://127.0.0.1:8765 and paste an API token.
"""
import argparse
import os
import urllib.error
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

WEBSITE_DIR = os.path.dirname(os.path.abspath(__file__))


class Handler(BaseHTTPRequestHandler):
    upstream = "https://time.ts.piusdischinger.com"

    def log_message(self, fmt, *args):  # quieter console
        pass

    def do_GET(self):
        if self.path.startswith("/api/"):
            self.proxy("GET")
        else:
            self.serve_file(self.path)

    def do_POST(self):
        if self.path.startswith("/api/"):
            self.proxy("POST")
        else:
            self.send_error(404)

    def serve_file(self, path):
        if path in ("/", "/index.html"):
            name = "index.html"
        else:
            # Only flat files from the website directory, no traversal.
            name = os.path.basename(path)
        file_path = os.path.join(WEBSITE_DIR, name)
        if not os.path.isfile(file_path):
            self.send_error(404)
            return
        with open(file_path, "rb") as f:
            body = f.read()
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8" if name.endswith(".html") else "application/octet-stream")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def proxy(self, method):
        length = int(self.headers.get("Content-Length") or 0)
        body = self.rfile.read(length) if length else None
        request = urllib.request.Request(self.upstream + self.path, data=body, method=method)
        for header in ("Authorization", "Content-Type", "Accept"):
            value = self.headers.get(header)
            if value:
                request.add_header(header, value)
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                data = response.read()
                status = response.status
                content_type = response.headers.get("Content-Type", "application/json")
        except urllib.error.HTTPError as error:
            data = error.read()
            status = error.code
            content_type = error.headers.get("Content-Type", "application/json")
        except Exception as error:  # server unreachable
            message = str(error).encode()
            self.send_response(502)
            self.send_header("Content-Type", "text/plain; charset=utf-8")
            self.send_header("Content-Length", str(len(message)))
            self.end_headers()
            self.wfile.write(message)
            return
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--port", type=int, default=8765)
    parser.add_argument(
        "--server",
        default=os.environ.get("TT_SERVER", "https://time.ts.piusdischinger.com"),
        help="Upstream time tracker server url",
    )
    args = parser.parse_args()
    Handler.upstream = args.server.rstrip("/")
    print(f"Analysis website on http://127.0.0.1:{args.port} (server: {Handler.upstream})")
    ThreadingHTTPServer(("127.0.0.1", args.port), Handler).serve_forever()


if __name__ == "__main__":
    main()
