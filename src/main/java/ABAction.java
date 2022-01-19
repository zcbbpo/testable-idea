import com.intellij.lang.annotation.Annotation;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import org.jetbrains.annotations.NotNull;
import org.testable.idea.action.AutoGenerationTestCaseAction;

import java.awt.*;
import java.awt.datatransfer.Clipboard;

public class ABAction extends AnAction {

    private static final Logger LOG = Logger.getInstance(ABAction.class);


    @Override
    public void actionPerformed(AnActionEvent e) {
        Clipboard systemClipboard = Toolkit.getDefaultToolkit()
                .getSystemClipboard();
        System.out.println(systemClipboard);
        System.out.println(systemClipboard);

        // TODO: insert action logic here
       /* Project openProject = ProjectManager.getInstance().getOpenProjects()[0];
        Editor editor = FileEditorManager.getInstance(openProject).getSelectedTextEditor();
        Document document = editor.getDocument();
        FileEditor[] editors = FileEditorManager.getInstance(openProject).getAllEditors();
        FileEditor fileEditor = editors[0];
        VirtualFile file = fileEditor.getFile();*/
        /*PsiElement element = (PsiElement) e.getDataContext().getData("psi.Element");

        if (element == null) {
            return;
        }

        if (!(element instanceof PsiClass)) {
            return;
        }

        PsiClass psiClass = (PsiClass) element;

        PsiField[] fields = psiClass.getFields();
        PsiField field = fields[0];
        PsiAnnotation[] annotations = field.getAnnotations();
        PsiAnnotation annotation = annotations[0];*/
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
