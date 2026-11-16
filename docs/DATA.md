# What the phone sends the watch

The phone app reads a few sources, builds one small state, and puts it in the Wear OS Data Layer at
`/sablewatch/state`. The watch app stores it and refreshes the Sable complications. Steps, heart rate
and battery never involve the phone.

| Watch shows | SableOS build (`sable`) | Any phone (`generic`, e.g. Titan 2 with TenSeven) |
|---|---|---|
| Messages count | Sable Hub `content://org.sableos.hub.snapshot/apps` | TenSeven Hub `content://io.github.aimindseye.tenseven.hubcounts/apps` |
| (fallback) | Notification count, if notification access is on | Notification count, if notification access is on |
| Agenda Peek | Sable Calendar `content://org.sableos.calendar.snapshot/next` | The phone's calendars (`READ_CALENDAR`) |
| (fallback) | The phone's calendars | none |
| Weather | Sable Weather `content://org.sableos.weather.snapshot/current` | Open-Meteo for the city picked in the app |
| (fallback) | Open-Meteo, if a city is picked | none |
| World clock | Cities picked in the app; the times are computed on the watch | same |

By default only counts and the next event's title and times leave the phone: no message text,
sender or notification content. "Show every event as Busy" hides titles too.

## Hub on the watch (opt-in)

"Hub on the watch", in the app's Messages section and off by default, adds a BB10-style Hub list
on the watch, opened by tapping the Hub bubble. With it on, the phone also sends, at
`/sablewatch/hub`, the newest 20 Hub notifications: message notifications, plus any from an app
Hub is counting (or, when Hub can't be read, any clearable notification that alerts). For each:

- the app's name, the sender (or group name), and the first 120 characters of the latest message;
- the time and the unread count;
- an opaque id (a hash of the notification key), so a tap on the watch opens that conversation
  on the phone.

It reads them through notification access, so it works the same with TenSeven, Sable Hub or no
Hub. Notifications an app marks as secret (hidden on the lock screen) are never sent. Turning
the setting off deletes the list from the watch. Nothing is stored on the phone.

## Who may read the Hub counts

- **SableOS**: each provider checks its caller. SableOS patch `0222` adds Sable Watch, pinned to its
  signing certificate, and gives it counts only (Hub `/apps`, Calendar `/next`, Weather `/current`).
- **TenSeven Launcher**: the provider is protected by the signature permission
  `io.github.aimindseye.tenseven.permission.READ_HUB_COUNTS`, so only an app signed with the
  TenSeven key gets it. Sign Sable Watch with the same key (see [RELEASING.md](RELEASING.md)).

## When it syncs

- Soon after a notification is posted or removed, or a Hub, calendar or weather provider reports a
  change (needs notification access, which keeps the listener running).
- When the next event ends, so the arc moves on to the one after it.
- Every 15 minutes, and when Sync now (or `S`) is pressed.
- When the watch asks: opening Sable Hub on the watch, or its Refresh button, sends a
  `/sablewatch/refresh` message (no data) and the phone syncs at once.
- Nothing is sent when nothing changed, except for Sync now, a refresh from the watch, and once
  every 30 minutes, so the watch can tell "nothing new" from "the phone is away".

## When the phone is away

The Data Layer also works through Google's cloud when both have internet (Galaxy Wearable >
Remote connection), so updates keep coming without Bluetooth. With no connection at all:

- The clock, date, steps, battery, heart rate and world clock carry on; they are computed on the
  watch.
- After an hour without the phone, the watch fetches Open-Meteo itself for the phone's weather
  city (its coordinates come with each update; never the watch's location), at most every 30
  minutes, if the watch has its own Wi-Fi or LTE.
- After two hours, the Hub bubble's icon changes to a struck-through bubble: the count is the
  last one the phone sent, not a live one.

## Default complications

The face (`watchface.xml`) names the Sable sources as each slot's primary provider and keeps a system
provider as the fallback. The watch app lists the face in `SAFE_WATCH_FACES`, so the face may use
them without a permission prompt. Still to confirm on a Galaxy Watch 7: that Wear OS honours a
third-party primary provider for a Watch Face Format face. If it doesn't, pick the Sable sources once
in the face editor.
