/**
 * 
 */
package org.eclipse.dltk.tcl.internal.ui.text;

import java.net.URI;
import java.util.Iterator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.core.IExternalSourceModule;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.environment.EnvironmentManager;
import org.eclipse.dltk.core.environment.IEnvironment;
import org.eclipse.dltk.core.environment.IFileHandle;
import org.eclipse.dltk.internal.core.ModelManager;
import org.eclipse.dltk.launching.IInterpreterInstall;
import org.eclipse.dltk.launching.ScriptRuntime;
import org.eclipse.dltk.tcl.core.TclPackagesManager;
import org.eclipse.dltk.tcl.core.packages.TclModuleInfo;
import org.eclipse.dltk.tcl.core.packages.TclPackagesFactory;
import org.eclipse.dltk.tcl.core.packages.TclProjectInfo;
import org.eclipse.dltk.tcl.core.packages.TclSourceEntry;
import org.eclipse.dltk.tcl.core.packages.UserCorrection;
import org.eclipse.dltk.tcl.internal.ui.TclUI;
import org.eclipse.dltk.ui.editor.IScriptAnnotation;
import org.eclipse.dltk.ui.environment.IEnvironmentUI;
import org.eclipse.dltk.ui.text.IAnnotationResolution;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.PlatformUI;

final class TclSourceMarkerResolution implements IMarkerResolution,
		IAnnotationResolution {
	private String sourceName;
	private IScriptProject project;
	private ISourceModule module;

	public TclSourceMarkerResolution(String pkgName,
			IScriptProject scriptProject, ISourceModule module) {
		this.sourceName = pkgName;
		this.project = scriptProject;
		this.module = module;
	}

	public String getLabel() {
		return "Add user specified source file location to buildpath";
	}

	private static IPath resolveSourceValue(IPath folder, String source,
			IEnvironment environment) {
		IPath valuePath = null;
		if (environment.isLocal()) {
			valuePath = Path.fromOSString(source);
		} else {
			source = source.replace('\\', '/');
			valuePath = Path.fromPortableString(source);
		}
		IPath sourcedPath = null;
		if (valuePath.isAbsolute()) {
			sourcedPath = valuePath;
		} else {
			if (TclPackagesManager.isValidName(source)) {
				sourcedPath = folder.append(valuePath);
			}
		}
		return sourcedPath;
	}

	public static boolean fixAvailable(ISourceModule module, String source) {
		if( module == null || !module.exists()) {
			return false;
		}
		IEnvironment env = EnvironmentManager.getEnvironment(module);

		IPath modulePath = module.getPath();
		if (module.getResource() != null) {
			modulePath = module.getResource().getLocation();
			if (modulePath == null) {
				URI uri = module.getResource().getLocationURI();
				if (uri != null) {
					IFileHandle file = env.getFile(uri);
					if (file != null) {
						modulePath = file.getPath();
					}
				}
			}
		}
		IPath sourcePath = resolveSourceValue(modulePath.removeLastSegments(1),
				source, env);
		if (sourcePath == null) {
			return false;
		}
		return true;
	}

	private boolean resolve() {

		final IInterpreterInstall install;
		try {
			install = ScriptRuntime.getInterpreterInstall(project);
			if (install != null) {
				// Ask for user correction.

				IEnvironment env = EnvironmentManager
						.getEnvironment(this.module);

				IPath modulePath = module.getPath();
				if (module.getResource() != null) {
					modulePath = module.getResource().getLocation();
					if (modulePath == null) {
						URI uri = module.getResource().getLocationURI();
						if (uri != null) {
							IFileHandle file = env.getFile(uri);
							if (file != null) {
								modulePath = file.getPath();
							}
						}
					}
				}
				IPath sourcePath = resolveSourceValue(modulePath
						.removeLastSegments(1), this.sourceName, env);
				if (sourcePath == null) {
					return false;
				}

				TclProjectInfo tclProject = TclPackagesManager
						.getTclProject(project.getElementName());
				EList modules = tclProject.getModules();
				String handle = this.module.getHandleIdentifier();
				TclModuleInfo info = null;
				for (Iterator iterator = modules.iterator(); iterator.hasNext();) {
					TclModuleInfo moduleInfo = (TclModuleInfo) iterator.next();
					if (handle.equals(moduleInfo.getHandle())) {
						info = moduleInfo;
						break;
					}
				}
				if (info == null) {
					// This is almost impossibly situation.
					info = TclPackagesFactory.eINSTANCE.createTclModuleInfo();
					info.setHandle(handle);
					info
							.setExternal(this.module instanceof IExternalSourceModule);
					TclSourceEntry sourceEntry = TclPackagesFactory.eINSTANCE
							.createTclSourceEntry();
					sourceEntry.setStart(-1);
					sourceEntry.setEnd(-1);
					sourceEntry.setValue(sourceName);
					info.getSourced().add(sourceEntry);
					modules.add(info);
				}
				UserCorrection correction = TclPackagesFactory.eINSTANCE
						.createUserCorrection();
				correction.setOriginalValue(sourceName);
				correction.setUserValue(sourcePath.toString());
				info.getSourceCorrections().add(correction);
				TclPackagesManager.save();
				// We need to fire external archives change.
				ModelManager.getModelManager().getDeltaProcessor()
						.checkExternalChanges(new IModelElement[] { project },
								new NullProgressMonitor());
			}
		} catch (CoreException e) {
			TclUI.error("require package resolve error", e); //$NON-NLS-1$
		}
		return false;
	}

	public void run(final IMarker marker) {
		resolve();
	}

	public void run(IScriptAnnotation annotation, IDocument document) {
		resolve();
	}
}