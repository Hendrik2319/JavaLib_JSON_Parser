package net.schwarzbaer.java.lib.jsonparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import net.schwarzbaer.java.lib.jsonparser.JSON_Data.ArrayValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.BoolValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.FactoryForExtras;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.FloatValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.IntegerValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.JSON_Array;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.JSON_Object;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.NamedValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.NamedValueExtra;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Null;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.NullValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.ObjectValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.StringValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value.Type;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.ValueExtra;

public class JSON_Parser<NVExtra extends NamedValueExtra, VExtra extends ValueExtra> {
	private final File sourcefile;
	private final ParseInput parseInput;
	private final String json_text;
	private final FactoryForExtras<NVExtra, VExtra> factoryForExtras;

	public JSON_Parser( File sourcefile, FactoryForExtras<NVExtra, VExtra> factoryForExtras) {
		this(sourcefile,null,factoryForExtras);
		if (sourcefile==null) throw new IllegalArgumentException();
	}

	public JSON_Parser(String json_text, FactoryForExtras<NVExtra, VExtra> factoryForExtras) {
		this(null,json_text,factoryForExtras);
		if (json_text==null) throw new IllegalArgumentException();
	}

	private JSON_Parser(File sourcefile, String json_text, FactoryForExtras<NVExtra, VExtra> factoryForExtras) {
		this.sourcefile = sourcefile;
		this.json_text = json_text;
		this.factoryForExtras = factoryForExtras;
		this.parseInput = new ParseInput();
	}

	public static class Result<NVExtra extends NamedValueExtra, VExtra extends ValueExtra> {
	
		public final JSON_Array<NVExtra,VExtra> array;
		public final JSON_Object<NVExtra,VExtra> object;
	
		Result(JSON_Array<NVExtra,VExtra> array) {
			this.object = null;
			this.array = array;
		}
	
		Result(JSON_Object<NVExtra,VExtra> object) {
			this.object = object;
			this.array = null;
		}
	}

