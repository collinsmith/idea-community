package com.intellij.openapi.roots.impl;

import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ModuleFileIndex;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModuleFileIndexImpl implements ModuleFileIndex {
  private final Module myModule;
  private final FileTypeManager myFileTypeManager;
  private final DirectoryIndex myDirectoryIndex;
  private final ContentFilter myContentFilter;

  public ModuleFileIndexImpl(Module module, DirectoryIndex directoryIndex) {
    myModule = module;
    myDirectoryIndex = directoryIndex;
    myFileTypeManager = FileTypeManager.getInstance();

    myContentFilter = new ContentFilter();
  }

  public boolean iterateContent(@NotNull ContentIterator iterator) {
    VirtualFile[] contentRoots = ModuleRootManager.getInstance(myModule).getContentRoots();
    for (VirtualFile contentRoot : contentRoots) {
      VirtualFile parent = contentRoot.getParent();
      if (parent != null) {
        DirectoryInfo parentInfo = myDirectoryIndex.getInfoForDirectory(parent);
        if (parentInfo != null && myModule.equals(parentInfo.module)) continue; // inner content - skip it
      }

      boolean finished = FileIndexImplUtil.iterateRecursively(contentRoot, myContentFilter, iterator);
      if (!finished) return false;
    }

    return true;
  }

  public boolean iterateContentUnderDirectory(@NotNull VirtualFile dir, @NotNull ContentIterator iterator) {
    return FileIndexImplUtil.iterateRecursively(dir, myContentFilter, iterator);
  }

  public boolean isContentJavaSourceFile(@NotNull VirtualFile file) {
    return !file.isDirectory()
           && myFileTypeManager.getFileTypeByFile(file) == StdFileTypes.JAVA
           && !myFileTypeManager.isFileIgnored(file.getName())
           && isInSourceContent(file);
  }

  public boolean isInContent(@NotNull VirtualFile fileOrDir) {
    if (fileOrDir.isDirectory()) {
      DirectoryInfo info = myDirectoryIndex.getInfoForDirectory(fileOrDir);
      return info != null && myModule.equals(info.module);
    }
    else {
      VirtualFile parent = fileOrDir.getParent();
      return parent != null && isInContent(parent);
    }
  }

  public boolean isInSourceContent(@NotNull VirtualFile fileOrDir) {
    if (fileOrDir.isDirectory()) {
      DirectoryInfo info = myDirectoryIndex.getInfoForDirectory(fileOrDir);
      return info != null && info.isInModuleSource && myModule.equals(info.module);
    }
    else {
      VirtualFile parent = fileOrDir.getParent();
      return parent != null && isInSourceContent(parent);
    }
  }

  @NotNull
  public List<OrderEntry> getOrderEntriesForFile(@NotNull VirtualFile fileOrDir) {
    VirtualFile dir = fileOrDir.isDirectory() ? fileOrDir : fileOrDir.getParent();
    if (dir == null) return Collections.emptyList();
    final DirectoryInfo info = myDirectoryIndex.getInfoForDirectory(dir);
    if (info == null) return Collections.emptyList();
    return new ArrayList<OrderEntry>(info.getOrderEntries(myModule));
  }

  public OrderEntry getOrderEntryForFile(@NotNull VirtualFile fileOrDir) {
    VirtualFile dir = fileOrDir.isDirectory() ? fileOrDir : fileOrDir.getParent();
    if (dir == null) return null;
    final DirectoryInfo info = myDirectoryIndex.getInfoForDirectory(dir);
    if (info == null) return null;
    final List<OrderEntry> orderEntries = info.getOrderEntries();
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < orderEntries.size(); i++) {
      final OrderEntry orderEntry = orderEntries.get(i);
      if (orderEntry.getOwnerModule() == myModule) return orderEntry;
    }
    return null;
  }

  public boolean isInTestSourceContent(@NotNull VirtualFile fileOrDir) {
    if (fileOrDir.isDirectory()) {
      DirectoryInfo info = myDirectoryIndex.getInfoForDirectory(fileOrDir);
      return info != null && info.isInModuleSource && info.isTestSource && myModule.equals(info.module);
    }
    else {
      VirtualFile parent = fileOrDir.getParent();
      return parent != null && isInTestSourceContent(parent);
    }
  }

  private class ContentFilter implements VirtualFileFilter {
    public boolean accept(@NotNull VirtualFile file) {
      if (file.isDirectory()) {
        DirectoryInfo info = myDirectoryIndex.getInfoForDirectory(file);
        return info != null && myModule.equals(info.module);
      }
      else {
        return !myFileTypeManager.isFileIgnored(file.getName());
      }
    }
  }
}