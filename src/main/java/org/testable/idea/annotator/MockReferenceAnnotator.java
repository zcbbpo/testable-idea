package org.testable.idea.annotator;

import com.alibaba.testable.core.annotation.MockInvoke;
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
public class MockReferenceAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (! (element instanceof PsiAnnotation)) {
            return;
        }
        PsiAnnotation psiAnnotation = (PsiAnnotation)element;
        String qualifiedName = psiAnnotation.getQualifiedName();
        if (!StringUtils.equals(MockInvoke.class.getCanonicalName(), qualifiedName)) {
            return;
        }

        PsiAnnotationParameterList parameterList = psiAnnotation.getParameterList();
        PsiNameValuePair[] attributes = parameterList.getAttributes();
        PsiNameValuePair targetMethod = Arrays.stream(attributes)
                .filter(v -> v.getNameIdentifier() != null)
                .filter(v -> StringUtils.equals(v.getNameIdentifier().getText(), "targetMethod"))
                .findFirst()
                .orElse(null);
        if (targetMethod == null) {
            return;
        }

        // Define the text ranges (start is inclusive, end is exclusive)
        // "simple:key"
        //  01234567890
        PsiAnnotationMemberValue targetValue = targetMethod.getValue();
        if (targetValue == null) {
            return;
        }
        if (! (targetValue instanceof PsiLiteralExpression)) {
            return;
        }
        PsiLiteralExpression targetLiteral = (PsiLiteralExpression) targetValue;
        TextRange prefixRange = TextRange.from(targetLiteral.getTextRange().getStartOffset(), targetLiteral.getTextRange().getLength());

        TextAttributesKey textAttributesKey = TextAttributesKey.createTextAttributesKey("MY_VAR", DefaultLanguageHighlighterColors.INSTANCE_METHOD);

        textAttributesKey.getDefaultAttributes().setBackgroundColor(new JBColor(new Color(237, 252, 237), new Color(54,65,53)));

        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(prefixRange)
                .textAttributes(textAttributesKey)
                .create();
    }
}
