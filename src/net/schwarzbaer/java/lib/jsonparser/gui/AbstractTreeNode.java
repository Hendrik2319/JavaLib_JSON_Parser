package net.schwarzbaer.java.lib.jsonparser.gui;

import java.awt.Color;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.tree.TreeNode;

public abstract class AbstractTreeNode implements TreeNode
{
	public interface CachedIcon { Icon getIcon(); }
	
	final TreeNode parent;
	Vector<TreeNode> children;
	final boolean allowsChildren;
	final boolean isLeaf;
	final CachedIcon icon;
	final Color color;

	AbstractTreeNode(TreeNode parent, boolean allowsChildren, boolean isLeaf) {
		this(parent, allowsChildren, isLeaf, null, null);
	}
	AbstractTreeNode(TreeNode parent, boolean allowsChildren, boolean isLeaf, CachedIcon icon) {
		this(parent, allowsChildren, isLeaf, icon, null);
	}
	AbstractTreeNode(TreeNode parent, boolean allowsChildren, boolean isLeaf, CachedIcon icon, Color color) {
		this.parent = parent;
		this.allowsChildren = allowsChildren;
		this.isLeaf = isLeaf;
		this.icon = icon;
		this.color = color;
		children = null;
	}
	
	Icon getIcon() { return icon==null ? null : icon.getIcon(); }
	Color getColor() { return color; }

	@Override public abstract String toString();
	protected abstract Vector<TreeNode> createChildren();

	@Override public TreeNode getParent() { return parent; }
	@Override public boolean getAllowsChildren() { return allowsChildren; }
	@Override public boolean isLeaf() { return isLeaf; }

	@Override public int getChildCount() {
		if (children==null) children = createChildren();
		return children.size();
	}

	@Override public TreeNode getChildAt(int childIndex) {
		if (children==null) children = createChildren();
		return childIndex<0 || childIndex>=children.size() ? null : children.get(childIndex);
	}

	@Override public int getIndex(TreeNode node) {
		if (children==null) children = createChildren();
		return children.indexOf(node);
	}

	@Override public Enumeration<TreeNode> children() {
		if (children==null) children = createChildren();
		return children.elements();
	}
}
