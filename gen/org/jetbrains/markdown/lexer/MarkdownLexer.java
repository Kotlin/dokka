package org.jetbrains.markdown.lexer;

import com.intellij.lexer.FlexAdapter;

public class MarkdownLexer extends FlexAdapter {
    public MarkdownLexer() {
        super(new _MarkdownLexer());
    }
}
