package org.eclipse.dltk.tcl.internal.debug;

import org.eclipse.dltk.debug.core.model.AtomicScriptType;
import org.eclipse.dltk.debug.core.model.ComplexScriptType;
import org.eclipse.dltk.debug.core.model.IScriptType;
import org.eclipse.dltk.debug.core.model.IScriptTypeFactory;


public class TclTypeFactory implements IScriptTypeFactory {
	private static final String[] atomicTypes = { "string" };
	
	public TclTypeFactory() {
		
	}

	public IScriptType buildType(String type) {
		for (int i = 0; i < atomicTypes.length; ++i) {
			if (atomicTypes[i].equals(type)) {
				return new AtomicScriptType(type);
			}
		}

		return new ComplexScriptType(type);
	}
}