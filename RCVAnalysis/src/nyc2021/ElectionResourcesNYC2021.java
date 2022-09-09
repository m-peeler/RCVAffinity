package nyc2021;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import affinity.DataInterface;
import affinity.ElectionResource;

/**
 * Implementation of the {@link ElectionResource} interface for the 2021
 * New York City primary. 
 * @author Michael Peeler
 * @version August 3rd, 2022
 *
 */
public class ElectionResourcesNYC2021 implements ElectionResource {

	/** 
	 * An enumeration of the potential races of analysis
	 * in the 2021 NYC primary; these races each had more than 5 candidates,
	 * so could potentially be investigated. The races are: 
	 * {@link #MAYORAL_DEM} and {@link #COMPTROLLER_DEM}.
	 *
	 */
	public static enum Race {
		/** The entry for the Democratic Mayoral primary. */
		MAYORAL_DEM,
		/** The entry for the Democratic Comptroller primary. */
		COMPTROLLER_DEM;
		
		@Override
		public String toString() {
			switch (this) {
			case COMPTROLLER_DEM:
				return "DEM Comptroller";
			case MAYORAL_DEM:
				return "DEM Mayor";
			default:
				return defaultRace().toString();
			}
		}
		
		public String toTitle() {
			switch (this) {
			case COMPTROLLER_DEM:
				return "Democratic Comptroller";
			case MAYORAL_DEM:
				return "Democratic Mayor";
			default:
				return defaultRace().toTitle();
			}
		}
		
		public String toCandFile() {
			switch (this) {
			case COMPTROLLER_DEM:
				return COMPTROLLER_DEM_CAND_ORDER_2021;
			case MAYORAL_DEM:
				return MAYORAL_DEM_CAND_ORDER_2021;
			default:
				return defaultRace().toString();
			}
		}
		
		public static Race defaultRace() {
			return MAYORAL_DEM;
		}
	}
	
	// File name variables.
	
	/** The file that the alias names and the unique name they correspond to will be taken from. */
	private static final String ALIAS_FILE_NYC_2021 = "data\\NYC2021\\2021P_CandidacyID_To_Name.csv";
	
	
	// Information about the specific election.
			
	/** Minimum number of ranked parties above the line needed to have a valid NYC 2021 ballot. */
	private static final int MINIMUM_ABOVE_NYC_2021 = 1;
	/** Minimum number of ranked candidates below the line needed to have a valid NYC 2021 ballot. */
	private static final int MINIMUM_BELOW_NYC_2021 = 1;
	/** The name of the election this ElectionResource is for. */
	private static final String ELECTION_NAME = "New York City 2021";
	/** The directory with the ballot data for the 2021 NYC primary. */
	private static final String DATA_FILE_NYC_2021 = "data\\NYC2021\\dataNYC";
	/** The candidate file for the Democratic Mayoral primary. */
	private static final String MAYORAL_DEM_CAND_ORDER_2021 = "data\\NYC2021\\DemMayorCandOrder.txt";
	/** The candidate file for the Democratic Comptroller primary. */
	private static final String COMPTROLLER_DEM_CAND_ORDER_2021 = "data\\NYC2021\\DemComptrollerCandOrder.txt";

	
	// Instance variables.
		
	/** The name of the specific race being compared. */
	private final Race _race;


	// Instantiation methods.
	
	public ElectionResourcesNYC2021(Race race) {
		_race = race;
	}
	
	
	// Implementation of abstract methods.
	
	/**
	 * Returns a new DataInterface using the string provided as the file location.
	 */
	@Override
	public DataInterface newDataInterface(String fileName) {
		
		return new DataNYC2021(_race.toCandFile(), ALIAS_FILE_NYC_2021, fileName, _race.toString());

	}
	
	/**
	 * Returns a list of all parties competing in the election.
	 */
	@Override
	public ArrayList<String> getPartyList() {
		
		ArrayList<String> parties = new ArrayList<>();
		Scanner pList = null;
		try {
			pList = new Scanner(new FileInputStream(_race.toCandFile()));
		} catch (FileNotFoundException e) {
			System.out.println(_race.toString() + " " + _race.toCandFile());
			System.out.println("Order File not found when instantiaing NYC Election");
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
			aList = new Scanner(new FileInputStream(ALIAS_FILE_NYC_2021));
		} catch (FileNotFoundException e) {
			System.out.println("Alias File not found when instantiating BallotStreamNYC2021");
			System.out.println(ALIAS_FILE_NYC_2021);
		}
		
		while (aList.hasNextLine()) {
			String[] temp = aList.nextLine().split(",");
			if (temp.length > 1) alias.put(temp[0], temp[1]);
		}
		
		for (String cand : getPartyList()) {
			alias.put(cand, cand);
		}
		
		return alias;
		
	}
	
	/**
	 * Provides the location of the ballot data, as given when instantiated.
	 * @return
	 */
	@Override
	public String getDataLocation() { return DATA_FILE_NYC_2021;	}
	
	/**
	 * Name of the election the ElectionResource services.
	 * @return Election name.
	 */
	@Override
	public String getElectionName() 	{ return ELECTION_NAME + " - " + _race.toTitle(); }
	
	/**
	 * Returns the minimum number of options ranked below for a below the line vote to be 
	 * counted as valid.
	 */
	@Override
	public int minRankedBelow() { return MINIMUM_BELOW_NYC_2021; }
	
	/**
	 * Returns the minimum number of options ranked above for an above the line vote to be
	 * counted as valid.
	 */
	@Override
	public int minRankedAbove() { return MINIMUM_ABOVE_NYC_2021; }

}
