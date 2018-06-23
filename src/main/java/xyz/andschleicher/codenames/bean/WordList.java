package xyz.andschleicher.codenames.bean;

import java.util.List;

public class WordList {
	private String name;
	private List<Word> words;

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<Word> getWords() {
		return words;
	}
	public void setWords(List<Word> words) {
		this.words = words;
	}
	
	public class Word {
		private String word;

		public String getWord() {
			return word;
		}
		public void setWord(String word) {
			this.word = word;
		}
	}
}
