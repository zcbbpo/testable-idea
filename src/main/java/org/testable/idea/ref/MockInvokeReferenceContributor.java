package org.testable.idea.ref;

import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author jim
 */
public class MockInvokeReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(PsiLiteralExpression.class),
                new PsiReferenceProvider() {
                    @Override
                    public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
                                                                 @NotNull ProcessingContext context) {
                        PsiLiteralExpression psiLiteralExpression = (PsiLiteralExpression) element;

                        PsiElement pt = psiLiteralExpression.getParent();
                        if (pt == null) {
                            return PsiReference.EMPTY_ARRAY;
                        }
                        if ( !(pt instanceof PsiNameValuePair)) {
                            return PsiReference.EMPTY_ARRAY;
                        }

                        PsiNameValuePair targetMethodPair = (PsiNameValuePair) pt;

                        if (targetMethodPair.getNameIdentifier() == null) {
                            return PsiReference.EMPTY_ARRAY;
                        }
                        if (!StringUtils.equals(targetMethodPair.getNameIdentifier().getText(), "targetMethod")) {
                            return PsiReference.EMPTY_ARRAY;
                        }

                        PsiElement parent = targetMethodPair.getParent();

                        if (parent == null) {
                            return PsiReference.EMPTY_ARRAY;
                        }

                        if (! (parent instanceof PsiAnnotationParameterList)) {
                            return PsiReference.EMPTY_ARRAY;
                        }

                        PsiAnnotationParameterList parameterList = (PsiAnnotationParameterList)parent;

                        PsiNameValuePair[] attributes = parameterList.getAttributes();

                        String targetClassName = getTargetClass(attributes);

                        if (StringUtils.isBlank(targetClassName)) {
                            PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
                            targetClassName = Optional.ofNullable(psiClass).map(PsiClass::getQualifiedName).orElse(null);
                        }

                        if (targetClassName == null) {
                            return PsiReference.EMPTY_ARRAY;
                        }

                        String targetMethodName = targetMethodPair.getLiteralValue();

                        if (StringUtils.isBlank(targetMethodName)) {
                            return PsiReference.EMPTY_ARRAY;
                        }

                        TextRange property = new TextRange(1, targetMethodName.length() + 1);
                        return new PsiReference[]{new MockInvokeReference(element, property, targetClassName, targetMethodName)};
                    }
                });
    }

    private String getTargetClass(PsiNameValuePair[] attributes) {
        return getTarget(attributes, "targetClass");
    }

    private String getTargetMethod(PsiNameValuePair[] attributes) {
        return getTarget(attributes, "targetMethod");
    }

    private String getTarget(PsiNameValuePair[] attributes, String target) {
        if (ArrayUtils.isEmpty(attributes)) {
            return null;
        }

        return Arrays.stream(attributes)
                .filter(v -> v.getNameIdentifier() != null)
                .filter(v -> StringUtils.equals(v.getNameIdentifier().getText(), target))
                .findFirst()
                .map(PsiNameValuePair::getValue)
                .filter(v -> v instanceof PsiClassObjectAccessExpression)
                .map(v -> (PsiClassObjectAccessExpression) v)
                .map(PsiClassObjectAccessExpression::getOperand)
                .map(PsiTypeElement::getText)
                .orElse(null);
    }
}
