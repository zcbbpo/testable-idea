package org.testable.idea.helper;

import com.alibaba.testable.core.annotation.MockInvoke;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PostprocessReformattingAspect;
import com.intellij.ui.GotItMessage;
import com.squareup.javapoet.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.testable.idea.utils.ClassNameUtils;
import org.testable.idea.utils.JavaPoetClassNameUtils;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static com.intellij.openapi.roots.JavaProjectRootsUtil.isForGeneratedSources;
import static io.vavr.API.*;

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
        Module srcModule = ModuleUtilCore.findModuleForPsiElement(bizService);
        if (srcModule == null) {
            return;
        }
        List<VirtualFile> testRootUrls = computeTestRoots(srcModule);

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

        PostprocessReformattingAspect
                .getInstance(openProject)
                .postponeFormattingInside(() -> {
                    try {
                        generationTestFile(bizService, testVirtualFile);
                        //openProject.getBaseDir().refresh(false,true);
                        /*testRootUrls.forEach(v -> {
                            v.refresh(false, true);
                        });*/
                        VfsUtil.markDirtyAndRefresh(false, true, true, ProjectRootManager.getInstance(openProject).getContentRoots());

                        //openProject.getProjectFile().refresh(false, true);
                        NotificationGroupManager.getInstance().getNotificationGroup("Custom Notification Group")
                                .createNotification(testJavaFile + "文件创建成功", NotificationType.INFORMATION)
                                .notify(openProject);
                    } catch (IOException e) {
                        // TODO notify to user
                        LOG.warn("Generation Testcase fail", e);
                    }
                });
    }

    public void generationTestFile(PsiClass bizService, VirtualFile testVirtualFile) throws IOException {
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
                        .addMethods(transformMethod(methods, simpleClassName))
                        .build()
                )
                .build();
        JavaFile javaFile = JavaFile.builder(packageName, testClassTypeSpec)
                // indentation with 4 spaces
                .indent("    ")
                .build();
        javaFile.writeToPath(Paths.get(testVirtualFile.getPath()));
    }

    public List<MethodSpec> transformMethod(PsiMethod[] methods, String targetClassName) {

        return Arrays.stream(methods)
                //.filter(v -> !v.getModifierList().hasModifierProperty(PsiModifier.NATIVE))
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
                .addAnnotation(mockInvokeAnnotation)
                .addParameters(transformParameter(method.getParameterList().getParameters()))
                .addCode(returnBody(returnType))
                .returns(returnType)
                .addModifiers(transformModifier(method.getModifierList()))
                .build();
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

    public static List<VirtualFile> computeTestRoots(@NotNull Module mainModule) {
        List<SourceFolder> sourceFolders = suitableTestSourceFolders(mainModule);
        if (CollectionUtils.isNotEmpty(sourceFolders)) {
            return sourceFolders.stream()
                    .map(SourceFolder::getFile)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } else {
            Set<Module> modules = Sets.newHashSet();
            ModuleUtilCore.collectModulesDependsOn(mainModule, modules);
            return modules.stream()
                    .map(GenerationTestCaseHelper::suitableTestSourceFolders)
                    .flatMap(Collection::stream)
                    .map(SourceFolder::getFile)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
    }

    private static List<SourceFolder> suitableTestSourceFolders(@NotNull Module module) {
        return Arrays.stream(ModuleRootManager.getInstance(module).getContentEntries())
                .map(contentEntry -> contentEntry.getSourceFolders(JavaSourceRootType.TEST_SOURCE))
                .flatMap(Collection::stream)
                .filter(sourceFolder -> !isForGeneratedSources(sourceFolder))
                .collect(Collectors.toList());
    }

}
