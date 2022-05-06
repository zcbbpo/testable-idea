package org.testable.idea.helper;

import com.alibaba.testable.core.annotation.MockInvoke;
import com.google.common.collect.Lists;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.PsiTypeParameterList;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.testable.idea.utils.ClassNameUtils;
import org.testable.idea.utils.JavaPoetClassNameUtils;
import org.testable.idea.utils.ModuleUtils;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

/**
 * @author jim
 */
public class GenerationTestCaseHelper {

    private static final Logger LOG = Logger.getInstance(GenerationTestCaseHelper.class);
    private static final GenerationTestCaseHelper INSTANCE = new GenerationTestCaseHelper();

    public static GenerationTestCaseHelper getInstance() {
        return INSTANCE;
    }

    public void generateTest(PsiClass bizService) {
        generateTest(bizService, Lists.newArrayList());
    }

    public void generateTest(PsiClass bizService, List<PsiMethod> methods) {
        Module srcModule = ModuleUtilCore.findModuleForPsiElement(bizService);
        if (srcModule == null) {
            return;
        }
        List<VirtualFile> testRootUrls = ModuleUtils.computeTestRoots(srcModule);

        if (CollectionUtils.isEmpty(testRootUrls)) {
            return;
        }
        VirtualFile testVirtualFile = testRootUrls.get(0);
        Project openProject = ProjectManager.getInstance().getOpenProjects()[0];

        String testJavaFile = bizService.getQualifiedName() + "Test";
        String mockJavaFile = bizService.getQualifiedName() + "Mock";
        Path testJavaFilePath = Paths.get(testVirtualFile.getPath()).resolve(ClassNameUtils.getRelativePathFromClassFullName(testJavaFile));
        Path mockJavaFilePath = Paths.get(testVirtualFile.getPath()).resolve(ClassNameUtils.getRelativePathFromClassFullName(mockJavaFile));
        if (Files.exists(testJavaFilePath) || Files.exists(mockJavaFilePath)) {
            String msg = MessageFormat.format("The file was exist.. {0}", bizService.getQualifiedName());
            LOG.info(msg);
            return;
        }

        AtomicReference<String> tip = new AtomicReference<>("");
        WriteCommandAction.runWriteCommandAction(openProject, () -> {
            try {
                Path testFilePath = generationTestFile(bizService, testVirtualFile, methods);
                VfsUtil.markDirtyAndRefresh(false, true, true, ProjectRootManager.getInstance(openProject).getContentRoots());
                VirtualFile virtualFile = VfsUtil.findFile(testFilePath, true);
                if (virtualFile != null) {
                    FileEditorManager.getInstance(openProject).openTextEditor(new OpenFileDescriptor(openProject, virtualFile), true);
                }
                tip.set(testJavaFile + " File created success");
                //EditSourceUtil.navigate();
            } catch (IOException e) {
                tip.set(testJavaFile + " File created fail");
                LOG.warn("Generation Testcase fail", e);
            }
        });
        Messages.showInfoMessage(openProject, tip.get(), "Create Test Class");
    }

    public Path generationTestFile(PsiClass bizService, VirtualFile testVirtualFile, List<PsiMethod> selectMethodList) throws IOException {
        String qualifiedName = bizService.getQualifiedName();
        String simpleClassName = ClassNameUtils.getClassNameFromClassFullName(qualifiedName);
        PsiMethod[] methods = bizService.getMethods();

        String packageName = ClassNameUtils.getPackageNameFromClassFullName(qualifiedName);
        packageName = packageName == null ? "" : packageName;
        String classSimpleName = String.format("%sTest", simpleClassName);
        TypeSpec testClassTypeSpec = TypeSpec.classBuilder(classSimpleName)
                .addModifiers(Modifier.PUBLIC)
                .addType(TypeSpec.classBuilder("Mock")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        // not required generation the method ...
                        .addMethods(transformMethod(selectMethodList.toArray(new PsiMethod[0]), simpleClassName))
                        .build()
                )
                .build();
        JavaFile javaFile = JavaFile.builder(packageName, testClassTypeSpec)
                // indentation with 4 spaces
                .indent("    ")
                .build();
        return javaFile.writeToPath(Paths.get(testVirtualFile.getPath()));
    }

    public List<MethodSpec> transformMethod(PsiMethod[] methods, String targetClassName) {

        return Arrays.stream(methods)
                .map(v -> transformMethod(v, targetClassName))
                .collect(Collectors.toList());
    }

