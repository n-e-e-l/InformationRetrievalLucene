import java.io.BufferedReader;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.Date;
import java.io.InputStreamReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.analysis.Analyzer;



public class SearchingFiles {

	public static void search(String Index_Path, String q, String Ranking_Model) throws IOException, ParseException {

		String[] fields = {"title", "contents"};

		final int Max_Hits = 10;	// Number of results to be displayed

		// Initialize the index reader
		IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(Index_Path)));

		// Initialize the index searcher
		IndexSearcher searcher = new IndexSearcher(reader);

		// Getting the model for ranking and set the same
		if (Ranking_Model.equalsIgnoreCase("OK")) {
			System.out.println("\nUsing OKAPI BM25 Ranking Model...");
			BM25Similarity model = new BM25Similarity();
			searcher.setSimilarity(model);

		} else if(Ranking_Model.equalsIgnoreCase("VS")) {
			System.out.println("\nUsing Vector Space Model...");
			ClassicSimilarity model = new ClassicSimilarity();
			searcher.setSimilarity(model);
		}

		else {
			// Checking the ranking model given in the input
			System.out.println("Invalid Ranking Model Selected");
			System.exit(0);
		}

		Analyzer analyzer = new StandardAnalyzer();

		// MultifieldQueryParser will search for document field using only one instance which is created
		MultiFieldQueryParser MFQparser = new MultiFieldQueryParser(fields, analyzer);

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

		// Query is parsed and stored
		Query query = MFQparser.parse(q);

		System.out.println("\nSearching For: " + q + "\n");

		// Executing the search
		PerformSearch(in, searcher, query, Max_Hits);

		// Index reader is closed
		reader.close();

	}

	public static void PerformSearch(BufferedReader in, IndexSearcher searcher, Query query,
			int Max_Hits) throws IOException {

			Date startDate = new Date();

			// Track number of hits
			TopDocs results = searcher.search(query, Max_Hits);
			ScoreDoc[] hits = results.scoreDocs;
			int numTotalHits = Math.toIntExact(results.totalHits);

			// choose the lesser value between the maximum hits and actual hits recorded.
			//Based on the value we may have to iterate over maximum hit or actual hit
			int end = Math.min(Max_Hits, numTotalHits);

			Date endDate = new Date();

			System.out.println("Total " + numTotalHits + " Matching Documents Found in " + ((endDate.getTime() - startDate.getTime()) / 1000.0) + " seconds");
			System.out.println("Showing Top " + end + "\n");

			// Iterate over array containing hits
			for (int i = 0; i < end; i++) {

				Document doc = searcher.doc(hits[i].doc);

				String title = doc.get("title");
				String path = doc.get("path");
				double score = 	hits[i].score;

				if (path != null) {
					// print out the title and rank of document
					System.out.println((i+1) + ". " + title);

					// print out the  document path
					System.out.println("   Path: " + path);

					// print out the score of document
					System.out.println("   Score: " + score + "\n");

				} else {
					System.out.println((i+1) + ". " + "No path for this document exists");
				}
			}
	}
}
