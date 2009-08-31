package com.intellij.find.findUsages;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;

import javax.swing.*;

public class FindVariableUsagesDialog extends JavaFindUsagesDialog {

  public FindVariableUsagesDialog(PsiElement element, Project project, FindUsagesOptions findUsagesOptions,
                                  boolean toShowInNewTab, boolean mustOpenInNewTab, boolean isSingleFile, FindUsagesHandler handler){
    super(element, project, findUsagesOptions, toShowInNewTab, mustOpenInNewTab, isSingleFile, handler);
  }

  public JComponent getPreferredFocusedControl() {
    return myCbToSkipResultsWhenOneUsage;
  }

  public void calcFindUsagesOptions(FindUsagesOptions options) {
    super.calcFindUsagesOptions(options);

    options.isReadAccess = true;
    options.isWriteAccess = true;
  }

  protected JPanel createFindWhatPanel(){
    return null;
  }

  protected JPanel createAllOptionsPanel() {
    return getPsiElement() instanceof PsiField ? super.createAllOptionsPanel() : createUsagesOptionsPanel();
  }

  protected void update() {
    setOKActionEnabled(true);
  }
}