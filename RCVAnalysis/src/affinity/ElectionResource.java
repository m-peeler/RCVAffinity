package affinity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * An interface that provides information about a specific election; this interface gives
 * the capacity to:<br>
 * 	- Get the parties running in the election<br>
 * 	- Generate a new {@link DataInterface} to read the data files for the election<br>
 * 	- Make an alias table for possible alternative names for the parties in the election<br>
 * 	- Get the name of the election<br>
 * 	- Get the location of the data<br>
 * 	- Get information on required number of above and / or below the line rankings
 * 		for a ballot to be formal.<br>
 * 	- Do additional, election-specific post-parsing processing on the ballots
 * 		from the election, such as setting them all to formal or changing the
 * 		names of candidates.<br>
 * 
 * @author Michael Peeler
 * @version August 3rd, 2022
 *
 */
public interface ElectionResource {
	
	public class CategorizationException extends Exception {
		private static final long serialVersionUID = 1L;
		
		public CategorizationException(String s) {
			super(s);
		}
	}
	
	/**
	 * Indicates that at least one party was not included in a category;
	 * since categorization must include all parties, the categorization was 
	 * thus not implemented.
	 */
	public class MissingPartyException extends CategorizationException {
		private static final long serialVersionUID = 1L;
		
		public MissingPartyException(String s) {
			super(s);
		}
	}
	
	/** 
	 * Indicates that the {@link BallotStream} had already begun processing ballots,
	 * and thus the categorization was not implemented.
	 */
	public class StreamInProgressException extends CategorizationException {
		private static final long serialVersionUID = 1L;
		
		public StreamInProgressException(String s) {
			super(s);
		}
	}
	
	/**
	 * Indicates that an interface-specified categorization was not overridden
	 * in the implenetation of the interface; it could be because the category
	 * of analysis does not make sense for the election, like if we used
	 * {@link ElectionResource#allAustraliaCategories(BallotStream, boolean, boolean)}
	 * on the New York City election.
	 */	
	public class NotDefinedException extends CategorizationException {
		private static final long serialVersionUID = 1L;

		public NotDefinedException(String s) {
			super(s);
		}
	}
	
	public static enum Categories {
		/**
		 * The default, uncategorized status.
		 */
		UNCATEGORIZED,
		/** 
		 * Category that groups Australian that run across
		 * any 3 elections into a shared name.
		 */
		ALL_AUSTRALIA,
		/**
		 * Category that gives standardized names to parties that run in a single 
		 * electorate, to allow comparisons between two years elections in
		 * the same electorate.
		 */
		CROSS_ELECTION,
		/**
		 * Splits up by size / popularity of party, allows trends in how
		 * size affects liklihood to vote.
		 */
		SIZE_BASED;
		
		@Override
		public String toString() {
			switch (this) {
			case UNCATEGORIZED:
				return "uncategorized";
			case ALL_AUSTRALIA:
				return "all-Australia";
			case CROSS_ELECTION:
				return "cross-election";
			case SIZE_BASED:
				return "size-based";
			default:
				return "";
			}
		}
		
		public String toTitle() {
			switch (this) {
			case ALL_AUSTRALIA:
				return "All Australia";
			case CROSS_ELECTION:
				return "Cross-Election";
			case SIZE_BASED:
				return "Size-Based";
			case UNCATEGORIZED:
				return "Uncategorized";
			default:
				return "";
			}
		}
	}

	// Abstract methods.
	
	/**
	 * Return a new DataInterface for the specific election, 
	 * with the provided file loaded into it.
	 */
	abstract public DataInterface newDataInterface(String fileName);
	
	/**
	 * Provides an array list of all political parties from the election
	 */
	abstract public ArrayList<String> getPartyList();
	
	/**
	 * Provides a map of all aliases and the unique name they correspond to.
	 * @param aliasFile
	 */
	abstract public Map<String, String> getAliasTable();
		
