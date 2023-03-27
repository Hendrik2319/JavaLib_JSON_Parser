package net.schwarzbaer.java.lib.jsonparser;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import net.schwarzbaer.java.lib.jsonparser.JSON_Data.JSON_Array;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.JSON_Object;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.NamedValueExtra;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.ValueExtra;

public class JSON_Helper {

	private static abstract class KnownValues<SelfType,NodeType,TypeType> {
		private final String defaultPrefixStr;
		private final HashMap<String,HashSet<TypeType>> knownValues;
		
		private KnownValues(String packagePrefix, Class<?> dataClass, String annex)
		{
			knownValues = new HashMap<>();
			
			if (dataClass == null)
				defaultPrefixStr = null;
			else
			{
				String str = dataClass.getCanonicalName();
				if (str==null) str = dataClass.getName();
				
				if (str.startsWith(packagePrefix))
					str = str.substring(packagePrefix.length());
				
				if (annex!=null)
					str += annex;
				defaultPrefixStr = str;
			}
		}
		
		public SelfType add(String name, TypeType type) {
			HashSet<TypeType> hashSet = knownValues.get(name);
			if (hashSet==null) knownValues.put(name,hashSet = new HashSet<>());
			hashSet.add(type);
			return getThis();
		}
		
		protected boolean contains(String name, TypeType type) {
			HashSet<TypeType> hashSet = knownValues.get(name);
			return hashSet!=null && hashSet.contains(type);
		}
		
		protected abstract SelfType getThis();
		
		public final void scanUnexpectedValues(NodeType node)
		{
			if (defaultPrefixStr==null) throw new IllegalStateException("Can't call scanUnexpectedValues without prefixStr, if KnownValues was constructed without class object.");
			scanUnexpectedValues(node, defaultPrefixStr);
		}
		
		public abstract void scanUnexpectedValues(NodeType node, String prefixStr);
	}

	public static class KnownJsonValues<NV extends NamedValueExtra, V extends ValueExtra> extends KnownValues<KnownJsonValues<NV,V>, JSON_Object<NV,V>, JSON_Data.Value.Type>
	{
		private final HashSet<String> unknownValueStatements;

		private KnownJsonValues(HashSet<String> unknownValueStatements, String packagePrefix, Class<?> dataClass, String annex) {
			super(packagePrefix, dataClass, annex);
			this.unknownValueStatements = unknownValueStatements;
		}
		
		@Override protected KnownJsonValues<NV,V> getThis() { return this; }
		
		@Override public void scanUnexpectedValues(JSON_Object<NV, V> object, String prefixStr) {
			for (JSON_Data.NamedValue<NV,V> nvalue:object)
				if (!contains(nvalue.name, nvalue.value.type))
					unknownValueStatements.add(String.format("%s.%s:%s", prefixStr, nvalue.name, nvalue.value.type));
		}
	}

	public static class KnownJsonValuesFactory<NV extends NamedValueExtra, V extends ValueExtra>
	{
		private final HashSet<String> unknownValueStatements;
		private final String packagePrefix;
		
		public KnownJsonValuesFactory(String packagePrefix) {
			this.packagePrefix = packagePrefix;
			unknownValueStatements = new HashSet<>();
		}
		
		public void clearStatementList()
		{
			unknownValueStatements.clear();
		}
		
		public void showStatementList(PrintStream out, String title) {
			if (unknownValueStatements.isEmpty()) return;
			Vector<String> vec = new Vector<>(unknownValueStatements);
			out.printf("%s: [%d]%n", title, vec.size());
			vec.sort(Comparator.<String,String>comparing(String::toLowerCase).thenComparing(Comparator.naturalOrder()));
			for (String str:vec)
				out.printf("   %s%n", str);
		}
		
		public KnownJsonValues<NV,V> create() { return create(null, null); }
		public KnownJsonValues<NV,V> create(Class<?> dataClass) { return create(dataClass, null); }
		public KnownJsonValues<NV,V> create(Class<?> dataClass, String annex) {
			return new KnownJsonValues<>(unknownValueStatements, packagePrefix, dataClass, annex);
		}
	}
	
