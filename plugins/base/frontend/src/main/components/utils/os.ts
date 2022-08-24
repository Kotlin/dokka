export enum OsKind{
    WINDOWS,
    MACOS,
    LINUX,
    OTHER
}

export const detectOsKind = (): OsKind => {
    const userAgent = navigator.userAgent
    if(userAgent.includes("Mac")) return OsKind.MACOS
    else if (userAgent.includes("Win")) return OsKind.WINDOWS
    else if (userAgent.includes("Linux")) return OsKind.LINUX
    else return OsKind.OTHER
}