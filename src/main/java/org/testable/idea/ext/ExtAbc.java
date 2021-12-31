package org.testable.idea.ext;

import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

/**
 * @author jim
 */
public class ExtAbc extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        /*try {
            Class.forName("com.intellij.psi.PsiLiteral");
            registrar.registerReferenceProvider(StandardPatterns.instanceOf(PsiLiteral.class), new PsiReferenceProvider() {
                @Override
                @NotNull
                public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
                    return new PsiReference[]{new OneWayPsiFileFromPsiLiteralReference((PsiLiteral) element)};
                }
            });
        } catch (ClassNotFoundException e) {
            //Ok, then. Some JetBrains platform IDE that has no Java support.
        }*/
    }
}
