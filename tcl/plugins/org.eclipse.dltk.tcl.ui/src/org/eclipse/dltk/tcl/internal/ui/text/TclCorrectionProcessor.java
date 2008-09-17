package org.eclipse.dltk.tcl.internal.ui.text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.dltk.core.CorrectionEngine;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.DLTKLanguageManager;
import org.eclipse.dltk.core.IDLTKLanguageToolkit;
import org.eclipse.dltk.core.IScriptModelMarker;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.internal.ui.editor.EditorUtility;
import org.eclipse.dltk.internal.ui.editor.ScriptEditor;
import org.eclipse.dltk.launching.IInterpreterInstall;
import org.eclipse.dltk.launching.ScriptRuntime;
import org.eclipse.dltk.tcl.core.TclNature;
import org.eclipse.dltk.tcl.core.TclProblems;
import org.eclipse.dltk.tcl.internal.core.packages.PackagesManager;
import org.eclipse.dltk.ui.editor.IScriptAnnotation;
import org.eclipse.dltk.ui.text.AnnotationResolutionProposal;
import org.eclipse.dltk.ui.text.MarkerResolutionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.SimpleMarkerAnnotation;

public class TclCorrectionProcessor implements IQuickAssistProcessor {
	TclCorrectionAssistant fAssistant;

	public TclCorrectionProcessor(TclCorrectionAssistant tclCorrectionAssistant) {
		this.fAssistant = tclCorrectionAssistant;
	}

	public boolean canAssist(IQuickAssistInvocationContext invocationContext) {
		return true;
	}

	public boolean canFix(Annotation annotation) {
		return hasCorrections(annotation);
	}

	public ICompletionProposal[] computeQuickAssistProposals(
			IQuickAssistInvocationContext invocationContext) {
		final Annotation[] annotations = fAssistant.getAnnotationsAtOffset();
		final ScriptEditor editor = (ScriptEditor) this.fAssistant.getEditor();
		final ISourceModule sourceModule = EditorUtility
				.getEditorInputModelElement(editor, false);
		final IScriptProject scriptProject = sourceModule.getScriptProject();
		final Set packageNames = new HashSet();
		List proposals = null;
		for (int i = 0; i < annotations.length; i++) {
			final Annotation annotation = annotations[i];
			ICompletionProposal proposal = null;
			if (annotation instanceof MarkerAnnotation) {
				MarkerAnnotation mAnnot = (MarkerAnnotation) annotation;
				IMarker marker = mAnnot.getMarker();
				if (isFixable(marker)) {
					final String pkgName = CorrectionEngine
							.getProblemArguments(marker)[0];
					if (packageNames.add(pkgName)) {
						proposal = new MarkerResolutionProposal(
								new TclRequirePackageMarkerResolution(pkgName,
										scriptProject), marker);
					}
				}
			} else if (annotation instanceof IScriptAnnotation) {
				final IScriptAnnotation sa = (IScriptAnnotation) annotation;
				if (isFixable(sa)) {
					final String pkgName = sa.getArguments()[0];
					if (packageNames.add(pkgName)) {
						proposal = new AnnotationResolutionProposal(
								new TclRequirePackageMarkerResolution(pkgName,
										scriptProject), annotation);
					}
				}
			}
			if (proposal != null) {
				if (proposals == null) {
					proposals = new ArrayList();
				}
				proposals.add(proposal);
			}
		}
		if (proposals != null) {
			return (ICompletionProposal[]) proposals
					.toArray(new ICompletionProposal[proposals.size()]);
		}
		return null;
	}

	public String getErrorMessage() {
		return null;
	}

	public static boolean isQuickFixableType(Annotation annotation) {
		return (annotation instanceof IScriptAnnotation || annotation instanceof SimpleMarkerAnnotation)
				&& !annotation.isMarkedDeleted();
	}

	public static boolean isFixable(IMarker marker) {
		if (marker.getAttribute(IScriptModelMarker.ID, 0) == TclProblems.UNKNOWN_REQUIRED_PACKAGE) {
			final String[] args = CorrectionEngine.getProblemArguments(marker);
			if (args != null && args.length != 0 && args[0] != null) {
				IResource resource = marker.getResource();
				IProject project = resource.getProject();
				IScriptProject scriptProject = DLTKCore.create(project);
				if (isFixable(args[0], scriptProject)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isFixable(IScriptAnnotation annotation) {
		if (annotation.getId() == TclProblems.UNKNOWN_REQUIRED_PACKAGE) {
			final String[] args = annotation.getArguments();
			if (args != null && args.length != 0 && args[0] != null) {
				final ISourceModule module = annotation.getSourceModule();
				if (module != null) {
					final IScriptProject project = module.getScriptProject();
					if (project != null) {
						if (isFixable(args[0], project)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public static boolean isFixable(String pkgName, IScriptProject scriptProject) {
		IDLTKLanguageToolkit toolkit = null;
		toolkit = DLTKLanguageManager.getLanguageToolkit(scriptProject);
		if (toolkit != null
				&& toolkit.getNatureId().equals(TclNature.NATURE_ID)) {
			IInterpreterInstall install = null;
			try {
				install = ScriptRuntime.getInterpreterInstall(scriptProject);
			} catch (CoreException e) {
				if (DLTKCore.DEBUG) {
					e.printStackTrace();
				}
			}
			if (install != null) {
				PackagesManager manager = PackagesManager.getInstance();
				IPath[] paths = manager.getPathsForPackage(install, pkgName);
				if (paths != null && paths.length > 0) {
					return true;
				}
				Map dependencies = manager.getDependencies(pkgName, install);
				for (Iterator iterator = dependencies.keySet().iterator(); iterator
						.hasNext();) {
					String pkg = (String) iterator.next();
					IPath[] paths2 = manager.getPathsForPackage(install, pkg);
					if (paths2 != null && paths2.length > 0) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public static boolean hasCorrections(Annotation annotation) {
		if (annotation instanceof MarkerAnnotation) {
			MarkerAnnotation mAnnot = (MarkerAnnotation) annotation;
			IMarker marker = mAnnot.getMarker();
			return isFixable(marker);
		} else if (annotation instanceof IScriptAnnotation) {
			return isFixable((IScriptAnnotation) annotation);
		}
		return false;
	}
}
