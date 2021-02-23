package net.schwarzbaer.java.lib.jsonparser;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value.Type;

public class JSON_Data {
	
	public static class TraverseException extends Exception {
		private static final long serialVersionUID = -2729201690665554532L;
		public TraverseException(String message) {
			super(message);
		}
		public TraverseException(String format, Object...args) {
			super(String.format(Locale.ENGLISH, format, args));
		}
	}

	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> boolean hasSubNode(JSON_Object<NVExtra,VExtra> json_object, Object... path) {
		if (path==null || path.length==0) throw new IllegalArgumentException("hasSubNode(JSON_Object) without a path is not allowed");
		try {
			return getSubNode(json_object, path) != null;
		} catch (TraverseException e) {
			return false;
		}
	}
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> Value<NVExtra,VExtra> getSubNode(JSON_Object<NVExtra,VExtra> json_object, Object... path) throws TraverseException {
		if (path==null || path.length==0) throw new IllegalArgumentException("getSubNode(JSON_Object) without a path is not allowed");
		if (!(path[0] instanceof String)) throw new IllegalArgumentException("getSubNode(JSON_Object,path): First value of path must be a String");
		return getSubNode(new ObjectValue<NVExtra,VExtra>(json_object,null), path);
	}
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> Value<NVExtra,VExtra> getSubNode(JSON_Array<NVExtra,VExtra> json_array, Object... path) throws TraverseException {
		if (path==null || path.length==0)  throw new IllegalArgumentException("getSubNode(JSON_Array) without a path is not allowed");
		if (!(path[0] instanceof Integer)) throw new IllegalArgumentException("getSubNode(JSON_Array,path): First value of path must be an Integer");
		return getSubNode(new ArrayValue<NVExtra,VExtra>(json_array,null), path);
	}
	
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> Value<NVExtra,VExtra> getSubNode(Value<NVExtra,VExtra> baseValue, Object... path) throws TraverseException {
		for (int i=0; i<path.length; ++i) {
			if      (baseValue.type==Type.Array  && baseValue.castToArrayValue ()!=null && path[i] instanceof Integer) { ExtraCalls.markAsProcessed(baseValue); baseValue = getChild(baseValue.castToArrayValue (),(Integer)path[i],()->toString(path)); }
			else if (baseValue.type==Type.Object && baseValue.castToObjectValue()!=null && path[i] instanceof String ) { ExtraCalls.markAsProcessed(baseValue); baseValue = getChild(baseValue.castToObjectValue(),(String )path[i],()->toString(path)); }
			else
				throw new TraverseException("Path is not solvable: %s", toString(path));
		}
		return baseValue;
	}
	private static String toString(Object... path) {
		Stream<String> stream = Arrays.stream(path).map(obj->{
			if (obj instanceof String) return "\""+obj.toString()+"\"";
			if (obj instanceof Number) return obj.toString();
			return "<"+obj+">";
		});
		Iterable<String> iterable = ()->stream.iterator();
		return "[ "+String.join(", ", iterable)+" ]";
	}

	private static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> Value<NVExtra,VExtra> getChild(ArrayValue<NVExtra,VExtra> arrayValue, int index, Supplier<String> getPathStr) throws TraverseException {
		if (index<0 || index>=arrayValue.value.size())
			throw new TraverseException("This ArrayValue has no child at index %d. [Path: %s]", index, getPathStr.get());
		return arrayValue.value.get(index);
	}

	private static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> Value<NVExtra,VExtra> getChild(ObjectValue<NVExtra,VExtra> objectValue, String label, Supplier<String> getPathStr) throws TraverseException {
		Value<NVExtra,VExtra> value = objectValue.value.getValue(label);
		if (value==null) 
			throw new TraverseException("This ObjectValue has no child with name \"%s\". [Path: %s]", label, getPathStr.get());
		return value;
	}
	
