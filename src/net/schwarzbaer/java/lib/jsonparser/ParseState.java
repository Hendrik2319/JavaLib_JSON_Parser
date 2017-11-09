package net.schwarzbaer.java.lib.jsonparser;

import java.io.BufferedReader;
import java.io.IOException;

class ParseState {
	
	private int ch;
	private long charPos;
	private boolean charWasConsumed;
	private BufferedReader input;
	
	public ParseState() {
		this.ch = 0;
		this.charPos = -1;
		this.charWasConsumed = false;
	}
	
	boolean readChar() throws IOException {
		ch = input.read();
		++charPos;
		charWasConsumed = false;
		return ch>=0;
	}
	
	int getChar() {
		return ch;
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
	
	void setParseInput(BufferedReader input) {
		this.input = input;
	}
	
}