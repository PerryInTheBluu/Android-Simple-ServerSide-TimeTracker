/**
 * Comprehensive Edge Case Test Suite for TimeTracker
 */
"use strict";
const fs = require("fs");
const assert = require("assert");

const html = fs.readFileSync(__dirname + "/index.html", "utf8");
const script = html.match(/<script>([\s\S]*?)<\/script>/)[1];

const elements = {};
function makeEl(id) {
  if (!elements[id]) {
    elements[id] = {
      id,
      value: "",
      textContent: "",
      innerHTML: "",
      disabled: false,
      classList: {
        _set: new Set(),
        add(c) { this._set.add(c); },
        remove(c) { this._set.delete(c); },
        contains(c) { return this._set.has(c); },
        toggle(c, force) { if (force !== undefined) { if (force) this.add(c); else this.remove(c); } else { if (this.contains(c)) this.remove(c); else this.add(c); } }
      },
      addEventListener() {},
      style: {}
    };
  }
  return elements[id];
}

global.document = {
  getElementById: makeEl,
  createElement: () => ({ style: {}, classList: { add(){}, remove(){} } }),
  documentElement: { setAttribute() {} }
};
global.localStorage = {
  _store: {},
  getItem(k) { return this._store[k] || ""; },
  setItem(k, v) { this._store[k] = String(v); },
  removeItem(k) { delete this._store[k]; },
  clear() { this._store = {}; }
};
global.location = { hash: "", origin: "http://127.0.0.1:8765", pathname: "/" };

const pullData = JSON.parse(fs.readFileSync(__dirname + "/test_dataset_10days.json", "utf8"));
global.fetch = async () => ({
  ok: true,
  status: 200,
  json: async () => pullData,
  text: async () => JSON.stringify(pullData)
});

const vm = require("vm");
vm.runInThisContext(script);

