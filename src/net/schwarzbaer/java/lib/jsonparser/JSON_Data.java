package net.schwarzbaer.java.lib.jsonparser;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.function.Function;

import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value.Type;

public class JSON_Data {
	
	public static class PathIsNotSolvableException extends Exception {
		private static final long serialVersionUID = -2729201690665554532L;
		public PathIsNotSolvableException(String message) { super(message); }
	}

	public static boolean hasSubNode(JSON_Object json_object, Object... path) {
		try {
			return getSubNode(json_object, path) != null;
		} catch (PathIsNotSolvableException e) {
			return false;
		}
	}
	public static Value getSubNode(JSON_Object json_object, Object... path) throws PathIsNotSolvableException {
		return getSubNode(new ObjectValue(json_object), path);
	}
	public static Value getSubNode(JSON_Array json_array, Object... path) throws PathIsNotSolvableException {
		return getSubNode(new ArrayValue(json_array), path);
	}
	public static Value getSubNode(Value baseValue, Object... path) throws PathIsNotSolvableException {
		for (int i=0; i<path.length; ++i) {
			if      (baseValue.type==Type.Array  && path[i] instanceof Integer) { baseValue.wasProcessed=true; baseValue = getChild((ArrayValue)baseValue,(Integer)path[i]); }
			else if (baseValue.type==Type.Object && path[i] instanceof String ) { baseValue.wasProcessed=true; baseValue = getChild((ObjectValue)baseValue,(String)path[i]); }
			else
				throw new PathIsNotSolvableException("Path is not solvable: "+Arrays.toString(path));
		}
		return baseValue;
	}

	private static Value getChild(ArrayValue arrayValue, int index) throws PathIsNotSolvableException {
		if (index<0 || index>=arrayValue.value.size())
			throw new PathIsNotSolvableException("This value has no child at index "+index+".");
		return arrayValue.value.get(index);
	}

	private static Value getChild(ObjectValue objectValue, String label) throws PathIsNotSolvableException {
		Value value = objectValue.value.getValue(label);
//		for (NamedValue namedvalue : objectValue.value)
//			if (namedvalue.name.equals(label))
//				return namedvalue.value;
		if (value==null) 
			throw new PathIsNotSolvableException("This value has no child with name \""+label+"\".");
		return value;
	}
	
//	public Value getSubNode(Object... path) throws PathIsNotSolvableException {
//		if (path.length==0) return this;
//		
//		switch(type) {
//		case Array:
//			if (path[0] instanceof Integer)
//				return getChild((Integer)path[0]);
//		case Object:
//			if (path[0] instanceof String)
//				return getChild((String)path[0]);
//		default:
//			break;
//		}
//		
//		throw new PathIsNotSolvableException("Path is not solvable: "+Arrays.toString(path));
//	}
	
	public static class JSON_Object extends Vector<NamedValue> {
		private static final long serialVersionUID = -8191469330084921029L;

		public JSON_Object() {}
		public JSON_Object(Collection<? extends NamedValue> values) {
			super(values);
		}
		
		public Value getValue(String name) {
			for (NamedValue namedvalue : this)
				if (namedvalue.name.equals(name))
					return namedvalue.value;
			return null;
		}
	}
	
	public static class JSON_Array extends Vector<Value> {
		private static final long serialVersionUID = -8537671053731284735L;
		
		public JSON_Array() {}
		public JSON_Array(Collection<? extends Value> values) {
			super(values);
		}

//		private ValueType valueType;
//		
//		
//		public JSON_Array() {
//			this.valueType = null;
//		}
//		
//		public boolean hasMixedContent() {
//			return !isEmpty() && (valueType == null);
//		}
//
//		@Override
//		public synchronized boolean add(Value value) {
//			if (isEmpty())
//				valueType = value.valueType;
//			else
//				if (valueType != value.valueType)
//					valueType = null;
//			return super.add(value);
//		}
		
		public String toString(Value.Type expectedType) {
			return toString(expectedType,null);
		}
		
