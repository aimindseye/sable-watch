#!/usr/bin/env python3
"""Writes watchface/src/main/res/raw/watchface.xml (Watch Face Format v1).

The layout is generated so the five round slots share one template. Edit the
SLOTS table or the helpers below, then run: python3 tools/gen-watchface.py
"""
from pathlib import Path

CYAN = "#FF00A8DF"
# The accent colour is the user's pick in the face editor (Customize > Colour); cyan by default.
ACCENT = "[CONFIGURATION.accent.0]"
ACCENTS = [  # id, label resource, colour
    ("cyan", "color_cyan", CYAN),
    ("green", "color_green", "#FF3DDC84"),
    ("amber", "color_amber", "#FFFFB300"),
    ("red", "color_red", "#FFFF5252"),
    ("violet", "color_violet", "#FFB388FF"),
    ("white", "color_white", "#FFEDEDED"),
]
WHITE = "#FFFFFFFF"
GREY = "#FF9A9A9A"
BUBBLE = "#FF0D1216"
TRACK_ALPHA = 90  # of 255: under a range (battery, steps), the unfilled part of the ring
# Per-slot text size, where the slot's usual source sends long words ("Heart rate" before a reading).
TEXT_SIZE = {"heart_rate": 15}
FONT = "SYNC_TO_DEVICE"
COMPLICATION_END = "      </Complication>\n"

# id, name, centre x, centre y, radius, supported types, default provider, default type
# The Sable Watch data sources, fed from the phone. A slot uses its Sable source when the watch
# app is installed, and falls back to the system provider when it is not.
SABLE = "io.github.aimindseye.sablewatch/io.github.aimindseye.sablewatch.complications."

SLOTS = [
    (1, "messages", 78, 262, 38, "SHORT_TEXT MONOCHROMATIC_IMAGE SMALL_IMAGE",
     "UNREAD_NOTIFICATION_COUNT", "SHORT_TEXT", SABLE + "MessagesDataSource"),
    (2, "weather", 366, 192, 36, "SHORT_TEXT MONOCHROMATIC_IMAGE SMALL_IMAGE", "EMPTY", "SHORT_TEXT",
     SABLE + "WeatherDataSource"),
    (3, "heart_rate", 358, 286, 42, "SHORT_TEXT MONOCHROMATIC_IMAGE RANGED_VALUE", "EMPTY", "SHORT_TEXT"),
    (4, "battery", 140, 350, 34, "RANGED_VALUE SHORT_TEXT MONOCHROMATIC_IMAGE",
     "WATCH_BATTERY", "RANGED_VALUE"),
    (5, "steps", 232, 357, 42, "RANGED_VALUE SHORT_TEXT MONOCHROMATIC_IMAGE",
     "STEP_COUNT", "SHORT_TEXT"),
]


def ambient_hide(indent):
    return f'{indent}<Variant mode="AMBIENT" target="alpha" value="0"/>\n'


def text(x, y, w, h, size, color, expr, indent, weight="NORMAL", ambient=None):
    amb = "" if ambient is None else ambient
    return (
        f'{indent}<PartText x="{x}" y="{y}" width="{w}" height="{h}">\n{amb}'
        f'{indent}  <Text align="CENTER" ellipsis="TRUE">\n'
        f'{indent}    <Font family="{FONT}" size="{size}" color="{color}" weight="{weight}">'
        f'<Template>%s<Parameter expression="{expr}"/></Template></Font>\n'
        f'{indent}  </Text>\n{indent}</PartText>\n'
    )


