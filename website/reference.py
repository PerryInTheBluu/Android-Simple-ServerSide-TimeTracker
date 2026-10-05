#!/usr/bin/env python3
"""Independent reference computation of the website numbers from a
sync pull json, to cross check the page logic. Europe/Berlin."""
import json
import sys
from datetime import datetime, timedelta, date
from zoneinfo import ZoneInfo

TZ = ZoneInfo("Europe/Berlin")
data = json.load(open(sys.argv[1]))
active = lambda xs: [x for x in (xs or []) if not x.get("deleted_at")]

entries = active(data["time_entries"])
events = active(data["timetable_events"])
overrides = active(data["timetable_overrides"])
days = active(data["timetable_days"])
todos = active(data["timetable_todos"])
goals = active(data["subject_goals"])
acts = [a for a in active(data["activities"]) if not a.get("archived")]

def parse(ts):
    return datetime.fromisoformat(ts)

earliest = min(parse(e["started_at"]) for e in entries)
now = datetime.now(TZ)
frm = earliest.astimezone(TZ).replace(hour=0, minute=0, second=0, microsecond=0)
to = now.replace(hour=0, minute=0, second=0, microsecond=0) + timedelta(days=1)
print(f"window: {frm.date()} .. {to.date()-timedelta(days=1)}  (now {now:%H:%M})")

free = {d["date"] for d in days if d["free_day"]}
rows = []
for ev in events:
    cursor = frm
    attended = missed = upcoming = cancelled = 0
    detail = []
    while cursor < to:
        iso = cursor.isoweekday()
        if iso == int(ev["day_of_week"]):
            ds = cursor.strftime("%Y-%m-%d")
            ov = next((o for o in overrides if o["event_sync_id"] == ev["id"] and o["date"] == ds), None)
            if ov and ov.get("cancelled"):
                cancelled += 1; detail.append((ds, "x"))
            elif ds in free:
                cancelled += 1; detail.append((ds, "x"))
            else:
                sm = int(ov["start_time"]) if ov and ov.get("start_time") else int(ev["start_time"])
                em = int(ov["end_time"]) if ov and ov.get("end_time") else int(ev["end_time"])
                s = cursor.replace(hour=sm//60, minute=sm%60)
                e = cursor.replace(hour=em//60, minute=em%60)
                hit = any(
                    entries[i]["activity_id"] == ev["activity_sync_id"]
                    and parse(entries[i]["started_at"]) < e
                    and (parse(entries[i]["ended_at"]) if entries[i].get("ended_at") else now) > s
                    for i in range(len(entries))
                )
                if hit: attended += 1; detail.append((ds, "a"))
                elif e < now: missed += 1; detail.append((ds, "m"))
                elif s <= now: upcoming += 1; detail.append((ds, "r"))
                else: upcoming += 1; detail.append((ds, "u"))
        cursor += timedelta(days=1)
    rows.append((ev["name"], attended, missed, upcoming, cancelled, detail))
    print(f"{ev['name']:15s} attended={attended} missed={missed} upcoming={upcoming} cancelled={cancelled} {detail}")

sa = sum(r[1] for r in rows); sm = sum(r[2] for r in rows)
print(f"TOTAL past={sa+sm} attended={sa} rate={100*sa/(sa+sm) if sa+sm else 0:.0f}%")

# hours per activity in window
per = {}
for e in entries:
    st = parse(e["started_at"]); en = parse(e["ended_at"]) if e.get("ended_at") else now
    if st < to and en > frm:
        per[e["activity_id"]] = per.get(e["activity_id"], 0) + max(0, (en - st).total_seconds())
total = sum(per.values())
name = {a["id"]: a["name"] for a in acts}
print(f"total tracked: {total/3600:.1f} h")
for aid, secs in sorted(per.items(), key=lambda x: -x[1]):
    print(f"  {name.get(aid, aid[:8]):15s} {secs/3600:6.1f} h  {100*secs/total:3.0f}%")
for g in goals:
    aid = g["activity_sync_id"]
    print(f"goal {name.get(aid, aid[:8])}: tracked={per.get(aid,0)/3600:.1f}h target={g['target_seconds']/3600:.0f}h pct={100*per.get(aid,0)/g['target_seconds']:.0f}%")
print("todos:", {t: (sum(1 for x in todos if x["type"]==t and x["done"]), sum(1 for x in todos if x["type"]==t)) for t in (0,1,2)})

# hours per activity category
cats = active(data.get("categories"))
cat_name = {c["id"]: c["name"] for c in cats}
act_cats = {}
for a in acts:
    try: act_cats[a["id"]] = [c for c in json.loads(a.get("category") or "[]") if c in cat_name]
    except Exception: act_cats[a["id"]] = []
per_cat = {}
for e in entries:
    st = parse(e["started_at"]); en = parse(e["ended_at"]) if e.get("ended_at") else now
    if st < to and en > frm:
        secs = max(0, (en - st).total_seconds())
        for c in (act_cats.get(e["activity_id"]) or ["none"]):
            per_cat[c] = per_cat.get(c, 0) + secs
print("per category:")
for cid, secs in sorted(per_cat.items(), key=lambda x: -x[1]):
    print(f"  {cat_name.get(cid, 'Ohne Kategorie'):15s} {secs/3600:6.1f} h  {100*secs/total:3.0f}%")

# attendance per calendar week (monday based)
weeks = {}
for r in rows:
    for ds, status in r[5]:
        if status == "x": continue
        d = date.fromisoformat(ds)
        monday = d - timedelta(days=d.weekday())
        w = weeks.setdefault(monday, {"expected": 0, "attended": 0, "missed": 0, "upcoming": 0})
        w["expected"] += 1
        if status == "a": w["attended"] += 1
        elif status == "m": w["missed"] += 1
        else: w["upcoming"] += 1
print("per week:")
for monday in sorted(weeks):
    w = weeks[monday]
    rate = f"{100*w['attended']/(w['attended']+w['missed']):.0f}%" if w["attended"]+w["missed"] else "-"
    print(f"  {monday} KW{monday.isocalendar()[1]:02d}: expected={w['expected']} attended={w['attended']} missed={w['missed']} upcoming={w['upcoming']} rate={rate}")

# running slots right now
running = [(r[0], ds) for r in rows for ds, st in r[5] if st == "r"]
print("running slots:", running)