		public String toString(Value.Type expectedType, String format) {
			String str = "";
			for (Value val:this) {
				if (!str.isEmpty()) str+=", ";
				if (val.type!=expectedType)
					str+=val.toString();
				else
					switch (expectedType) {
					case Array  : str+="[" +((ArrayValue  )val).value.size()+"]"; break;
					case Object : str+="{" +((ObjectValue )val).value.size()+"}"; break;
					case String : str+="\""+((StringValue )val).value+"\""; break;
					case Bool   : str+=""  +((BoolValue   )val).value+  ""; break;
					case Null   : str+=""  +((NullValue   )val).value+  ""; break;
					case Integer: str+=""  +((IntegerValue)val).value+  ""; break;
					case Float  : Double d = ((FloatValue)val).value; str+= d==null?"null":String.format(Locale.ENGLISH,format,d); break;
					}
			}
			return "[ "+str+" ]";
		}
	}

	public static class NamedValue {
		public String name;
		public Value value;
		public boolean wasDeObfuscated;
		
		public NamedValue(String name, Value value) {
			this.name = name;
			this.value = value;
			this.wasDeObfuscated = false;
		}

		@Override
		public String toString() {
			return "NamedValue [name=\""+name+"\", value="+value+"]";
		}
		
	}
//	boolean wasDeObfuscated = true;
//	boolean hasObfuscatedChildren = true;
	
	public static abstract class Value {
		
		public enum Type {
			Array(false), Object(false), String(true), Bool(true), Integer(true), Float(true), Null(true);
			public final boolean isSimple;
			Type(boolean isSimple) { this.isSimple = isSimple; }
		}
		
		public final Type type;
		public boolean wasProcessed;
		protected Boolean hasUnprocessedChildren;
		protected Boolean hasObfuscatedChildren;

		public Value(Type type) {
			this.type = type;
			wasProcessed = false;
			hasUnprocessedChildren = null;
			hasObfuscatedChildren  = null;
		}

		public boolean hasUnprocessedChildren() { // default implementation 
			hasUnprocessedChildren = false;
			return false;
		}

		public boolean hasObfuscatedChildren() { // default implementation 
			hasObfuscatedChildren = false;
			return false;
		}

		@Override
		public String toString() {
			return ""+type;
		}
	}
	
	public static class Null {
		@Override public String toString() { return "<null>"; }
	}
	
	public static class GenericValue<T> extends Value {
		public T value;

		public GenericValue(T value, Type type) {
			super(type);
			this.value = value;
		}
	}
	
	public static <T> T getValue(Value value, Type type, Function<Value,GenericValue<T>> cast) {
		if (value==null) return null;
		if (value.type!=type) return null;
		GenericValue<T> val = cast.apply(value);
		if (val!=null) return val.value;
		return null;
	}
	public static JSON_Array    getArrayValue(Value value) { return getValue(value, Type.Array  , val->value instanceof   ArrayValue ? (  ArrayValue) value : null); }
	public static JSON_Object  getObjectValue(Value value) { return getValue(value, Type.Object , val->value instanceof  ObjectValue ? ( ObjectValue) value : null); }
	public static String       getStringValue(Value value) { return getValue(value, Type.String , val->value instanceof  StringValue ? ( StringValue) value : null); }
	public static Boolean        getBoolValue(Value value) { return getValue(value, Type.Bool   , val->value instanceof    BoolValue ? (   BoolValue) value : null); }
	public static Long        getIntegerValue(Value value) { return getValue(value, Type.Integer, val->value instanceof IntegerValue ? (IntegerValue) value : null); }
	public static Double        getFloatValue(Value value) { return getValue(value, Type.Float  , val->value instanceof   FloatValue ? (  FloatValue) value : null); }
	public static Null           getNullValue(Value value) { return getValue(value, Type.Null   , val->value instanceof    NullValue ? (   NullValue) value : null); }
	
