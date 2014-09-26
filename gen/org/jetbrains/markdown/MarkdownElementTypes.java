// This is a generated file. Not intended for manual editing.
package org.jetbrains.markdown;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.dokka.Markdown.MarkdownTokenType;

public interface MarkdownElementTypes {

  IElementType BLANK_LINE = new IElementType("BLANK_LINE", null);
  IElementType BULLET = new IElementType("BULLET", null);
  IElementType BULLET_LIST = new IElementType("BULLET_LIST", null);
  IElementType EMPH = new IElementType("EMPH", null);
  IElementType END_LINE = new IElementType("END_LINE", null);
  IElementType ENUMERATOR = new IElementType("ENUMERATOR", null);
  IElementType HORIZONTAL_RULE = new IElementType("HORIZONTAL_RULE", null);
  IElementType HREF = new IElementType("HREF", null);
  IElementType INDENTED_LINE = new IElementType("INDENTED_LINE", null);
  IElementType LINK = new IElementType("LINK", null);
  IElementType LIST_BLOCK = new IElementType("LIST_BLOCK", null);
  IElementType LIST_BLOCK_LINE = new IElementType("LIST_BLOCK_LINE", null);
  IElementType LIST_CONTINUATION_BLOCK = new IElementType("LIST_CONTINUATION_BLOCK", null);
  IElementType LIST_ITEM = new IElementType("LIST_ITEM", null);
  IElementType NONBLANK_INDENTED_LINE = new IElementType("NONBLANK_INDENTED_LINE", null);
  IElementType ORDERED_LIST = new IElementType("ORDERED_LIST", null);
  IElementType PARA = new IElementType("PARA", null);
  IElementType PLAIN_TEXT = new IElementType("PLAIN_TEXT", null);
  IElementType STRONG = new IElementType("STRONG", null);
  IElementType STRONG_STAR = new IElementType("STRONG_STAR", null);
  IElementType STRONG_UNDERSCORE = new IElementType("STRONG_UNDERSCORE", null);
  IElementType TARGET = new IElementType("TARGET", null);
  IElementType VERBATIM = new IElementType("VERBATIM", null);
  IElementType VERBATIM_ITEM = new IElementType("VERBATIM_ITEM", null);
  IElementType WHITESPACE = new IElementType("WHITESPACE", null);

  IElementType ANYCHAR = new MarkdownTokenType("AnyChar");
  IElementType BOM = new MarkdownTokenType("\\357\\273\\277");
  IElementType EOL = new MarkdownTokenType("\"\\r\"|\"\\n\"|\"\\r\\n\"");
  IElementType LINE_WS = new MarkdownTokenType("LINE_WS");
  IElementType NEWLINE = new MarkdownTokenType("Newline");
  IElementType NUMBER = new MarkdownTokenType("Number");
  IElementType SPACECHAR = new MarkdownTokenType("Spacechar");
  IElementType STRING = new MarkdownTokenType("String");
}
