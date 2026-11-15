#!/usr/bin/env python3
"""
Add, update or remove a coupon code in app/src/main/assets/seed/game_data.json and bump its version,
so every installed app picks it up on its next data check (and notifies about new codes).

  python3 tools/add_coupon.py ADD   CODE --rewards "300 Crystals" --expires 2026-12-31
  python3 tools/add_coupon.py REMOVE CODE

Coupons that expired more than 30 days ago are pruned automatically.
"""
import argparse, datetime as dt, json, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
GAME = ROOT / "app/src/main/assets/seed/game_data.json"
CODE_RE = re.compile(r"^[A-Z0-9]{4,30}$")
PRUNE_AFTER_DAYS = 30


def fail(msg):
    print(f"error: {msg}", file=sys.stderr)
    sys.exit(1)


def main():
    p = argparse.ArgumentParser()
    p.add_argument("action", type=str.upper, choices=["ADD", "REMOVE"])
    p.add_argument("code")
    p.add_argument("--rewards", default="")
    p.add_argument("--expires", default="", help="YYYY-MM-DD (last valid day), empty = unknown")
    p.add_argument("--unverified", action="store_true", help="mark as not yet confirmed working")
    a = p.parse_args()

    code = a.code.strip().upper().replace(" ", "")
    if not CODE_RE.match(code):
        fail(f"'{a.code}' doesn't look like a coupon code (4-30 letters/digits)")
    expires = a.expires.strip() or None
    if expires:
        try:
            dt.date.fromisoformat(expires)
        except ValueError:
            fail(f"expiry '{expires}' must be YYYY-MM-DD")

    data = json.loads(GAME.read_text())
    coupons = data.setdefault("coupons", [])
    before = json.dumps(coupons, sort_keys=True)
    existing = next((c for c in coupons if c["code"].upper() == code), None)

    if a.action == "ADD":
        entry = {"code": code, "rewards": a.rewards.strip(), "expires": expires, "verified": not a.unverified}
        if existing:
            existing.update({k: v for k, v in entry.items() if v not in ("", None) or k == "verified"})
            print(f"Updated {code}")
        else:
            coupons.append(entry)
            print(f"Added {code}")
    else:
        if not existing:
            fail(f"{code} is not in the list")
        coupons.remove(existing)
        print(f"Removed {code}")

    cutoff = dt.date.today() - dt.timedelta(days=PRUNE_AFTER_DAYS)
    pruned = [c["code"] for c in coupons if c.get("expires") and dt.date.fromisoformat(c["expires"]) < cutoff]
    if pruned:
        data["coupons"] = [c for c in coupons if c["code"] not in pruned]
        print(f"Pruned expired: {', '.join(pruned)}")

    if json.dumps(data["coupons"], sort_keys=True) == before:
        print("No change")
        return
    data["version"] = int(data.get("version", 0)) + 1
    GAME.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n")
    print(f"game_data.json is now version {data['version']} with {len(data['coupons'])} coupon(s)")


if __name__ == "__main__":
    main()
