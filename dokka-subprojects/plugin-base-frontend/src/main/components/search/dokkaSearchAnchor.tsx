/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import React from "react";
import Tooltip from '@jetbrains/ring-ui/components/tooltip/tooltip';
import SearchIcon from 'react-svg-loader!../assets/searchIcon.svg';
import {CustomAnchorProps} from "./types";
import {Hotkey} from "../utils/hotkey";

const HOTKEY_LETTER = 'k'
const HOTKEY_TOOLTIP_DISPLAY_DELAY = 0.5 * 1000 // seconds

export const DokkaSearchAnchor = ({wrapperProps, buttonProps, popup}: CustomAnchorProps) => {
    const hotkeys = new Hotkey()
    hotkeys.registerHotkeyWithAccel(buttonProps.onClick, HOTKEY_LETTER)

    return (
        <span {...wrapperProps}>
            <Tooltip
                title={`${hotkeys.getOsAccelKeyName()} + ${HOTKEY_LETTER.toUpperCase()}`}
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
