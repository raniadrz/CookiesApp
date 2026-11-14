#!/usr/bin/env python3
"""
Rebuild app/src/main/assets/seed/cookies.json from the Cookie Run: Kingdom Fandom wiki.

Imported per cookie (facts only): rarity, class, position, elements, skill name + cooldown,
recommended topping sets and recommended stat lines, Magic Candy / Crystal Jam availability,
the illustration URL (loaded at runtime by Coil; nothing is bundled) and the wiki page URL.

Not imported: skill descriptions and other prose (that is the game's own text). The app shows a short
neutral line and an "Open on wiki" button instead.

Builds for Arena / World Exploration / Guild Boss are derived from the wiki's recommended toppings and
stats with mode-specific thresholds — treat them as a starting point and refine by hand.

Usage:  python3 tools/import_wiki.py            (fetch from the wiki, apply overrides, bump seed version)
        python3 tools/import_wiki.py --offline  (re-apply tools/overrides.json to the current seed, no network)
Wiki text is CC BY-SA 3.0 — keep the attribution in the app and README.
"""
import json, re, sys, time, hashlib, colorsys, urllib.parse, urllib.request
from pathlib import Path

API = "https://cookierunkingdom.fandom.com/api.php"
WIKI = "https://cookierunkingdom.fandom.com/wiki/"
UA = "CrumbCodexSeedImporter/1.0 (open-source companion app seed builder)"
ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "app/src/main/assets/seed/cookies.json"
OVERRIDES = ROOT / "tools/overrides.json"

# Cookies whose rarity categories are not exposed as plain categories on the wiki.
FIXED_TITLES = [
    "Shadow Milk Cookie", "Eternal Sugar Cookie", "Burning Spice Cookie", "Mystic Flour Cookie", "Silent Salt Cookie",
    "Pure Vanilla Cookie", "Hollyberry Cookie", "Dark Cacao Cookie", "Golden Cheese Cookie", "White Lily Cookie",
]
CATEGORIES = ["Legendary_Cookies", "Dragon_Cookies", "Super_Epic_Cookies", "Epic_Cookies"]

RARITY = {"beast": "BEAST", "ancient": "ANCIENT", "legendary": "LEGENDARY", "dragon": "DRAGON",
          "super epic": "SUPER_EPIC", "superepic": "SUPER_EPIC", "epic": "EPIC", "rare": "RARE",
          "common": "COMMON", "special": "SPECIAL"}
CLASSES = {"bomber": "BOMBER", "charge": "CHARGE", "ambush": "AMBUSH", "magic": "MAGIC", "ranged": "RANGED",
           "defense": "DEFENSE", "support": "SUPPORT", "healing": "HEALING"}
POSITIONS = {"front": "FRONT", "middle": "MIDDLE", "rear": "REAR"}
ELEMENTS = {"poison": "POISON", "steel": "STEEL", "fire": "FIRE", "water": "WATER", "earth": "EARTH",
            "light": "LIGHT", "darkness": "DARKNESS", "dark": "DARKNESS", "ice": "ICE",
            "electricity": "ELECTRICITY", "electric": "ELECTRICITY", "wind": "WIND"}
TOPPINGS = {"swiftchocolate": "SWIFT_CHOCOLATE", "chocolate": "SWIFT_CHOCOLATE", "raspberry": "SEARING_RASPBERRY",
            "almond": "SOLID_ALMOND", "applejelly": "JUICY_APPLE_JELLY", "caramel": "BOUNCY_CARAMEL",
            "peanut": "HEALTHY_PEANUT", "walnut": "HARD_WALNUT"}
STATS = {"atk": "ATK", "atk spd": "ATK_SPD", "crit%": "CRIT", "crit": "CRIT", "cooldown": "COOLDOWN",
         "dmg resist": "DMG_RESIST", "hp": "HP", "def": "DEF", "debuff resist": "DEBUFF_RESIST",
         "amplify buff": "AMPLIFY_BUFF", "crit resist": "CRIT_RESIST", "dmg resist bypass": "DMG_RESIST_BYPASS",
         "healing": "HEALING"}
TART = {"SWIFT_CHOCOLATE": ("Chocolate Tart", "COOLDOWN"), "SEARING_RASPBERRY": ("Raspberry Tart", "ATK"),
        "SOLID_ALMOND": ("Almond Tart", "DMG_RESIST"), "JUICY_APPLE_JELLY": ("Apple Jelly Tart", "CRIT"),
        "BOUNCY_CARAMEL": ("Caramel Tart", "ATK_SPD"), "HEALTHY_PEANUT": ("Peanut Tart", "HP"),
        "HARD_WALNUT": ("Walnut Tart", "DEF")}
