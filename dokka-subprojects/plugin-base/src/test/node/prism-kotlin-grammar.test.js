// Tests for the Prism.js Kotlin grammar, specifically that keywords preceded
// by a hyphen are not highlighted. Regression test for:
// https://github.com/Kotlin/dokka/issues/XXXX
//
// Run: node --test src/test/node/prism-kotlin-grammar.test.js
// Requires Node.js >= 18 (node:test built-in).

"use strict";

const { test } = require("node:test");
const assert = require("node:assert/strict");
const path = require("path");

const Prism = require(path.resolve(
    __dirname,
    "../../main/resources/dokka/scripts/prism.js"
));

/**
 * Returns keyword token contents found in the given code snippet.
 * @param {string} code
 * @returns {string[]}
 */
function keywordsIn(code) {
    return Prism.tokenize(code, Prism.languages.kotlin)
        .filter(t => typeof t === "object" && t.type === "keyword")
        .map(t => (Array.isArray(t.content) ? t.content.join("") : t.content));
}

// --- Regression: keywords after a hyphen must not be highlighted ---

test("'external' after hyphen is not a keyword token", () => {
    const keywords = keywordsIn("apollo-gradle-plugin-external");
    assert.equal(keywords.length, 0,
        `Expected no keywords, got: ${JSON.stringify(keywords)}`);
});

test("'in' after hyphen is not a keyword token", () => {
    const keywords = keywordsIn("check-in");
    assert.equal(keywords.length, 0,
        `Expected no keywords, got: ${JSON.stringify(keywords)}`);
});

test("'is' after hyphen is not a keyword token", () => {
    const keywords = keywordsIn("type-is-valid");
    assert.equal(keywords.length, 0,
        `Expected no keywords, got: ${JSON.stringify(keywords)}`);
});

// --- Keywords in real Kotlin inline expressions must still be highlighted ---

test("'external' as a standalone modifier is a keyword", () => {
    const keywords = keywordsIn("external fun foo()");
    assert.ok(keywords.includes("external"),
        `Expected 'external' in keywords, got: ${JSON.stringify(keywords)}`);
    assert.ok(keywords.includes("fun"),
        `Expected 'fun' in keywords, got: ${JSON.stringify(keywords)}`);
});

test("'val' in a variable declaration is a keyword", () => {
    const keywords = keywordsIn("val x: Int = 0");
    assert.ok(keywords.includes("val"),
        `Expected 'val' in keywords, got: ${JSON.stringify(keywords)}`);
});

test("'fun interface' keywords are highlighted", () => {
    const keywords = keywordsIn("fun interface EventHandler");
    assert.ok(keywords.includes("fun"),
        `Expected 'fun' in keywords, got: ${JSON.stringify(keywords)}`);
    assert.ok(keywords.includes("interface"),
        `Expected 'interface' in keywords, got: ${JSON.stringify(keywords)}`);
});

// --- Original dot-prefix protection must still work ---

test("'external' after dot is not a keyword token (dot-prefix guard)", () => {
    // 'object' is a Kotlin keyword and will appear; 'external' must not
    const keywords = keywordsIn("object.external");
    assert.ok(!keywords.includes("external"),
        `'external' must not be a keyword after a dot, got: ${JSON.stringify(keywords)}`);
});
