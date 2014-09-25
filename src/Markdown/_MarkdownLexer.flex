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

Newline="\r"|"\n"|"\r\n"
Spacechar=[\ \t\f]
Number=[0-9]+(\.[0-9]*)?
String=[^~\*_`&\[\]()<!#\\ \t\n\r]+
AnyChar=.
Line=!'\r' !'\n' .* {Newline}

%%
<YYINITIAL> {
  {Spacechar}                         { return SPACECHAR; }
  {Newline}                             { return NEWLINE; }
  "\\357\\273\\277"                 { return BOM; }

  {Number}                          { return NUMBER; }
  {String}                          { return STRING; }
  {AnyChar}                         { return ANYCHAR; }

  [^] { return com.intellij.psi.TokenType.BAD_CHARACTER; }
}
