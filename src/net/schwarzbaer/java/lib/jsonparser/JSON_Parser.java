package net.schwarzbaer.java.lib.jsonparser;

import java.io.File;
import java.io.FileNotFoundException;

public class JSON_Parser {
	private File sourcefile;

	public JSON_Parser( File sourcefile ) throws FileNotFoundException {
		this.sourcefile = sourcefile;
		if (!sourcefile.isFile()) throw new FileNotFoundException(String.format("Can't find file \"%s\".", sourcefile.getPath()));
	}

	public JSON_Object parse() {
		// TODO Auto-generated method stub
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