	public Result<NVExtra,VExtra> parse() {
		try {
			return parse_withParseException();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public Result<NVExtra,VExtra> parse_withParseException() throws ParseException {
		//return createTestObject();
		
		try (
				Reader in = sourcefile==null ? new StringReader(json_text) : new InputStreamReader(new FileInputStream(sourcefile), StandardCharsets.UTF_8);
				BufferedReader input = new BufferedReader(in );
		) {
			parseInput.setReader(input);
			parseInput.skipWhiteSpaces();
			char ch = parseInput.getChar();
			
			if (ch=='{') {
				parseInput.setCharConsumed();
				return new Result<>(read_Object());
			}
			if (ch=='[') {
				parseInput.setCharConsumed();
				return new Result<>(read_Array());
			}
			
		} catch (IOException e) { e.printStackTrace(); }
		
		
		return null;
	}
	
	private VExtra createValueExtra(Type type) {
		if (factoryForExtras==null) return null;
		return factoryForExtras.createValueExtra(type);
	}
	
	private NVExtra createNamedValueExtra(Type type) {
		if (factoryForExtras==null) return null;
		return factoryForExtras.createNamedValueExtra(type);
	}

	private Value<NVExtra,VExtra> read_Value() throws ParseException {
		parseInput.skipWhiteSpaces();
		
		char ch = parseInput.getChar();
		
		if (ch=='-' || ('0'<=ch && ch<='9')) {
			IntFloat number = read_Number();
			if (number.isInt  ()) return new IntegerValue<>(number.getInt  (), createValueExtra(Type.Integer));
			if (number.isFloat()) return new FloatValue<>  (number.getFloat(), createValueExtra(Type.Float));
			throw new ParseException(parseInput.getCharPos(),"Parsed number should be integer or float.");
		}
		if (ch=='n') {
			Null null_ = read_Null();
			return new NullValue<>(null_, createValueExtra(Type.Null));
		}
		if (ch=='f' || ch=='t') {
			boolean bool = read_Bool();
			return new BoolValue<>(bool, createValueExtra(Type.Bool));
		}
		if (ch=='"') {
			String str = read_String();
			return new StringValue<>(str, createValueExtra(Type.String));
		}
		if (ch=='[') {
			parseInput.setCharConsumed();
			JSON_Array<NVExtra,VExtra> array = read_Array();
			return new ArrayValue<>(array, createValueExtra(Type.Array));
		}
		if (ch=='{') {
			parseInput.setCharConsumed();
			JSON_Object<NVExtra,VExtra> obj = read_Object();
			return new ObjectValue<>(obj, createValueExtra(Type.Object));
		}
		throw new ParseException(parseInput.getCharPos(),"Unexpected character ('%s',#%d) at beginning of value.", ch, (int)ch);
	}

	private NamedValue<NVExtra,VExtra> read_NamedValue() throws ParseException {
		String name = null;
		
		parseInput.skipWhiteSpaces();
		
		char ch = parseInput.getChar();
		if (ch=='"') {
			name = read_String();
		} else
			throw new ParseException(parseInput.getCharPos(),"Name string expected at beginning of object value.");
		
		parseInput.skipWhiteSpaces();
		
		ch = parseInput.getChar();
		if (ch==':') {
			parseInput.setCharConsumed();
		} else
			throw new ParseException(parseInput.getCharPos(),"Character ':' expected after name string in object value. Got this: '%s',#%d", ch, (int)ch);
		
		Value<NVExtra,VExtra> value = read_Value();
		return new NamedValue<NVExtra,VExtra>(name, value, createNamedValueExtra(value.type));
	}

	private JSON_Object<NVExtra,VExtra> read_Object() throws ParseException {
		// pre: last char was consumed
		// post: last char is consumed
		
		JSON_Object<NVExtra,VExtra> json_Object = new JSON_Object<>();
		
		while (true) {
			
			parseInput.skipWhiteSpaces();
				
			char ch = parseInput.getChar();
			if (ch=='}') {
				parseInput.setCharConsumed();
				return json_Object;
			}
				
			NamedValue<NVExtra,VExtra> value = read_NamedValue();
			json_Object.add(value);
			
			parseInput.skipWhiteSpaces();
			
			ch = parseInput.getChar();
			if (ch==',') {
				parseInput.setCharConsumed();
				continue;
			}
			
			if (ch=='}') {
				parseInput.setCharConsumed();
				return json_Object;
			}
			
			throw new ParseException(parseInput.getCharPos(),"Unexpected character ('%s',#%d) after object value.", ch, (int)ch);
		}
	}

	private JSON_Array<NVExtra,VExtra> read_Array() throws ParseException {
		// pre: last char was consumed
		// post: last char is consumed
		
		JSON_Array<NVExtra,VExtra> json_Array = new JSON_Array<>();
		
		while (true) {
			
			parseInput.skipWhiteSpaces();
				
			char ch = parseInput.getChar();
			if (ch==']') {
				parseInput.setCharConsumed();
				return json_Array;
			}
				
			Value<NVExtra,VExtra> value = read_Value();
			json_Array.add(value);
			
			parseInput.skipWhiteSpaces();
			
			ch = parseInput.getChar();
			if (ch==',') {
				parseInput.setCharConsumed();
				continue;
			}
			
			if (ch==']') {
				parseInput.setCharConsumed();
				return json_Array;
			}
			
			throw new ParseException(parseInput.getCharPos(),"Unexpected character ('%s',#%d) after array value.", ch, (int)ch);
		}
	}

	private Null read_Null() throws ParseException {
		// pre: last char was NOT consumed
		// post: last char is consumed
		
		if (parseInput.getChar()=='n') {
			if (!parseInput.readKnownChars("ull")) 
				throw new ParseException(parseInput.getCharPos(),"Unexpected keyword.");
			return new Null();
		}
		throw new ParseException(parseInput.getCharPos(),"Unexpected keyword.");
	}

	private boolean read_Bool() throws ParseException {
		// pre: last char was NOT consumed
		// post: last char is consumed
		
		if (parseInput.getChar()=='f') {
			if (!parseInput.readKnownChars("alse")) 
				throw new ParseException(parseInput.getCharPos(),"Unexpected keyword.");
			return false;
		}
		if (parseInput.getChar()=='t') {
			if (!parseInput.readKnownChars("rue")) 
				throw new ParseException(parseInput.getCharPos(),"Unexpected keyword.");
			return true;
		}
		throw new ParseException(parseInput.getCharPos(),"Unexpected keyword.");
	}

	private IntFloat read_Number() throws ParseException {
		// pre: last char was NOT consumed
		// post: last char is NOT consumed
		
		// 7.706607796365006e-9
		
		boolean hasFraction = false;
		boolean isNegative = false;
		long    intValue = 0;
		long    fraction = 0;
		double  fractionFactor = 1;
		long    expValue = 0;
		boolean isExpNegative = false;
		
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
			throw new ParseException(parseInput.getCharPos(),"IOException while parsing a number.\r\nIOException: "+e1.getMessage());
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
				throw new ParseException(parseInput.getCharPos(),"IOException while parsing a number.\r\nIOException: "+e1.getMessage());
			}
		}
		
		if (parseInput.getChar()=='e') {
			try {
				if (parseInput.readChar()) {
					if (parseInput.getChar()=='-') {
						isExpNegative = true;
						parseInput.setCharConsumed();
					}
				}
				
				while (!parseInput.wasCharConsumed() || parseInput.readChar()) {
					if ('0'<=parseInput.getChar() && parseInput.getChar()<='9') {
						expValue *= 10;
						expValue += (parseInput.getChar()-'0');
						parseInput.setCharConsumed();
					}
					else
						break;
				}
			} catch (IOException e1) {
				throw new ParseException(parseInput.getCharPos(),"IOException while parsing a number.\r\nIOException: "+e1.getMessage());
			}
		}
		
		double expFactor = 1;
		if (expValue!=0) {
			for (int i=0; i<expValue; ++i)
				expFactor *= 10;
			if (isExpNegative)
				expFactor = 1/expFactor;
		}
		
		if (hasFraction) {
			if (expValue==0)
				return new IntFloat( (isNegative?-1:1) * (intValue + fraction/fractionFactor) );
			else
				return new IntFloat( (isNegative?-1:1) * (intValue + fraction/fractionFactor) * expFactor );
		} else {
			if (expValue==0)
				return new IntFloat( (isNegative?-1:1) * (intValue) );
			else
				return new IntFloat( (isNegative?-1:1) * (intValue) * expFactor );
		}
	}