	public static class OptionalValues<NV extends NamedValueExtra, V extends ValueExtra> extends HashMap<String,OptionalValues.BlockTypes> {
		private static final long serialVersionUID = -8422934659235134268L;

		public void scan(JSON_Data.Value<NV, V> value, String prefixStr) {
			
			BlockTypes blockTypes = get(prefixStr);
			//boolean blockIsNew = false;
			if (blockTypes==null) {
				//blockIsNew = true;
				put(prefixStr, blockTypes=new BlockTypes());
			}
			
			if (blockTypes.baseValue == null)
				blockTypes.baseValue = new Types();
			
			if (value==null)
				blockTypes.baseValue.types.add(null);
			else {
				blockTypes.baseValue.types.add(value.type);
				scanSubValues(blockTypes.baseValue, value, "<Base>", prefixStr);
			}
		}
		
		public void scan(JSON_Array<NV,V> array, String prefixStr) {
			
			BlockTypes blockTypes = get(prefixStr);
			//boolean blockIsNew = false;
			if (blockTypes==null) {
				//blockIsNew = true;
				put(prefixStr, blockTypes=new BlockTypes());
			}
			
			if (blockTypes.baseValue == null)
				blockTypes.baseValue = new Types();
			
			blockTypes.baseValue.types.add(JSON_Data.Value.Type.Array);
			if (array==null)
				blockTypes.baseValue.types.add(null);
			else
				scanArray(blockTypes.baseValue, array, "<Base>", prefixStr);
		}
		
		public void scan(JSON_Object<NV,V> object, String prefixStr) {
			
			BlockTypes blockTypes = get(prefixStr);
			boolean blockIsNew = false;
			if (blockTypes==null) {
				blockIsNew = true;
				put(prefixStr, blockTypes=new BlockTypes());
			}
			
			scanObject(blockTypes, blockIsNew, object, prefixStr);
		}

		private void scanObject(BlockTypes blockTypes, boolean blockIsNew, JSON_Object<NV, V> object, String prefixStr) {
			blockTypes.objectValues.forEach((name,types)->{
				JSON_Data.Value<NV, V> value = object.getValue(name);
				if (value==null) types.types.add(null);
			});
			
			for (JSON_Data.NamedValue<NV,V> nvalue:object) {
				Types types = blockTypes.objectValues.get(nvalue.name);
				if (types==null) {
					blockTypes.objectValues.put(nvalue.name, types=new Types());
					if (!blockIsNew) types.types.add(null);
				}
				types.types.add(nvalue.value.type);
				scanSubValues(types, nvalue.value, nvalue.name, prefixStr);
			}
		}

		private void scanSubValues(Types types, JSON_Data.Value<NV, V> value, String name, String prefixStr) {
			switch (value.type) {
			case Bool: case Float: case Integer: case Null: case String: break;
			case Array: {
				JSON_Data.ArrayValue<NV, V> arrayValue = value.castToArrayValue();
				if (arrayValue==null || arrayValue.value==null) throw new IllegalStateException();
				scanArray(types, arrayValue.value, name, prefixStr);
			} break;
			case Object:
				JSON_Data.ObjectValue<NV, V> objectValue = value.castToObjectValue();
				if (objectValue==null || objectValue.value==null) throw new IllegalStateException();
				String prefixStr2 = prefixStr+"."+name;
				scan(objectValue.value, prefixStr2);
				break;
			}
		}

		private void scanArray(Types types, JSON_Array<NV, V> array, String name, String prefixStr) {
			Types types2 = types.arrayValueTypes;
			if (types2==null) types.arrayValueTypes = (types2=new Types());
			if (array.isEmpty())
				types.isEmptyArrayPossible = true;
			else
				for (JSON_Data.Value<NV, V> value:array) {
					if (value==null || value.type==null) throw new IllegalStateException();
					types2.types.add(value.type);
					scanSubValues(types2, value, name+"[]", prefixStr);
				}
		}