	/**
	 * Name of the election the ElectionResource services.
	 * @return Election name.
	 */
	abstract public String getElectionName();

	/**
	 * Provides the location of the ballot data, as given when instantiated.
	 * @return
	 */
	abstract public String getDataLocation();
	
	/**
	 * The minimum number of parties that must be ranked above the line for a
	 * ballot from this ballot stream to be valid.
	 * @return
	 */
	abstract public int minRankedAbove();
	
	/**
	 * The minimum number of candidates that must be ranked below the line for
	 * a ballot from this election to be valid.
	 * @return
	 */
	abstract public int minRankedBelow();
	
	/**
	 * Returns the output location of data analysis, which by default will be
	 * the parent directory of the location of data files.
	 * @return
	 */
	default public String getOutputLocation() {
		File f = new File(getDataLocation());
		return f.getParent() + "\\output";
	}
	
	// Default methods.
	
	/**
	 * Provides any additional processing that may be needed for an election; by default,
	 * no processing is done.
	 * @param ballot Current ballot.
	 * @return Ballot with processing done.
	 */
	
	default public Queue<String> datafileQueue() {
		
		File f = 				new File(getDataLocation());
		Queue<String> queue = 	new LinkedList<String>();
		
		if (f.isFile()) queue.add(getDataLocation());
		else if (f.isDirectory()) 
			for (String file : f.list()) queue.add(getDataLocation() + "//" + file);
		else
			System.out.println("The ElectionResource's data location is neither a file nor a directory.");

		return queue;
	}
	
	default public StandardBallot additionalProcessing(StandardBallot ballot) {
		return ballot;
	}
	
	default public void premadeCategorizations(Categories cat, BallotStream stream, 
			boolean useCategoriesPrimary, boolean useCategoriesSecondary) throws CategorizationException {
		switch (cat) {
		case UNCATEGORIZED:
			decategorize(stream);
		default:
			throw new NotDefinedException(null);
		}
	}
	
	public static void makeCategorization(BallotStream stream, String[] categories, int[][] parties,
			boolean usingPrimaryCats, boolean useSecondaryCats) throws CategorizationException {
		ArrayList<String> cats = new ArrayList<>();
		HashMap<String, String> categorizations = new HashMap<>();
		
		for (int i = 0; i < categories.length; i++) {
			String category = categories[i];
			cats.add(category);
			for (int ind : parties[i]) {
				categorizations.put(stream.indexPartyToName(ind), category);
			}
		}
		
		stream.categorizeParties(cats, categorizations, usingPrimaryCats, useSecondaryCats);
	}

	public static void makeIncompleteCategorization(BallotStream stream, 
			String[] categories, int[][] parties, boolean usingPrimaryCats,
			boolean usingSecondaryCats, String finalCategoryName) throws CategorizationException {
				
		HashMap<String, String> categorizations = new HashMap<>();
		ArrayList<String> cats = new ArrayList<>();
		
		if (categories.length != parties.length) throw new MissingPartyException(null);
		
		for (int i = 0; i < categories.length; i++) {
			cats.add(categories[i]);
			for (int j : parties[i]) categorizations.put(stream.indexPartyToName(j), categories[i]);
		}
		
		cats.add(finalCategoryName);
			
		// Deep searches to make sure that every party
		// appears somewhere in the categorization; if it doesn't
		// appear, it is added to the final category.
		
		for (int i = 0; i < stream.getNumberOfParties(); i++) {
			
			boolean contains = false;
			for (int[] membs : parties) 
				for (int memb : membs) 
					if (memb == i) 
						contains = true;
			
			if (!contains) {
				categorizations.put(stream.indexPartyToName(i), finalCategoryName);
			}
		}
		
		stream.categorizeParties(cats, categorizations, usingPrimaryCats, usingSecondaryCats);		
	}
	
	public static void decategorize(BallotStream stream) throws CategorizationException {
		stream.stopCategories();
	}
}
