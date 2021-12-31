package org.testable.idea.annotator;

import com.alibaba.testable.core.annotation.MockInvoke;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationParameterList;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameValuePair;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author jim
 */
public class MockMethodAnnotator implements Annotator {
    private static AtomicInteger atomicInteger = new AtomicInteger(0);



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
        TextRange prefixRange = TextRange.from(element.getTextRange().getStartOffset(), element.getTextRange().getLength());

        List<TextAttributesKey> allKeys = TextAttributesKey.getAllKeys();
        int andIncrement = atomicInteger.getAndIncrement();
        if (andIncrement < allKeys.size()) {
            // highlight "simple" prefix and ":" separator

            holder.newSilentAnnotation(HighlightSeverity.WARNING)
                    .range(prefixRange)
                    .textAttributes(allKeys.get(andIncrement))
                    .create();
        } else {
            // highlight "simple" prefix and ":" separator
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(prefixRange)
                    .textAttributes(DefaultLanguageHighlighterColors.KEYWORD)
                    .create();
        }



    }
}
