/*
 * Copyright 2005-2008 Sascha Weinreuter
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

package org.intellij.lang.xpath.completion;

import org.intellij.lang.xpath.psi.*;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupItem;
import static com.intellij.patterns.PlatformPatterns.psiElement;
import com.intellij.util.ProcessingContext;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/*
* Created by IntelliJ IDEA.
* User: sweinreuter
*/
public class XPathCompletionContributor extends CompletionContributor {
    public static final XPathInsertHandler INSERT_HANDLER = new XPathInsertHandler();

    public XPathCompletionContributor() {
        extend(CompletionType.BASIC, psiElement().withParent(XPathNodeTest.class), new CompletionProvider<CompletionParameters>() {
            protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet result) {
                addResult(result, CompletionLists.getNodeTestCompletions((XPathNodeTest)parameters.getPosition().getParent()), parameters.getPosition(), parameters.getOffset());
            }
        });
        extend(CompletionType.BASIC, psiElement().withParent(XPathAxisSpecifier.class), new CompletionProvider<CompletionParameters>() {
            protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet result) {
                addResult(result, CompletionLists.getAxisCompletions(), parameters.getPosition(), parameters.getOffset());
            }
        });
        extend(CompletionType.BASIC, psiElement().withParent(XPathFunctionCall.class), new CompletionProvider<CompletionParameters>() {
            protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet result) {
                addResult(result, CompletionLists.getFunctionCompletions((XPathFunctionCall)parameters.getPosition().getParent()), parameters.getPosition(), parameters.getOffset());
                addResult(result, CompletionLists.getNodeTypeCompletions(), parameters.getPosition(), parameters.getOffset());
            }
        });
        extend(CompletionType.BASIC, psiElement().withParent(XPathVariableReference.class), new CompletionProvider<CompletionParameters>() {
            protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet result) {
                addResult(result, CompletionLists.getVariableCompletions((XPathVariableReference)parameters.getPosition().getParent()), parameters.getPosition(), parameters.getOffset());
            }
        });
    }

    private static void addResult(CompletionResultSet result, Collection<Lookup> collection, PsiElement position, int offset) {
        result = result.withPrefixMatcher(findPrefixStatic(position, offset));

        for (Lookup lookup : collection) {
            final LookupItem<Lookup> item = new LookupItem<Lookup>(lookup, lookup.toString());
            item.setInsertHandler(INSERT_HANDLER);
            if (lookup.isKeyword()) {
                item.setBold();
            }
            result.addElement(item);
        }
    }

    private static String findPrefixStatic(PsiElement element, int i) {
        String prefix = CompletionData.findPrefixStatic(element, i);

        if (element.getParent() instanceof XPathVariableReference) {
            final String text = element.getText();
            prefix = "$" + text.substring(0, text.indexOf(CompletionLists.INTELLIJ_IDEA_RULEZ));
        }

        if (element.getParent() instanceof XPathNodeTest) {
            final XPathNodeTest nodeTest = ((XPathNodeTest)element.getParent());
            if (nodeTest.isNameTest()) {
                final PrefixedName prefixedName = nodeTest.getQName();
                assert prefixedName != null;
                final String p = prefixedName.getPrefix();

                int endIndex = prefixedName.getLocalName().indexOf(CompletionLists.INTELLIJ_IDEA_RULEZ);
                if (endIndex != -1) {
                    prefix = prefixedName.getLocalName().substring(0, endIndex);
                } else if (p != null) {
                    endIndex = p.indexOf(CompletionLists.INTELLIJ_IDEA_RULEZ);
                    if (endIndex != -1) {
                        prefix = p.substring(0, endIndex);
                    }
                }
            }
        }

        return prefix;
    }
}
