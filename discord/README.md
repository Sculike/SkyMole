# SkyMole Discord setup

`server.py` builds the whole server over Discord's API: roles, categories,
channels, permissions, and the pinned welcome and rules posts.

You run it with your own bot token. The script reads the token from an
environment variable and never writes it anywhere, so it stays on your machine.

## What you get

```
INFORMATION   welcome · rules · announcements · changelog · links     (staff post, everyone reads)
SUPPORT       help · bug-reports · feature-requests
COMMUNITY     general · big-drops · media · off-topic
VOICE         General · Mining
STAFF         staff-chat · mod-log                                     (staff only, hidden)
```

Roles: **Developer** (gold, admin), **Moderator** (pink, kick/ban/timeout),
**Contributor** (cyan), **Member** (grey). The first three show separately in
the member list.

## Steps

**1. Make the server.** In Discord, `+` at the bottom of the server list →
*Create My Own*. Call it SkyMole. Set the icon to `branding/icon-512.png`.

**2. Make a bot and invite it.**

- Go to <https://discord.com/developers/applications> → *New Application*
- *Bot* → *Reset Token* → copy it. Treat it like a password: anyone with it
  controls the bot. Don't paste it into a chat, including to me.
- *OAuth2* → *URL Generator* → scope **bot**, permission **Administrator**
- Open the generated URL and add the bot to your server

**3. Get the server id.** In Discord, User Settings → Advanced → turn on
*Developer Mode*. Then right-click the server icon → *Copy Server ID*.

**4. Run it.** From this folder:

```
set DISCORD_TOKEN=your-token-here
set DISCORD_GUILD=your-server-id
python server.py
```

PowerShell uses `$env:DISCORD_TOKEN = "..."` instead.

**5. Afterwards.** Server Settings → Roles: drag **Developer** above the bot's
own role, give it to yourself, and kick the bot once you are done with it. A bot
with Administrator sitting in a server forever is a liability, not a feature.

## Changing it

Edit `STRUCTURE`, `ROLES`, `RULES` or `WELCOME` at the top of `server.py` and run
it again. Anything that already exists by name is skipped rather than duplicated,
so re-running only adds what is new. It never deletes anything — removing a
channel is something you do by hand, on purpose.

## Worth doing by hand afterwards

The API cannot do these, and they matter more than the channel list:

- **Server Settings → Safety Setup**: raise the verification level, and turn on
  AutoMod for spam and slurs. This is the single most useful thing on a public
  server.
- **Onboarding**: make reading `rules` the first step for new members.
- **Community Server**: enable it if you want announcement channels that other
  servers can follow, which is how mod update posts spread.
