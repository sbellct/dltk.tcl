package org.eclipse.dltk.tcl.internal.core.parser.processors.tcl;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.ast.Modifiers;
import org.eclipse.dltk.ast.declarations.Argument;
import org.eclipse.dltk.ast.declarations.MethodDeclaration;
import org.eclipse.dltk.ast.expressions.Expression;
import org.eclipse.dltk.ast.expressions.StringLiteral;
import org.eclipse.dltk.ast.references.SimpleReference;
import org.eclipse.dltk.ast.statements.Block;
import org.eclipse.dltk.compiler.problem.ProblemSeverities;
import org.eclipse.dltk.tcl.ast.TclStatement;
import org.eclipse.dltk.tcl.ast.expressions.TclBlockExpression;
import org.eclipse.dltk.tcl.ast.expressions.TclExecuteExpression;
import org.eclipse.dltk.tcl.core.AbstractTclCommandProcessor;
import org.eclipse.dltk.tcl.core.ITclParser;
import org.eclipse.dltk.tcl.core.TclParseUtil;
import org.eclipse.dltk.tcl.internal.core.codeassist.TclVisibilityUtils;
import org.eclipse.dltk.tcl.internal.parser.OldTclParserUtils;

public class TclProcProcessor extends AbstractTclCommandProcessor {

	public ASTNode process(TclStatement statement, ITclParser parser,
			ASTNode parent) {
		if (statement.getCount() < 4) {
			this.report(parser,
					Messages.TclProcProcessor_Wrong_Number_of_Arguments,
					statement.sourceStart(), statement.sourceEnd(),
					ProblemSeverities.Error);
			return null;
		}
		Expression procName = statement.getAt(1);

		String sName = null;
		if (procName instanceof SimpleReference) {
			sName = ((SimpleReference) procName).getName();
		} else if (procName instanceof TclBlockExpression) {
			sName = TclParseUtil.extractBraces(((TclBlockExpression) procName)
					.getBlock());
		} else if (procName instanceof TclExecuteExpression) {
			sName = ((TclExecuteExpression) procName).getExpression();
		} else if (procName instanceof StringLiteral) {
			sName = ((StringLiteral) procName).getValue();
			sName = sName.substring(1, sName.length() - 1);
		}
		if (sName == null || sName.length() == 0) {
			this.report(parser, Messages.TclProcProcessor_Empty_Proc_Name,
					statement.sourceStart(), statement.sourceEnd(),
					ProblemSeverities.Error);
			return null;
		}
		Expression procArguments = statement.getAt(2);
		Expression procCode = statement.getAt(3);

		List arguments = null;
		if (procArguments instanceof TclBlockExpression) {
			List/* < Statement > */st = null;

			st = ((TclBlockExpression) procArguments).parseBlockSimple(false);

			arguments = OldTclParserUtils.parseArguments(st);
		}
		if (procArguments instanceof SimpleReference) {
			arguments = new ArrayList();
			Argument a = new Argument((SimpleReference) procArguments,
					procArguments.sourceStart(), null, 0);
			arguments.add(a);
		}

		MethodDeclaration method = new MethodDeclaration(statement
				.sourceStart(), statement.sourceEnd());
		method.setName(sName);
		method.setNameStart(procName.sourceStart());
		method.setNameEnd(procName.sourceEnd());
		method.acceptArguments(arguments);
		if (TclVisibilityUtils.isPrivate(sName)) {
			method.setModifier(Modifiers.AccPrivate);
		} else {
			method.setModifier(Modifiers.AccPublic);
		}

		if (procCode instanceof Block) {
			method.acceptBody((Block) procCode);
		} else if (procCode instanceof TclBlockExpression) {
			Block block = new Block(procCode.sourceStart(), procCode.sourceEnd());
			String content = ((TclBlockExpression) procCode).getBlock();
			if (content.startsWith("{") && content.endsWith("}")) {
				content = content.substring(1, content.length() - 1);
			}
			method.acceptBody(block);
			parser.parse(content, procCode.sourceStart() + 1
					- parser.getStartPos(), block);
		}
		this.addToParent(parent, method);
		String methodFQN = TclParseUtil.getElementFQN(method, "::",
				getModuleDeclaration());
		int pos = methodFQN.lastIndexOf("::");
		if (pos != -1) {
			methodFQN = methodFQN.substring(0, pos);
			method.setDeclaringTypeName(methodFQN);
		}
		return method;
	}
}
