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

global.document = { getElementById: makeEl };
global.localStorage = { getItem: () => "", setItem: () => {} };
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
})();
