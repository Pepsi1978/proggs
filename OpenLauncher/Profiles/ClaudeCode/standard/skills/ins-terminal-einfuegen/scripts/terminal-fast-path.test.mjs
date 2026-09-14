import test from 'node:test';
import assert from 'node:assert/strict';
import { createTerminalFastPath } from './terminal-fast-path.mjs';

function fixture() {
  const calls = [];
  let failCapture = false;
  const window = { id: 'test-only-window' };
  const sky = {
    async get_window_state(options) {
      calls.push(['observe', options.include_text]);
      if (failCapture) throw new Error('capture failed');
      return { window, screenshots: [{ id: 'test-only-screenshot' }] };
    },
    async type_text({ text }) { calls.push(['type', text]); },
    async press_key({ key }) { calls.push(['key', key]); },
  };
  return {
    calls,
    helper: createTerminalFastPath(sky, window, { renderDelayMs: 0 }),
    failCapture() { failCapture = true; },
    restoreCapture() { failCapture = false; },
  };
}

test('construction and observation never type or send; typing does not press Enter', async () => {
  const f = fixture();
  assert.deepEqual(f.calls, []);
  await assert.rejects(f.helper.send());
  await assert.rejects(f.helper.type('test'));
  assert.deepEqual(f.calls, []);
  await f.helper.observe();
  await f.helper.type('Grüße, "Test" und $literal');
  assert.deepEqual(f.calls, [
    ['observe', true], ['type', 'Grüße, "Test" und $literal'], ['observe', true],
  ]);
  await f.helper.send();
  await assert.rejects(f.helper.send());
  assert.equal(f.calls.filter(([kind]) => kind === 'key').length, 1);
});

test('failed refresh after Enter never enables a duplicate send, even after re-observation', async () => {
  const f = fixture();
  await f.helper.observe();
  await f.helper.type('test');
  f.failCapture();
  await assert.rejects(f.helper.send(), /capture failed/);
  f.restoreCapture();
  await f.helper.observe();
  await assert.rejects(f.helper.send());
  assert.equal(f.calls.filter(([kind]) => kind === 'key').length, 1);
});

test('failed refresh after typing neither sends nor retries typing', async () => {
  const f = fixture();
  await f.helper.observe();
  f.failCapture();
  await assert.rejects(f.helper.type('test'), /capture failed/);
  await assert.rejects(f.helper.send());
  assert.equal(f.calls.filter(([kind]) => kind === 'type').length, 1);
  assert.equal(f.calls.filter(([kind]) => kind === 'key').length, 0);
});

test('control characters are rejected before UI input; invalidation blocks pending send', async () => {
  const f = fixture();
  await f.helper.observe();
  await assert.rejects(f.helper.type('test\n'), /control characters/);
  assert.equal(f.calls.filter(([kind]) => kind === 'type').length, 0);
  await f.helper.observe();
  await f.helper.type('test');
  f.helper.invalidate();
  await assert.rejects(f.helper.send());
  assert.equal(f.calls.filter(([kind]) => kind === 'key').length, 0);
});
