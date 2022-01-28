package org.testable.idea.utils;

import com.google.common.collect.Sets;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.testable.idea.helper.GenerationTestCaseHelper;

import java.util.*;
import java.util.stream.Collectors;

import static com.intellij.openapi.roots.JavaProjectRootsUtil.isForGeneratedSources;

/**
 * @author jim
 */
public interface ModuleUtils {
    static List<VirtualFile> computeTestRoots(@NotNull Module mainModule) {
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
                    .map(ModuleUtils::suitableTestSourceFolders)
                    .flatMap(Collection::stream)
                    .map(SourceFolder::getFile)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
    }

    static List<SourceFolder> suitableTestSourceFolders(@NotNull Module module) {
        return Arrays.stream(ModuleRootManager.getInstance(module).getContentEntries())
                .map(contentEntry -> contentEntry.getSourceFolders(JavaSourceRootType.TEST_SOURCE))
                .flatMap(Collection::stream)
                .filter(sourceFolder -> !isForGeneratedSources(sourceFolder))
                .collect(Collectors.toList());
    }
}
