/*******************************************************************************
 * Copyright (c) 2009 xored software, Inc.  
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html  
 *
 * Contributors:
 *     xored software, Inc. - initial API and Implementation (Alex Panchenko)
 *******************************************************************************/
package org.eclipse.dltk.tcl.internal.debug.ui.interpreters;

import org.eclipse.osgi.util.NLS;

/**
 * @since 2.0
 */
public class TclInterpreterMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.dltk.tcl.internal.debug.ui.interpreters.tclInterpeterMessages"; //$NON-NLS-1$
	public static String AddTclInterpreterDialog_0;
	public static String TclInterpreterComboBlock_buttonAdd;
	public static String TclInterpreterComboBlock_buttonAddAll;
	public static String TclInterpreterComboBlock_buttonRemove;
	public static String TclInterpreterComboBlock_CategoryAutomatic;
	public static String TclInterpreterComboBlock_CategoryManual;
	public static String TclInterpreterComboBlock_errorMessage;
	public static String TclInterpreterComboBlock_errorTitle;
	public static String TclInterpreterComboBlock_title;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, TclInterpreterMessages.class);
	}

	private TclInterpreterMessages() {
	}
}