	public interface FactoryForExtras<NVExtra extends NamedValueExtra, VExtra extends ValueExtra> {
		NVExtra createNamedValueExtra(Type type);
		VExtra createValueExtra(Type type);
	}
	public interface NamedValueExtra {
		public static abstract class Dummy implements NamedValueExtra {}
	}
	public interface ValueExtra {
		public static abstract class Dummy implements ValueExtra {
			@Override public void markAsProcessed() {}
		}
		public void markAsProcessed();
	}
	private static class ExtraCalls {

		public static void markAsProcessed(Value<?, ? extends ValueExtra> value) {
			if (value!=null && value.extra!=null)
				value.extra.markAsProcessed();
		}
		
	}
	

	public static class NamedValue<NVExtra extends NamedValueExtra, VExtra extends ValueExtra> {
		public String name;
		public final Value<NVExtra,VExtra> value;
		public final NVExtra extra;
		
		public NamedValue(String name, Value<NVExtra,VExtra> value, NVExtra extra) {
			this.name = name;
			this.value = value;
			this.extra = extra;
		}

		@Override
		public String toString() {
			return "NamedValue [name=\""+name+"\", value="+value+"]";
		}
		
	}
	
	public static abstract class Value<NVExtra extends NamedValueExtra, VExtra extends ValueExtra> {
		
		public enum Type {
			Array(false), Object(false), String(true), Bool(true), Integer(true), Float(true), Null(true);
			public final boolean isSimple;
			Type(boolean isSimple) { this.isSimple = isSimple; }
		}
		
		public final Type type;
		public final VExtra extra;

		public Value(Type type, VExtra extra) {
			this.type = type;
			this.extra = extra;
		}

		@Override
		public String toString() {
			return ""+type;
		}
		
		public  ObjectValue<NVExtra,VExtra> castToObjectValue () { return null; }
		public   ArrayValue<NVExtra,VExtra> castToArrayValue  () { return null; }
		public  StringValue<NVExtra,VExtra> castToStringValue () { return null; }
		public    BoolValue<NVExtra,VExtra> castToBoolValue   () { return null; }
		public IntegerValue<NVExtra,VExtra> castToIntegerValue() { return null; }
		public   FloatValue<NVExtra,VExtra> castToFloatValue  () { return null; }
		public    NullValue<NVExtra,VExtra> castToNullValue   () { return null; }
	}
	
	public static class JSON_Object<NVExtra extends NamedValueExtra, VExtra extends ValueExtra> extends Vector<NamedValue<NVExtra,VExtra>> {
		private static final long serialVersionUID = -8191469330084921029L;
	
		public JSON_Object() {}
		public JSON_Object(Collection<? extends NamedValue<NVExtra,VExtra>> values) {
			super(values);
		}
		
		public Value<NVExtra,VExtra> getValue(String name) {
			for (NamedValue<NVExtra,VExtra> namedvalue : this)
				if (namedvalue.name.equals(name))
					return namedvalue.value;
			return null;
		}
		
		public void forEach(BiConsumer<String,Value<NVExtra,VExtra>> action) {
			forEach(nv->action.accept(nv.name, nv.value));
		}
		
		public Vector<String> getNames() {
			Vector<String> names = new Vector<>();
			forEach(nv->names.add(nv.name));
			return names;
		}
	}

	public static class JSON_Array<NVExtra extends NamedValueExtra, VExtra extends ValueExtra> extends Vector<Value<NVExtra,VExtra>> {
		private static final long serialVersionUID = -8537671053731284735L;
		
		public JSON_Array() {}
		public JSON_Array(Collection<? extends Value<NVExtra,VExtra>> values) {
			super(values);
		}
		
		public String toString(Value.Type expectedType) {
			return toString(expectedType,null);
		}
		
