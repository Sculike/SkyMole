"""Builds the SkyMole Discord server: roles, categories, channels and pinned posts.

Run it yourself with your own bot token. The token is read from the environment
and never written anywhere, so it stays on your machine.

    $env:DISCORD_TOKEN = "..."       (PowerShell)
    $env:DISCORD_GUILD = "..."
    python server.py

Safe to run twice: anything already there by name is left alone rather than
duplicated, so you can edit this file and run it again. It never deletes.

The shape follows Elite Skyblock Farmers, the best-run server in this corner of
SkyBlock: forums for anything with a back-and-forth, announcement channels other
servers can follow, a support category split by topic, and a waiting room.
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
SEND_IN_THREADS = 1 << 38
CREATE_POSTS = 1 << 35
MANAGE_MESSAGES = 1 << 13
KICK = 1 << 1
BAN = 1 << 2
MODERATE = 1 << 40
ADMIN = 1 << 3

TEXT, VOICE, CATEGORY, ANNOUNCEMENT, FORUM = 0, 2, 4, 5, 15

# name, colour, permissions, shown separately in the member list
ROLES = [
    ("Developer", GOLD, ADMIN, True),
    ("Moderator", PINK, KICK | BAN | MODERATE | MANAGE_MESSAGES, True),
    ("Contributor", CYAN, 0, True),
    ("Member", GREY, 0, False),
]

# Everyone reads, only staff posts.
READ_ONLY = {"welcome", "rules", "announcements", "changelog", "links", "faq"}
# Hidden from everyone but staff.
STAFF_ONLY = {"staff-chat", "mod-log"}

STRUCTURE = [
    ("INFORMATION", [
        ("welcome", TEXT, "Start here."),
        ("rules", TEXT, "Read before posting."),
        ("announcements", ANNOUNCEMENT, "Releases and news. Follow this channel to get them in your own server."),
        ("changelog", ANNOUNCEMENT, "What changed in each version."),
        ("links", TEXT, "Modrinth, GitHub, and where else to find SkyMole."),
    ]),
    ("SUPPORT", [
        ("faq", TEXT, "Read this before asking. It is faster than waiting for a reply."),
        ("help", FORUM, "One post per problem. Include your Minecraft version, mod version, and what you expected."),
        ("bug-reports", FORUM, "Run /skymole debug first and attach the file it writes to your config folder."),
        ("feature-requests", FORUM, "One post per idea. Search first, someone may have asked already."),
    ]),
    ("COMMUNITY", [
        ("general", TEXT, "Talk about anything SkyBlock."),
        ("big-drops", TEXT, "Post your lucky drops."),
        ("media", TEXT, "Screenshots and clips."),
        ("setups", TEXT, "Share your HUD layout and settings."),
        ("off-topic", TEXT, "Everything else."),
    ]),
    ("VOICE", [
        ("no-mic", TEXT, "Type here while others talk."),
        ("General", VOICE, None),
        ("Mining", VOICE, None),
        ("AFK", VOICE, None),
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
**5. Support goes in the support channels.** Not in general, and not in my DMs.
**6. One post per problem.** Help and bug-reports are forums so your issue does not get buried.
**7. Staff decisions are final.** Disagree in DMs, not in public.

You get a warning first, unless it is serious enough not to."""

WELCOME = """**Welcome to SkyMole.**

SkyMole is a mining helper for Hypixel SkyBlock: commissions, powder, events, solvers and trackers.

**Get the mod** - see links.
**Something not working?** - read faq, then post in bug-reports. Run `/skymole debug` first; it writes a file to your config folder saying exactly what the mod could see, which usually answers the question outright.
**Want something added?** - feature-requests.

Built by sculike."""

FAQ = """**Frequently asked**

**Which versions does it work on?**
Minecraft 1.26.2, Fabric. It needs Fabric API and Java 25.

**It is installed but nothing shows up.**
You have to be in SkyBlock, on a mining island. Most panels hide themselves everywhere else on purpose.

**A HUD is in the way.**
`/skymole hud` lets you drag every panel. Scroll to resize, middle-click to hide one, right-click to reset it.

**Does it break Hypixel's rules?**
No. Nothing is automated, and nothing is drawn through walls. Outlines are depth-tested exactly like a vanilla block outline.

**It stopped working after a Hypixel update.**
Likely a text format changed. Run `/skymole debug` and post the file in bug-reports; that file is what makes it a quick fix instead of a guess.

**Can I use it with SkyHanni / Skyblocker / SBA?**
Yes. If two mods draw the same thing, turn one of them off in its menu."""


def call(method, path, body=None, soft=False):
    """One API call, retried when Discord asks us to slow down.

    soft=True returns None on failure instead of stopping, which is how the
    richer channel types degrade gracefully when the server is not a Community.
    """
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
            if soft:
                return None
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
    """Who can see and post here. The @everyone role shares the guild's own id."""
    staff_allow = VIEW | SEND | REACT | HISTORY | EMBED | ATTACH | THREADS | CREATE_POSTS
    if name in STAFF_ONLY:
        out = [{"id": GUILD, "type": 0, "deny": str(VIEW)}]
        for staff in ("Developer", "Moderator"):
            if staff in roles:
                out.append({"id": roles[staff], "type": 0, "allow": str(staff_allow)})
        return out
    if name in READ_ONLY:
        out = [{"id": GUILD, "type": 0, "allow": str(VIEW | HISTORY | REACT),
                "deny": str(SEND | CREATE_POSTS)}]
        for staff in ("Developer", "Moderator"):
            if staff in roles:
                out.append({"id": roles[staff], "type": 0, "allow": str(SEND | EMBED | ATTACH)})
        return out
    return []


def make_channel(body, name, kind):
    """Create a channel, stepping down to a plain text one if the type is refused.

    Forum and announcement channels need the server to be a Community. Rather
    than failing the whole run, the channel is made as text and reported, so it
    can be converted later from Discord's own settings.
    """
    channel = call("POST", "/guilds/%s/channels" % GUILD, body, soft=True)
    if channel is not None:
        return channel, False
    if kind in (FORUM, ANNOUNCEMENT):
        body = dict(body, type=TEXT)
        return call("POST", "/guilds/%s/channels" % GUILD, body), True
    raise SystemExit("Could not create #%s. Check the bot has Administrator." % name)


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
    downgraded = []
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
            channel, stepped_down = make_channel(body, name, kind)
            existing[name] = channel
            if stepped_down:
                downgraded.append(name)
            print("  channel created:", name, "(as text)" if stepped_down else "")
            if name == "rules":
                posted.append((channel["id"], RULES))
            if name == "welcome":
                posted.append((channel["id"], WELCOME))
            if name == "faq":
                posted.append((channel["id"], FAQ))

    for channel_id, text in posted:
        message = call("POST", "/channels/%s/messages" % channel_id, {"content": text})
        call("PUT", "/channels/%s/pins/%s" % (channel_id, message["id"]), soft=True)
        print("  posted and pinned")

    print("")
    if downgraded:
        print("Made as plain text because the server is not a Community yet:")
        print(" ", ", ".join(downgraded))
        print("Turn on Community in Server Settings, then change their type in each")
        print("channel's settings to get forums and followable announcements.")
        print("")
    print("Then: Server Settings > Roles, drag Developer above the bot's own role,")
    print("give it to yourself, and kick the bot once you are done with it.")


if __name__ == "__main__":
    sys.exit(main())
