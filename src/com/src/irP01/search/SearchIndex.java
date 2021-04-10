package com.src.irP01.search;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.FSDirectory;

public class SearchIndex {
	public IndexSearcher indexSearcher;
	public IndexReader indexReader;
	public Query query;
	public int numberOfResultsToDisplay;
	public String indexPath;
	public String searchQuery;

	public SearchIndex(String indexPath, String q) {
		this.indexPath = indexPath;
		this.searchQuery = q;
	}

	/**
	 * Method to initializes the objects which are required for the searching and
	 * ranking.
	 * 
	 * @throws IOException
	 * @throws ParseException
	 */
	public void initializeSearchAndRankParamters() throws IOException, ParseException {

		String[] fields = { "title", "contents" };

		numberOfResultsToDisplay = 20;
		indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
		indexSearcher = new IndexSearcher(indexReader);

		System.out.println("\nUsing Vector Space Model to rank");
		ClassicSimilarity model = new ClassicSimilarity();
		indexSearcher.setSimilarity(model);

		Analyzer analyzer = new StandardAnalyzer();

		MultiFieldQueryParser multiFieldQueryparser = new MultiFieldQueryParser(fields, analyzer);

		query = multiFieldQueryparser.parse(searchQuery);

		System.out.println("\nSearching For: " + searchQuery + "\n");

	}

	/**
	 * Method for get the search results and prints the documents rank, file name,
	 * path, last modification time and relevance score.
	 * 
	 * @throws IOException
	 */
	public void performSearch() throws IOException {
		TopDocs indexResults = indexSearcher.search(query, numberOfResultsToDisplay);
		ScoreDoc[] documentHits = indexResults.scoreDocs;
		int numTotalHits = Math.toIntExact(indexResults.totalHits);

		System.out.println(numTotalHits + " Matching Documents Found");

		for (int index = 0; index < numTotalHits; index++) {
			Document document = indexSearcher.doc(documentHits[index].doc);
			if (document.get("path") != null) {
				System.out.println("Rank - " + (index + 1));
				if (document.get("path").contains("html")) {
					System.out.println("Title - " + document.get("title"));
					System.out.println("Summary - " + document.get("summary"));
				}
				System.out.println("Path: " + document.get("path"));
				Date date = new Date(Long.parseLong(document.get("lastModificationTime")));
				System.out.println("Last Modification Time: " + date);
				System.out.println("Relevance Score: " + documentHits[index].score + "\n");
			} else {
				System.out.println((index + 1) + ". " + "No path exists for the document");
			}
		}
	}

}
