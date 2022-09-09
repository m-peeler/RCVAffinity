package aus2022;

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
 * Implementation of the {@link ElectionResource} interface for the 2019
 * Australia Senate Election. 
 * @author Michael Peeler
 * @version August 3rd, 2022
 *
 */
public class ElectionResources2022 implements ElectionResource {

	// File name variables.
	
	/** The file that the party order and the list of unique party names will be taken from. */
	private static final String ORDER_FILE_2022 = "data\\2022\\PartyOrder.txt";
	/** The file that the alias names and the unique name they correspond to will be taken from. */
	private static final String ALIAS_FILE_2022 = "data\\2022\\AliasList.txt";
	/** The directory that contains the ballot data for the 2019 Australia Senate election. */
	private static final String DATA_FILE_2022 = "data\\2022\\data";
	
	// Information about the specific election.
	
	/** Minimum number of ranked parties above the line needed to have a valid 2022 ballot. */
	private static final int MINIMUM_ABOVE_2022 = 1;
	/** Minimum number of ranked candidates below the line needed to have a valid 2022 ballot. */
	private static final int MINIMUM_BELOW_2022 = 6;
	/** The name of the election this BallotStream is for. */
	private static final String ELECTION_NAME = "Australia 2022";

	
	// Implementation of abstract methods.
	
	/**
	 * Returns a new DataInterface using the string provided as the file location.
	 */
	@Override
	public DataInterface newDataInterface(String fileName) {
		
		return new Data2022(fileName);

	}
	
	/**
	 * Returns a list of all parties competing in the election.
	 */
	@Override
	public ArrayList<String> getPartyList() {
		
		ArrayList<String> parties = new ArrayList<>();
		Scanner pList = null;
		try {
			pList = new Scanner(new FileInputStream(ORDER_FILE_2022));
		} catch (FileNotFoundException e) {
			System.out.println("Order File not found when instantiating BallotStream2019");
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
			aList = new Scanner(new FileInputStream(ALIAS_FILE_2022));
		} catch (FileNotFoundException e) {
			System.out.println("Alias File not found when instantiating BallotStream2019");
		}
		
		while (aList.hasNextLine()) {
			String[] temp = aList.nextLine().split("\t");
			if (temp.length > 1) alias.put(temp[0], temp[1]);
		}
		
		return alias;
		
	}

	/**
	 * Provides the location of the ballot data.
	 * @return
	 */
	@Override
	public String getDataLocation() {	return DATA_FILE_2022;	}

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
	public int minRankedBelow() { return MINIMUM_BELOW_2022; }
	
	/**
	 * Returns the minimum number of options ranked above for an above the line vote to be
	 * counted as valid.
	 */
	@Override
	public int minRankedAbove() { return MINIMUM_ABOVE_2022; }
	
	
	// Methods related to categorization.
	
	/**
	 * Sets the provided {@link BallotStream} to the categorization provided, or returns a
	 * {@link CategorizationStatus} indicating why it was unable to do so.
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
	private void sizeBasedCategories(BallotStream stream, boolean useCatssPrimary, boolean useCatsSecondary) 
			throws CategorizationException {
		String[] cats = new String[]{"Lib/Lab", "Parliamentary", "Minor", "Unnamed"};
		int[] partiesFst = new int[] {5, 24};
		int[] partiesSnd = new int[] {10, 20, 29, 40, 43};
		int[] partiesTrd = new int[] {1, 2, 3, 4, 6, 7, 9, 11, 12, 14, 15, 18, 19, 22, 23, 25, 31, 32, 33, 34, 35, 36, 37, 39, 41, 42, 44, 45};
		int[] partiesFrt = new int[] {0, 8, 13, 16, 17, 21, 26, 27, 28, 30, 38};
		int[][] parties = new int[][] {partiesFst, partiesSnd, partiesTrd, partiesFrt};
		
		ElectionResource.makeCategorization(stream, cats, parties, useCatssPrimary, useCatsSecondary);

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
	 */
	public void multiElectionCategories(BallotStream stream, boolean useCatsPrimary, boolean useCatsSecondary) 
		throws CategorizationException {
		String[] cats = new String[] {"Animal Justice Party", "Australian Christians", 
				"Australian Democrats", "Australian Labor Party", "Center Alliance", 
				"Derryn Hinch's Justice Party", "Jacqui Lambie Network", "Legalise Cannabis Australia", 
				"Liberal", "Liberal Democrats", "Pauline Hanson's One Nation", "Seniors United Party", 
				"Shooters Fishers and Farmers", "Socialist Alliance", "Socialist Equality Party", 
				"Sustainable Australia Party", "The Greens", "United Australia Party"};
		int[][] indices = new int[][] {
			{1}, 	{2}, 	{3}, 	{5}, 
			{27}, 	{11}, 	{20}, 	{23}, 
			{24}, 	{25}, 	{29},	{33}, 
			{34}, 	{35}, 	{36}, 	{37}, 
			{40}, 	{43}};
		
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
	public void allAustraliaCategories(BallotStream stream, boolean useCatsPrimary,
			boolean useCatsSecondary) throws CategorizationException {
		
		String[] cats = new String[] {"Animal Justice Party", "Australian Christians", "Australian Conservatives", 
				"Australian Democrats", "Australian Federation", "Australian Labor Party", "Centre Alliance", 
				"Christian Democratic Party", "Derryn Hinch's Justice Party", "Jacqui Lambie Network", 
				"Legalize Cannabis Australia", "Liberal / Nationalist", "Liberal Democrats", "Pauline Hanson's One Nation",
				"Seniors United Party", "Shooters Fishers and Farmers", "Socialist Alliance", "Socialist Equality Party", 
				"Sustaniable Australia Party", "The Greens", "United Australia Party", "Voluntary Euthanasia"};
		int[][] indices = new int[][] {
			{1}, 	{2}, 	{}, 	{3}, 	
			{4}, 	{5}, 	{27}, 	{}, 
			{11}, 	{20}, 	{23}, 	{24},
			{25}, 	{29}, 	{33}, 	{34}, 
			{35}, 	{36},	{37},	{40},
			{43}, 	{}};
		
		ElectionResource.makeIncompleteCategorization(stream, cats, indices, useCatsPrimary, useCatsSecondary, "Other");
	}
}