    public MethodSpec transformMethod(PsiMethod method, String targetClassName) {
        TypeName returnType = Optional.ofNullable(method.getReturnType())
                .map(JavaPoetClassNameUtils::guessType)
                .orElse(TypeName.VOID);
        String packageName = ClassNameUtils.getPackageNameFromClassFullName(targetClassName);
        String classSimpleName = ClassNameUtils.getClassNameFromClassFullName(targetClassName);
        ClassName targetClass = ClassName.get(packageName, classSimpleName);
        AnnotationSpec mockInvokeAnnotation = AnnotationSpec.builder(MockInvoke.class)
                .addMember("targetClass", CodeBlock.builder().add("$T.class", targetClass).build())
                .addMember("targetMethod", CodeBlock.builder().add("$S", method.getName()).build())
                .build();
        return MethodSpec.methodBuilder(method.getName())
                .addTypeVariables(transformTypeVariables(method.getTypeParameterList()))
                .addAnnotation(mockInvokeAnnotation)
                .addParameters(transformParameter(method.getParameterList().getParameters()))
                .addCode(returnBody(returnType))
                .returns(returnType)
                .addModifiers(transformModifier(method.getModifierList()))
                .build();
    }

    private List<TypeVariableName> transformTypeVariables(PsiTypeParameterList typeParameterList) {
        if (typeParameterList == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(typeParameterList.getTypeParameters())
                .filter(v -> StringUtils.isNotBlank(v.getName()))
                .map(v -> Optional.of(v)
                        .map(PsiTypeParameter::getExtendsList)
                        .map(PsiReferenceList::getReferencedTypes)
                        .map(z -> TypeVariableName.get(v.getName(), JavaPoetClassNameUtils.guessTypes(z)))
                        .orElse(TypeVariableName.get(v.getName())))
                .collect(Collectors.toList());
    }

    private CodeBlock returnBody(TypeName returnType) {
        if (returnType == TypeName.VOID) {
            return CodeBlock.of("");
        }

        if (!returnType.isPrimitive()) {
            return CodeBlock.of("return null;");
        }

        return Match(returnType).of(
                Case($(v -> v == TypeName.BOOLEAN), v -> CodeBlock.of("return false;")),
                Case($(v -> v == TypeName.BYTE), v -> CodeBlock.of("return (byte)0;")),
                Case($(v -> v == TypeName.SHORT), v -> CodeBlock.of("return (short)0;")),
                Case($(v -> v == TypeName.INT), v -> CodeBlock.of("return 0;")),
                Case($(v -> v == TypeName.LONG), v -> CodeBlock.of("return 0L;")),
                Case($(v -> v == TypeName.CHAR), v -> CodeBlock.of("return (char)0;")),
                Case($(v -> v == TypeName.FLOAT), v -> CodeBlock.of("return 0.0f;")),
                Case($(v -> v == TypeName.DOUBLE), v -> CodeBlock.of("return 0.0d;")),
                Case($(), v -> null)
        );
    }

    public List<Modifier> transformModifier(PsiModifierList psiModifierList) {
        if (psiModifierList == null) {
            return Collections.emptyList();
        }
        List<Modifier> modifiers = Lists.newArrayList();
        if (psiModifierList.hasModifierProperty(PsiModifier.PUBLIC)) {
            modifiers.add(Modifier.PUBLIC);
        }
        if (psiModifierList.hasModifierProperty(PsiModifier.PROTECTED)) {
            modifiers.add(Modifier.PROTECTED);
        }
        if (psiModifierList.hasModifierProperty(PsiModifier.PRIVATE)) {
            modifiers.add(Modifier.PRIVATE);
        }
        if (psiModifierList.hasModifierProperty(PsiModifier.STATIC)) {
            modifiers.add(Modifier.STATIC);
        }
        if (psiModifierList.hasModifierProperty(PsiModifier.SYNCHRONIZED)) {
            modifiers.add(Modifier.SYNCHRONIZED);
        }
        if (psiModifierList.hasModifierProperty(PsiModifier.FINAL)) {
            modifiers.add(Modifier.FINAL);
        }
        return modifiers;
    }

    private List<ParameterSpec> transformParameter(PsiParameter[] parameters) {
        if (ArrayUtils.isEmpty(parameters)) {
            return Collections.emptyList();
        }

        return Arrays.stream(parameters)
                .map(v -> ParameterSpec.builder(JavaPoetClassNameUtils.guessType(v.getType()), v.getName()).build())
                .collect(Collectors.toList());
    }

}
