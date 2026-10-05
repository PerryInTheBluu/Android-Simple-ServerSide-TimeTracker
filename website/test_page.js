// Runs the analysis page logic headless in node with real server data
// and prints the computed tables. Compare against reference.py.
"use strict";
const fs = require("fs");

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
      classList: { add() {}, remove() {} },
      addEventListener() {},
    };
  }
  return elements[id];
}

global.document = { getElementById: makeEl, createElement: () => ({ style: {} }),
  documentElement: { setAttribute() {} } };
global.localStorage = { getItem: () => "", setItem: () => {} };
global.location = { hash: "", origin: "http://x", pathname: "/" };
const pullData = JSON.parse(fs.readFileSync(process.argv[2], "utf8"));
global.fetch = async () => ({
  ok: true,
  status: 200,
  text: async () => "",
  json: async () => pullData,
});

const vm = require("vm");
vm.runInThisContext(script);

(async () => {
  makeEl("token").value = "test-token";
  await load();
  if (!elements["error"].classList) throw new Error("no classlist");
  const errText = elements["error"].textContent;
  if (errText) console.log("ERROR SHOWN:", errText);
  console.log("=== stats innerHTML ===");
  console.log(elements["stats"].innerHTML.replace(/></g, ">\n<"));
  console.log("=== attendance table ===");
  console.log(elements["attendance"].innerHTML.replace(/></g, ">\n<"));
  console.log("=== goals table ===");
  console.log(elements["goals"].innerHTML.replace(/></g, ">\n<"));
  console.log("=== activities table ===");
  console.log(elements["activities"].innerHTML.replace(/></g, ">\n<"));
  console.log("=== todos ===");
  console.log(elements["todos"].innerHTML.replace(/></g, ">\n<"));
  console.log("=== daily chart svg (first 400 chars) ===");
  console.log(elements["dailyChart"].innerHTML);
  console.log("=== csv ===");
  console.log(buildCsv());
  console.log("=== categories table ===");
  console.log(elements["categories"].innerHTML.replace(/></g, ">\n<"));
  console.log("=== weeks table ===");
  console.log(elements["weeks"].innerHTML.replace(/></g, ">\n<"));
  console.log("=== meal chart ===");
  console.log(elements["mealChart"].innerHTML);
  console.log("=== chart legend ===");
  console.log(elements["chartLegend"].innerHTML);
  chartMode = "week";
  render();
  console.log("=== week chart titles ===");
  const titles = elements["dailyChart"].innerHTML.match(/<title>[^<]+<\/title>/g) || [];
  titles.forEach((t) => console.log(t.replace(/<\/?title>/g, "")));
  chartMode = "month";
  render();
  console.log("=== month chart titles ===");
  (elements["dailyChart"].innerHTML.match(/<title>[^<]+<\/title>/g) || [])
    .forEach((t) => console.log(t.replace(/<\/?title>/g, "")));
  // Exclude the Uni category everywhere and check that the stats shrink.
  const uniCat = (pullData.categories || []).find((c) => !c.deleted_at && c.name === "Uni");
  if (uniCat) {
    chartMode = "day";
    FILTER.cats[uniCat.id] = "exc";
    render();
    console.log("=== stats with Uni excluded ===");
    console.log(elements["stats"].innerHTML.replace(/></g, ">\n<"));
    console.log("=== activities with Uni excluded ===");
    console.log(elements["activities"].innerHTML.replace(/></g, ">\n<"));
    console.log("=== csv with filter (category section) ===");
    console.log(buildCsv().split("\r\n").filter((l) => l.includes("Hinweis")).join("\n"));
  }

})();
