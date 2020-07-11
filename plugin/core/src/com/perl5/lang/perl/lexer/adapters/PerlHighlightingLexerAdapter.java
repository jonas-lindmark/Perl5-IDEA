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

import com.intellij.lexer.LayeredLexer;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.tree.IElementType;
import com.perl5.lang.perl.lexer.PerlElementTypes;
import com.perl5.lang.perl.lexer.PerlLexer;
import com.perl5.lang.perl.lexer.PerlLexingContext;
import com.perl5.lang.pod.lexer.PodLexerAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class PerlHighlightingLexerAdapter extends LayeredLexer implements PerlElementTypes {
  public PerlHighlightingLexerAdapter(@Nullable Project project) {
    this(project, new PerlMergingLexerAdapter(PerlLexingContext.create(project).withAllowToMergeCode(false).withEnforcedSublexing(true)));
  }

  public PerlHighlightingLexerAdapter(@Nullable Project project, @NotNull Lexer lexer) {
    super(lexer);
    registerSelfStoppingLayer(
      new PodLexerAdapter(project),
      new IElementType[]{POD},
      IElementType.EMPTY_ARRAY
    );
    PerlLexingContext baseContext = PerlLexingContext.create(project).withAllowToMergeCode(false).withEnforcedSublexing(true);
    registerSelfStoppingLayer(
      new PerlMergingLexerAdapter(baseContext.withEnforcedInitialState(PerlLexer.STRING_Q)),
      new IElementType[]{HEREDOC},
      IElementType.EMPTY_ARRAY
    );
    registerSelfStoppingLayer(
      new PerlMergingLexerAdapter(baseContext.withEnforcedInitialState(PerlLexer.STRING_QQ)),
      new IElementType[]{HEREDOC_QQ},
      IElementType.EMPTY_ARRAY
    );
    registerSelfStoppingLayer(
      new PerlMergingLexerAdapter(baseContext.withEnforcedInitialState(PerlLexer.STRING_QX)),
      new IElementType[]{HEREDOC_QX},
      IElementType.EMPTY_ARRAY
    );
    registerSelfStoppingLayer(
      new PerlMergingLexerAdapter(baseContext.withEnforcedInitialState(PerlLexer.ANNOTATION)),
      new IElementType[]{COMMENT_ANNOTATION},
      IElementType.EMPTY_ARRAY
    );
    registerSelfStoppingLayer(
      new PerlMergingLexerAdapter(baseContext),
      new IElementType[]{LP_CODE_BLOCK},
      IElementType.EMPTY_ARRAY
    );
    registerSelfStoppingLayer(
      new PerlMergingLexerAdapter(baseContext.withTryCatchSyntax(true)),
      new IElementType[]{LP_CODE_BLOCK_WITH_TRYCATCH},
      IElementType.EMPTY_ARRAY
    );
  }
}