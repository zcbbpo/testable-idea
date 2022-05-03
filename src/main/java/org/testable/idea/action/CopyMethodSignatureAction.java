package org.testable.idea.action;

import com.intellij.codeInsight.editorActions.*;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.jetbrains.annotations.NotNull;
import org.testable.idea.helper.GenerationTestCaseHelper;

import java.util.List;

/**
 * @author jimcao
 */
public class CopyMethodSignatureAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(CopyMethodSignatureAction.class);

    @Override
    public void actionPerformed(AnActionEvent e) {
        final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        final Project project = e.getRequiredData(CommonDataKeys.PROJECT);

        PsiElement element = (PsiElement) e.getDataContext().getData("psi.Element");
        Messages.showInfoMessage(project, "get element", "get element");
        if (!(element instanceof PsiMethod)) {
            return;
        }
        PsiMethod method = (PsiMethod) element;
        if (method.getContainingClass() == null) {
            return;
        }

        PsiFile psiFile = method.getContainingClass().getContainingFile();
        if (!(psiFile instanceof PsiJavaFile)) {
            return;
        }


        MethodSpec methodSpec = GenerationTestCaseHelper.getInstance().transformMethod(method, method.getContainingClass().getQualifiedName());
        PsiJavaFile psiJavaFile = (PsiJavaFile) psiFile;

        JavaFile javaFile = JavaFile.builder(psiJavaFile.getPackageName(), TypeSpec.classBuilder("Temp").addMethod(methodSpec).build())
                // indentation with 4 spaces
                .indent("    ")
                .build();
        copyText2Clipboard(editor, project, javaFile.toString());
        Messages.showMessageDialog(project, method.getName() + " mock-method signature copied success", "Copy Success", null);
//        NotificationGroupManager.getInstance().getNotificationGroup("Custom Notification Group")
//                .createNotification(method.getName() + " mock-method signature copied success", NotificationType.INFORMATION)
//                .notify(project);
    }

    private static void copyText2Clipboard(Editor editor, Project project, String text) {
        PsiFileFactory fileFactory = PsiFileFactory.getInstance(project);
        PsiJavaFile psiFile = (PsiJavaFile) fileFactory.createFileFromText("Temp", JavaLanguage.INSTANCE, text);
        PsiMethod method = psiFile.getClasses()[0].getMethods()[0];

        JavaCopyPasteReferenceProcessor javaCopyPasteReferenceProcessor = new JavaCopyPasteReferenceProcessor();

        List<ReferenceTransferableData> referenceTransferableData =
                javaCopyPasteReferenceProcessor.collectTransferableData(psiFile, editor, new int[]{method.getTextRange().getStartOffset()}, new int[]{method.getTextRange().getEndOffset()});

        TextBlockTransferable contents = new TextBlockTransferable(method.getText(), referenceTransferableData, null);
        LOG.info("copy to pasteBoard");
        CopyPasteManager.getInstance().setContents(contents);

    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        final Editor editor = e.getData(CommonDataKeys.EDITOR);

        // Set visibility only in case of existing project and editor and if a selection exists
        e.getPresentation().setEnabledAndVisible( project != null
                && editor != null
                && editor.getSelectionModel().hasSelection() );
    }

}
