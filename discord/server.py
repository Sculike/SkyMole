"""Builds the SkyMole Discord server: roles, categories, channels and pinned posts.

Run it yourself with your own bot token. The token is read from the environment
and never written anywhere, so it stays on your machine.

    set DISCORD_TOKEN=...        (Windows, cmd)
    set DISCORD_GUILD=...
    python server.py

Safe to run twice: anything already there by name is left alone rather than
duplicated, so you can edit this file and run it again.
"""
import json
import os
import sys
import time
import urllib.error
import urllib.request

API = "https://discord.com/api/v10"
TOKEN = os.environ.get("DISCORD_TOKEN", "").strip()
GUILD = os.environ.get("DISCORD_GUILD", "").strip()

GOLD = 0xFFC93C
PINK = 0xF29BB0
CYAN = 0x4FC9DE
GREY = 0x9B8C7A

# Discord permission bits, named so the tables below stay readable.
VIEW = 1 << 10
SEND = 1 << 11
REACT = 1 << 6
HISTORY = 1 << 16
EMBED = 1 << 14
ATTACH = 1 << 15
THREADS = 1 << 38
MANAGE_MESSAGES = 1 << 13
KICK = 1 << 1
BAN = 1 << 2
MODERATE = 1 << 40
ADMIN = 1 << 3

TEXT, VOICE, CATEGORY = 0, 2, 4

# name, colour, permissions, shown separately in the member list
ROLES = [
    ("Developer", GOLD, ADMIN, True),
    ("Moderator", PINK, KICK | BAN | MODERATE | MANAGE_MESSAGES, True),
    ("Contributor", CYAN, 0, True),
    ("Member", GREY, 0, False),
]

# Everyone can read these; only staff can post.
READ_ONLY = {"welcome", "rules", "announcements", "changelog", "links"}
# Nobody outside staff can even see these.
STAFF_ONLY = {"staff-chat", "mod-log"}

STRUCTURE = [
    ("INFORMATION", [
        ("welcome", TEXT, "Start here."),
        ("rules", TEXT, "Read before posting."),
        ("announcements", TEXT, "Releases and news."),
        ("changelog", TEXT, "What changed in each version."),
        ("links", TEXT, "Modrinth, GitHub, and where else to find SkyMole."),
    ]),
    ("SUPPORT", [
        ("help", TEXT, "Something not working? Ask here, with your Minecraft and mod version."),
        ("bug-reports", TEXT, "Run /skymole debug first and attach the file it writes."),
        ("feature-requests", TEXT, "Ideas for the mod. Search first, someone may have asked."),
    ]),
    ("COMMUNITY", [
        ("general", TEXT, "Talk about anything SkyBlock."),
        ("big-drops", TEXT, "Post your lucky drops."),
        ("media", TEXT, "Screenshots and clips."),
        ("off-topic", TEXT, "Everything else."),
    ]),
    ("VOICE", [
        ("General", VOICE, None),
        ("Mining", VOICE, None),
    ]),
    ("STAFF", [
        ("staff-chat", TEXT, "Private."),
        ("mod-log", TEXT, "Moderation records."),
    ]),
]

RULES = """**SkyMole - rules**

**1. Be decent.** No harassment, slurs, or personal attacks.
**2. Keep it on topic.** Every channel has a topic line saying what it is for.
**3. No spam or advertising.** Sharing your own clips in media is fine; promoting your server is not.
**4. No piracy or cheat clients.** SkyMole follows Hypixel's rules and so does this server.
**5. Support belongs in help.** Include your Minecraft version, mod version, and what you expected to happen.
**6. Staff decisions are final.** Disagree in DMs, not in public.

You get a warning first, unless it is serious enough not to."""

WELCOME = """**Welcome to SkyMole.**

SkyMole is a mining helper for Hypixel SkyBlock: commissions, powder, events, solvers and trackers.

**Get the mod** - see links.
**Something broken?** - bug-reports. Run `/skymole debug` first; it writes a file to your config folder saying exactly what the mod could see.
**Want something added?** - feature-requests.

Built by sculike."""


