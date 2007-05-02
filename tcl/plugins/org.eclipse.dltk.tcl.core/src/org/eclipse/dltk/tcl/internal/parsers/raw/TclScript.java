/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.tcl.internal.parsers.raw;

import java.util.ArrayList;
import java.util.List;

public class TclScript extends TclElement {
	
	List commands; 
	
	public TclScript() {		
		commands = new ArrayList();
	}
	
	public void addCommand (TclCommand cmd) {
		commands.add (cmd);
	}
	
	public String getRawText() {
		//TODO
		return null;
	}

	public List getCommands () {
		return commands;
	}
	
}
