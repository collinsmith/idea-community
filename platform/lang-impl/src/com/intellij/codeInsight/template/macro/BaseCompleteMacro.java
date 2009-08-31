package com.intellij.codeInsight.template.macro;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.lookup.*;
import com.intellij.codeInsight.template.*;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateState;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilBase;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public abstract class BaseCompleteMacro implements Macro {
  private final String myName;

  protected BaseCompleteMacro(@NonNls String name) {
    myName = name;
  }

  abstract CodeInsightActionHandler getCompletionHandler ();

  public String getName() {
    return myName;
  }

  public String getDescription() {
    return myName + "()";
  }

  public String getDefaultValue() {
    return "a";
  }

  public final Result calculateResult(@NotNull Expression[] params, final ExpressionContext context) {
    return new InvokeActionResult(
      new Runnable() {
        public void run() {
          invokeCompletion(context);
        }
      }
    );
  }

  private void invokeCompletion(final ExpressionContext context) {
    final Project project = context.getProject();
    final Editor editor = context.getEditor();

    final PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
    new WriteCommandAction.Simple(project, psiFile) {
        public void run() {
          PsiDocumentManager.getInstance(project).commitAllDocuments();
        }
      }.execute();

    if (ApplicationManager.getApplication().isUnitTestMode()) {
      getCompletionHandler().invoke(project, editor, psiFile);
      return;
    }

    ApplicationManager.getApplication().invokeLater(new Runnable() {
      public void run() {
        if (project.isDisposed()) return;

        CommandProcessor.getInstance().executeCommand(
            project, new Runnable() {
            public void run() {
              if (!ApplicationManager.getApplication().isUnitTestMode()) {
                getCompletionHandler().invoke(project, editor, psiFile);
              }

              final LookupManager lookupManager = LookupManager.getInstance(project);
              Lookup lookup = lookupManager.getActiveLookup();

              if (lookup != null) {
                lookup.addLookupListener(new MyLookupListener(context));
              }
              else {
                TemplateState templateState = TemplateManagerImpl.getTemplateState(editor);
                if (templateState != null) {
                  TextRange range = templateState.getCurrentVariableRange();
                  if (range != null && range.getLength() > 0/* && TemplateEditorUtil.getOffset(editor) == range.getEndOffset()*/) {
                    templateState.nextTab();
                  }
                }
              }
            }
          },
            "",
            null
        );
      }
    });
  }

  public Result calculateQuickResult(@NotNull Expression[] params, ExpressionContext context) {
    return null;
  }

  public LookupElement[] calculateLookupItems(@NotNull Expression[] params, ExpressionContext context) {
    return null;
  }

  private static class MyLookupListener extends LookupAdapter {
    private final ExpressionContext myContext;

    public MyLookupListener(@NotNull ExpressionContext context) {
      myContext = context;
    }

    public void itemSelected(LookupEvent event) {
      LookupElement item = event.getItem();
      if (item == null) return;

      boolean goNextTab = true;
      for(TemplateCompletionProcessor processor: Extensions.getExtensions(TemplateCompletionProcessor.EP_NAME)) {
        if (!processor.nextTabOnItemSelected(myContext, item)) {
          goNextTab = false;
          break;
        }
      }

      if (goNextTab) {
        final Editor editor = myContext.getEditor();
        if (editor != null) {
          TemplateState templateState = TemplateManagerImpl.getTemplateState(editor);
          if (templateState != null) {
            templateState.nextTab();
          }
        }
      }
    }
  }
}