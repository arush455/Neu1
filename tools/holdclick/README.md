# holdclick

A dead-simple **"hold left mouse button" toggle**. Press a hotkey once to hold
the left mouse button down; press it again to release. Handy for mining a long
vein (e.g. hardstone) without keeping the button pressed by hand.

> ⚠️ This is a plain OS-level input tool. It does **not** hook into Minecraft,
> read game memory, hide itself, randomise timings, or try to defeat any
> anti-cheat — it just holds a mouse button. **Only use it where it's allowed:**
> your own server, a friends' server that permits it, or singleplayer.
> Unattended input automation is against the rules on public servers like
> Hypixel and can get you banned. Don't use it there.

## Two versions — pick one

### Windows (easiest): AutoHotkey

1. Install [AutoHotkey v2](https://www.autohotkey.com/).
2. Double-click `holdclick.ahk`.

### Cross-platform: Python

Works on Windows, macOS, and Linux (X11).

```bash
pip install -r requirements.txt      # installs pynput
python holdclick.py
```

## Keys (both versions)

| Key  | Action                                            |
|------|---------------------------------------------------|
| `F8` | Toggle — start / stop holding the left button     |
| `F10`| Quit (releases the button first so it never sticks)|

You can change the keys at the top of `holdclick.py` (`TOGGLE_KEY` / `QUIT_KEY`)
or in `holdclick.ahk` (the `F8::` / `F10::` labels).

## Good to know

- The hold is **global**: it keeps holding even if Minecraft loses focus. So
  toggle off (`F8`) or quit (`F10`) before you alt-tab away.
- **Linux:** needs an X server. On Wayland, run under Xorg/XWayland — otherwise
  input injection won't work.
- **macOS:** grant your terminal *Accessibility* permission in
  System Settings → Privacy & Security → Accessibility.
