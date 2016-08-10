/*
 * Copyright 2016 Alexandr Evstigneev
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

package com.perl5.lang.perl.fileTypes;

import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import org.jetbrains.annotations.NotNull;

public class PerlFileTypeFactory extends FileTypeFactory
{
	@Override
	public void createFileTypes(@NotNull FileTypeConsumer fileTypeConsumer)
	{
		fileTypeConsumer.consume(PerlFileTypePackage.INSTANCE, PerlFileTypePackage.EXTENSION);
		fileTypeConsumer.consume(PerlFileType.INSTANCE, PerlFileType.EXTENSION_CGI);
		fileTypeConsumer.consume(PerlFileType.INSTANCE, PerlFileType.EXTENSION_PL);
		fileTypeConsumer.consume(PerlFileType.INSTANCE, PerlFileType.EXTENSION_PH);
		fileTypeConsumer.consume(PerlFileType.INSTANCE, PerlFileType.EXTENSION_AL);

		fileTypeConsumer.consume(PerlFileTypeTest.INSTANCE, PerlFileTypeTest.EXTENSION);
	}
}
