/*
 * Copyright 2015-2019 Alexandr Evstigneev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.perl5.lang.perl.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.perl5.lang.perl.extensions.parser.PerlParserExtension;
import com.perl5.lang.perl.parser.builder.PerlBuilder;
import org.jetbrains.annotations.NotNull;


public class PerlParserImpl extends PerlParserGenerated implements PerlParser {
  public static final PsiParser INSTANCE = new PerlParserImpl();

  private static final TokenSet[] EXTENDS_SETS_BACKUP = new TokenSet[EXTENDS_SETS_.length];

  static {
    for (int i = 0; i < EXTENDS_SETS_.length; i++) {
      EXTENDS_SETS_BACKUP[i] = TokenSet.orSet(EXTENDS_SETS_[i]);
    }
  }

  public static void restoreDefaultExtendsSet() {
    for (int i = 0; i < EXTENDS_SETS_BACKUP.length; i++) {
      EXTENDS_SETS_[i] = TokenSet.orSet(EXTENDS_SETS_BACKUP[i]);
    }
  }

  public PerlParserImpl() {
  }

  public boolean parseStatement(PsiBuilder b, int l) {
    return false;
  }

  public boolean parseTerm(PsiBuilder b, int l) {
    for (PerlParserExtension parserExtension : PerlParserExtension.EP_NAME.getExtensionList()) {
      if (parserExtension.parseTerm((PerlBuilder)b, l)) {
        return true;
      }
    }
    return false;
  }

  public boolean parseStatementModifier(PsiBuilder b, int l) {
    return false;
  }

  public boolean parseNestedElementVariation(PsiBuilder b, int l) {
    for (PerlParserExtension parserExtension : PerlParserExtension.EP_NAME.getExtensionList()) {
      if (parserExtension.parseNestedElement((PerlBuilder)b, l)) {
        return true;
      }
    }
    return false;
  }

  @NotNull
  public TokenSet getBadCharacterForbiddenTokens() {
    return BAD_CHARACTER_FORBIDDEN_TOKENS;
  }

  @NotNull
  public TokenSet getStatementRecoveryConsumableTokenSet() {
    return STATEMENT_RECOVERY_CONSUMABLE_TOKENS;
  }

  @NotNull
  public TokenSet getConsumableSemicolonTokens() {
    return CONSUMABLE_SEMI_TOKENS;
  }

  @NotNull
  public TokenSet getAnonHashSuffixTokens() {
    return ANON_HASH_TOKEN_SUFFIXES;
  }

  @NotNull
  public TokenSet getUnconsumableSemicolonTokens() {
    return UNCONSUMABLE_SEMI_TOKENS;
  }

  public boolean parseFileContents(PsiBuilder b, int l) {
    return PerlParserGenerated.file_items(b, l);
  }

  public boolean parseStatementSemi(PsiBuilder b, int l) {
    IElementType tokenType = b.getTokenType();
    if (((PerlBuilder)b).getPerlParser().getConsumableSemicolonTokens().contains(tokenType)) {
      b.advanceLexer();
      return true;
    }
    else if (((PerlBuilder)b).getPerlParser().getUnconsumableSemicolonTokens().contains(tokenType)) {
      return true;
    }
    else if (b.eof()) // eof
    {
      return true;
    }

    b.mark().error("Semicolon expected");

    return true;
  }
}
