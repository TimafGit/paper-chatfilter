# ChatFilter

A Paper 1.21 chat filter plugin. Catches banned words and hits the player with sound effects, potion effects, and a big red screen warning. Everything is customizable from config.yml — live reload included. Keep it up and get kicked.

Tested on Paper build 26.1.2.

---

## Features

- Detects banned words and cancels the message before it reaches chat
- Plays a sound effect (configurable, chosen at random from a list)
- Shows a bold red title in the center of the screen
- Applies potion effects (blindness, slowness, nausea — or whatever you configure)
- Tracks violations per player and kicks them after a configurable threshold
- Everything is editable from `config.yml` with live reload via `/chatfilter reload`
<img width="740" alt="2026-07-05_13 27 30" src="https://github.com/user-attachments/assets/47796238-4af7-4fa3-a3be-939bf431f58a" />
<img width="740" alt="Screenshot_20260705_132740" src="https://github.com/user-attachments/assets/f1d20b10-c199-4b4d-ba02-956e217e048a" />
---

## Installation

1. Download the latest `.jar` from [Releases](../../releases)
2. Drop it into your server's `plugins/` folder
3. Restart the server — `plugins/ChatFilter/config.yml` will be generated automatically
4. Edit `config.yml` to add your banned words, messages, sounds, and effects
5. Run `/chatfilter reload` in-game to apply changes without restarting

**Requirements:** Paper 1.21 (build 26.1.2+), Java 25

---

## Configuration

`config.yml` is fully documented with comments. Here's a quick overview:

| Section | What it does |
|---|---|
| `banned-words` | List of words to detect (case-insensitive, strips special characters) |
| `warning-messages` | List of title messages shown on detection, one picked at random |
| `sound.options` | List of vanilla sound event keys, one picked at random |
| `sound.volume` / `sound.pitch` | Volume and pitch of the sound |
| `effects` | Potion effects applied on each violation (type, duration, amplifier) |
| `kick.enabled` | Whether to kick players after repeated violations |
| `kick.threshold` | Number of violations before a kick |
| `kick.message` | Message shown on the kick screen |

**Effect duration** can be written as `duration-seconds: 1` (recommended) or `duration-ticks: 20`.

**Sound keys** follow Minecraft's namespaced format, e.g. `entity.ghast.hurt`, `ambient.cave`. Full list [here](https://jd.papermc.io/paper/26.1.2/io/papermc/paper/registry/keys/SoundEventKeys.html).

---

## Commands & Permissions

| Command | Description | Permission |
|---|---|---|
| `/chatfilter reload` | Reloads config.yml without restarting | `chatfilter.admin` |

`chatfilter.admin` is granted to ops by default.

---

## How It Works

When a player sends a message, the plugin normalizes it (strips spaces, punctuation, and special characters) before checking it against the banned word list. This catches basic bypass attempts like `b.a.d` or `b a d`.

If a match is found, the message is cancelled before it reaches chat. The player then receives a sound, a screen title, and any configured potion effects. A per-player violation counter increments each time — once it hits the configured threshold, the player is kicked and the counter resets.

> **Note:** The normalization is intentionally simple and not bypass-proof. Determined players can still work around it. For stricter filtering, consider combining this plugin with a regex-based solution.

---

## Building from Source

**Requirements:** JDK 25, internet connection (Gradle downloads dependencies automatically)

```bash
git clone https://github.com/TimafGit/paper-chatfilter
cd paper-chatfilter
./gradlew shadowJar
```

The output jar will be in `build/libs/`.

## Acknowledgements

Developed with the assistance of [Claude](https://claude.ai) (Anthropic).

---

## License

This project is licensed under the GNU General Public License v3.0. See [LICENSE](LICENSE) for details.
