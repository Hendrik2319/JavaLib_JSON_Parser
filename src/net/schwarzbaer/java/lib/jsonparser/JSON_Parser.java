package net.schwarzbaer.java.lib.jsonparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Vector;

public class JSON_Parser {
	private File sourcefile;
	private ParseState currentState;

	public JSON_Parser( File sourcefile ) throws FileNotFoundException {
		this.sourcefile = sourcefile;
		this.currentState = new ParseState();
		if (!sourcefile.isFile()) throw new FileNotFoundException(String.format("Can't find file \"%s\".", sourcefile.getPath()));
	}

	public JSON_Object parse() {
		//return createTestObject();
		
		BufferedReader input;
		try { input = new BufferedReader(new InputStreamReader(new FileInputStream(sourcefile), StandardCharsets.UTF_8) ); }
		catch (FileNotFoundException e1) { e1.printStackTrace(); return null; }
		currentState.setParseInput(input);
		
		JSON_Object json_Object = null;
		
		try {
			currentState.skipWhiteSpaces();
			
			if (currentState.getChar()=='{') {
				currentState.setCharConsumed();
				json_Object = read_Object(input);
			}
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		
		try { input.close(); } catch (IOException e) {}
		
		return json_Object;
	}

	private Value read_Value(BufferedReader input) throws ParseException {
		currentState.skipWhiteSpaces();
		
		if (currentState.getChar()=='-' || ('0'<=currentState.getChar() && currentState.getChar()<='9')) {
			IntFloat number = read_Number(input);
			if (number.isInt  ()) return new IntegerValue(number.getInt  ());
			if (number.isFloat()) return new FloatValue  (number.getFloat());
			throw new ParseException("Parsed number should be integer or float.",currentState.getCharPos());
		}
		if (currentState.getChar()=='f' || currentState.getChar()=='t') {
			boolean bool = read_Bool(input);
			return new BoolValue(bool);
		}
		if (currentState.getChar()=='"') {
			String str = read_String(input);
			return new StringValue(str);
		}
		if (currentState.getChar()=='[') {
			currentState.setCharConsumed();
			JSON_Array array = read_Array(input);
			return new ArrayValue(array);
		}
		if (currentState.getChar()=='{') {
			currentState.setCharConsumed();
			JSON_Object obj = read_Object(input);
			return new ObjectValue(obj);
		}
		throw new ParseException("Unexpected character at beginning of value.",currentState.getCharPos());
	}

	private NamedValue read_NamedValue(BufferedReader input) throws ParseException {
		String name = null;
		
		currentState.skipWhiteSpaces();
		
		if (currentState.getChar()=='"') {
			name = read_String(input);
		} else
			throw new ParseException("Name string expected at beginning of object value.",currentState.getCharPos());
		
		currentState.skipWhiteSpaces();
			
		if (currentState.getChar()==':') {
			currentState.setCharConsumed();
		} else
			throw new ParseException("Character ':' expected after name string in object value.",currentState.getCharPos());
		
		Value value = read_Value(input);
		return new NamedValue(name, value);
	}

	private JSON_Object read_Object(BufferedReader input) throws ParseException {
		// pre: last char was consumed
		// post: last char is consumed
		
		JSON_Object json_Object = new JSON_Object();
		
		while (true) {
			
			currentState.skipWhiteSpaces();
				
			if (currentState.getChar()=='}') {
				currentState.setCharConsumed();
				return json_Object;
			}
				
			NamedValue value = read_NamedValue(input);
			json_Object.add(value);
			
			currentState.skipWhiteSpaces();
			
			if (currentState.getChar()==',') {
				currentState.setCharConsumed();
				continue;
			}
			
			if (currentState.getChar()=='}') {
				currentState.setCharConsumed();
				return json_Object;
			}
			
			throw new ParseException("Unexpected character after object value.",currentState.getCharPos());
		}
	}

	private JSON_Array read_Array(BufferedReader input) throws ParseException {
		// pre: last char was consumed
		// post: last char is consumed
		
		JSON_Array json_Array = new JSON_Array();
		
		while (true) {
			
			currentState.skipWhiteSpaces();
				
			if (currentState.getChar()==']') {
				currentState.setCharConsumed();
				return json_Array;
			}
				
			Value value = read_Value(input);
			json_Array.add(value);
			
			currentState.skipWhiteSpaces();
			
			if (currentState.getChar()==',') {
				currentState.setCharConsumed();
				continue;
			}
			
			if (currentState.getChar()==']') {
				currentState.setCharConsumed();
				return json_Array;
			}
			
			throw new ParseException("Unexpected character after object value.",currentState.getCharPos());
		}
	}

	private boolean read_Bool(BufferedReader input) throws ParseException {
		// pre: last char was NOT consumed
		// post: last char is consumed
		
		if (currentState.getChar()=='f') {
			if (!currentState.readKnownChars("alse")) 
				throw new ParseException("Unexpected keyword.",currentState.getCharPos());
			return false;
		}
		if (currentState.getChar()=='t') {
			if (!currentState.readKnownChars("rue")) 
				throw new ParseException("Unexpected keyword.",currentState.getCharPos());
			return true;
		}
		throw new ParseException("Unexpected keyword.",currentState.getCharPos());
	}

	private IntFloat read_Number(BufferedReader input) throws ParseException {
		// pre: last char was NOT consumed
		// post: last char is NOT consumed
		
		boolean isNegative = false;
		boolean hasFraction = false;
		long    intValue = 0;
		long    fraction = 0;
		double  fractionFactor = 1;
		
		if (currentState.getChar()=='-') {
			currentState.setCharConsumed();
			isNegative = true;
		}
		
		try {
			while (!currentState.wasCharConsumed() || currentState.readChar()) {
				if ('0'<=currentState.getChar() && currentState.getChar()<='9') {
					intValue *= 10;
					intValue += (currentState.getChar()-'0');
					currentState.setCharConsumed();
				}
				else
					break;
			}
		} catch (IOException e1) {
			throw new ParseException("IOException while parsing a number.\r\nIOException: "+e1.getMessage(),currentState.getCharPos());
		}
		
		if (currentState.getChar()=='.') {
			hasFraction = true;
			
			try {
				while (currentState.readChar()) {
					if ('0'<=currentState.getChar() && currentState.getChar()<='9') {
						fractionFactor *= 10;
						fraction *= 10;
						fraction += (currentState.getChar()-'0');
						currentState.setCharConsumed();
					}
					else
						break;
				}
			} catch (IOException e1) {
				throw new ParseException("IOException while parsing a number.\r\nIOException: "+e1.getMessage(),currentState.getCharPos());
			}
		}
		
		if (hasFraction)
			return new IntFloat( (isNegative?-1:1) * (intValue + fraction/fractionFactor) );
		else
			return new IntFloat( (isNegative?-1:1) * (intValue) );
	}

	private String read_String(BufferedReader input) throws ParseException {
		// pre: last char was NOT consumed
		// post: last char is consumed
		
		if (currentState.getChar()!='"')
			throw new ParseException("Unexpected character to enclose strings.",currentState.getCharPos());
		
		char endChar = '"';
		StringBuilder sb = new StringBuilder();
		try {
			while (currentState.readChar()) {
				if (currentState.getChar()!=endChar)
					sb.append(currentState.getChar());
				else {
					currentState.setCharConsumed();
					break;
				}
			}
		} catch (IOException e1) {
			throw new ParseException("IOException while parsing.\r\nIOException: "+e1.getMessage(),currentState.getCharPos());
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
	
	public enum ValueType { Array, Object, String, Bool, Integer, Float }

	public static abstract class Value {
		public final ValueType valueType;

		public Value(ValueType valueType) {
			this.valueType = valueType;
		}
	}
	
	public static class GenericValue<T> extends Value {
		public T value;

		public GenericValue(T value, ValueType valueType) {
			super(valueType);
			this.value = value;
		}
	}
	
	public static class ArrayValue   extends GenericValue<JSON_Array>  { public ArrayValue  (JSON_Array  value) { super(value, ValueType.Array  ); } }
	public static class ObjectValue  extends GenericValue<JSON_Object> { public ObjectValue (JSON_Object value) { super(value, ValueType.Object ); } }
	public static class StringValue  extends GenericValue<String>      { public StringValue (String      value) { super(value, ValueType.String ); } }
	public static class BoolValue    extends GenericValue<Boolean>     { public BoolValue   (boolean     value) { super(value, ValueType.Bool   ); } }
	public static class IntegerValue extends GenericValue<Long>        { public IntegerValue(long        value) { super(value, ValueType.Integer); } }
	public static class FloatValue   extends GenericValue<Double>      { public FloatValue  (double      value) { super(value, ValueType.Float  ); } }
}
