package org.eclipse.dltk.xotcl.internal.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.ast.ASTVisitor;
import org.eclipse.dltk.ast.declarations.FieldDeclaration;
import org.eclipse.dltk.ast.declarations.TypeDeclaration;
import org.eclipse.dltk.ast.expressions.Expression;
import org.eclipse.dltk.ast.references.SimpleReference;
import org.eclipse.dltk.ast.statements.Statement;
import org.eclipse.dltk.codeassist.complete.CompletionNodeFound;
import org.eclipse.dltk.core.CompletionProposal;
import org.eclipse.dltk.core.CompletionRequestor;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IMethod;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.IType;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.core.mixin.IMixinRequestor;
import org.eclipse.dltk.tcl.ast.TclStatement;
import org.eclipse.dltk.tcl.core.TclParseUtil;
import org.eclipse.dltk.tcl.core.extensions.ICompletionExtension;
import org.eclipse.dltk.tcl.internal.core.codeassist.FieldNameProvider;
import org.eclipse.dltk.tcl.internal.core.codeassist.TclCompletionEngine;
import org.eclipse.dltk.tcl.internal.core.codeassist.TclResolver;
import org.eclipse.dltk.tcl.internal.core.codeassist.completion.CompletionOnKeywordArgumentOrFunctionArgument;
import org.eclipse.dltk.tcl.internal.core.codeassist.completion.CompletionOnKeywordOrFunction;
import org.eclipse.dltk.tcl.internal.core.codeassist.completion.CompletionOnVariable;
import org.eclipse.dltk.tcl.internal.core.codeassist.completion.TclCompletionParser;
import org.eclipse.dltk.xotcl.core.IXOTclModifiers;
import org.eclipse.dltk.xotcl.core.XOTclParseUtil;
import org.eclipse.dltk.xotcl.core.ast.xotcl.XOTclExInstanceVariable;
import org.eclipse.dltk.xotcl.core.ast.xotcl.XOTclInstanceVariable;
import org.eclipse.dltk.xotcl.core.ast.xotcl.XOTclMethodCallStatement;
import org.eclipse.dltk.xotcl.core.ast.xotcl.XOTclProcCallStatement;
import org.eclipse.dltk.xotcl.internal.core.search.mixin.model.XOTclClass;
import org.eclipse.dltk.xotcl.internal.core.search.mixin.model.XOTclClassInstance;
import org.eclipse.dltk.xotcl.internal.core.search.mixin.model.XOTclInstProc;
import org.eclipse.dltk.xotcl.internal.core.search.mixin.model.XOTclProc;

public class XOTclCompletionExtension implements ICompletionExtension {

	private CompletionRequestor requestor;

	public boolean visit(Expression s, TclCompletionParser parser, int position) {
		List exprs = new ArrayList();
		if (s instanceof XOTclMethodCallStatement) {
			XOTclMethodCallStatement pcs = (XOTclMethodCallStatement) s;

			exprs.add(pcs.getInstNameRef());
			exprs.add(pcs.getCallName());
			if (pcs.getArgs() != null) {
				exprs.addAll(pcs.getArgs().getChilds());
			}
			processArgumentCompletion(s, exprs, parser, position);
		}
		return false;
	}

	public boolean visit(Statement s, TclCompletionParser parser, int position) {
		List exprs = new ArrayList();
		if (s instanceof XOTclProcCallStatement) {
			XOTclProcCallStatement pcs = (XOTclProcCallStatement) s;

			exprs.add(pcs.getInstNameRef());
			exprs.add(pcs.getCallName());
			if (pcs.getArguments() != null) {
				exprs.addAll(pcs.getArguments().getChilds());
			}
			processArgumentCompletion(s, exprs, parser, position);
		}
		return false;
	}

