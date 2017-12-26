package net.schwarzbaer.java.lib.jsonparser;

import java.util.Arrays;
import java.util.Vector;

import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value.Type;

public class JSON_Data {
	
	public static class PathIsNotSolvableException extends Exception {
		private static final long serialVersionUID = -2729201690665554532L;
		public PathIsNotSolvableException(String message) { super(message); }
	}

	public static Value getSubNode(JSON_Object json_object, Object... path) throws PathIsNotSolvableException {
		Value baseValue = new ObjectValue(json_object);
		
		for (int i=0; i<path.length; ++i) {
			if      (baseValue.type==Type.Array && path[i] instanceof Integer) baseValue = getChild((ArrayValue)baseValue,(Integer)path[i]);
			else if (baseValue.type==Type.Object && path[i] instanceof String) baseValue = getChild((ObjectValue)baseValue,(String)path[i]);
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

		public Value getValue(String name) {
			for (NamedValue namedvalue : this)
				if (namedvalue.name.equals(name))
					return namedvalue.value;
			return null;
		}
	}
	
	public static class JSON_Array extends Vector<Value> {
		private static final long serialVersionUID = -8537671053731284735L;

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
		
	}

	public static class NamedValue {
		public String name;
		public Value value;
		
		public NamedValue(String name, Value value) {
			this.name = name;
			this.value = value;
		}

		@Override
		public String toString() {
			return "NamedValue [name=\""+name+"\", value="+value+"]";
		}
		
	}
	
	public static abstract class Value {
		
		public enum Type { Array, Object, String, Bool, Integer, Float }
		public final Type type;

		public Value(Type type) {
			this.type = type;
		}

		@Override
		public String toString() {
			return ""+type;
		}
		
		public static JSON_Array  getArray  (Value val) { if (val!=null && (val instanceof ArrayValue  ) && val.type==Type.Array  ) return ((ArrayValue  )val).value; return null; }
		public static JSON_Object getObject (Value val) { if (val!=null && (val instanceof ObjectValue ) && val.type==Type.Object ) return ((ObjectValue )val).value; return null; }
		public static String      getString (Value val) { if (val!=null && (val instanceof StringValue ) && val.type==Type.String ) return ((StringValue )val).value; return null; }
		public static Boolean     getBool   (Value val) { if (val!=null && (val instanceof BoolValue   ) && val.type==Type.Bool   ) return ((BoolValue   )val).value; return null; }
		public static Long        getInteger(Value val) { if (val!=null && (val instanceof IntegerValue) && val.type==Type.Integer) return ((IntegerValue)val).value; return null; }
		public static Double      getFloat  (Value val) { if (val!=null && (val instanceof FloatValue  ) && val.type==Type.Float  ) return ((FloatValue  )val).value; return null; }
	}
	
	public static class GenericValue<T> extends Value {
		public T value;

		public GenericValue(T value, Type type) {
			super(type);
			this.value = value;
		}
	}
	
	public static class ArrayValue   extends GenericValue<JSON_Array>  { public ArrayValue  (JSON_Array  value) { super(value, Type.Array  ); } @Override public String toString() { return super.toString()+"["+value.size()+"]"; } }
	public static class ObjectValue  extends GenericValue<JSON_Object> { public ObjectValue (JSON_Object value) { super(value, Type.Object ); } @Override public String toString() { return super.toString()+"{"+value.size()+"}"; } }
	public static class StringValue  extends GenericValue<String>      { public StringValue (String      value) { super(value, Type.String ); } @Override public String toString() { return super.toString()+"(\""+value+"\")"; } }
	public static class BoolValue    extends GenericValue<Boolean>     { public BoolValue   (boolean     value) { super(value, Type.Bool   ); } @Override public String toString() { return super.toString()+"("  +value+  ")"; } }
	public static class IntegerValue extends GenericValue<Long>        { public IntegerValue(long        value) { super(value, Type.Integer); } @Override public String toString() { return super.toString()+"("  +value+  ")"; } }
	public static class FloatValue   extends GenericValue<Double>      { public FloatValue  (double      value) { super(value, Type.Float  ); } @Override public String toString() { return super.toString()+"("  +value+  ")"; } }
}
