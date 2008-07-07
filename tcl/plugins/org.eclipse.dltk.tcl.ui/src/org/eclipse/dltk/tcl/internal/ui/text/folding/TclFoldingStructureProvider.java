/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.tcl.internal.ui.text.folding;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.ILog;
import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.ast.declarations.MethodDeclaration;
import org.eclipse.dltk.ast.declarations.ModuleDeclaration;
import org.eclipse.dltk.ast.declarations.TypeDeclaration;
import org.eclipse.dltk.ast.expressions.Expression;
import org.eclipse.dltk.ast.parser.ISourceParser;
import org.eclipse.dltk.ast.references.SimpleReference;
import org.eclipse.dltk.ast.statements.Block;
import org.eclipse.dltk.ast.statements.Statement;
import org.eclipse.dltk.core.DLTKLanguageManager;
import org.eclipse.dltk.tcl.ast.ITclStatementLookLike;
import org.eclipse.dltk.tcl.ast.TclStatement;
import org.eclipse.dltk.tcl.ast.expressions.TclBlockExpression;
import org.eclipse.dltk.tcl.core.TclNature;
import org.eclipse.dltk.tcl.core.ast.IfStatement;
import org.eclipse.dltk.tcl.core.ast.TclCatchStatement;
import org.eclipse.dltk.tcl.core.ast.TclForStatement;
import org.eclipse.dltk.tcl.core.ast.TclForeachStatement;
import org.eclipse.dltk.tcl.core.ast.TclSwitchStatement;
import org.eclipse.dltk.tcl.core.ast.TclWhileStatement;
import org.eclipse.dltk.tcl.internal.ui.TclUI;
import org.eclipse.dltk.tcl.internal.ui.text.TclPartitionScanner;
import org.eclipse.dltk.tcl.ui.TclPreferenceConstants;
import org.eclipse.dltk.tcl.ui.text.TclPartitions;
import org.eclipse.dltk.ui.text.folding.AbstractASTFoldingStructureProvider;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.rules.IPartitionTokenScanner;

/**
 */
