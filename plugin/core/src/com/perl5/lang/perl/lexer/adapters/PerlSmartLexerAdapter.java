/*
 * Copyright 2015-2020 Alexandr Evstigneev
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

package com.perl5.lang.perl.lexer.adapters;

import com.intellij.openapi.project.Project;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.TokenSet;
import com.perl5.lang.perl.lexer.PerlProtoLexer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.perl5.lang.perl.lexer.PerlElementTypesGenerated.*;

public class PerlSmartLexerAdapter extends PerlSmartLexerAdapterBase<PerlProtoLexer> {
  public static final TokenSet TOKENS_TO_MERGE = TokenSet.create(
    STRING_CONTENT, REGEX_TOKEN, STRING_CONTENT_QQ, STRING_CONTENT_XQ, TokenType.WHITE_SPACE, COMMENT_BLOCK
  );

  private @Nullable Project myProject;

  public PerlSmartLexerAdapter(@NotNull PerlProtoLexer lexer,
                               @Nullable Project project) {
    super(lexer);
    myProject = project;
  }

  public PerlSmartLexerAdapter withProject(@Nullable Project project) {
    myProject = project;
    return this;
  }

  @Override
  protected void clarifyToken(@NotNull Token token) {
    super.clarifyToken(token);
  }

  @Override
  protected boolean isMergeableToken(@NotNull Token token) {
    return TOKENS_TO_MERGE.contains(token.getTokenType());
  }
}
