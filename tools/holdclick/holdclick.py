#!/usr/bin/env python3
"""
holdclick — a dead-simple "hold left mouse button" toggle.

Press a hotkey once to hold the left mouse button down; press it again to
release. Useful for mining a long vein (e.g. hardstone) on a private server /
singleplayer world where you don't want to keep the button pressed by hand.

This is a plain OS-level input tool. It does NOT hook into Minecraft, read game
memory, hide itself, randomise anything, or try to defeat any anti-cheat. It
just holds a mouse button. Only use it where it's allowed (your own server,
friends' server, singleplayer).

Keys (defaults, change them below):
    F8   toggle: start / stop holding the left button
    F10  quit (also releases the button so it never gets stuck)

Install once:
    pip install pynput

Run:
    python holdclick.py

Notes:
    - The hold is global: it keeps holding even if Minecraft loses focus, so
      remember to toggle off (F8) or quit (F10) before you alt-tab to do
      something else.
    - Linux: needs an X server. On Wayland, run the game / session under Xorg
      or XWayland, otherwise pynput can't inject input.
    - macOS: grant your terminal "Accessibility" permission in
      System Settings -> Privacy & Security -> Accessibility.
"""

from pynput import mouse, keyboard

# --- config -----------------------------------------------------------------
TOGGLE_KEY = keyboard.Key.f8   # start / stop holding
QUIT_KEY = keyboard.Key.f10    # release + exit
BUTTON = mouse.Button.left     # which mouse button to hold
# ----------------------------------------------------------------------------

_mouse = mouse.Controller()
_holding = False


def _set_hold(on: bool) -> None:
    global _holding
    if on == _holding:
        return
    _holding = on
    if on:
        _mouse.press(BUTTON)
        print("[holdclick] HOLDING left button  (F8 to stop)")
    else:
        _mouse.release(BUTTON)
        print("[holdclick] released             (F8 to hold)")


def _on_press(key):
    if key == TOGGLE_KEY:
        _set_hold(not _holding)
    elif key == QUIT_KEY:
        _set_hold(False)  # never leave the button stuck down
        print("[holdclick] bye")
        return False  # stops the listener


def main() -> None:
    print(__doc__.strip())
    print("\n[holdclick] ready. F8 = toggle hold, F10 = quit.\n")
    try:
        with keyboard.Listener(on_press=_on_press) as listener:
            listener.join()
    finally:
        # Safety net: if we crash or get Ctrl-C'd, don't leave the button held.
        if _holding:
            _mouse.release(BUTTON)


if __name__ == "__main__":
    main()
