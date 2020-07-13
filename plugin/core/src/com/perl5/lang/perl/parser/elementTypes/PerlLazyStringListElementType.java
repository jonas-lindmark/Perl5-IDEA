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

package com.perl5.lang.perl.parser.elementTypes;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.util.PsiUtilCore;
import com.perl5.lang.perl.lexer.PerlElementTypesGenerated;
import com.perl5.lang.perl.lexer.PerlLexer;
import com.perl5.lang.perl.lexer.PerlLexingContext;
import org.jetbrains.annotations.NotNull;


public class PerlLazyStringListElementType extends PerlLazyBlockElementType {
  private static final Logger LOG = Logger.getInstance(PerlLazyStringListElementType.class);

  public PerlLazyStringListElementType(String name) {
    super(name);
  }

  @Override
  protected @NotNull PerlLexingContext getLexingContext(@NotNull Project project, @NotNull ASTNode chameleon) {
    PerlLexingContext baseContext = super.getLexingContext(project, chameleon).withEnforcedInitialState(PerlLexer.STRING_LIST);
    ASTNode prevNode = chameleon.getTreePrev();
    if (PsiUtilCore.getElementType(prevNode) != PerlElementTypesGenerated.QUOTE_SINGLE_OPEN) {
      LOG.error("Unable to find opening quote, got: " + prevNode);
      return baseContext;
    }
    CharSequence nodeChars = prevNode.getChars();
    if (nodeChars.length() != 1) {
      LOG.error("Got " + nodeChars);
    }
    return baseContext.withOpenChar(nodeChars.charAt(0));
  }
}