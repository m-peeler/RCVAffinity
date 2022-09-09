
package affinity;


import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Queue;

/**
 * Adopted from code written by Marc. Using an {@link ElectionResource},
 * it will collect all party names and create an alphabetic list and a 
 * list where names from {@link DataInterface#getPartiesUnaltered()}
 * are paired with their value in {@link DataInterface#getParties()}
 * 
 * @author Marc D'Avanzo
 * @edited Michael Peeler
 * @version August 1st, 2022
 */
class PartyNameCollector {
	
	private static class PartyNamePairs implements Comparable<PartyNamePairs> {
		protected String cleanName;
		protected String origName;

		
		public PartyNamePairs (String orig, String clean) {
			origName = orig;
			cleanName = clean;
		}
		
		public String toString() {
			return origName + "\t" + cleanName;
		}
		
		public int compareTo(PartyNamePairs oth) {
			return cleanName.toLowerCase().compareTo(oth.cleanName.toLowerCase());
		}
	}
	
	/**
	 * Going through all files in the {@link ElectionResource} provided, collects 
	 * a list of parties contesting the election, with their cleaned name
	 * paired with their unaltered name.
	 * @param elecResor The {@link ElectionResource} that files and {@link DataInterface}
	 * instances will be drawn from.
	 * @return A list of {@link PartyNamePairs} representing the parties in the 
	 * election.
	 */
	private static ArrayList<PartyNamePairs> extractPartyNames(ElectionResource elecResor)
	{
		Queue<String> queue = elecResor.datafileQueue(); 
		ArrayList<PartyNamePairs> list = new ArrayList<>();
		
		while (!queue.isEmpty()) {
						
			DataInterface process = elecResor.newDataInterface(queue.remove());
			
			ArrayList<String> unalt = process.getPartiesUnaltered();
			ArrayList<String> cleaned = process.getParties();
			
			for (int i = 0; i < unalt.size(); i++) {
				PartyNamePairs p = new PartyNamePairs(unalt.get(i), cleaned.get(i));
				if (!list.contains(p)) list.add(p);
			}

		}
		
		Collections.sort(list);
		return list;
				
	}
	
	/**
	 * Writes provided {@link PartyNamePairs} ArrayList to the provided file, 
	 * including the {@link PartyNamePairs#origName} with the 
	 * {@link PartyNamePairs#cleanName}, as well as the {@link PartyNamePairs#cleanName}
	 * with the {@link PartyNamePairs#cleanName} (So that alias tables constructed from
	 * it will not result in official party names yielding null when entered into them.
	 * AKA an AliasFile.
	 * @throws IOException
	 * @param list The list whose party pairs are being added to file.
	 */
	private static void writeList(ArrayList<PartyNamePairs> list, String outputFile) throws IOException
	{
		PrintWriter docWriter = new PrintWriter(new FileWriter(outputFile, false));

		for(PartyNamePairs temp: list)
		{
			docWriter.println(temp.toString());
			docWriter.println(temp.cleanName + "\t" + temp.cleanName);
		}
		
		docWriter.close();
	}
	
	/**
	 * Takes the list provided and writes to the file provided the 
	 * {@link PartyNamePairs#cleanName} of every {@link PartyNamePair} 
	 * included. The ordering of this file will become the ordering in
	 * any future {@link ElectionResource} and, by proxy, {@link BallotStream}
	 * where this is the OrderFile.
	 */
	private static void writeParties(ArrayList<PartyNamePairs> list, String outputFile) throws IOException {
		
		PrintWriter docWriter = new PrintWriter(new FileWriter(outputFile, false));

		for (PartyNamePairs temp: list) {
			docWriter.println(temp.cleanName);
		}
		
		docWriter.close();
	}
	
	/**
	 * When called with an {@link ElectionResource} instance, it will generate two files, 
	 * the first at {@code orderedFileLoc} with an alphabetically ordered list of the 
	 * parties contesting the ballot, and the second at {@code aliasFileLoc} containing
	 * tab-delimited pairings of unaltered and altered party names from the {@link ElectionResource},
	 * as provided by the {@link DataInterface#getPartiesUnaltered()} and {@link DataInterface#getParties()}
	 * methods.
	 * @param elecResor
	 * @param orderedFileLoc
	 * @param aliasFileLoc
	 * @throws IOException
	 */
	public static void generateBasicAliasAndOrderedFiles(ElectionResource elecResor, 
			String orderedFileLoc, String aliasFileLoc) throws IOException {
		
		ArrayList<PartyNamePairs> list = extractPartyNames(elecResor);
		writeList(list, orderedFileLoc);
		writeParties(list, aliasFileLoc);

	} 

}
