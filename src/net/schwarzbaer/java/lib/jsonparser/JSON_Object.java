package net.schwarzbaer.java.lib.jsonparser;

import java.util.Vector;

public class JSON_Object {
	
	public Vector<Value> values;
	
	public JSON_Object() {
		values = new Vector<>();
	}

	public static abstract class Value {
		public String name;
		
		public Value(String name) {
			this.name = name;
		}
	}
	
	public static class StringValue extends Value {
		public String value;
		
		public StringValue(String name, String value) {
			super(name);
			this.value = value;
		}
	}
	
	public static class ObjectValue extends Value {
		public JSON_Object value;

		public ObjectValue(String name, JSON_Object value) {
			super(name);
			this.value = value;
		}
	}
	
	public static class IntegerValue extends Value {
		public int value;

		public IntegerValue(String name, int value) {
			super(name);
			this.value = value;
		}
	}
	
	public static class FloatValue extends Value {
		public float value;

		public FloatValue(String name, float value) {
			super(name);
			this.value = value;
		}
	}
	
}
