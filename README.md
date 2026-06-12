# 🏠 Nepremicnine Scraper

A small **personal / educational** desktop app (Java + Swing + Selenium) that
loads real-estate rental listings from nepremicnine.net and notifies you when a
**new** advertisement appears.

It is a learning project, not a product. It is intentionally limited in scope
(see *Scope & limitations* below) and is **not** designed to harvest the site at
scale.

## ✨ What it does

- Loads the first results page of a rental search (newest listings first).
- Shows each listing as a card (title, description, price, clickable link).
- Re-checks on an interval and highlights **new** listings in green, with a
  desktop (system-tray) notification.

## 🔍 Scope & limitations

- **Single page only.** nepremicnine.net is fronted by Cloudflare, which serves a
  bot challenge on every result page after the first. This app deliberately
  **does not attempt to bypass that protection** — when it sees the challenge it
  simply stops. Because the search is sorted newest-first, new ads appear on the
  first page anyway, which is all the notifier needs.
- **Low frequency.** It re-checks every 30 minutes by default
  (`SCRAPE_INTERVAL_MINUTES` in `Interface.java`).
- **No data is stored or redistributed.** Listings are only shown in the running
  window. Nothing is written to disk or committed to this repository.

## ⚖️ Legal / responsible use

This project is provided for personal, non-commercial, educational use only.

- Scraping may be restricted by nepremicnine.net's
  [Terms of Use](https://www.nepremicnine.net/pogoji-uporabe.html) and
  [robots.txt](https://www.nepremicnine.net/robots.txt), and listing content may
  be protected by copyright and database rights.
- **You are responsible** for ensuring your use complies with the site's terms
  and applicable law. Do not use this to republish listings, for commercial
  purposes, or at high request volumes.
- This app does not circumvent the site's anti-bot protection, and you should
  not modify it to do so.

If you only need new-listing alerts, consider the site's own saved-search /
email-alert features instead.

## 🛠️ Tech stack

- Java 23, Maven
- Swing (GUI)
- Selenium + WebDriverManager (auto-resolves a matching chromedriver — no manual
  driver path needed; requires Google Chrome installed)

## 🚀 Build & run

```bash
mvn clean compile
mvn exec:java -Dexec.mainClass=Main
```

or build a jar and run it, or just run `Main` from your IDE.

Press **Start scraping** to load the current listings and begin monitoring;
press **STOP** to halt.
