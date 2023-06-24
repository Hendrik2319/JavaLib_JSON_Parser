package net.schwarzbaer.java.lib.jsonparser.gui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

public class TreeNodeRenderer extends DefaultTreeCellRenderer
{
	private static final long serialVersionUID = -6336890375902571997L;

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected, boolean isExpanded, boolean isLeaf, int row, boolean hasFocus)
	{
		super.getTreeCellRendererComponent(tree, value, isSelected, isExpanded, isLeaf, row, hasFocus);
		
		if (value instanceof AbstractTreeNode) {
			AbstractTreeNode treeNode = (AbstractTreeNode) value;
			if (!isSelected) {
				Color color = treeNode.getColor();
				setForeground(color!=null ? color : tree.getForeground());
			}
			Icon icon = treeNode.getIcon();
			if (icon!=null) setIcon(icon);
			
		} else {
			if (!isSelected) setForeground(tree.getForeground());
			//setIcon(null);
		}
		
		return this;
	}
}
