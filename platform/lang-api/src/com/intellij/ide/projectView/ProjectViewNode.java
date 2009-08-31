/*
 * Copyright 2000-2007 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.ide.projectView;

import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.problems.WolfTheProblemSolver;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A node in the project view tree.
 *
 * @see TreeStructureProvider#modify(com.intellij.ide.util.treeView.AbstractTreeNode, java.util.Collection, com.intellij.ide.projectView.ViewSettings)
 */

public abstract class ProjectViewNode <Value> extends AbstractTreeNode<Value> {

  protected static final Logger LOG = Logger.getInstance("#com.intellij.ide.projectView.ProjectViewNode");

  private final ViewSettings mySettings;

  /**
   * Creates an instance of the project view node.
   *
   * @param project      the project containing the node.
   * @param value        the object (for example, a PSI element) represented by the project view node
   * @param viewSettings the settings of the project view.
   */
  protected ProjectViewNode(Project project, Value value, ViewSettings viewSettings) {
    super(project, value);
    mySettings = viewSettings;
  }

  /**
   * Checks if this node or one of its children represents the specified virtual file.
   *
   * @param file the file to check for.
   * @return true if the file is found in the subtree, false otherwise.
   */
  public abstract boolean contains(@NotNull VirtualFile file);

  /**
   * Returns the virtual file represented by this node or one of its children.
   *
   * @return the virtual file instance, or null if the project view node doesn't represent a virtual file.
   */
  @Nullable
  public VirtualFile getVirtualFile() {
    return null;
  }

  public final ViewSettings getSettings() {
    return mySettings;
  }

  public static List<AbstractTreeNode> wrap(Collection objects,
                                            Project project,
                                            Class<? extends AbstractTreeNode> nodeClass,
                                            ViewSettings settings) {
    try {
      ArrayList<AbstractTreeNode> result = new ArrayList<AbstractTreeNode>();
      for (Object object : objects) {
        result.add(createTreeNode(nodeClass, project, object, settings));
      }
      return result;
    }
    catch (Exception e) {
      LOG.error(e);
      return new ArrayList<AbstractTreeNode>();
    }
  }

  public static AbstractTreeNode createTreeNode(Class<? extends AbstractTreeNode> nodeClass,
                                                Project project,
                                                Object value,
                                                ViewSettings settings) throws NoSuchMethodException,
                                                                              InstantiationException,
                                                                              IllegalAccessException,
                                                                              InvocationTargetException {
    Object[] parameters = new Object[]{project, value, settings};
    for (Constructor<? extends AbstractTreeNode> constructor : (Constructor<? extends AbstractTreeNode>[])nodeClass.getConstructors()) {
      if (constructor.getParameterTypes().length != 3) continue;
      try {
        return constructor.newInstance(parameters);
      }
      catch (InstantiationException e) {
      }
      catch (IllegalAccessException e) {
      }
      catch (IllegalArgumentException e) {
      }
      catch (InvocationTargetException e) {
      }
    }
    throw new InstantiationException("no constructor found in " + nodeClass);
  }

  public boolean someChildContainsFile(final VirtualFile file) {
    Collection<? extends AbstractTreeNode> kids = getChildren();
    for (final AbstractTreeNode kid : kids) {
      ProjectViewNode node = (ProjectViewNode)kid;
      if (node.contains(file)) return true;
    }
    return false;
  }

  protected boolean hasProblemFileBeneath() {
    return WolfTheProblemSolver.getInstance(getProject()).hasProblemFilesBeneath(new Condition<VirtualFile>() {
      public boolean value(final VirtualFile virtualFile) {
        return contains(virtualFile)
               // in case of flattened packages, when package node a.b.c contains error file, node a.b might not.
               && (getValue() instanceof PsiElement && PsiUtilBase.getVirtualFile((PsiElement)getValue()) == virtualFile ||
                   someChildContainsFile(virtualFile));
      }
    });
  }

  /**
   * Efficiently checks if there are nodes under the project view node which match the specified condition. Should
   * return true if it's not possible to perform the check efficiently (for example, if recursive traversal of
   * all child nodes is required to check the condition).
   *
   * @param condition the condition to check the nodes.
   */
  public boolean canHaveChildrenMatching(Condition<PsiFile> condition) {
    return true;
  }

  @Nullable
  public String getTitle() {
    return null;
  }

  public boolean isSortByFirstChild() {
    return false;
  }

  public int getTypeSortWeight(boolean sortByType) {
    return 0;
  }

  @Nullable
  public Comparable getTypeSortKey() {
    return null;
  }

  @Nullable
  public String getQualifiedNameSortKey() {
    return null;
  }

  public boolean shouldDrillDownOnEmptyElement() {
    return false;
  }

  public boolean validate() {
    update();
    return getValue() != null;
  }
}