# (min, max) targets per stat and mode. None max = "or more".
THRESHOLDS = {
    "ARENA": {"COOLDOWN": (25, 29.5), "DMG_RESIST": (20, None), "ATK": (12, None), "CRIT": (15, None),
              "DMG_RESIST_BYPASS": (8, None), "ATK_SPD": (15, None), "HP": (10, None), "DEF": (10, None),
              "DEBUFF_RESIST": (10, None), "AMPLIFY_BUFF": (10, None), "ELEMENTAL_DMG": (10, None)},
    "WORLD_EXPLORATION": {"COOLDOWN": (15, None), "DMG_RESIST": (15, None), "ATK": (25, None), "CRIT": (15, None),
              "DMG_RESIST_BYPASS": (5, None), "ATK_SPD": (15, None), "HP": (10, None), "DEF": (10, None),
              "DEBUFF_RESIST": (5, None), "AMPLIFY_BUFF": (8, None), "ELEMENTAL_DMG": (10, None)},
    "GUILD_BOSS": {"COOLDOWN": (20, 25), "DMG_RESIST": (10, None), "ATK": (20, None), "CRIT": (20, None),
              "DMG_RESIST_BYPASS": (10, None), "ATK_SPD": (20, None), "HP": (8, None), "DEF": (8, None),
              "DEBUFF_RESIST": (5, None), "AMPLIFY_BUFF": (12, None), "ELEMENTAL_DMG": (15, None)},
}
DAMAGE = {"BOMBER", "CHARGE", "AMBUSH", "MAGIC", "RANGED"}


def api(params):
    params = {**params, "format": "json"}
    url = API + "?" + urllib.parse.urlencode(params)
    for attempt in range(5):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": UA})
            with urllib.request.urlopen(req, timeout=30) as r:
                return json.load(r)
        except Exception as e:  # network hiccup / throttling
            wait = 2 ** attempt
            print(f"  retry in {wait}s: {e}", file=sys.stderr)
            time.sleep(wait)
    raise RuntimeError("wiki API unreachable")


def category_titles(cat):
    out, cont = [], {}
    while True:
        d = api({"action": "query", "list": "categorymembers", "cmtitle": f"Category:{cat}",
                 "cmlimit": "500", "cmnamespace": "0", **cont})
        out += [m["title"] for m in d["query"]["categorymembers"]]
        if "continue" not in d:
            return out
        cont = d["continue"]


def fetch_pages(titles):
    pages = {}
    for i in range(0, len(titles), 40):
        batch = titles[i:i + 40]
        d = api({"action": "query", "titles": "|".join(batch), "prop": "revisions|pageimages",
                 "rvprop": "content", "rvslots": "main", "piprop": "original", "redirects": "1"})
        for p in d["query"]["pages"].values():
            if "revisions" in p:
                pages[p["title"]] = p
        time.sleep(1)  # be polite
    return pages


def field(text, name):
    m = re.search(r"^\|\s*" + name + r"\s*=\s*(.*)$", text, re.M | re.I)
    if not m:
        return ""
    v = re.sub(r"<[^>]+>", "", m.group(1))
    v = re.sub(r"\[\[(?:[^|\]]*\|)?([^\]]*)\]\]", r"\1", v)
    return v.replace("{{!}}", "|").strip()


def skill(text):
    m = re.search(r"\{\{Skill box\s*\|(.*?)\|\s*Description", text, re.S | re.I)
    if not m:
        return None, None
    body = m.group(1)
    name = re.search(r"Name\s*=\s*([^|\n}]+)", body)
    cd = re.search(r"Cooldown\s*=\s*(\d+)", body)
    clean = lambda s: re.sub(r"\[\[(?:[^|\]]*\|)?([^\]]*)\]\]", r"\1", s).strip()
    return (clean(name.group(1)) if name else None), (int(cd.group(1)) if cd else None)


def slug(name):
    s = re.sub(r"\s+cookie$", "", name.strip(), flags=re.I)
    s = s.encode("ascii", "ignore").decode() or name
    return re.sub(r"[^a-z0-9]+", "_", s.lower()).strip("_")


def palette(name):
    h = int(hashlib.md5(name.encode()).hexdigest(), 16)
    hue = (h % 360) / 360
    a = colorsys.hls_to_rgb(hue, 0.72, 0.65)
    b = colorsys.hls_to_rgb((hue + 0.12) % 1, 0.6, 0.55)
    return ["#%02X%02X%02X" % tuple(int(c * 255) for c in a), "#%02X%02X%02X" % tuple(int(c * 255) for c in b)]


def image_url(page):
    src = page.get("original", {}).get("source")
    if not src:
        return None
    # Ask Fandom's CDN for a ~400px rendition instead of the full illustration.
    base, _, query = src.partition("?")
    if "/revision/latest" in base and "scale-to-width" not in base:
        base = base + "/scale-to-width-down/400"
    return base + ("?" + query if query else "")


