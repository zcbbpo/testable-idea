package org.testable.idea.ref;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.util.ui.EmptyIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testable.idea.helper.SearchMatchMethodHelper;

import java.util.List;

/**
 * @author jim
 */
public class MockInvokeReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {

    private final List<PsiMethodCallExpression> psiMethodCallExpressions;

    public MockInvokeReference(@NotNull PsiElement element, TextRange textRange, String targetClass, String targetMethod) {
        super(element, textRange);
        psiMethodCallExpressions = SearchMatchMethodHelper
                .getInstance()
                .queryMockMethod(myElement.getProject(), targetClass, targetMethod);
    }

    @Override
    public ResolveResult [] multiResolve(boolean incompleteCode) {
        return psiMethodCallExpressions.stream().map(PsiElementResolveResult::new).toArray(ResolveResult[]::new);
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        ResolveResult[] resolveResults = multiResolve(false);
        return resolveResults.length == 1 ? resolveResults[0].getElement() : null;
    }

    @Override
    public Object[] getVariants() {
        return psiMethodCallExpressions.stream()
                .map(v -> LookupElementBuilder
                        .create(v).withIcon(EmptyIcon.ICON_13)
                        .withTypeText(v.getContainingFile().getName()))
                .toArray(LookupElement[]::new);

    }
}