		@Override public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(String.format("%s {%n",getClass().getSimpleName()));
			String indent = "    ";
			forEach((blockName,valueMap)->{
				valueMap.toString(sb, indent, blockName);
			});
			sb.append("}\r\n");
			return sb.toString();
		}

		public void show(PrintStream out) {
			show("Optional JSON Values",out);
		}
		public void show(String label, PrintStream out) {
			if (isEmpty()) return;
			String indent = "    ";
			Vector<String> prefixStrs = new Vector<>(keySet());
			prefixStrs.sort(null);
			out.printf("%s: [%d blocks]%n", label, prefixStrs.size());
			for (String prefixStr:prefixStrs) {
				BlockTypes valueMap = get(prefixStr);
				valueMap.show(out, indent, prefixStr);
			}
				
		}
		
		private static class BlockTypes {
			HashMap<String,Types> objectValues = new HashMap<>();
			Types baseValue = null;

			void toString(StringBuilder sb, String indent, String blockName) {
				String indent2 = indent+"    ";
				sb.append(String.format("%s%s {%n", indent, blockName));
				if (baseValue!=null) baseValue.toString(sb,indent2,"<Base>");
				objectValues.forEach((valueName,types)->types.toString(sb, indent2, valueName));
				sb.append(String.format("%s}%n", indent));
			}

			void show(PrintStream out, String indent, String prefixStr) {
				Vector<String> names = new Vector<>(objectValues.keySet());
				names.sort(null);
				out.printf("%sBlock \"%s\" [%d]%n", indent, prefixStr, names.size());
				String indent2 = indent+"    ";
				if (baseValue!=null)
					baseValue.show(out,indent2,"<Base>");
				for (String name:names)
					objectValues.get(name).show(out,indent2,name);
			}
		}

		private static class Types {
			
			HashSet<JSON_Data.Value.Type> types = new HashSet<>();
			Types arrayValueTypes = null;
			boolean isEmptyArrayPossible = false;
			
			void toString(StringBuilder sb, String indent, String valueName) {
				toString(sb, indent, valueName, false);
			}
			void toString(StringBuilder sb, String indent, String valueName, boolean isEmptyArrayPossible) {
				sb.append(String.format("%s%s = %s%s%n", indent, valueName, types.toString(), isEmptyArrayPossible ? " | EmptyArray" : ""));
				if (arrayValueTypes!=null)
					arrayValueTypes.toString(sb, indent, valueName+"[]", this.isEmptyArrayPossible);
			}
			
			void show(PrintStream out, String indent, String name) {
				show(out, indent, name, false);
			}
			void show(PrintStream out, String indent, String name, boolean isEmptyArrayPossible) {
				Vector<JSON_Data.Value.Type> types = new Vector<>(this.types);
				types.sort(Comparator.nullsLast(Comparator.naturalOrder()));
				StringBuilder sb = new StringBuilder();
				toString(types, sb);
				if (isEmptyArrayPossible) {
					if (sb.length()>0) sb.append(" or ");
					sb.append("empty array");
				}
				out.printf("%s%s:%s%n", indent, name, sb.toString());
				if (arrayValueTypes!=null)
					arrayValueTypes.show(out, indent, name+"[]", this.isEmptyArrayPossible);
				
//				for (JSON_Data.Value.Type type:types) {
//					String comment = ":"+type;
//					if (type==null ) {
//						if (name.endsWith("[]"))
//							comment = " is empty"; // array was empty
//						else if (name.isEmpty())
//							comment = " == <null>"; // base value of this block was NULL
//						else
//							comment = " is optional"; // sub value of an object was optional
//					}
//					out.printf("      %s%s%n", name, comment);
//				}
			}
			private void toString(Vector<JSON_Data.Value.Type> types, StringBuilder sb) {
				if      (types.size()==1) sb.append(toString(types.firstElement()));
				else if (types.size()> 1) {
					Iterable<String> it = ()->types.stream().map(this::toString).iterator();
					sb.append(String.format("[%s]", String.join(", ", it))); 
				}
			}
			private String toString(JSON_Data.Value.Type t) {
				return t==null ? "<unset>" : t.toString();
			}
		}
	}
	

}
