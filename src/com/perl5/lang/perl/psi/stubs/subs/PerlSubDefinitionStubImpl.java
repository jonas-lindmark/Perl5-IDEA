/*
 * Copyright 2015 Alexandr Evstigneev
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

package com.perl5.lang.perl.psi.stubs.subs;

import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.perl5.lang.perl.lexer.PerlAnnotations;
import com.perl5.lang.perl.psi.utils.PerlSubAnnotations;
import com.perl5.lang.perl.psi.utils.PerlSubArgument;
import com.perl5.lang.perl.psi.PsiPerlSubDefinition;
import com.perl5.lang.perl.psi.stubs.PerlStubElementTypes;

import java.util.List;

/**
 * Created by hurricup on 25.05.2015.
 */
public class PerlSubDefinitionStubImpl extends StubBase<PsiPerlSubDefinition> implements PerlSubDefinitionStub
{
	private final String packageName;
	private final String functionName;
	private final List<PerlSubArgument> myArguments;
	private final boolean isMethod;
	private final PerlSubAnnotations myAnnotations;

	public PerlSubDefinitionStubImpl(final StubElement parent, final String packageName, final String functionName, List<PerlSubArgument> arguments, boolean isMethod, PerlSubAnnotations annotations)
	{
		super(parent, PerlStubElementTypes.SUB_DEFINITION);
		this.packageName = packageName;
		this.functionName = functionName;
		myArguments = arguments;
		this.isMethod = isMethod;
		myAnnotations = annotations;
	}

	@Override
	public String getPackageName()
	{
		return packageName;
	}

	@Override
	public String getSubName()
	{
		return functionName;
	}

	@Override
	public List<PerlSubArgument> getSubArgumentsList()
	{
		return myArguments;
	}

	@Override
	public boolean isMethod()
	{
		return isMethod;
	}

	@Override
	public PerlSubAnnotations getSubAnnotations()
	{
		return myAnnotations;
	}

	@Override
	public String getCanonicalName()
	{
		return getPackageName() + "::" + getSubName();
	}
}
