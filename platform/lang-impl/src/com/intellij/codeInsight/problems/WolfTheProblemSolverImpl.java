package com.intellij.codeInsight.problems;

import com.intellij.codeInsight.daemon.impl.*;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.impl.LaterInvocator;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.FileStatusListener;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.ex.StatusBarEx;
import com.intellij.problems.Problem;
import com.intellij.problems.WolfTheProblemSolver;
import com.intellij.psi.*;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author cdr
 */
public class WolfTheProblemSolverImpl extends WolfTheProblemSolver {
  private final Map<VirtualFile, ProblemFileInfo> myProblems = new THashMap<VirtualFile, ProblemFileInfo>();
  private final Collection<VirtualFile> myCheckingQueue = new THashSet<VirtualFile>(10);

  private final Project myProject;
  private final List<ProblemListener> myProblemListeners = ContainerUtil.createEmptyCOWList();
  private final List<Condition<VirtualFile>> myFilters = ContainerUtil.createEmptyCOWList();
  private boolean myFiltersLoaded = false;
  private final ProblemListener fireProblemListeners = new ProblemListener() {
    public void problemsAppeared(VirtualFile file) {
      for (final ProblemListener problemListener : myProblemListeners) {
        problemListener.problemsAppeared(file);
      }
    }

    public void problemsChanged(VirtualFile file) {
      for (final ProblemListener problemListener : myProblemListeners) {
        problemListener.problemsChanged(file);
      }
    }

    public void problemsDisappeared(VirtualFile file) {
      for (final ProblemListener problemListener : myProblemListeners) {
        problemListener.problemsDisappeared(file);
      }
    }
  };

  private final VirtualFileListener myVirtualFileListener = new VirtualFileAdapter() {
    public void fileDeleted(final VirtualFileEvent event) {
      onDeleted(event.getFile());
    }

    public void fileMoved(final VirtualFileMoveEvent event) {
      onDeleted(event.getFile());
    }

    private void onDeleted(final VirtualFile file) {
      if (file.isDirectory()) {
        clearInvalidFiles();
      }
      else {
        doRemove(file);
      }
    }
  };

  private void doRemove(VirtualFile problemFile) {
    ProblemFileInfo old;
    synchronized (myProblems) {
      old = myProblems.remove(problemFile);
    }
    synchronized (myCheckingQueue) {
      myCheckingQueue.remove(problemFile);
    }
    if (old != null) {
      // firing outside lock
      fireProblemListeners.problemsDisappeared(problemFile);
    }
  }

  private final PsiTreeChangeListener myChangeListener;

  private static class ProblemFileInfo {
    private final Collection<Problem> problems = new THashSet<Problem>();
    private boolean hasSyntaxErrors;

    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      final ProblemFileInfo that = (ProblemFileInfo)o;

      if (hasSyntaxErrors != that.hasSyntaxErrors) return false;
      if (!problems.equals(that.problems)) return false;

      return true;
    }

