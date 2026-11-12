<img src="stealth.png">

# Stealth

Stealth is an account-free, privacy-oriented, and feature-rich browser for Reddit.

It provides features from Reddit, without the need for an account.

A fork of [cosmosapps/stealth](https://gitlab.com/cosmosapps/stealth) on GitLab.

[<img src="https://img.shields.io/github/v/release/RohitSurwase/Stealth-App?label=GitHub%20Release"
     alt="Get it on GitHub"
     height="30">](https://github.com/RohitSurwase/Stealth-App/releases)

Join Stealth on Matrix: [#stealth-app:matrix.org](https://matrix.to/#/#stealth-app:matrix.org)

> For the current status of the app, follow [Issue #15](https://github.com/RohitSurwase/Stealth-App/issues/15).

## Features

- Browse Reddit (view posts, comments, subreddits, and users)
- Search posts, subreddits and users
- Subscribe to subreddits
- Sort
- History
- Save posts and comments
- Create different profiles (per-profile subscriptions, history and saved items)
- Show/hide NSFW content
- Image flairs
- Awards
- Save photos and videos
- Light/Dark theme
- Load Reddit through Redlib instances

## Redlib support

This build loads Reddit through [Redlib](https://github.com/redlib-org/redlib) instances.

- Redlib instances are fetched from [`instances.json`](https://github.com/redlib-org/redlib-instances/blob/main/instances.json).
- You can enter your own Redlib instance URL.
- Redlib relies on a Cloudflare challenge that must be passed manually — use the **"Verify Instance in Browser"** option to complete it.
- Redlib has its own NSFW settings. The app tries to sync its preferences with the Redlib instance, but the setting resets on instance change, so run **"Verify Instance in Browser"** again whenever you toggle **"Show NSFW content"**.
- The active Redlib instance is shown under the app title.
- A custom User-Agent string can be set, useful if instance verification fails with the default User-Agent.

## Maintenance & contributions

This repo contains only the necessary patches to keep the core app working. I'll maintain it as long as I can, as soon as possible.

Don't expect new features or quality-of-life improvements.

If you'd like a feature, open a Pull Request — if it's good enough, I'll review and merge it.

## License

Copyright 2021 CosmosDev

Licensed under the GPLv3: http://www.gnu.org/licenses/gpl-3.0.html

---

*Reddit is a registered trademark of Reddit Inc. This project is not affiliated with or endorsed by Reddit Inc.*