def parse_stats(raw, elements):
    stats, beascuit_element = [], None
    for part in re.split(r"[,|]", raw):
        p = part.strip().lower()
        if not p:
            continue
        if p.endswith(" dmg") and p[:-4] in ELEMENTS:
            stats.append("ELEMENTAL_DMG"); beascuit_element = ELEMENTS[p[:-4]]
        elif p in STATS:
            stats.append(STATS[p])
        elif p in ELEMENTS:
            beascuit_element = ELEMENTS[p]
    seen = []
    [seen.append(s) for s in stats if s not in seen]
    return seen, beascuit_element or (elements[0] if elements else None)


def builds_for(cls, toppings, stats, beascuit_el):
    if not toppings:
        toppings = ["SWIFT_CHOCOLATE"] if cls not in DAMAGE else ["SEARING_RASPBERRY", "SWIFT_CHOCOLATE"]
    if not stats:
        stats = ["COOLDOWN", "DMG_RESIST"] if cls not in DAMAGE else ["ATK", "COOLDOWN", "CRIT"]
    main = toppings[0]
    dmg = cls in DAMAGE

    def pick_pve():
        if dmg and "SEARING_RASPBERRY" in toppings: return [("SEARING_RASPBERRY", 5)]
        return [(main, 5)]

    def pick_guild():
        if dmg and "SEARING_RASPBERRY" in toppings and "SWIFT_CHOCOLATE" in toppings:
            return [("SEARING_RASPBERRY", 3), ("SWIFT_CHOCOLATE", 2)]
        if dmg and "SEARING_RASPBERRY" in toppings: return [("SEARING_RASPBERRY", 5)]
        return [(main, 5)]

    def order(mode):
        s = list(stats)
        if mode == "WORLD_EXPLORATION" and dmg and "ATK" in s:
            s.remove("ATK"); s.insert(0, "ATK")
        if mode == "GUILD_BOSS" and dmg:
            for st in ("CRIT", "ATK"):
                if st in s: s.remove(st)
                s.insert(0, st)
        return s[:4]

    def subs(mode):
        out = []
        for i, st in enumerate(order(mode)):
            lo, hi = THRESHOLDS[mode].get(st, (5, None))
            d = {"stat": st, "min": lo, "priority": i + 1}
            if hi is not None: d["max"] = hi
            out.append(d)
        return out

    def build(mode, title, summary, tops):
        t, s = TART[tops[0][0]]
        return {"mode": mode, "title": title, "summary": summary,
                "toppings": [{"type": a, "count": c} for a, c in tops],
                "tart": {"name": t, "stat": s},
                "substats": subs(mode),
                "beascuit": {"grade": "LEGENDARY", "element": beascuit_el,
                             "attunements": order(mode)[:3]},
                "notes": ["Topping sets and stat lines follow the community wiki's recommendation; thresholds are general targets — adjust for your account."]}

    alt = " / ".join(TOPPINGS_LABEL[t] for t in toppings)
    return [
        build("ARENA", f"Arena: 5× {TOPPINGS_LABEL[main]}", f"Wiki-recommended sets: {alt}.", [(main, 5)]),
        build("WORLD_EXPLORATION", "World Exploration", "Shorter fights — lean into damage where the kit allows." if dmg
              else "Comfortable sustain for farming stages.", pick_pve()),
        build("GUILD_BOSS", "Guild Boss", "Long single-target fight: ATK/CRIT with enough cooldown for repeated casts." if dmg
              else "Maximise skill uptime to keep buffs/heals rolling.", pick_guild()),
    ]


TOPPINGS_LABEL = {"SWIFT_CHOCOLATE": "Swift Chocolate", "SEARING_RASPBERRY": "Searing Raspberry",
                  "SOLID_ALMOND": "Solid Almond", "JUICY_APPLE_JELLY": "Juicy Apple Jelly",
                  "BOUNCY_CARAMEL": "Bouncy Caramel", "HEALTHY_PEANUT": "Healthy Peanut", "HARD_WALNUT": "Hard Walnut"}


def candy(text, rarity):
    has_jam = re.search(r"crystal jam", text, re.I) is not None
    has_candy = re.search(r"magic candy", text, re.I) is not None
    if has_jam and rarity in ("BEAST", "ANCIENT", "LEGENDARY", "DRAGON"):
        kind = "CRYSTAL_JAM"
    elif has_candy:
        kind = "MAGIC_CANDY"
    else:
        return {"kind": "NONE", "priority": "SKIP", "summary": "", "effects": [], "ingredients": []}
    prio = "HIGH" if rarity in ("BEAST", "ANCIENT", "LEGENDARY", "DRAGON") else "MEDIUM"
    return {"kind": kind, "priority": prio,
            "summary": f"This cookie has {kind.replace('_', ' ').title()} upgrades. See the wiki page for level-by-level effects.",
            "effects": [],
            "ingredients": (["Crystal Jam ingredients", "Soulstones", "Coins"] if kind == "CRYSTAL_JAM"
                            else ["Magic Candy ingredients", "Cookie-specific candy fragments", "Coins"])}


