package org.eclipse.dltk.xotcl.internal.core.parser.processors.xotcl;

import org.eclipse.dltk.ast.ASTListNode;
import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.ast.declarations.TypeDeclaration;
import org.eclipse.dltk.ast.expressions.Expression;
import org.eclipse.dltk.ast.references.SimpleReference;
import org.eclipse.dltk.compiler.problem.ProblemSeverities;
import org.eclipse.dltk.tcl.ast.TclStatement;
import org.eclipse.dltk.tcl.core.AbstractTclCommandProcessor;
import org.eclipse.dltk.tcl.core.ITclParser;
import org.eclipse.dltk.xotcl.core.ast.xotcl.XOTclProcCallStatement;

public class XOTclClassProcCallProcessor extends AbstractTclCommandProcessor {

	public XOTclClassProcCallProcessor() {
	}

	public ASTNode process(TclStatement statement, ITclParser parser, 
			ASTNode parent) {
		Object param = this.getDetectedParameter();
		if( param == null || !(param instanceof TypeDeclaration)) {
			return null;
		}

		TypeDeclaration type = (TypeDeclaration) param;
		Expression nameExpr = statement.getAt(1);
		if( !(nameExpr instanceof SimpleReference ) ) {
			this.report(parser, "A class proc name expected.", nameExpr, ProblemSeverities.Error);
			return null;
		}
		SimpleReference name = (SimpleReference)nameExpr;
		ASTListNode args = null;
		if(statement.getCount() > 2) {
			args = new ASTListNode(name.sourceEnd() + 1, statement.sourceEnd());
			for (int i = 2; i < statement.getCount(); i++) {
				args.addNode(statement.getAt(i));
			}
		}
		XOTclProcCallStatement call = new XOTclProcCallStatement(name, type, args);
		call.setInstNameRef((SimpleReference)statement.getAt(0));
		call.setStart(statement.sourceStart());
		call.setEnd(statement.sourceEnd());
		this.addToParent(parent, call);
		return call;
	}

}
