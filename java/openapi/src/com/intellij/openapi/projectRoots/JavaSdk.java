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
package com.intellij.openapi.projectRoots;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.projectRoots.impl.SdkVersionUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public abstract class JavaSdk extends SdkType implements JavaSdkType, ApplicationComponent {
  public JavaSdk(@NonNls String name) {
    super(name);
  }

  public static JavaSdk getInstance() {
    return ApplicationManager.getApplication().getComponent(JavaSdk.class);
  }

  public final Sdk createJdk(final String jdkName, String jreHome) {
    return createJdk(jdkName, jreHome, true);
  }

  public abstract int compareTo(@NotNull String versionString, @NotNull String versionNumber);

  public abstract Sdk createJdk(@NonNls String jdkName, String home, boolean isJre);

  public static boolean checkForJdk(File file) {
    return JdkUtil.checkForJdk(file);
  }

  public static boolean checkForJre(String file) {
    return JdkUtil.checkForJre(file);
  }

  @Nullable
  public static String getJdkVersion(final String sdkHome) {
    return SdkVersionUtil.readVersionFromProcessOutput(sdkHome, new String[] {sdkHome + File.separator + "bin" + File.separator + "java",  "-version"}, "version");
  }
}
