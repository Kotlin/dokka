/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

declare module 'fast-text-encoding' {
    export class TextEncoder {
        constructor(label?: string, options?: any);
        encoding: string;
        encode(input?: string, options?: any): Uint8Array;
    }

    export class TextDecoder {
        constructor(label?: string, options?: any);
        encoding: string;
        fatal: boolean;
        ignoreBOM: boolean;
        decode(input?: Uint8Array | ArrayBuffer | ArrayBufferView, options?: any): string;
    }
}