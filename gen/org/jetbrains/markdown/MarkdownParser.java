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

  public ASTNode parse(IElementType root_, PsiBuilder builder_) {
    parse_only_(root_, builder_);
    return builder_.getTreeBuilt();
  }

  public void parse_only_(IElementType root_, PsiBuilder builder_) {
    boolean result_;
    builder_ = adapt_builder_(root_, builder_, this, null);
    Marker marker_ = enter_section_(builder_, 0, _COLLAPSE_, null);
    if (root_ == BLANK_LINE) {
      result_ = BlankLine(builder_, 0);
    }
    else if (root_ == BLOCK) {
      result_ = Block(builder_, 0);
    }
    else if (root_ == BULLET) {
      result_ = Bullet(builder_, 0);
    }
    else if (root_ == BULLET_LIST) {
      result_ = BulletList(builder_, 0);
    }
    else if (root_ == EMPH) {
      result_ = Emph(builder_, 0);
    }
    else if (root_ == END_LINE) {
      result_ = EndLine(builder_, 0);
    }
    else if (root_ == ENUMERATOR) {
      result_ = Enumerator(builder_, 0);
    }
    else if (root_ == HORIZONTAL_RULE) {
      result_ = HorizontalRule(builder_, 0);
    }
    else if (root_ == HREF) {
      result_ = Href(builder_, 0);
    }
    else if (root_ == INLINE) {
      result_ = Inline(builder_, 0);
    }
    else if (root_ == LINK) {
      result_ = Link(builder_, 0);
    }
    else if (root_ == LIST_BLOCK) {
      result_ = ListBlock(builder_, 0);
    }
    else if (root_ == LIST_BLOCK_LINE) {
      result_ = ListBlockLine(builder_, 0);
    }
    else if (root_ == LIST_CONTINUATION_BLOCK) {
      result_ = ListContinuationBlock(builder_, 0);
    }
    else if (root_ == LIST_ITEM) {
      result_ = ListItem(builder_, 0);
    }
    else if (root_ == ORDERED_LIST) {
      result_ = OrderedList(builder_, 0);
    }
    else if (root_ == PARA) {
      result_ = Para(builder_, 0);
    }
    else if (root_ == STRONG) {
      result_ = Strong(builder_, 0);
    }
    else if (root_ == STRONG_STAR) {
      result_ = StrongStar(builder_, 0);
    }
    else if (root_ == STRONG_UNDERSCORE) {
      result_ = StrongUnderscore(builder_, 0);
    }
    else if (root_ == TARGET) {
      result_ = Target(builder_, 0);
    }
    else if (root_ == WHITESPACE) {
      result_ = Whitespace(builder_, 0);
    }
    else {
      result_ = parse_root_(root_, builder_, 0);
    }
    exit_section_(builder_, 0, marker_, root_, result_, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType root_, PsiBuilder builder_, int level_) {
    return Document(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // OptionalSpace Newline
  public static boolean BlankLine(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "BlankLine")) return false;
    if (!nextTokenIs(builder_, "<blank line>", NEWLINE, SPACECHAR)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<blank line>");
    result_ = OptionalSpace(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, NEWLINE);
    exit_section_(builder_, level_, marker_, BLANK_LINE, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // BlankLine* (
  //         Para
  //         | Plain
  //         | OrderedList
  //         | BulletList
  //         )
  public static boolean Block(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Block")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<block>");
    result_ = Block_0(builder_, level_ + 1);
    result_ = result_ && Block_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, BLOCK, result_, false, null);
    return result_;
  }

  // BlankLine*
  private static boolean Block_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Block_0")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!BlankLine(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "Block_0", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // Para
  //         | Plain
  //         | OrderedList
  //         | BulletList
  private static boolean Block_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Block_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = Para(builder_, level_ + 1);
    if (!result_) result_ = Plain(builder_, level_ + 1);
    if (!result_) result_ = OrderedList(builder_, level_ + 1);
    if (!result_) result_ = BulletList(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // !HorizontalRule NonindentSpace ('+' | '*' | '-') Spacechar+
  public static boolean Bullet(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Bullet")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<bullet>");
    result_ = Bullet_0(builder_, level_ + 1);
    result_ = result_ && NonindentSpace(builder_, level_ + 1);
    result_ = result_ && Bullet_2(builder_, level_ + 1);
    result_ = result_ && Bullet_3(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, BULLET, result_, false, null);
    return result_;
  }

  // !HorizontalRule
  private static boolean Bullet_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Bullet_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !HorizontalRule(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  // '+' | '*' | '-'
  private static boolean Bullet_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Bullet_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "+");
    if (!result_) result_ = consumeToken(builder_, "*");
    if (!result_) result_ = consumeToken(builder_, "-");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // Spacechar+
  private static boolean Bullet_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Bullet_3")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, SPACECHAR);
    int pos_ = current_position_(builder_);
    while (result_) {
      if (!consumeToken(builder_, SPACECHAR)) break;
      if (!empty_element_parsed_guard_(builder_, "Bullet_3", pos_)) break;
      pos_ = current_position_(builder_);
    }
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // &Bullet List
  public static boolean BulletList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "BulletList")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<bullet list>");
    result_ = BulletList_0(builder_, level_ + 1);
    result_ = result_ && List(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, BULLET_LIST, result_, false, null);
    return result_;
  }

  // &Bullet
  private static boolean BulletList_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "BulletList_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _AND_, null);
    result_ = Bullet(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // BOM? ( Block )*
  static boolean Document(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Document")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = Document_0(builder_, level_ + 1);
    result_ = result_ && Document_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // BOM?
  private static boolean Document_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Document_0")) return false;
    consumeToken(builder_, BOM);
    return true;
  }

  // ( Block )*
  private static boolean Document_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Document_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!Document_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "Document_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // ( Block )
  private static boolean Document_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Document_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = Block(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // EmphStar | EmphUnderscore
  public static boolean Emph(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Emph")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<emph>");
    result_ = EmphStar(builder_, level_ + 1);
    if (!result_) result_ = EmphUnderscore(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, EMPH, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // '*' !Whitespace (!'*' Inline)+ '*'
  static boolean EmphStar(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "EmphStar")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "*");
    result_ = result_ && EmphStar_1(builder_, level_ + 1);
    result_ = result_ && EmphStar_2(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "*");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // !Whitespace
  private static boolean EmphStar_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "EmphStar_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !Whitespace(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  // (!'*' Inline)+
  private static boolean EmphStar_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "EmphStar_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = EmphStar_2_0(builder_, level_ + 1);
    int pos_ = current_position_(builder_);
    while (result_) {
      if (!EmphStar_2_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "EmphStar_2", pos_)) break;
      pos_ = current_position_(builder_);
    }
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // !'*' Inline
  private static boolean EmphStar_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "EmphStar_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = EmphStar_2_0_0(builder_, level_ + 1);
    result_ = result_ && Inline(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // !'*'
  private static boolean EmphStar_2_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "EmphStar_2_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !consumeToken(builder_, "*");
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // '_' !Whitespace (!'_' Inline)+ '_'
  static boolean EmphUnderscore(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "EmphUnderscore")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "_");
    result_ = result_ && EmphUnderscore_1(builder_, level_ + 1);
    result_ = result_ && EmphUnderscore_2(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "_");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // !Whitespace
  private static boolean EmphUnderscore_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "EmphUnderscore_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !Whitespace(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  // (!'_' Inline)+
  private static boolean EmphUnderscore_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "EmphUnderscore_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = EmphUnderscore_2_0(builder_, level_ + 1);
    int pos_ = current_position_(builder_);
    while (result_) {
      if (!EmphUnderscore_2_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "EmphUnderscore_2", pos_)) break;
      pos_ = current_position_(builder_);
    }
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // !'_' Inline
  private static boolean EmphUnderscore_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "EmphUnderscore_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = EmphUnderscore_2_0_0(builder_, level_ + 1);
    result_ = result_ && Inline(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // !'_'
  private static boolean EmphUnderscore_2_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "EmphUnderscore_2_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !consumeToken(builder_, "_");
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // TerminalEndline | NormalEndline
  public static boolean EndLine(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "EndLine")) return false;
    if (!nextTokenIs(builder_, "<end line>", NEWLINE, SPACECHAR)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<end line>");
    result_ = TerminalEndline(builder_, level_ + 1);
    if (!result_) result_ = NormalEndline(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, END_LINE, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // NonindentSpace Number '.' Spacechar+
  public static boolean Enumerator(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Enumerator")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<enumerator>");
    result_ = NonindentSpace(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, NUMBER);
    result_ = result_ && consumeToken(builder_, ".");
    result_ = result_ && Enumerator_3(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, ENUMERATOR, result_, false, null);
    return result_;
  }

  // Spacechar+
  private static boolean Enumerator_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Enumerator_3")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, SPACECHAR);
    int pos_ = current_position_(builder_);
    while (result_) {
      if (!consumeToken(builder_, SPACECHAR)) break;
      if (!empty_element_parsed_guard_(builder_, "Enumerator_3", pos_)) break;
      pos_ = current_position_(builder_);
    }
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // NonindentSpace
  //                  ( '*' OptionalSpace '*' OptionalSpace '*' (OptionalSpace '*')*
  //                  | '-' OptionalSpace '-' OptionalSpace '-' (OptionalSpace '-')*
  //                  | '_' OptionalSpace '_' OptionalSpace '_' (OptionalSpace '_')*)
  //                  OptionalSpace Newline BlankLine+
  public static boolean HorizontalRule(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "HorizontalRule")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<horizontal rule>");
    result_ = NonindentSpace(builder_, level_ + 1);
    result_ = result_ && HorizontalRule_1(builder_, level_ + 1);
    result_ = result_ && OptionalSpace(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, NEWLINE);
    result_ = result_ && HorizontalRule_4(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, HORIZONTAL_RULE, result_, false, null);
    return result_;
  }

  // '*' OptionalSpace '*' OptionalSpace '*' (OptionalSpace '*')*
  //                  | '-' OptionalSpace '-' OptionalSpace '-' (OptionalSpace '-')*
  //                  | '_' OptionalSpace '_' OptionalSpace '_' (OptionalSpace '_')*
  private static boolean HorizontalRule_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "HorizontalRule_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = HorizontalRule_1_0(builder_, level_ + 1);
    if (!result_) result_ = HorizontalRule_1_1(builder_, level_ + 1);
    if (!result_) result_ = HorizontalRule_1_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // '*' OptionalSpace '*' OptionalSpace '*' (OptionalSpace '*')*
  private static boolean HorizontalRule_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "HorizontalRule_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "*");
    result_ = result_ && OptionalSpace(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "*");
    result_ = result_ && OptionalSpace(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "*");
    result_ = result_ && HorizontalRule_1_0_5(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (OptionalSpace '*')*
  private static boolean HorizontalRule_1_0_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "HorizontalRule_1_0_5")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!HorizontalRule_1_0_5_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "HorizontalRule_1_0_5", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // OptionalSpace '*'
  private static boolean HorizontalRule_1_0_5_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "HorizontalRule_1_0_5_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = OptionalSpace(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "*");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // '-' OptionalSpace '-' OptionalSpace '-' (OptionalSpace '-')*
  private static boolean HorizontalRule_1_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "HorizontalRule_1_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "-");
    result_ = result_ && OptionalSpace(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "-");
    result_ = result_ && OptionalSpace(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "-");
    result_ = result_ && HorizontalRule_1_1_5(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (OptionalSpace '-')*
  private static boolean HorizontalRule_1_1_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "HorizontalRule_1_1_5")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!HorizontalRule_1_1_5_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "HorizontalRule_1_1_5", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // OptionalSpace '-'
  private static boolean HorizontalRule_1_1_5_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "HorizontalRule_1_1_5_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = OptionalSpace(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "-");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // '_' OptionalSpace '_' OptionalSpace '_' (OptionalSpace '_')*
  private static boolean HorizontalRule_1_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "HorizontalRule_1_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "_");
    result_ = result_ && OptionalSpace(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "_");
    result_ = result_ && OptionalSpace(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "_");
    result_ = result_ && HorizontalRule_1_2_5(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (OptionalSpace '_')*
  private static boolean HorizontalRule_1_2_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "HorizontalRule_1_2_5")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!HorizontalRule_1_2_5_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "HorizontalRule_1_2_5", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // OptionalSpace '_'
  private static boolean HorizontalRule_1_2_5_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "HorizontalRule_1_2_5_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = OptionalSpace(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "_");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // BlankLine+
  private static boolean HorizontalRule_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "HorizontalRule_4")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = BlankLine(builder_, level_ + 1);
    int pos_ = current_position_(builder_);
    while (result_) {
      if (!BlankLine(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "HorizontalRule_4", pos_)) break;
      pos_ = current_position_(builder_);
    }
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // String
  public static boolean Href(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Href")) return false;
    if (!nextTokenIs(builder_, STRING)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, STRING);
    exit_section_(builder_, marker_, HREF, result_);
    return result_;
  }

  /* ********************************************************** */
  // '[' Target ']' '(' Href ')'
  static boolean HrefLink(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "HrefLink")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "[");
    result_ = result_ && Target(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "]");
    result_ = result_ && consumeToken(builder_, "(");
    result_ = result_ && Href(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ")");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // "\t" | "    "
  static boolean Indent(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Indent")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "\t");
    if (!result_) result_ = consumeToken(builder_, "    ");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // String | Number | EndLine | Spacechar+ | Strong | Emph | Link
  public static boolean Inline(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Inline")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<inline>");
    result_ = consumeToken(builder_, STRING);
    if (!result_) result_ = consumeToken(builder_, NUMBER);
    if (!result_) result_ = EndLine(builder_, level_ + 1);
    if (!result_) result_ = Inline_3(builder_, level_ + 1);
    if (!result_) result_ = Strong(builder_, level_ + 1);
    if (!result_) result_ = Emph(builder_, level_ + 1);
    if (!result_) result_ = Link(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, INLINE, result_, false, null);
    return result_;
  }

  // Spacechar+
  private static boolean Inline_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Inline_3")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, SPACECHAR);
    int pos_ = current_position_(builder_);
    while (result_) {
      if (!consumeToken(builder_, SPACECHAR)) break;
      if (!empty_element_parsed_guard_(builder_, "Inline_3", pos_)) break;
      pos_ = current_position_(builder_);
    }
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // (!EndLine Inline | EndLine &Inline )+ EndLine?
  static boolean Inlines(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Inlines")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = Inlines_0(builder_, level_ + 1);
    result_ = result_ && Inlines_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (!EndLine Inline | EndLine &Inline )+
  private static boolean Inlines_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Inlines_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = Inlines_0_0(builder_, level_ + 1);
    int pos_ = current_position_(builder_);
    while (result_) {
      if (!Inlines_0_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "Inlines_0", pos_)) break;
      pos_ = current_position_(builder_);
    }
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // !EndLine Inline | EndLine &Inline
  private static boolean Inlines_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Inlines_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = Inlines_0_0_0(builder_, level_ + 1);
    if (!result_) result_ = Inlines_0_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // !EndLine Inline
  private static boolean Inlines_0_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Inlines_0_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = Inlines_0_0_0_0(builder_, level_ + 1);
    result_ = result_ && Inline(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // !EndLine
  private static boolean Inlines_0_0_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Inlines_0_0_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !EndLine(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  // EndLine &Inline
  private static boolean Inlines_0_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Inlines_0_0_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = EndLine(builder_, level_ + 1);
    result_ = result_ && Inlines_0_0_1_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // &Inline
  private static boolean Inlines_0_0_1_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Inlines_0_0_1_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _AND_, null);
    result_ = Inline(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  // EndLine?
  private static boolean Inlines_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Inlines_1")) return false;
    EndLine(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // HrefLink | ReferenceLink
  public static boolean Link(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Link")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<link>");
    result_ = HrefLink(builder_, level_ + 1);
    if (!result_) result_ = ReferenceLink(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, LINK, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // (ListItem BlankLine*)+
  static boolean List(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "List")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = List_0(builder_, level_ + 1);
    int pos_ = current_position_(builder_);
    while (result_) {
      if (!List_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "List", pos_)) break;
      pos_ = current_position_(builder_);
    }
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // ListItem BlankLine*
  private static boolean List_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "List_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = ListItem(builder_, level_ + 1);
    result_ = result_ && List_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // BlankLine*
  private static boolean List_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "List_0_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!BlankLine(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "List_0_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // !BlankLine Plain ( ListBlockLine )*
  public static boolean ListBlock(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ListBlock")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<list block>");
    result_ = ListBlock_0(builder_, level_ + 1);
    result_ = result_ && Plain(builder_, level_ + 1);
    result_ = result_ && ListBlock_2(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, LIST_BLOCK, result_, false, null);
    return result_;
  }

  // !BlankLine
  private static boolean ListBlock_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ListBlock_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !BlankLine(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  // ( ListBlockLine )*
  private static boolean ListBlock_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ListBlock_2")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!ListBlock_2_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "ListBlock_2", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // ( ListBlockLine )
  private static boolean ListBlock_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ListBlock_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = ListBlockLine(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // !BlankLine !(Indent? (Bullet | Enumerator)) !HorizontalRule Indent? Plain
  public static boolean ListBlockLine(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ListBlockLine")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<list block line>");
    result_ = ListBlockLine_0(builder_, level_ + 1);
    result_ = result_ && ListBlockLine_1(builder_, level_ + 1);
    result_ = result_ && ListBlockLine_2(builder_, level_ + 1);
    result_ = result_ && ListBlockLine_3(builder_, level_ + 1);
    result_ = result_ && Plain(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, LIST_BLOCK_LINE, result_, false, null);
    return result_;
  }

  // !BlankLine
  private static boolean ListBlockLine_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ListBlockLine_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !BlankLine(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  // !(Indent? (Bullet | Enumerator))
  private static boolean ListBlockLine_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ListBlockLine_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !ListBlockLine_1_0(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  // Indent? (Bullet | Enumerator)
  private static boolean ListBlockLine_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ListBlockLine_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = ListBlockLine_1_0_0(builder_, level_ + 1);
    result_ = result_ && ListBlockLine_1_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // Indent?
  private static boolean ListBlockLine_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ListBlockLine_1_0_0")) return false;
    Indent(builder_, level_ + 1);
    return true;
  }

  // Bullet | Enumerator
  private static boolean ListBlockLine_1_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ListBlockLine_1_0_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = Bullet(builder_, level_ + 1);
    if (!result_) result_ = Enumerator(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // !HorizontalRule
  private static boolean ListBlockLine_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ListBlockLine_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !HorizontalRule(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  // Indent?
  private static boolean ListBlockLine_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ListBlockLine_3")) return false;
    Indent(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // BlankLine* (Indent ListBlock)+
  public static boolean ListContinuationBlock(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ListContinuationBlock")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<list continuation block>");
    result_ = ListContinuationBlock_0(builder_, level_ + 1);
    result_ = result_ && ListContinuationBlock_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, LIST_CONTINUATION_BLOCK, result_, false, null);
    return result_;
  }

  // BlankLine*
  private static boolean ListContinuationBlock_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ListContinuationBlock_0")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!BlankLine(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "ListContinuationBlock_0", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // (Indent ListBlock)+
  private static boolean ListContinuationBlock_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ListContinuationBlock_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = ListContinuationBlock_1_0(builder_, level_ + 1);
    int pos_ = current_position_(builder_);
    while (result_) {
      if (!ListContinuationBlock_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "ListContinuationBlock_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // Indent ListBlock
  private static boolean ListContinuationBlock_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ListContinuationBlock_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = Indent(builder_, level_ + 1);
    result_ = result_ && ListBlock(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // (Bullet | Enumerator) ListBlock ( ListContinuationBlock )*
  public static boolean ListItem(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ListItem")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<list item>");
    result_ = ListItem_0(builder_, level_ + 1);
    result_ = result_ && ListBlock(builder_, level_ + 1);
    result_ = result_ && ListItem_2(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, LIST_ITEM, result_, false, null);
    return result_;
  }

  // Bullet | Enumerator
  private static boolean ListItem_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ListItem_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = Bullet(builder_, level_ + 1);
    if (!result_) result_ = Enumerator(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // ( ListContinuationBlock )*
  private static boolean ListItem_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ListItem_2")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!ListItem_2_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "ListItem_2", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // ( ListContinuationBlock )
  private static boolean ListItem_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ListItem_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = ListContinuationBlock(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // ("   " | "  " | " ")?
  static boolean NonindentSpace(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "NonindentSpace")) return false;
    NonindentSpace_0(builder_, level_ + 1);
    return true;
  }

  // "   " | "  " | " "
  private static boolean NonindentSpace_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "NonindentSpace_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "   ");
    if (!result_) result_ = consumeToken(builder_, "  ");
    if (!result_) result_ = consumeToken(builder_, " ");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // OptionalSpace Newline !BlankLine
  static boolean NormalEndline(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "NormalEndline")) return false;
    if (!nextTokenIs(builder_, "", NEWLINE, SPACECHAR)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = OptionalSpace(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, NEWLINE);
    result_ = result_ && NormalEndline_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // !BlankLine
  private static boolean NormalEndline_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "NormalEndline_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !BlankLine(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // Spacechar*
  static boolean OptionalSpace(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "OptionalSpace")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!consumeToken(builder_, SPACECHAR)) break;
      if (!empty_element_parsed_guard_(builder_, "OptionalSpace", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // &Enumerator List
  public static boolean OrderedList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "OrderedList")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<ordered list>");
    result_ = OrderedList_0(builder_, level_ + 1);
    result_ = result_ && List(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, ORDERED_LIST, result_, false, null);
    return result_;
  }

  // &Enumerator
  private static boolean OrderedList_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "OrderedList_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _AND_, null);
    result_ = Enumerator(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // NonindentSpace Inlines (BlankLine+ | TerminalEndline)
  public static boolean Para(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Para")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<para>");
    result_ = NonindentSpace(builder_, level_ + 1);
    result_ = result_ && Inlines(builder_, level_ + 1);
    result_ = result_ && Para_2(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, PARA, result_, false, null);
    return result_;
  }

  // BlankLine+ | TerminalEndline
  private static boolean Para_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Para_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = Para_2_0(builder_, level_ + 1);
    if (!result_) result_ = TerminalEndline(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // BlankLine+
  private static boolean Para_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Para_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = BlankLine(builder_, level_ + 1);
    int pos_ = current_position_(builder_);
    while (result_) {
      if (!BlankLine(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "Para_2_0", pos_)) break;
      pos_ = current_position_(builder_);
    }
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // Inlines
  static boolean Plain(PsiBuilder builder_, int level_) {
    return Inlines(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // '[' Target ']'
  static boolean ReferenceLink(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ReferenceLink")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "[");
    result_ = result_ && Target(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "]");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // Spacechar+
  static boolean RequiredSpace(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "RequiredSpace")) return false;
    if (!nextTokenIs(builder_, SPACECHAR)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, SPACECHAR);
    int pos_ = current_position_(builder_);
    while (result_) {
      if (!consumeToken(builder_, SPACECHAR)) break;
      if (!empty_element_parsed_guard_(builder_, "RequiredSpace", pos_)) break;
      pos_ = current_position_(builder_);
    }
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // StrongStar | StrongUnderscore
  public static boolean Strong(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Strong")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<strong>");
    result_ = StrongStar(builder_, level_ + 1);
    if (!result_) result_ = StrongUnderscore(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, STRONG, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // '**' !Whitespace (!'**' Inline)+ '**'
  public static boolean StrongStar(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "StrongStar")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<strong star>");
    result_ = consumeToken(builder_, "**");
    result_ = result_ && StrongStar_1(builder_, level_ + 1);
    result_ = result_ && StrongStar_2(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "**");
    exit_section_(builder_, level_, marker_, STRONG_STAR, result_, false, null);
    return result_;
  }

  // !Whitespace
  private static boolean StrongStar_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "StrongStar_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !Whitespace(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  // (!'**' Inline)+
  private static boolean StrongStar_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "StrongStar_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = StrongStar_2_0(builder_, level_ + 1);
    int pos_ = current_position_(builder_);
    while (result_) {
      if (!StrongStar_2_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "StrongStar_2", pos_)) break;
      pos_ = current_position_(builder_);
    }
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // !'**' Inline
  private static boolean StrongStar_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "StrongStar_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = StrongStar_2_0_0(builder_, level_ + 1);
    result_ = result_ && Inline(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // !'**'
  private static boolean StrongStar_2_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "StrongStar_2_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !consumeToken(builder_, "**");
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // '__' !Whitespace (!'__' Inline)+ '__'
  public static boolean StrongUnderscore(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "StrongUnderscore")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<strong underscore>");
    result_ = consumeToken(builder_, "__");
    result_ = result_ && StrongUnderscore_1(builder_, level_ + 1);
    result_ = result_ && StrongUnderscore_2(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "__");
    exit_section_(builder_, level_, marker_, STRONG_UNDERSCORE, result_, false, null);
    return result_;
  }

  // !Whitespace
  private static boolean StrongUnderscore_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "StrongUnderscore_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !Whitespace(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  // (!'__' Inline)+
  private static boolean StrongUnderscore_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "StrongUnderscore_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = StrongUnderscore_2_0(builder_, level_ + 1);
    int pos_ = current_position_(builder_);
    while (result_) {
      if (!StrongUnderscore_2_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "StrongUnderscore_2", pos_)) break;
      pos_ = current_position_(builder_);
    }
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // !'__' Inline
  private static boolean StrongUnderscore_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "StrongUnderscore_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = StrongUnderscore_2_0_0(builder_, level_ + 1);
    result_ = result_ && Inline(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // !'__'
  private static boolean StrongUnderscore_2_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "StrongUnderscore_2_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !consumeToken(builder_, "__");
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // String
  public static boolean Target(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Target")) return false;
    if (!nextTokenIs(builder_, STRING)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, STRING);
    exit_section_(builder_, marker_, TARGET, result_);
    return result_;
  }

  /* ********************************************************** */
  // OptionalSpace Newline <<eof>>
  static boolean TerminalEndline(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "TerminalEndline")) return false;
    if (!nextTokenIs(builder_, "", NEWLINE, SPACECHAR)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = OptionalSpace(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, NEWLINE);
    result_ = result_ && eof(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // Spacechar | Newline
  public static boolean Whitespace(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Whitespace")) return false;
    if (!nextTokenIs(builder_, "<whitespace>", NEWLINE, SPACECHAR)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<whitespace>");
    result_ = consumeToken(builder_, SPACECHAR);
    if (!result_) result_ = consumeToken(builder_, NEWLINE);
    exit_section_(builder_, level_, marker_, WHITESPACE, result_, false, null);
    return result_;
  }

}
