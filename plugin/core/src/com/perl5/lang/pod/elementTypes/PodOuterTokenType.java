/*
 * Copyright 2015-2021 Alexandr Evstigneev
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

package com.perl5.lang.pod.elementTypes;

import com.intellij.lang.ASTFactory;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.impl.source.tree.TreeUtil;
import com.intellij.psi.tree.IReparseableLeafElementType;
import com.intellij.psi.tree.OuterLanguageElementType;
import com.perl5.lang.perl.PerlLanguage;
import com.perl5.lang.perl.parser.elementTypes.PerlReparseableElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.perl5.lang.perl.lexer.PerlElementTypesGenerated.POD;

public class PodOuterTokenType extends OuterLanguageElementType implements IReparseableLeafElementType<ASTNode> {
  private static final Logger LOG = Logger.getInstance(PodOuterTokenType.class);

  public PodOuterTokenType() {
    super("POD_OUTER", PerlLanguage.INSTANCE);
  }

  private boolean isReparseable(@NotNull ASTNode leaf, @NotNull CharSequence newText) {
    var fileElement = TreeUtil.getFileElement((TreeElement)leaf);
    if (fileElement == null) {
      LOG.debug("Unable to find file element for ", leaf);
      return false;
    }

    var prevLeaf = TreeUtil.prevLeaf(leaf);
    var nextLeaf = TreeUtil.nextLeaf(leaf);
    var currentLeafRange = leaf.getTextRange();

    int startOffset = prevLeaf == null ? currentLeafRange.getStartOffset() : prevLeaf.getStartOffset();
    int endOffset = nextLeaf == null ? currentLeafRange.getEndOffset() : nextLeaf.getTextRange().getEndOffset();

    var originalChars = fileElement.getChars().subSequence(startOffset, endOffset);
    int currentLeafRelativeStart = currentLeafRange.getStartOffset() - startOffset;
    int currentLeafRelativeEnd = currentLeafRange.getEndOffset() - startOffset;
    var newChars = StringUtil.replaceSubSequence(originalChars, currentLeafRelativeStart, currentLeafRelativeEnd, newText);
    var newLeafRange = TextRange.from(currentLeafRelativeStart, newText.length());

    var baseLexer = PerlReparseableElementType.createLexer(leaf);
    baseLexer.start(newChars);

    // first token
    if (prevLeaf != null && (
      baseLexer.getTokenType() != prevLeaf.getElementType() ||
      baseLexer.getTokenEnd() != prevLeaf.getTextLength())) {
      LOG.debug("First token mismatch: type or range. Original: ", prevLeaf,
                "; new one: ", baseLexer.getTokenType(), " with length: ", baseLexer.getTokenEnd());
      return false;
    }

    baseLexer.advance();

    // mid tokens
    while (true) {
      var tokenType = baseLexer.getTokenType();
      if (tokenType == null || tokenType == POD) {
        LOG.debug("Found POD element inside the modified fragment at ", baseLexer.getTokenStart() + startOffset);
        return false;
      }
      if (baseLexer.getTokenEnd() == newLeafRange.getEndOffset()) {
        break;
      }
      if (baseLexer.getTokenEnd() > newLeafRange.getEndOffset()) {
        LOG.debug("Token overlapped new leaf at ", baseLexer.getTokenStart() + startOffset, "; type: ", baseLexer.getTokenType());
        return false;
      }
      baseLexer.advance();
    }

    // last token
    baseLexer.advance();
    if (nextLeaf == null) {
      if (baseLexer.getTokenType() == null) {
        LOG.debug("MATCHED as the last element of the file");
        return true;
      }
      LOG.debug("Has next token, when should not, at ", baseLexer.getTokenStart() + startOffset, "; type: ", baseLexer.getTokenType());
      return false;
    }

    if (nextLeaf.getElementType() != baseLexer.getTokenType() ||
        baseLexer.getTokenEnd() - baseLexer.getTokenStart() != nextLeaf.getTextLength()) {
      LOG.debug("Next token mismatch, original: ", nextLeaf, " new one ", baseLexer.getTokenType(), " length ",
                baseLexer.getTokenEnd() - baseLexer.getTokenStart());
      return false;
    }
    LOG.debug("MATCHED as modified");
    return true;
  }

  @Override
  public @Nullable ASTNode reparseLeaf(@NotNull ASTNode leaf, @NotNull CharSequence newText) {
    return isReparseable(leaf, newText) ? ASTFactory.leaf(this, newText) : null;
  }
}
