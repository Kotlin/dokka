/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import {IWindow} from "../search/types"

export const relativizeUrlForRequest = (filePath: string) : string => {
    const pathToRoot = (window as IWindow).pathToRoot
    const relativePath = pathToRoot == "" ? "." : pathToRoot
    return relativePath.endsWith('/') ? `${relativePath}${filePath}` : `${relativePath}/${filePath}`
}
