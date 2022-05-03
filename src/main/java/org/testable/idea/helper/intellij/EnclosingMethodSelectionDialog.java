package org.testable.idea.helper.intellij;


import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.PsiMethod;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.ui.MethodCellRenderer;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.JBInsets;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class EnclosingMethodSelectionDialog extends DialogWrapper {
    private final List<PsiMethod> myEnclosingMethods;
    private JList myEnclosingMethodsList;
    private final JCheckBox myCbReplaceInstanceOf = new JCheckBox(RefactoringBundle.message("use.interface.superclass.in.instanceof"));
    private static final String REFACTORING_NAME = RefactoringBundle.message("introduce.parameter.title");

    public EnclosingMethodSelectionDialog(Project project, List<PsiMethod> enclosingMethods) {
        super(project, true);
        this.myEnclosingMethods = enclosingMethods;
        this.setTitle(REFACTORING_NAME);
        this.init();
    }

    public PsiMethod getSelectedMethod() {
        return this.myEnclosingMethodsList != null ? (PsiMethod)this.myEnclosingMethodsList.getSelectedValue() : null;
    }

    @NotNull
    protected Action[] createActions() {
        Action[] var10000 = new Action[]{this.getOKAction(), this.getCancelAction()};
        if (var10000 == null) {
           throw new IllegalStateException("createActions exception:" + Thread.getAllStackTraces());
        }

        return var10000;
    }

    public JComponent getPreferredFocusedComponent() {
        return this.myEnclosingMethodsList;
    }

    protected JComponent createNorthPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbConstraints = new GridBagConstraints();
        gbConstraints.insets = JBInsets.create(4, 8);
        gbConstraints.weighty = 0.0D;
        gbConstraints.weightx = 1.0D;
        gbConstraints.gridy = 0;
        gbConstraints.gridwidth = 0;
        gbConstraints.gridheight = 1;
        gbConstraints.fill = 1;
        gbConstraints.anchor = 17;
        panel.add(new JLabel(RefactoringBundle.message("introduce.parameter.to.method")), gbConstraints);
        gbConstraints.weighty = 1.0D;
        this.myEnclosingMethodsList = new JBList(this.myEnclosingMethods.toArray());
        this.myEnclosingMethodsList.setCellRenderer(new MethodCellRenderer());
        this.myEnclosingMethodsList.getSelectionModel().setSelectionMode(0);
        int indexToSelect = 0;
        this.myEnclosingMethodsList.setSelectedIndex(indexToSelect);
        ++gbConstraints.gridy;
        panel.add(ScrollPaneFactory.createScrollPane(this.myEnclosingMethodsList), gbConstraints);
        return panel;
    }

    protected String getDimensionServiceKey() {
        return "#com.intellij.refactoring.introduceParameter.EnclosingMethodSelectonDialog";
    }

    protected void doOKAction() {
        if (this.isOKActionEnabled()) {
            super.doOKAction();
        }
    }

    protected JComponent createCenterPanel() {
        return null;
    }
}