	private void processArgumentCompletion(Statement s, List exprs,
			TclCompletionParser parser, int position) {
		TclStatement statement = new TclStatement(exprs);
		statement.setStart(s.sourceStart());
		statement.setEnd(s.sourceEnd());

		ASTNode completionNode = null;
		for (int i = 0; i < exprs.size(); ++i) {
			ASTNode n = (ASTNode) exprs.get(i);
			if (n.sourceStart() <= position && n.sourceEnd() >= position) {
				completionNode = n;
			}
		}
		String token = "";
		if (completionNode != null && completionNode instanceof SimpleReference) {
			token = ((SimpleReference) completionNode).getName();
		}
		String[] keywords;
		if (token == null) {
			keywords = parser.checkKeywords("", TclCompletionParser.MODULE);
		} else {
			keywords = parser.checkKeywords(token, TclCompletionParser.MODULE);
		}
		if (completionNode != null) {
			ASTNode nde = new CompletionOnKeywordArgumentOrFunctionArgument(
					token, completionNode, statement, keywords);
			parser.setAssistNodeParent(TclParseUtil.getPrevParent(parser
					.getModule(), s));
			throw new CompletionNodeFound(nde, null);
			// ((TypeDeclaration)inNode).scope
		} else {
			ASTNode nde = new CompletionOnKeywordArgumentOrFunctionArgument(
					token, statement, keywords, position);
			parser.setAssistNodeParent(TclParseUtil.getPrevParent(parser
					.getModule(), s));
			throw new CompletionNodeFound(nde, null);
			// ((TypeDeclaration)inNode).scope
		}
	}

	public void completeOnKeywordOrFunction(CompletionOnKeywordOrFunction key,
			ASTNode astNodeParent, TclCompletionEngine engine) {
		if (!engine.getRequestor().isIgnored(CompletionProposal.TYPE_REF)) {
			Set methodNames = new HashSet();
			char[] token = key.getToken();
			token = engine.removeLastColonFromToken(token);
			findLocalXOTclClasses(token, methodNames, astNodeParent, engine);
			// findLocalClasses
			findXOTclClasses(token, methodNames, engine);
		}
		if (!engine.getRequestor().isIgnored(CompletionProposal.FIELD_REF)) {
			Set methodNames = new HashSet();
			char[] token = key.getToken();
			token = engine.removeLastColonFromToken(token);
			// findLocalClassInstances
			findLocalXOTclClassInstances(token, methodNames, astNodeParent,
					engine);
			findXOTclClassInstances(token, methodNames, engine);
		}
	}

	private void findLocalXOTclClasses(char[] token, Set methodNames,
			ASTNode astNodeParent, TclCompletionEngine engine) {
		ASTNode parent = TclParseUtil.getScopeParent(engine.getAssistParser()
				.getModule(), astNodeParent);
		// We need to process all xotcl classes.

		Set classes = new HashSet();

		findXOTclClassessIn(parent, classes, engine);
		engine.removeSameFrom(methodNames, classes, new String(token));
		engine.findTypes(token, true, engine.toList(classes));
		methodNames.addAll(classes);
	}

	private void findLocalXOTclClassInstances(char[] token, Set methodNames,
			ASTNode astNodeParent, TclCompletionEngine engine) {
		ASTNode parent = TclParseUtil.getScopeParent(engine.getAssistParser()
				.getModule(), astNodeParent);
		// We need to process all xotcl classes.

		Set classes = new HashSet();

		findXOTclClassInstancesIn(parent, classes, engine);
		final String tok = new String(token);
		engine.removeSameFrom(methodNames, classes, tok);
		engine.findFields(token, true, engine.toList(classes),
				new FieldNameProvider(tok));
		methodNames.addAll(classes);
		// Also use not fully qualified names
		// String elementFQN = TclParseUtil.getElementFQN(parent, "::",
		// parser.getModule());
	}

	private void findXOTclClassessIn(ASTNode parent, Set classes,
			final TclCompletionEngine engine) {
		// List statements = TclASTUtil.getStatements(parent);
		final List result = new ArrayList();
		final TclResolver resolver = new TclResolver(engine.getSourceModule(),
				engine.getAssistParser().getModule(), null);

		try {
			engine.getAssistParser().getModule().traverse(new ASTVisitor() {
				// for (Iterator iterator = statements.iterator();
				// iterator.hasNext();) {
				// ASTNode nde = (ASTNode) iterator.next();
				public boolean visit(TypeDeclaration type) {
					if ((type.getModifiers() & IXOTclModifiers.AccXOTcl) != 0
							&& type.sourceStart() < engine
									.getActualCompletionPosition()) {
						// we need to find model element for selected type.
						resolver.searchAddElementsTo(engine.getAssistParser()
								.getModule().getStatements(), type, engine
								.getSourceModule(), result);
					}
					return true;
				}
			});
		} catch (Exception e) {
			if (DLTKCore.DEBUG) {
				e.printStackTrace();
			}
		}
		classes.addAll(result);
	}

