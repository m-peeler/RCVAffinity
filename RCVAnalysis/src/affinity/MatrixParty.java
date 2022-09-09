package affinity;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A MatrixParty is roughly equivalent to a single row in the matrix. It is associated
 * with one of the primary options, and contains a data field for every possible secondary
 * option, as well as a spare data field for additional information as needed.
 * 
 * @author Michael Peeler
 * @version July 19th, 2022
 *
 */
public class MatrixParty {
	
	/**
	 * The name of the party the MatrixParty is associated with.
	 */
	private String _partyName;

	/**
	 * The map used to store the data fields for the secondary options in the MatrixParty.
	 */
	private Map<String, Integer> _secondPrefs;
	
	/**
	 * The string used as the map key for the spare slot.
	 */
	private static final String SPARE_SLOT = "-<.<- EXTRA SLOT AS NEEDED ->.>-";

	/**
	 * The string which provides the name for the spare slot.
	 */
	private String _extraString; 
	
	
	// Constructors
	
	/**
	 * Takes in the party name and the list of all secondary options.
	 * By default, sets the spare slot to count the number of ballots with
	 * no second choice.
	 * @param partyName The name of the primary option party that this MatrixParty
	 * corresponds to.
	 * @param partyList The list of secondary options that this MatrixParty will
	 * store data related to.
	 */
	public MatrixParty (String partyName, ArrayList<String> partyList) {
	
		this(partyName, partyList, StandardBallot.NO_SECOND_CHOICE);

	}

	/**
	 * Takes in the party name and the list of all secondary options, as well as
	 * a string that will be contained in the name of any secondary choice that should
	 * be added to the spare slot.
	 * @param partyName The name of the primary option party that this MatrixParty
	 * corresponds to.
	 * @param partyList The list of secondary options that this MatrixParty will
	 * store data related to.
	 * @param extraContains String that will be in the party name of any party that should
	 * be counted in the spare slot.
	 */
	public MatrixParty(String partyName, ArrayList<String> partyList, String extraContains) {
		_partyName = partyName;
		_extraString = extraContains;
		_secondPrefs = new HashMap<String, Integer>();
		
		for (int i = 0; i < partyList.size(); i++) {
			_secondPrefs.put(partyList.get(i), 0);
		}
		_secondPrefs.put(SPARE_SLOT, 0);
	}
	
	/**
	 * Adds a secondary preference of the string specified to the current MatrixParty.
	 * @param secondPref The name of the party the second preference should be applied to.
	 */
	public void addSecondPref(String secondPref) {
		
		if (!_secondPrefs.containsKey(secondPref)) { 
			if (secondPref.contains(_extraString)) {
				_secondPrefs.put(SPARE_SLOT, _secondPrefs.get(SPARE_SLOT) + 1);
			}
		}
		else _secondPrefs.put(secondPref, _secondPrefs.get(secondPref) + 1);
		
	}
	
	/**
	 * Returns the total number of secondary preferences for the string
	 * provided.
	 * @param secondPref The secondary option whose total preferences
	 * are being queried.
	 * @return The total number of second preferences for the specified
	 * party within the current MatrixParty; how many people who voted 
	 * for the initial choice chose this as a second choice.
	 */
	public int getSecondPref(String secondPref) {
		return _secondPrefs.get(secondPref);
	}
	
	/**
	 * Returns the total number of secondary preferences that fall into the spare
	 * slot for this MatrixParty.
	 * @return The number of spare slot preferences. 
	 */
	public int getSpareSlot() 		{ return _secondPrefs.get(SPARE_SLOT); }
	
	/**
	 * Returns the name of the primary option party that this MatrixParty 
	 * corresponds to.
	 * @return String name of the party.
	 */
	public String getPartyName() 	{ return _partyName; }
	
	/**
	 * Clears the matrix of preference totals.
	 */
	public void clearSecondPrefs() {
		for (String key : _secondPrefs.keySet()) {
			_secondPrefs.put(key, 0);
		}
	}
	
	/**
	 * Returns a set of all entries in the MatrixParty's map.
	 * @return
	 */
	public Set<Entry<String, Integer>> entrySet() { return _secondPrefs.entrySet(); }
	
}
