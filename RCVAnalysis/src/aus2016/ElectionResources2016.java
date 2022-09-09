package aus2016;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import affinity.BallotStream;
import affinity.DataInterface;
import affinity.ElectionResource;

/**
 * Implementation of the {@link ElectionResource} interface for the 2016
 * Australia Senate Election. 
 * @author Michael Peeler
 * @version August 3rd, 2022
 *
 */
public class ElectionResources2016 implements ElectionResource {

	// File name variables.
	
	/** The file that the party order and the list of unique party names will be taken from. */
	private static final String ORDER_FILE_2016 = "data\\2016\\2016PartyOrder.txt";
	/** The file that the alias names and the unique name they correspond to will be taken from. */
	private static final String ALIAS_FILE_2016 = "data\\2016\\2016Aliases.txt";
	/** The file that provides a mapping from ballot position to candidate name. */
	private static final String ID_FILE_2016 = "data\\2016\\aec-senate-candidateinformation-20499.csv";
	/** The location of the directory with ballot data for the 2016 election. */
	private static final String DATA_LOCATION_2016 = "data\\2016\\data";

	
	// Information about the specific election.
	
	/** Minimum number of ranked parties above the line needed to have a valid 2016 ballot. */
	private static final int MINIMUM_ABOVE_2016 = 1;
	/** Minimum number of ranked candidates below the line needed to have a valid 2016 ballot. */
	private static final int MINIMUM_BELOW_2016 = 6;
	/** The name of the election this ElectionResource is for. */
	private final static String ELECTION_NAME = "Australia 2016";
	
	// Implementation of abstract methods.
	
	/**
	 * Returns a new DataInterface using the string provided as the file location.
	 */
	@Override
	public DataInterface newDataInterface(String fileName) {
		return new Data2016(ID_FILE_2016, fileName);
	}
	
