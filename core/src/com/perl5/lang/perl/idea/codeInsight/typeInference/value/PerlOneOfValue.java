/*
 * Copyright 2015-2018 Alexandr Evstigneev
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

package com.perl5.lang.perl.idea.codeInsight.typeInference.value;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.perl5.PerlBundle;
import com.perl5.lang.perl.psi.utils.PerlContextType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static com.perl5.lang.perl.idea.codeInsight.typeInference.value.PerlUnknownValue.UNKNOWN_VALUE;

public final class PerlOneOfValue extends PerlValue {
  private static final Logger LOG = Logger.getInstance(PerlOneOfValue.class);
  @NotNull
  private final Set<PerlValue> myVariants;

  private PerlOneOfValue(@NotNull Set<PerlValue> variants) {
    myVariants = Collections.unmodifiableSet(variants);
  }

  PerlOneOfValue(@NotNull StubInputStream dataStream) throws IOException {
    super(dataStream);
    int elementsNumber = dataStream.readVarInt();
    Set<PerlValue> variants = new HashSet<>();
    for (int i = 0; i < elementsNumber; i++) {
      variants.add(PerlValuesManager.readValue(dataStream));
    }
    myVariants = Collections.unmodifiableSet(variants);
  }

  @Nullable
  @Override
  protected PerlContextType getContextType() {
    if (myVariants.isEmpty()) {
      return null;
    }
    PerlContextType contextType = null;
    boolean first = true;
    for (PerlValue variant : myVariants) {
      if (first) {
        contextType = variant.getContextType();
        first = false;
        continue;
      }
      if (contextType != variant.getContextType()) {
        return null;
      }
    }
    return contextType;
  }

  @Override
  protected int getSerializationId() {
    return PerlValuesManager.ONE_OF_ID;
  }

  @Override
  protected void serializeData(@NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeVarInt(myVariants.size());
    for (PerlValue variant : myVariants) {
      variant.serialize(dataStream);
    }
  }

  /**
   * could be cached
   */
  @Override
  protected boolean computeIsDeterministic() {
    return PerlListValue.isDeterministic(myVariants);
  }

  @NotNull
  @Override
  protected PerlValue computeScalarRepresentation() {
    LOG.assertTrue(isDeterministic());
    return convert(PerlValue::computeScalarRepresentation);
  }

  @NotNull
  @Override
  protected Set<String> getNamespaceNames(@NotNull Project project,
                                          @NotNull GlobalSearchScope searchScope,
                                          @Nullable Set<PerlValue> recursion) {
    if (myVariants.isEmpty()) {
      return Collections.emptySet();
    }

    return PerlValuesCacheService.getInstance(project).getNamespaceNames(this, () -> {
      Set<PerlValue> finalRecursion = ObjectUtils.notNull(recursion, new HashSet<>());
      Set<String> result = new HashSet<>();
      myVariants.forEach(it -> result.addAll(it.getNamespaceNames(project, searchScope, finalRecursion)));
      return result;
    });
  }

  @NotNull
  @Override
  protected Set<String> getSubNames(@NotNull Project project,
                                    @NotNull GlobalSearchScope searchScope,
                                    @Nullable Set<PerlValue> recursion) {
    if (myVariants.isEmpty()) {
      return Collections.emptySet();
    }

    return PerlValuesCacheService.getInstance(project).getSubsNames(this, () -> {
      Set<PerlValue> finalRecursion = ObjectUtils.notNull(recursion, new HashSet<>());
      Set<String> result = new HashSet<>();
      myVariants.forEach(it -> result.addAll(it.getSubNames(project, searchScope, finalRecursion)));
      return result;
    });
  }

  @Override
  public boolean canRepresentNamespace(@Nullable String namespaceName) {
    for (PerlValue variant : myVariants) {
      if (variant.canRepresentNamespace(namespaceName)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean canRepresentSubName(@Nullable String subName) {
    for (PerlValue variant : myVariants) {
      if (variant.canRepresentSubName(subName)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public PerlValue convert(@NotNull Function<PerlValue, PerlValue> converter) {
    Builder builder = builder();
    myVariants.forEach(it -> builder.addVariant(converter.apply(it)));
    return builder.build();
  }

  @Override
  public PerlValue convertStrict(@NotNull Function<PerlValue, PerlValue> converter) {
    Builder builder = builder();
    for (PerlValue variant : myVariants) {
      PerlValue convertedValue = converter.apply(variant);
      if (convertedValue.isEmpty()) {
        return UNKNOWN_VALUE;
      }
      builder.addVariant(convertedValue);
    }
    return builder.build();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    PerlOneOfValue of = (PerlOneOfValue)o;

    return myVariants.equals(of.myVariants);
  }

  @Override
  protected int computeHashCode() {
    int result = super.computeHashCode();
    result = 31 * result + myVariants.hashCode();
    return result;
  }

  @Override
  public String toString() {
    List<String> variants = ContainerUtil.map(myVariants, PerlValue::toString);
    ContainerUtil.sort(variants);
    return "OneOf: [" + StringUtil.join(variants, ", ") + "]";
  }

  @NotNull
  @Override
  public String getPresentableText() {
    List<String> variants = ContainerUtil.map(myVariants, PerlValue::getPresentableText);
    ContainerUtil.sort(variants);
    return PerlBundle.message("perl.value.oneof.static.presentable", StringUtil.join(variants, ",\n"));
  }

  @NotNull
  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    @NotNull
    private final Set<PerlValue> myVariants = new HashSet<>();

    private Builder(@NotNull PsiElement... elements) {
      addVariants(elements);
    }

    public Builder addVariants(@NotNull PsiElement... elements) {
      for (PsiElement element : elements) {
        addVariant(element);
      }
      return this;
    }

    public Builder addVariant(@Nullable PsiElement element) {
      return addVariant(from(element));
    }

    public Builder addVariant(@Nullable PerlValue variant) {
      if (variant == null || variant == UNKNOWN_VALUE) {
        return this;
      }

      if (variant instanceof PerlOneOfValue) {
        myVariants.addAll(((PerlOneOfValue)variant).myVariants);
      }
      else {
        myVariants.add(variant);
      }
      return this;
    }

    @NotNull
    public PerlValue build() {
      if (myVariants.isEmpty()) {
        return UNKNOWN_VALUE;
      }
      else if (myVariants.size() == 1) {
        return myVariants.iterator().next();
      }
      else {
        return new PerlOneOfValue(myVariants);
      }
    }
  }
}