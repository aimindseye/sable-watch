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

Only counts and the next event's title and times leave the phone: no message text, sender or
notification content. "Show every event as Busy" hides titles too.

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
- Nothing is sent when nothing changed, except for Sync now.

## Default complications

The face (`watchface.xml`) names the Sable sources as each slot's primary provider and keeps a system
provider as the fallback. The watch app lists the face in `SAFE_WATCH_FACES`, so the face may use
them without a permission prompt. Still to confirm on a Galaxy Watch 7: that Wear OS honours a
third-party primary provider for a Watch Face Format face. If it doesn't, pick the Sable sources once
in the face editor.
