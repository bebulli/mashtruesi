"use strict";

// ---- State ----
const state = {
  players: [],        // input order
  game: null,         // CreateGameResponse
  revealOrder: [],    // [{name, position}] in input order (hides speaking order)
  revealIndex: 0,
  categoryWordCounts: {},
  timerInterval: null,
  timerRemaining: 0,
  scores: {},          // playerName -> wins, accumulated across rounds
};

// ---- Helpers ----
const $ = (id) => document.getElementById(id);

function show(screenId) {
  document.querySelectorAll(".screen").forEach((s) => s.classList.add("hidden"));
  $(screenId).classList.remove("hidden");
}

async function api(method, path, body) {
  const opts = { method, headers: { "Content-Type": "application/json" } };
  if (body !== undefined) opts.body = JSON.stringify(body);
  const res = await fetch(path, opts);
  let data = null;
  try { data = await res.json(); } catch (e) { data = null; }
  if (!res.ok) {
    const msg = data && data.message ? data.message : "Gabim (" + res.status + ")";
    throw new Error(msg);
  }
  return data;
}

// ---- Setup screen ----
function renderPlayers() {
  const ul = $("playerList");
  ul.innerHTML = "";
  state.players.forEach((name, i) => {
    const li = document.createElement("li");
    const span = document.createElement("span");
    span.textContent = name;
    const btn = document.createElement("button");
    btn.className = "remove";
    btn.textContent = "\u00d7";
    btn.title = "Hiq";
    btn.onclick = () => { state.players.splice(i, 1); renderPlayers(); };
    li.appendChild(span);
    li.appendChild(btn);
    ul.appendChild(li);
  });
}

function addPlayer() {
  const input = $("playerInput");
  const name = input.value.trim();
  $("setupError").textContent = "";
  if (!name) return;
  if (state.players.some((p) => p.toLowerCase() === name.toLowerCase())) {
    $("setupError").textContent = "Ky emer ekziston tashme.";
    return;
  }
  state.players.push(name);
  input.value = "";
  input.focus();
  renderPlayers();
}

async function loadCategories(preserveSelection) {
  try {
    const previous = preserveSelection ? $("categorySelect").value : null;
    const cats = await api("GET", "/api/categories");
    const sel = $("categorySelect");
    sel.innerHTML = "";
    state.categoryWordCounts = {};
    cats.forEach((c) => {
      state.categoryWordCounts[c.name] = c.wordCount;
      const opt = document.createElement("option");
      opt.value = c.name;
      opt.textContent = c.name + " (" + c.wordCount + ")";
      sel.appendChild(opt);
    });
    if (previous && state.categoryWordCounts[previous] !== undefined) {
      sel.value = previous;
    }
    updateCategoryPreview();
  } catch (e) {
    $("setupError").textContent = "Nuk u ngarkuan kategorite: " + e.message;
  }
}

// ---- Category preview ----
function updateCategoryPreview() {
  const name = $("categorySelect").value;
  const count = state.categoryWordCounts[name];
  $("categoryPreview").textContent = (count === undefined)
    ? ""
    : "Kjo kategori ka " + count + " fjale.";
}

// ---- Custom word input ----
async function addWord() {
  const msg = $("addWordMsg");
  msg.textContent = "";
  const category = $("categorySelect").value;
  const text = $("newWordText").value.trim();
  const hint = $("newWordHint").value.trim();
  if (!category) {
    msg.textContent = "Zgjidh nje kategori se pari.";
    return;
  }
  if (!text) {
    msg.textContent = "Shkruaj nje fjale.";
    return;
  }
  try {
    await api("POST", "/api/words", { category, text, hint: hint || null });
    $("newWordText").value = "";
    $("newWordHint").value = "";
    msg.textContent = "U shtua '" + text + "' ne '" + category + "'.";
    await loadCategories(true);
  } catch (e) {
    msg.textContent = e.message;
  }
}

