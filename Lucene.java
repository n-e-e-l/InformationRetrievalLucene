import java.io.InputStreamReader;
import java.io.IOException;
import org.apache.lucene.queryparser.classic.ParseException;
import java.util.Scanner;



public class Lucene {

	public static void main(String[] args) {

		// Checking the input format from command line
		if (!args[0].equals("") && !args[1].equals("") && !args[2].equals("") && !args[3].equals("")) {
			/** As we are using cmd line input we need to separate the path name,
			 * indexing path where directory would be present,
			 * ranking model to be used and query to be parsed **/
			String Doc_Path = args[0];
			String Index_Path = args[1];
			String Ranking_Model = args[2];
			String query = args[3];


			//Passing the document location and indexing directory to save the indexed results
			IndexingFile.index(Doc_Path, Index_Path);
			System.out.println("Indexing completed");
			/** Will search for the query in the indexed files and
			 * then calculate the ranking based on the model selected
			**/
			try {
				SearchingFiles.search(Index_Path, query, Ranking_Model);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		else {
			System.out.print("Command line arguments are not in a valid format. Please follow the format as shown below\n");
			System.out.print("java -jar IR_P01.jar [document path] [indexed folder] [VS/OK] [query]\n" + "");
			return;
		}
	}

}
