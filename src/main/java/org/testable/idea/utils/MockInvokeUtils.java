package org.testable.idea.utils;

import com.alibaba.testable.core.annotation.MockInvoke;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationParameterList;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameValuePair;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * @author jim
 */
public interface MockInvokeUtils {
    static boolean has(@NotNull PsiElement element) {
        if (! (element instanceof PsiAnnotation)) {
            return false;
        }
        PsiAnnotation psiAnnotation = (PsiAnnotation)element;
        String qualifiedName = psiAnnotation.getQualifiedName();
        if (!StringUtils.equals(MockInvoke.class.getCanonicalName(), qualifiedName)) {
            return false;
        }

        PsiAnnotationParameterList parameterList = psiAnnotation.getParameterList();
        PsiNameValuePair[] attributes = parameterList.getAttributes();
        PsiNameValuePair targetMethod = Arrays.stream(attributes)
                .filter(v -> v.getNameIdentifier() != null)
                .filter(v -> StringUtils.equals(v.getNameIdentifier().getText(), "targetMethod"))
                .findFirst()
                .orElse(null);
        //noinspection RedundantIfStatement
        if (targetMethod == null) {
            return false;
        }

        return true;
    }
}