async function startGame() {
  $("setupError").textContent = "";
  if (state.players.length < 4) {
    $("setupError").textContent = "Duhen te pakten 4 lojtare.";
    return;
  }
  const rawCount = $("imposterCount").value;
  const req = {
    players: state.players.slice(),
    category: $("categorySelect").value,
    imposterCount: rawCount === "" ? null : parseInt(rawCount, 10),
    hintEnabled: $("hintEnabled").checked,
    imposterFirstWeight: parseFloat($("weight").value),
  };
  try {
    const game = await api("POST", "/api/games", req);
    state.game = game;
    // Reveal in INPUT order so the pass-the-phone sequence never leaks who
    // the server picked to speak first.
    state.revealOrder = state.players.map((name) => ({
      name,
      position: game.turnOrder.indexOf(name),
    }));
    state.revealIndex = 0;
    nextHandoff();
  } catch (e) {
    $("setupError").textContent = e.message;
  }
}

// ---- Reveal flow ----
function nextHandoff() {
  if (state.revealIndex >= state.revealOrder.length) {
    showRoundStart();
    return;
  }
  $("handoffName").textContent = state.revealOrder[state.revealIndex].name;
  show("screen-handoff");
}

async function revealCurrent() {
  const entry = state.revealOrder[state.revealIndex];
  try {
    const r = await api("GET", "/api/games/" + state.game.gameId + "/reveal/" + entry.position);
    const card = $("revealCard");
    if (r.imposter) {
      card.classList.add("imposter");
      $("revealRole").textContent = "MASHTRUESI";
      $("revealWord").textContent = "Ti je mashtruesi!";
      $("revealHint").textContent = r.hint ? "Ndihmese: " + r.hint : "Pa ndihmese.";
    } else {
      card.classList.remove("imposter");
      $("revealRole").textContent = "Fjala jote";
      $("revealWord").textContent = r.word;
      $("revealHint").textContent = "";
    }
    show("screen-reveal");
  } catch (e) {
    $("setupError").textContent = e.message;
    show("screen-setup");
  }
}

function hideAndNext() {
  state.revealIndex += 1;
  nextHandoff();
}

function showRoundStart() {
  // First speaker = first entry of the server's turn order.
  $("firstSpeaker").textContent = state.game.turnOrder[0];
  show("screen-round");
  startTimer();
}

// ---- Round timer ----
function startTimer() {
  stopTimer();
  const timerEl = $("roundTimer");
  if (!$("timerEnabled").checked) {
    timerEl.classList.add("hidden");
    return;
  }
  state.timerRemaining = parseInt($("timerDuration").value, 10);
  timerEl.classList.remove("hidden");
  renderTimer();
  state.timerInterval = setInterval(() => {
    state.timerRemaining -= 1;
    renderTimer();
    if (state.timerRemaining <= 0) {
      stopTimer();
      show("screen-catch");
    }
  }, 1000);
}

function renderTimer() {
  const m = Math.floor(state.timerRemaining / 60);
  const s = state.timerRemaining % 60;
  $("roundTimer").textContent = m + ":" + String(s).padStart(2, "0");
}

function stopTimer() {
  if (state.timerInterval) {
    clearInterval(state.timerInterval);
    state.timerInterval = null;
  }
}

// ---- End flow ----
async function finishRound(caught) {
  stopTimer();
  try {
    const result = await api("POST", "/api/games/" + state.game.gameId + "/end", {
      imposterCaught: caught,
    });
    $("resultVerdict").textContent = caught ? "Grupi fitoi!" : "Mashtruesi fitoi!";
    $("resultWord").textContent = result.secretWord;
    $("resultImposters").textContent = result.imposterNames.join(", ");
    recordScores(caught, result.imposterNames);
    renderScoreboard();
    show("screen-result");
  } catch (e) {
    alert(e.message);
  }
}

