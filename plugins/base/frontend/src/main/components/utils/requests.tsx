import { IWindow } from "../search/types"

export const relativizeUrlForRequest = (filePath: string) : string => {
    const pathToRoot = (window as IWindow).pathToRoot
    const relativePath = pathToRoot == "" ? "." : pathToRoot
    return relativePath.endsWith('/') ? `${relativePath}${filePath}` : `${relativePath}/${filePath}`
}