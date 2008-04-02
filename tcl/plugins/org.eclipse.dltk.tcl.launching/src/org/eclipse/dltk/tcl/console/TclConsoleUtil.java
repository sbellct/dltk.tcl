/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.tcl.console;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.dltk.console.ScriptConsoleServer;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.environment.EnvironmentsManager;
import org.eclipse.dltk.core.environment.IExecutionEnvironment;
import org.eclipse.dltk.core.environment.IFileHandle;
import org.eclipse.dltk.launching.ScriptLaunchUtil;
import org.eclipse.dltk.tcl.core.TclNature;
import org.eclipse.dltk.tcl.launching.TclLaunchingPlugin;

public class TclConsoleUtil {
	public static void runDefaultTclInterpreter(TclInterpreter interpreter)
			throws CoreException, IOException {
		ScriptConsoleServer server = ScriptConsoleServer.getInstance();

		String id = server.register(interpreter);
		String port = Integer.toString(server.getPort());

		String[] args = new String[] { "127.0.0.1", port, id };

		// TODO: Add support for environments
		IExecutionEnvironment exeEnv = (IExecutionEnvironment) EnvironmentsManager
				.getLocalEnvironment().getAdapter(IExecutionEnvironment.class);

		IFileHandle scriptFile = TclLaunchingPlugin.getDefault()
				.getConsoleProxy(exeEnv);
		final ILaunch launch = ScriptLaunchUtil.runScript(TclNature.NATURE_ID,
				scriptFile, null, null, args, null);
		if (launch != null) {
			interpreter.addCloseOperation(new Runnable() {
				public void run() {
					IProcess[] processes = launch.getProcesses();
					if (processes != null) {
						for (int i = 0; i < processes.length; i++) {
							try {
								processes[i].terminate();
							} catch (DebugException e) {
								if (DLTKCore.DEBUG) {
									e.printStackTrace();
								}
							}
						}
					}
				}
			});
		}
	}
}
