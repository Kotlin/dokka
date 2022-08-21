import React from "react";
import Tooltip from '@jetbrains/ring-ui/components/tooltip/tooltip';
import SearchIcon from 'react-svg-loader!../assets/searchIcon.svg';
import {CustomAnchorProps} from "./types";
import {detectOsKind, OsKind} from "../utils/os";

const HOTKEY_LETTER = 'k'
const HOTKEY_TOOLTIP_DISPLAY_DELAY = 0.5 * 1000 // seconds

export const DokkaSearchAnchor = ({wrapperProps, buttonProps, popup}: CustomAnchorProps) => {
    const currentOs = detectOsKind()
    registerSearchHotkey(currentOs, buttonProps)

    return (
        <span {...wrapperProps}>
            <Tooltip
                title={`${osMetaKeyName(currentOs)} + ${HOTKEY_LETTER.toUpperCase()}`}
                delay={HOTKEY_TOOLTIP_DISPLAY_DELAY}
                popupProps={{className: "search-hotkey-popup"}}
            >
                <button type="button" {...buttonProps}>
                    <SearchIcon/>
                </button>
            </Tooltip>
            {popup}
        </span>
    )
}


const registerSearchHotkey = (currentOs: OsKind, buttonProps: any) => {
    document.onkeydown = (keyDownEvent) => {
        if (isOsMetaKeyPressed(currentOs, keyDownEvent) && keyDownEvent.key === HOTKEY_LETTER) {
            keyDownEvent.preventDefault()
            buttonProps.onClick()
        }
    };
}

const isOsMetaKeyPressed = (currentOs: OsKind, keyEvent: KeyboardEvent): Boolean => {
    switch (osMetaKeyName(currentOs)) {
        case "Command":
            return keyEvent.metaKey
        case "Ctrl":
            return keyEvent.ctrlKey
        default:
            return keyEvent.ctrlKey
    }
}

const osMetaKeyName = (currentOs: OsKind): String => {
    switch (currentOs) {
        case OsKind.MACOS:
            return "Command"
        default:
            return "Ctrl"
    }
}