def convert(title, page):
    text = page["revisions"][0]["slots"]["main"]["*"]
    if "{{Cookie infobox" not in text:
        return None
    rarity = RARITY.get(field(text, "rarity").lower().strip())
    cls = CLASSES.get(field(text, "role").lower().strip())
    pos = POSITIONS.get(field(text, "position").lower().strip())
    if not (rarity and cls and pos):
        print(f"  skip {title}: rarity={field(text,'rarity')!r} role={field(text,'role')!r} pos={field(text,'position')!r}", file=sys.stderr)
        return None
    elements = []
    for e in re.split(r"[,/&]| and ", field(text, "elements")):
        e = ELEMENTS.get(e.strip().lower())
        if e and e not in elements: elements.append(e)
    toppings = []
    for t in field(text, "toppings").split(","):
        key = t.strip().split(" ")[0].lower() if t.strip() else ""
        if key in TOPPINGS and TOPPINGS[key] not in toppings: toppings.append(TOPPINGS[key])
    stats, b_el = parse_stats(field(text, "stats"), elements)
    sk_name, sk_cd = skill(text)
    name = title
    sk_name = sk_name or f"{name.replace(' Cookie', '')}'s skill"
    cid = slug(name)
    return {
        "id": cid, "name": name,
        "title": f"{rarity.replace('_', ' ').title()} {cls.title()} · {pos.title()}",
        "rarity": rarity, "class": cls, "position": pos, "elements": elements,
        "palette": palette(name),
        "imageUrl": image_url(page),
        "wikiUrl": WIKI + urllib.parse.quote(title.replace(" ", "_")),
        "skill": {"name": sk_name, "cooldown": sk_cd,
                  "description": f"{cls.title()}-class skill" + (f" ({', '.join(e.title() for e in elements)} element)" if elements else "")
                                 + ". Open the wiki page for the full in-game description and level scaling."},
        "magicCandy": candy(text, rarity),
        "builds": builds_for(cls, toppings, stats, b_el),
    }


def apply_overrides(cookies):
    if not OVERRIDES.exists():
        return cookies
    overrides = {k: v for k, v in json.loads(OVERRIDES.read_text()).items() if not k.startswith("_")}
    by_id = {c["id"]: c for c in cookies}
    for cid, ov in overrides.items():
        c = by_id.get(cid)
        if c is None:
            print(f"  override for unknown cookie '{cid}' ignored", file=sys.stderr); continue
        for key, value in ov.items():
            if key == "builds":
                modes = {b["mode"]: b for b in c.get("builds", [])}
                for b in value:
                    modes[b["mode"]] = b
                order = ["ARENA", "WORLD_EXPLORATION", "GUILD_BOSS"]
                c["builds"] = sorted(modes.values(), key=lambda b: order.index(b["mode"]))
            else:
                c[key] = value
        print(f"  applied overrides to {cid}")
    return cookies


def write(cookies):
    """Writes the seed only when the cookie data actually changed, so the version (and the app's
    update check) moves only on real changes. Prints `changed=true|false` for CI."""
    existing = json.loads(OUT.read_text()) if OUT.exists() else {"version": 0, "cookies": None}
    if existing.get("cookies") == cookies:
        print(f"No changes ({len(cookies)} cookies, version {existing['version']} kept)")
        print("changed=false")
        return False
    version = existing["version"] + 1
    OUT.write_text(json.dumps({"version": version, "source": "Cookie Run: Kingdom Wiki (Fandom), CC BY-SA 3.0",
                               "cookies": cookies}, indent=2, ensure_ascii=False) + "\n")
    print(f"Wrote {len(cookies)} cookies to {OUT.relative_to(ROOT)} (version {version})")
    print("changed=true")
    return True


def main():
    if "--offline" in sys.argv:
        write(apply_overrides(json.loads(OUT.read_text())["cookies"]))
        return
    titles = list(FIXED_TITLES)
    for c in CATEGORIES:
        titles += [t for t in category_titles(c) if t not in titles]
    print(f"Fetching {len(titles)} pages…")
    pages = fetch_pages(titles)
    cookies, seen = [], set()
    for t in titles:
        p = pages.get(t)
        if not p:
            print(f"  missing page {t}", file=sys.stderr); continue
        c = convert(p["title"], p)
        if c and c["id"] not in seen:
            seen.add(c["id"]); cookies.append(c)
    write(apply_overrides(cookies))


if __name__ == "__main__":
    main()