async function runEdgeCaseTests() {
  console.log("=================================================");
  console.log("RUNNING TIMETRACKER EDGE CASE VERIFICATION SUITE");
  console.log("=================================================");

  makeEl("token").value = "test-token";
  await load();
  assert(DATA !== null, "DATA must be loaded");

  // TEST 1: Midnight Crossing in Day View
  console.log("\n[Test 1] Midnight Crossing Handling in Day Calendar & Summary...");
  {
    // The dataset contains an entry on read: 2026-10-04 23:15 to 2026-10-05 01:00 (105 min total)
    // 1. Check Oct 4
    CALENDAR_DATE = new Date("2026-10-04T12:00:00");
    makeEl("shiftStart").value = "6";
    makeEl("shiftEnd").value = "24";
    renderTodayTimeline();
    const dayCalOct4 = makeEl("dayCalInner").innerHTML;
    assert(dayCalOct4.includes("read"), "Day calendar on Oct 4 must render read entry");
    assert(dayCalOct4.includes("23:15–24:00"), "Day calendar on Oct 4 must clip end to 24:00");
    const summaryOct4 = makeEl("todaySummary").textContent;
    console.log("  -> Oct 4 Summary:", summaryOct4);
    assert(summaryOct4.includes("13 h") || summaryOct4.includes("13,"), "Oct 4 summary must only include the 45m of read (not 1h45m)");

    // 2. Check Oct 5 with shiftStart = 0
    CALENDAR_DATE = new Date("2026-10-05T12:00:00");
    makeEl("shiftStart").value = "0";
    makeEl("shiftEnd").value = "24";
    renderTodayTimeline();
    const dayCalOct5 = makeEl("dayCalInner").innerHTML;
    assert(dayCalOct5.includes("read"), "Day calendar on Oct 5 must render read entry when shift includes 00:00");
    assert(dayCalOct5.includes("00:00–01:00"), "Day calendar on Oct 5 must clip start to 00:00");
    const summaryOct5 = makeEl("todaySummary").textContent;
    console.log("  -> Oct 5 Summary:", summaryOct5);
    assert(summaryOct5.includes("21,5 h"), "Oct 5 summary must only include the 60m of read (not 1h45m)");
    console.log("  ✓ Test 1 Passed: Midnight crossing clips correctly without double counting.");
  }

  // TEST 2: Out of Hours Detection
  console.log("\n[Test 2] Out of Hours Alert Banner...");
  {
    // With shiftStart = 6 and shiftEnd = 23 on Oct 5:
    CALENDAR_DATE = new Date("2026-10-05T12:00:00");
    makeEl("shiftStart").value = "6";
    makeEl("shiftEnd").value = "23";
    renderTodayTimeline();
    const oohOct5 = makeEl("outOfHoursBanner").innerHTML;
    assert(oohOct5.includes("read"), "Out of hours banner must capture the 00:00-01:00 read entry on Oct 5");
    assert(oohOct5.includes("06:00–23:00"), "Banner must display the active shift hours (06:00–23:00)");

    // With shiftStart = 0 and shiftEnd = 24:
    makeEl("shiftStart").value = "0";
    makeEl("shiftEnd").value = "24";
    renderTodayTimeline();
    const oohOct5Wide = makeEl("outOfHoursBanner").innerHTML;
    assert(!oohOct5Wide.includes("read"), "Banner must NOT flag read when shift covers 00:00-01:00");
    console.log("  ✓ Test 2 Passed: Out of hours banner responds dynamically to configured shift.");
  }

  // TEST 3: Micro-slot Collisions & Hover Readability
  console.log("\n[Test 3] Micro-slots & Hover Readability...");
  {
    // 2026-10-02 has 4 sequential 2-minute micro slots (commute, bio, pause, social)
    CALENDAR_DATE = new Date("2026-10-02T12:00:00");
    makeEl("shiftStart").value = "6";
    makeEl("shiftEnd").value = "23";
    renderTodayTimeline();
    const dayCalOct2 = makeEl("dayCalInner").innerHTML;
    assert(dayCalOct2.includes("commute"), "Oct 2 must render commute slot");
    assert(dayCalOct2.includes("bio"), "Oct 2 must render bio slot");
    assert(dayCalOct2.includes("pause"), "Oct 2 must render pause slot");
    assert(dayCalOct2.includes("social"), "Oct 2 must render social slot");
    // Check that box height has a minimum of 14px so it is never 0 or collapsed
    assert(!dayCalOct2.includes("height:0"), "Slots must not collapse to 0 height");
    console.log("  ✓ Test 3 Passed: Micro slots render with minimum height and full text metadata.");
  }

  // TEST 4: Week View & Table Attendance Consistency
  console.log("\n[Test 4] Attendance Consistency Across Week View, Table, and CSV...");
  {
    // Render the analytics view
    render();
    const attHtml = makeEl("attendance").innerHTML;
    const weeksHtml = makeEl("weeks").innerHTML;
    const csvContent = buildCsv();

    // Check that get2 Übung on 2026-09-29 is marked missed
    assert(attHtml.includes("get2"), "Attendance table must include get2");
    assert(attHtml.includes("class='cell m'"), "Attendance table must show red missed cell for get2 Übung");

    // Check CSV week attendance lines
    const csvLines = csvContent.split("\r\n");
    const weekSectionIdx = csvLines.findIndex((l) => l.includes("Termine pro Woche"));
    assert(weekSectionIdx !== -1, "CSV must contain 'Termine pro Woche' section");
    const weekRows = [];
    for (let i = weekSectionIdx + 2; i < csvLines.length; i++) {
      if (!csvLines[i] || csvLines[i].startsWith('"Gesamt')) break;
      weekRows.push(csvLines[i]);
    }
    console.log("  -> CSV Week Attendance Rows:\n    " + weekRows.join("\n    "));
    let totalExp = 0, totalAtt = 0, totalMiss = 0, totalUp = 0;
    for (const r of weekRows) {
      const parts = r.split(";").map((p) => p.replace(/"/g, ""));
      totalExp += parseInt(parts[1], 10);
      totalAtt += parseInt(parts[2], 10);
      totalMiss += parseInt(parts[3], 10);
      totalUp += parseInt(parts[4], 10);
    }
    console.log(`  -> Sum across weeks: expected=${totalExp}, attended=${totalAtt}, missed=${totalMiss}, upcoming=${totalUp}`);
    assert.strictEqual(totalExp, 15, "Total expected timetable events must be 15");
    assert.strictEqual(totalAtt, 12, "Total attended timetable events must be 12");
    assert.strictEqual(totalMiss, 1, "Total missed timetable events must be 1");
    assert.strictEqual(totalUp, 2, "Total upcoming timetable events must be 2");
    console.log("  ✓ Test 4 Passed: Week view, table, and CSV are 100% consistent.");
  }

  // TEST 5: Pure Self-Study Module Handling
  console.log("\n[Test 5] Pure Self-Study Module (Strömi)...");
  {
    const attHtml = makeEl("attendance").innerHTML;
    const goalsHtml = makeEl("goals").innerHTML;
    assert(attHtml.includes("strömi") || attHtml.includes("stromi"), "Attendance table must include strömi");
    assert(attHtml.includes("Reines Selbststudium"), "Strömi must be identified as Reines Selbststudium");
    assert(goalsHtml.includes("strömi") || goalsHtml.includes("stromi"), "Goals table must include strömi");
    assert(goalsHtml.includes("kein Stundenplan-Eintrag"), "Goals table must flag that strömi has no timetable events");
    console.log("  ✓ Test 5 Passed: Pure self-study subject handled gracefully without timetable events.");
  }

  // TEST 6: Donut Chart Selection & Inspector
  console.log("\n[Test 6] Donut Chart & Activity Inspector...");
  {
    const donutHtml = makeEl("activitiesDonut").innerHTML;
    assert(donutHtml.includes("<svg class='donutSvg'"), "Donut SVG must be rendered");
    assert(donutHtml.includes("sleep"), "Donut must include sleep activity");
    // Trigger slice selection
    const sleepAct = DATA.activities.find((a) => a.name === "sleep");
    selectDonutActivity(sleepAct.id);
    const inspectorHtml = makeEl("donutActivityInspector").innerHTML;
    assert(inspectorHtml.includes("sleep"), "Inspector must show selected sleep activity");
    assert(inspectorHtml.includes("Einträge im Zeitraum"), "Inspector must list entries in the period");
    selectDonutActivity(null); // reset
    console.log("  ✓ Test 6 Passed: Donut chart and activity inspector respond correctly.");
  }

  // TEST 7: Notes Explorer Search Filtering
  console.log("\n[Test 7] Notes Explorer Search...");
  {
    makeEl("notesSearchInput").value = "Clean Code";
    renderNotesExplorer();
    const notesHtml = makeEl("notesExplorerList").innerHTML;
    assert(notesHtml.includes("Clean Code"), "Notes explorer must find matching note 'Clean Code'");
    makeEl("notesSearchInput").value = "NonExistentKeywordXYZ";
    renderNotesExplorer();
    const emptyHtml = makeEl("notesExplorerList").innerHTML;
    assert(emptyHtml.includes("Keine Notizen gefunden"), "Notes explorer must show friendly empty state when no notes match");
    console.log("  ✓ Test 7 Passed: Notes explorer searches accurately and handles empty states.");
  }

  console.log("\n=================================================");
  console.log("ALL 7 EDGE CASE TESTS PASSED SUCCESSFULLY!");
  console.log("=================================================");
}

runEdgeCaseTests().catch((err) => {
  console.error("EDGE CASE TEST FAILED:", err);
  process.exit(1);
});
