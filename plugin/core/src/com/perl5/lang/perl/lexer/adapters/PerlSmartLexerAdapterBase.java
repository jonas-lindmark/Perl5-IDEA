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

import com.intellij.lexer.FlexLexer;
import com.intellij.lexer.LexerBase;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * This adapter should take part of perl lexer work on itself and include all magic on clarifying perl element types
 * Look-ahead in flex is pretty expensive and here we may do this more efficient
 * - it changes semantic of some tokens based on tokens around
 * - may join tokens
 * - it can't split tokens, flex lexer supposed to do that
 */
public class PerlSmartLexerAdapterBase<Flex extends FlexLexer> extends LexerBase {
  private static final Logger LOG = Logger.getInstance(PerlSmartLexerAdapterBase.class);
  private final @NotNull Flex myLexer;
  private Token myTokensHead;
  private Token myTokensTail;
  private Token myCurrentToken;
  private int myBufferStart;
  private int myBufferEnd;
  private CharSequence myBuffer;

  public PerlSmartLexerAdapterBase(@NotNull Flex flex) {
    myLexer = flex;
  }

  @Override
  public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
    myLexer.reset(buffer, startOffset, endOffset, initialState);
    myBufferStart = startOffset;
    myBufferEnd = endOffset;
    myBuffer = buffer;
    resetInternalState();
  }

  protected final void resetInternalState() {
    myTokensHead = null;
    myCurrentToken = null;
    myTokensTail = null;
  }

  public @NotNull Flex getLexer() {
    return myLexer;
  }

  public @Nullable CharSequence getTokenChars() {
    return myBuffer.subSequence(getTokenStart(), getTokenEnd());
  }

  public @NotNull CharSequence getTokenChars(@NotNull Token token) {
    return myBuffer.subSequence(getTokenStart(token), token.tokenEnd);
  }

  @Override
  public void advance() {
    initAndClarify();
    if (myCurrentToken == null) {
      return;
    }
    if (myCurrentToken.nextToken == null) {
      requestNextToken();
    }
    myCurrentToken = myCurrentToken.nextToken;
    // fixme we could drop some head tokens here
  }

  @Override
  public @Nullable IElementType getTokenType() {
    initAndClarify();
    return myCurrentToken == null ? null : myCurrentToken.clarifiedType;
  }

  @Override
  public int getState() {
    initAndClarify();
    return myCurrentToken == null ? myLexer.yystate() : myCurrentToken.lexerState;
  }

  @Override
  public int getTokenEnd() {
    initAndClarify();
    return myCurrentToken == null ? myBufferEnd : myCurrentToken.tokenEnd;
  }

  @Override
  public int getTokenStart() {
    initAndClarify();
    return getTokenStart(myCurrentToken);
  }

  protected final int getTokenStart(@Nullable Token token) {
    if (token == null) {
      return myTokensTail == null ? myBufferStart : myTokensTail.tokenEnd;
    }
    return token.prevToken == null ? myBufferStart : token.prevToken.tokenEnd;
  }

  @Override
  public @NotNull CharSequence getBufferSequence() {
    return myBuffer;
  }

  @Override
  public int getBufferEnd() {
    return myBufferEnd;
  }

  private void initAndClarify() {
    if (myTokensHead == null) {
      requestNextToken();
    }
    if (myCurrentToken == null) {
      return;
    }
    if (myCurrentToken.clarifiedType != null) {
      return;
    }
    mergeWithNext(myCurrentToken);
    clarifyToken(myCurrentToken);
  }

  /**
   * This method may adjust passed {@code token} by looking ahead
   */
  protected void clarifyToken(@NotNull Token token) {
    token.setClarifiedType(token.getTokenType());
  }

  protected void requestNextToken() {
    try {
      int state = myLexer.yystate();
      IElementType tokenType = myLexer.advance();
      if (tokenType == null) {
        return;
      }
      int tokenEnd = myLexer.getTokenEnd();
      Token newToken = new Token(tokenEnd, tokenType, state);
      if (myTokensHead == null) {
        myTokensHead = newToken;
        myCurrentToken = newToken;
        myTokensTail = newToken;
      }
      else {
        myTokensTail.append(newToken);
        myTokensTail = newToken;
      }
    }
    catch (IOException e) {
      LOG.warn(e);
    }
  }

  /**
   * @return true iff this token may be merged with following ones. E.g. has proper type.
   */
  protected boolean isMergeableToken(@NotNull Token token) {
    return false;
  }

  private void mergeWithNext(@NotNull Token token) {
    if (!isMergeableToken(token)) {
      return;
    }
    requestNextToken();
    Token run = token;
    while (token.isSameType(run.getNextToken())) {
      run = run.getNextToken();
      requestNextToken();
    }

    if (run != token) {
      mergeRange(token, run);
    }
  }

  protected final @NotNull CharSequence getTokensChars(@NotNull Token firstToken, @NotNull Token lastToken) {
    return myBuffer.subSequence(getTokenStart(firstToken), lastToken.tokenEnd);
  }

  /**
   * @return next token for the {@code token} with lazy lexing
   */
  protected final @Nullable Token getNextToken(@NotNull Token token) {
    if (token.nextToken == null) {
      requestNextToken();
    }
    return token.nextToken;
  }

  @Contract("null->false")
  protected final boolean isSpaceOrComment(@Nullable Token token) {
    return token != null && isSpaceOrComment(token.getTokenType());
  }

  protected boolean isSpaceOrComment(@NotNull IElementType tokenType) {
    return tokenType == TokenType.WHITE_SPACE;
  }

  protected final @Nullable Token getNextMeaningfulToken(@NotNull Token token) {
    Token run = token;
    while (true) {
      run = getNextToken(run);
      if (run == null) {
        return null;
      }
      if (!isSpaceOrComment(run)) {
        return run;
      }
    }
  }

  /**
   * Merges all tokens from {@code firstToken} to {@code lastToken} into the {@code firstToken}
   */
  protected final void mergeRange(@NotNull Token firstToken, @NotNull Token lastToken) {
    if (myCurrentToken != firstToken) {
      Token run = firstToken.nextToken;
      while (run != null) {
        if (myCurrentToken == run) {
          myCurrentToken = firstToken;
          break;
        }
        if (run == lastToken) {
          break;
        }
        run = run.getNextToken();
      }
    }
    if (myTokensTail == lastToken) {
      myTokensTail = firstToken;
    }
    Token newNextToken = lastToken.getNextToken();
    firstToken.nextToken = newNextToken;
    firstToken.tokenEnd = lastToken.tokenEnd;
    if (newNextToken != null) {
      newNextToken.prevToken = firstToken;
    }
  }

  protected static class Token {
    private int tokenEnd;
    private final @NotNull IElementType tokenType;
    private IElementType clarifiedType;
    private final int lexerState;
    private Token prevToken;
    private Token nextToken;

    public Token(int tokenEnd, @NotNull IElementType tokenType, int lexerState) {
      this.tokenEnd = tokenEnd;
      this.tokenType = tokenType;
      this.lexerState = lexerState;
    }

    @Override
    public String toString() {
      return "Token{" +
             ", tokenEnd=" + tokenEnd +
             ", tokenType=" + tokenType +
             ", clarifiedType=" + clarifiedType +
             ", lexerState=" + lexerState +
             '}';
    }

    void append(@NotNull Token nextToken) {
      this.nextToken = nextToken;
      nextToken.prevToken = this;
    }

    public void setClarifiedType(@NotNull IElementType clarifiedType) {
      this.clarifiedType = clarifiedType;
    }

    public @NotNull IElementType getTokenType() {
      return tokenType;
    }

    public @Nullable Token getNextToken() {
      return nextToken;
    }

    @Contract("null->false")
    public boolean isSameType(@Nullable Token token) {
      return token != null && tokenType == token.tokenType;
    }
  }
}