		public String toString(Value.Type expectedType, String format) {
			String str = "";
			for (Value<NVExtra,VExtra> val:this) {
				if (!str.isEmpty()) str+=", ";
				if (val.type == expectedType)
					switch (val.type) {
					case Array  : str+="[" + val.castToArrayValue  ().value.size()+"]"; break;
					case Object : str+="{" + val.castToObjectValue ().value.size()+"}"; break;
					case String : str+="\""+ val.castToStringValue ().value+"\""; break;
					case Bool   : str+=""  + val.castToBoolValue   ().value+  ""; break;
					case Null   : str+=""  + val.castToNullValue   ().value+  ""; break;
					case Integer: str+=""  + val.castToIntegerValue().value+  ""; break;
					case Float  : Double d = val.castToFloatValue  ().value; str+= d==null?"null":String.format(Locale.ENGLISH,format,d); break;
					}
				else
					str+=val.toString();
			}
			return "[ "+str+" ]";
		}
	}

	public static class Null {
		@Override public String toString() { return "<null>"; }
	}
	
	public static class GenericValue<NVExtra extends NamedValueExtra, VExtra extends ValueExtra, T> extends Value<NVExtra,VExtra> {
		public T value;

		public GenericValue(T value, Type type, VExtra extra) {
			super(type,extra);
			this.value = value;
		}
	}
	
	public static class ObjectValue<NVExtra extends NamedValueExtra, VExtra extends ValueExtra> extends GenericValue<NVExtra,VExtra,JSON_Object<NVExtra,VExtra>> {
		public ObjectValue(JSON_Object<NVExtra,VExtra> value, VExtra extra) {
			super(value, Type.Object, extra);
		}
		@Override public String toString() { return super.toString()+"{"+value.size()+"}"; }
		@Override public ObjectValue<NVExtra, VExtra> castToObjectValue() { return this; }
	}
	public static class ArrayValue<NVExtra extends NamedValueExtra, VExtra extends ValueExtra> extends GenericValue<NVExtra,VExtra,JSON_Array<NVExtra,VExtra>> {
		public ArrayValue(JSON_Array<NVExtra,VExtra> value, VExtra extra) {
			super(value, Type.Array, extra);
		}
		@Override public String toString() { return super.toString()+"["+value.size()+"]"; }
		@Override public ArrayValue<NVExtra, VExtra> castToArrayValue() { return this; }
	}
	public static class  StringValue<NVExtra extends NamedValueExtra, VExtra extends ValueExtra> extends GenericValue<NVExtra,VExtra,String>  { public  StringValue(String  value, VExtra extra) { super(value, Type.String , extra); } @Override public String toString() { return super.toString()+"(\""+value+"\")"; } @Override public  StringValue<NVExtra,VExtra> castToStringValue () { return this; } }
	public static class    BoolValue<NVExtra extends NamedValueExtra, VExtra extends ValueExtra> extends GenericValue<NVExtra,VExtra,Boolean> { public    BoolValue(boolean value, VExtra extra) { super(value, Type.Bool   , extra); } @Override public String toString() { return super.toString()+"("  +value+  ")"; } @Override public    BoolValue<NVExtra,VExtra> castToBoolValue   () { return this; } }
	public static class IntegerValue<NVExtra extends NamedValueExtra, VExtra extends ValueExtra> extends GenericValue<NVExtra,VExtra,Long>    { public IntegerValue(long    value, VExtra extra) { super(value, Type.Integer, extra); } @Override public String toString() { return super.toString()+"("  +value+  ")"; } @Override public IntegerValue<NVExtra,VExtra> castToIntegerValue() { return this; } }
	public static class   FloatValue<NVExtra extends NamedValueExtra, VExtra extends ValueExtra> extends GenericValue<NVExtra,VExtra,Double>  { public   FloatValue(double  value, VExtra extra) { super(value, Type.Float  , extra); } @Override public String toString() { return super.toString()+"("  +value+  ")"; } @Override public   FloatValue<NVExtra,VExtra> castToFloatValue  () { return this; } }
	public static class    NullValue<NVExtra extends NamedValueExtra, VExtra extends ValueExtra> extends GenericValue<NVExtra,VExtra,Null>    { public    NullValue(Null    value, VExtra extra) { super(value, Type.Null   , extra); } @Override public String toString() { return super.toString()+"("  +value+  ")"; } @Override public    NullValue<NVExtra,VExtra> castToNullValue   () { return this; } }
	
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> double getNumber(
			JSON_Object<NVExtra,VExtra> object,
			String subValueName,
			String debugOutputPrefixStr
	) throws TraverseException {
		return JSON_Data.getNumber(object, subValueName, false, debugOutputPrefixStr);
	}
	
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> Double getNumber(
			JSON_Object<NVExtra,VExtra> object,
			String subValueName,
			boolean isOptionalValue,
			String debugOutputPrefixStr
	) throws TraverseException {
		if (object==null) throw new TraverseException("%s==NULL", debugOutputPrefixStr);
		Value<NVExtra,VExtra> value = object.getValue(subValueName);
		if (value==null) {
			if (isOptionalValue) return null;
			throw new TraverseException("%s.%s don't exists", debugOutputPrefixStr, subValueName);
		}
		return JSON_Data.getNumber(value, debugOutputPrefixStr+"."+subValueName);
	}
	
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> double getNumber(
			Value<NVExtra,VExtra> value,
			String debugOutputPrefixStr
	) throws TraverseException {
		Long     intValue = getValue(value, JSON_Data.Value.Type.Integer, JSON_Data.Value::castToIntegerValue, true, debugOutputPrefixStr);
		Double floatValue = getValue(value, JSON_Data.Value.Type.Float  , JSON_Data.Value::castToFloatValue  , true, debugOutputPrefixStr);
		if (  intValue!=null) return   intValue.doubleValue();
		if (floatValue!=null) return floatValue.doubleValue();
		throw new TraverseException("%s isn't either an IntegerValue or a FloatValue", debugOutputPrefixStr);
	}

