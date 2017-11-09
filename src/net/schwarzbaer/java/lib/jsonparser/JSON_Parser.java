package net.schwarzbaer.java.lib.jsonparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

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
		
		JSON_Object json_Object = new JSON_Object();
		
		try {
			while (currentState.readChar()) {
//			while ((ch=input.read())>=0) {
				
				if (currentState.getChar()=='{') {
					read_Object(input, json_Object);
				}
				
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		try { input.close(); } catch (IOException e) {}
		
		return json_Object;
	}

	private void read_Object(BufferedReader input, JSON_Object json_Object) {
		try {
			int ch;
			while (currentState.readChar()) {
				
				if (currentState.getChar()=='"') {
					StringBuilder sb = new StringBuilder();
					read_String(input, sb);
				}
				
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// TODO Auto-generated method stub
	}

	private void read_String(BufferedReader input, StringBuilder sb) {
		// TODO Auto-generated method stub
	}

	@SuppressWarnings("unused")
	private JSON_Object createTestObject() {
		JSON_Object json_Object = new JSON_Object();
		json_Object.values.add(new JSON_Object.StringValue ("string","value"));
		json_Object.values.add(new JSON_Object.IntegerValue("int"   ,123));
		json_Object.values.add(new JSON_Object.FloatValue  ("float" ,123.456f));

		JSON_Object json_Object2 = new JSON_Object();
		json_Object2.values.add(new JSON_Object.StringValue ("string","value2"));
		json_Object2.values.add(new JSON_Object.IntegerValue("int"   ,1234));
		json_Object2.values.add(new JSON_Object.FloatValue  ("float" ,1234.56f));
		json_Object.values.add(new JSON_Object.ObjectValue("object",json_Object2));
		return json_Object;
	}
}
