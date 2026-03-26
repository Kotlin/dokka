/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import { TextEncoder, TextDecoder } from 'fast-text-encoding';

// Basic global objects for the GraalJS environment
(global as any).self = global;
(global as any).window = global;

if (typeof (global as any).TextEncoder === 'undefined') {
    (global as any).TextEncoder = TextEncoder;
}
if (typeof (global as any).TextDecoder === 'undefined') {
    (global as any).TextDecoder = TextDecoder;
}

// Polyfill setTimeout/clearTimeout for React SSR
if (typeof setTimeout === 'undefined') {
    (global as any).setTimeout = function (func: Function, delay: number) {
        return func(); // In synchronous SSR just call the function
    };
    (global as any).clearTimeout = function (id: any) {};
}

// Polyfill MessageChannel for React 19 Scheduler
if (typeof MessageChannel === 'undefined') {
    (global as any).MessageChannel = class MessageChannel {
        port1: any;
        port2: any;

        constructor() {
            this.port1 = { onmessage: null };
            this.port2 = { onmessage: null };

            this.port1.postMessage = (msg: any) => {
                if (this.port2.onmessage) this.port2.onmessage({ data: msg });
            };

            this.port2.postMessage = (msg: any) => {
                if (this.port1.onmessage) this.port1.onmessage({ data: msg });
            };
        }
    };
}