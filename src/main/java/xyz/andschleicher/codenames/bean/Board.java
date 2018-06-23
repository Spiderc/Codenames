package xyz.andschleicher.codenames.bean;

import com.google.gson.Gson;

public class Board {
	private int wordLoc;
	private String word;
	private String type;
	private boolean revealed = false;
	
	public int getWordLoc() {
		return wordLoc;
	}
	public void setWordLoc(int wordLoc) {
		this.wordLoc = wordLoc;
	}
	public String getWord() {
		return word;
	}
	public void setWord(String word) {
		this.word = word;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public boolean isRevealed() {
		return revealed;
	}
	public void setRevealed(boolean revealed) {
		this.revealed = revealed;
	}
	
	public String toString() {
		Gson gson = new Gson();
		return "Board: " + gson.toJson(this);
	}
}