// ---- Scoreboard ----
function recordScores(caught, imposterNames) {
  const winners = caught
    ? state.players.filter((p) => !imposterNames.includes(p))
    : imposterNames;
  winners.forEach((name) => {
    state.scores[name] = (state.scores[name] || 0) + 1;
  });
}

function renderScoreboard() {
  const body = $("scoreboardBody");
  body.innerHTML = "";
  const names = Object.keys(state.scores).sort((a, b) => state.scores[b] - state.scores[a]);
  names.forEach((name) => body.appendChild(statLine(name, state.scores[name])));
}

function playAgain() {
  // Same players, fresh round.
  state.game = null;
  state.revealOrder = [];
  state.revealIndex = 0;
  $("revealCard").classList.remove("imposter");
  startGame();
}

function newGame() {
  state.game = null;
  state.revealOrder = [];
  state.revealIndex = 0;
  state.scores = {};
  $("revealCard").classList.remove("imposter");
  show("screen-setup");
}

// ---- Stats ----
async function showStats() {
  const body = $("statsBody");
  body.innerHTML = "<p class='muted'>Po ngarkohet...</p>";
  show("screen-stats");
  try {
    const s = await api("GET", "/api/stats");
    body.innerHTML = "";
    body.appendChild(statLine("Lojera aktive", s.activeGames));
    body.appendChild(statLine("Lojera gjithsej", s.totalGames));
    const cats = Object.keys(s.playsByCategory || {});
    if (cats.length) {
      body.appendChild(sectionTitle("Sipas kategorise"));
      cats.forEach((k) => body.appendChild(statLine(k, s.playsByCategory[k])));
    }
    if (s.recentGames && s.recentGames.length) {
      body.appendChild(sectionTitle("Lojerat e fundit"));
      s.recentGames.forEach((g) => {
        const label = g.category + " \u00b7 " + g.playerCount + " lojtare";
        const val = g.ended ? (g.imposterCaught ? "I kapur" : "Iku") : "Ne vazhdim";
        body.appendChild(statLine(label, val));
      });
    }
  } catch (e) {
    body.innerHTML = "<p class='error'>" + e.message + "</p>";
  }
}

function statLine(label, value) {
  const div = document.createElement("div");
  div.className = "stat-line";
  const a = document.createElement("span");
  a.textContent = label;
  const b = document.createElement("strong");
  b.textContent = value;
  div.appendChild(a);
  div.appendChild(b);
  return div;
}

function sectionTitle(text) {
  const p = document.createElement("p");
  p.className = "stat-section-title";
  p.textContent = text;
  return p;
}

async function clearStats() {
  try {
    await api("DELETE", "/api/stats");
    showStats();
  } catch (e) {
    alert(e.message);
  }
}

// ---- Wiring ----
function init() {
  $("addPlayer").onclick = addPlayer;
  $("playerInput").addEventListener("keydown", (e) => { if (e.key === "Enter") addPlayer(); });
  $("startGame").onclick = startGame;
  $("revealBtn").onclick = revealCurrent;
  $("hideBtn").onclick = hideAndNext;
  $("endRound").onclick = () => { stopTimer(); show("screen-catch"); };
  $("caughtYes").onclick = () => finishRound(true);
  $("caughtNo").onclick = () => finishRound(false);
  $("playAgain").onclick = playAgain;
  $("newGame").onclick = newGame;
  $("navStats").onclick = showStats;
  $("backFromStats").onclick = () => show("screen-setup");
  $("clearStats").onclick = clearStats;
  $("weight").addEventListener("input", (e) => {
    $("weightLabel").textContent = parseFloat(e.target.value).toFixed(2) + "\u00d7";
  });
  $("categorySelect").addEventListener("change", updateCategoryPreview);
  $("addWordBtn").onclick = addWord;

  loadCategories();
  renderPlayers();
  show("screen-setup");
}

document.addEventListener("DOMContentLoaded", init);
