package com.src.irP01.indexConstruction;

import org.tartarus.snowball.ext.PorterStemmer;

public class Stemmer {

	public String parsedContents;

	public Stemmer(String parsedContents) {
		this.parsedContents = parsedContents;
	}

	/**
	 * Method to perform stemming for words.
	 * @return stemmedContents
	 */
	public String porterStemming() {
		String[] contents = parsedContents.split("\\s+");
		String wordsStemmed = "";
		PorterStemmer stemmer = new PorterStemmer();

		for (int index = 0; index < contents.length ; index++) {
			stemmer.setCurrent(contents[index]);
			stemmer.stem();
			String wordStem = stemmer.getCurrent();
			if (wordsStemmed.equalsIgnoreCase(""))
				wordsStemmed = wordStem;
			else {
				wordsStemmed += " ";
				wordsStemmed += wordStem;
			}
		}
		return wordsStemmed;
	}
}
