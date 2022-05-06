package org.testable.idea.ref;

import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotationParameterList;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import io.vavr.Tuple;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * @author jim
 */
public class MockInvokeReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(PsiLiteralExpression.class),
                new PsiReferenceProvider() {
                    @NotNull
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

                        PsiClass targetPsiClass = getTargetClass(attributes);
                        if (targetPsiClass == null) {
                            targetPsiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
                        }

                        if (targetPsiClass == null) {
                            return PsiReference.EMPTY_ARRAY;
                        }

                        String targetMethodName = targetMethodPair.getLiteralValue();

                        if (StringUtils.isBlank(targetMethodName)) {
                            return PsiReference.EMPTY_ARRAY;
                        }

                        TextRange property = new TextRange(1, targetMethodName.length() + 1);
                        return new PsiReference[]{new MockInvokeReference(element, property, targetPsiClass, targetMethodName)};
                    }
                });
    }

    private PsiClass getTargetClass(PsiNameValuePair[] attributes) {
        return getTarget(attributes, "targetClass");
    }

    private PsiClass getTarget(PsiNameValuePair[] attributes, String target) {
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
                .map(PsiElement::getFirstChild)
                .filter(v -> v instanceof PsiJavaCodeReferenceElement)
                .map(v -> (PsiJavaCodeReferenceElement) v)
                .map(v -> Tuple.of(v.getProject(), v.getCanonicalText()))
                .map(v -> JavaPsiFacade.getInstance(v._1).findClass(v._2, GlobalSearchScope.projectScope(v._1)))
                .orElse(null);
    }
}
