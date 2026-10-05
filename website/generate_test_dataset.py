#!/usr/bin/env python3
"""Generates a rich, realistic 10-day test dataset for the TimeTracker analysis page.
Spans 10 days (ending today), with full timetables, overrides, todos, subject goals,
and rich tracked entries (Uni, everyday, leisure) including an active running timer.
"""
import json
import uuid
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo

TZ = ZoneInfo("Europe/Berlin")

def make_id():
    return str(uuid.uuid4())

def iso(dt):
    return dt.isoformat()

def generate(output_file="/tmp/test_dataset_10days.json"):
    now = datetime.now(TZ)
    # 10 days ending today: Day 0 = 9 days ago, Day 9 = today
    start_date = (now - timedelta(days=9)).replace(hour=0, minute=0, second=0, microsecond=0)

    # 1. Categories
    cat_uni_id = make_id()
    cat_alltag_id = make_id()
    cat_freizeit_id = make_id()

    categories = [
        {"id": cat_uni_id, "name": "Uni", "color_id": 10, "note": "Vorlesungen, Übungen, Lernen", "updated_at": iso(now)},
        {"id": cat_alltag_id, "name": "Alltag", "color_id": 15, "note": "Schlaf, Mahlzeiten, Haushalt", "updated_at": iso(now)},
        {"id": cat_freizeit_id, "name": "Freizeit", "color_id": 4, "note": "Sport, Lesen, Gaming", "updated_at": iso(now)},
    ]

    # 2. Activities
    act_analysis = {"id": make_id(), "name": "Analysis", "icon": "Analysis", "color_id": 10, "category": json.dumps([cat_uni_id]), "updated_at": iso(now)}
    act_thermo = {"id": make_id(), "name": "Thermodynamik", "icon": "Thermo", "color_id": 9, "category": json.dumps([cat_uni_id]), "updated_at": iso(now)}
    act_java = {"id": make_id(), "name": "Java", "icon": "Java", "color_id": 11, "category": json.dumps([cat_uni_id]), "updated_at": iso(now)}
    act_theoinfo = {"id": make_id(), "name": "TheoInfo", "icon": "TheoInfo", "color_id": 12, "category": json.dumps([cat_uni_id]), "updated_at": iso(now)}
    act_essen = {"id": make_id(), "name": "Essen", "icon": "ic_restaurant_24px", "color_id": 15, "category": json.dumps([cat_alltag_id]), "updated_at": iso(now)}
    act_pause = {"id": make_id(), "name": "Pause", "icon": "ic_free_breakfast_24px", "color_id": 14, "category": json.dumps([cat_alltag_id]), "updated_at": iso(now)}
    act_schlafen = {"id": make_id(), "name": "Schlafen", "icon": "ic_single_bed_24px", "color_id": 1, "category": json.dumps([cat_alltag_id]), "updated_at": iso(now)}
    act_haushalt = {"id": make_id(), "name": "Haushalt", "icon": "ic_cleaning_services_24px", "color_id": 17, "category": json.dumps([cat_alltag_id]), "updated_at": iso(now)}
    act_einkaufen = {"id": make_id(), "name": "Einkaufen", "icon": "ic_shopping_cart_24px", "color_id": 8, "category": json.dumps([cat_alltag_id]), "updated_at": iso(now)}
    act_sport = {"id": make_id(), "name": "Sport", "icon": "ic_fitness_center_24px", "color_id": 4, "category": json.dumps([cat_freizeit_id]), "updated_at": iso(now)}
    act_lesen = {"id": make_id(), "name": "Lesen", "icon": "ic_menu_book_24px", "color_id": 5, "category": json.dumps([cat_freizeit_id]), "updated_at": iso(now)}
    act_gaming = {"id": make_id(), "name": "Gaming", "icon": "ic_sports_esports_24px", "color_id": 3, "category": json.dumps([cat_freizeit_id]), "updated_at": iso(now)}

    activities = [
        act_analysis, act_thermo, act_java, act_theoinfo,
        act_essen, act_pause, act_schlafen, act_haushalt, act_einkaufen,
        act_sport, act_lesen, act_gaming
    ]

    # 3. Timetable events (Weekly schedule, ISO DayOfWeek: 1=Mo..7=So)
    ev_thermo_mo = {"id": make_id(), "name": "Thermodynamik", "day_of_week": 1, "start_time": 8 * 60 + 15, "end_time": 9 * 60 + 45, "room": "HS 1", "type": 0, "activity_sync_id": act_thermo["id"], "updated_at": iso(now)}
    ev_analysis_mo = {"id": make_id(), "name": "Analysis", "day_of_week": 1, "start_time": 10 * 60 + 15, "end_time": 11 * 60 + 45, "room": "HS 2", "type": 0, "activity_sync_id": act_analysis["id"], "updated_at": iso(now)}
    ev_java_mo = {"id": make_id(), "name": "Java", "day_of_week": 1, "start_time": 14 * 60 + 15, "end_time": 15 * 60 + 45, "room": "HS 3", "type": 0, "activity_sync_id": act_java["id"], "updated_at": iso(now)}

    ev_java_di = {"id": make_id(), "name": "Java", "day_of_week": 2, "start_time": 8 * 60 + 15, "end_time": 9 * 60 + 45, "room": "R 0.014", "type": 1, "activity_sync_id": act_java["id"], "updated_at": iso(now)}
    ev_thermo_di = {"id": make_id(), "name": "Thermodynamik", "day_of_week": 2, "start_time": 12 * 60, "end_time": 13 * 60 + 30, "room": "R 2.104", "type": 1, "activity_sync_id": act_thermo["id"], "updated_at": iso(now)}

    ev_theoinfo_mi = {"id": make_id(), "name": "TheoInfo", "day_of_week": 3, "start_time": 9 * 60 + 15, "end_time": 10 * 60 + 45, "room": "HS 1", "type": 0, "activity_sync_id": act_theoinfo["id"], "updated_at": iso(now)}
    ev_analysis_mi = {"id": make_id(), "name": "Analysis", "day_of_week": 3, "start_time": 11 * 60 + 15, "end_time": 12 * 60 + 45, "room": "R 1.05", "type": 1, "activity_sync_id": act_analysis["id"], "updated_at": iso(now)}

    ev_thermo_do = {"id": make_id(), "name": "Thermodynamik", "day_of_week": 4, "start_time": 8 * 60 + 15, "end_time": 9 * 60 + 45, "room": "HS 1", "type": 0, "activity_sync_id": act_thermo["id"], "updated_at": iso(now)}
    ev_theoinfo_do = {"id": make_id(), "name": "TheoInfo", "day_of_week": 4, "start_time": 14 * 60 + 15, "end_time": 15 * 60 + 45, "room": "R 2.201", "type": 1, "activity_sync_id": act_theoinfo["id"], "updated_at": iso(now)}

    ev_analysis_fr = {"id": make_id(), "name": "Analysis", "day_of_week": 5, "start_time": 10 * 60 + 15, "end_time": 11 * 60 + 45, "room": "HS 2", "type": 0, "activity_sync_id": act_analysis["id"], "updated_at": iso(now)}
    ev_java_fr = {"id": make_id(), "name": "Java", "day_of_week": 5, "start_time": 13 * 60, "end_time": 14 * 60 + 30, "room": "R 0.014", "type": 2, "activity_sync_id": act_java["id"], "updated_at": iso(now)}

    timetable_events = [
        ev_thermo_mo, ev_analysis_mo, ev_java_mo,
        ev_java_di, ev_thermo_di,
        ev_theoinfo_mi, ev_analysis_mi,
        ev_thermo_do, ev_theoinfo_do,
        ev_analysis_fr, ev_java_fr,
    ]

    # 4. Overrides: One cancelled lecture, one room override
    override_date_di = (start_date + timedelta(days=2)).strftime("%Y-%m-%d")
    override_date_mi = (start_date + timedelta(days=3)).strftime("%Y-%m-%d")
    timetable_overrides = [
        {"id": make_id(), "event_sync_id": ev_thermo_di["id"], "date": override_date_di, "cancelled": True, "comment": "Dozent krank", "updated_at": iso(now)},
        {"id": make_id(), "event_sync_id": ev_analysis_mi["id"], "date": override_date_mi, "cancelled": False, "room": "Audimax", "comment": "Raumverlegung", "updated_at": iso(now)},
    ]

    # 5. Timetable Days (free days / holidays)
    free_day_date = (start_date + timedelta(days=7)).strftime("%Y-%m-%d")
    timetable_days = [
        {"id": make_id(), "date": free_day_date, "free_day": False, "name": "Semestertag", "updated_at": iso(now)},
    ]

    # 6. Todos (Vorbereitung: 0, Nachbereitung: 1, Todo: 2)
    timetable_todos = [
        {"id": make_id(), "event_sync_id": ev_analysis_mo["id"], "date": (start_date + timedelta(days=1)).strftime("%Y-%m-%d"), "text": "Skript Kapitel 3 lesen", "type": 0, "done": True, "updated_at": iso(now)},
        {"id": make_id(), "event_sync_id": ev_thermo_mo["id"], "date": (start_date + timedelta(days=1)).strftime("%Y-%m-%d"), "text": "Übungsblatt 2 Aufgaben 1-3 vorbereiten", "type": 0, "done": True, "updated_at": iso(now)},
        {"id": make_id(), "event_sync_id": ev_java_mo["id"], "date": (start_date + timedelta(days=1)).strftime("%Y-%m-%d"), "text": "JDK 21 Setup verifizieren", "type": 0, "done": True, "updated_at": iso(now)},
        {"id": make_id(), "event_sync_id": ev_theoinfo_mi["id"], "date": (start_date + timedelta(days=3)).strftime("%Y-%m-%d"), "text": "Automaten-Beispiele nacharbeiten", "type": 1, "done": True, "updated_at": iso(now)},
        {"id": make_id(), "event_sync_id": ev_analysis_mi["id"], "date": (start_date + timedelta(days=3)).strftime("%Y-%m-%d"), "text": "Beweis Satz 4.2 rekapitulieren", "type": 1, "done": False, "updated_at": iso(now)},
        {"id": make_id(), "event_sync_id": ev_thermo_do["id"], "date": (start_date + timedelta(days=4)).strftime("%Y-%m-%d"), "text": "Formelsammlung Thermodynamik drucken", "type": 2, "done": True, "updated_at": iso(now)},
        {"id": make_id(), "event_sync_id": ev_java_fr["id"], "date": now.strftime("%Y-%m-%d"), "text": "Git Push für Übungsaufgabe 3", "type": 2, "done": False, "updated_at": iso(now)},
        {"id": make_id(), "event_sync_id": ev_analysis_fr["id"], "date": now.strftime("%Y-%m-%d"), "text": "Klausuranmeldung im Portal prüfen", "type": 2, "done": False, "updated_at": iso(now)},
    ]

    # 7. Goals
    subject_goals = [
        {"id": make_id(), "activity_sync_id": act_analysis["id"], "target_seconds": 150 * 3600, "updated_at": iso(now)}, # 5 ECTS ≈ 150h
        {"id": make_id(), "activity_sync_id": act_thermo["id"], "target_seconds": 150 * 3600, "updated_at": iso(now)},
        {"id": make_id(), "activity_sync_id": act_java["id"], "target_seconds": 150 * 3600, "updated_at": iso(now)},
        {"id": make_id(), "activity_sync_id": act_theoinfo["id"], "target_seconds": 150 * 3600, "updated_at": iso(now)},
    ]

    # 8. Time entries across 10 days
    time_entries = []

    for day_idx in range(10):
        day_date = start_date + timedelta(days=day_idx)
        iso_dow = day_date.isoweekday() # 1=Mo..7=So
        is_today = (day_idx == 9)

        # Night sleep: 23:30 (prev day) to 07:15
        s_sleep = day_date.replace(hour=0, minute=0)
        e_sleep = day_date.replace(hour=7, minute=15)
        dur_sleep = int((e_sleep - s_sleep).total_seconds())
        time_entries.append({
            "id": make_id(), "activity_id": act_schlafen["id"],
            "started_at": iso(s_sleep), "ended_at": iso(e_sleep), "duration_seconds": dur_sleep,
            "comment": "Gut geschlafen", "tags": "", "updated_at": iso(now)
        })

        # Breakfast: 07:30 to 08:00
        s_fr = day_date.replace(hour=7, minute=30)
        e_fr = day_date.replace(hour=8, minute=0)
        time_entries.append({
            "id": make_id(), "activity_id": act_essen["id"],
            "started_at": iso(s_fr), "ended_at": iso(e_fr), "duration_seconds": 1800,
            "comment": "Frühstück & Kaffee", "tags": "", "updated_at": iso(now)
        })

        # Weekday lectures & tracking
        if iso_dow == 1: # Monday
            # Thermo lecture: 08:15 to 09:45
            s_th = day_date.replace(hour=8, minute=15); e_th = day_date.replace(hour=9, minute=45)
            time_entries.append({"id": make_id(), "activity_id": act_thermo["id"], "started_at": iso(s_th), "ended_at": iso(e_th), "duration_seconds": 5400, "comment": "HS 1 Vorlesung", "tags": "", "updated_at": iso(now)})
            # Analysis lecture: 10:15 to 11:45
            s_an = day_date.replace(hour=10, minute=15); e_an = day_date.replace(hour=11, minute=45)
            time_entries.append({"id": make_id(), "activity_id": act_analysis["id"], "started_at": iso(s_an), "ended_at": iso(e_an), "duration_seconds": 5400, "comment": "HS 2 Folgen & Reihen", "tags": "", "updated_at": iso(now)})
            # Java lecture: 14:15 to 15:45
            s_ja = day_date.replace(hour=14, minute=15); e_ja = day_date.replace(hour=15, minute=45)
            time_entries.append({"id": make_id(), "activity_id": act_java["id"], "started_at": iso(s_ja), "ended_at": iso(e_ja), "duration_seconds": 5400, "comment": "Streams & Lambdas", "tags": "", "updated_at": iso(now)})

        elif iso_dow == 2: # Tuesday
            # Java exercise: 08:15 to 09:45
            s_ja = day_date.replace(hour=8, minute=15); e_ja = day_date.replace(hour=9, minute=45)
            time_entries.append({"id": make_id(), "activity_id": act_java["id"], "started_at": iso(s_ja), "ended_at": iso(e_ja), "duration_seconds": 5400, "comment": "Übungszettel Besprechung", "tags": "", "updated_at": iso(now)})
            # Thermo was cancelled on Tuesday!

        elif iso_dow == 3: # Wednesday
            # TheoInfo: 09:15 to 10:45
            s_ti = day_date.replace(hour=9, minute=15); e_ti = day_date.replace(hour=10, minute=45)
            time_entries.append({"id": make_id(), "activity_id": act_theoinfo["id"], "started_at": iso(s_ti), "ended_at": iso(e_ti), "duration_seconds": 5400, "comment": "DFA & NFA", "tags": "", "updated_at": iso(now)})
            # Analysis exercise: 11:15 to 12:45
            s_an = day_date.replace(hour=11, minute=15); e_an = day_date.replace(hour=12, minute=45)
            time_entries.append({"id": make_id(), "activity_id": act_analysis["id"], "started_at": iso(s_an), "ended_at": iso(e_an), "duration_seconds": 5400, "comment": "Audimax Übung", "tags": "", "updated_at": iso(now)})

        elif iso_dow == 4: # Thursday
            # Thermo: 08:15 to 09:45
            s_th = day_date.replace(hour=8, minute=15); e_th = day_date.replace(hour=9, minute=45)
            time_entries.append({"id": make_id(), "activity_id": act_thermo["id"], "started_at": iso(s_th), "ended_at": iso(e_th), "duration_seconds": 5400, "comment": "Hauptsätze", "tags": "", "updated_at": iso(now)})
            # TheoInfo exercise: 14:15 to 15:45
            s_ti = day_date.replace(hour=14, minute=15); e_ti = day_date.replace(hour=15, minute=45)
            time_entries.append({"id": make_id(), "activity_id": act_theoinfo["id"], "started_at": iso(s_ti), "ended_at": iso(e_ti), "duration_seconds": 5400, "comment": "Pumping Lemma", "tags": "", "updated_at": iso(now)})

        elif iso_dow == 5: # Friday
            # Analysis: 10:15 to 11:45
            s_an = day_date.replace(hour=10, minute=15); e_an = day_date.replace(hour=11, minute=45)
            time_entries.append({"id": make_id(), "activity_id": act_analysis["id"], "started_at": iso(s_an), "ended_at": iso(e_an), "duration_seconds": 5400, "comment": "Stetigkeit", "tags": "", "updated_at": iso(now)})
            # Java Tutorium: 13:00 to 14:30
            s_ja = day_date.replace(hour=13, minute=0); e_ja = day_date.replace(hour=14, minute=30)
            time_entries.append({"id": make_id(), "activity_id": act_java["id"], "started_at": iso(s_ja), "ended_at": iso(e_ja), "duration_seconds": 5400, "comment": "Tutorium", "tags": "", "updated_at": iso(now)})

        # Lunch: 12:45 to 13:30 (if not today or earlier)
        if not is_today or now.hour >= 13:
            s_lu = day_date.replace(hour=12, minute=45); e_lu = day_date.replace(hour=13, minute=30)
            time_entries.append({"id": make_id(), "activity_id": act_essen["id"], "started_at": iso(s_lu), "ended_at": iso(e_lu), "duration_seconds": 2700, "comment": "Mensa Mittagessen", "tags": "", "updated_at": iso(now)})

        # Afternoon Coffee Pause: 15:50 to 16:15
        if not is_today or now.hour >= 16:
            s_co = day_date.replace(hour=15, minute=50); e_co = day_date.replace(hour=16, minute=15)
            time_entries.append({"id": make_id(), "activity_id": act_pause["id"], "started_at": iso(s_co), "ended_at": iso(e_co), "duration_seconds": 1500, "comment": "Kaffeepause", "tags": "", "updated_at": iso(now)})

        # Sport / Workout on even days: 17:00 to 18:15
        if day_idx % 2 == 0 and (not is_today or now.hour >= 18):
            s_sp = day_date.replace(hour=17, minute=0); e_sp = day_date.replace(hour=18, minute=15)
            time_entries.append({"id": make_id(), "activity_id": act_sport["id"], "started_at": iso(s_sp), "ended_at": iso(e_sp), "duration_seconds": 4500, "comment": "Gym Workout & Laufen", "tags": "", "updated_at": iso(now)})

        # Dinner: 19:00 to 19:45
        if not is_today or now.hour >= 20:
            s_di = day_date.replace(hour=19, minute=0); e_di = day_date.replace(hour=19, minute=45)
            time_entries.append({"id": make_id(), "activity_id": act_essen["id"], "started_at": iso(s_di), "ended_at": iso(e_di), "duration_seconds": 2700, "comment": "Abendessen", "tags": "", "updated_at": iso(now)})

        # Evening Leisure: Lesen or Gaming (20:30 to 22:00)
        if not is_today or now.hour >= 22:
            act_lei = act_lesen if day_idx % 2 == 1 else act_gaming
            s_le = day_date.replace(hour=20, minute=30); e_le = day_date.replace(hour=22, minute=0)
            time_entries.append({"id": make_id(), "activity_id": act_lei["id"], "started_at": iso(s_le), "ended_at": iso(e_le), "duration_seconds": 5400, "comment": "Feierabend", "tags": "", "updated_at": iso(now)})

    # On today (the 10th day): Add an active currently-running timer!
    # Running for the last 38 minutes on Analysis
    start_running = now - timedelta(minutes=38)
    time_entries.append({
        "id": make_id(),
        "activity_id": act_analysis["id"],
        "started_at": iso(start_running),
        "ended_at": None,
        "duration_seconds": 0,
        "comment": "Übungsblatt 4 lösen",
        "tags": "",
        "updated_at": iso(now)
    })

    payload = {
        "time_entries": time_entries,
        "activities": activities,
        "categories": categories,
        "timetable_events": timetable_events,
        "timetable_overrides": timetable_overrides,
        "timetable_days": timetable_days,
        "timetable_todos": timetable_todos,
        "subject_goals": subject_goals,
        "tags": [],
        "server_time": iso(now),
    }

    with open(output_file, "w", encoding="utf-8") as f:
        json.dump(payload, f, indent=2, ensure_ascii=False)

    print(f"Generated 10-day test dataset with {len(time_entries)} time entries across {len(activities)} activities.")
    print(f"Saved to: {output_file}")
    return payload

if __name__ == "__main__":
    import sys
    target = sys.argv[1] if len(sys.argv) > 1 else "/tmp/test_dataset_10days.json"
    generate(target)
