// Small wrappers around the existing sky API. No input on import or creation.
// The caller must inspect each returned screenshot before the next action.
export function createTerminalFastPath(sky, initialWindow, { renderDelayMs = 250 } = {}) {
  if (!initialWindow || !Number.isInteger(renderDelayMs) || renderDelayMs < 0 || renderDelayMs > 1000) {
    throw new Error('A previously observed window and a render delay from 0 to 1000 ms are required.');
  }
  let window = initialWindow;
  let state = null;
  let pending = false;
  let busy = false;
  const waitForRender = () => new Promise(resolve => setTimeout(resolve, renderDelayMs));

  async function capture(includeText) {
    state = null;
    const next = await sky.get_window_state({ window, include_screenshot: true, include_text: includeText });
    if (!next?.window) throw new Error('No current window returned.');
    window = next.window;
    state = next;
    return next;
  }

  async function run(action) {
    if (busy) throw new Error('Finish and inspect the current action before starting another.');
    busy = true;
    try {
      return await action();
    } catch (error) {
      state = null;
      pending = false;
      throw error;
    } finally {
      busy = false;
    }
  }

  return {
    observe: () => run(() => capture(true)),
    type: text => run(async () => {
      if (!state) throw new Error('Observe and inspect the target before typing.');
      if (pending) throw new Error('An earlier input is pending; inspect it instead of typing again.');
      if (typeof text !== 'string' || !text.trim() || /[\x00-\x1f\x7f]/u.test(text)) {
        throw new Error('Provide nonempty literal text without control characters; Enter is separate.');
      }
      state = null;
      await sky.type_text({ window, text });
      await waitForRender();
      const next = await capture(true);
      pending = true;
      return next;
    }),
    send: () => run(async () => {
      if (!state || !pending) throw new Error('No observed pending input. Never retry an uncertain send blindly.');
      // Consume before Enter: even a failed refresh must never cause an automatic repeat.
      pending = false;
      state = null;
      await sky.press_key({ window, key: 'Return' });
      await waitForRender();
      return capture(false);
    }),
    invalidate: () => {
      if (busy) throw new Error('An action is in flight; await its outcome before invalidating.');
      state = null;
      pending = false;
    },
  };
}
