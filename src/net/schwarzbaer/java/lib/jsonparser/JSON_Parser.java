package net.schwarzbaer.java.lib.jsonparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Vector;

public class JSON_Parser {
	private File sourcefile;
	private ParseInput parseInput;

	public JSON_Parser( File sourcefile ) {
		this.sourcefile = sourcefile;
		this.parseInput = new ParseInput();
	}

	public JSON_Object parse() {
		//return createTestObject();
		
		JSON_Object json_Object = null;
		
		try ( BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(sourcefile), StandardCharsets.UTF_8) ) ) {
			parseInput.setReader(input);
			
			parseInput.skipWhiteSpaces();
			
			if (parseInput.getChar()=='{') {
				parseInput.setCharConsumed();
				json_Object = read_Object();
			}
			
		} catch (ParseException | IOException e) { e.printStackTrace(); }
		
		
		return json_Object;
	}

	private Value read_Value() throws ParseException {
		parseInput.skipWhiteSpaces();
		
		if (parseInput.getChar()=='-' || ('0'<=parseInput.getChar() && parseInput.getChar()<='9')) {
			IntFloat number = read_Number();
			if (number.isInt  ()) return new IntegerValue(number.getInt  ());
			if (number.isFloat()) return new FloatValue  (number.getFloat());
			throw new ParseException("Parsed number should be integer or float.",parseInput.getCharPos());
		}
		if (parseInput.getChar()=='f' || parseInput.getChar()=='t') {
			boolean bool = read_Bool();
			return new BoolValue(bool);
		}
		if (parseInput.getChar()=='"') {
			String str = read_String();
			return new StringValue(str);
		}
		if (parseInput.getChar()=='[') {
			parseInput.setCharConsumed();
			JSON_Array array = read_Array();
			return new ArrayValue(array);
		}
		if (parseInput.getChar()=='{') {
			parseInput.setCharConsumed();
			JSON_Object obj = read_Object();
			return new ObjectValue(obj);
		}
		throw new ParseException("Unexpected character at beginning of value.",parseInput.getCharPos());
	}

	private NamedValue read_NamedValue() throws ParseException {
		String name = null;
		
		parseInput.skipWhiteSpaces();
		
		if (parseInput.getChar()=='"') {
			name = read_String();
		} else
			throw new ParseException("Name string expected at beginning of object value.",parseInput.getCharPos());
		
		parseInput.skipWhiteSpaces();
			
		if (parseInput.getChar()==':') {
			parseInput.setCharConsumed();
		} else
			throw new ParseException("Character ':' expected after name string in object value.",parseInput.getCharPos());
		
		Value value = read_Value();
		return new NamedValue(name, value);
	}

	private JSON_Object read_Object() throws ParseException {
		// pre: last char was consumed
		// post: last char is consumed
		
		JSON_Object json_Object = new JSON_Object();
		
		while (true) {
			
			parseInput.skipWhiteSpaces();
				
			if (parseInput.getChar()=='}') {
				parseInput.setCharConsumed();
				return json_Object;
			}
				
			NamedValue value = read_NamedValue();
			json_Object.add(value);
			
			parseInput.skipWhiteSpaces();
			
			if (parseInput.getChar()==',') {
				parseInput.setCharConsumed();
				continue;
			}
			
			if (parseInput.getChar()=='}') {
				parseInput.setCharConsumed();
				return json_Object;
			}
			
			throw new ParseException("Unexpected character after object value.",parseInput.getCharPos());
		}
	}

	private JSON_Array read_Array() throws ParseException {
		// pre: last char was consumed
		// post: last char is consumed
		
		JSON_Array json_Array = new JSON_Array();
		
		while (true) {
			
			parseInput.skipWhiteSpaces();
				
			if (parseInput.getChar()==']') {
				parseInput.setCharConsumed();
				return json_Array;
			}
				
			Value value = read_Value();
			json_Array.add(value);
			
			parseInput.skipWhiteSpaces();
			
			if (parseInput.getChar()==',') {
				parseInput.setCharConsumed();
				continue;
			}
			
			if (parseInput.getChar()==']') {
				parseInput.setCharConsumed();
				return json_Array;
			}
			
			throw new ParseException("Unexpected character after object value.",parseInput.getCharPos());
		}
	}

	private boolean read_Bool() throws ParseException {
		// pre: last char was NOT consumed
		// post: last char is consumed
		
		if (parseInput.getChar()=='f') {
			if (!parseInput.readKnownChars("alse")) 
				throw new ParseException("Unexpected keyword.",parseInput.getCharPos());
			return false;
		}
		if (parseInput.getChar()=='t') {
			if (!parseInput.readKnownChars("rue")) 
				throw new ParseException("Unexpected keyword.",parseInput.getCharPos());
			return true;
		}
		throw new ParseException("Unexpected keyword.",parseInput.getCharPos());
	}

	private IntFloat read_Number() throws ParseException {
		// pre: last char was NOT consumed
		// post: last char is NOT consumed
		
		boolean isNegative = false;
		boolean hasFraction = false;
		long    intValue = 0;
		long    fraction = 0;
		double  fractionFactor = 1;
		
		if (parseInput.getChar()=='-') {
			parseInput.setCharConsumed();
			isNegative = true;
		}
		
		try {
			while (!parseInput.wasCharConsumed() || parseInput.readChar()) {
				if ('0'<=parseInput.getChar() && parseInput.getChar()<='9') {
					intValue *= 10;
					intValue += (parseInput.getChar()-'0');
					parseInput.setCharConsumed();
				}
				else
					break;
			}
		} catch (IOException e1) {
			throw new ParseException("IOException while parsing a number.\r\nIOException: "+e1.getMessage(),parseInput.getCharPos());
		}
		
		if (parseInput.getChar()=='.') {
			hasFraction = true;
			
			try {
				while (parseInput.readChar()) {
					if ('0'<=parseInput.getChar() && parseInput.getChar()<='9') {
						fractionFactor *= 10;
						fraction *= 10;
						fraction += (parseInput.getChar()-'0');
						parseInput.setCharConsumed();
					}
					else
						break;
				}
			} catch (IOException e1) {
				throw new ParseException("IOException while parsing a number.\r\nIOException: "+e1.getMessage(),parseInput.getCharPos());
			}
		}
		
		if (hasFraction)
			return new IntFloat( (isNegative?-1:1) * (intValue + fraction/fractionFactor) );
		else
			return new IntFloat( (isNegative?-1:1) * (intValue) );
	}

	private String read_String() throws ParseException {
		// pre: last char was NOT consumed
		// post: last char is consumed
		
		if (parseInput.getChar()!='"')
			throw new ParseException("Unexpected character to enclose strings.",parseInput.getCharPos());
		
		char endChar = '"';
		StringBuilder sb = new StringBuilder();
		try {
			while (parseInput.readChar()) {
				if (parseInput.getChar()!=endChar)
					sb.append(parseInput.getChar());
				else {
					parseInput.setCharConsumed();
					break;
				}
			}
		} catch (IOException e1) {
			throw new ParseException("IOException while parsing.\r\nIOException: "+e1.getMessage(),parseInput.getCharPos());
		}
		
		return sb.toString();
	}

	@SuppressWarnings("unused")
	private JSON_Object createTestObject() {
		JSON_Object json_Object = new JSON_Object();
		json_Object.add(new NamedValue("string", new StringValue ("value")));
		json_Object.add(new NamedValue("int"   , new IntegerValue(123)));
		json_Object.add(new NamedValue("float" , new FloatValue  (123.456f)));

		JSON_Object json_Object2 = new JSON_Object();
		json_Object2.add(new NamedValue("string", new StringValue ("value2")));
		json_Object2.add(new NamedValue("int"   , new IntegerValue(1234)));
		json_Object2.add(new NamedValue("float" , new FloatValue  (1234.56f)));
		json_Object.add(new NamedValue("object",new ObjectValue(json_Object2)));
		return json_Object;
	}

	private class IntFloat {

		private long intValue;
		private double floatValue;
		private boolean isInt;

		public IntFloat(long intValue) {
			this.intValue = intValue;
			this.floatValue = 0;
			this.isInt = true;
		}

		public IntFloat(double floatValue) {
			this.intValue = 0;
			this.floatValue = floatValue;
			this.isInt = false;
		}

		public boolean isInt() {
			return isInt;
		}

		public long getInt() {
			if (!isInt) throw new IllegalStateException("Can't call getInt(). Value is float.");
			return intValue;
		}

		public boolean isFloat() {
			return !isInt;
		}

		public double getFloat() {
			if (isInt) throw new IllegalStateException("Can't call getFloat(). Value is integer.");
			return floatValue;
		}
	
	}

	public static class ParseException extends Exception {
		private static final long serialVersionUID = -7641164253081256862L;

		public ParseException(String message, long pos) {
			super(String.format("Char[0x%08X]: %s", pos, message));
		}
	
	}
	
	public class JSON_Object extends Vector<NamedValue> {
		private static final long serialVersionUID = -8191469330084921029L;
	}
	
	public class JSON_Array extends Vector<Value> {
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
	}
	
	public static abstract class Value {
		
		public enum Type { Array, Object, String, Bool, Integer, Float }
		public final Type type;

		public Value(Type type) {
			this.type = type;
		}
	}
	
	public static class GenericValue<T> extends Value {
		public T value;

		public GenericValue(T value, Type type) {
			super(type);
			this.value = value;
		}
	}
	
	public static class ArrayValue   extends GenericValue<JSON_Array>  { public ArrayValue  (JSON_Array  value) { super(value, Type.Array  ); } }
	public static class ObjectValue  extends GenericValue<JSON_Object> { public ObjectValue (JSON_Object value) { super(value, Type.Object ); } }
	public static class StringValue  extends GenericValue<String>      { public StringValue (String      value) { super(value, Type.String ); } }
	public static class BoolValue    extends GenericValue<Boolean>     { public BoolValue   (boolean     value) { super(value, Type.Bool   ); } }
	public static class IntegerValue extends GenericValue<Long>        { public IntegerValue(long        value) { super(value, Type.Integer); } }
	public static class FloatValue   extends GenericValue<Double>      { public FloatValue  (double      value) { super(value, Type.Float  ); } }
}
