package org.testable.idea.action;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.ClassUtil;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.testable.idea.helper.GenerationTestCaseHelper;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.intellij.openapi.module.ModuleUtilCore.findModuleForPsiElement;
import static com.intellij.testIntegration.createTest.CreateTestUtils.computeTestRoots;

/**
 * @author jim
 */
public class GenerateMethodToTestCaseAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(GenerateMethodToTestCaseAction.class);


    @Override
    public void actionPerformed(AnActionEvent e) {
        final Project project = e.getRequiredData(CommonDataKeys.PROJECT);

        PsiElement element = (PsiElement) e.getDataContext().getData("psi.Element");

        if (!(element instanceof PsiMethod)) {
            return;
        }
        PsiMethod psiMethod = (PsiMethod) element;
        PsiClass containingClass = psiMethod.getContainingClass();
        if (containingClass == null) {
            return;
        }

        Module srcModule = findModuleForPsiElement(element);
        if (srcModule == null) {
            return;
        }
        List<VirtualFile> testRootUrls = computeTestRoots(srcModule);

        String testJavaFile = containingClass.getQualifiedName() + "Test";
        String mockJavaFile = containingClass.getQualifiedName() + "Mock";

        PsiClass testClass = JavaPsiFacade.getInstance(project).findClass(testJavaFile, GlobalSearchScope.projectScope(project));
        if (testClass == null) {
            testClass = JavaPsiFacade.getInstance(project).findClass(mockJavaFile, GlobalSearchScope.projectScope(project));
        }

        if (testClass == null) {
            GenerationTestCaseHelper.getInstance().generateTest(containingClass, Lists.newArrayList(psiMethod));
            return;
        }

        Boolean inTestFolder = Optional.ofNullable(testClass.getContainingFile())
                .map(PsiFile::getVirtualFile)
                .map(v -> testRootUrls.stream().allMatch(z -> isChild(z, v)))
                .orElse(false);

        if (!inTestFolder) {
            LOG.debug("The test class not in test folder");
            return;
        }

        String asmMethodSignature = ClassUtil.getAsmMethodSignature(psiMethod);
        PsiMethod[] mockMethods = Arrays.stream(testClass.getInnerClasses())
                .filter(v -> StringUtils.equals(v.getName(), "Mock"))
                .findFirst()
                .map(PsiClass::getMethods)
                .orElse(null);

        boolean hasMockClass = mockMethods != null;
        if (hasMockClass) {
            boolean exist = Arrays.stream(mockMethods)
                    .filter(v -> StringUtils.equals(v.getName(), psiMethod.getName()))
                    .anyMatch(v -> StringUtils.equals(ClassUtil.getAsmMethodSignature(v), asmMethodSignature));
            if (exist) {
                LOG.debug("The test class mock method was exist");
                return;
            }
        }

        final PsiClass finalTestClass = testClass;

        WriteCommandAction.runWriteCommandAction(project, () -> {
            MethodSpec methodSpec = GenerationTestCaseHelper.getInstance().transformMethod(psiMethod, psiMethod.getContainingClass().getQualifiedName());

            if (hasMockClass) {
                PsiElement insert = PsiElementFactory.getInstance(project).createMethodFromText(methodSpec.toString(), element);
                Arrays.stream(finalTestClass.getInnerClasses())
                        .filter(v -> StringUtils.equals(v.getName(), "Mock"))
                        .findFirst()
                        .ifPresent(v -> v.add(insert));
            } else {
                PsiElement insert =  PsiElementFactory.getInstance(project).createClassFromText(TypeSpec.classBuilder("Mock").addMethod(methodSpec).build().toString(), element);
                finalTestClass.add(insert);
            }
            JavaCodeStyleManager.getInstance(project).shortenClassReferences(finalTestClass);
            LOG.debug("The test class mock created success");
        });

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

    private boolean isChild(VirtualFile parent, VirtualFile child) {
        return Paths.get(child.getPath()).startsWith(Paths.get(parent.getPath()));
    }

    private List<String> getImportFromJavaFile(JavaFile javaFile) {
        String s = javaFile.toString();
        return Splitter.on("\n").splitToList(s).stream()
                .filter(v -> StringUtils.startsWith(v, "import "))
                .filter(v -> StringUtils.endsWith(v, ";"))
                .map(v -> StringUtils.remove(v, "import "))
                .map(v -> StringUtils.remove(v, ";"))
                .map(v -> StringUtils.remove(v, " "))
                .collect(Collectors.toList());
    }
}