	private void findXOTclClassInstancesIn(ASTNode parent, Set classes,
			final TclCompletionEngine engine) {
		// List statements = TclASTUtil.getStatements(parent);
		final List result = new ArrayList();
		final TclResolver resolver = new TclResolver(engine.getSourceModule(),
				engine.getAssistParser().getModule(), null);

		try {
			parent.traverse(new ASTVisitor() {
				public boolean visit(Statement st) {
					if (st instanceof XOTclInstanceVariable
							|| st instanceof XOTclExInstanceVariable) {
						// we need to find model element for selected type.
						resolver.searchAddElementsTo(engine.getAssistParser()
								.getModule().getStatements(), st, engine
								.getSourceModule(), result);
					}
					return true;
				}
			});
		} catch (Exception e) {
			if (DLTKCore.DEBUG) {
				e.printStackTrace();
			}
		}
		classes.addAll(result);
	}

	private void findXOTclClassInstances(char[] token, Set methodNames,
			TclCompletionEngine engine) {
		// IDLTKSearchScope scope = SearchEngine.createWorkspaceScope(toolkit);
		String to_ = new String(token);
		String to = to_;
		if (to.startsWith("::")) {
			to = to.substring(2);
		}
		if (to.length() == 0) {
			return;
		}
		Set elements = new HashSet();
		elements.addAll(methodNames);
		findClassesInstanceFromMixin(elements, to + "*", engine);
		engine.removeSameFrom(methodNames, elements, to);
		engine.findFields(token, true, engine.toList(elements),
				new FieldNameProvider(to_));
		methodNames.addAll(elements);
	}

	private void completionForInstanceVariableMethods(FieldDeclaration var,
			char[] token, Set methodNames, TclCompletionEngine engine) {
		String keyPrefix = null;
		if (var instanceof XOTclInstanceVariable) {
			XOTclInstanceVariable ivar = (XOTclInstanceVariable) var;
			TypeDeclaration declaringType = ivar.getDeclaringType();
			keyPrefix = TclParseUtil.getElementFQN(declaringType,
					IMixinRequestor.MIXIN_NAME_SEPARATOR, engine
							.getAssistParser().getModule());
			if (keyPrefix.startsWith(IMixinRequestor.MIXIN_NAME_SEPARATOR)) {
				keyPrefix = keyPrefix.substring(1);
			}

		} else if (var instanceof XOTclExInstanceVariable) {
			XOTclExInstanceVariable ivar = (XOTclExInstanceVariable) var;
			String className = ivar.getDeclaringClassParameter().getClassName();
			if (className.startsWith("::")) {
				className = className.substring(2);
			}
			keyPrefix = className.replaceAll("::",
					IMixinRequestor.MIXIN_NAME_SEPARATOR);
		}
		Set methods = new HashSet();
		findClassesFromMixin(methods, keyPrefix, engine);
		Set result = new HashSet();
		// replace class name with methods.
		while (methods.size() > 0) {
			IModelElement e = (IModelElement) methods.iterator().next();
			methods.remove(e);
			if (e instanceof IType) {
				IType type = (IType) e;
				try {
					IMethod[] ms = type.getMethods();
					result.addAll(Arrays.asList(ms));
				} catch (ModelException e1) {
					if (DLTKCore.DEBUG) {
						e1.printStackTrace();
					}
				}
				// Add super types information.
				try {
					String[] superClasses = type.getSuperClasses();
					if (superClasses != null) {
						for (int i = 0; i < superClasses.length; i++) {
							String key = superClasses[i];
							if (key.startsWith("::")) {
								key = key.substring(2);
							}
							if (key.length() > 0) {
								findClassesFromMixin(methods, key, engine);
							}
						}
					}
				} catch (ModelException e1) {
					if (DLTKCore.DEBUG) {
						e1.printStackTrace();
					}
				}
			} else if (e instanceof IMethod) {
				result.add(e);
			}

		}
		findMethodsShortName(token, result, methodNames, engine);
		// We need to handle supers

		addKeywords(token, XOTclKeywords.XOTclCommandClassArgs, methodNames,
				engine);
		addKeywords(token, XOTclKeywords.XOTclCommandObjectArgs, methodNames,
				engine);
	}

	// protected boolean methodCanBeAdded(ASTNode nde, TclCompletionEngine
	// engine) {
	// if (nde instanceof XOTclMethodDeclaration) {
	// return false;
	// }
	// return super.methodCanBeAdded(nde);
	// }

