package org.testable.idea.utils;

import org.apache.commons.lang3.StringUtils;

/**
 * @author jim
 */
public interface ClassNameUtils {

    static String getRelativePathFromClassFullName(String classFullName) {
        return StringUtils.replace(classFullName, ".", "/");
    }

    static String getClassNameFromClassFullName(String classFullName) {
        String[] split = StringUtils.split(classFullName, ".");
        if (split.length < 1) {
            return null;
        }

        return split[split.length - 1];
    }

    static String getPackageNameFromClassFullName(String classFullName) {
        return StringUtils.left(classFullName, StringUtils.lastIndexOf(classFullName, "."));
    }

}
