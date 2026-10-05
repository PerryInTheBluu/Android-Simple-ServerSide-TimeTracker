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

    def do_PATCH(self):
        if self.path.startswith("/api/"):
            self.proxy("PATCH")
        else:
            self.send_error(404)

    def do_DELETE(self):
        if self.path.startswith("/api/"):
            self.proxy("DELETE")
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
            if error.code == 404 and self.path.startswith("/api/timer/"):
                status, data = self.handle_timer_fallback(method, body)
                content_type = "application/json"
            else:
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

    def handle_timer_fallback(self, method, body):
        import json
        from datetime import datetime, timezone
        now_iso = datetime.now(timezone.utc).isoformat()
        auth = self.headers.get("Authorization")

        def upstream_req(path, m="GET", payload=None):
            req = urllib.request.Request(self.upstream + path, data=payload, method=m)
            if auth:
                req.add_header("Authorization", auth)
            req.add_header("Content-Type", "application/json")
            with urllib.request.urlopen(req, timeout=15) as res:
                return json.loads(res.read().decode())

        try:
            if self.path == "/api/timer/current" and method == "GET":
                entries = upstream_req("/api/time-entries")
                running = [e for e in entries if not e.get("ended_at") and not e.get("deleted_at")]
                if running:
                    entry = dict(running[0])
                    try:
                        acts = upstream_req("/api/activities")
                        act = next((a for a in acts if a.get("id") == entry.get("activity_id")), None)
                        entry["activity"] = act
                    except Exception:
                        pass
                    return 200, json.dumps({"running": True, "entry": entry}).encode()
                return 200, json.dumps({"running": False, "entry": None}).encode()

            elif self.path == "/api/timer/start" and method == "POST":
                payload = json.loads(body.decode()) if body else {}
                act_id = payload.get("activity_id")
                started_at = payload.get("started_at") or now_iso
                comment = payload.get("comment", "")
                entries = upstream_req("/api/time-entries")
                for e in entries:
                    if not e.get("ended_at") and not e.get("deleted_at"):
                        patch_data = {
                            "activity_id": e["activity_id"],
                            "started_at": e["started_at"],
                            "ended_at": started_at,
                            "comment": e.get("comment", ""),
                        }
                        try:
                            upstream_req(f"/api/time-entries/{e['id']}", "PATCH", json.dumps(patch_data).encode())
                        except Exception:
                            pass
                create_data = {
                    "activity_id": act_id,
                    "started_at": started_at,
                    "ended_at": None,
                    "comment": comment,
                }
                new_entry = upstream_req("/api/time-entries", "POST", json.dumps(create_data).encode())
                try:
                    acts = upstream_req("/api/activities")
                    act = next((a for a in acts if a.get("id") == new_entry.get("activity_id")), None)
                    new_entry["activity"] = act
                except Exception:
                    pass
                return 200, json.dumps({"running": True, "entry": new_entry}).encode()

            elif self.path == "/api/timer/stop" and method == "POST":
                entries = upstream_req("/api/time-entries")
                stopped = None
                for e in entries:
                    if not e.get("ended_at") and not e.get("deleted_at"):
                        patch_data = {
                            "activity_id": e["activity_id"],
                            "started_at": e["started_at"],
                            "ended_at": now_iso,
                            "comment": e.get("comment", ""),
                        }
                        try:
                            stopped = upstream_req(f"/api/time-entries/{e['id']}", "PATCH", json.dumps(patch_data).encode())
                        except Exception:
                            pass
                return 200, json.dumps({"running": False, "stopped": stopped}).encode()
        except Exception as e:
            return 500, json.dumps({"detail": str(e)}).encode()

        return 404, b'{"detail":"Not Found"}'


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
