// helps with some corner cases where <wbr> starts working already,
// but the signature is not yet long enough to be wrapped
const leftPaddingPx = 60

const wrapAllSymbolParameters = () => {
    document.querySelectorAll("div.symbol").forEach(symbol => wrapSymbolParameters(symbol))
}

const wrapSymbolParameters = (symbol) => {
    let parametersBlock = symbol.querySelector("span.parameters")
    if (parametersBlock == null) {
        return // nothing to wrap
    }

    let symbolBlockWidth = symbol.clientWidth
    let innerTextWidth = Array.from(symbol.children)
        .filter(it => !it.classList.contains("block")) // blocks are usually on their own (like annotations), so ignore it
        .map(it => it.getBoundingClientRect().width).reduce((a, b) => a + b, 0)

    // if signature text takes up more than a single line, wrap params for readability
    let shouldWrapParams = innerTextWidth > (symbolBlockWidth - leftPaddingPx)
    if (shouldWrapParams) {
        parametersBlock.classList.add("wrapped")
        parametersBlock.querySelectorAll("span.parameter").forEach(param => {
            // has to be a physical indent so that it can be copied. styles like
            // paddings and `::before { content: "    " }` do not work for that
            param.prepend(createNbspIndent())
        })
    }
}

const createNbspIndent = () => {
    let indent = document.createElement("span")
    indent.append(document.createTextNode("\u00A0\u00A0\u00A0\u00A0"))
    indent.classList.add("nbsp-indent")
    return indent
}

const resetAllSymbolParametersWrapping = () => {
    document.querySelectorAll("div.symbol").forEach(symbol => resetSymbolParametersWrapping(symbol))
}

const resetSymbolParametersWrapping = (symbol) => {
    let parameters = symbol.querySelector("span.parameters")
    if (parameters != null) {
        parameters.classList.remove("wrapped")
        parameters.querySelectorAll("span.parameter").forEach(param => {
            let indent = param.querySelector("span.nbsp-indent")
            if (indent != null) indent.remove()
        })
    }
}

if (document.readyState === 'loading') {
    window.addEventListener('DOMContentLoaded', () => {
        wrapAllSymbolParameters()
    })
} else {
    wrapAllSymbolParameters()
}

window.onresize = event => {
    // need to re-calculate if params need to be wrapped after resize
    resetAllSymbolParametersWrapping()
    wrapAllSymbolParameters()
}
