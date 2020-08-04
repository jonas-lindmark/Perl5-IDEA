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
import com.intellij.openapi.util.ClearableLazyValue;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.perl5.lang.perl.idea.project.PerlNamesCache;
import com.perl5.lang.perl.lexer.PerlProtoLexer;
import com.perl5.lang.perl.psi.references.PerlImplicitDeclarationsService;
import com.perl5.lang.perl.util.PerlPackageUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.perl5.lang.perl.lexer.PerlElementTypesGenerated.*;
import static com.perl5.lang.perl.util.PerlPackageUtil.*;

public class PerlSmartLexerAdapter extends PerlSmartLexerAdapterBase<PerlProtoLexer> {
  private static final TokenSet TOKENS_TO_MERGE = TokenSet.create(
    STRING_CONTENT, REGEX_TOKEN, STRING_CONTENT_QQ, STRING_CONTENT_XQ, TokenType.WHITE_SPACE, COMMENT_BLOCK
  );

  private static final Set<String> BUILT_IN_QUALIFYING_NAMESPACES = Set.of(
    MAIN_NAMESPACE_FULL, SUPER_NAMESPACE_FULL, CORE_NAMESPACE_FULL, CORE_GLOBAL_NAMESPACE_FULL, UNIVERSAL_NAMESPACE_FULL
  );

  private static final Set<String> LIST_OPERATORS = Set.of(
    "warn", "waitpid", "vec", "utime", "untie", "tied", "tie", "system", "syscall", "symlink", "substr", "sprintf", "socketpair", "socket",
    "shutdown", "shmwrite", "shmread", "shmget", "shmctl", "setsockopt", "setpriority", "setpgrp", "send", "semop", "semget", "semctl",
    "rindex", "rename", "recv", "pipe", "pack", "msgsnd", "msgrcv", "msgget", "msgctl", "lock", "listen", "link", "kill", "join", "index",
    "getsockopt", "getservbyport", "getservbyname", "getprotobynumber", "getpriority", "getnetbyaddr", "gethostbyaddr", "formline", "exec",
    "dump", "die", "dbmopen", "dbmclose", "crypt", "connect", "chown", "chmod", "bind", "atan2", "accept"
  );

  private static final Set<String> NAMED_ARGUMENTLESS = Set.of(
    "wait", "times", "time", "setpwent", "setgrent", "getservent", "getpwent", "getprotoent", "getppid", "getnetent", "getlogin",
    "gethostent", "getgrent", "fork", "endservent", "endpwent", "endprotoent", "endnetent", "endhostent", "endgrent", "break"
  );

  private static final Set<String> NAMED_UNARY = Set.of(
    "umask", "srand", "sleep", "setservent", "setprotoent", "setnetent", "sethostent", "reset", "readline", "rand", "prototype",
    "localtime", "gmtime", "getsockname", "getpwuid", "getpwnam", "getprotobyname", "getpgrp", "getpeername", "getnetbyname",
    "gethostbyname", "getgrnam", "getgrgid", "exists", "caller",
    // following were implicit users, but looks like behave the same
    "unpack", "unlink", "ucfirst", "uc", "study", "stat", "sqrt", "sin", "rmdir", "reverse", "ref", "readpipe", "readlink", "quotemeta",
    "pos", "ord", "oct", "mkdir", "lstat", "log", "length", "lcfirst", "lc", "int", "hex", "glob", "fc", "exp", "evalbytes", "cos",
    "chroot", "chr", "chop", "chomp", "alarm", "abs"
  );

  private static final Set<String> BARE_HANDLE_ACCEPTORS = Set.of(
    "truncate", "syswrite", "sysseek", "sysread", "sysopen", "stat", "select", "seekdir", "seek", "read", "opendir", "open", "lstat",
    "ioctl", "flock", "fcntl", "binmode"
  );

  private static final Set<String> UNARY_BARE_HANDLE_ACCEPTORS = Set.of(
    "write", "telldir", "tell", "rewinddir", "readdir", "getc", "fileno", "eof", "closedir", "close", "chdir"
  );

  private @Nullable Project myProject;
  private final ClearableLazyValue<Set<String>> mySubNamesProvider = ClearableLazyValue.create(
    () -> myProject == null ? Collections.emptySet() : PerlNamesCache.getInstance(myProject).getSubsNamesSet());
  private final ClearableLazyValue<Set<String>> myNamespaceNamesProvider = ClearableLazyValue.create(
    () -> myProject == null ? Collections.emptySet() : PerlNamesCache.getInstance(myProject).getNamespacesNamesSet());
  private @Nullable PerlImplicitDeclarationsService myImplicitSubsService;
  private final Set<String> myLocalPackages = new HashSet<>();

