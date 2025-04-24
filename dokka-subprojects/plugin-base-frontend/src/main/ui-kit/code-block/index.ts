/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import './styles.scss';

// helps with some corner cases where <wbr> starts working already,
// but the signature is not yet long enough to be wrapped

const CODE_BLOCK_PADDING = 16 * 2;

const symbolsObserver = new ResizeObserver((entries) =>
  requestAnimationFrame(() => entries.forEach(wrapSymbolParameters))
);

function initHandlers() {
  document.querySelectorAll('div.symbol').forEach((symbol) => symbolsObserver.observe(symbol));
}

if (document.readyState === 'loading') {
  window.addEventListener('DOMContentLoaded', initHandlers);
} else {
  initHandlers();
}

function createNbspIndent() {
  const indent = document.createElement('span');
  indent.append(document.createTextNode('\u00A0\u00A0\u00A0\u00A0'));
  indent.classList.add('nbsp-indent');
  return indent;
}

function wrapSymbolParameters(entry: ResizeObserverEntry) {
  const symbol = entry.target;
  const symbolBlockWidth = entry.borderBoxSize && entry.borderBoxSize[0] && entry.borderBoxSize[0].inlineSize;
  const sourceButtonWidth =
    symbol.querySelector('[data-element-type="source-link"]')?.getBoundingClientRect().width || 0;

  // Even though the script is marked as `defer` and we wait for `DOMContentLoaded` event,
  // or if this block is a part of hidden tab, it can happen that `symbolBlockWidth` is 0,
  // indicating that something hasn't been loaded.
  // In this case, observer will be triggered onсe again when it will be ready.
  if (symbolBlockWidth > 0) {
    const node = symbol.querySelector('.parameters');

    if (node) {
      // if window resize happened and observer was triggered, reset previously wrapped
      // parameters as they might not need wrapping anymore, and check again
      node.classList.remove('wrapped');
      node.querySelectorAll('.parameter .nbsp-indent').forEach((indent) => indent.remove());

      const innerTextWidth = Array.from(symbol.children)
        .filter((it) => !it.classList.contains('block')) // blocks are usually on their own (like annotations), so ignore it
        .map((it) => it.getBoundingClientRect().width)
        .reduce((a, b) => a + b, 0);

      // if signature text takes up more than a single line, wrap params for readability
      if (innerTextWidth > symbolBlockWidth - CODE_BLOCK_PADDING - sourceButtonWidth) {
        node.classList.add('wrapped');
        node.querySelectorAll('.parameter').forEach((param) => {
          // has to be a physical indent so that it can be copied. styles like
          // paddings and `::before { content: "    " }` do not work for that
          param.prepend(createNbspIndent());
        });
      }
    }
  }
}
