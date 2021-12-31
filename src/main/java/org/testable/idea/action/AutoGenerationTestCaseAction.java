package org.testable.idea.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.testable.idea.helper.GenerationTestCaseHelper;

/**
 * @author jimcao
 */
public class AutoGenerationTestCaseAction extends AnAction {

    private static final Logger LOG = Logger.getInstance(AutoGenerationTestCaseAction.class);


    @Override
    public void actionPerformed(AnActionEvent e) {

        PsiElement element = (PsiElement) e.getDataContext().getData("psi.Element");

        if (element == null) {
            return;
        }

        if (!(element instanceof PsiClass)) {
            return;
        }

        PsiClass psiClass = (PsiClass) element;
        GenerationTestCaseHelper.getInstance().generateTest(psiClass);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {

        LOG.debug("update....");
        final Presentation presentation = e.getPresentation();
        final Project project = e.getProject();

        if (project == null) {
            presentation.setEnabledAndVisible(false);
            return;
        }
        presentation.setEnabledAndVisible(true);
    }

}
