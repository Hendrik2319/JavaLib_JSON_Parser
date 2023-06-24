package net.schwarzbaer.java.lib.jsonparser.gui;

import java.awt.Color;
import java.io.InputStream;
import java.util.Comparator;
import java.util.Vector;

import javax.swing.tree.TreeNode;

import net.schwarzbaer.java.lib.jsonparser.JSON_Data;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.NamedValueExtra;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.ValueExtra;

public class AbstractJsonTreeNode<NV extends NamedValueExtra, V extends ValueExtra, SelfType extends AbstractJsonTreeNode<NV,V,?>> extends AbstractTreeNode
{
	private final String name;
	private final JSON_Data.Value<NV, V> value;
	private final boolean showNamedValuesSorted;
	private final Factory<NV,V,SelfType> factory;

	public AbstractJsonTreeNode(JSON_Data.Value<NV,V> value, boolean showNamedValuesSorted, Factory<NV,V,SelfType> factory) {
		this(null, null, value, showNamedValuesSorted, factory);
	}
	public AbstractJsonTreeNode(AbstractJsonTreeNode<NV,V,?> parent, String name, JSON_Data.Value<NV,V> value, boolean showNamedValuesSorted, Factory<NV,V,SelfType> factory) {
		//super(parent, allowsChildren_, isLeaf_, icon_, color_);
		super(parent, factory.allowsChildren(value), factory.isLeaf(value), factory.getIcon(value), factory.getColor(value));
		this.name = name;
		this.value = value;
		this.showNamedValuesSorted = showNamedValuesSorted;
		this.factory = factory;
	}
	
//	public String[] getPath() { return PathElement.toStringArr(getPath(0)); }
	public String   getName() { return name; }
	public String   getValue() { return getValueString(); }
	
//	private PathElement[] getPath(int length) {
//		PathElement[] path;
//		
//		if (parent instanceof AbstractJsonTreeNode) {
//			AbstractJsonTreeNode<NV,V> jsonParent = (AbstractJsonTreeNode<NV,V>) parent;
//			
//			PathElement pe =
//					name!=null 
//					? new PathElement(name)
//					: new PathElement(jsonParent.getIndexOf(this));
//			
//			path = jsonParent.getPath(length+1);
//			path[path.length-length-1] = pe;
//			
//			} else {
//			PathElement pe =
//					name!=null 
//					? new PathElement(name)
//					: new PathElement("<root>");
//			
//				path = new PathElement[length+1];
//			path[0] = pe;
//			}
//		
//		return path;
//	}

//	private int getIndexOf(AbstractJsonTreeNode<NV,V> jsonTreeNode) {
//		return children.indexOf(jsonTreeNode);
//	}

//	static class PathElement {
//		final Integer index;
//		final String name;
//		PathElement(int index) {
//			this.name = null;
//			this.index = index;
//		}
//		PathElement(String name) {
//			this.name = name;
//			this.index = null;
//		}
//		static String[] toStringArr(PathElement[] path) {
//			return Arrays
//					.stream(path)
//					.<String>map(pe->pe.name!=null ? pe.name : String.format("[%d]", pe.index))
//					.toArray(String[]::new);
//		}
//	}
	
	@Override
	protected Vector<TreeNode> createChildren() {
		Vector<TreeNode> children = new Vector<>();
		
		switch (value.type) {
		case Object:
			Vector<JSON_Data.NamedValue<NV,V>> values;
			if (showNamedValuesSorted) {
				values = new Vector<>(value.castToObjectValue().value);
				values.sort(Comparator.<JSON_Data.NamedValue<NV,V>,String>comparing(nv->nv.name));
			} else
				values = value.castToObjectValue().value;
			
			for (JSON_Data.NamedValue<NV,V> nv : values)
				children.add(factory.createNode(this, nv.name, nv.value, showNamedValuesSorted));
			break;
			
		case Array:
			for (JSON_Data.Value<NV, V> v : value.castToArrayValue().value)
				children.add(factory.createNode(this, null, v, showNamedValuesSorted));
			break;
			
		default:
			break;
		}
		
		return children;
	}
	
	@Override
	public String toString() {
		if (name==null) return getValueString();
		return String.format("%s: %s", name, getValueString());
	}
	
	private String getValueString() {
		switch(value.type) {
		case String : return String.format("\"%s\"", value.castToStringValue ().value);
		case Bool   : return String.format("%s"    , value.castToBoolValue   ().value);
		case Float  : return                      ""+value.castToFloatValue  ().value ;
		case Integer: return String.format("%d"    , value.castToIntegerValue().value);
		case Array  : return String.format("[%d]"  , value.castToArrayValue  ().value.size());
		case Object : return String.format("{%d}"  , value.castToObjectValue ().value.size());
		case Null   : return "<null>";
		}
		return value.toString();
	}
	
	public static abstract class Factory<NV extends NamedValueExtra, V extends ValueExtra, TreeNodeType extends AbstractJsonTreeNode<NV, V, ?>>
	{
		public TreeNodeType createRootNode(JSON_Data.Value<NV,V> value, boolean showNamedValuesSorted)
		{
			return createNode_impl(null, null, value, showNamedValuesSorted, this);
		}
		public TreeNodeType createNode(AbstractJsonTreeNode<NV, V, ?> parent, String name, JSON_Data.Value<NV,V> value, boolean showNamedValuesSorted)
		{
			return createNode_impl(parent, name, value, showNamedValuesSorted, this); 
		}
		
		protected abstract TreeNodeType createNode_impl(AbstractJsonTreeNode<NV, V, ?> parent, String name, JSON_Data.Value<NV, V> value, boolean showNamedValuesSorted, Factory<NV,V,TreeNodeType> factory);
//		{
//			return new AbstractJsonTreeNode<NV,V>(parent, name, value, showNamedValuesSorted, factory);
//		}
		
		protected boolean allowsChildren(JSON_Data.Value<NV, V> value) {
			if (value!=null && value.type!=null)
				switch (value.type) {
				case Object :
				case Array  :
					return true;
				case String :
				case Float  :
				case Integer:
				case Bool   :
				case Null   :
					break;
				}
			return false;
		}
		
		protected boolean isLeaf(JSON_Data.Value<NV, V> value) {
			if (value!=null && value.type!=null)
				switch (value.type) {
				case Object : return value.castToObjectValue().value.isEmpty();
				case Array  : return value.castToArrayValue ().value.isEmpty();
				case String :
				case Float  :
				case Integer:
				case Bool   :
				case Null   :
					return true;
				}
			return true;
		}
		
		protected AbstractTreeNode.CachedIcon getIcon(JSON_Data.Value<NV, V> value) {
			return null;
		}
		
		protected ExampleTreeIcons getExampleTreeIcon(JSON_Data.Value<NV, V> value) {
			if (value!=null && value.type!=null)
				switch (value.type) {
				case Object : return ExampleTreeIcons.Object;
				case Array  : return ExampleTreeIcons.Array ;
				case String : return ExampleTreeIcons.String;
				case Float  : return ExampleTreeIcons.Number;
				case Integer: return ExampleTreeIcons.Number;
				case Bool   : return ExampleTreeIcons.Bool  ;
				case Null   : return ExampleTreeIcons.Null  ;
				}
			return null;
		}
		
		protected Color getColor(Value<NV, V> value)
		{
			return null;
		}
	}
	
	public enum ExampleTreeIcons
	{
		Object, Array, String, Number, Bool, Null;
		public static InputStream getImageAsResourceInputStream()
		{
			return ExampleTreeIcons.class.getResourceAsStream("ExampleTreeIcons.png");
		}
	}
}
