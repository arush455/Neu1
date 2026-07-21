; holdclick.ahk — Windows-only "hold left mouse button" toggle (AutoHotkey v2)
;
; Same idea as holdclick.py, but no Python needed. Install AutoHotkey v2 from
; https://www.autohotkey.com/ , then double-click this file.
;
;   F8   toggle: start / stop holding the left button
;   F10  quit (releases the button first so it never gets stuck)
;
; Plain input tool. No game hooks, no anti-cheat evasion. Use it only where it's
; allowed (your own / friends' server, singleplayer).

#Requires AutoHotkey v2.0

holding := false

F8:: {
    global holding
    holding := !holding
    if (holding) {
        Click("Left Down")
        ToolTip("holdclick: HOLDING (F8 to stop)")
    } else {
        Click("Left Up")
        ToolTip("holdclick: released (F8 to hold)")
    }
    SetTimer(() => ToolTip(), -1500)
}

F10:: {
    global holding
    if (holding)
        Click("Left Up")   ; never leave the button stuck down
    ExitApp()
}