def bubble(d, indent, ring_alpha=255):
    return (
        f'{indent}<PartDraw x="0" y="0" width="{d}" height="{d}">\n'
        f'{indent}  <Ellipse x="1" y="1" width="{d - 2}" height="{d - 2}">\n'
        f'{indent}    <Fill color="{BUBBLE}"/>\n'
        f'{indent}  </Ellipse>\n'
        f'{indent}  <Variant mode="AMBIENT" target="alpha" value="0"/>\n'
        f'{indent}</PartDraw>\n'
        # The same thick accent ring on every bubble. Under a range (battery, steps) it is dimmed
        # to a track that the range fills in the full accent.
        f'{indent}<PartDraw x="0" y="0" width="{d}" height="{d}" alpha="{ring_alpha}">\n'
        f'{indent}  <Ellipse x="2" y="2" width="{d - 4}" height="{d - 4}">\n'
        f'{indent}    <Stroke color="{ACCENT}" thickness="4"/>\n'
        f'{indent}  </Ellipse>\n'
        f'{indent}  <Variant mode="AMBIENT" target="alpha" value="0"/>\n'
        f'{indent}</PartDraw>\n'
        # Ambient keeps only a thin grey outline, no fill.
        f'{indent}<PartDraw x="0" y="0" width="{d}" height="{d}" alpha="0">\n'
        f'{indent}  <Ellipse x="1" y="1" width="{d - 2}" height="{d - 2}">\n'
        f'{indent}    <Stroke color="#FF444444" thickness="1"/>\n'
        f'{indent}  </Ellipse>\n'
        f'{indent}  <Variant mode="AMBIENT" target="alpha" value="255"/>\n'
        f'{indent}</PartDraw>\n'
    )


def icon(d, size, y, indent):
    x = (d - size) // 2
    return (
        f'{indent}<PartImage x="{x}" y="{y}" width="{size}" height="{size}" tintColor="{ACCENT}">\n'
        f'{indent}  <Variant mode="AMBIENT" target="alpha" value="170"/>\n'
        f'{indent}  <Image resource="[COMPLICATION.MONOCHROMATIC_IMAGE]"/>\n'
        f'{indent}</PartImage>\n'
    )


def policy(provider, ptype, primary=None, primary_type="SHORT_TEXT"):
    sable = f' primaryProvider="{primary}" primaryProviderType="{primary_type}"' if primary else ""
    return (f'      <DefaultProviderPolicy defaultSystemProvider="{provider}" '
            f'defaultSystemProviderType="{ptype}"{sable}/>\n')


