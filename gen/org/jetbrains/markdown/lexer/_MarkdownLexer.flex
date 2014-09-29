package org.jetbrains.markdown.lexer;

import com.intellij.lexer.*;
import com.intellij.psi.tree.IElementType;
import static org.jetbrains.markdown.MarkdownElementTypes.*;

%%

%{
  public _MarkdownLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class _MarkdownLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

LineTerminator = \r|\n|\r\n
Word = {Letter}+
Number = [:digit:]+
Space     = [ \t\f]
Letter = [^~:{}$\*_`&\[\]()<!#\\ \t\n\r]
Special = [~:{}$\*_`&\[\]()<!#\\ \t\n\r]
EOL = {Space}* {LineTerminator}
EOP = {Space}* {LineTerminator} {Space}* {LineTerminator}

%%
<YYINITIAL> {
  {Space}                           { return SPACE; }
  {EOP}                             { return EOP; }
  {EOL}                             { return EOL; }
  "\\357\\273\\277"                 { return BOM; }

  {Number}                          { return NUMBER; }

  {Special}                         { return SPECIAL; }
  {Word}                          { return WORD; }

  [^] { return com.intellij.psi.TokenType.BAD_CHARACTER; }
}
