/*
package org.testable.idea.provider;

import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testable.idea.utils.MockInvokeUtils;

*/
/**
 * @author jim
 *//*

public class FindTestMethodUsagesProvider implements FindUsagesProvider {
    @Override
    public boolean canFindUsagesFor(@NotNull PsiElement psiElement) {
        return MockInvokeUtils.has(psiElement);
    }

    @Override
    public @Nullable @NonNls String getHelpId(@NotNull PsiElement psiElement) {
        return null;
    }

    @Override
    public @Nls @NotNull String getType(@NotNull PsiElement element) {
        if (MockInvokeUtils.has(element)) {
            return "Test mock..";
        } else {
            return "";
        }
    }

    @Override
    public @Nls @NotNull String getDescriptiveName(@NotNull PsiElement element) {
        if (MockInvokeUtils.has(element)) {
            return "getDescriptiveName .......";
        } else {
            return "";
        }
    }

    @Override
    public @Nls @NotNull String getNodeText(@NotNull PsiElement element, boolean useFullName) {
        if (MockInvokeUtils.has(element)) {
            return "getNodeText .......";
        } else {
            return "";
        }
    }
}
*/
