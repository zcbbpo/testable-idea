package org.testable.idea.helper;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiJvmMember;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AllClassesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
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

        return AllClassesSearch.search(GlobalSearchScope.projectScope(project), project)
                .allowParallelProcessing()
                .findAll()
                .stream()
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
