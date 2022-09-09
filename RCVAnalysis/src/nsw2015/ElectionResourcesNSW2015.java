package nsw2015;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import affinity.StandardBallot;
import affinity.BallotStream;
import affinity.DataInterface;
import affinity.ElectionResource;

/**
 * Implementation of the {@link ElectionResource} interface for the 2015
 * New South Wales election. 
 * @author Michael Peeler
 * @version August 3rd, 2022
 *
 */
public class ElectionResourcesNSW2015 implements ElectionResource {

		/** The file that the party order and the list of unique party names will be taken from. */
		private static final String ORDER_FILE_NSW_2015 = "data\\NSW2015\\PartyOrder.txt";
		/** The file that the alias names and the unique name they correspond to will be taken from. */
		private static final String ALIAS_FILE_NSW_2015 = "data\\NSW2015\\AliasList.csv";
		/** The file that provides a mapping from ballot position to candidate name. */
		private static final String ID_FILE_NSW_2015 = "data\\NSW2015\\SGE2015 LC Candidates v1.csv";
		/** The file that contains ballot data for the NSW 2015 election. */
		private static final String DATA_FILE_NSW_2015 = "data\\NSW2015\\SGE2015 LC Pref Data_NA_State.txt";
		
		/** Minimum number of ranked parties above the line needed to have a valid NSW 2019 ballot. */
		private static final int MINIMUM_ABOVE_NSW_2015 = 1;
		/** Minimum number of ranked candidates below the line needed to have a valid NSW 2019 ballot. */
		private static final int MINIMUM_BELOW_NSW_2015 = 15;
		/** The name of the election this ElectionResource is for. */
		private static final String ELECTION_NAME = "New South Wales 2015";
					
		/**
		 * Returns a new DataInterface using the string provided as the file location.
		 */
		@Override
		public DataInterface newDataInterface(String fileName) {
			
			return new DataNSW2015(ID_FILE_NSW_2015, fileName);

		}
		
		/**
		 * Returns a list of all parties competing in the election.
		 */
		@Override
		public ArrayList<String> getPartyList() {
			
			ArrayList<String> parties = new ArrayList<>();
			
			Scanner pList = null;
			try {
				pList = new Scanner(new FileInputStream(ORDER_FILE_NSW_2015));
			} catch (FileNotFoundException e) {
				System.err.println("Order File not found when instantiating BallotStreamNSW2019");
			}
			
			while (pList.hasNextLine()) {
				String temp = pList.nextLine();
				parties.add(temp);
			}
			
			return parties;
		
		}
		
		/**
		 * Creates a table of all aliases for the election.
		 * Uses an class-shared CSV of the following setup:
		 * 		alias,real
		 * 		a,A
		 * 		mike,Michael
		 * This means that:
		 * 		alias 	becomes  real;
		 * 		a 		becomes	 a;
		 * 		mike	becomes	 Michael.
		 */
		@Override
		public Map<String, String> getAliasTable() {
			
			Map<String,String> alias = new HashMap<>();
			Scanner aList = null;
			try {
				aList = new Scanner(new FileInputStream(ALIAS_FILE_NSW_2015));
			} catch (FileNotFoundException e) {
				System.out.println("Alias File not found when instantiating BallotStream2015");
			}
			
			while (aList.hasNextLine()) {
				String[] temp = aList.nextLine().split(",");
				if (temp.length > 1) alias.put(temp[0], temp[1]);
			}
			return alias;
			
		}
		
		/**
		 * Provides the location of the ballot data, as given when instantiated.
		 * @return
		 */
		@Override
		public String getDataLocation() { return DATA_FILE_NSW_2015;	}
		
		/**
		 * Name of the election the ElectionResource services.
		 * @return Election name.
		 */
		@Override
		public String getElectionName() 	{ return ELECTION_NAME; }
		
		/**
		 * Returns the minimum number of options ranked below for a below the line vote to be 
		 * counted as valid.
		 */
		@Override
		public int minRankedBelow() 	{ return MINIMUM_BELOW_NSW_2015; }
		
		/**
		 * Returns the minimum number of options ranked above for an above the line vote to be
		 * counted as valid.
		 */
		@Override
		public int minRankedAbove() 	{ return MINIMUM_ABOVE_NSW_2015; }
		
