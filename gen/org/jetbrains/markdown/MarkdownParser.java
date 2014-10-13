// This is a generated file. Not intended for manual editing.
package org.jetbrains.markdown;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static org.jetbrains.markdown.MarkdownElementTypes.*;
import static org.jetbrains.dokka.Markdown.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class MarkdownParser implements PsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parse_only_(t, b);
    return b.getTreeBuilt();
  }

  public void parse_only_(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, null);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    if (t == ANONYMOUS_SECTION) {
      r = AnonymousSection(b, 0);
    }
    else if (t == BLANK_LINE) {
      r = BlankLine(b, 0);
    }
    else if (t == BLOCK) {
      r = Block(b, 0);
    }
    else if (t == BULLET) {
      r = Bullet(b, 0);
    }
    else if (t == BULLET_LIST) {
      r = BulletList(b, 0);
    }
    else if (t == CODE) {
      r = Code(b, 0);
    }
    else if (t == DIRECTIVE) {
      r = Directive(b, 0);
    }
    else if (t == DIRECTIVE_NAME) {
      r = DirectiveName(b, 0);
    }
    else if (t == DIRECTIVE_PARAMS) {
      r = DirectiveParams(b, 0);
    }
    else if (t == EMPH) {
      r = Emph(b, 0);
    }
    else if (t == END_LINE) {
      r = EndLine(b, 0);
    }
    else if (t == ENUMERATOR) {
      r = Enumerator(b, 0);
    }
    else if (t == HORIZONTAL_RULE) {
      r = HorizontalRule(b, 0);
    }
    else if (t == HREF) {
      r = Href(b, 0);
    }
    else if (t == INLINE) {
      r = Inline(b, 0);
    }
    else if (t == LINK) {
      r = Link(b, 0);
    }
    else if (t == LIST_BLOCK) {
      r = ListBlock(b, 0);
    }
    else if (t == LIST_BLOCK_LINE) {
      r = ListBlockLine(b, 0);
    }
    else if (t == LIST_CONTINUATION_BLOCK) {
      r = ListContinuationBlock(b, 0);
    }
    else if (t == LIST_ITEM) {
      r = ListItem(b, 0);
    }
    else if (t == NAMED_SECTION) {
      r = NamedSection(b, 0);
    }
    else if (t == ORDERED_LIST) {
      r = OrderedList(b, 0);
    }
    else if (t == PARA) {
      r = Para(b, 0);
    }
    else if (t == PLAIN_TEXT) {
      r = PlainText(b, 0);
    }
    else if (t == SECTION_BODY) {
      r = SectionBody(b, 0);
    }
    else if (t == SECTION_NAME) {
      r = SectionName(b, 0);
    }
    else if (t == STRONG) {
      r = Strong(b, 0);
    }
    else if (t == STRONG_STAR) {
      r = StrongStar(b, 0);
    }
    else if (t == STRONG_UNDERSCORE) {
      r = StrongUnderscore(b, 0);
    }
    else if (t == TARGET) {
      r = Target(b, 0);
    }
    else if (t == UNUSED) {
      r = Unused(b, 0);
    }
    else if (t == WHITESPACE) {
      r = Whitespace(b, 0);
    }
    else {
      r = parse_root_(t, b, 0);
    }
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return Document(b, l + 1);
  }

  /* ********************************************************** */
  // SectionBody
  public static boolean AnonymousSection(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "AnonymousSection")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<anonymous section>");
    r = SectionBody(b, l + 1);
    exit_section_(b, l, m, ANONYMOUS_SECTION, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // OptionalSpace EOL
  public static boolean BlankLine(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "BlankLine")) return false;
    if (!nextTokenIs(b, "<blank line>", EOL, SPACE)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<blank line>");
    r = OptionalSpace(b, l + 1);
    r = r && consumeToken(b, EOL);
    exit_section_(b, l, m, BLANK_LINE, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // BlankLine* (
  //               OrderedList
  //             | BulletList
  //             | HorizontalRule
  //             | Directive
  //             | Para
  //             )
  public static boolean Block(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Block")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<block>");
    r = Block_0(b, l + 1);
    r = r && Block_1(b, l + 1);
    exit_section_(b, l, m, BLOCK, r, false, null);
    return r;
  }

  // BlankLine*
  private static boolean Block_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Block_0")) return false;
    int c = current_position_(b);
    while (true) {
      if (!BlankLine(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "Block_0", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // OrderedList
  //             | BulletList
  //             | HorizontalRule
  //             | Directive
  //             | Para
  private static boolean Block_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Block_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = OrderedList(b, l + 1);
    if (!r) r = BulletList(b, l + 1);
    if (!r) r = HorizontalRule(b, l + 1);
    if (!r) r = Directive(b, l + 1);
    if (!r) r = Para(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // !HorizontalRule NonindentSpace ('+' | '*' | '-') Space+
  public static boolean Bullet(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Bullet")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<bullet>");
    r = Bullet_0(b, l + 1);
    r = r && NonindentSpace(b, l + 1);
    r = r && Bullet_2(b, l + 1);
    r = r && Bullet_3(b, l + 1);
    exit_section_(b, l, m, BULLET, r, false, null);
    return r;
  }

  // !HorizontalRule
  private static boolean Bullet_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Bullet_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !HorizontalRule(b, l + 1);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  // '+' | '*' | '-'
  private static boolean Bullet_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Bullet_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, "+");
    if (!r) r = consumeToken(b, "*");
    if (!r) r = consumeToken(b, "-");
    exit_section_(b, m, null, r);
    return r;
  }

  // Space+
  private static boolean Bullet_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Bullet_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, SPACE);
    int c = current_position_(b);
    while (r) {
      if (!consumeToken(b, SPACE)) break;
      if (!empty_element_parsed_guard_(b, "Bullet_3", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // &Bullet List
  public static boolean BulletList(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "BulletList")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<bullet list>");
    r = BulletList_0(b, l + 1);
    r = r && List(b, l + 1);
    exit_section_(b, l, m, BULLET_LIST, r, false, null);
    return r;
  }

  // &Bullet
  private static boolean BulletList_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "BulletList_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_, null);
    r = Bullet(b, l + 1);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // '`' !Whitespace (!'`' Inline)+ '`'
  public static boolean Code(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Code")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<code>");
    r = consumeToken(b, "`");
    r = r && Code_1(b, l + 1);
    r = r && Code_2(b, l + 1);
    r = r && consumeToken(b, "`");
    exit_section_(b, l, m, CODE, r, false, null);
    return r;
  }

  // !Whitespace
  private static boolean Code_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Code_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !Whitespace(b, l + 1);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  // (!'`' Inline)+
  private static boolean Code_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Code_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Code_2_0(b, l + 1);
    int c = current_position_(b);
    while (r) {
      if (!Code_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "Code_2", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // !'`' Inline
  private static boolean Code_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Code_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Code_2_0_0(b, l + 1);
    r = r && Inline(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // !'`'
  private static boolean Code_2_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Code_2_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !consumeToken(b, "`");
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // '{' DirectiveName DirectiveParams '}'
  public static boolean Directive(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Directive")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<directive>");
    r = consumeToken(b, "{");
    r = r && DirectiveName(b, l + 1);
    r = r && DirectiveParams(b, l + 1);
    r = r && consumeToken(b, "}");
    exit_section_(b, l, m, DIRECTIVE, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // Word
  public static boolean DirectiveName(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "DirectiveName")) return false;
    if (!nextTokenIs(b, WORD)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, WORD);
    exit_section_(b, m, DIRECTIVE_NAME, r);
    return r;
  }

  /* ********************************************************** */
  // PlainText
  public static boolean DirectiveParams(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "DirectiveParams")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<directive params>");
    r = PlainText(b, l + 1);
    exit_section_(b, l, m, DIRECTIVE_PARAMS, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // BOM? Whitespace* AnonymousSection? (Whitespace* NamedSection)*
  static boolean Document(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Document")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Document_0(b, l + 1);
    r = r && Document_1(b, l + 1);
    r = r && Document_2(b, l + 1);
    r = r && Document_3(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // BOM?
  private static boolean Document_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Document_0")) return false;
    consumeToken(b, BOM);
    return true;
  }

  // Whitespace*
  private static boolean Document_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Document_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!Whitespace(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "Document_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // AnonymousSection?
  private static boolean Document_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Document_2")) return false;
    AnonymousSection(b, l + 1);
    return true;
  }

  // (Whitespace* NamedSection)*
  private static boolean Document_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Document_3")) return false;
    int c = current_position_(b);
    while (true) {
      if (!Document_3_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "Document_3", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // Whitespace* NamedSection
  private static boolean Document_3_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Document_3_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Document_3_0_0(b, l + 1);
    r = r && NamedSection(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // Whitespace*
  private static boolean Document_3_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Document_3_0_0")) return false;
    int c = current_position_(b);
    while (true) {
      if (!Whitespace(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "Document_3_0_0", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  /* ********************************************************** */
  // EmphStar | EmphUnderscore
  public static boolean Emph(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Emph")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<emph>");
    r = EmphStar(b, l + 1);
    if (!r) r = EmphUnderscore(b, l + 1);
    exit_section_(b, l, m, EMPH, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // '*' !Whitespace (!'*' Inline)+ '*'
  static boolean EmphStar(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "EmphStar")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, "*");
    r = r && EmphStar_1(b, l + 1);
    r = r && EmphStar_2(b, l + 1);
    r = r && consumeToken(b, "*");
    exit_section_(b, m, null, r);
    return r;
  }

  // !Whitespace
  private static boolean EmphStar_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "EmphStar_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !Whitespace(b, l + 1);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  // (!'*' Inline)+
  private static boolean EmphStar_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "EmphStar_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = EmphStar_2_0(b, l + 1);
    int c = current_position_(b);
    while (r) {
      if (!EmphStar_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "EmphStar_2", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // !'*' Inline
  private static boolean EmphStar_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "EmphStar_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = EmphStar_2_0_0(b, l + 1);
    r = r && Inline(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // !'*'
  private static boolean EmphStar_2_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "EmphStar_2_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !consumeToken(b, "*");
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // '_' !Whitespace (!'_' Inline)+ '_'
  static boolean EmphUnderscore(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "EmphUnderscore")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, "_");
    r = r && EmphUnderscore_1(b, l + 1);
    r = r && EmphUnderscore_2(b, l + 1);
    r = r && consumeToken(b, "_");
    exit_section_(b, m, null, r);
    return r;
  }

  // !Whitespace
  private static boolean EmphUnderscore_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "EmphUnderscore_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !Whitespace(b, l + 1);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  // (!'_' Inline)+
  private static boolean EmphUnderscore_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "EmphUnderscore_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = EmphUnderscore_2_0(b, l + 1);
    int c = current_position_(b);
    while (r) {
      if (!EmphUnderscore_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "EmphUnderscore_2", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // !'_' Inline
  private static boolean EmphUnderscore_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "EmphUnderscore_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = EmphUnderscore_2_0_0(b, l + 1);
    r = r && Inline(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // !'_'
  private static boolean EmphUnderscore_2_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "EmphUnderscore_2_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !consumeToken(b, "_");
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // TerminalEndline | NormalEndline
  public static boolean EndLine(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "EndLine")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<end line>");
    r = TerminalEndline(b, l + 1);
    if (!r) r = NormalEndline(b, l + 1);
    exit_section_(b, l, m, END_LINE, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // NonindentSpace Number '.' Space+
  public static boolean Enumerator(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Enumerator")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<enumerator>");
    r = NonindentSpace(b, l + 1);
    r = r && consumeToken(b, NUMBER);
    r = r && consumeToken(b, ".");
    r = r && Enumerator_3(b, l + 1);
    exit_section_(b, l, m, ENUMERATOR, r, false, null);
    return r;
  }

  // Space+
  private static boolean Enumerator_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Enumerator_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, SPACE);
    int c = current_position_(b);
    while (r) {
      if (!consumeToken(b, SPACE)) break;
      if (!empty_element_parsed_guard_(b, "Enumerator_3", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // NonindentSpace
  //                  ( '*' OptionalSpace '*' OptionalSpace '*' (OptionalSpace '*')*
  //                  | '-' OptionalSpace '-' OptionalSpace '-' (OptionalSpace '-')*
  //                  | '_' OptionalSpace '_' OptionalSpace '_' (OptionalSpace '_')*)
  //                  OptionalSpace EOL BlankLine+
  public static boolean HorizontalRule(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "HorizontalRule")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<horizontal rule>");
    r = NonindentSpace(b, l + 1);
    r = r && HorizontalRule_1(b, l + 1);
    r = r && OptionalSpace(b, l + 1);
    r = r && consumeToken(b, EOL);
    r = r && HorizontalRule_4(b, l + 1);
    exit_section_(b, l, m, HORIZONTAL_RULE, r, false, null);
    return r;
  }

  // '*' OptionalSpace '*' OptionalSpace '*' (OptionalSpace '*')*
  //                  | '-' OptionalSpace '-' OptionalSpace '-' (OptionalSpace '-')*
  //                  | '_' OptionalSpace '_' OptionalSpace '_' (OptionalSpace '_')*
  private static boolean HorizontalRule_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "HorizontalRule_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = HorizontalRule_1_0(b, l + 1);
    if (!r) r = HorizontalRule_1_1(b, l + 1);
    if (!r) r = HorizontalRule_1_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // '*' OptionalSpace '*' OptionalSpace '*' (OptionalSpace '*')*
  private static boolean HorizontalRule_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "HorizontalRule_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, "*");
    r = r && OptionalSpace(b, l + 1);
    r = r && consumeToken(b, "*");
    r = r && OptionalSpace(b, l + 1);
    r = r && consumeToken(b, "*");
    r = r && HorizontalRule_1_0_5(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (OptionalSpace '*')*
  private static boolean HorizontalRule_1_0_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "HorizontalRule_1_0_5")) return false;
    int c = current_position_(b);
    while (true) {
      if (!HorizontalRule_1_0_5_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "HorizontalRule_1_0_5", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // OptionalSpace '*'
  private static boolean HorizontalRule_1_0_5_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "HorizontalRule_1_0_5_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = OptionalSpace(b, l + 1);
    r = r && consumeToken(b, "*");
    exit_section_(b, m, null, r);
    return r;
  }

  // '-' OptionalSpace '-' OptionalSpace '-' (OptionalSpace '-')*
  private static boolean HorizontalRule_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "HorizontalRule_1_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, "-");
    r = r && OptionalSpace(b, l + 1);
    r = r && consumeToken(b, "-");
    r = r && OptionalSpace(b, l + 1);
    r = r && consumeToken(b, "-");
    r = r && HorizontalRule_1_1_5(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (OptionalSpace '-')*
  private static boolean HorizontalRule_1_1_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "HorizontalRule_1_1_5")) return false;
    int c = current_position_(b);
    while (true) {
      if (!HorizontalRule_1_1_5_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "HorizontalRule_1_1_5", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // OptionalSpace '-'
  private static boolean HorizontalRule_1_1_5_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "HorizontalRule_1_1_5_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = OptionalSpace(b, l + 1);
    r = r && consumeToken(b, "-");
    exit_section_(b, m, null, r);
    return r;
  }

  // '_' OptionalSpace '_' OptionalSpace '_' (OptionalSpace '_')*
  private static boolean HorizontalRule_1_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "HorizontalRule_1_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, "_");
    r = r && OptionalSpace(b, l + 1);
    r = r && consumeToken(b, "_");
    r = r && OptionalSpace(b, l + 1);
    r = r && consumeToken(b, "_");
    r = r && HorizontalRule_1_2_5(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (OptionalSpace '_')*
  private static boolean HorizontalRule_1_2_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "HorizontalRule_1_2_5")) return false;
    int c = current_position_(b);
    while (true) {
      if (!HorizontalRule_1_2_5_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "HorizontalRule_1_2_5", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // OptionalSpace '_'
  private static boolean HorizontalRule_1_2_5_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "HorizontalRule_1_2_5_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = OptionalSpace(b, l + 1);
    r = r && consumeToken(b, "_");
    exit_section_(b, m, null, r);
    return r;
  }

  // BlankLine+
  private static boolean HorizontalRule_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "HorizontalRule_4")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = BlankLine(b, l + 1);
    int c = current_position_(b);
    while (r) {
      if (!BlankLine(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "HorizontalRule_4", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // (Word | Number | ':' | '/')+
  public static boolean Href(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Href")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<href>");
    r = Href_0(b, l + 1);
    int c = current_position_(b);
    while (r) {
      if (!Href_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "Href", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, l, m, HREF, r, false, null);
    return r;
  }

  // Word | Number | ':' | '/'
  private static boolean Href_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Href_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, WORD);
    if (!r) r = consumeToken(b, NUMBER);
    if (!r) r = consumeToken(b, ":");
    if (!r) r = consumeToken(b, "/");
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // "\t" | "    "
  static boolean Indent(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Indent")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, "\t");
    if (!r) r = consumeToken(b, "    ");
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // Strong | Emph | Code | Link | PlainText
  public static boolean Inline(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Inline")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<inline>");
    r = Strong(b, l + 1);
    if (!r) r = Emph(b, l + 1);
    if (!r) r = Code(b, l + 1);
    if (!r) r = Link(b, l + 1);
    if (!r) r = PlainText(b, l + 1);
    exit_section_(b, l, m, INLINE, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // (!EndLine Inline | EndLine &Inline )+ EndLine?
  static boolean Inlines(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Inlines")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Inlines_0(b, l + 1);
    r = r && Inlines_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (!EndLine Inline | EndLine &Inline )+
  private static boolean Inlines_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Inlines_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Inlines_0_0(b, l + 1);
    int c = current_position_(b);
    while (r) {
      if (!Inlines_0_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "Inlines_0", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // !EndLine Inline | EndLine &Inline
  private static boolean Inlines_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Inlines_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Inlines_0_0_0(b, l + 1);
    if (!r) r = Inlines_0_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // !EndLine Inline
  private static boolean Inlines_0_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Inlines_0_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Inlines_0_0_0_0(b, l + 1);
    r = r && Inline(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // !EndLine
  private static boolean Inlines_0_0_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Inlines_0_0_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !EndLine(b, l + 1);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  // EndLine &Inline
  private static boolean Inlines_0_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Inlines_0_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = EndLine(b, l + 1);
    r = r && Inlines_0_0_1_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // &Inline
  private static boolean Inlines_0_0_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Inlines_0_0_1_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_, null);
    r = Inline(b, l + 1);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  // EndLine?
  private static boolean Inlines_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Inlines_1")) return false;
    EndLine(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // '[' Target ']' ('(' Href ')')?
  public static boolean Link(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Link")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<link>");
    r = consumeToken(b, "[");
    r = r && Target(b, l + 1);
    r = r && consumeToken(b, "]");
    r = r && Link_3(b, l + 1);
    exit_section_(b, l, m, LINK, r, false, null);
    return r;
  }

  // ('(' Href ')')?
  private static boolean Link_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Link_3")) return false;
    Link_3_0(b, l + 1);
    return true;
  }

  // '(' Href ')'
  private static boolean Link_3_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Link_3_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, "(");
    r = r && Href(b, l + 1);
    r = r && consumeToken(b, ")");
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // (ListItem BlankLine*)+
  static boolean List(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "List")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = List_0(b, l + 1);
    int c = current_position_(b);
    while (r) {
      if (!List_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "List", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // ListItem BlankLine*
  private static boolean List_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "List_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = ListItem(b, l + 1);
    r = r && List_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // BlankLine*
  private static boolean List_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "List_0_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!BlankLine(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "List_0_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  /* ********************************************************** */
  // !BlankLine Inlines ( ListBlockLine )*
  public static boolean ListBlock(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListBlock")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<list block>");
    r = ListBlock_0(b, l + 1);
    r = r && Inlines(b, l + 1);
    r = r && ListBlock_2(b, l + 1);
    exit_section_(b, l, m, LIST_BLOCK, r, false, null);
    return r;
  }

  // !BlankLine
  private static boolean ListBlock_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListBlock_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !BlankLine(b, l + 1);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  // ( ListBlockLine )*
  private static boolean ListBlock_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListBlock_2")) return false;
    int c = current_position_(b);
    while (true) {
      if (!ListBlock_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "ListBlock_2", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // ( ListBlockLine )
  private static boolean ListBlock_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListBlock_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = ListBlockLine(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // !BlankLine !(Indent? (Bullet | Enumerator)) !HorizontalRule Indent? Inlines
  public static boolean ListBlockLine(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListBlockLine")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<list block line>");
    r = ListBlockLine_0(b, l + 1);
    r = r && ListBlockLine_1(b, l + 1);
    r = r && ListBlockLine_2(b, l + 1);
    r = r && ListBlockLine_3(b, l + 1);
    r = r && Inlines(b, l + 1);
    exit_section_(b, l, m, LIST_BLOCK_LINE, r, false, null);
    return r;
  }

  // !BlankLine
  private static boolean ListBlockLine_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListBlockLine_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !BlankLine(b, l + 1);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  // !(Indent? (Bullet | Enumerator))
  private static boolean ListBlockLine_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListBlockLine_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !ListBlockLine_1_0(b, l + 1);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  // Indent? (Bullet | Enumerator)
  private static boolean ListBlockLine_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListBlockLine_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = ListBlockLine_1_0_0(b, l + 1);
    r = r && ListBlockLine_1_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // Indent?
  private static boolean ListBlockLine_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListBlockLine_1_0_0")) return false;
    Indent(b, l + 1);
    return true;
  }

  // Bullet | Enumerator
  private static boolean ListBlockLine_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListBlockLine_1_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Bullet(b, l + 1);
    if (!r) r = Enumerator(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // !HorizontalRule
  private static boolean ListBlockLine_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListBlockLine_2")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !HorizontalRule(b, l + 1);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  // Indent?
  private static boolean ListBlockLine_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListBlockLine_3")) return false;
    Indent(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // BlankLine* (Indent ListBlock)+
  public static boolean ListContinuationBlock(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListContinuationBlock")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<list continuation block>");
    r = ListContinuationBlock_0(b, l + 1);
    r = r && ListContinuationBlock_1(b, l + 1);
    exit_section_(b, l, m, LIST_CONTINUATION_BLOCK, r, false, null);
    return r;
  }

  // BlankLine*
  private static boolean ListContinuationBlock_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListContinuationBlock_0")) return false;
    int c = current_position_(b);
    while (true) {
      if (!BlankLine(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "ListContinuationBlock_0", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // (Indent ListBlock)+
  private static boolean ListContinuationBlock_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListContinuationBlock_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = ListContinuationBlock_1_0(b, l + 1);
    int c = current_position_(b);
    while (r) {
      if (!ListContinuationBlock_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "ListContinuationBlock_1", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // Indent ListBlock
  private static boolean ListContinuationBlock_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListContinuationBlock_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Indent(b, l + 1);
    r = r && ListBlock(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // (Bullet | Enumerator) ListBlock ( ListContinuationBlock )*
  public static boolean ListItem(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListItem")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<list item>");
    r = ListItem_0(b, l + 1);
    r = r && ListBlock(b, l + 1);
    r = r && ListItem_2(b, l + 1);
    exit_section_(b, l, m, LIST_ITEM, r, false, null);
    return r;
  }

  // Bullet | Enumerator
  private static boolean ListItem_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListItem_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Bullet(b, l + 1);
    if (!r) r = Enumerator(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ( ListContinuationBlock )*
  private static boolean ListItem_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListItem_2")) return false;
    int c = current_position_(b);
    while (true) {
      if (!ListItem_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "ListItem_2", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // ( ListContinuationBlock )
  private static boolean ListItem_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListItem_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = ListContinuationBlock(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // SectionHeader SectionBody
  public static boolean NamedSection(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "NamedSection")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<named section>");
    r = SectionHeader(b, l + 1);
    r = r && SectionBody(b, l + 1);
    exit_section_(b, l, m, NAMED_SECTION, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // ("   " | "  " | " ")?
  static boolean NonindentSpace(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "NonindentSpace")) return false;
    NonindentSpace_0(b, l + 1);
    return true;
  }

  // "   " | "  " | " "
  private static boolean NonindentSpace_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "NonindentSpace_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, "   ");
    if (!r) r = consumeToken(b, "  ");
    if (!r) r = consumeToken(b, " ");
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // EOL !BlankLine
  static boolean NormalEndline(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "NormalEndline")) return false;
    if (!nextTokenIs(b, EOL)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, EOL);
    r = r && NormalEndline_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // !BlankLine
  private static boolean NormalEndline_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "NormalEndline_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !BlankLine(b, l + 1);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // Space*
  static boolean OptionalSpace(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "OptionalSpace")) return false;
    int c = current_position_(b);
    while (true) {
      if (!consumeToken(b, SPACE)) break;
      if (!empty_element_parsed_guard_(b, "OptionalSpace", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  /* ********************************************************** */
  // &Enumerator List
  public static boolean OrderedList(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "OrderedList")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<ordered list>");
    r = OrderedList_0(b, l + 1);
    r = r && List(b, l + 1);
    exit_section_(b, l, m, ORDERED_LIST, r, false, null);
    return r;
  }

  // &Enumerator
  private static boolean OrderedList_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "OrderedList_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_, null);
    r = Enumerator(b, l + 1);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // Inlines (EOP | Space* <<eof>>)?
  public static boolean Para(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Para")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<para>");
    r = Inlines(b, l + 1);
    r = r && Para_1(b, l + 1);
    exit_section_(b, l, m, PARA, r, false, null);
    return r;
  }

  // (EOP | Space* <<eof>>)?
  private static boolean Para_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Para_1")) return false;
    Para_1_0(b, l + 1);
    return true;
  }

  // EOP | Space* <<eof>>
  private static boolean Para_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Para_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, EOP);
    if (!r) r = Para_1_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // Space* <<eof>>
  private static boolean Para_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Para_1_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Para_1_0_1_0(b, l + 1);
    r = r && eof(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // Space*
  private static boolean Para_1_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Para_1_0_1_0")) return false;
    int c = current_position_(b);
    while (true) {
      if (!consumeToken(b, SPACE)) break;
      if (!empty_element_parsed_guard_(b, "Para_1_0_1_0", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  /* ********************************************************** */
  // (Word | Number | Space | ':')+
  public static boolean PlainText(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PlainText")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<plain text>");
    r = PlainText_0(b, l + 1);
    int c = current_position_(b);
    while (r) {
      if (!PlainText_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "PlainText", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, l, m, PLAIN_TEXT, r, false, null);
    return r;
  }

  // Word | Number | Space | ':'
  private static boolean PlainText_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PlainText_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, WORD);
    if (!r) r = consumeToken(b, NUMBER);
    if (!r) r = consumeToken(b, SPACE);
    if (!r) r = consumeToken(b, ":");
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // Block*
  public static boolean SectionBody(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SectionBody")) return false;
    Marker m = enter_section_(b, l, _NONE_, "<section body>");
    int c = current_position_(b);
    while (true) {
      if (!Block(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "SectionBody", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, l, m, SECTION_BODY, true, false, null);
    return true;
  }

  /* ********************************************************** */
  // '$' SectionName OptionalSpace ':' OptionalSpace
  static boolean SectionHeader(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SectionHeader")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, "$");
    r = r && SectionName(b, l + 1);
    r = r && OptionalSpace(b, l + 1);
    r = r && consumeToken(b, ":");
    r = r && OptionalSpace(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // SectionNameStart | '{' OptionalSpace SectionNameStart (Space+ Word)* OptionalSpace '}'
  public static boolean SectionName(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SectionName")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<section name>");
    r = SectionNameStart(b, l + 1);
    if (!r) r = SectionName_1(b, l + 1);
    exit_section_(b, l, m, SECTION_NAME, r, false, null);
    return r;
  }

  // '{' OptionalSpace SectionNameStart (Space+ Word)* OptionalSpace '}'
  private static boolean SectionName_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SectionName_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, "{");
    r = r && OptionalSpace(b, l + 1);
    r = r && SectionNameStart(b, l + 1);
    r = r && SectionName_1_3(b, l + 1);
    r = r && OptionalSpace(b, l + 1);
    r = r && consumeToken(b, "}");
    exit_section_(b, m, null, r);
    return r;
  }

  // (Space+ Word)*
  private static boolean SectionName_1_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SectionName_1_3")) return false;
    int c = current_position_(b);
    while (true) {
      if (!SectionName_1_3_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "SectionName_1_3", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // Space+ Word
  private static boolean SectionName_1_3_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SectionName_1_3_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = SectionName_1_3_0_0(b, l + 1);
    r = r && consumeToken(b, WORD);
    exit_section_(b, m, null, r);
    return r;
  }

  // Space+
  private static boolean SectionName_1_3_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SectionName_1_3_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, SPACE);
    int c = current_position_(b);
    while (r) {
      if (!consumeToken(b, SPACE)) break;
      if (!empty_element_parsed_guard_(b, "SectionName_1_3_0_0", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '$'? Word
  static boolean SectionNameStart(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SectionNameStart")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = SectionNameStart_0(b, l + 1);
    r = r && consumeToken(b, WORD);
    exit_section_(b, m, null, r);
    return r;
  }

  // '$'?
  private static boolean SectionNameStart_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SectionNameStart_0")) return false;
    consumeToken(b, "$");
    return true;
  }

  /* ********************************************************** */
  // StrongStar | StrongUnderscore
  public static boolean Strong(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Strong")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<strong>");
    r = StrongStar(b, l + 1);
    if (!r) r = StrongUnderscore(b, l + 1);
    exit_section_(b, l, m, STRONG, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // '**' !Whitespace (!'**' Inline)+ '**'
  public static boolean StrongStar(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "StrongStar")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<strong star>");
    r = consumeToken(b, "**");
    r = r && StrongStar_1(b, l + 1);
    r = r && StrongStar_2(b, l + 1);
    r = r && consumeToken(b, "**");
    exit_section_(b, l, m, STRONG_STAR, r, false, null);
    return r;
  }

  // !Whitespace
  private static boolean StrongStar_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "StrongStar_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !Whitespace(b, l + 1);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  // (!'**' Inline)+
  private static boolean StrongStar_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "StrongStar_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = StrongStar_2_0(b, l + 1);
    int c = current_position_(b);
    while (r) {
      if (!StrongStar_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "StrongStar_2", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // !'**' Inline
  private static boolean StrongStar_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "StrongStar_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = StrongStar_2_0_0(b, l + 1);
    r = r && Inline(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // !'**'
  private static boolean StrongStar_2_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "StrongStar_2_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !consumeToken(b, "**");
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // '__' !Whitespace (!'__' Inline)+ '__'
  public static boolean StrongUnderscore(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "StrongUnderscore")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<strong underscore>");
    r = consumeToken(b, "__");
    r = r && StrongUnderscore_1(b, l + 1);
    r = r && StrongUnderscore_2(b, l + 1);
    r = r && consumeToken(b, "__");
    exit_section_(b, l, m, STRONG_UNDERSCORE, r, false, null);
    return r;
  }

  // !Whitespace
  private static boolean StrongUnderscore_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "StrongUnderscore_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !Whitespace(b, l + 1);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  // (!'__' Inline)+
  private static boolean StrongUnderscore_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "StrongUnderscore_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = StrongUnderscore_2_0(b, l + 1);
    int c = current_position_(b);
    while (r) {
      if (!StrongUnderscore_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "StrongUnderscore_2", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // !'__' Inline
  private static boolean StrongUnderscore_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "StrongUnderscore_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = StrongUnderscore_2_0_0(b, l + 1);
    r = r && Inline(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // !'__'
  private static boolean StrongUnderscore_2_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "StrongUnderscore_2_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !consumeToken(b, "__");
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // Word+
  public static boolean Target(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Target")) return false;
    if (!nextTokenIs(b, WORD)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, WORD);
    int c = current_position_(b);
    while (r) {
      if (!consumeToken(b, WORD)) break;
      if (!empty_element_parsed_guard_(b, "Target", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, m, TARGET, r);
    return r;
  }

  /* ********************************************************** */
  // OptionalSpace <<eof>>
  static boolean TerminalEndline(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TerminalEndline")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = OptionalSpace(b, l + 1);
    r = r && eof(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // Special
  public static boolean Unused(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Unused")) return false;
    if (!nextTokenIs(b, SPECIAL)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, SPECIAL);
    exit_section_(b, m, UNUSED, r);
    return r;
  }

  /* ********************************************************** */
  // Space | EOL | EOP
  public static boolean Whitespace(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Whitespace")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<whitespace>");
    r = consumeToken(b, SPACE);
    if (!r) r = consumeToken(b, EOL);
    if (!r) r = consumeToken(b, EOP);
    exit_section_(b, l, m, WHITESPACE, r, false, null);
    return r;
  }

}
