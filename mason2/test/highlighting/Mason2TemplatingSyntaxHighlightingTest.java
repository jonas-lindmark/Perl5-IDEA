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

package highlighting;

import base.Mason2TopLevelComponentTestCase;
import com.perl5.lang.mason2.filetypes.MasonInternalComponentFileType;
import org.junit.Test;

public class Mason2TemplatingSyntaxHighlightingTest extends Mason2TopLevelComponentTestCase {
  @Override
  protected String getBaseDataPath() {
    return "testData/highlighting/syntax/templates";
  }

  @Test
  public void testHeredoc() {
    doTest();
  }

  @Test
  public void testTestComponent() {
    doTest();
  }

  @Test
  public void testLiveTemplates() {
    doTest();
  }

  @Test
  public void testIssue1077() {
    doTest();
  }

  protected void doTest() {
    doTest(true);
  }

  private void doTest(boolean checkErrors) {
    doTestHighlighter(checkErrors);
  }

  public static class InternalComponent extends Mason2TemplatingSyntaxHighlightingTest {
    @Override
    public String getFileExtension() {
      return MasonInternalComponentFileType.INTERNAL_COMPONENT_EXTENSION;
    }
  }
}
