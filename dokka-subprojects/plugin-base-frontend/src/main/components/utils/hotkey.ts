/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import {detectOsKind, OsKind} from "./os";

type ModifierKey = {
    name: string
    keyArg: string
}

class ModifierKeys {
    static metaKey: ModifierKey = {name: "Command", keyArg: "Meta"}
    static ctrlKey: ModifierKey = {name: "Ctrl", keyArg: "Control"}
    static altKey: ModifierKey = {name: "Alt", keyArg: "Alt"}
    static shiftKey: ModifierKey = {name: "Shift", keyArg: "Shift"}
}

const setOfKeys = [ModifierKeys.altKey, ModifierKeys.shiftKey, ModifierKeys.ctrlKey, ModifierKeys.metaKey]

export class Hotkey {
    private readonly osKind: OsKind;

    constructor() {
        this.osKind = detectOsKind()
    }

    public getOsAccelKeyName() {
        return this.getOsAccelKey().name
    }

    /**
     * Register a hotkey of combination Accel key (Cmd/Ctrl depending on OS).
     * The method also checks that other modifiers key is not pressed to avoid shortcuts intersection.
     * E.g. don't trigger [Ctrl+K] if [Ctrl + Shift + K] pressed
     */
    public registerHotkeyWithAccel = (event: () => void, letter: string) => {
        const osMetaKey = this.getOsAccelKey()
        document.onkeydown = (keyDownEvent) => {
            const isMetaKeyPressed = keyDownEvent.getModifierState(osMetaKey.keyArg)
            const isOtherModifierKeyPressed = setOfKeys
                .filter(key => key !== osMetaKey)
                .map((otherKeys: ModifierKey) => keyDownEvent.getModifierState(otherKeys.keyArg))
                .some(value => value)

            if (isMetaKeyPressed && !isOtherModifierKeyPressed && keyDownEvent.key === letter) {
                keyDownEvent.preventDefault()
                event()
            }
        };
    }

    private getOsAccelKey(): ModifierKey {
        switch (this.osKind) {
            case OsKind.MACOS:
                return ModifierKeys.metaKey
            default:
                return ModifierKeys.ctrlKey
        }
    }
}

