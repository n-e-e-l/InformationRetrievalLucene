import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.Formatter;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.tartarus.snowball.ext.PorterStemmer;


 //	Text files will be indexed in single directory.

public class IndexingFile {

	public static void index(String Doc_Path, String Index_Path) {

		boolean create = true;

		final Path Document_Dir = Paths.get(Doc_Path);

		// Verification of the directory and if java virtual machine has the authorization.

		if (!Files.isReadable(Document_Dir)) {
			System.out.println("Document directory '" + Document_Dir.toAbsolutePath()
					+ "' do not exist or is not readable, please check the path");
			System.exit(1);
		}

		// Determine the time taken i.e. how much time it takes to index the documents.

		Date start = new Date();

		try {
			System.out.println("Index in directory '" + Index_Path + "'...");

			Directory dir = FSDirectory.open(Paths.get(Index_Path));

			// Created an object to do pre-processing i.e. lower case conversion and filtering stopwords.

			Analyzer analyzer = new StandardAnalyzer();

			// Save the configuration variables for indexing.

			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

			if (create) {
				// Existing index will be removed and new indexes is created.

				iwc.setOpenMode(OpenMode.CREATE);
			} else {
				// We add new documents to the existing indexes.

				iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
			}

			// The object initializes and updates the indexes.

			IndexWriter writer = new IndexWriter(dir, iwc);
			System.out.println("came out of Indexing to directory '" + Index_Path + "'...");

			// Start of the index generation process.

			Index_Docs(writer, Document_Dir);

			writer.close();

			// Calculation of processing time for indexing by comparing the system time.

			Date end = new Date();
			System.out.println("Documents are indexed In " + ((end.getTime() - start.getTime()) / 1000.0) + " seconds");

		} catch (IOException e) {
			System.out.println(" caught a " + e.getClass() + "\n with error message: " + e.getMessage());
		}

	}

	// Method to crawl the document folder and generate objects having files inside it.
	// Also add these objects.

	static void Index_Docs(final IndexWriter writer, Path path) throws IOException {
		System.out.println("in index documents");

		if (Files.isDirectory(path)) {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					try {
						// Traverse the directory.

						indexDocs(writer, file, attrs.lastModifiedTime().toMillis());
					} catch (IOException ignore) {
						// Omit the files which are not readable.

					}
					return FileVisitResult.CONTINUE;
				}
			});
		} else {
			System.out.println("in index document else");
			indexDocs(writer, path, Files.getLastModifiedTime(path).toMillis());
		}
	}

	// Documents are indexed.

	static void indexDocs(IndexWriter writer, Path file, long lastModified) throws IOException {

		String filename=file.getFileName().toString();
				int lastDot = filename.lastIndexOf('.');
				String extension = filename.substring(lastDot+1);

		if (extension.equalsIgnoreCase("html") || extension.equalsIgnoreCase("txt")) {


			System.out.println(filename);

			try (InputStream stream = Files.newInputStream(file)) {
				// Create new and empty objects.
				Document doc = new Document();

				// Path of file is added.
				Field pathField = new StringField("path", file.toString(), Field.Store.YES);
				doc.add(pathField);

				// Last modified timestamp is added.
				Field modifiedField = new LongPoint("modified", lastModified);
				doc.add(modifiedField);

				// Function call to parse HTML files.
				org.jsoup.nodes.Document document = parseHTML(file);

				// Title of the file is added.
				String title = document.title();
				Field titleField = new TextField("title", title, Field.Store.YES);
				doc.add(titleField);

				// Fetch HTML file contents and remove the syntax of standard HTML document.
				String parsedContents="";
				if(document.body()!=null)
				{
					document.body().text().replaceAll("&lt;body&gt;","<body>");

					document.body().text().replaceAll("&lt;/body&gt;","</body>");
					parsedContents = document.body().text();

				}


				// PorterStemming function is called to perform stemming on the data.
				String stemmedContents = PorterStemming(parsedContents);

				// Contents of files are added.
				Field contentsField = new TextField("contents", stemmedContents, Field.Store.NO);
				doc.add(contentsField);

				if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
					// Addition of documents into the newly created index.
					System.out.println("Adding " + file);
					writer.addDocument(doc);
				} else {
					// If index exists then update file, since previous version might exist.
					System.out.println("Updating " + file);
					writer.updateDocument(new Term("path", file.toString()), doc);
				}
			}
		}
	}

	// Fucntion to conduct stemming on HTML data.
	private static String PorterStemming(String parsedContents) {
		String stemmedContents = "";

		//  Object created.
		PorterStemmer stemmer = new PorterStemmer();

		// Words has been separated and stored as an array for easy traversal.
		String[] words = parsedContents.split("\\s+");

		// Traverse the words.
		for (String word : words) {
			stemmer.setCurrent(word);

			// Stem word which is being traversed.
			stemmer.stem();

			String stemmedWord = stemmer.getCurrent();

			if (stemmedContents.equalsIgnoreCase(""))
				stemmedContents = stemmedWord;
			else {

				// Include the stemmed word separated by spaces.
				stemmedContents += " ";
				stemmedContents += stemmedWord;
			}
		}
		// Return the stemmed words in a string format.
		return stemmedContents;
	}

	// Function to create a object to traverse through the HTML files.
	// And parse the documents.
private static org.jsoup.nodes.Document parseHTML(Path file) {

		Scanner scanner = null;
		String rawContents = null;

		String emptyHtml="<html><head><title></title></head><body></body></html>";
		try {
			scanner = new Scanner( new File(file.toString()) );
			if(scanner.hasNext())
			rawContents = scanner.useDelimiter("\\A").next();
			else
				rawContents=emptyHtml;
		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			scanner.close();
		}

		// Read HTML files.
		String filename=file.getFileName().toString();
		int lastDot = filename.lastIndexOf('.');
		String extension = filename.substring(lastDot+1);

		org.jsoup.nodes.Document document;
		if (extension.equalsIgnoreCase("html")) {
			 document= Jsoup.parse(rawContents.toString(),"",Parser.xmlParser());

		}
		else
		{

			 document= Jsoup.parse(rawContents.toString());

		}


		return document;
	}
}
