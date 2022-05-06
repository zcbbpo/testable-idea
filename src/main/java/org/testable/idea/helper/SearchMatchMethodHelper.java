package org.testable.idea.helper;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiJvmMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiMethodReferenceExpression;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AllClassesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author jim
 */
public class SearchMatchMethodHelper {
    private static final Logger LOG = Logger.getInstance(SearchMatchMethodHelper.class);

    private static final SearchMatchMethodHelper INSTANCE = new SearchMatchMethodHelper();

    public static SearchMatchMethodHelper getInstance() {
        return INSTANCE;
    }

    public List<PsiElement> queryMockMethod(Project project, PsiClass targetPsiClass, String targetMethod) {

        PsiClassType psiClassType = PsiElementFactory.getInstance(project).createType(targetPsiClass);
        String targetClass = targetPsiClass.getQualifiedName();
        return AllClassesSearch.search(GlobalSearchScope.projectScope(project), project)
                .findAll()
                .stream()
                .map(psiClass -> {
                    // Method call expression
                    Collection<PsiMethodCallExpression> methodCallExpressions = PsiTreeUtil.findChildrenOfAnyType(psiClass, PsiMethodCallExpression.class);
                    // Method reference expression
                    Collection<PsiMethodReferenceExpression> methodReferenceExpressions = PsiTreeUtil.findChildrenOfAnyType(psiClass, PsiMethodReferenceExpression.class);

                    List<PsiElement> methodReferenceList = methodReferenceExpressions.stream()
                            .filter(v -> Optional.ofNullable(v.getReferenceName())
                                    .map(z -> StringUtils.contains(z, targetMethod))
                                    .orElse(false))
                            .filter(v -> v.getContainingFile() instanceof PsiJavaFile)
                            .filter(v -> {
                                PsiElement resolve = v.resolve();
                                if (!(resolve instanceof PsiMethod)) {
                                    return false;
                                }
                                PsiMethod psiMethod = (PsiMethod) resolve;
                                PsiClass containingClass = psiMethod.getContainingClass();
                                if (containingClass == null) {
                                    return false;
                                }
                                if (StringUtils.contains(containingClass.getQualifiedName(), targetClass)) {
                                    return true;
                                }
                                return isAssignableFrom(PsiElementFactory.getInstance(project).createType(containingClass), psiClassType);
                            })
                            .collect(Collectors.toList());
                    List<PsiElement> methodCallList =methodCallExpressions.stream()
                            .filter(v -> StringUtils.equals(v.getMethodExpression().getReferenceName(), targetMethod))
                            .filter(v -> Optional.ofNullable(v.resolveMethod())
                                    .map(PsiJvmMember::getContainingClass)
                                    .map(q -> StringUtils.contains(q.getQualifiedName(), targetClass) || isAssignableFrom(PsiElementFactory.getInstance(project).createType(q), psiClassType))
                                    .orElse(false))
                            .collect(Collectors.toList());
                    methodReferenceList.addAll(methodCallList);
                    return methodReferenceList;
                })
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private boolean isAssignableFrom(PsiClassType p1, PsiClassType p2) {
        boolean assignableFrom = p1.isAssignableFrom(p2);
        String format = MessageFormat.format("P1: {0} assignableFrom p2: {1} result: {2}", p1.getClassName(), p2.getClassName(), assignableFrom);
        LOG.debug(format);
        return assignableFrom;
    }

}