	public static class   ArrayValue extends GenericValue<JSON_Array>  { public   ArrayValue(JSON_Array  value) { super(value, Type.Array  ); } @Override public String toString() { return super.toString()+"["+value.size()+"]"; } @Override public boolean hasUnprocessedChildren() { return hUC(this); } @Override public boolean hasObfuscatedChildren() { return hOC(this); } }
	public static class  ObjectValue extends GenericValue<JSON_Object> { public  ObjectValue(JSON_Object value) { super(value, Type.Object ); } @Override public String toString() { return super.toString()+"{"+value.size()+"}"; } @Override public boolean hasUnprocessedChildren() { return hUC(this); } @Override public boolean hasObfuscatedChildren() { return hOC(this); } }
	public static class  StringValue extends GenericValue<String>      { public  StringValue(String      value) { super(value, Type.String ); } @Override public String toString() { return super.toString()+"(\""+value+"\")"; } }
	public static class    BoolValue extends GenericValue<Boolean>     { public    BoolValue(boolean     value) { super(value, Type.Bool   ); } @Override public String toString() { return super.toString()+"("  +value+  ")"; } }
	public static class IntegerValue extends GenericValue<Long>        { public IntegerValue(long        value) { super(value, Type.Integer); } @Override public String toString() { return super.toString()+"("  +value+  ")"; } }
	public static class   FloatValue extends GenericValue<Double>      { public   FloatValue(double      value) { super(value, Type.Float  ); } @Override public String toString() { return super.toString()+"("  +value+  ")"; } }
	public static class    NullValue extends GenericValue<Null>        { public    NullValue(Null        value) { super(value, Type.Null   ); } @Override public String toString() { return super.toString()+"("  +value+  ")"; } }

	private static boolean hUC( ArrayValue  arrayValue) { return hasUnprocessedChildren( arrayValue, arrayValue.value, v->v); }
	private static boolean hUC(ObjectValue objectValue) { return hasUnprocessedChildren(objectValue,objectValue.value,nv->nv.value); }
	private static boolean hOC( ArrayValue  arrayValue) { return hasObfuscatedChildren ( arrayValue, arrayValue.value, v->v       , v->true              ); }
	private static boolean hOC(ObjectValue objectValue) { return hasObfuscatedChildren (objectValue,objectValue.value,nv->nv.value,nv->nv.wasDeObfuscated); }

	private static <T> boolean hasUnprocessedChildren(Value value, Vector<T> array, Function<T,Value> getValue) {
		if (value.hasUnprocessedChildren!=null) return value.hasUnprocessedChildren;
		value.hasUnprocessedChildren=false;
		for (T t:array) {
			Value child = getValue.apply(t);
			if (!child.wasProcessed || child.hasUnprocessedChildren()) {
				value.hasUnprocessedChildren=true;
				break;
			}
		}
		return value.hasUnprocessedChildren;
	}

	private static <T> boolean hasObfuscatedChildren(Value value, Vector<T> array, Function<T,Value> getValue, Function<T,Boolean> wasDeObfuscated) {
		if (value.hasObfuscatedChildren!=null) return value.hasObfuscatedChildren;
		value.hasObfuscatedChildren=false;
		for (T t:array) {
			Value child = getValue.apply(t);
			if (!wasDeObfuscated.apply(t) || child.hasObfuscatedChildren()) {
				value.hasObfuscatedChildren=true;
				break;
			}
		}
		return value.hasObfuscatedChildren;
	}
	
	public static void traverseNamedValues(JSON_Object data, BiConsumer<String,NamedValue> consumer) {
		traverseNamedValues("", data, consumer);
	}
	
	public static void traverseNamedValues(JSON_Array array, BiConsumer<String,NamedValue> consumer) {
		traverseNamedValues("", array, consumer);
	}
	
	private static void traverseNamedValues(String path, JSON_Object data, BiConsumer<String,NamedValue> consumer) {
		String newPath;
		for (NamedValue nv : data) {
			newPath = (path.isEmpty()?"":path+".") + nv.name;
			consumer.accept(newPath,nv);
			newPath = (path.isEmpty()?"":path+".") + nv.name;
			if (nv.value.type == Type.Object) traverseNamedValues(newPath, ((ObjectValue)nv.value).value, consumer);
			if (nv.value.type == Type.Array ) traverseNamedValues(newPath, (( ArrayValue)nv.value).value, consumer);
		}
	}
	
	private static void traverseNamedValues(String path, JSON_Array array, BiConsumer<String,NamedValue> consumer) {
		String newPath;
		for (int i=0; i<array.size(); i++) {
			Value v = array.get(i);
			newPath = (path.isEmpty()?"":path) + "["+i+"]";
			if (v.type == Type.Object) traverseNamedValues(newPath, ((ObjectValue)v).value, consumer);
			if (v.type == Type.Array ) traverseNamedValues(newPath, (( ArrayValue)v).value, consumer);
		}
	}
}
