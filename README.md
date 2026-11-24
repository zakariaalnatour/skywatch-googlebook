# SkyWatch for Googlebook OS

A native **Googlebook OS (Android 17 / API 37)** live flight tracker built with **Kotlin** and **Jetpack Compose**, engineered for desktop-class Middle East and global ADS-B airspace monitoring with zero API keys required.

## Highlights

- **Googlebook OS Exclusive (Blocks Chromebooks / ARCVM)**
  - `minSdk = 37`, `targetSdk = 37`, `compileSdk = 37`
  - Declares `<uses-feature android:name="android.software.app_widgets" android:required="true" />` with zero implicit location hardware features
  - Runtime ChromeOS ARCVM guard blocking `org.chromium.arc*` system features and ARC build markers (`cheets`, `bertha`, `arcvm`)
- **Googlebook Desktop Integration**
  - **Android 17 Continue On (`HandoffActivityData`)** exporting active hub, map viewport, selected aircraft, and live web fallback URI (`globe.adsb.fi`)
  - **Multi-Instance Windowing** (`PROPERTY_SUPPORTS_MULTI_INSTANCE_SYSTEM_UI` + `Ctrl+N` / Pop Out in New Window)
  - **Adaptive 3-Pane Desktop UI** (Flights & Search | Interactive Geographic Radar | Live Telemetry & Route Inspector) that seamlessly adapts to 2-pane split-screen and compact freeform windows
  - **Desktop App Widget** displaying active hub, live flight count, top aircraft, one-tap live `SYNC`, and quick-jump hub chips (`DXB`, `DOH`, `RUH`, `MIL`)
  - **Keyboard & Trackpad Shortcuts** (`Ctrl+K` or `/` search, `R`/`F5` refresh, `Space` pause/resume, `+`/`-` or trackpad scroll zoom, arrow pan, `J`/`K` step flights, `F` follow, `M` map layer, `T` trails, `G` great-circle route, `1–9` hubs, `Ctrl+N` new window, `?` help, `Esc` dismiss)
- **Live Keyless Middle East & Global Flight Tracking**
  - Primary regional feed: `opendata.adsb.fi`
  - Automatic failover, global search, widebody aggregation, and military feed: `api.adsb.lol`
  - Live route lookup & aircraft photos: `api.adsbdb.com`
- **Keyless Geographic Map**
  - Primary tiles: **Esri World Dark Gray Base + Reference** (`{z}/{y}/{x}`)
  - Automatic & manual fallback: **OpenStreetMap** (`{z}/{x}/{y}`)
  - Great-circle (SLERP) route arcs with antimeridian splitting, altitude-coded live trails, and 14 one-click hubs:
    - **Middle East:** `DXB`, `DOH`, `RUH`, `JED`, `AMM`, `CAI`, `IST`
    - **Global:** `SFO`, `JFK`, `LAX`, `LHR`, `HND`
    - **Feeds:** `Widebody`, `Military`

## Building & Testing

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

## Download & Install on Googlebook

Grab `SkyWatch-Googlebook-v1.1.0.apk` from the [`v1.1.0` GitHub Release](https://github.com/zakariaalnatour/skywatch-googlebook/releases/tag/v1.1.0).