	private String read_String() throws ParseException {
		// pre: last char was NOT consumed
		// post: last char is consumed
		
		char ch = parseInput.getChar();
		if (ch!='"')
			throw new ParseException(parseInput.getCharPos(),"Unexpected character ('%s',#%d) to enclose strings.", ch, (int)ch);
		
		char endChar = '"';
		char escapeChar = '\\';
		boolean nextCharIsEscaped = false;
		StringBuilder sb = new StringBuilder();
		try {
			while (parseInput.readChar()) {
				if (parseInput.getChar()==escapeChar)
					nextCharIsEscaped = true;
				else if (parseInput.getChar()!=endChar || nextCharIsEscaped) {
					sb.append(parseInput.getChar());
					nextCharIsEscaped = false;
				} else {
					parseInput.setCharConsumed();
					break;
				}
			}
		} catch (IOException e1) {
			throw new ParseException(parseInput.getCharPos(),"IOException while parsing.\r\nIOException: "+e1.getMessage());
		}
		
		return sb.toString();
	}

	@SuppressWarnings("unused")
	private JSON_Object<NVExtra,VExtra> createTestObject() {
		JSON_Object<NVExtra,VExtra> json_Object = new JSON_Object<>();
		json_Object.add(new NamedValue<>("string", new StringValue<> ("value",null),null));
		json_Object.add(new NamedValue<>("int"   , new IntegerValue<>(123,null),null));
		json_Object.add(new NamedValue<>("float" , new FloatValue<>  (123.456f,null),null));

		JSON_Object<NVExtra,VExtra> json_Object2 = new JSON_Object<>();
		json_Object2.add(new NamedValue<>("string", new StringValue<> ("value2",null),null));
		json_Object2.add(new NamedValue<>("int"   , new IntegerValue<>(1234,null),null));
		json_Object2.add(new NamedValue<>("float" , new FloatValue<>  (1234.56f,null),null));
		json_Object.add(new NamedValue<>("object",new ObjectValue<>(json_Object2,null),null));
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

		public ParseException(long pos, String message) {
			super(String.format("Char[0x%08X]: %s", pos, message));
		}
		public ParseException(long pos, String format, Object...args) {
			this(pos, String.format(Locale.ENGLISH, format, args));
		}
	
	}
}
