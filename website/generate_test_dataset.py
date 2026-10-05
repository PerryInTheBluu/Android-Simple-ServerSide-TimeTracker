#!/usr/bin/env python3
"""Generates a rich, realistic 10-day test dataset for the TimeTracker analysis page.
Spans 10 days (ending today), with full timetables, overrides, todos, subject goals,
and rich tracked entries (Uni, everyday, leisure) including an active running timer.
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
    if output_file is None:
        output_file = os.path.join(os.path.dirname(__file__), "test_dataset_10days.json")

    now = datetime.now(TZ)
    # 10 days ending today: Day 0 = 9 days ago, Day 9 = today
    start_date = (now - timedelta(days=9)).replace(hour=0, minute=0, second=0, microsecond=0)

    # 1. Categories
    cat_essen = {"id": make_id(), "name": "Essen", "color_id": 15, "note": "Kochen, Mahlzeiten", "updated_at": iso(now)}
    cat_zeit = {"id": make_id(), "name": "Zeitverschwendung", "color_id": 2, "note": "YouTube, Gaming", "updated_at": iso(now)}
    cat_uni = {"id": make_id(), "name": "Uni", "color_id": 10, "note": "Vorlesungen, Übungen, Lernen", "updated_at": iso(now)}
    cat_freizeit = {"id": make_id(), "name": "Freizeit", "color_id": 4, "note": "Sport, Musik, Basteln, Lesen", "updated_at": iso(now)}
    cat_alltag = {"id": make_id(), "name": "Alltag", "color_id": 1, "note": "Schlaf, Haushalt, Pendeln, Hygiene", "updated_at": iso(now)}

    categories = [cat_essen, cat_zeit, cat_uni, cat_freizeit, cat_alltag]

    # 2. Activities (Alle Symbole sind einfarbige Vektorsymbole, Uni bekommt diploma)
    # Alltag: bio, chores, commute, sleep, pause
    act_bio = {"id": make_id(), "name": "bio", "icon": "bio", "color_id": 14, "category": json.dumps([cat_alltag["id"]]), "updated_at": iso(now)}
    act_chores = {"id": make_id(), "name": "chores", "icon": "chores", "color_id": 17, "category": json.dumps([cat_alltag["id"]]), "updated_at": iso(now)}
    act_commute = {"id": make_id(), "name": "commute", "icon": "commute", "color_id": 8, "category": json.dumps([cat_alltag["id"]]), "updated_at": iso(now)}
    act_sleep = {"id": make_id(), "name": "sleep", "icon": "sleep", "color_id": 1, "category": json.dumps([cat_alltag["id"]]), "updated_at": iso(now)}
    act_pause = {"id": make_id(), "name": "pause", "icon": "pause", "color_id": 14, "category": json.dumps([cat_alltag["id"]]), "updated_at": iso(now)}

    # Essen: cooking, breakfast, lunch, dinner
    act_cooking = {"id": make_id(), "name": "cooking", "icon": "cooking", "color_id": 15, "category": json.dumps([cat_essen["id"]]), "updated_at": iso(now)}
    act_breakfast = {"id": make_id(), "name": "breakfast", "icon": "breakfast", "color_id": 15, "category": json.dumps([cat_essen["id"]]), "updated_at": iso(now)}
    act_lunch = {"id": make_id(), "name": "lunch", "icon": "lunch", "color_id": 15, "category": json.dumps([cat_essen["id"]]), "updated_at": iso(now)}
    act_dinner = {"id": make_id(), "name": "dinner", "icon": "dinner", "color_id": 15, "category": json.dumps([cat_essen["id"]]), "updated_at": iso(now)}

    # Zeitverschwendung: youtube, games
    act_youtube = {"id": make_id(), "name": "youtube", "icon": "youtube", "color_id": 2, "category": json.dumps([cat_zeit["id"]]), "updated_at": iso(now)}
    act_games = {"id": make_id(), "name": "games", "icon": "games", "color_id": 3, "category": json.dumps([cat_zeit["id"]]), "updated_at": iso(now)}

    # Freizeit: social, tinkering, exercise, egitarre, read, oma besucht diesen monat
    act_social = {"id": make_id(), "name": "social", "icon": "social", "color_id": 7, "category": json.dumps([cat_freizeit["id"]]), "updated_at": iso(now)}
    act_tinkering = {"id": make_id(), "name": "tinkering", "icon": "tinkering", "color_id": 13, "category": json.dumps([cat_freizeit["id"]]), "updated_at": iso(now)}
    act_exercise = {"id": make_id(), "name": "exercise", "icon": "exercise", "color_id": 4, "category": json.dumps([cat_freizeit["id"]]), "updated_at": iso(now)}
    act_egitarre = {"id": make_id(), "name": "egitarre", "icon": "egitarre", "color_id": 16, "category": json.dumps([cat_freizeit["id"]]), "updated_at": iso(now)}
    act_read = {"id": make_id(), "name": "read", "icon": "read", "color_id": 5, "category": json.dumps([cat_freizeit["id"]]), "updated_at": iso(now)}
    act_oma = {"id": make_id(), "name": "oma besucht diesen monat", "icon": "oma", "color_id": 6, "category": json.dumps([cat_freizeit["id"]]), "updated_at": iso(now)}

    # Uni: thermo, get2, ente, wust, strömi, Mess (alle mit Diploma Hut!)
    act_thermo = {"id": make_id(), "name": "thermo", "icon": "diploma", "color_id": 9, "category": json.dumps([cat_uni["id"]]), "updated_at": iso(now)}
    act_get2 = {"id": make_id(), "name": "get2", "icon": "diploma", "color_id": 11, "category": json.dumps([cat_uni["id"]]), "updated_at": iso(now)}
    act_ente = {"id": make_id(), "name": "ente", "icon": "diploma", "color_id": 12, "category": json.dumps([cat_uni["id"]]), "updated_at": iso(now)}
    act_wust = {"id": make_id(), "name": "wust", "icon": "diploma", "color_id": 10, "category": json.dumps([cat_uni["id"]]), "updated_at": iso(now)}
    act_stromi = {"id": make_id(), "name": "strömi", "icon": "diploma", "color_id": 18, "category": json.dumps([cat_uni["id"]]), "updated_at": iso(now)}
    act_mess = {"id": make_id(), "name": "Mess", "icon": "diploma", "color_id": 19, "category": json.dumps([cat_uni["id"]]), "updated_at": iso(now)}

    activities = [
        act_bio, act_chores, act_commute, act_sleep, act_pause,
        act_cooking, act_breakfast, act_lunch, act_dinner,
        act_youtube, act_games,
        act_social, act_tinkering, act_exercise, act_egitarre, act_read, act_oma,
        act_thermo, act_get2, act_ente, act_wust, act_stromi, act_mess
    ]

    # 3. Timetable events (Weekly schedule, ISO DayOfWeek: 1=Mo..7=So)
    # HINWEIS: strömi hat KEINE timetable events (Grenzfall: reines Selbststudium!)
    ev_thermo_mo = {"id": make_id(), "name": "thermo", "day_of_week": 1, "start_time": 8 * 60 + 15, "end_time": 9 * 60 + 45, "room": "HS 1", "type": 0, "activity_sync_id": act_thermo["id"], "updated_at": iso(now)}
    ev_get2_mo = {"id": make_id(), "name": "get2", "day_of_week": 1, "start_time": 10 * 60 + 15, "end_time": 11 * 60 + 45, "room": "HS 2", "type": 0, "activity_sync_id": act_get2["id"], "updated_at": iso(now)}

    ev_get2_di = {"id": make_id(), "name": "get2", "day_of_week": 2, "start_time": 8 * 60 + 15, "end_time": 9 * 60 + 45, "room": "R 0.014", "type": 1, "activity_sync_id": act_get2["id"], "updated_at": iso(now)}
    ev_ente_di = {"id": make_id(), "name": "ente", "day_of_week": 2, "start_time": 12 * 60, "end_time": 13 * 60 + 30, "room": "Audimax", "type": 0, "activity_sync_id": act_ente["id"], "updated_at": iso(now)}

    ev_wust_mi = {"id": make_id(), "name": "wust", "day_of_week": 3, "start_time": 9 * 60 + 15, "end_time": 10 * 60 + 45, "room": "HS 1", "type": 0, "activity_sync_id": act_wust["id"], "updated_at": iso(now)}

    ev_thermo_do = {"id": make_id(), "name": "thermo", "day_of_week": 4, "start_time": 8 * 60 + 15, "end_time": 9 * 60 + 45, "room": "R 2.104", "type": 1, "activity_sync_id": act_thermo["id"], "updated_at": iso(now)}
    ev_wust_do = {"id": make_id(), "name": "wust", "day_of_week": 4, "start_time": 14 * 60 + 15, "end_time": 15 * 60 + 45, "room": "R 2.201", "type": 1, "activity_sync_id": act_wust["id"], "updated_at": iso(now)}

    ev_mess_fr = {"id": make_id(), "name": "Mess", "day_of_week": 5, "start_time": 10 * 60 + 15, "end_time": 11 * 60 + 45, "room": "HS 2", "type": 0, "activity_sync_id": act_mess["id"], "updated_at": iso(now)}
    ev_thermo_fr = {"id": make_id(), "name": "thermo", "day_of_week": 5, "start_time": 13 * 60, "end_time": 14 * 60 + 30, "room": "R 0.014", "type": 2, "activity_sync_id": act_thermo["id"], "updated_at": iso(now)}

    timetable_events = [
        ev_thermo_mo, ev_get2_mo,
        ev_get2_di, ev_ente_di,
        ev_wust_mi,
        ev_thermo_do, ev_wust_do,
        ev_mess_fr, ev_thermo_fr,
    ]

    # 4. Overrides: EnTe Ausfall am ersten Dienstag, WuSt Raumverlegung am Mittwoch
    override_date_di = (start_date + timedelta(days=2)).strftime("%Y-%m-%d")
    override_date_mi = (start_date + timedelta(days=3)).strftime("%Y-%m-%d")
    timetable_overrides = [
        {"id": make_id(), "event_sync_id": ev_ente_di["id"], "date": override_date_di, "cancelled": True, "comment": "Dozent krank", "updated_at": iso(now)},
        {"id": make_id(), "event_sync_id": ev_wust_mi["id"], "date": override_date_mi, "cancelled": False, "room": "Neuer Hörsaal", "comment": "Raumverlegung", "updated_at": iso(now)},
    ]

    # 5. Timetable Days
    free_day_date = (start_date + timedelta(days=7)).strftime("%Y-%m-%d")
    timetable_days = [
        {"id": make_id(), "date": free_day_date, "free_day": False, "name": "Vorlesungstag", "updated_at": iso(now)},
    ]

    # 6. Todos (Vorbereitung: 0, Nachbereitung: 1, Todo: 2)
    # Beachte: Unterscheidung zwischen thermo Vorlesung, Übung und Tutorium!
    timetable_todos = [
        {"id": make_id(), "event_sync_id": ev_thermo_mo["id"], "date": (now + timedelta(days=1)).strftime("%Y-%m-%d"), "text": "Skript Kapitel 3 durcharbeiten", "type": 0, "done": False, "updated_at": iso(now)},
        {"id": make_id(), "event_sync_id": ev_thermo_do["id"], "date": (now + timedelta(days=2)).strftime("%Y-%m-%d"), "text": "Übungsblatt 4 Aufgaben 1-3 rechnen", "type": 0, "done": False, "updated_at": iso(now)},
        {"id": make_id(), "event_sync_id": ev_thermo_fr["id"], "date": (now + timedelta(days=4)).strftime("%Y-%m-%d"), "text": "Fragen zu Aufgabe 2 für Tutor notieren", "type": 0, "done": False, "updated_at": iso(now)},
        {"id": make_id(), "event_sync_id": ev_get2_mo["id"], "date": (now + timedelta(days=3)).strftime("%Y-%m-%d"), "text": "Formelsammlung Wechselstrom ergänzen", "type": 1, "done": False, "updated_at": iso(now)},
        {"id": make_id(), "event_sync_id": ev_wust_do["id"], "date": (now + timedelta(days=5)).strftime("%Y-%m-%d"), "text": "R-Skript zur Chi-Quadrat-Verteilung testen", "type": 2, "done": False, "updated_at": iso(now)},
        # Erledigte Aufgaben (für eingeklapptes Dropdown)
        {"id": make_id(), "event_sync_id": ev_ente_di["id"], "date": (start_date + timedelta(days=1)).strftime("%Y-%m-%d"), "text": "BHKW Kenndaten recherchieren", "type": 1, "done": True, "updated_at": iso(now)},
        {"id": make_id(), "event_sync_id": ev_mess_fr["id"], "date": (start_date + timedelta(days=4)).strftime("%Y-%m-%d"), "text": "Oszilloskop Kalibrierungsprotokoll abgeben", "type": 2, "done": True, "updated_at": iso(now)},
        {"id": make_id(), "event_sync_id": ev_thermo_mo["id"], "date": (start_date + timedelta(days=1)).strftime("%Y-%m-%d"), "text": "Zustandsgleichung idealer Gase wiederholen", "type": 0, "done": True, "updated_at": iso(now)},
    ]

    # 7. Goals: inklusive strömi (ohne Vorlesung, aber Ziel!)
    subject_goals = [
        {"id": make_id(), "activity_sync_id": act_thermo["id"], "target_seconds": 150 * 3600, "updated_at": iso(now)}, # 5 ECTS
        {"id": make_id(), "activity_sync_id": act_get2["id"], "target_seconds": 150 * 3600, "updated_at": iso(now)},
        {"id": make_id(), "activity_sync_id": act_ente["id"], "target_seconds": 150 * 3600, "updated_at": iso(now)},
        {"id": make_id(), "activity_sync_id": act_wust["id"], "target_seconds": 150 * 3600, "updated_at": iso(now)},
        {"id": make_id(), "activity_sync_id": act_stromi["id"], "target_seconds": 120 * 3600, "updated_at": iso(now)}, # 4 ECTS (Reines Selbststudium!)
        {"id": make_id(), "activity_sync_id": act_mess["id"], "target_seconds": 90 * 3600, "updated_at": iso(now)},   # 3 ECTS
    ]

    # 8. Time entries across 10 days
    time_entries = []

    for day_idx in range(10):
        day_date = start_date + timedelta(days=day_idx)
        iso_dow = day_date.isoweekday() # 1=Mo..7=So
        is_today = (day_idx == 9)

        # Schlaf: 00:00 bis 07:15
        s_sleep = day_date.replace(hour=0, minute=0)
        e_sleep = day_date.replace(hour=7, minute=15)
        time_entries.append({
            "id": make_id(), "activity_id": act_sleep["id"],
            "started_at": iso(s_sleep), "ended_at": iso(e_sleep), "duration_seconds": 26100,
            "comment": "Schlaf", "tags": "", "created_at": iso(s_sleep), "updated_at": iso(now)
        })

        # Frühstück & Bio: 07:20 bis 08:00
        s_bio = day_date.replace(hour=7, minute=20); e_bio = day_date.replace(hour=7, minute=35)
        time_entries.append({
            "id": make_id(), "activity_id": act_bio["id"],
            "started_at": iso(s_bio), "ended_at": iso(e_bio), "duration_seconds": 900,
            "comment": "Bad & Dusche", "tags": "", "created_at": iso(s_bio), "updated_at": iso(now)
        })
        s_bf = day_date.replace(hour=7, minute=35); e_bf = day_date.replace(hour=8, minute=0)
        time_entries.append({
            "id": make_id(), "activity_id": act_breakfast["id"],
            "started_at": iso(s_bf), "ended_at": iso(e_bf), "duration_seconds": 1500,
            "comment": "Frühstück & Kaffee", "tags": "", "created_at": iso(s_bf), "updated_at": iso(now)
        })

        # Weekday lectures & tracking
        if iso_dow == 1: # Monday
            # thermo Vorlesung: 08:15 to 09:45
            # Day 1: Live attended (created_at = started_at)
            # Day 8: Nachgetragen! (created_at = 2 days after ended_at!)
            s_th = day_date.replace(hour=8, minute=15); e_th = day_date.replace(hour=9, minute=45)
            created_th = (e_th + timedelta(days=2)) if day_idx == 8 else s_th
            comment_th = "Vorlesung HS 1 (später eingetragen)" if day_idx == 8 else "Vorlesung HS 1"
            time_entries.append({
                "id": make_id(), "activity_id": act_thermo["id"],
                "started_at": iso(s_th), "ended_at": iso(e_th), "duration_seconds": 5400,
                "comment": comment_th, "tags": "", "created_at": iso(created_th), "updated_at": iso(now)
            })

            # get2 Vorlesung: 10:15 to 11:45 (Live attended)
            s_g2 = day_date.replace(hour=10, minute=15); e_g2 = day_date.replace(hour=11, minute=45)
            time_entries.append({
                "id": make_id(), "activity_id": act_get2["id"],
                "started_at": iso(s_g2), "ended_at": iso(e_g2), "duration_seconds": 5400,
                "comment": "HS 2 Wechselstromtechnik", "tags": "", "created_at": iso(s_g2), "updated_at": iso(now)
            })

        elif iso_dow == 2: # Tuesday
            # get2 Übung: 08:15 to 09:45
            if day_idx == 2:
                # GRENZFALL: Am vergangenen Dienstag wurde get2 Übung VERPASST! (Kein Zeiteintrag -> missed = 1)
                pass
            elif not is_today or now.hour >= 10:
                # Am heutigen Dienstag nur wenn schon vorbei, ansonsten bevorstehend (upcoming)
                s_g2u = day_date.replace(hour=8, minute=15); e_g2u = day_date.replace(hour=9, minute=45)
                time_entries.append({
                    "id": make_id(), "activity_id": act_get2["id"],
                    "started_at": iso(s_g2u), "ended_at": iso(e_g2u), "duration_seconds": 5400,
                    "comment": "R 0.014 Übung", "tags": "", "created_at": iso(s_g2u), "updated_at": iso(now)
                })

            # ente Vorlesung: 12:00 to 13:30 (Am ersten Di ausgefallen, heute anstehend oder besucht)
            if day_idx != 2 and (not is_today or now.hour >= 14):
                s_en = day_date.replace(hour=12, minute=0); e_en = day_date.replace(hour=13, minute=30)
                time_entries.append({
                    "id": make_id(), "activity_id": act_ente["id"],
                    "started_at": iso(s_en), "ended_at": iso(e_en), "duration_seconds": 5400,
                    "comment": "Audimax Gasturbinen", "tags": "", "created_at": iso(s_en), "updated_at": iso(now)
                })

        elif iso_dow == 3: # Wednesday
            # wust: 09:15 to 10:45 (Live besucht)
            s_wu = day_date.replace(hour=9, minute=15); e_wu = day_date.replace(hour=10, minute=45)
            time_entries.append({
                "id": make_id(), "activity_id": act_wust["id"],
                "started_at": iso(s_wu), "ended_at": iso(e_wu), "duration_seconds": 5400,
                "comment": "Kombinatorik & Wahrscheinlichkeit", "tags": "", "created_at": iso(s_wu), "updated_at": iso(now)
            })

        elif iso_dow == 4: # Thursday
            # thermo Übung: 08:15 to 09:45
            s_th = day_date.replace(hour=8, minute=15); e_th = day_date.replace(hour=9, minute=45)
            time_entries.append({
                "id": make_id(), "activity_id": act_thermo["id"],
                "started_at": iso(s_th), "ended_at": iso(e_th), "duration_seconds": 5400,
                "comment": "R 2.104 Kreisprozesse", "tags": "", "created_at": iso(s_th), "updated_at": iso(now)
            })
            # wust Übung: 14:15 to 15:45
            s_wu = day_date.replace(hour=14, minute=15); e_wu = day_date.replace(hour=15, minute=45)
            time_entries.append({
                "id": make_id(), "activity_id": act_wust["id"],
                "started_at": iso(s_wu), "ended_at": iso(e_wu), "duration_seconds": 5400,
                "comment": "Übungsaufgaben Besprechung", "tags": "", "created_at": iso(s_wu), "updated_at": iso(now)
            })

        elif iso_dow == 5: # Friday
            # Mess: 10:15 to 11:45
            s_me = day_date.replace(hour=10, minute=15); e_me = day_date.replace(hour=11, minute=45)
            time_entries.append({
                "id": make_id(), "activity_id": act_mess["id"],
                "started_at": iso(s_me), "ended_at": iso(e_me), "duration_seconds": 5400,
                "comment": "Sensoren & Messfehler", "tags": "", "created_at": iso(s_me), "updated_at": iso(now)
            })
            # thermo Tutorium: 13:00 to 14:30
            s_th = day_date.replace(hour=13, minute=0); e_th = day_date.replace(hour=14, minute=30)
            time_entries.append({
                "id": make_id(), "activity_id": act_thermo["id"],
                "started_at": iso(s_th), "ended_at": iso(e_th), "duration_seconds": 5400,
                "comment": "Tutorium Aufgaben", "tags": "", "created_at": iso(s_th), "updated_at": iso(now)
            })

        # GRENZFALL: STRÖMI (keine Stundenplan-Veranstaltung, aber fleißig getrackt!)
        if day_idx in (2, 5, 8):
            s_str = day_date.replace(hour=15, minute=30); e_str = day_date.replace(hour=17, minute=30)
            time_entries.append({
                "id": make_id(), "activity_id": act_stromi["id"],
                "started_at": iso(s_str), "ended_at": iso(e_str), "duration_seconds": 7200,
                "comment": "Selbststudium Navier-Stokes & Grenzschichten", "tags": "", "created_at": iso(s_str), "updated_at": iso(now)
            })

        # Mittagessen (lunch) & Pause
        if not is_today or now.hour >= 13:
            s_lu = day_date.replace(hour=12, minute=45); e_lu = day_date.replace(hour=13, minute=20)
            time_entries.append({
                "id": make_id(), "activity_id": act_lunch["id"],
                "started_at": iso(s_lu), "ended_at": iso(e_lu), "duration_seconds": 2100,
                "comment": "Mensa Mittagessen", "tags": "", "created_at": iso(s_lu), "updated_at": iso(now)
            })

        # Nachmittag: Kochen, Freizeit, Pendeln, Oma-Besuch
        if day_idx == 4: # Oma besucht diesen Monat!
            s_om = day_date.replace(hour=15, minute=0); e_om = day_date.replace(hour=18, minute=0)
            time_entries.append({
                "id": make_id(), "activity_id": act_oma["id"],
                "started_at": iso(s_om), "ended_at": iso(e_om), "duration_seconds": 10800,
                "comment": "Kaffee & Kuchen bei Oma", "tags": "", "created_at": iso(s_om), "updated_at": iso(now)
            })
        elif day_idx % 3 == 0 and (not is_today or now.hour >= 18):
            # Exercise (Sport)
            s_ex = day_date.replace(hour=17, minute=0); e_ex = day_date.replace(hour=18, minute=15)
            time_entries.append({
                "id": make_id(), "activity_id": act_exercise["id"],
                "started_at": iso(s_ex), "ended_at": iso(e_ex), "duration_seconds": 4500,
                "comment": "Fitnessstudio", "tags": "", "created_at": iso(s_ex), "updated_at": iso(now)
            })
        elif day_idx % 3 == 1 and (not is_today or now.hour >= 18):
            # Tinkering (Basteln)
            s_ti = day_date.replace(hour=16, minute=30); e_ti = day_date.replace(hour=18, minute=0)
            time_entries.append({
                "id": make_id(), "activity_id": act_tinkering["id"],
                "started_at": iso(s_ti), "ended_at": iso(e_ti), "duration_seconds": 5400,
                "comment": "Lötkolben & ESP32", "tags": "", "created_at": iso(s_ti), "updated_at": iso(now)
            })
        elif day_idx % 3 == 2 and (not is_today or now.hour >= 18):
            # E-Gitarre
            s_eg = day_date.replace(hour=16, minute=45); e_eg = day_date.replace(hour=17, minute=45)
            time_entries.append({
                "id": make_id(), "activity_id": act_egitarre["id"],
                "started_at": iso(s_eg), "ended_at": iso(e_eg), "duration_seconds": 3600,
                "comment": "Riffs & Pentatonik", "tags": "", "created_at": iso(s_eg), "updated_at": iso(now)
            })

        # Abendessen (cooking / dinner)
        if not is_today or now.hour >= 20:
            s_ck = day_date.replace(hour=18, minute=45); e_ck = day_date.replace(hour=19, minute=20)
            time_entries.append({
                "id": make_id(), "activity_id": act_cooking["id"],
                "started_at": iso(s_ck), "ended_at": iso(e_ck), "duration_seconds": 2100,
                "comment": "Pasta kochen", "tags": "", "created_at": iso(s_ck), "updated_at": iso(now)
            })
            s_di = day_date.replace(hour=19, minute=20); e_di = day_date.replace(hour=19, minute=55)
            time_entries.append({
                "id": make_id(), "activity_id": act_dinner["id"],
                "started_at": iso(s_di), "ended_at": iso(e_di), "duration_seconds": 2100,
                "comment": "Abendessen", "tags": "", "created_at": iso(s_di), "updated_at": iso(now)
            })

        # Abend Freizeit: read / youtube / games / social
        if not is_today or now.hour >= 22:
            act_eve = act_read if day_idx % 4 == 0 else act_youtube if day_idx % 4 == 1 else act_games if day_idx % 4 == 2 else act_social
            s_ev = day_date.replace(hour=20, minute=30); e_ev = day_date.replace(hour=22, minute=0)
            time_entries.append({
                "id": make_id(), "activity_id": act_eve["id"],
                "started_at": iso(s_ev), "ended_at": iso(e_ev), "duration_seconds": 5400,
                "comment": "Feierabend", "tags": "", "created_at": iso(s_ev), "updated_at": iso(now)
            })

    # GRENZFALL: MIKRO-AKTIVITÄTEN (~2 MINUTEN HINTEREINANDER) AM HEUTIGEN TAG (Testet Unterspalten-Kollision)
    t_base = start_date + timedelta(days=9)
    s_m1 = t_base.replace(hour=11, minute=30); e_m1 = t_base.replace(hour=11, minute=32)
    s_m2 = t_base.replace(hour=11, minute=32); e_m2 = t_base.replace(hour=11, minute=34)
    s_m3 = t_base.replace(hour=11, minute=34); e_m3 = t_base.replace(hour=11, minute=36)
    time_entries.append({"id": make_id(), "activity_id": act_pause["id"], "started_at": iso(s_m1), "ended_at": iso(e_m1), "duration_seconds": 120, "comment": "Kurze Kaffeepause", "tags": "", "created_at": iso(s_m1), "updated_at": iso(now)})
    time_entries.append({"id": make_id(), "activity_id": act_bio["id"], "started_at": iso(s_m2), "ended_at": iso(e_m2), "duration_seconds": 120, "comment": "Händewaschen", "tags": "", "created_at": iso(s_m2), "updated_at": iso(now)})
    time_entries.append({"id": make_id(), "activity_id": act_commute["id"], "started_at": iso(s_m3), "ended_at": iso(e_m3), "duration_seconds": 120, "comment": "Sprint zum Bus", "tags": "", "created_at": iso(s_m3), "updated_at": iso(now)})

    # GRENZFALL: OUT-OF-HOURS AUFZEICHNUNG (<06:00 UHR, Testet Warn-Banner #outOfHoursBanner)
    s_ooh = t_base.replace(hour=2, minute=30); e_ooh = t_base.replace(hour=3, minute=15)
    time_entries.append({
        "id": make_id(), "activity_id": act_youtube["id"],
        "started_at": iso(s_ooh), "ended_at": iso(e_ooh), "duration_seconds": 2700,
        "comment": "Nachts Doku geschaut", "tags": "", "created_at": iso(s_ooh), "updated_at": iso(now)
    })

    # AKTIV LAUFENDER TIMER: thermo läuft seit 35 Minuten
    start_running = now - timedelta(minutes=35)
    time_entries.append({
        "id": make_id(),
        "activity_id": act_thermo["id"],
        "started_at": iso(start_running),
        "ended_at": None,
        "duration_seconds": 0,
        "comment": "Übungsblatt 3 rechnen",
        "tags": "",
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

    print(f"Generated 10-day test dataset with {len(time_entries)} time entries across {len(activities)} activities.")
    print(f"Saved to: {output_file}")
    return payload

if __name__ == "__main__":
    default_out = os.path.join(os.path.dirname(__file__), "test_dataset_10days.json")
    target = sys.argv[1] if len(sys.argv) > 1 else default_out
    generate(target)
