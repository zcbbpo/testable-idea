package org.testable.idea.annotator;

import com.alibaba.testable.core.annotation.MockInvoke;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.ui.JBColor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Arrays;

/**
 * @author jim
 */
public class KeywordAnnotator implements Annotator {
    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!(element instanceof PsiMethod)) {
            return;
        }

        PsiMethod psiMethod = (PsiMethod) element;

        PsiAnnotation[] annotations = psiMethod.getAnnotations();
        Arrays.stream(annotations)
                .filter(this::isMockInvokeAnnotation)
                .findFirst()
                .ifPresent(annotation -> {
                    addStyleForAnnotation(annotation, holder);
                    addStyleForMethodName(psiMethod, holder);
                });
    }

    private boolean isMockInvokeAnnotation(PsiAnnotation annotation) {
        String qualifiedName = annotation.getQualifiedName();
        return StringUtils.equals(MockInvoke.class.getCanonicalName(), qualifiedName);
    }

    private void addStyleForMethodName(PsiMethod psiMethod, AnnotationHolder holder) {
        PsiIdentifier nameIdentifier = psiMethod.getNameIdentifier();
        if (nameIdentifier == null) {
            return;
        }
        setStyle(nameIdentifier, holder);
    }

    private void addStyleForAnnotation(PsiAnnotation annotation, AnnotationHolder holder) {
        PsiAnnotationParameterList parameterList = annotation.getParameterList();
        PsiNameValuePair[] attributes = parameterList.getAttributes();
        PsiNameValuePair targetMethod = Arrays.stream(attributes)
                .filter(v -> v.getNameIdentifier() != null)
                .filter(v -> StringUtils.equals(v.getNameIdentifier().getText(), "targetMethod"))
                .findFirst()
                .orElse(null);
        if (targetMethod == null) {
            return;
        }

        PsiAnnotationMemberValue targetValue = targetMethod.getValue();
        if (targetValue == null) {
            return;
        }
        if (! (targetValue instanceof PsiLiteralExpression)) {
            return;
        }
        PsiLiteralExpression targetLiteral = (PsiLiteralExpression) targetValue;
        setStyle(targetLiteral, holder);
    }

    private void setStyle(PsiElement psiElement, AnnotationHolder holder) {
        if (psiElement == null) {
            return;
        }
        TextRange prefixRange = TextRange.from(psiElement.getTextRange().getStartOffset(), psiElement.getTextRange().getLength());

        TextAttributesKey textAttributesKey = TextAttributesKey.createTextAttributesKey("MY_VAR", DefaultLanguageHighlighterColors.INSTANCE_METHOD);

        textAttributesKey.getDefaultAttributes().setBackgroundColor(new JBColor(new Color(237, 252, 237), new Color(54,65,53)));

        Annotation annotation = holder.createAnnotation(HighlightSeverity.INFORMATION, prefixRange, null);
        annotation.setTextAttributes(textAttributesKey);
        /*holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(prefixRange)
                .textAttributes(textAttributesKey)
                .create();*/
    }
}