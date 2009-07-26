/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.tcl.launching;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.dltk.core.environment.IFileHandle;
import org.eclipse.dltk.launching.AbstractScriptLaunchConfigurationDelegate;
import org.eclipse.dltk.launching.InterpreterConfig;
import org.eclipse.dltk.tcl.core.TclNature;

public class TclLaunchConfigurationDelegate extends
		AbstractScriptLaunchConfigurationDelegate {

	/**
	 * @since 1.1
	 */
	public static final String TCLLIBPATH_ENV_VAR = "TCLLIBPATH"; //$NON-NLS-1$

	public String getLanguageId() {
		return TclNature.NATURE_ID;
	}

	protected InterpreterConfig createInterpreterConfig(
			ILaunchConfiguration configuration, ILaunch launch)
			throws CoreException {
		InterpreterConfig config = super.createInterpreterConfig(configuration,
				launch);
		if (config != null) {
			addLibpathEnvVar(config, configuration);
			checkEnvVars(config, configuration);
		}

		return config;
	}

	protected void addLibpathEnvVar(InterpreterConfig config,
			ILaunchConfiguration configuration) throws CoreException {
		String currentValue = config.removeEnvVar(TCLLIBPATH_ENV_VAR);
		IPath[] paths = createBuildPath(configuration, config.getEnvironment());

		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < paths.length; ++i) {
			final IFileHandle file = config.getEnvironment().getFile(paths[i]);
			if (file != null) {
				if (sb.length() != 0) {
					sb.append(' ');
				}
				sb.append('{');
				sb.append(file.toOSString());
				sb.append('}');
			}
		}
		if (currentValue != null) {
			if (sb.length() != 0) {
				sb.append(' ');
			}
			sb.append(convertToTclLibPathFormat(currentValue));
			// sb.append(currentValue).append(" ");
		}
		if (sb.length() != 0) {
			config.addEnvVar(TCLLIBPATH_ENV_VAR, sb.toString());
		}
	}

	/**
	 * @since 1.1
	 */
	public static String convertToTclLibPathFormat(String currentValue) {
		currentValue = currentValue.trim();
		if (currentValue.startsWith("'") && currentValue.endsWith("'")
				&& currentValue.length() >= 2) {
			return convertToTCLLIBPATH(currentValue.substring(1, currentValue
					.length() - 1));
		}
		if (currentValue.startsWith("\"") && currentValue.endsWith("\"")
				&& currentValue.length() >= 2) {
			return convertToTCLLIBPATH(currentValue.substring(1, currentValue
					.length() - 1));
		}
		return currentValue;
	}

	private static String convertToTCLLIBPATH(String value) {
		String[] values = value.split(" ");
		StringBuffer sb = new StringBuffer();
		for (String val : values) {
			if (!(val.startsWith("{") && val.endsWith("}"))) {
				sb.append('{');
				sb.append(val);
				sb.append('}').append(" ");
			} else {
				sb.append(val).append(" ");
			}
		}
		return sb.toString();
	}

	protected void checkEnvVars(InterpreterConfig config,
			ILaunchConfiguration configuration) {
	}

}
