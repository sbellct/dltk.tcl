package org.eclipse.dltk.tcl.core;

import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.ast.declarations.ModuleDeclaration;
import org.eclipse.dltk.ast.references.SimpleReference;
import org.eclipse.dltk.compiler.problem.DefaultProblem;
import org.eclipse.dltk.compiler.problem.IProblemReporter;
import org.eclipse.dltk.compiler.problem.ProblemSeverity;

public abstract class AbstractTclCommandProcessor implements
		ITclCommandProcessor {
	private ModuleDeclaration moduleDeclaration;
	private Object parameter;

	public ModuleDeclaration getModuleDeclaration() {
		return moduleDeclaration;
	}

	public void report(ITclParser parser, String message, ASTNode node,
			ProblemSeverity severity) {
		this.report(parser, message, node.sourceStart(), node.sourceEnd(),
				severity);
	}

	public void report(ITclParser parser, String message, int start, int end,
			ProblemSeverity severity) {
		IProblemReporter problemReporter = parser.getProblemReporter();
		if (problemReporter == null) {
			return;
		}
		if (TclPlugin.REPORT_PARSER_PROBLEMS) {
			problemReporter.reportProblem(new DefaultProblem(message, 0, null,
					severity, start, end, parser.getCodeModel()
							.getLineNumberOfOffset(start)));
		}
	}

	public void addToParent(ASTNode parent, ASTNode node) {
		if (parent == null) {
			return;
		}
		TclParseUtil.addToDeclaration(parent, node);
	}

	public void setCurrentASTTree(ModuleDeclaration module) {
		this.moduleDeclaration = module;
	}

	public void setDetectedParameter(Object parameter) {
		this.parameter = parameter;
	}

	public Object getDetectedParameter() {
		return this.parameter;
	}

	public static String extractSimpleReference(ASTNode node) {
		if (node instanceof SimpleReference) {
			return ((SimpleReference) node).getName();
		}
		return null;
	}
}
