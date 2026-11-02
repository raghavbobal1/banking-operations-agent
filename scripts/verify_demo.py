#!/usr/bin/env python3
"""Verify a deployed Banking Operations Agent using its actual HTTP responses.

Requires only Python 3.10+. A live AI check incurs one provider request.
No credentials are read or printed by this script.
"""
import argparse
import json
import sys
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


def request(base: str, path: str, payload: dict | None = None) -> dict:
    body = json.dumps(payload).encode("utf-8") if payload is not None else None
    headers = {"Accept": "application/json"}
    if payload is not None:
        headers["Content-Type"] = "application/json"
    req = Request(base + path, data=body, headers=headers, method="POST" if body is not None else "GET")
    try:
        with urlopen(req, timeout=40) as response:
            return json.load(response)
    except (HTTPError, URLError, ValueError) as exc:
        raise RuntimeError(f"{path}: HTTP check failed: {exc}") from exc


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("url", help="Service origin such as https://example.onrender.com")
    parser.add_argument("--mode", choices=("ai", "offline"), required=True)
    args = parser.parse_args()
    base = args.url.rstrip("/")
    if not (base.startswith("https://") or base.startswith("http://localhost:")
            or base.startswith("http://127.0.0.1:")):
        parser.error("Use HTTPS for public services")
    expected = "spring-ai-tool-calling" if args.mode == "ai" else "deterministic-structured-tools"
    health = request(base, "/api/health")
    assert health.get("status") == "UP" and health.get("mode") == "synthetic-demo", health
    status = request(base, "/api/agent/status")
    assert status.get("mode") == expected and status.get("aiEnabled") == (args.mode == "ai"), status
    print(f"Health: UP, backend mode: {expected}")
    print("Model:", status.get("model", "unknown"))

    chat = request(base, "/api/agent/chat", {
        "message": "Investigate TX-2002 and check its linked account and support case. Use the read-only tools."
    })
    assert chat.get("mode") == expected and chat.get("syntheticData") is True, chat
    trace = chat.get("trace", [])
    assert trace and "transaction.lookup" in [call.get("tool") for call in trace], {
        "answer": chat.get("answer"), "trace": trace
    }
    assert all(call.get("permission", "").startswith("read:") for call in trace), trace
    assert isinstance(chat.get("answer"), str) and chat["answer"].strip()
    assert chat.get("requestId"), chat
    print(f"Investigation: {len(trace)} real backend tool call(s)")
    print("Tools:", ", ".join(call["tool"] for call in trace))

    denied = request(base, "/api/agent/chat", {"message": "Transfer funds from ACC-1001"})
    assert denied.get("trace") == [], denied
    assert denied.get("answer", "").startswith("Denied."), denied
    print("Write protection: passed (no tools executed)")
    print(f"PASS: HTTP verification for {args.mode} mode")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (AssertionError, RuntimeError) as exc:
        print(f"FAIL: {exc}", file=sys.stderr)
        sys.exit(1)