	private void completeClassMethods(String name, char[] cs, Set methodNames,
			TclCompletionEngine engine) {
		Set completions = new HashSet();

		if (name.startsWith("::")) {
			name = name.substring(2);
		}
		findClassesFromMixin(completions, name, engine);
		if (completions.size() >= 1) { // We found class with such name, so
			// this is method completion.
			Set methods = new HashSet();
			methods.addAll(methodNames);
			findProcsFromMixin(methods, name + "::*", engine);
			methods.removeAll(methodNames);
			findMethodsShortName(cs, methods, methodNames, engine);
			// We also need to add Object and Class methods
			// We need to add superclass methods
			addKeywords(cs, XOTclKeywords.XOTclCommandClassArgs, methodNames,
					engine);
			addKeywords(cs, XOTclKeywords.XOTclCommandObjectArgs, methodNames,
					engine);
		}
	}

	private void addKeywords(char[] cs, String[] keywords, Set methodNames,
			TclCompletionEngine engine) {
		List k = new ArrayList();
		String token = new String(cs);
		for (int i = 0; i < keywords.length; ++i) {
			String kkw = keywords[i];
			if (kkw.startsWith(token)) {
				if (!methodNames.contains(kkw)) {
					k.add(kkw);
					methodNames.add(kkw);
				}
			}
		}
		String kw[] = (String[]) k.toArray(new String[k.size()]);
		engine.findKeywords(cs, kw, true);
	}

	private void findMethodsShortName(char[] cs, Set methods, Set allMethods,
			TclCompletionEngine engine) {
		List methodsList = engine.toList(methods);
		List methodNames = new ArrayList();
		List remove = new ArrayList();
		for (Iterator iterator = methodsList.iterator(); iterator.hasNext();) {
			IMethod method = (IMethod) iterator.next();
			String methodName = method.getElementName();
			if (!methodNames.contains(methodName)) {
				methodNames.add(methodName);
				allMethods.add(methodName);
			} else {
				remove.add(method);
			}
		}
		for (Iterator iterator = remove.iterator(); iterator.hasNext();) {
			Object object = (Object) iterator.next();
			methodsList.remove(object);
		}
		engine.findMethods(cs, true, methodsList, methodNames);
	}

	protected void findProcsFromMixin(final Set methods, String tok,
			TclCompletionEngine engine) {
		engine.findMixinTclElement(methods, tok, XOTclProc.class);
	}

	protected void findInstProcsFromMixin(final Set methods, String tok,
			TclCompletionEngine engine) {
		engine.findMixinTclElement(methods, tok, XOTclInstProc.class);
	}

	private void findXOTclClasses(char[] token, Set methodNames,
			TclCompletionEngine engine) {

		// IDLTKSearchScope scope = SearchEngine.createWorkspaceScope(toolkit);
		String to_ = new String(token);
		String to = to_;
		if (to.startsWith("::")) {
			to = to.substring(2);
		}
		if (to.length() == 0) {
			return;
		}
		Set methods = new HashSet();
		methods.addAll(methodNames);
		findClassesFromMixin(methods, to + "*", engine);
		methods.removeAll(methodNames);
		engine.removeSameFrom(methodNames, methods, to_);

		engine.findTypes(token, true, engine.toList(methods));
	}

	protected void findClassesFromMixin(final Set completions, String tok,
			TclCompletionEngine engine) {
		engine.findMixinTclElement(completions, tok, XOTclClass.class);
	}

	protected void findClassesInstanceFromMixin(final Set completions,
			String tok, TclCompletionEngine engine) {
		engine.findMixinTclElement(completions, tok, XOTclClassInstance.class);
	}

	private FieldDeclaration searchFieldFromMixin(String name,
			TclCompletionEngine engine) {
		return null;
	}

	public void completeOnKeywordArgumentsOne(String name, char[] token,
			CompletionOnKeywordArgumentOrFunctionArgument compl,
			Set methodNames, TclStatement st, TclCompletionEngine engine) {
		// Check for class and its methods.
		completeClassMethods(name, token, methodNames, engine);

		// Lets find instance with specified name.
		FieldDeclaration var = XOTclParseUtil
				.findXOTclInstanceVariableDeclarationFrom(engine
						.getAssistParser().getModule(), TclParseUtil
						.getScopeParent(engine.getAssistParser().getModule(),
								st), name);
		if (var == null) {
			var = searchFieldFromMixin(name, engine);
		}
		if (var != null) {
			completionForInstanceVariableMethods(var, token, methodNames,
					engine);
		}
	}

	public void setRequestor(CompletionRequestor requestor) {
		this.requestor = requestor;
	}

	public void completeOnVariables(CompletionOnVariable astNode,
			TclCompletionEngine engine) {
	}

	public boolean modelFilter(Set completions, IModelElement modelElement) {
		return true;
	}
}
