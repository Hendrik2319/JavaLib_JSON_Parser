package net.schwarzbaer.java.lib.jsonparser;

import java.io.BufferedReader;
import java.io.IOException;

class ParseInput {
	
	final BufferedReader input;
	private int ch;
	private long charPos;
	private boolean charWasConsumed;
	private boolean isEOF;
	
	ParseInput(BufferedReader input) {
		this.input = input;
		ch = 0;
		charPos = -1;
		charWasConsumed = false;
		isEOF = false;
	}
	
	boolean readChar() throws IOException {
		ch = input.read();
		charWasConsumed = false;
		isEOF = (ch<0);
		if (!isEOF) ++charPos;
		return !isEOF;
	}
	
	char getChar() {
		return (char)ch;
	}

	long getCharPos() {
		return charPos;
	}

	void setCharConsumed() {
		charWasConsumed = true;
	}
	boolean wasCharConsumed() {
		return charWasConsumed;
	}

	boolean isEOF() {
		return isEOF;
	}

	public boolean isWhiteSpace() {
		return ch<=' ';
	}

	public void skipWhiteSpaces() {
		try {
			while (!wasCharConsumed() || readChar()) {
				if (!isWhiteSpace()) break;
				setCharConsumed();
			}
		} catch (IOException e1) { e1.printStackTrace(); }
	}

	public boolean readKnownChars(String string) {
		return readKnownChars(string, false);
	}

	public boolean readKnownChars(String string, boolean ignoreCase) {
		for (int i=0; i<string.length(); ++i) {
			try { if (!readChar()) return false; }
			catch (IOException e) { e.printStackTrace(); }
			char readChar = getChar();
			char strChar  = string.charAt(i);
			if (ignoreCase)
			{
				readChar = Character.toLowerCase(readChar);
				strChar  = Character.toLowerCase(strChar );
			}
			if (readChar != strChar )
				return false;
			else
				setCharConsumed();
		}
		return true;
	}
	
}