/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.openapi.vfs.encoding;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.Element;

import java.nio.charset.Charset;
import java.util.Map;

/**
 * @author cdr
 */
public abstract class EncodingProjectManager extends EncodingManager implements ProjectComponent, PersistentStateComponent<Element> {
  public static EncodingProjectManager getInstance(Project project) {
    return project.getComponent(EncodingProjectManager.class);
  }

  public abstract Map<VirtualFile, Charset> getAllMappings();

  public abstract void setMapping(Map<VirtualFile, Charset> result);

}
