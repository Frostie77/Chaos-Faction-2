// Exercise the production bridge with CF2's real default keyboard actions.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const html = fs.readFileSync(path.join(__dirname, '../app/src/main/assets/www/index.html'), 'utf8');
const script = html.match(/<script>([\s\S]*?)<\/script>/)[1];
const events = [], callbacks = {};
let suspended = false, loaded;
const api = {load: async config => {loaded = config;}, suspend: () => {suspended = true;}, resume: () => {suspended = false;}};
const player = {ruffle: () => api, focus: () => {}, dispatchEvent: event => events.push(event)};
const nodes = {
  game: {appendChild: () => {}, style: {}, getBoundingClientRect: () => ({width:800, height:600})},
  status: {remove: () => {}, textContent: ''}
};
const context = vm.createContext({
  window: {RufflePlayer: {newest: () => ({createPlayer: () => player})}, addEventListener: (name, fn) => {callbacks[name] = fn;}},
  document: {getElementById: id => nodes[id], addEventListener: () => {}, hidden: false},
  KeyboardEvent: class {constructor(type, args) {this.type = type; Object.assign(this,args);}},
  innerWidth: 1600, innerHeight: 900, console, setTimeout, clearTimeout, setInterval, clearInterval
});
vm.runInContext(script, context, {filename:'production-index.html'});
const bridge = context.window;
const count = (code, type) => events.filter(event => event.code === code && event.type === type).length;
(async () => {
  bridge.cf2SetMenuPaused(true);
  await callbacks.load();
  assert.equal(loaded.url, '/game/chaos-faction-2.swf');
  assert(suspended, 'Menu opened during loading must pause the new runtime');
  bridge.cf2SetMenuPaused(false);
  assert(!suspended);

  for (const [action,direction,code] of [['jump','up','ArrowUp'], ['shield','down','ArrowDown']]) {
    bridge.cf2Key(action,true,'face');
    bridge.cf2Key(direction,true,'pad');
    bridge.cf2Key(action,false,'face');
    assert.equal(count(code,'keyup'),0,'Movement pad must retain the shared key');
    bridge.cf2Key(direction,false,'pad');
    assert.equal(count(code,'keydown'),1);
    assert.equal(count(code,'keyup'),1);
  }

  bridge.cf2Key('attack1',true,'controller');
  bridge.cf2Key('attack1',true,'touch');
  await new Promise(resolve => setTimeout(resolve,650));
  assert.equal(count('KeyZ','keydown'),1,'Holding attack must not generate repeated presses');
  bridge.cf2Key('attack1',false,'touch');
  assert.equal(count('KeyZ','keyup'),0,'Controller still owns the held attack');
  bridge.cf2SetMenuPaused(true);
  assert.equal(count('KeyZ','keyup'),1);
  const before = events.length;
  bridge.cf2Key('drop',true,'ignored');
  assert.equal(events.length,before,'Menu must block game input');
  bridge.cf2SetBackgroundPaused(true);
  bridge.cf2SetMenuPaused(false);
  assert(suspended,'Closing settings while backgrounded must leave the runtime paused');
  bridge.cf2SetBackgroundPaused(false);
  assert(!suspended);
  assert.equal(events.length,before,'Resume must not replay held inputs');

  bridge.cf2Key('melee',true,'bumper');
  assert.equal(events.at(-1).code,'ShiftLeft');
  assert.equal(events.at(-1).shiftKey,true);
  bridge.cf2Key('attack1',true,'face');
  assert.equal(events.at(-1).code,'KeyZ');
  assert.equal(events.at(-1).shiftKey,true,'Weapon melee needs Shift on the attack event');
  bridge.cf2Key('attack2',true,'face');
  assert.equal(events.at(-1).code,'KeyX');
  assert.equal(events.at(-1).shiftKey,true);
  bridge.cf2Key('melee',false,'bumper');
  assert.equal(events.at(-1).code,'ShiftLeft');
  assert.equal(events.at(-1).shiftKey,false);
  bridge.cf2Key('attack1',false,'face');
  bridge.cf2Key('attack2',false,'face');

  for (const [action,code] of [['drop','KeyD'],['pause','Space']]) {
    bridge.cf2Key(action,true,'controller');
    bridge.cf2Key(action,false,'controller');
    assert.equal(count(code,'keydown'),1,action);
    assert.equal(count(code,'keyup'),1,action);
  }
  bridge.cf2Key('left',true,'stick');
  bridge.cf2Key('melee',true,'touch');
  bridge.cf2SetBackgroundPaused(true);
  assert.equal(count('ArrowLeft','keyup'),1);
  assert.equal(count('ShiftLeft','keyup'),2);
  bridge.cf2SetBackgroundPaused(false);
  const cleared = events.length;
  bridge.cf2Key('unknown',true,'invalid');
  bridge.cf2ClearKeys();
  assert.equal(events.length,cleared);
  bridge.cf2SetDisplayMode('stretch');
  assert.equal(nodes.game.style.transform,'scale(2,1.5)');
  bridge.cf2SetDisplayMode('original');
  assert.equal(nodes.game.style.transform,'');
  console.log('PASS: CF2 jump/shield ownership, attacks, Shift modifier, drop/pause, lifecycle and display modes.');
})().catch(error => {console.error(error);process.exitCode = 1;});