	/**
	 * Returns a list of all parties competing in the election.
	 */
	@Override
	public ArrayList<String> getPartyList() {

		ArrayList<String> parties = new ArrayList<>();

		Scanner pList = null;
		try {
			pList = new Scanner(new FileInputStream(ORDER_FILE_2016));
		} catch (FileNotFoundException e) {
			System.err.println("Order File not found when instantiating BallotStream2016");
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
		
		Map<String, String> alias = new HashMap<>();
		Scanner aList = null;
		try {
			aList = new Scanner(new FileInputStream(ALIAS_FILE_2016));
		} catch (FileNotFoundException e) {
			System.out.println("Alias File not found when instantiating BallotStream2019");
		}
		
		while (aList.hasNextLine()) {
			
			String[] temp =  aList.nextLine().split(",");
			if (temp.length > 1) alias.put(temp[0], temp[1]);
			
		}
				
		return alias;
	}

	/**
	 * Provides the location of the ballot data.
	 * @return
	 */
	@Override
	public String getDataLocation() {	return DATA_LOCATION_2016; }
	
	/**
	 * Name of the election the ElectionResource services.
	 * @return Election name.
	 */
	@Override
	public String getElectionName() { return ELECTION_NAME; }
	
	/**
	 * Returns the minimum number of options ranked below for a below the line vote to be 
	 * counted as valid.
	 */
	@Override
	public int minRankedBelow() { return MINIMUM_BELOW_2016; }
	
	/**
	 * Returns the minimum number of options ranked above for an above the line vote to be
	 * counted as valid.
	 */
	@Override
	public int minRankedAbove() { return MINIMUM_ABOVE_2016; }
	
	
	// Methods related to categorization
	
	@Override
	public void premadeCategorizations(Categories cat, BallotStream stream, 
			boolean useCategoriesPrimary, boolean useCategoriesSecondary) throws CategorizationException {
		switch (cat) {
		case ALL_AUSTRALIA:
			allAustraliaCategories(stream, useCategoriesPrimary, useCategoriesSecondary);
		case CROSS_ELECTION:
			multiElectionCategories(stream, useCategoriesPrimary, useCategoriesSecondary);
		case SIZE_BASED:
			sizeBasedCategories(stream, useCategoriesPrimary, useCategoriesSecondary);
		case UNCATEGORIZED:
			ElectionResource.decategorize(stream);
		default:
			throw new ElectionResource.NotDefinedException(cat.name() + " " + getElectionName());
		
		}
	}
	
	/**
	 * Provides the scheme to categorize the parties into size-based categories; sets the stream
	 * the this pre-defined category and determines what portion of the analysis will
	 * use categories, whether it be the primary or secondary votes.
	 * @param stream The {@link BallotStream} the categorization is being applied to.
	 * @param useCatsPrimary Whether categories will be used for primary votes.
	 * @param useCatsSecondary Whether categories will be used for secondary votes.
	 * @throws CategorizationException 
	 */
	private void sizeBasedCategories(BallotStream stream, boolean useCatsPrimary, boolean useCatsSecondary) 
			throws CategorizationException {
		String[] cats = new String[]{"Lib/Lab", "Parliamentary", "Minor", "Unnamed"};
		
		// Indices are indices in the OrderFile or the BallotStream's party list.
		int[] partiesFst = new int[] {6, 26};
		int[] partiesSnd = new int[] {18, 24, 25, 27, 32, 36, 49};
		int[] partiesTrd = new int[] {0, 1, 2, 3, 4, 5, 7, 8, 9, 10, 11, 13, 14, 15, 16, 17, 19, 21, 22, 28,
				29, 30, 31, 33, 34, 35, 37, 38, 40, 41, 42, 43, 44, 45, 46, 47, 48, 51, 52, 53};
		int[] partiesFrt = new int[] {12, 20, 23, 39, 50};
		int[][] parties = new int[][] {partiesFst, partiesSnd, partiesTrd, partiesFrt};
		
		ElectionResource.makeCategorization(stream, cats, parties, useCatsPrimary, useCatsSecondary);
	}
	
	/**
	 * Provides the scheme to categorize the parties into multi-election categories; 
	 * gives the same name and ordering to the 18 parties that ran in all 3 
	 * Australian federal elections. Sets the stream the this pre-defined category
	 * and determines what portion of the analysis will use categories, whether it 
	 * be the primary or secondary votes.
	 * @param stream The {@link BallotStream} the categorization is being applied to.
	 * @param useCatsPrimary Whether categories will be used for primary votes.
	 * @param useCatsSecondary Whether categories will be used for secondary votes.
	 * @throws CategorizationException 
	 */
	private void multiElectionCategories(BallotStream stream, boolean useCatsPrimary, boolean useCatsSecondary) 
			throws CategorizationException {
		String[] cats = new String[] {"Animal Justice Party", "Australian Christians", 
				"Australian Democrats", "Australian Labor Party", "Center Alliance", 
				"Derryn Hinch's Justice Party", "Jacqui Lambie Network", "Legalise Cannabis Australia", 
				"Liberal", "Liberal Democrats", "Pauline Hanson's One Nation", "Seniors United Party", 
				"Shooters Fishers and Farmers", "Socialist Alliance", "Socialist Equality Party", 
				"Sustainable Australia Party", "The Greens", "United Australia Party"};
		int[][] indices = new int[][] {
			{0}, 	{3}, 	{15}, 	{6}, 
			{32}, 	{17}, 	{24}, 	{28}, 
			{26}, 	{27}, 	{36}, 	{43}, 
			{44}, 	{45}, 	{46}, 	{47}, 
			{49}, 	{35}};
		
		ElectionResource.makeIncompleteCategorization(stream, cats, indices, useCatsPrimary, useCatsSecondary, "Other");
		
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
	private void allAustraliaCategories(BallotStream stream, boolean usingPrimaryGroups,
			boolean usingSecondaryGroups) throws CategorizationException {
		
		String[] groups = new String[] {"Animal Justice Party", "Australian Christians", "Australian Conservatives", 
				"Australian Democrats", "Australian Federation", "Australian Labor Party", "Centre Alliance", 
				"Christian Democratic Party", "Derryn Hinch's Justice Party", "Jacqui Lambie Network", 
				"Legalize Cannabis Australia", "Liberal / Nationalist", "Liberal Democrats", "Pauline Hanson's One Nation",
				"Seniors United Party", "Shooters Fishers and Farmers", "Socialist Alliance", "Socialist Equality Party", 
				"Sustaniable Australia Party", "The Greens", "United Australia Party", "Voluntary Euthanasia"};
		int[][] indices = new int[][] {
			{0}, 	{3}, 	{19}, 	{15}, 	
			{4}, 	{6}, 	{32}, 	{13}, 
			{17}, 	{24}, 	{28}, 	{26},
			{27}, 	{36}, 	{43}, 	{44}, 
			{45}, 	{46},	{47},	{49},
			{35}, 	{52}};
		
		ElectionResource.makeIncompleteCategorization(stream, groups, indices, usingPrimaryGroups, usingSecondaryGroups, "Other");
	}

}