def slot(sid, name, cx, cy, r, types, provider, ptype, primary=None):
    # Wear OS rejects the whole face if a fallback type is not one the slot supports,
    # even for the EMPTY provider.
    assert ptype in types.split(), f"slot {name}: fallback type {ptype} not in {types}"
    d = 2 * r
    i = "      "
    out = [
        f'    <ComplicationSlot slotId="{sid}" name="{name}" displayName="slot_{name}" '
        f'x="{cx - r}" y="{cy - r}" width="{d}" height="{d}" supportedTypes="{types}" isCustomizable="TRUE">\n',
        policy(provider, ptype, primary),
        f'      <BoundingOval x="0" y="0" width="{d}" height="{d}"/>\n',
    ]
    isz = max(18, r // 2 + 4)
    tsz = TEXT_SIZE.get(name, 20 if r >= 36 else 17)
    j = i + "  "

    def complication(ctype, *parts):
        ring = TRACK_ALPHA if ctype == "RANGED_VALUE" else 255
        out.extend([f'      <Complication type="{ctype}">\n', bubble(d, j, ring), *parts, COMPLICATION_END])

    # SHORT_TEXT: icon on top, value below (the BB10 bubbles).
    complication(
        "SHORT_TEXT",
        icon(d, isz, int(r * 0.30), j),
        text(4, int(r * 0.30) + isz, d - 8, tsz + 8, tsz, WHITE, "[COMPLICATION.TEXT]", j,
             ambient=f'{i}    <Variant mode="AMBIENT" target="alpha" value="200"/>\n'),
    )
    # MONOCHROMATIC_IMAGE: icon only.
    complication("MONOCHROMATIC_IMAGE", icon(d, int(d * 0.55), int(d * 0.225), j))
    if "SMALL_IMAGE" in types:
        s = int(d * 0.6)
        complication(
            "SMALL_IMAGE",
            f'{i}  <PartImage x="{(d - s) // 2}" y="{(d - s) // 2}" width="{s}" height="{s}">\n'
            f'{i}    <Image resource="[COMPLICATION.SMALL_IMAGE]"/>\n{i}  </PartImage>\n',
        )
    if "RANGED_VALUE" in types:
        # A cyan progress ring round the bubble, as on the battery and steps bubbles.
        frac = ("([COMPLICATION.RANGED_VALUE_VALUE] - [COMPLICATION.RANGED_VALUE_MIN]) / "
                "([COMPLICATION.RANGED_VALUE_MAX] - [COMPLICATION.RANGED_VALUE_MIN])")
        complication(
            "RANGED_VALUE",
            f'{i}  <PartDraw x="0" y="0" width="{d}" height="{d}">\n'
            f'{i}    <Arc centerX="{r}" centerY="{r}" width="{d - 4}" height="{d - 4}" startAngle="0" endAngle="360">\n'
            f'{i}      <Stroke color="{ACCENT}" thickness="4" cap="ROUND"/>\n'
            f'{i}      <Transform target="endAngle" value="360 * {frac}"/>\n'
            f'{i}    </Arc>\n'
            f'{i}    <Variant mode="AMBIENT" target="alpha" value="0"/>\n'
            f'{i}  </PartDraw>\n',
            icon(d, isz, int(r * 0.30), j),
            text(4, int(r * 0.30) + isz, d - 8, tsz + 8, tsz, WHITE, "[COMPLICATION.TEXT]", j),
        )
    out.append('    </ComplicationSlot>\n')
    return "".join(out)


def top_arc():
    # Sable Agenda Peek by default (the system "next event" without the watch app); the user
    # can pick Sable World Clock or any other long-text source in the watch's face editor.
    arc = 'centerX="225" centerY="225" width="330" height="330" startAngle="-68" endAngle="68"'
    body = (
        '    <ComplicationSlot slotId="0" name="top_arc" displayName="slot_top_arc" x="0" y="0" '
        'width="450" height="450" supportedTypes="LONG_TEXT SHORT_TEXT" isCustomizable="TRUE">\n'
        + policy("NEXT_EVENT", "LONG_TEXT", SABLE + "AgendaPeekDataSource", "LONG_TEXT") +
        '      <BoundingArc centerX="225" centerY="225" width="350" height="350" thickness="40" '
        'startAngle="-68" endAngle="68" isRoundEdge="TRUE"/>\n'
    )
    for t in ("LONG_TEXT", "SHORT_TEXT"):
        body += (
            f'      <Complication type="{t}">\n'
            '        <PartText x="0" y="0" width="450" height="450">\n'
            '          <Variant mode="AMBIENT" target="alpha" value="170"/>\n'
            f'          <TextCircular {arc} align="CENTER" direction="CLOCKWISE" ellipsis="TRUE">\n'
            f'            <Font family="{FONT}" size="22" color="{WHITE}" weight="LIGHT">'
            '<Template>%s<Parameter expression="[COMPLICATION.TEXT]"/></Template></Font>\n'
            '          </TextCircular>\n'
            '        </PartText>\n'
            + COMPLICATION_END
        )
    return body + '    </ComplicationSlot>\n'


def user_configurations():
    options = "".join(
        f'      <ColorOption id="{oid}" displayName="{label}" colors="{color}"/>\n' for oid, label, color in ACCENTS
    )
    return ('  <UserConfigurations>\n'
            '    <ColorConfiguration id="accent" displayName="accent_label" defaultValue="cyan">\n'
            f'{options}'
            '    </ColorConfiguration>\n'
            '  </UserConfigurations>\n')


def main():
    x = ['<?xml version="1.0" encoding="utf-8"?>\n',
         '<!-- Generated by tools/gen-watchface.py: edit that file, not this one. -->\n',
         '<WatchFace width="450" height="450">\n',
         '  <Metadata key="CLOCK_TYPE" value="DIGITAL"/>\n',
         '  <Metadata key="PREVIEW_TIME" value="10:09:32"/>\n',
         user_configurations(),
         '  <Scene backgroundColor="#FF000000">\n',
         '    <PartImage x="0" y="0" width="450" height="450">\n'
         '      <Variant mode="AMBIENT" target="alpha" value="0"/>\n'
         '      <Image resource="carbon"/>\n    </PartImage>\n',
         '    <PartImage x="0" y="0" width="450" height="450">\n'
         '      <Variant mode="AMBIENT" target="alpha" value="0"/>\n'
         '      <Image resource="ticks"/>\n    </PartImage>\n',
         # The hour marks are white in the image and take the accent colour here.
         '    <PartImage x="0" y="0" width="450" height="450" tintColor="' + ACCENT + '">\n'
         '      <Variant mode="AMBIENT" target="alpha" value="0"/>\n'
         '      <Image resource="ticks_hours"/>\n    </PartImage>\n',
         # Cyan accent arc over the top half, with the dot at nine o'clock.
         '    <PartDraw x="0" y="0" width="450" height="450">\n'
         '      <Arc centerX="225" centerY="225" width="400" height="400" startAngle="-90" endAngle="90">\n'
         f'        <Stroke color="{ACCENT}" thickness="3"/>\n      </Arc>\n'
         '      <Ellipse x="19" y="219" width="12" height="12">\n'
         f'        <Fill color="{ACCENT}"/>\n      </Ellipse>\n'
         '      <Variant mode="AMBIENT" target="alpha" value="0"/>\n    </PartDraw>\n',
         # Thin grey bezel ring for ambient only.
         '    <PartDraw x="0" y="0" width="450" height="450" alpha="0">\n'
         '      <Ellipse x="20" y="20" width="410" height="410">\n'
         '        <Stroke color="#FF333333" thickness="1"/>\n      </Ellipse>\n'
         '      <Variant mode="AMBIENT" target="alpha" value="255"/>\n    </PartDraw>\n',
         # Time and date sit high, just under the top arc, leaving the lower half to the bubbles.
         '    <DigitalClock x="75" y="128" width="300" height="90">\n'
         '      <TimeText format="hh:mm" hourFormat="SYNC_TO_DEVICE" align="CENTER" x="0" y="0" width="300" height="90">\n'
         f'        <Font family="{FONT}" size="74" color="{WHITE}" weight="LIGHT"/>\n'
         '      </TimeText>\n    </DigitalClock>\n',
         # "MON," in white and "NOV 9" in cyan, as in the BB10 design.
         '    <PartText x="100" y="216" width="120" height="32">\n'
         '      <Text align="END">\n'
         f'        <Font family="{FONT}" size="23" color="{WHITE}">'
         '<Upper><Template>%s,<Parameter expression="[DAY_OF_WEEK_S]"/></Template></Upper></Font>\n'
         '      </Text>\n    </PartText>\n',
         '    <PartText x="228" y="216" width="130" height="32">\n'
         '      <Variant mode="AMBIENT" target="alpha" value="200"/>\n'
         '      <Text align="START">\n'
         f'        <Font family="{FONT}" size="23" color="{ACCENT}">'
         '<Upper><Template>%s %s<Parameter expression="[MONTH_S]"/><Parameter expression="[DAY]"/></Template></Upper></Font>\n'
         '      </Text>\n    </PartText>\n',
         top_arc()]
    x += [slot(*s) for s in SLOTS]
    x.append('  </Scene>\n</WatchFace>\n')
    out = Path(__file__).resolve().parent.parent / "watchface/src/main/res/raw/watchface.xml"
    out.write_text("".join(x), encoding="utf-8")
    print(out)


if __name__ == "__main__":
    main()