public class TclFoldingStructureProvider extends
		AbstractASTFoldingStructureProvider {

	// ~ Instance fields

	private List fBlockExcludeList = new ArrayList();

	/* preferences */
	private int fBlockFolding = 0;
	private List fBlockIncludeList = new ArrayList();
	private boolean fInitCollapseBlocks = true;
	private boolean fInitCollapseComments = true;
	private boolean fInitCollapseHeaderComments = false;
	private boolean fInitCollapseNamespaces = true;

	// ~ Methods

	protected CodeBlock[] getCodeBlocks(String code, int offset) {
		/*
		 * if an ASTVisitor implementation is created for this, just override
		 * getFoldingVisitor() and remove this method
		 */
		ISourceParser pp = null;
		pp = DLTKLanguageManager.getSourceParser(TclNature.NATURE_ID);
		ModuleDeclaration md = pp.parse(null, code.toCharArray(), null);
		List statements = md.getStatements();
		if (statements == null) {
			return new CodeBlock[0];
		}

		List result = new ArrayList();
		traverse(result, statements, offset, code);

		return (CodeBlock[]) result.toArray(new CodeBlock[result.size()]);
	}

	private void checkStatement(String code, int offset, List result,
			Statement sst) {
		if (sst instanceof TclStatement) {
			TclStatement statement = (TclStatement) sst;
			result.add(new CodeBlock(statement, new Region(offset
					+ statement.sourceStart(), statement.sourceEnd()
					- statement.sourceStart())));

			Iterator si = statement.getExpressions().iterator();
			while (si.hasNext()) {
				Expression ex = (Expression) si.next();
				if (ex instanceof TclBlockExpression) {
					TclBlockExpression be = (TclBlockExpression) ex;
					try {
						String newContents = code.substring(
								be.sourceStart() + 1, be.sourceEnd() - 1);
						CodeBlock[] cb = getCodeBlocks(newContents, offset
								+ be.sourceStart() + 1);
						for (int j = 0; j < cb.length; j++) {
							result.add(cb[j]);
						}
					} catch (StringIndexOutOfBoundsException e) {
					}
				}
			}
		}
	}

	private class TclFoldBlock extends Block {
		public TclFoldBlock(Block block) {
			super(block.sourceStart(), block.sourceEnd());
			this.getStatements().addAll(block.getStatements());
		}
	}

	private void traverse(List result, List statements, int offset, String code) {
		for (Iterator iterator = statements.iterator(); iterator.hasNext();) {
			ASTNode node = (ASTNode) iterator.next();
			if (node instanceof TclStatement) {
				checkStatement(code, offset, result, (Statement) node);
				continue;
			}
			boolean fold = false;
			List children = null;
			if (node instanceof TypeDeclaration) {
				TypeDeclaration statement = (TypeDeclaration) node;
				children = statement.getStatements();
				fold = true;
			} else if (node instanceof MethodDeclaration) {
				MethodDeclaration statement = (MethodDeclaration) node;
				children = statement.getStatements();
				fold = true;
			} else if (node instanceof IfStatement) {
				fold = true;
				children = new ArrayList();
				IfStatement statement = (IfStatement) node;
				Statement thenNode = statement.getThen();
				if (thenNode instanceof Block) {
					children.addAll(((Block) thenNode).getStatements());
				}
				ASTNode elseNode = statement.getElse();
				if (elseNode instanceof Block) {
					children.addAll(((Block) elseNode).getStatements());
				}
			} else if (node instanceof TclCatchStatement) {
				fold = true;
				TclCatchStatement statement = (TclCatchStatement) node;
				children = statement.getStatements();
			} else if (node instanceof TclSwitchStatement) {
				fold = true;
				TclSwitchStatement statement = (TclSwitchStatement) node;
				Block alts = statement.getAlternatives();
				List childs = alts.getStatements();
				children = new ArrayList();
				for (int i = 0; i < childs.size(); i++) {
					ASTNode child = (ASTNode) childs.get(i);
					if (child instanceof Block) {
						result.add(new CodeBlock(
								new TclFoldBlock((Block) child), new Region(
										offset + child.sourceStart(), child
												.sourceEnd()
												- child.sourceStart())));
						children.addAll(((Block) child).getStatements());
					}
				}
			} else if (node instanceof TclWhileStatement) {
				fold = true;
				TclWhileStatement statement = (TclWhileStatement) node;
				children = statement.getBlock().getStatements();
			} else if (node instanceof TclForeachStatement) {
				fold = true;
				TclForeachStatement statement = (TclForeachStatement) node;
				children = statement.getBlock().getStatements();
			} else if (node instanceof TclForStatement) {
				fold = true;
				TclForStatement statement = (TclForStatement) node;
				children = statement.getBlock().getStatements();
			}
			if (fold) {
				result.add(new CodeBlock(node, new Region(offset
						+ node.sourceStart(), node.sourceEnd()
						- node.sourceStart())));
			}
			if (children != null && children.size() > 0) {
				traverse(result, children, offset, code);
			}
		}
	}

	/*
	 * @see
	 * org.eclipse.dltk.ui.text.folding.AbstractASTFoldingStructureProvider#
	 * getCommentPartition()
	 */
	protected String getCommentPartition() {
		return TclPartitions.TCL_COMMENT;
	}

	/*
	 * @see
	 * org.eclipse.dltk.ui.text.folding.AbstractASTFoldingStructureProvider#
	 * getLog()
	 */
	protected ILog getLog() {
		return TclUI.getDefault().getLog();
	}

	/*
	 * @see
	 * org.eclipse.dltk.ui.text.folding.AbstractASTFoldingStructureProvider#
	 * getPartition()
	 */
	protected String getPartition() {
		return TclPartitions.TCL_PARTITIONING;
	}

	/*
	 * @see
	 * org.eclipse.dltk.ui.text.folding.AbstractASTFoldingStructureProvider#
	 * getPartitionScanner()
	 */
	protected IPartitionTokenScanner getPartitionScanner() {
		return new TclPartitionScanner();
	}

	/*
	 * @see
	 * org.eclipse.dltk.ui.text.folding.AbstractASTFoldingStructureProvider#
	 * getPartitionTypes()
	 */
	protected String[] getPartitionTypes() {
		return TclPartitions.TCL_PARTITION_TYPES;
	}

	/*
	 * @see
	 * org.eclipse.dltk.ui.text.folding.AbstractASTFoldingStructureProvider#
	 * getNatureId()
	 */
	protected String getNatureId() {
		return TclNature.NATURE_ID;
	}

	/*
	 * @see
	 * org.eclipse.dltk.ui.text.folding.AbstractASTFoldingStructureProvider#
	 * initializePreferences(org.eclipse.jface.preference.IPreferenceStore)
	 */
	protected void initializePreferences(IPreferenceStore store) {
		super.initializePreferences(store);
		fBlockFolding = store
				.getInt(TclPreferenceConstants.EDITOR_FOLDING_BLOCKS);

		String t = store
				.getString(TclPreferenceConstants.EDITOR_FOLDING_EXCLUDE_LIST);
		String[] items = t.split(",");
		fBlockExcludeList.clear();
		for (int i = 0; i < items.length; i++) {
			if (items[i].trim().length() > 0) {
				fBlockExcludeList.add(items[i]);
			}
		}

		t = store.getString(TclPreferenceConstants.EDITOR_FOLDING_INCLUDE_LIST);
		items = t.split(",");
		fBlockIncludeList.clear();
		for (int i = 0; i < items.length; i++) {
			if (items[i].trim().length() > 0) {
				fBlockIncludeList.add(items[i]);
			}
		}

		fFoldNewLines = store
				.getBoolean(TclPreferenceConstants.EDITOR_FOLDING_COMMENTS_WITH_NEWLINES);
		fInitCollapseBlocks = store
				.getBoolean(TclPreferenceConstants.EDITOR_FOLDING_INIT_BLOCKS);
		fInitCollapseComments = store
				.getBoolean(TclPreferenceConstants.EDITOR_FOLDING_INIT_COMMENTS);
		fInitCollapseHeaderComments = store
				.getBoolean(TclPreferenceConstants.EDITOR_FOLDING_INIT_HEADER_COMMENTS);
		fInitCollapseNamespaces = store
				.getBoolean(TclPreferenceConstants.EDITOR_FOLDING_INIT_NAMESPACES);
	}

	protected boolean initiallyCollapse(ASTNode s,
			FoldingStructureComputationContext ctx) {
		if (s instanceof TclStatement || s instanceof ITclStatementLookLike) {
			TclStatement statement = null;
			if (s instanceof ITclStatementLookLike) {
				statement = ((ITclStatementLookLike) s).getStatement();
			} else {
				statement = (TclStatement) s;
			}
			if (statement == null) {
				return false;
			}
			if (!(statement.getAt(0) instanceof SimpleReference)) {
				return false;
			}

			String name = null;
			name = ((SimpleReference) statement.getAt(0)).getName();
			if (name.equals("namespace")) {
				return ctx.allowCollapsing() && fInitCollapseNamespaces;
			}

			return ctx.allowCollapsing() && fInitCollapseBlocks;
		}

		return false;
	}

	/*
	 * @see
	 * org.eclipse.dltk.ui.text.folding.AbstractASTFoldingStructureProvider#
	 * initiallyCollapseComments
	 * (org.eclipse.dltk.ui.text.folding.AbstractASTFoldingStructureProvider
	 * .FoldingStructureComputationContext)
	 */
	protected boolean initiallyCollapseComments(IRegion commentRegion,
			FoldingStructureComputationContext ctx) {
		if (ctx.allowCollapsing()) {
			return isHeaderRegion(commentRegion, ctx) ? fInitCollapseHeaderComments
					: fInitCollapseComments;
		}
		return false;
	}

	/*
	 * @see
	 * org.eclipse.dltk.ui.text.folding.AbstractASTFoldingStructureProvider#
	 * mayCollapse(org.eclipse.dltk.ast.statements.Statement,
	 * org.eclipse.dltk.ui.text.folding.AbstractASTFoldingStructureProvider.
	 * FoldingStructureComputationContext)
	 */
	protected boolean canFold(String name) {
		switch (fBlockFolding) {
		case TclPreferenceConstants.EDITOR_FOLDING_BLOCKS_OFF: {
			if (name.equals("proc") || name.equals("namespace")) {
				return true;
			}

			return false;
		}
		case TclPreferenceConstants.EDITOR_FOLDING_BLOCKS_INCLUDE: {
			if (fBlockIncludeList.contains(name)) {
				return true;
			}

			return false;
		}
		case TclPreferenceConstants.EDITOR_FOLDING_BLOCKS_EXCLUDE: {
			if (fBlockExcludeList.contains(name)) {
				return false;
			}

			return true;
		}
		}
		return false;
	}

	protected boolean mayCollapse(ASTNode s,
			FoldingStructureComputationContext ctx) {
		if (s instanceof TypeDeclaration) {
			return canFold("namespace");
		} else if (s instanceof MethodDeclaration) {
			return canFold("proc");
		} else if (s instanceof IfStatement) {
			return canFold("if");
		} else if (s instanceof TclSwitchStatement) {
			return canFold("swith");
		} else if (s instanceof TclFoldBlock) {
			return canFold("swith");
		} else if (s instanceof TclCatchStatement) {
			return canFold("catch");
		} else if (s instanceof TclWhileStatement) {
			return canFold("while");
		} else if (s instanceof TclForeachStatement) {
			return canFold("foreach");
		} else if (s instanceof TclForStatement) {
			return canFold("for");
		} else if (s instanceof TclStatement) {
			TclStatement statement = (TclStatement) s;
			if (!(statement.getAt(0) instanceof SimpleReference)) {
				return false;
			}

			String name = null;
			name = ((SimpleReference) statement.getAt(0)).getName();
			return canFold(name);
		}

		return false;
	}

}