		/**
		 * Provides additional processing for the ballot based on the election; for the 2019 NSW
		 * election, sets all ballots to true.
		 */
		@Override 
		public StandardBallot additionalProcessing(StandardBallot ballot) {
			return ballot.setFormal();
		}
		
		
		// Methods related to categorization.
		
		/**
		 * @throws CategorizationException 
		 */
		@Override
		public void premadeCategorizations(Categories cat, BallotStream stream, 
				boolean useCategoriesPrimary, boolean useCategoriesSecondary) throws CategorizationException {
			switch (cat) {
			case ALL_AUSTRALIA:
				allAustraliaCategories(stream, useCategoriesPrimary, useCategoriesSecondary);
			case CROSS_ELECTION:
				multiElectionCategories(stream, useCategoriesPrimary, useCategoriesSecondary);
			case UNCATEGORIZED:
				ElectionResource.decategorize(stream);
			default:
				throw new ElectionResource.NotDefinedException(cat.name() + " " + getElectionName());
			
			}
		}
		
		/**
		 * Provides the scheme to categorize the parties into multi-election categories; 
		 * gives the same name and ordering to the 8 parties that ran in both
		 * New South Wales elections. Sets the stream the this pre-defined category
		 * and determines what portion of the analysis will use categories, whether it 
		 * be the primary or secondary votes.
		 * @param stream The {@link BallotStream} the categorization is being applied to.
		 * @param useCatsPrimary Whether categories will be used for primary votes.
		 * @param useCatsSecondary Whether categories will be used for secondary votes.
		 */
		private void multiElectionCategories(BallotStream stream, boolean useCatsPrimary, boolean useCatsSecondary)
			throws CategorizationException {
			String[] cats = new String[] {"ANIMAL JUSTICE PARTY", "CHRISTIAN DEMOCRATIC PARTY (FRED NILE GROUP)", 
					"LABOR / COUNTRY LABOR", "LIBERAL", "SHOOTERS", "SOCIALIST AlLIANCE", "THE GREENS", 
					"VOLUNTARY EUTHANASIA PARTY"};
			int[][] indices = new int[][] {
				{0}, 	{5}, 	{11}, 	{12}, 
				{17}, 	{18}, 	{21}, 	{23}};
			
			ElectionResource.makeIncompleteCategorization(stream, cats, indices, useCatsPrimary, useCatsSecondary, "OTHER");
		}
		
		/**
		 * Provides the scheme to categorize the parties into All-Australia categories; 
		 * gives the same name and ordering to the 22 parties that ran in any 3 of the
		 * 5 Australian elections we have access to. Sets the stream the this pre-defined 
		 * category and determines what portion of the analysis will use categories, whether
		 * it be the primary or secondary votes.
		 * @param stream The {@link BallotStream} the categorization is being applied to.
		 * @param useGroupsPrimary Whether categories will be used for primary votes.
		 * @param useGroupsSecondary Whether categories will be used for secondary votes.
		 * @throws CategorizationException 
		 */
		private void allAustraliaCategories(BallotStream stream, boolean useCatsPrimary,
				boolean useCatsSecondary) throws CategorizationException {
			
			String[] cats = new String[] {"Animal Justice Party", "Australian Christians", "Australian Conservatives", 
					"Australian Democrats", "Australian Federation", "Australian Labor Party", "Centre Alliance", 
					"Christian Democratic Party", "Derryn Hinch's Justice Party", "Jacqui Lambie Network", 
					"Legalize Cannabis Australia", "Liberal / Nationalist", "Liberal Democrats", "Pauline Hanson's One Nation",
					"Seniors United Party", "Shooters Fishers and Farmers", "Socialist Alliance", "Socialist Equality Party", 
					"Sustaniable Australia Party", "The Greens", "United Australia Party", "Voluntary Euthanasia"};
			int[][] indices = new int[][] {
				{0}, 	{}, 	{}, 	{2}, 	
				{6}, 	{11}, 	{}, 	{5}, 
				{}, 	{}, 	{}, 	{12},
				{}, 	{}, 	{}, 	{17}, 
				{18}, 	{},		{},		{21},
				{}, 	{23}};
			
			ElectionResource.makeIncompleteCategorization(stream, cats, indices, useCatsPrimary, useCatsSecondary, "Other");
		}
	
}
