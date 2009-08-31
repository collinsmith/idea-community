package com.intellij.xdebugger.impl.actions;

import org.jetbrains.annotations.NotNull;
import com.intellij.xdebugger.impl.DebuggerSupport;

/**
 * @author nik
 */
public class ForceStepOverAction extends XDebuggerActionBase {
  @NotNull
  protected DebuggerActionHandler getHandler(@NotNull final DebuggerSupport debuggerSupport) {
    return debuggerSupport.getForceStepOverHandler();
  }
}