package org.testable.idea.s;

import com.alibaba.testable.core.annotation.MockInvoke;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * @author jim
 */
public class SimpleReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(PsiLiteralExpression.class),
                new PsiReferenceProvider() {
                    @Override
                    public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                                           @NotNull ProcessingContext context) {

                        PsiLiteralExpression psiLiteralExpression = (PsiLiteralExpression) element;

                        PsiElement pt = psiLiteralExpression.getParent();
                        if (pt == null) {
                            return PsiReference.EMPTY_ARRAY;
                        }
                        if ( !(pt instanceof PsiNameValuePair)) {
                            return PsiReference.EMPTY_ARRAY;
                        }

                        PsiNameValuePair targetMethod = (PsiNameValuePair) pt;

                        if (targetMethod.getNameIdentifier() == null) {
                            return PsiReference.EMPTY_ARRAY;
                        }
                        if (!StringUtils.equals(targetMethod.getNameIdentifier().getText(), "targetMethod")) {
                            return PsiReference.EMPTY_ARRAY;
                        }
                        PsiElement parent = targetMethod.getParent();

                        if (parent == null) {
                            return PsiReference.EMPTY_ARRAY;
                        }

                        if (! (parent instanceof PsiAnnotationParameterList)) {
                            return PsiReference.EMPTY_ARRAY;
                        }

                        PsiAnnotationParameterList parameterList = (PsiAnnotationParameterList)parent;

                        /*String qualifiedName = psiAnnotation.getQualifiedName();
                        if (!StringUtils.equals(MockInvoke.class.getCanonicalName(), qualifiedName)) {
                            return PsiReference.EMPTY_ARRAY;
                        }*/

                        PsiNameValuePair[] attributes = parameterList.getAttributes();
                        // TODO
                        PsiNameValuePair targetClass = Arrays.stream(attributes)
                                .filter(v -> v.getNameIdentifier() != null)
                                .filter(v -> StringUtils.equals(v.getNameIdentifier().getText(), "targetClass"))
                                .findFirst()
                                .orElse(null);

                        if (targetClass == null) {
                            return PsiReference.EMPTY_ARRAY;
                        }

                        PsiAnnotationMemberValue classValue = targetClass.getValue();
                        if (classValue == null) {
                            return PsiReference.EMPTY_ARRAY;
                        }

                        if (! (classValue instanceof PsiClassObjectAccessExpression)) {
                            return PsiReference.EMPTY_ARRAY;
                        }

                        PsiClassObjectAccessExpression psiClassObjectAccessExpression = (PsiClassObjectAccessExpression)classValue;

                        //return PsiReference.EMPTY_ARRAY;
                        String classLiteralValue = psiClassObjectAccessExpression.getOperand().getText();
                        String methodLiteralValue = targetMethod.getLiteralValue();

                        if (StringUtils.isBlank(classLiteralValue) || StringUtils.isBlank(methodLiteralValue)) {
                            return PsiReference.EMPTY_ARRAY;
                        }

                        TextRange property = new TextRange(1, methodLiteralValue.length() + 1);
                        return new PsiReference[]{new SimpleReference(element, property, classLiteralValue, methodLiteralValue)};
                    }
                });
    }
}
