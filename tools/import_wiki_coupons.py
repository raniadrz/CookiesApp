#!/usr/bin/env python3
"""
Sync coupon codes from the "Active Codes & Promotions" table of the Cookie Run: Kingdom wiki
(https://cookierunkingdom.fandom.com/wiki/Coupon_Codes) into app/src/main/assets/seed/game_data.json.

- New codes are added with source "wiki" and verified=false (the community page has no expiry dates
  and is sometimes outdated).
- Wiki-sourced codes that disappear from the wiki's active table are removed (they have expired).
- Codes added by hand (tools/add_coupon.py or the "Add coupon code" workflow) are never touched.
- The file's version is bumped only when something changed. Prints `changed=true|false` for CI.
"""
import json, re, sys, time, urllib.parse, urllib.request
from pathlib import Path

API = "https://cookierunkingdom.fandom.com/api.php"
UA = "CrumbCodexSeedImporter/1.0 (open-source companion app seed builder)"
ROOT = Path(__file__).resolve().parent.parent
GAME = ROOT / "app/src/main/assets/seed/game_data.json"
CODE_RE = re.compile(r"^[A-Z0-9]{4,30}$")


def fetch_page():
    url = API + "?" + urllib.parse.urlencode({
        "action": "query", "titles": "Coupon_Codes", "prop": "revisions",
        "rvprop": "content", "rvslots": "main", "format": "json"})
    for attempt in range(5):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": UA}), timeout=30) as r:
                page = next(iter(json.load(r)["query"]["pages"].values()))
                return page["revisions"][0]["slots"]["main"]["*"]
        except Exception as e:
            print(f"  retry: {e}", file=sys.stderr)
            time.sleep(2 ** attempt)
    raise SystemExit("wiki unreachable")


def humanize(name):
    return re.sub(r"(?<=[a-z])(?=[A-Z])", " ", name)


def rewards_text(cell):
    """{{IconCrystal|amount=3000}}{{IconBeascuitDough|radiant=true|amount=50}} -> '3000 Crystal, 50 Radiant Beascuit Dough'"""
    parts = []
    for name, args in re.findall(r"\{\{Icon([A-Za-z]+)((?:\|[^}]*)?)\}\}", cell):
        params = dict(p.split("=", 1) for p in args.strip("|").split("|") if "=" in p)
        label = humanize(name)
        if params.get("radiant") == "true":
            label = "Radiant " + label
        if "type" in params:
            label = f"{params['type'].title()} {label}"
        if "rarity" in params:
            label += f" (Lv.{params['rarity']})"
        amount = params.get("amount", "").strip()
        parts.append(f"{amount} {label}".strip())
    return ", ".join(parts)


def parse_active(text):
    """Returns {CODE: rewards} from the wiki's active-codes table."""
    start = text.find("Active Codes")
    if start < 0:
        raise SystemExit("Couldn't find the 'Active Codes' section. Has the wiki page layout changed?")
    nxt = re.search(r"^==[^=]", text[start + 20:], re.M)
    section = text[start: start + 20 + nxt.start()] if nxt else text[start:]
    codes = {}
    for row in section.split("\n|-"):
        lines = [l for l in row.strip().split("\n") if l.strip()]
        header = next((l for l in lines if l.startswith("!")), None)
        if not header:
            continue
        raw = header.lstrip("!").split("|")[-1]
        code = re.sub(r"'''|''|<[^>]+>|\[\[|\]\]", "", raw).strip().upper()
        if not CODE_RE.match(code):
            continue  # promo links like "Studio Kingdom Website Gift" are not codes
        cells = [l for l in lines if l.startswith("|") and not l.startswith("|}")]
        codes[code] = rewards_text(cells[0]) if cells else ""
    return codes


def main():
    active = parse_active(fetch_page())
    if not active:
        raise SystemExit("Parsed 0 codes from the wiki, refusing to wipe existing wiki codes")
    data = json.loads(GAME.read_text())
    coupons = data.setdefault("coupons", [])
    before = json.dumps(coupons, sort_keys=True)
    known = {c["code"].upper(): c for c in coupons}

    added = [code for code in active if code not in known]
    for code in added:
        coupons.append({"code": code, "rewards": active[code], "expires": None, "verified": False, "source": "wiki"})
    # Refresh rewards text of wiki codes, drop wiki codes no longer listed as active.
    removed = [c["code"] for c in coupons if c.get("source") == "wiki" and c["code"].upper() not in active]
    data["coupons"] = [c for c in coupons if c["code"] not in removed]
    for c in data["coupons"]:
        if c.get("source") == "wiki" and active.get(c["code"]) and c.get("rewards") != active[c["code"]]:
            c["rewards"] = active[c["code"]]

    print(f"Wiki lists {len(active)} active code(s); added {len(added)}, removed {len(removed)}")
    for code in added:
        print(f"  + {code}: {active[code]}")
    for code in removed:
        print(f"  - {code}")
    if json.dumps(data["coupons"], sort_keys=True) == before:
        print("changed=false")
        return
    data["version"] = int(data.get("version", 0)) + 1
    GAME.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n")
    print(f"game_data.json is now version {data['version']}")
    print("changed=true")


if __name__ == "__main__":
    main()