	private static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra, ResultType> ResultType getValue( // has specific methods
			JSON_Object<NVExtra,VExtra> object,
			String subValueName,
			Value.Type type,
			Function<Value<NVExtra,VExtra>,GenericValue<NVExtra,VExtra,ResultType>> cast,
			String debugOutputPrefixStr
	) throws TraverseException {
		return getValue(object, subValueName, false, type, cast, false, debugOutputPrefixStr);
	}

	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra, ResultType> ResultType getValue(
			JSON_Object<NVExtra,VExtra> object,
			String subValueName,
			boolean isOptionalValue,
			Value.Type type,
			Function<Value<NVExtra,VExtra>,GenericValue<NVExtra,VExtra,ResultType>> cast,
			boolean isOptionalType,
			String debugOutputPrefixStr
	) throws TraverseException {
		if (object==null) throw new TraverseException("%s==NULL", debugOutputPrefixStr);
		Value<NVExtra,VExtra> value = object.getValue(subValueName);
		if (value==null) {
			if (isOptionalValue) return null;
			throw new TraverseException("%s.%s don't exists", debugOutputPrefixStr, subValueName);
		}
		return getValue(value, type, cast, isOptionalType, debugOutputPrefixStr+"."+subValueName);
	}
	
	private static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra, ResultType> ResultType getValue( // has specific methods
			Value<NVExtra,VExtra> value,
			Value.Type type,
			Function<Value<NVExtra,VExtra>,GenericValue<NVExtra,VExtra,ResultType>> cast,
			String debugOutputPrefixStr
	) throws TraverseException {
		return getValue(value, type, cast, false, debugOutputPrefixStr);
	}

	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra, ResultType> ResultType getValue(
			Value<NVExtra,VExtra> value,
			Value.Type type,
			Function<Value<NVExtra,VExtra>,GenericValue<NVExtra,VExtra,ResultType>> cast,
			boolean isOptionalType,
			String debugOutputPrefixStr
	) throws TraverseException {
		if (type==null) throw new IllegalArgumentException("type must not be NULL");
		if (cast==null) throw new IllegalArgumentException("cast must not be NULL");
		
		if (value==null)
			throw new TraverseException("%s==NULL", debugOutputPrefixStr);
		
		if (value.type!=type && !isOptionalType)
			throw new TraverseException("%s isn't a %sValue (type)", debugOutputPrefixStr, type);
		
		GenericValue<NVExtra,VExtra,ResultType> genVal = cast.apply(value);
		if (genVal==null) {
			if (isOptionalType) return null;
			throw new TraverseException("%s isn't a %sValue (cast)", debugOutputPrefixStr, type);
		}
		
		if (genVal.value==null)
			throw new TraverseException("%s(%sValue).value==NULL", type, debugOutputPrefixStr);
		
		if (genVal.extra!=null)
			genVal.extra.markAsProcessed();
		
		return genVal.value;
	}
	
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra, ResultType> ResultType getValue(
			JSON_Object<NVExtra,VExtra> object,
			String subValueName,
			Value.Type type,
			Function<Value<NVExtra,VExtra>,GenericValue<NVExtra,VExtra,ResultType>> cast
	) {
		if (object==null) return null;
		return getValue(object.getValue(subValueName), type, cast);
	}
	
