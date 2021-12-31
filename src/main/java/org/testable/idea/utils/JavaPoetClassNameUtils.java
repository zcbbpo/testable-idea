package org.testable.idea.utils;

import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.squareup.javapoet.*;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

/**
 * @author jim
 */
public interface JavaPoetClassNameUtils {
    static TypeName guessType(PsiType psiType) {
        //TODO PsiEllipsisType, PsiWildcardType ...
        String type = psiType.getCanonicalText();
        if (psiType instanceof PsiEllipsisType && StringUtils.contains(type, "...")) {
            PsiEllipsisType psiEllipsisType = (PsiEllipsisType)psiType;
            return ArrayTypeName.of(guessType(psiEllipsisType.toArrayType().getDeepComponentType()));
        }
        if (psiType instanceof PsiWildcardType) {
            PsiWildcardType psiWildcardType = (PsiWildcardType) psiType;
            //psiWildcardType.
            if (psiWildcardType.isExtends()) {
                return WildcardTypeName.subtypeOf(guessType(psiWildcardType.getExtendsBound()));
            }
            if (psiWildcardType.isSuper()) {
                return WildcardTypeName.subtypeOf(guessType(psiWildcardType.getSuperBound()));
            }
            throw new RuntimeException("The PsiWildcardType parse err");
        }
        if (psiType instanceof PsiPrimitiveType) {
            PsiPrimitiveType psiPrimitiveType = (PsiPrimitiveType)psiType;
            return getPrimitiveTypeBySimpleName(psiPrimitiveType.getCanonicalText());
        }
        if (StringUtils.contains(type, "<") && psiType instanceof PsiClassReferenceType) {
            PsiClassReferenceType psiClassReferenceType = (PsiClassReferenceType) psiType;
            if (!psiClassReferenceType.hasParameters()) {
                return ClassName.bestGuess(type);
            }
            PsiType[] parameters = psiClassReferenceType.getParameters();
            String raw = Optional.ofNullable(psiClassReferenceType.resolveGenerics().getElement()).map(PsiClass::getQualifiedName).orElse(psiClassReferenceType.getCanonicalText());
            return ParameterizedTypeName.get(ClassName.bestGuess(raw), guessTypes(parameters));

        }
        return ClassName.bestGuess(type);
    }

    static TypeName[] guessTypes(PsiType[] psiTypes) {
        TypeName[] classNames = new TypeName[psiTypes.length];
        for (int i = 0; i < psiTypes.length; i++) {
            classNames[i] = guessType(psiTypes[i]);
        }
        return classNames;
    }

    static TypeName getPrimitiveTypeBySimpleName(String classSimpleName) {
        if (StringUtils.equals(classSimpleName, boolean.class.getSimpleName())) {
            return TypeName.BOOLEAN;
        }
        if (StringUtils.equals(classSimpleName, byte.class.getSimpleName())) {
            return TypeName.BYTE;
        }
        if (StringUtils.equals(classSimpleName, char.class.getSimpleName())) {
            return TypeName.CHAR;
        }
        if (StringUtils.equals(classSimpleName, short.class.getSimpleName())) {
            return TypeName.SHORT;
        }
        if (StringUtils.equals(classSimpleName, int.class.getSimpleName())) {
            return TypeName.INT;
        }
        if (StringUtils.equals(classSimpleName, long.class.getSimpleName())) {
            return TypeName.LONG;
        }
        if (StringUtils.equals(classSimpleName, float.class.getSimpleName())) {
            return TypeName.FLOAT;
        }
        if (StringUtils.equals(classSimpleName, double.class.getSimpleName())) {
            return TypeName.DOUBLE;
        }
        return null;
    }
}