    public int hashCode() {
      int result = problems.hashCode();
      result = 31 * result + (hasSyntaxErrors ? 1 : 0);
      return result;
    }
  }

  public WolfTheProblemSolverImpl(Project project, PsiManager psiManager, VirtualFileManager virtualFileManager) {
    myProject = project;
    myChangeListener = new PsiTreeChangeAdapter() {
      public void childAdded(PsiTreeChangeEvent event) {
        childrenChanged(event);
      }

      public void childRemoved(PsiTreeChangeEvent event) {
        childrenChanged(event);
      }

      public void childReplaced(PsiTreeChangeEvent event) {
        childrenChanged(event);
      }

      public void childMoved(PsiTreeChangeEvent event) {
        childrenChanged(event);
      }

      public void propertyChanged(PsiTreeChangeEvent event) {
        childrenChanged(event);
      }

      public void childrenChanged(PsiTreeChangeEvent event) {
        clearSyntaxErrorFlag(event);
      }
    };
    psiManager.addPsiTreeChangeListener(myChangeListener);
    virtualFileManager.addVirtualFileListener(myVirtualFileListener, myProject);
    FileStatusManager fileStatusManager = FileStatusManager.getInstance(myProject);
    if (fileStatusManager != null) { //tests?
      fileStatusManager.addFileStatusListener(new FileStatusListener() {
        public void fileStatusesChanged() {
          clearInvalidFiles();
        }

        public void fileStatusChanged(@NotNull VirtualFile virtualFile) {
          fileStatusesChanged();
        }
      });
    }
  }

  private void clearInvalidFiles() {
    VirtualFile[] files;
    synchronized (myProblems) {
      files = myProblems.keySet().toArray(new VirtualFile[myProblems.size()]);
    }
    for (VirtualFile problemFile : files) {
      if (!problemFile.isValid() || !isToBeHighlighted(problemFile)) {
        doRemove(problemFile);
      }
    }
  }

  private void clearSyntaxErrorFlag(final PsiTreeChangeEvent event) {
    PsiFile file = event.getFile();
    if (file == null) return;
    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) return;
    synchronized (myProblems) {
      ProblemFileInfo info = myProblems.get(virtualFile);
      if (info == null) return;
      info.hasSyntaxErrors = false;
    }
  }

  public void projectOpened() {
  }

  public void projectClosed() {
  }

  @NotNull
  @NonNls
  public String getComponentName() {
    return "Problems";
  }

  public void initComponent() {

  }

  public void disposeComponent() {

  }

  public void startCheckingIfVincentSolvedProblemsYet(final ProgressIndicator progress, ProgressableTextEditorHighlightingPass pass) throws ProcessCanceledException{
    if (!myProject.isOpen()) return;

    int size;
    List<VirtualFile> files;
    synchronized (myCheckingQueue) {
      files = new ArrayList<VirtualFile>(myCheckingQueue);
      size = files.size();
    }
    pass.setProgressLimit(size);
    final StatusBar statusBar = WindowManager.getInstance().getStatusBar(myProject);
    String oldInfo = saveStatusBarInfo(statusBar);
    try {
      for (final VirtualFile virtualFile : files) {
        progress.checkCanceled();
        if (virtualFile == null) break;
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          public void run() {
            statusBar.setInfo("Checking '" + virtualFile.getPresentableUrl() + "'");
          }
        });
        if (!virtualFile.isValid() || orderVincentToCleanTheCar(virtualFile, progress)) {
          doRemove(virtualFile);
        }
        pass.advanceProgress(1);
      }
    }
    finally {
      restoreStatusBarInfo(statusBar, oldInfo);
    }
  }

  private static class HaveGotErrorException extends RuntimeException {
    private final HighlightInfo myHighlightInfo;
    private final boolean myHasErrorElement;

    private HaveGotErrorException(HighlightInfo info, final boolean hasErrorElement) {
      myHighlightInfo = info;
      myHasErrorElement = hasErrorElement;
    }
  }

  // returns true if car has been cleaned
  private boolean orderVincentToCleanTheCar(final VirtualFile file, ProgressIndicator progressIndicator) throws ProcessCanceledException {
    if (!isToBeHighlighted(file)) {
      clearProblems(file);
      return true; // file is going to be red waved no more
    }
    if (hasSyntaxErrors(file)) {
      // it's no use anyway to try clean the file with syntax errors, only changing the file itself can help
      return false;
    }
    if (myProject.isDisposed()) return false;
    if (willBeHighlightedAnyway(file)) return false;
    final PsiFile psiFile = PsiManager.getInstance(myProject).findFile(file);
    if (psiFile == null) return false;
    final Document document = FileDocumentManager.getInstance().getDocument(file);
    if (document == null) return false;

    try {
      GeneralHighlightingPass pass = new GeneralHighlightingPass(myProject, psiFile, document, 0, document.getTextLength(), false) {
        protected HighlightInfoHolder createInfoHolder() {
          return new HighlightInfoHolder(psiFile, HighlightInfoFilter.EMPTY_ARRAY) {
            public boolean add(HighlightInfo info) {
              if (info != null && info.getSeverity() == HighlightSeverity.ERROR) {
                throw new HaveGotErrorException(info, myHasErrorElement);
              }
              return super.add(info);
            }
          };
        }

        protected AnnotationHolderImpl createAnnotationHolder() {
          return new AnnotationHolderImpl(){
            public boolean add(Annotation annotation) {
              if (annotation != null && annotation.getSeverity() == HighlightSeverity.ERROR) {
                HighlightInfo highlightInfo = HighlightInfo.fromAnnotation(annotation);
                throw new HaveGotErrorException(highlightInfo, myHasErrorElement);
              }
              return super.add(annotation);
            }
          };
        }
      };
      pass.collectInformation(progressIndicator);
    }
    catch (HaveGotErrorException e) {
      ProblemImpl problem = new ProblemImpl(file, e.myHighlightInfo, e.myHasErrorElement);
      reportProblems(file, Collections.<Problem>singleton(problem));
      return false;
    }
    clearProblems(file);
    return true;
  }

  private static String saveStatusBarInfo(StatusBar statusBar) {
    String oldInfo = null;
    if (statusBar instanceof StatusBarEx) {
      oldInfo = ((StatusBarEx)statusBar).getInfo();
    }
    return oldInfo;
  }

  private static void restoreStatusBarInfo(final StatusBar statusBar, final String oldInfo) {
    if (statusBar instanceof StatusBarEx) {
      LaterInvocator.invokeLater(new Runnable() {
        public void run() {
          statusBar.setInfo(oldInfo);
        }
      });
    }
  }

  public boolean hasSyntaxErrors(final VirtualFile file) {
    synchronized (myProblems) {
      ProblemFileInfo info = myProblems.get(file);
      return info != null && info.hasSyntaxErrors;
    }
  }

  private boolean willBeHighlightedAnyway(final VirtualFile file) {
    // opened in some editor, and hence will be highlighted automatically sometime later
    FileEditor[] selectedEditors = FileEditorManager.getInstance(myProject).getSelectedEditors();
    for (FileEditor editor : selectedEditors) {
      if (!(editor instanceof TextEditor)) continue;
      Document document = ((TextEditor)editor).getEditor().getDocument();
      PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getCachedPsiFile(document);
      if (psiFile == null) continue;
      if (file == psiFile.getVirtualFile()) return true;
    }
    return false;
  }

  public boolean hasProblemFilesBeneath(Condition<VirtualFile> condition) {
    if (!myProject.isOpen()) return false;
    synchronized (myProblems) {
      if (!myProblems.isEmpty()) {
        Set<VirtualFile> problemFiles = myProblems.keySet();
        for (VirtualFile problemFile : problemFiles) {
          if (problemFile.isValid() && condition.value(problemFile)) return true;
        }
      }
      return false;
    }
  }

  public boolean hasProblemFilesBeneath(final Module scope) {
    return hasProblemFilesBeneath(new Condition<VirtualFile>() {
      public boolean value(final VirtualFile virtualFile) {
        return ModuleUtil.moduleContainsFile(scope, virtualFile, false);
      }
    });
  }

  public void addProblemListener(ProblemListener listener) {
    myProblemListeners.add(listener);
  }

  public void addProblemListener(final ProblemListener listener, Disposable parentDisposable) {
    addProblemListener(listener);
    Disposer.register(parentDisposable, new Disposable() {
      public void dispose() {
        removeProblemListener(listener);
      }
    });
  }

  public void removeProblemListener(ProblemListener listener) {
    myProblemListeners.remove(listener);
  }

  public void registerFileHighlightFilter(final Condition<VirtualFile> filter, Disposable parentDisposable) {
    myFilters.add(filter);
    Disposer.register(parentDisposable, new Disposable() {
      public void dispose() {
        myFilters.remove(filter);
      }
    });
  }

  @Override
  public void queue(VirtualFile suspiciousFile) {
    if (!isToBeHighlighted(suspiciousFile)) return;
    synchronized (myCheckingQueue) {
      myCheckingQueue.add(suspiciousFile);
    }
  }

  public boolean isProblemFile(VirtualFile virtualFile) {
    synchronized (myProblems) {
      return myProblems.containsKey(virtualFile);
    }
  }

  private boolean isToBeHighlighted(VirtualFile virtualFile) {
    if (virtualFile == null) return false;

    synchronized (myFilters) {
      if (!myFiltersLoaded) {
        myFiltersLoaded = true;
        Collections.addAll(myFilters, Extensions.getExtensions(FILTER_EP_NAME, myProject));
      }
    }
    for (final Condition<VirtualFile> filter : myFilters) {
      ProgressManager.getInstance().checkCanceled();
      if (filter.value(virtualFile)) {
        return true;
      }
    }

    return false;
  }

  public void weHaveGotProblems(@NotNull final VirtualFile virtualFile, @NotNull List<Problem> problems) {
    if (problems.isEmpty()) return;
    if (!isToBeHighlighted(virtualFile)) return;
    boolean fireListener = false;
    synchronized (myProblems) {
      ProblemFileInfo storedProblems = myProblems.get(virtualFile);
      if (storedProblems == null) {
        storedProblems = new ProblemFileInfo();

        myProblems.put(virtualFile, storedProblems);
        fireListener = true;
      }
      storedProblems.problems.addAll(problems);
    }
    synchronized (myCheckingQueue) {
      myCheckingQueue.add(virtualFile);
    }
    if (fireListener) {
      fireProblemListeners.problemsAppeared(virtualFile);
    }
  }

  public void clearProblems(@NotNull VirtualFile virtualFile) {
    doRemove(virtualFile);
  }

  public Problem convertToProblem(final VirtualFile virtualFile, final HighlightSeverity severity,
                                  final TextRange textRange, final String messageText) {
    if (virtualFile == null || textRange.getStartOffset() < 0 || textRange.getLength() < 0 ) return null;
    HighlightInfo info = ApplicationManager.getApplication().runReadAction(new Computable<HighlightInfo>() {
      public HighlightInfo compute() {
        return HighlightInfo.createHighlightInfo(HighlightInfo.convertSeverity(severity), textRange, messageText);
      }
    });
    return new ProblemImpl(virtualFile, info, false);
  }

  public Problem convertToProblem(final VirtualFile virtualFile, final int line, final int column, final String[] message) {
    if (virtualFile == null || virtualFile.isDirectory() || virtualFile.getFileType().isBinary()) return null;
    HighlightInfo info = ApplicationManager.getApplication().runReadAction(new Computable<HighlightInfo>() {
      public HighlightInfo compute() {
        TextRange textRange = getTextRange(virtualFile, line, column);
        return HighlightInfo.createHighlightInfo(HighlightInfoType.ERROR, textRange, StringUtil.join(message, "\n"));
      }
    });
    if (info == null) return null;
    return new ProblemImpl(virtualFile, info, false);
  }

  public void reportProblems(final VirtualFile file, Collection<Problem> problems) {
    if (problems.isEmpty()) {
      clearProblems(file);
      return;
    }
    if (!isToBeHighlighted(file)) return;
    boolean hasProblemsBefore;
    boolean fireChanged;
    synchronized (myProblems) {
      final ProblemFileInfo oldInfo = myProblems.remove(file);
      hasProblemsBefore = oldInfo != null;
      ProblemFileInfo newInfo = new ProblemFileInfo();
      myProblems.put(file, newInfo);
      for (Problem problem : problems) {
        newInfo.problems.add(problem);
        newInfo.hasSyntaxErrors |= ((ProblemImpl)problem).isSyntaxOnly();
      }
      fireChanged = hasProblemsBefore && !oldInfo.equals(newInfo);
    }
    synchronized (myCheckingQueue) {
      myCheckingQueue.add(file);
    }
    if (!hasProblemsBefore) {
      fireProblemListeners.problemsAppeared(file);
    }
    else if (fireChanged) {
      fireProblemListeners.problemsChanged(file);
    }
  }

  @NotNull
  private static TextRange getTextRange(final VirtualFile virtualFile, int line, final int column) {
    Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
    if (line > document.getLineCount()) line = document.getLineCount();
    line = line <= 0 ? 0 : line - 1;
    int offset = document.getLineStartOffset(line) + (column <= 0 ? 0 : column - 1);
    return new TextRange(offset, offset);
  }
}