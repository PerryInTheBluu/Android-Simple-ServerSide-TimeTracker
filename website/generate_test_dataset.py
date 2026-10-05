#!/usr/bin/env python3
"""Generates an extensive, realistic 10-day test dataset for the TimeTracker analysis page.
Spans 10 days (ending today), with 24 activities across 5 categories, full timetables (V, Ü, T),
overrides (cancellations, room changes), free days, deadline-sorted todos, subject goals,
and rich edge cases (micro-slots, concurrent collisions, out-of-hours, midnight crossing,
retro vs live attendance, pure self-study without events, and active running timer).
"""
import json
import os
import sys
import uuid
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo

TZ = ZoneInfo("Europe/Berlin")

def make_id():
    return str(uuid.uuid4())

def iso(dt):
    return dt.isoformat()

def generate(output_file=None):
    if not output_file:
        script_dir = os.path.dirname(os.path.abspath(__file__))
        output_file = os.path.join(script_dir, "test_dataset_10days.json")

    now = datetime.now(TZ)
    # 10 days window ending today: Day 0 = 9 days ago, Day 9 = today
    start_date = (now - timedelta(days=9)).replace(hour=0, minute=0, second=0, microsecond=0)

    # -------------------------------------------------------------------------
    # 1. Categories (5 categories)
    # -------------------------------------------------------------------------
    cat_uni = {"id": make_id(), "name": "Uni", "color_id": 10, "note": "Vorlesungen, Übungen, Tutorien, Lernen", "updated_at": iso(now)}
    cat_alltag = {"id": make_id(), "name": "Alltag", "color_id": 15, "note": "Schlaf, Mahlzeiten, Haushalt, Hygiene", "updated_at": iso(now)}
    cat_freizeit = {"id": make_id(), "name": "Freizeit", "color_id": 4, "note": "Sport, Musik, Lesen, Hobbys", "updated_at": iso(now)}
    cat_essen = {"id": make_id(), "name": "Essen", "color_id": 16, "note": "Frühstück, Mittagessen, Abendessen, Kochen", "updated_at": iso(now)}
    cat_zeit = {"id": make_id(), "name": "Zeitverschwendung", "color_id": 3, "note": "Social Media, Gaming, Prokrastination", "updated_at": iso(now)}

    categories = [cat_uni, cat_alltag, cat_freizeit, cat_essen, cat_zeit]

    # -------------------------------------------------------------------------
    # 2. Activities (24 activities with wide color spectrum & contrast tests)
    # -------------------------------------------------------------------------
    # Uni subjects (all get mortarboard diploma icon)
    act_thermo = {"id": make_id(), "name": "thermo", "icon": "diploma", "color_id": 9, "category": json.dumps([cat_uni["id"]]), "updated_at": iso(now)} # Teal #009688
    act_get2 = {"id": make_id(), "name": "get2", "icon": "diploma", "color_id": 6, "category": json.dumps([cat_uni["id"]]), "updated_at": iso(now)} # Blue #2196F3
    act_ente = {"id": make_id(), "name": "ente", "icon": "diploma", "color_id": 10, "category": json.dumps([cat_uni["id"]]), "updated_at": iso(now)} # Green #4CAF50
    act_wust = {"id": make_id(), "name": "wust", "icon": "diploma", "color_id": 4, "category": json.dumps([cat_uni["id"]]), "updated_at": iso(now)} # Deep Purple #673AB7
    act_stromi = {"id": make_id(), "name": "strömi", "icon": "diploma", "color_id": 18, "category": json.dumps([cat_uni["id"]]), "updated_at": iso(now)} # Slate Gray #607D8B
    act_mess = {"id": make_id(), "name": "Mess", "icon": "diploma", "color_id": 12, "category": json.dumps([cat_uni["id"]]), "updated_at": iso(now)} # Lime #CDDC39 (Bright contrast test!)
    act_java = {"id": make_id(), "name": "java", "icon": "diploma", "color_id": 11, "category": json.dumps([cat_uni["id"]]), "updated_at": iso(now)} # Light Green #8BC34A (Bright contrast test!)

    # Everyday & Hygiene
    act_sleep = {"id": make_id(), "name": "sleep", "icon": "sleep", "color_id": 1, "category": json.dumps([cat_alltag["id"]]), "updated_at": iso(now)} # Dark Red #F53639
    act_bio = {"id": make_id(), "name": "bio", "icon": "bio", "color_id": 14, "category": json.dumps([cat_alltag["id"]]), "updated_at": iso(now)} # Amber #FFC107
    act_chores = {"id": make_id(), "name": "chores", "icon": "chores", "color_id": 17, "category": json.dumps([cat_alltag["id"]]), "updated_at": iso(now)} # Brown #785248
    act_commute = {"id": make_id(), "name": "commute", "icon": "commute", "color_id": 8, "category": json.dumps([cat_alltag["id"]]), "updated_at": iso(now)} # Cyan #00BCD4
    act_pause = {"id": make_id(), "name": "pause", "icon": "pause", "color_id": 14, "category": json.dumps([cat_alltag["id"]]), "updated_at": iso(now)}

    # Meals
    act_breakfast = {"id": make_id(), "name": "breakfast", "icon": "breakfast", "color_id": 14, "category": json.dumps([cat_alltag["id"], cat_essen["id"]]), "updated_at": iso(now)}
    act_lunch = {"id": make_id(), "name": "lunch", "icon": "lunch", "color_id": 15, "category": json.dumps([cat_alltag["id"], cat_essen["id"]]), "updated_at": iso(now)} # Orange #FF9800
    act_dinner = {"id": make_id(), "name": "dinner", "icon": "dinner", "color_id": 16, "category": json.dumps([cat_alltag["id"], cat_essen["id"]]), "updated_at": iso(now)} # Deep Orange #FF5722
    act_cooking = {"id": make_id(), "name": "cooking", "icon": "cooking", "color_id": 15, "category": json.dumps([cat_alltag["id"], cat_essen["id"]]), "updated_at": iso(now)}

    # Leisure & Hobbies
    act_exercise = {"id": make_id(), "name": "exercise", "icon": "exercise", "color_id": 4, "category": json.dumps([cat_freizeit["id"]]), "updated_at": iso(now)}
    act_egitarre = {"id": make_id(), "name": "egitarre", "icon": "egitarre", "color_id": 16, "category": json.dumps([cat_freizeit["id"]]), "updated_at": iso(now)}
    act_read = {"id": make_id(), "name": "read", "icon": "read", "color_id": 5, "category": json.dumps([cat_freizeit["id"]]), "updated_at": iso(now)} # Indigo #3F51B5
    act_tinkering = {"id": make_id(), "name": "tinkering", "icon": "tinkering", "color_id": 13, "category": json.dumps([cat_freizeit["id"]]), "updated_at": iso(now)} # Yellow #FFEB3B (Bright contrast test!)
    act_social = {"id": make_id(), "name": "social", "icon": "social", "color_id": 7, "category": json.dumps([cat_freizeit["id"]]), "updated_at": iso(now)} # Light Blue #03A9F4
    act_oma = {"id": make_id(), "name": "oma besucht diesen monat", "icon": "oma", "color_id": 6, "category": json.dumps([cat_freizeit["id"]]), "updated_at": iso(now)}

    # Time wasters
    act_youtube = {"id": make_id(), "name": "youtube", "icon": "youtube", "color_id": 2, "category": json.dumps([cat_zeit["id"]]), "updated_at": iso(now)} # Pink #E91E63
    act_games = {"id": make_id(), "name": "games", "icon": "games", "color_id": 3, "category": json.dumps([cat_zeit["id"]]), "updated_at": iso(now)} # Purple #9C27B0

    activities = [
        act_thermo, act_get2, act_ente, act_wust, act_stromi, act_mess, act_java,
        act_sleep, act_bio, act_chores, act_commute, act_pause,
        act_breakfast, act_lunch, act_dinner, act_cooking,
        act_exercise, act_egitarre, act_read, act_tinkering, act_social, act_oma,
        act_youtube, act_games
    ]

    # -------------------------------------------------------------------------
    # 3. Timetable events (Types: 0 = Vorlesung / V, 1 = Übung / Ü, 2 = Tutorium / T)
    # -------------------------------------------------------------------------
    ev_thermo_v_mo = {"id": make_id(), "name": "thermo", "day_of_week": 1, "start_time": 8 * 60 + 15, "end_time": 9 * 60 + 45, "room": "HS 1", "type": 0, "activity_sync_id": act_thermo["id"], "updated_at": iso(now)}
    ev_get2_v_mo = {"id": make_id(), "name": "get2", "day_of_week": 1, "start_time": 10 * 60 + 15, "end_time": 11 * 60 + 45, "room": "HS 2", "type": 0, "activity_sync_id": act_get2["id"], "updated_at": iso(now)}
    ev_java_v_mo = {"id": make_id(), "name": "java", "day_of_week": 1, "start_time": 14 * 60 + 15, "end_time": 15 * 60 + 45, "room": "HS 3", "type": 0, "activity_sync_id": act_java["id"], "updated_at": iso(now)}

    ev_get2_u_di = {"id": make_id(), "name": "get2", "day_of_week": 2, "start_time": 8 * 60 + 15, "end_time": 9 * 60 + 45, "room": "R 0.014", "type": 1, "activity_sync_id": act_get2["id"], "updated_at": iso(now)}
    ev_ente_v_di = {"id": make_id(), "name": "ente", "day_of_week": 2, "start_time": 12 * 60, "end_time": 13 * 60 + 30, "room": "HS 1", "type": 0, "activity_sync_id": act_ente["id"], "updated_at": iso(now)}

    ev_wust_v_mi = {"id": make_id(), "name": "wust", "day_of_week": 3, "start_time": 9 * 60 + 15, "end_time": 10 * 60 + 45, "room": "HS 1", "type": 0, "activity_sync_id": act_wust["id"], "updated_at": iso(now)}
    ev_java_u_mi = {"id": make_id(), "name": "java", "day_of_week": 3, "start_time": 14 * 60 + 15, "end_time": 15 * 60 + 45, "room": "R 1.05", "type": 1, "activity_sync_id": act_java["id"], "updated_at": iso(now)}

    ev_thermo_u_do = {"id": make_id(), "name": "thermo", "day_of_week": 4, "start_time": 8 * 60 + 15, "end_time": 9 * 60 + 45, "room": "R 2.104", "type": 1, "activity_sync_id": act_thermo["id"], "updated_at": iso(now)}
    ev_wust_u_do = {"id": make_id(), "name": "wust", "day_of_week": 4, "start_time": 14 * 60 + 15, "end_time": 15 * 60 + 45, "room": "R 2.201", "type": 1, "activity_sync_id": act_wust["id"], "updated_at": iso(now)}

    ev_mess_v_fr = {"id": make_id(), "name": "Mess", "day_of_week": 5, "start_time": 10 * 60 + 15, "end_time": 11 * 60 + 45, "room": "HS 3", "type": 0, "activity_sync_id": act_mess["id"], "updated_at": iso(now)}
    ev_thermo_t_fr = {"id": make_id(), "name": "thermo", "day_of_week": 5, "start_time": 13 * 60, "end_time": 14 * 60 + 30, "room": "R 0.014", "type": 2, "activity_sync_id": act_thermo["id"], "updated_at": iso(now)}

    timetable_events = [
        ev_thermo_v_mo, ev_get2_v_mo, ev_java_v_mo,
        ev_get2_u_di, ev_ente_v_di,
        ev_wust_v_mi, ev_java_u_mi,
        ev_thermo_u_do, ev_wust_u_do,
        ev_mess_v_fr, ev_thermo_t_fr,
    ]

    # -------------------------------------------------------------------------
    # 4. Overrides (Cancellation, Room change, Time shift)
    # -------------------------------------------------------------------------
    ov_date_di = (start_date + timedelta(days=2)).strftime("%Y-%m-%d") # 2026-09-29
    ov_date_mi = (start_date + timedelta(days=3)).strftime("%Y-%m-%d") # 2026-09-30
    ov_date_do = (start_date + timedelta(days=4)).strftime("%Y-%m-%d") # 2026-10-01
    timetable_overrides = [
        {"id": make_id(), "event_sync_id": ev_ente_v_di["id"], "date": ov_date_di, "cancelled": True, "comment": "Dozent auf Fachtagung", "updated_at": iso(now)},
        {"id": make_id(), "event_sync_id": ev_wust_v_mi["id"], "date": ov_date_mi, "cancelled": False, "room": "Audimax", "comment": "Hörsaalwechsel wegen Bauarbeiten", "updated_at": iso(now)},
        {"id": make_id(), "event_sync_id": ev_thermo_u_do["id"], "date": ov_date_do, "cancelled": False, "start_time": 8 * 60 + 30, "end_time": 10 * 60, "room": "R 2.104", "comment": "30 Min verschoben", "updated_at": iso(now)},
    ]

    # -------------------------------------------------------------------------
    # 5. Timetable Days (Holiday / free day)
    # -------------------------------------------------------------------------
    free_day_date = (start_date + timedelta(days=7)).strftime("%Y-%m-%d") # Sunday/Holiday
    timetable_days = [
        {"id": make_id(), "date": free_day_date, "free_day": True, "name": "Vorlesungsfreier Hochschulsporttag", "updated_at": iso(now)},
    ]

    # -------------------------------------------------------------------------
    # 6. Todos (0 = Vorbereitung, 1 = Nachbereitung, 2 = Todo)
    # -------------------------------------------------------------------------
    timetable_todos = [
        # Vorbereitung (Type 0)
        {"id": make_id(), "event_sync_id": ev_thermo_v_mo["id"], "date": (start_date + timedelta(days=1)).strftime("%Y-%m-%d"), "text": "Zustandsgleichung idealer Gase wiederholen", "type": 0, "done": True, "updated_at": iso(now)},
        {"id": make_id(), "event_sync_id": ev_thermo_v_mo["id"], "date": (now + timedelta(days=1)).strftime("%Y-%m-%d"), "text": "Skript Kapitel 3 durcharbeiten", "type": 0, "done": False, "updated_at": iso(now)},
        {"id": make_id(), "event_sync_id": ev_thermo_u_do["id"], "date": (now + timedelta(days=2)).strftime("%Y-%m-%d"), "text": "Übungsblatt 4 Aufgaben 1-3 rechnen", "type": 0, "done": False, "updated_at": iso(now)},
        {"id": make_id(), "event_sync_id": ev_thermo_t_fr["id"], "date": (now + timedelta(days=4)).strftime("%Y-%m-%d"), "text": "Fragen zu Aufgabe 2 für Tutor notieren", "type": 0, "done": False, "updated_at": iso(now)},
        {"id": make_id(), "event_sync_id": ev_java_v_mo["id"], "date": (now + timedelta(days=6)).strftime("%Y-%m-%d"), "text": "Generics & Bounded Type Parameters lesen", "type": 0, "done": False, "updated_at": iso(now)},

        # Nachbereitung (Type 1)
        {"id": make_id(), "event_sync_id": ev_ente_v_di["id"], "date": (start_date + timedelta(days=1)).strftime("%Y-%m-%d"), "text": "BHKW Kenndaten recherchieren", "type": 1, "done": True, "updated_at": iso(now)},
        {"id": make_id(), "event_sync_id": ev_get2_v_mo["id"], "date": (now + timedelta(days=3)).strftime("%Y-%m-%d"), "text": "Formelsammlung Wechselstrom ergänzen", "type": 1, "done": False, "updated_at": iso(now)},
        {"id": make_id(), "event_sync_id": ev_java_u_mi["id"], "date": (now + timedelta(days=5)).strftime("%Y-%m-%d"), "text": "JUnit Testfälle für Stack-Implementierung abgeben", "type": 1, "done": False, "updated_at": iso(now)},

        # Todos (Type 2)
        {"id": make_id(), "event_sync_id": ev_mess_v_fr["id"], "date": (start_date + timedelta(days=4)).strftime("%Y-%m-%d"), "text": "Oszilloskop Kalibrierungsprotokoll abgeben", "type": 2, "done": True, "updated_at": iso(now)},
        {"id": make_id(), "event_sync_id": ev_wust_u_do["id"], "date": (now + timedelta(days=5)).strftime("%Y-%m-%d"), "text": "R-Skript zur Chi-Quadrat-Verteilung testen", "type": 2, "done": False, "updated_at": iso(now)},
        {"id": make_id(), "event_sync_id": ev_thermo_v_mo["id"], "date": "", "text": "Klausuranmeldung im Prüfungsamt verifizieren", "type": 2, "done": False, "updated_at": iso(now)},
    ]

    # -------------------------------------------------------------------------
    # 7. Subject Goals (ECTS targets)
    # -------------------------------------------------------------------------
    subject_goals = [
        {"id": make_id(), "activity_sync_id": act_thermo["id"], "target_seconds": 150 * 3600, "updated_at": iso(now)},
        {"id": make_id(), "activity_sync_id": act_get2["id"], "target_seconds": 150 * 3600, "updated_at": iso(now)},
        {"id": make_id(), "activity_sync_id": act_ente["id"], "target_seconds": 150 * 3600, "updated_at": iso(now)},
        {"id": make_id(), "activity_sync_id": act_wust["id"], "target_seconds": 150 * 3600, "updated_at": iso(now)},
        {"id": make_id(), "activity_sync_id": act_stromi["id"], "target_seconds": 120 * 3600, "updated_at": iso(now)}, # 4 ECTS pure self-study!
        {"id": make_id(), "activity_sync_id": act_mess["id"], "target_seconds": 90 * 3600, "updated_at": iso(now)},
        {"id": make_id(), "activity_sync_id": act_java["id"], "target_seconds": 150 * 3600, "updated_at": iso(now)},
    ]

    # -------------------------------------------------------------------------
    # 8. Time Entries across 10 Days (Extensive Edge Cases)
    # -------------------------------------------------------------------------
    time_entries = []

    for day_idx in range(10):
        day_date = start_date + timedelta(days=day_idx)
        iso_dow = day_date.isoweekday() # 1=Mo..7=So
        is_today = (day_idx == 9)

        # A) Sleep: 00:00 to 07:15
        s_sl = day_date.replace(hour=0, minute=0); e_sl = day_date.replace(hour=7, minute=15)
        time_entries.append({
            "id": make_id(), "activity_id": act_sleep["id"],
            "started_at": iso(s_sl), "ended_at": iso(e_sl), "duration_seconds": int((e_sl - s_sl).total_seconds()),
            "comment": "Schlafphase & Erholung", "tags": "gesundheit", "created_at": iso(s_sl), "updated_at": iso(now)
        })

        # B) Breakfast: 07:30 to 07:55
        s_bf = day_date.replace(hour=7, minute=30); e_bf = day_date.replace(hour=7, minute=55)
        time_entries.append({
            "id": make_id(), "activity_id": act_breakfast["id"],
            "started_at": iso(s_bf), "ended_at": iso(e_bf), "duration_seconds": 1500,
            "comment": "Müsli, Obst & schwarzer Kaffee", "tags": "essen,routine", "created_at": iso(s_bf), "updated_at": iso(now)
        })

        # C) Morning Commute on weekdays: 07:55 to 08:12
        if iso_dow <= 5:
            s_cm = day_date.replace(hour=7, minute=55); e_cm = day_date.replace(hour=8, minute=12)
            time_entries.append({
                "id": make_id(), "activity_id": act_commute["id"],
                "started_at": iso(s_cm), "ended_at": iso(e_cm), "duration_seconds": 1020,
                "comment": "U-Bahn Linie 3 zum Campus", "tags": "unterwegs", "created_at": iso(s_cm), "updated_at": iso(now)
            })

        # D) Weekday Academic Tracking
        if iso_dow == 1: # Monday (Day 1 or Day 8)
            if day_idx == 1:
                # Live attendance thermo Vorlesung
                s_th = day_date.replace(hour=8, minute=15); e_th = day_date.replace(hour=9, minute=45)
                time_entries.append({"id": make_id(), "activity_id": act_thermo["id"], "started_at": iso(s_th), "ended_at": iso(e_th), "duration_seconds": 5400, "comment": "HS 1: 1. Hauptsatz der Thermodynamik", "tags": "vorlesung,wichtig", "created_at": iso(s_th), "updated_at": iso(now)})
            elif day_idx == 8:
                # Retroactive attendance thermo Vorlesung (created 5 hours later!)
                s_th = day_date.replace(hour=8, minute=15); e_th = day_date.replace(hour=9, minute=45)
                c_retro = day_date.replace(hour=15, minute=30)
                time_entries.append({"id": make_id(), "activity_id": act_thermo["id"], "started_at": iso(s_th), "ended_at": iso(e_th), "duration_seconds": 5400, "comment": "Aufzeichnung nachgearbeitet", "tags": "nachgetragen", "created_at": iso(c_retro), "updated_at": iso(now)})

            # Live attendance get2 Vorlesung
            s_ge = day_date.replace(hour=10, minute=15); e_ge = day_date.replace(hour=11, minute=45)
            time_entries.append({"id": make_id(), "activity_id": act_get2["id"], "started_at": iso(s_ge), "ended_at": iso(e_ge), "duration_seconds": 5400, "comment": "HS 2: Komplexe Wechselstromrechnung", "tags": "vorlesung", "created_at": iso(s_ge), "updated_at": iso(now)})

            # Live attendance java Vorlesung
            s_ja = day_date.replace(hour=14, minute=15); e_ja = day_date.replace(hour=15, minute=45)
            time_entries.append({"id": make_id(), "activity_id": act_java["id"], "started_at": iso(s_ja), "ended_at": iso(e_ja), "duration_seconds": 5400, "comment": "HS 3: Streams, Lambdas & Optionals", "tags": "programmieren", "created_at": iso(s_ja), "updated_at": iso(now)})

        elif iso_dow == 2: # Tuesday (Day 2)
            # get2 Übung: MISSED! (No entry recorded here -> test status "m")
            # ente Vorlesung: CANCELLED! (Override cancelled -> test status "x")
            pass

        elif iso_dow == 3: # Wednesday (Day 3)
            # wust Vorlesung (with Audimax room override):
            s_wu = day_date.replace(hour=9, minute=15); e_wu = day_date.replace(hour=10, minute=45)
            time_entries.append({"id": make_id(), "activity_id": act_wust["id"], "started_at": iso(s_wu), "ended_at": iso(e_wu), "duration_seconds": 5400, "comment": "Audimax: Wahrscheinlichkeitsräume & Bayes-Theorem", "tags": "mathe", "created_at": iso(s_wu), "updated_at": iso(now)})

            # java Übung:
            s_ju = day_date.replace(hour=14, minute=15); e_ju = day_date.replace(hour=15, minute=45)
            time_entries.append({"id": make_id(), "activity_id": act_java["id"], "started_at": iso(s_ju), "ended_at": iso(e_ju), "duration_seconds": 5400, "comment": "R 1.05: Übungsblatt 3 besprochen", "tags": "uebung", "created_at": iso(s_ju), "updated_at": iso(now)})

        elif iso_dow == 4: # Thursday (Day 4)
            # thermo Übung (time shifted to 08:30 via override):
            s_tu = day_date.replace(hour=8, minute=30); e_tu = day_date.replace(hour=10, minute=0)
            time_entries.append({"id": make_id(), "activity_id": act_thermo["id"], "started_at": iso(s_tu), "ended_at": iso(e_tu), "duration_seconds": 5400, "comment": "R 2.104: Kreisprozesse (Carnot & Joule)", "tags": "uebung", "created_at": iso(s_tu), "updated_at": iso(now)})

            # wust Übung:
            s_wu = day_date.replace(hour=14, minute=15); e_wu = day_date.replace(hour=15, minute=45)
            time_entries.append({"id": make_id(), "activity_id": act_wust["id"], "started_at": iso(s_wu), "ended_at": iso(e_wu), "duration_seconds": 5400, "comment": "R 2.201: Kombinatorik & Urnenmodelle", "tags": "uebung", "created_at": iso(s_wu), "updated_at": iso(now)})

        elif iso_dow == 5: # Friday (Day 5)
            # Mess Vorlesung:
            s_ms = day_date.replace(hour=10, minute=15); e_ms = day_date.replace(hour=11, minute=45)
            time_entries.append({"id": make_id(), "activity_id": act_mess["id"], "started_at": iso(s_ms), "ended_at": iso(e_ms), "duration_seconds": 5400, "comment": "HS 3: Messunsicherheiten nach GUM", "tags": "vorlesung", "created_at": iso(s_ms), "updated_at": iso(now)})

            # thermo Tutorium:
            s_tt = day_date.replace(hour=13, minute=0); e_tt = day_date.replace(hour=14, minute=30)
            time_entries.append({"id": make_id(), "activity_id": act_thermo["id"], "started_at": iso(s_tt), "ended_at": iso(e_tt), "duration_seconds": 5400, "comment": "R 0.014: Fragen zu Enthalpie & Entropie", "tags": "tutorium", "created_at": iso(s_tt), "updated_at": iso(now)})

        # E) Pure Self-Study for Strömi (4 sessions across days 1, 4, 7, 9)
        if day_idx in (1, 4, 7, 8):
            s_st = day_date.replace(hour=16, minute=0); e_st = day_date.replace(hour=18, minute=0)
            c_st = s_st if day_idx != 8 else day_date.replace(hour=22, minute=0)
            time_entries.append({
                "id": make_id(), "activity_id": act_stromi["id"],
                "started_at": iso(s_st), "ended_at": iso(e_st), "duration_seconds": 7200,
                "comment": f"Navier-Stokes Gleichungen & Grenzschichttheorie (Teil {day_idx//2 + 1})",
                "tags": "selbststudium,klausur", "created_at": iso(c_st), "updated_at": iso(now)
            })

        # F) Lunch & Cooking (Daily 12:15 to 13:00)
        if not is_today or now.hour >= 13:
            s_ck = day_date.replace(hour=12, minute=15); e_ck = day_date.replace(hour=12, minute=35)
            s_lc = day_date.replace(hour=12, minute=35); e_lc = day_date.replace(hour=13, minute=0)
            time_entries.append({"id": make_id(), "activity_id": act_cooking["id"], "started_at": iso(s_ck), "ended_at": iso(e_ck), "duration_seconds": 1200, "comment": "Pasta aglio e olio gekocht", "tags": "kochen", "created_at": iso(s_ck), "updated_at": iso(now)})
            time_entries.append({"id": make_id(), "activity_id": act_lunch["id"], "started_at": iso(s_lc), "ended_at": iso(e_lc), "duration_seconds": 1500, "comment": "Mittagessen auf dem Balkon", "tags": "essen", "created_at": iso(s_lc), "updated_at": iso(now)})

        # G) Afternoon Pause / Espresso (15:45 to 16:00)
        if not is_today or now.hour >= 16:
            s_ps = day_date.replace(hour=15, minute=45); e_ps = day_date.replace(hour=16, minute=0)
            time_entries.append({"id": make_id(), "activity_id": act_pause["id"], "started_at": iso(s_ps), "ended_at": iso(e_ps), "duration_seconds": 900, "comment": "Espresso & kurze Denkpause", "tags": "pause", "created_at": iso(s_ps), "updated_at": iso(now)})

        # H) Dinner (19:15 to 19:50)
        if not is_today or now.hour >= 20:
            s_dn = day_date.replace(hour=19, minute=15); e_dn = day_date.replace(hour=19, minute=50)
            time_entries.append({"id": make_id(), "activity_id": act_dinner["id"], "started_at": iso(s_dn), "ended_at": iso(e_dn), "duration_seconds": 2100, "comment": "Gemüsepfanne & Reis", "tags": "essen", "created_at": iso(s_dn), "updated_at": iso(now)})

        # I) Evening Leisure (alternating reading, music, gaming, social)
        if not is_today or now.hour >= 22:
            if day_idx % 3 == 0:
                s_rd = day_date.replace(hour=20, minute=30); e_rd = day_date.replace(hour=21, minute=45)
                time_entries.append({"id": make_id(), "activity_id": act_read["id"], "started_at": iso(s_rd), "ended_at": iso(e_rd), "duration_seconds": 4500, "comment": "Clean Code & Software Architecture gelesen", "tags": "lesen,weiterbildung", "created_at": iso(s_rd), "updated_at": iso(now)})
            elif day_idx % 3 == 1:
                s_eg = day_date.replace(hour=20, minute=15); e_eg = day_date.replace(hour=21, minute=15)
                time_entries.append({"id": make_id(), "activity_id": act_egitarre["id"], "started_at": iso(s_eg), "ended_at": iso(e_eg), "duration_seconds": 3600, "comment": "Riffs & Pentatonik geübt", "tags": "musik", "created_at": iso(s_eg), "updated_at": iso(now)})
            else:
                s_gm = day_date.replace(hour=20, minute=0); e_gm = day_date.replace(hour=21, minute=30)
                time_entries.append({"id": make_id(), "activity_id": act_games["id"], "started_at": iso(s_gm), "ended_at": iso(e_gm), "duration_seconds": 5400, "comment": "Multiplayer Session mit Freunden", "tags": "gaming", "created_at": iso(s_gm), "updated_at": iso(now)})

        # ---------------------------------------------------------------------
        # EDGE CASES INJECTION
        # ---------------------------------------------------------------------
        # Edge Case 1: Sequential 2-minute micro-slots on Day 5 (2026-10-02)
        if day_idx == 5:
            m1_s = day_date.replace(hour=12, minute=0); m1_e = day_date.replace(hour=12, minute=2)
            m2_s = day_date.replace(hour=12, minute=2); m2_e = day_date.replace(hour=12, minute=4)
            m3_s = day_date.replace(hour=12, minute=4); m3_e = day_date.replace(hour=12, minute=6)
            m4_s = day_date.replace(hour=12, minute=6); m4_e = day_date.replace(hour=12, minute=8)
            time_entries.append({"id": make_id(), "activity_id": act_commute["id"], "started_at": iso(m1_s), "ended_at": iso(m1_e), "duration_seconds": 120, "comment": "Sprint zur S-Bahn", "tags": "mikro", "created_at": iso(m1_s), "updated_at": iso(now)})
            time_entries.append({"id": make_id(), "activity_id": act_bio["id"], "started_at": iso(m2_s), "ended_at": iso(m2_e), "duration_seconds": 120, "comment": "Händewaschen & Frischepause", "tags": "mikro", "created_at": iso(m2_s), "updated_at": iso(now)})
            time_entries.append({"id": make_id(), "activity_id": act_pause["id"], "started_at": iso(m3_s), "ended_at": iso(m3_e), "duration_seconds": 120, "comment": "Schnellen Espresso geholt", "tags": "mikro", "created_at": iso(m3_s), "updated_at": iso(now)})
            time_entries.append({"id": make_id(), "activity_id": act_social["id"], "started_at": iso(m4_s), "ended_at": iso(m4_e), "duration_seconds": 120, "comment": "Kommilitone im Flur gegrüßt", "tags": "mikro", "created_at": iso(m4_s), "updated_at": iso(now)})

        # Edge Case 2: Concurrent multi-column collision on Day 6 (2026-10-03)
        if day_idx == 6:
            c1_s = day_date.replace(hour=15, minute=0); c1_e = day_date.replace(hour=15, minute=6)
            c2_s = day_date.replace(hour=15, minute=2); c2_e = day_date.replace(hour=15, minute=7)
            c3_s = day_date.replace(hour=15, minute=4); c3_e = day_date.replace(hour=15, minute=10)
            time_entries.append({"id": make_id(), "activity_id": act_tinkering["id"], "started_at": iso(c1_s), "ended_at": iso(c1_e), "duration_seconds": 360, "comment": "Widerstände gemessen", "tags": "kollision", "created_at": iso(c1_s), "updated_at": iso(now)})
            time_entries.append({"id": make_id(), "activity_id": act_egitarre["id"], "started_at": iso(c2_s), "ended_at": iso(c2_e), "duration_seconds": 300, "comment": "Verstärker eingepegelt", "tags": "kollision", "created_at": iso(c2_s), "updated_at": iso(now)})
            time_entries.append({"id": make_id(), "activity_id": act_youtube["id"], "started_at": iso(c3_s), "ended_at": iso(c3_e), "duration_seconds": 360, "comment": "Tutorial nebenbei", "tags": "kollision", "created_at": iso(c3_s), "updated_at": iso(now)})

        # Edge Case 3: Out-of-hours early morning on Day 3 (04:15 to 05:30 < 06:00)
        if day_idx == 3:
            ooh_s = day_date.replace(hour=4, minute=15); ooh_e = day_date.replace(hour=5, minute=30)
            time_entries.append({
                "id": make_id(), "activity_id": act_tinkering["id"],
                "started_at": iso(ooh_s), "ended_at": iso(ooh_e), "duration_seconds": 4500,
                "comment": "Schlaflos: nächtlicher Geistesblitz an der Platine",
                "tags": "outofhours", "created_at": iso(ooh_s), "updated_at": iso(now)
            })

        # Edge Case 4: Midnight crossing on Day 7 (23:15 to 01:00)
        if day_idx == 7:
            mc_s = day_date.replace(hour=23, minute=15); mc_e = day_date.replace(hour=23, minute=59) + timedelta(minutes=61) # 01:00 next day
            time_entries.append({
                "id": make_id(), "activity_id": act_read["id"],
                "started_at": iso(mc_s), "ended_at": iso(mc_e), "duration_seconds": int((mc_e - mc_s).total_seconds()),
                "comment": "Spannendes Kapitel bis spät in die Nacht fertig gelesen",
                "tags": "mitternacht", "created_at": iso(mc_s), "updated_at": iso(now)
            })

        # Edge Case 5: Family visit / Chores on Day 8 (Saturday)
        if day_idx == 8:
            s_om = day_date.replace(hour=15, minute=0); e_om = day_date.replace(hour=18, minute=0)
            time_entries.append({
                "id": make_id(), "activity_id": act_oma["id"],
                "started_at": iso(s_om), "ended_at": iso(e_om), "duration_seconds": 10800,
                "comment": "Kaffee und Kuchen mit Oma, Garten geholfen",
                "tags": "familie", "created_at": iso(s_om), "updated_at": iso(now)
            })

    # -------------------------------------------------------------------------
    # 9. Active currently-running timer on Today!
    # -------------------------------------------------------------------------
    start_running = now - timedelta(minutes=35)
    time_entries.append({
        "id": make_id(),
        "activity_id": act_java["id"],
        "started_at": iso(start_running),
        "ended_at": None,
        "duration_seconds": 0,
        "comment": "Spring Boot REST API & Hibernate Entities",
        "tags": "programmieren,live",
        "created_at": iso(start_running),
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

    print(f"Generated extensive 10-day test dataset with {len(time_entries)} entries across {len(activities)} activities.")
    print(f"Saved to: {output_file}")
    return payload

if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else None
    generate(out)