def call(method, path, body=None):
    """One API call, retried when Discord asks us to slow down."""
    data = json.dumps(body).encode() if body is not None else None
    request = urllib.request.Request(API + path, data=data, method=method, headers={
        "Authorization": "Bot " + TOKEN,
        "Content-Type": "application/json",
        "User-Agent": "SkyMoleSetup (https://github.com/Sculike/SkyMole, 1.0)",
    })
    while True:
        try:
            with urllib.request.urlopen(request) as response:
                time.sleep(0.4)  # stay comfortably under the rate limit
                raw = response.read()
                return json.loads(raw) if raw else {}
        except urllib.error.HTTPError as e:
            if e.code == 429:
                wait = json.loads(e.read()).get("retry_after", 2)
                print("  rate limited, waiting %.1fs" % wait)
                time.sleep(wait + 0.3)
                continue
            raise SystemExit("API %s %s failed (%s):\n%s" % (method, path, e.code, e.read().decode()))


def ensure_roles():
    ids = {r["name"]: r["id"] for r in call("GET", "/guilds/%s/roles" % GUILD)}
    for name, colour, perms, hoist in ROLES:
        if name in ids:
            print("  role exists:", name)
            continue
        role = call("POST", "/guilds/%s/roles" % GUILD, {
            "name": name, "color": colour, "permissions": str(perms),
            "hoist": hoist, "mentionable": False,
        })
        ids[name] = role["id"]
        print("  role created:", name)
    return ids


def overwrites(name, roles):
    """Who can see and post in this channel. @everyone shares the guild's own id."""
    if name in STAFF_ONLY:
        allow = VIEW | SEND | REACT | HISTORY | EMBED | ATTACH | THREADS
        out = [{"id": GUILD, "type": 0, "deny": str(VIEW)}]
        for staff in ("Developer", "Moderator"):
            if staff in roles:
                out.append({"id": roles[staff], "type": 0, "allow": str(allow)})
        return out
    if name in READ_ONLY:
        out = [{"id": GUILD, "type": 0, "allow": str(VIEW | HISTORY | REACT), "deny": str(SEND)}]
        for staff in ("Developer", "Moderator"):
            if staff in roles:
                out.append({"id": roles[staff], "type": 0, "allow": str(SEND | EMBED | ATTACH)})
        return out
    return []


def main():
    if not TOKEN or not GUILD:
        raise SystemExit("Set DISCORD_TOKEN and DISCORD_GUILD first. See README.md next to this file.")

    guild = call("GET", "/guilds/%s" % GUILD)
    print("Server:", guild["name"])

    print("Roles")
    roles = ensure_roles()

    print("Channels")
    existing = {c["name"]: c for c in call("GET", "/guilds/%s/channels" % GUILD)}
    posted = []
    for category, channels in STRUCTURE:
        parent = existing.get(category)
        if parent is None:
            body = {"name": category, "type": CATEGORY}
            if category == "STAFF":
                body["permission_overwrites"] = overwrites("staff-chat", roles)
            parent = call("POST", "/guilds/%s/channels" % GUILD, body)
            existing[category] = parent
            print("  category created:", category)

        for name, kind, topic in channels:
            if name in existing:
                print("  channel exists:", name)
                continue
            body = {"name": name, "type": kind, "parent_id": parent["id"]}
            if topic:
                body["topic"] = topic
            perms = overwrites(name, roles)
            if perms:
                body["permission_overwrites"] = perms
            channel = call("POST", "/guilds/%s/channels" % GUILD, body)
            existing[name] = channel
            print("  channel created:", name)
            if name == "rules":
                posted.append((channel["id"], RULES))
            if name == "welcome":
                posted.append((channel["id"], WELCOME))

    for channel_id, text in posted:
        message = call("POST", "/channels/%s/messages" % channel_id, {"content": text})
        call("PUT", "/channels/%s/pins/%s" % (channel_id, message["id"]))
        print("  posted and pinned")

    print("")
    print("Done. In Server Settings > Roles, drag Developer above the bot's own role,")
    print("then give yourself Developer and remove the bot when you no longer need it.")


if __name__ == "__main__":
    sys.exit(main())
