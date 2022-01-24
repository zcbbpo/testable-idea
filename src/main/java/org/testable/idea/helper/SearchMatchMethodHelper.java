package org.testable.idea.helper;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiJvmMember;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.indexing.FileBasedIndex;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author jim
 */
public class SearchMatchMethodHelper {

    private static final SearchMatchMethodHelper INSTANCE = new SearchMatchMethodHelper();

    public static SearchMatchMethodHelper getInstance() {
        return INSTANCE;
    }

    public List<PsiMethodCallExpression> queryMockMethod(Project project, String targetClass, String targetMethod) {

        // JavaPsiFacade.getInstance(...).findClass()
        //MethodReferencesSearch
        Collection<VirtualFile> containingFiles = FileBasedIndex.getInstance()
                .getContainingFiles(
                        FileTypeIndex.NAME,
                        JavaFileType.INSTANCE,
                        GlobalSearchScope.projectScope(project));

        // PsiMethodCallExpression
        return containingFiles.stream()
                .map(v -> PsiManager.getInstance(project).findFile(v))
                .filter(Objects::nonNull)
                .filter(v -> v instanceof PsiJavaFile)
                .map(v -> ((PsiJavaFile) v).getClasses())
                .flatMap(Arrays::stream)
                .map(psiClass -> {
                    Collection<PsiMethodCallExpression> childrenOfAnyType = PsiTreeUtil.findChildrenOfAnyType(psiClass, PsiMethodCallExpression.class);
                    return childrenOfAnyType.stream()
                            .filter(v -> Optional.ofNullable(v.resolveMethod())
                                    .map(PsiJvmMember::getContainingClass)
                                    .map(q -> StringUtils.contains(q.getQualifiedName(), targetClass))
                                    .orElse(false))
                            .filter(v -> StringUtils.equals(v.getMethodExpression().getReferenceName(), targetMethod))
                            .collect(Collectors.toList());
                })
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

    }

}