  public PerlSmartLexerAdapter(@NotNull PerlProtoLexer lexer,
                               @Nullable Project project) {
    super(lexer);
    withProject(project);
  }

  public final PerlSmartLexerAdapter withProject(@Nullable Project project) {
    myProject = project;
    myImplicitSubsService = project == null ? null : PerlImplicitDeclarationsService.getInstance(project);
    mySubNamesProvider.drop();
    myNamespaceNamesProvider.drop();
    return this;
  }

  @Override
  public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
    super.start(buffer, startOffset, endOffset, initialState);
    myLocalPackages.clear();
  }

  @Override
  protected void clarifyToken(@NotNull Token token) {
    IElementType tokenType = token.getTokenType();
    if (tokenType == RAW_QUALIFYING_IDENTIFIER) {
      clarifyQualifyingIdentifier(token);
    }
    else if (tokenType == RAW_IDENTIFIER) {
      clarifyIdentifier(token);
    }
    else {
      super.clarifyToken(token);
    }
  }

  private boolean isKnownSub(@NotNull String canonicalName) {
    return myImplicitSubsService != null && myImplicitSubsService.getSub(canonicalName) != null ||
           mySubNamesProvider.getValue().contains(canonicalName);
  }

  /**
   * Clarifying type for Foo::Bar::
   */
  private void clarifyQualifyingIdentifier(@NotNull Token token) {
    Token nextToken = getNextToken(token);
    if (nextToken == null) {
      token.setClarifiedType(PACKAGE);
      return;
    }
    if (myProject == null) {
      // this happens only with words scanner
      token.setClarifiedType(IDENTIFIER);
    }
    else if (BUILT_IN_QUALIFYING_NAMESPACES.contains(getTokenChars(token).toString())) {
      token.setClarifiedType(QUALIFYING_PACKAGE);
    }
    else if (nextToken.getTokenType() == RAW_IDENTIFIER) {
      String text = getTokensChars(token, nextToken).toString();
      String canonicalName = PerlPackageUtil.getCanonicalName(text);
      if (isKnownSub(canonicalName)) {
        token.setClarifiedType(QUALIFYING_PACKAGE);
        nextToken.setClarifiedType(SUB_NAME);
      }
      else if (isKnownNamespace(canonicalName)) {
        mergeRange(token, nextToken);
        token.setClarifiedType(PACKAGE);
      }
      else {
        token.setClarifiedType(QUALIFYING_PACKAGE);
        nextToken.setClarifiedType(SUB_NAME);
      }
    }
    else {
      token.setClarifiedType(QUALIFYING_PACKAGE);
    }
  }

  protected boolean isKnownNamespace(@NotNull String canonicalName) {
    return myNamespaceNamesProvider.getValue().contains(canonicalName) || myLocalPackages.contains(canonicalName);
  }

  /**
   * Clarifying type for identifier
   */
  private void clarifyIdentifier(@NotNull Token token) {
    if (myProject == null) {
      token.setClarifiedType(IDENTIFIER);
      return;
    }
    String tokenText = getTokenChars(token).toString();

    // this feels like a map
    if (LIST_OPERATORS.contains(tokenText)) {
      token.setClarifiedType(BUILTIN_LIST);
    }
    else if (NAMED_ARGUMENTLESS.contains(tokenText)) {
      token.setClarifiedType(BUILTIN_ARGUMENTLESS);
    }
    else if (NAMED_UNARY.contains(tokenText)) {
      token.setClarifiedType(BUILTIN_UNARY);
    }
    else if (BARE_HANDLE_ACCEPTORS.contains(tokenText)) {
      token.setClarifiedType(BUILTIN_LIST);
      remapNextBareToHandle(token);
    }
    else if (UNARY_BARE_HANDLE_ACCEPTORS.contains(tokenText)) {
      token.setClarifiedType(BUILTIN_UNARY);
      remapNextBareToHandle(token);
    }
    else if (StringUtil.isCapitalized(tokenText) && isKnownNamespace(tokenText)) {
      token.setClarifiedType(PACKAGE);
    }
    else {
      token.setClarifiedType(SUB_NAME);
    }
  }

  private void remapNextBareToHandle(@NotNull Token token) {
    // fixme we should make handle of next bare or bare after paren
  }

  @Override
  protected boolean isMergeableToken(@NotNull Token token) {
    return TOKENS_TO_MERGE.contains(token.getTokenType());
  }
}
