package org.jetbrains.dokka.Markdown;

import com.intellij.lexer.FlexAdapter;
import org.jetbrains.markdown.impl._MarkdownLexer;

public class MarkdownLexer extends FlexAdapter {
    public MarkdownLexer() {
        super(new _MarkdownLexer());
    }
}