	private static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra, ResultType> ResultType getValue( // has specific methods
			Value<NVExtra,VExtra> value,
			Type type,
			Function<Value<NVExtra,VExtra>,GenericValue<NVExtra,VExtra,ResultType>> cast
	) {
		if (type==null) throw new IllegalArgumentException("type must not be NULL");
		if (cast==null) throw new IllegalArgumentException("cast must not be NULL");
		if (value==null) return null;
		if (value.type!=type) return null;
		GenericValue<NVExtra,VExtra,ResultType> val = cast.apply(value);
		if (val!=null) {
			if (val.extra!=null) val.extra.markAsProcessed();
			return val.value;
		}
		return null;
	}
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> JSON_Object<NVExtra,VExtra> getObjectValue(JSON_Object<NVExtra,VExtra> object, String subValueName, String debugOutputPrefixStr) throws TraverseException { return getValue(object, subValueName, Type.Object , Value::castToObjectValue , debugOutputPrefixStr); }
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> JSON_Array <NVExtra,VExtra>  getArrayValue(JSON_Object<NVExtra,VExtra> object, String subValueName, String debugOutputPrefixStr) throws TraverseException { return getValue(object, subValueName, Type.Array  , Value::castToArrayValue  , debugOutputPrefixStr); }
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> String                      getStringValue(JSON_Object<NVExtra,VExtra> object, String subValueName, String debugOutputPrefixStr) throws TraverseException { return getValue(object, subValueName, Type.String , Value::castToStringValue , debugOutputPrefixStr); }
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> boolean                       getBoolValue(JSON_Object<NVExtra,VExtra> object, String subValueName, String debugOutputPrefixStr) throws TraverseException { return getValue(object, subValueName, Type.Bool   , Value::castToBoolValue   , debugOutputPrefixStr); }
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> long                       getIntegerValue(JSON_Object<NVExtra,VExtra> object, String subValueName, String debugOutputPrefixStr) throws TraverseException { return getValue(object, subValueName, Type.Integer, Value::castToIntegerValue, debugOutputPrefixStr); }
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> double                       getFloatValue(JSON_Object<NVExtra,VExtra> object, String subValueName, String debugOutputPrefixStr) throws TraverseException { return getValue(object, subValueName, Type.Float  , Value::castToFloatValue  , debugOutputPrefixStr); }
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> Null                          getNullValue(JSON_Object<NVExtra,VExtra> object, String subValueName, String debugOutputPrefixStr) throws TraverseException { return getValue(object, subValueName, Type.Null   , Value::castToNullValue   , debugOutputPrefixStr); }
	
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> JSON_Object<NVExtra,VExtra> getObjectValue(Value<NVExtra,VExtra> value, String debugOutputPrefixStr) throws TraverseException { return getValue(value, Type.Object , Value::castToObjectValue , debugOutputPrefixStr); }
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> JSON_Array <NVExtra,VExtra>  getArrayValue(Value<NVExtra,VExtra> value, String debugOutputPrefixStr) throws TraverseException { return getValue(value, Type.Array  , Value::castToArrayValue  , debugOutputPrefixStr); }
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> String                      getStringValue(Value<NVExtra,VExtra> value, String debugOutputPrefixStr) throws TraverseException { return getValue(value, Type.String , Value::castToStringValue , debugOutputPrefixStr); }
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> boolean                       getBoolValue(Value<NVExtra,VExtra> value, String debugOutputPrefixStr) throws TraverseException { return getValue(value, Type.Bool   , Value::castToBoolValue   , debugOutputPrefixStr); }
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> long                       getIntegerValue(Value<NVExtra,VExtra> value, String debugOutputPrefixStr) throws TraverseException { return getValue(value, Type.Integer, Value::castToIntegerValue, debugOutputPrefixStr); }
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> double                       getFloatValue(Value<NVExtra,VExtra> value, String debugOutputPrefixStr) throws TraverseException { return getValue(value, Type.Float  , Value::castToFloatValue  , debugOutputPrefixStr); }
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> Null                          getNullValue(Value<NVExtra,VExtra> value, String debugOutputPrefixStr) throws TraverseException { return getValue(value, Type.Null   , Value::castToNullValue   , debugOutputPrefixStr); }
	
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> JSON_Object<NVExtra,VExtra> getObjectValue(Value<NVExtra,VExtra> value) { return getValue(value, Type.Object , Value::castToObjectValue ); }
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> JSON_Array <NVExtra,VExtra>  getArrayValue(Value<NVExtra,VExtra> value) { return getValue(value, Type.Array  , Value::castToArrayValue  ); }
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> String                      getStringValue(Value<NVExtra,VExtra> value) { return getValue(value, Type.String , Value::castToStringValue ); }
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> Boolean                       getBoolValue(Value<NVExtra,VExtra> value) { return getValue(value, Type.Bool   , Value::castToBoolValue   ); }
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> Long                       getIntegerValue(Value<NVExtra,VExtra> value) { return getValue(value, Type.Integer, Value::castToIntegerValue); }
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> Double                       getFloatValue(Value<NVExtra,VExtra> value) { return getValue(value, Type.Float  , Value::castToFloatValue  ); }
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> Null                          getNullValue(Value<NVExtra,VExtra> value) { return getValue(value, Type.Null   , Value::castToNullValue   ); }
	
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> void traverseAllValues(
			Value<NVExtra,VExtra> data,
			boolean pathWithArrayIndexes,
			BiConsumer<String,NamedValue<NVExtra,VExtra>> consumerNV,
			BiConsumer<String,     Value<NVExtra,VExtra>> consumerV
	) {
		if (data==null) throw new IllegalArgumentException();
		if (consumerV !=null) consumerV .accept("",data);
		if (data.type == Type.Object) traverseAllValues("", data.castToObjectValue().value, pathWithArrayIndexes, consumerNV, consumerV);
		if (data.type == Type.Array ) traverseAllValues("", data.castToArrayValue ().value, pathWithArrayIndexes, consumerNV, consumerV);
	}
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> void traverseAllValues(
			JSON_Object<NVExtra,VExtra> data,
			boolean pathWithArrayIndexes,
			BiConsumer<String,NamedValue<NVExtra,VExtra>> consumerNV,
			BiConsumer<String,     Value<NVExtra,VExtra>> consumerV
	) {
		if (data==null) throw new IllegalArgumentException();
		traverseAllValues("", data, pathWithArrayIndexes, consumerNV, consumerV);
	}
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> void traverseAllValues(
			JSON_Array<NVExtra,VExtra> data,
			boolean pathWithArrayIndexes,
			BiConsumer<String,NamedValue<NVExtra,VExtra>> consumerNV,
			BiConsumer<String,     Value<NVExtra,VExtra>> consumerV
	) {
		if (data==null) throw new IllegalArgumentException();
		traverseAllValues("", data, pathWithArrayIndexes, consumerNV, consumerV);
	}
	private static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> void traverseAllValues(
			String path, JSON_Object<NVExtra, VExtra> data,
			boolean pathWithArrayIndexes,
			BiConsumer<String, NamedValue<NVExtra, VExtra>> consumerNV,
			BiConsumer<String,      Value<NVExtra, VExtra>> consumerV
	) {
		String newPath;
		for (NamedValue<NVExtra,VExtra> nv : data) {
			Value<NVExtra, VExtra> v = nv.value;
			newPath = path + (path.isEmpty()?"":".") + nv.name;
			if (consumerNV!=null) consumerNV.accept(newPath,nv);
			if (consumerV !=null) consumerV .accept(newPath,v);
			if (v.type == Type.Object) traverseAllValues(newPath, v.castToObjectValue().value, pathWithArrayIndexes, consumerNV, consumerV);
			if (v.type == Type.Array ) traverseAllValues(newPath, v.castToArrayValue ().value, pathWithArrayIndexes, consumerNV, consumerV);
		}
	}
	private static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> void traverseAllValues(
			String path, JSON_Array<NVExtra, VExtra> array,
			boolean pathWithArrayIndexes,
			BiConsumer<String,NamedValue<NVExtra, VExtra>> consumerNV,
			BiConsumer<String,     Value<NVExtra, VExtra>> consumerV
	) {
		String newPath;
		for (int i=0; i<array.size(); i++) {
			Value<NVExtra,VExtra> v = array.get(i);
			newPath = path + "["+(pathWithArrayIndexes?Integer.toString(i):"")+"]";
			if (consumerV !=null) consumerV .accept(newPath,v);
			if (v.type == Type.Object) traverseAllValues(newPath, v.castToObjectValue().value, pathWithArrayIndexes, consumerNV, consumerV);
			if (v.type == Type.Array ) traverseAllValues(newPath, v.castToArrayValue ().value, pathWithArrayIndexes, consumerNV, consumerV);
		}
	}
	
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> void traverseNamedValues(
			JSON_Object<NVExtra,VExtra> data,
			boolean pathWithArrayIndexes,
			BiConsumer<String,NamedValue<NVExtra,VExtra>> consumer
	) {
		if (data==null) throw new IllegalArgumentException();
		traverseAllValues("", data, pathWithArrayIndexes, consumer, null);
	}
	
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> void traverseNamedValues(
			JSON_Array<NVExtra,VExtra> data,
			boolean pathWithArrayIndexes,
			BiConsumer<String,NamedValue<NVExtra,VExtra>> consumer
	) {
		if (data==null) throw new IllegalArgumentException();
		traverseAllValues("", data, pathWithArrayIndexes, consumer, null);
	}
	
	public static <NVExtra extends NamedValueExtra, VExtra extends ValueExtra> boolean isEmpty(Value<NVExtra, VExtra> value) {
		if (value==null) return true;
		if (value instanceof GenericValue && ((GenericValue<?,?,?>) value).value==null) return true;
		ArrayValue<NVExtra, VExtra> array = value.castToArrayValue();
		if (array!=null && array.value!=null && array.value.isEmpty()) return true;
		ObjectValue<NVExtra, VExtra> object = value.castToObjectValue();
		if (object!=null && object.value!=null && object.value.isEmpty()) return true;
		StringValue<NVExtra, VExtra> string = value.castToStringValue();
		if (string!=null && string.value!=null && string.value.isEmpty()) return true;
		
		return false;
	}
}
