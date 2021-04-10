package com.src.irP01.indexConstruction;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;

public class IndexDocs {

	public String documentPath;
	public String indexPath;
	public Path documentDirectory;
	public IndexWriter writer;
	public File indexDirectory;

	public IndexDocs(String documentPath) {
		this.documentPath = documentPath;
	}

	/**
	 * Method to initializes the objects which are required for the indexing.
	 * Analyzer: The Analyzer generates tokens based on which the index terms are
	 * extracted. IndexWriterCongif:The class comprises of all the configuration is
	 * used to create an IndexWriter 
	 * IndexWriterConfig.setOpenMode(OpenMode.Create):Creates a new index if already 
	 * an index exists it simply overwrites it.
	 * IndexWriterConfig.setOpenMode(OpenMode.CreateORAPPEND): Create a new index if 
	 * there does not exists earlier ,if the index already exists it will open and
	 * simply append the document.
	 */
	public void initializeIndexParameter() {
		boolean create = true;

		documentDirectory = Paths.get(documentPath);

		// Check if directory exists
		if (!Files.isReadable(documentDirectory)) {
			System.out.println("Document directory '" + documentDirectory.toAbsolutePath()
					+ "' do not exist or is not readable, please check the path");
			System.exit(1);
		}

		try {
			indexDirectory = new File("index directory");
			indexDirectory.mkdir();
			Directory directory = FSDirectory.open(Paths.get(indexDirectory.getPath()));

			Analyzer analyzer = new StandardAnalyzer();

			IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);

			if (create) {
				indexWriterConfig.setOpenMode(OpenMode.CREATE);
			} else {
				indexWriterConfig.setOpenMode(OpenMode.CREATE_OR_APPEND);
			}

			writer = new IndexWriter(directory, indexWriterConfig);
		} catch (IOException e) {
			System.out.println(" caught a " + e.getClass() + "\n with error message: " + e.getMessage());
		}

	}

	/**
	 * Method to traverse through all the files in the directory 
	 * and sub-directory and ignores the files which are not readable, 
	 * later performs indexing and stores the results in the index directory.
	 */
	public void generateIndex() {
		System.out.println("in index documents");
		try {
			Path path = documentDirectory;
			if (Files.isDirectory(path)) {
				Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes fileAttributes) throws IOException {
						try {
							// Directory Traversals is done here.
							readAndFilterFiles(file, fileAttributes.lastModifiedTime().toMillis());
						} catch (IOException ignore) {
						}
						return FileVisitResult.CONTINUE;
					}
				});
			} else {
				System.out.println("in index document else");
				readAndFilterFiles(path, Files.getLastModifiedTime(path).toMillis());
			}
		} catch (IOException e) {
			System.out.println(e.getClass() + "\n with error: " + e.getMessage());
		}

	}

	/**
	 * Returns index directory path.
	 * @return string
	 */
	public String getIndexPath() {
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return indexDirectory.getPath();
	}

	/**
	 * Method to read, filter and parse text and HTML files.
	 * @param file
	 * @param lastModificationTime
	 * @throws IOException
	 */
	private void readAndFilterFiles(Path file, long lastModificationTime) throws IOException {
		String filename = file.getFileName().toString();
		int extensionDotIndex = filename.lastIndexOf('.');
		String fileExtension = filename.substring(extensionDotIndex + 1);

		if (fileExtension.equalsIgnoreCase("html") || fileExtension.equalsIgnoreCase("txt")) {

			System.out.println(filename);

			try (InputStream stream = Files.newInputStream(file)) {
				Document documentToAddInIndex = new Document();

				// Adding Path of file to document .
				Field pathField = new StringField("path", file.toString(), Field.Store.YES);
				documentToAddInIndex.add(pathField);

				// Adding Last modification time to the document.
				System.out.println(lastModificationTime);
				Field lastModificationTimeField = new StringField("lastModificationTime", Long.toString(lastModificationTime),
						Field.Store.YES);
				documentToAddInIndex.add(lastModificationTimeField);

				// Method call to parse files.
				org.jsoup.nodes.Document document = parseFiles(file);

				// Adding title of the file.
				String title = document.title();
				Field titleField = new TextField("title", title, Field.Store.YES);
				documentToAddInIndex.add(titleField);

				Field summaryField = new TextField("summary", document.getElementsByTag("summary").text(),
						Field.Store.YES);
				documentToAddInIndex.add(summaryField);

				// Fetching HTML file contents.
				String parsedContents = "";
				if (document.body() != null) {
					document.body().text().replaceAll("&lt;body&gt;", "<body>");
					document.body().text().replaceAll("&lt;/body&gt;", "</body>");
					parsedContents = document.body().text();
					parsedContents += " ";
					parsedContents += document.title();
					parsedContents += " ";
					parsedContents += document.getElementsByTag("time").text();
				}

				Stemmer porterStemmer = new Stemmer(parsedContents);
				// PorterStemming function is called to perform stemming on the data.
				String stemmedContents = porterStemmer.porterStemming();

				// Contents of files are added.
				Field contentsField = new TextField("contents", stemmedContents, Field.Store.NO);
				documentToAddInIndex.add(contentsField);

				if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
					// Addition of documents into the newly created index.
					System.out.println("Adding " + file);
					writer.addDocument(documentToAddInIndex);
				} else {
					// If index exists then update file, since previous version might exist.
					System.out.println("Updating " + file);
					writer.updateDocument(new Term("path", file.toString()), documentToAddInIndex);
				}
			}
		}
	}

	/**
	 * Method to read the contents of the text and HTML file.
	 * @param file
	 * @return org.jsoup.nodes.Document document
	 */
	private static org.jsoup.nodes.Document parseFiles(Path file) {
		Scanner scanner = null;
		String contents = null;
		String fileName = file.getFileName().toString();
		int extensionDotIndex = fileName.lastIndexOf('.');
		String extension = fileName.substring(extensionDotIndex + 1);
		String emptyHtml = "<html><head><title></title></head><body></body></html>";
		try {
			scanner = new Scanner(new File(file.toString()));
			if (scanner.hasNext())
				contents = scanner.useDelimiter("\\A").next();
			else
				contents = emptyHtml;
		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			scanner.close();
		}		
		org.jsoup.nodes.Document document;
		if (extension.equalsIgnoreCase("html")) {
			document = Jsoup.parse(contents.toString(), "", Parser.xmlParser());
		} else {
			document = Jsoup.parse(contents.toString());
		}
		return document;
	}
}
