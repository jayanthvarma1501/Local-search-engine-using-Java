package com.src.irP01.main;
import java.io.IOException;
import org.apache.lucene.queryparser.classic.ParseException;
import java.util.Scanner;

import com.src.irP01.indexConstruction.*;
import com.src.irP01.search.*;

public class IrP01Main {

	public static void main(String[] args) {

		if (!args[0].equals("")) {
			String documentPath = args[0];
			Scanner sc = new Scanner(System.in); 
			System.out.println("Please enter search query:");
			String query = sc.nextLine(); 
			System.out.println(query);
			IndexDocs indexDocs = new IndexDocs(documentPath); 
			indexDocs.initializeIndexParameter();
			indexDocs.generateIndex();	
			System.out.println("Indexing completed");
			SearchIndex searchIndex = new SearchIndex(indexDocs.getIndexPath(), query);
			sc.close();
			try {
				searchIndex.initializeSearchAndRankParamters();
				searchIndex.performSearch();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		else {
			System.out.print("Document path is not correct\n");
			System.out.print("java -jar IR_P01.jar [document path]");
			return;
		}
	}

}
