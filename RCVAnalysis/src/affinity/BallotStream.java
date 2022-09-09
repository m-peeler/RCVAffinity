package affinity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import affinity.ElectionResource.Categories;

/**
 * The key functionality is to provides the next ballot in the sequence for a specific
 * election. Additionally, this class:<br>
 * 	- Expands on the functionality of the <code>PartyHelper</code> class Connor developed to
 * 		turn index values into names and names into indices.<br>
 * 	- Allow one party to be "aliased" to another - for example, "The Greens (WA)" can be
 * 		treated the same as "The Greens".<br>
 * 	- Stores information on if a party competes on the ballot currently being read.<br>
 * 	- Performs additional evaluations on the ballots generated, such as adding them to a
 * 		SpectrumParty provided, or getting primary, secondary, or nth ranked preferences. <br>
 * 	- Allows for multiple parties to be categorized into multi-party categories for
 * 		analysis. <br>
 * 	- Can be queried for information on the current ballot that is modified in accord with
 * 		the settings regarding the current categorization. E.g. if {@link BallotStream#_useCtgrsSec} 
 * 		or {@link BallotStream#_useCtgrsPrim} are <code>true</code>, methods like
 * 		{@link BallotStream#getPrimNumOptions()}, {@link BallotStream#getSecOptions()},
 * 		{@link BallotStream#getPrimaryChoice()}, {@link BallotStream#getSecNameToIndex(String)} 
 * 		will return the values for the current ballot that pertain to the categorization. 
 * 
 * <b>Using {@link BallotStream}</b> <br><br>
 * 		A {@link BallotStream} can have settings altered, such as by categorizing parties together,
 * 		until the first time {@link #nextBallot()} is invoked; once ballots begin being processed,
 * 		settings are locked until {@link #restart} is called to restart the {@link BallotStream}.
 * 		To advance between ballots, {@link #nextBallot()} is called. A ballot will persist in the
 * 		{@link BallotStream} until either {@link #nextBallot()} is called, and it is replaced 
 * 		by a new {@link StandardBallot}, or if {@link #hasMoreBallots()} is called, at which point 
 * 		the current ballot is replaced by {@code null}, and data queries handled accordingly by returning
 * 		{@code null} or {@code -1}, as appropriate. This is because some implementations of 
 * 		{@link DataInterface} are ballot-destructive when {@link DataInterface#hasMoreBallots()} is
 * 		called; to avoid errors, ballots are made inaccessible after {@link #hasMoreBallots()} (and
 * 		by proxy {@link DataInterface#hasMoreBallots()}) is called.<br><br>
 * 
 * 		When {@link #_useCtgrsPrim} is set to true, all "primary" methods will return
 * 		category names; when {@link #_useCtgrsSec} is true, all "secondary". When generating
 * 		spectra with {@link SpectraMaker}, the ends of the spectrum will use the name of
 * 		whatever type {@link #_useCtgrsSec} specifies (because the spectrum is a measure of 
 * 		which party is prefereced after the primary vote), while the entries inside the 
 * 		spectrum will be of the type of {@link #_useCtgrsPrim}. For {@link Matrices},
 * 		the primary values will appear in the rows, and the secondary values will appear in the columns.
 * <br><br>
 * <b>Philosophy</b><br><br>
 * 		The general philosophy behind {@link BallotStream} is that it should be able to advance through 
 * 		all of the ballots in the election, conduct any pre-processing based on categorizations, aliases,
 * 		and the settings of {@link BallotStream#_useCtgrsPrim} and {@link BallotStream#_useCtgrsSec}
 * 		to provide the specific information that a user needs about a ballot. Further, to prevent the class 
 * 		using the ballots from accidentally changing the ballot state, the {@link StandardBallot} itself is 
 * 		kept solely within the {@link StandardBallot} so only data, and not mutability, is accessible outside.

 * <br><br>
 * <b>Categorization vs. Aliasing</b><br><br>
 * 	    The {@link BallotStream} contains the ability to do two similar but distinct categorizations of 
 * 		parties. The first is aliasing. When one party is aliased to another, they are treated 
 * 		identically; when the {@link BallotStream} receives a {@link StandardBallot} from the 
 *		{@link DataInterface}, it will immediately process it and replace the original party name
 *		on the ballot with the name of a party it has been aliased to. Since they are treated as
 *		the same party, only the first time that any candidate or party which aliases to the same
 *		party will be counted; subsequent votes will be ignored for functions like 
 *		{@link StandardBallot#primaryPreference}, {@link StandardBallot#secondPartyPreference},
 *		{@link StandardBallot#getFirstNParties}, and their BallotStream-handled interpreters of 
 *		{@link BallotStream#getPrimaryChoice}, {@link BallotStream#getSecondaryChoice()} and 
 *		{@link BallotStream#getSecondaryOrderedChoices()}. <br>
 *		[Red, Orange, Yellow, Green, Blue, Indigo, Violet]<br>
 *		As an example, let's assume we have the above ballot, and Red Party is aliased to Orange Party. 
 *		Internally, the ballot is now treated as: <br>
 *		[Orange, Orange, Yellow, Green, Blue, Indigo, Violet]<br>
 *		So, the {@link StandardBallot#primaryPreference()} would return Orange, rather than Red, and
 *		then the {@link StandardBallot#secondaryPrefrence()} would return Yellow, because it is 
 *		the next party that has not already appeared. <br><br>
 *		
 *		Categorization, similarly, allows multiple parties to be categorized together, but it functions 
 *		after aliasing has been run. So, if we had the same aliased ballot as above, while there were 7
 *		parties initially, there are only 6 parties post-aliasing that can be included in the 
 *		categorization: Orange, Yellow, Green, Blue, Indigo, Violet. We could then categorize a few of
 *		these parties together - Orange, Blue, Indigo, and Violet would be categorized into Dark, and Yellow
 *		and Green would be categorized into Light. The {@link StandardBallot} would now look like this:<br>
 *		Parties : [Orange, Orange, Yellow, Green, Blue, Indigo, Violet]<br>
 *		Categories: [Dark, Dark, Light, Light, Dark, Dark, Dark]<br>
 *		If we were to call {@link StandardBallot#secondCategoryPreference()}, we would get an output of 
 *		Light, because while the second vote is for Dark, it is for the same party as the primary vote. 
 *		So, we move down the vote preference until we get the the first non-repeated party, and the category
 *		of that party is returned as the output - in this case, the first non-repeated party is
 *		Yellow, so our output is Light.<br><br>
 *
 *		Similarly, if we reset the ballot to its original state, and no longer aliased Red to Orange, but instead 
 *		categorized it with Orange and Yellow into a category called Warm, and categorized the other four colors
 *		into a category called Cool, we would have the following {@link StandardBallot}:<br>
 *		Parties : [Red, Orange, Yellow, Green, Blue, Indigo, Violet]<br>
 *		Categories: [Warm, Warm, Warm, Cool, Cool, Cool, Cool]<br>
 *		Calling {@link StandardBallot#secondCategoryPreference()} would yield an output of Warm,
 *		because now Orange is the next unique party to appear.<br><br>
 *
 *		The difference in functionality between aliasing and categorization is primarily to service two different
 *		needs. Aliasing helps unify different naming conventions between states (for instance, "The Greens (WA)"
 *		becomes simply "The Greens", and we can treat it like The Greens in NSW and VIC), while categorization
 *		allows us to look at both inter- and inner- category relationships. An aliased adjacency matrix
 *		will always have its main diagonal equal to zero, since the second preference can never be the same as
 *		the first preference. This is desirable because so few people vote below the line that it is unlikely many
 *		ballots have the opportunity to rank their first choice party second, and for the sake of consistency it is
 *		easier to simply disregard those votes and instead count their vote as being for the next unique party. However,
 *		with categorization, since there are usually multiple parties in a category, there are now more opportunities for
 *		a voter to pick additional members of their primary category (And, for meaningful categories like "Left-Wing" or
 *		"Libertarian", this is precicesly the kind of behavior we would expect).<br><br>
 *		
 *		Sevearal weird things can come from this combination, however. First, spectrum values for categorized analysis should
 *		be disregared for the parties at either end of the spectrum if your categories only have a few members. Since votes
 *		for the primary party are disregarded when calculating later category preferences, like in the method 
 *		{@link StandardBallot#preferBetweenCats(String, String)}, categories with few parties will fall into strange spaces
 *		on the spectra. Using the {@link ElectionResource#multiElectionCategories(BallotStream, boolean, boolean)} on the 2022
 *		Australian election, for instance, will yield an Australian Labor Party to Liberal/National spectrum where
 *		the ALP scores a 1 (a.k.a. extremely pro-Liberal/National), while the Liberal/Nations score about 0.22 (a.k.a quite
 *		pro-Labor). [As an aside, the Liberals only score this high because, as a result of how we treat the coalition, they 
 *		are one of the few parties that can compete against themselves in a meaningful number of election; we currently alias
 *		Liberal, National, and Liberal/National Party of Queensland into a single "Liberal" party, but it may be smarter to 
 *		categorize them together instead for future analysis. Because they are able to compete, this spectrum value is slightly higher
 *		then the 0.0 spectrum value you would normally find for the Upper Bounds party in a categorized spectrum.] Before
 *		basing analysis on the values output, make certain they make intuitive sense, or if they are not intuitive, that they
 *		cannot be explained by one of the problems caused by having a few number of parties in a category, a few number of 
 *		competitors from a category, or the strange interaction between alised parties and categorized parties.
 *
 *		For futher information about how categorization is used in {@link StandardBallot}, read the documentation there,
 *		especially {@link StandardBallot#getFirstNCats(int)}. Additionally, 
 *		{@link ConsoleInteraction#userSelectsCategories(java.util.Scanner, boolean, boolean)} may be helpful.
 *
 * @author Michael Peeler
 * @version July 31th, 2022
 *
 */
public class BallotStream {
	
	// Instance variables relating to parties.
	
	/** 
	 * An object that provides information about the election being using and the location 
	 * of the files being accessed. When a new file is needed, or other values specific to
	 * an election are wanted, this object is queried.
	 */
	private final ElectionResource 		_elecInfo;
	/** A list of all unique parties that will appear in the data. */
	private final ArrayList<String> 	_parties;
	/** A hashtable indicating whether or not a party runs in the current state. */
	private Map<String, Boolean> 		_partyRunsInCurrentState;
	/** A hashtable indicating whether or not any members of a category run in the current state. */
	private Map<String, Boolean> 		_catRunsInCurrentState;
	/** A map that converts from aliases for a party into the unique party name 
	 *  the alias corresponds to, as the unique name appears in parties. */
	private final Map<String, String> 	_alias;
	
	
	// Instance variables relating to file access.
	
	/** The DataInterface object which standardizes file information 
	 *  and provides it to the BallotStream. */
	private DataInterface 		_data;
	/** The queue of files that ballots will be provided from. */
	private Queue<String> 		_files;
	
	
	// Instance variables relating to categorization.
	
	/** Indicates whether or not categories are currently active. */
	private boolean 			_ctgrsActive;
	/** Maps parties to the category they have been assigned. */
	private Map<String, String>	_categorization;
	/** List of all categories. */
	private ArrayList<String> 	_categories;
	/** Boolean indicating if the categories will be used for the secondary values */
	private boolean 			_useCtgrsSec;
	/** Boolean indicating if the categories will be used for the primary values */
	private boolean 			_useCtgrsPrim;
	
	
	/** Instance variables relating to the current ballot in the {@link BallotStream#}. */
	
	/** The total number of ballots that have been processed. */
	private int	 				_ballotsProcessed;
	/** The current ballot in the ballot stream. */
	private StandardBallot 		_curBallot;
	/** Whether or not debugging statements should be printed. */
	private boolean				_debug;
	
	
	// Instantiation and file access methods.
	
	/**
	 * Constructor that takes in an {@link ElectionResource} object and
	 * uses that to create a new {@link BallotStream}. By default,
	 * {@link #_debug} is set to {@code false}.
	 * @param ElectionResource Folder containing ballot data.
	 */
	public BallotStream(ElectionResource elecInfo) {
		this(elecInfo, false);
	}
	
	/**
	 * Constructor that takes in an {@link ElectionResource} object and
	 * a boolean that the {@link #_debug} will be set to. Creates a new
	 * {@link BallotStream} using this data, which will provide information
	 * about ballots in the election.
	 * @param elecInfo
	 * @param debug
	 */
	public BallotStream (ElectionResource elecInfo, boolean debug) {
		
		_elecInfo 			= elecInfo;
		_debug 				= debug;
		
		_files 				= new LinkedList<String>();
		_ballotsProcessed 	= 0;
		
		// Gets the specific party lists, alias tables,
		// and data locations for the election being
		// processed.
		_parties 			= _elecInfo.getPartyList();
		_alias 				= _elecInfo.getAliasTable();
		
		restart();
		
	}
	
	/**
	 * Restarts the BallotStream by filling the {@link #_files} queue with the files
	 * in {@link #_location} if the data location is a directory, or simply the file 
	 * at the specified path if the location is a single file. Also creates a new
	 * DataInterface that will interpret that data for the first file in the queue.
	 */
	public void restart() {
		
		/** Gets the queue of files that the {@link DataInterface} will draw from. */
		_files 				= _elecInfo.datafileQueue();
		_ballotsProcessed 	= 0;
				
		nextFile();
		
	}
	
	/**
	 * Advances the DataInterface to the next file in the {@link #_files} queue.
	 */
	protected void nextFile() 	{	
		if (_debug) System.err.println("Beginning processing of: " + _files.peek());
		_data = _elecInfo.newDataInterface(_files.remove()); 
		updateWhoIsRunning();
	}
	
	/**
	 * Provides the name of the current file being read from.
	 * @return String of the file name.
	 */
	public String getFileName() 	{ return _data.getCurFileName(); }
	
	/**
	 * @return The name of the election, as specified by the {@link ElectionResource}.
	 */
	public String getElectionName() {
		if (_ctgrsActive) 	return _elecInfo.getElectionName() + " - Using Categories";
		else 				return _elecInfo.getElectionName();
	}

	/**
	 * @return The {@link ElectionResource}'s {@link ElectionResource#getOutputLocation()}
	 */
	public String getOutputLocation() {
		return _elecInfo.getOutputLocation();
	}

	
	// Methods regarding whether parties or categories contest a specific state.
	
	/**
	 * Updates the map containing which parties and catgories are competing 
	 * on the current set of ballots; called if the {@link DataInterface} is
	 * changed or a categorization is implemented. 
	 */
	private void updateWhoIsRunning() {
		
		_partyRunsInCurrentState 	= new HashMap<String, Boolean>();
		_catRunsInCurrentState 		= new HashMap<String, Boolean>();

		// If categories are active, initalizes all categories to false.
		if (_ctgrsActive) {
			for (String cat : _categories) _catRunsInCurrentState.put(cat, false);
		}
		// Adds parties to the map with their status, and sets 
		// their category to true if they are running.
		for (String party : _parties) {
			_partyRunsInCurrentState.put(party, partyOnStateBallot(party));
			if (_ctgrsActive && _partyRunsInCurrentState.get(party)) 
				_catRunsInCurrentState.put(_categorization.get(party), true);
		}
		
	}
	
	/**
	 * Private helper method for {@link #updateWhoIsRunning()} indicating if the party 
	 * provided runs in the current state; uses the {@link DataInterface}'s current
	 * party list. Outside of {@link #updateWhoIsRunning()}, it is prefered that
	 * {@link #partyRunsInCurrentState(String)} is queried because this method has a
	 * worst case O(n) with regard to number of parties, whereas the other is O(1).
	 * @param party Party being queried
	 * @return If party is running in the state.
	 */
	private boolean partyOnStateBallot(String party) {
		for (int i = 0; i < _data.getPartiesIncludingSecondary().size(); i++) {
			// Debugging information to figure out why a party may be missing from the
			// alias map.
			if (_debug && realNameOf(_data.getPartiesIncludingSecondary().get(i)) == null) {
				System.out.println(i + " " + party + _data.getPartiesIncludingSecondary().get(i));
				for (Map.Entry<String, String> ent : _alias.entrySet()) {
					System.out.println(ent.getKey() + " " + ent.getValue());
				}
				for (String par : _data.getPartiesIncludingSecondary()) System.out.println(par);
			}
			
			if (nameEquality(_data.getPartiesIncludingSecondary().get(i), party)) return true;
		}
		return false;
	}
	
	/**
	 * Returns whether two parties are equivalent, factoring in party name aliases.
	 * @param fst First party being compared
	 * @param snd Second party being compared
	 * @return Boolean indicating equality.
	 */
	private boolean nameEquality(String fst, String snd) {
		
		if (_debug && realNameOf(fst) == null || realNameOf(snd) == null) {
			System.out.println("|" + fst + "| " + realNameOf(fst) + " |" + snd + "| " + realNameOf(snd) + " " + _elecInfo.getElectionName());
		}
		
		if (fst.equals(snd)) {
			return true;
		} else if (snd.equals(realNameOf(fst))) {
			return true;
		} else if (fst.equals(realNameOf(snd))) {
			return true;
		} else if (realNameOf(fst).equals(realNameOf(snd))) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Checks if parties run in the current state. A linear time operation if
	 * {@link #_alias} has been maintained by calling {@link #updateWhoIsRunning()}
	 * when a new {@link DataInstance} is started.
	 * @param party Party being checked
	 * @return Boolean indicating if the party runs in the current state.
	 */
	private boolean partyRunsInCurrentState(String party) {
		return _partyRunsInCurrentState.get(party);
	}
	
	/**
	 * Checks if categories run in the current state. A linear time operation if
	 * {@link #_alias} has been maintained by calling {@link #updateWhoIsRunning()}
	 * when a new {@link DataInstance} is started.
	 * @param party Category being checked
	 * @return Boolean indicating if the category runs in the current state.
	 */
	private boolean catRunsInCurrentState(String category) {
		return _ctgrsActive ? 
				_catRunsInCurrentState.get(category) : 
				false;
	}
	
	/**
	 * Checks if the string provided is the name of a primary choice option that is
	 * running in the current state. If it is, returns true. If it is not running 
	 * in the current state, or is not a valid option, returns false.
	 * @param primary Primary option name.
	 * @return boolean
	 */
	public boolean primRunsInCurrentState(String primary) {
		return usesCategoriesPrimary() ? 
				catRunsInCurrentState(primary) :
				partyRunsInCurrentState(primary);
	}
	
	/**
	 * Checks if the string provided is the name of a secondary choice option that is
	 * running in the current state. If it is, returns true. If it is not running 
	 * in the current state, or is not a valid option, returns false.
	 * @param secondary
	 * @return
	 */
	public boolean secRunsInCurrentState(String secondary) {
		return usesCategoriesSecondary() ? 
				catRunsInCurrentState(secondary) :
				partyRunsInCurrentState(secondary);
	}
	
	/**
	 * Returns an array list of the members of the specified category.
	 * @param category The category being sought out.
	 * @return The list of members of the category.
	 */
	public ArrayList<String> categoryMembers(String category) {
		ArrayList<String> rtrn = new ArrayList<>();
		
		// Gets all members of the category from the map.
		for (Entry<String, String> ent : _categorization.entrySet())
			if (ent.getValue().equals(category)) 
				rtrn.add(ent.getKey());
		
		return rtrn;
	}

	
	// Methods relating to the names, indices, and number of parties and categories.
	
	/**
	 * Fetches a party named based on the index entered.
	 * @param partyIndex party index entered
	 * @return the name of the party requested as a string
	 */
	public String indexPartyToName(int partyIndex) { 
		return _parties.get(partyIndex);	
	}
	
	/**
	 * Fetches a category name based on the index entered.
	 * @param index index of category in {@link BallotStream#_categories}.
	 * @return Category's name.
	 */
	public String indexCategoryToName(int index) { 
		return _categories.get(index);
	}

	/**
	 * Returns the index of a party name if it exists.
	 * @param name Name of party
	 * @return Index of party
	 */
	public int namePartyToIndex(String name) { 
		return _parties.indexOf(_alias.get(name));	
	}
	
	/**
	 * Returns the index of a category's name if the category exists.
	 * @param category Name of a category.
	 * @return Index of the category.
	 */
	public int nameCategoryToIndex(String category) {
		return _ctgrsActive ? 
				_categories.indexOf(_categorization.get(category)):
				-1;
	}
	
	/**
	 * Returns the list of unique parties.
	 * @return
	 */
	public ArrayList<String> getUniquePartyList() 		{ return _parties; }
	
	/**
	 * Returns the list of unique categories.
	 * @return
	 */
	public ArrayList<String> getUniqueCategoriesList() 	{ return _ctgrsActive ? _categories : null; }
	
	/**
	 * Returns the number of unique parties; equivalent to the length of
	 * {@link #getUniquePartyList}.
	 * @return Number of unique parties.
	 */
	public int getNumberOfParties() 			{ return _parties.size(); }
	
	/**
	 * Returns the number of unique categories; equivalent to the length of
	 * {@link #getUniqueCategoriesList}.
	 * @return Number of unique categories.
	 */
	public int getNumberOfCategories() 			{ return _categories.size(); }
	
	/**
	 * List of possible results of the primary preference.
	 * @return
	 */
	public ArrayList<String> getPrimOptions() {
		return usesCategoriesPrimary() ? getUniqueCategoriesList() : getUniquePartyList();
	}
	
	/**
	 * List of possible results of the secondary preference.
	 * @return
	 */
	public ArrayList<String> getSecOptions() {
		return usesCategoriesSecondary() ? getUniqueCategoriesList() : getUniquePartyList();
	}
	
	/**
	 * @return Number of possible primary options; the length of {@link #getPrimOptions()}.
	 */
	public int getPrimNumOptions() {
		return usesCategoriesPrimary() ? getNumberOfCategories() : getNumberOfParties(); 
	}
	
	/**
	 * @return Number of possible secondary options; the length of {@link #getSecOptions()}.
	 */
	public int getSecNumOptions() {
		return usesCategoriesSecondary() ? getNumberOfCategories() : getNumberOfParties();
	}
	
	/**
	 * Converts a party from a potential alias to its unique name.
	 * @param party Potentially aliased name.
	 * @return Unique name.
	 */
	public String realNameOf(String party) { 
		return _alias.get(party); 
	}
	
	/**
	 * Converts from a candidate name to the unique party they belong to.
	 * @param candidate Candidate
	 * @return Party candidate belongs to.
	 */
	public String partyOfCand(String candidate) { 
		return realNameOf(_data.partyOf(candidate)); 
	} 
		
	
	// Methods relating to progressing the BallotStream and operations on the current ballot.
	
	/**
	 * Checks if there are more ballots in the BallotStream.
	 * @return Boolean indicating if there are more ballots.
	 */
	public boolean hasMoreBallots() {
		
		// There are more ballots if the DataInterface has more ballots, or
		// if there are more files in the queue.
		_curBallot = null;
		if (_data.hasMoreBallots()) return true;
		else if (_files.isEmpty()) return false;
		else {
			nextFile();
			return hasMoreBallots();
		}
		
	}
	
	/**
	 * Provides the next StandardBallot in the sequence, or null if there are no more ballots.
	 * @return The next StandardBallot.
	 */
	public void nextBallot() {
		if (!hasMoreBallots()) {
			_curBallot = null;
			return;
		}
		_curBallot = _data.nextBallot();
	
		// Ensures that the ballot is using categories if the stream is using categories.
		if (_ctgrsActive && !_curBallot.usingCategories()) _curBallot.turnCategoriesOn();

		// De-alises the names of parties as they appear in the ballot, change the
		// ATL names into the unaliased names. Also adds the categories for the parties if
		// categorization is on.
		// Stops after the first non-party is found
		for (int i = 0; i < _curBallot.getAboveLine().length; i++) {
			if (!isParty(_curBallot.getAboveLine()[i])) {
				break;
			} else {
				_curBallot.setATL(i, realNameOf(_curBallot.getAboveLine()[i]), false);
				if (_ctgrsActive) {
					_curBallot.setCatsATL(i, _categorization.get(_curBallot.getAboveLine()[i]), false);
				}
			}
		}
		
		// Converts the candidates BTL into the unique name of the party they belong to.
		// Also adds the categories for the parties if categorization is on.
		// Stops after the first non-party is found
		for (int i = 0; i < _curBallot.getBelowLine().length; i++) {
			if (!isParty(_curBallot.getBelowLine()[i])) {
				break;
			} else {
				_curBallot.setBTL(i, partyOfCand(_curBallot.getBelowLine()[i]), false);
				if (_ctgrsActive) {
					_curBallot.setCatsBTL(i, _categorization.get(_curBallot.getBelowLine()[i]), false);
				}
			}
		}
		
		_ballotsProcessed ++;

		// Ensures the ballot is valid, allows the specific ElectionResource to conduct any 
		// additional post-processing that a specific election may require (mainly NSW elections,
		// where any ballot that makes it to this stage is set to be valid) and then 
		_curBallot = _elecInfo.additionalProcessing(_curBallot.validateBallot());
	
		return;
		
	}
	
	/**
	 * Returns a boolean indicating if a String is neither {@code null} nor 
	 * {@link StandardBallot#COLLISION}.
	 * @param par
	 * @return
	 */
	private boolean isParty(String par) {
		return (par != null) && !(par.equals(StandardBallot.COLLISION));
	}
	
	/**
	 * The primary choice on the current ballot; depending on the {@link #usesCategoriesPrimary()}
	 * value, which defaults to false but can be set to true when a party categorization is provided,
	 * it will be either a category (if true) or a party (if false).  
	 * @return The current ballot's primary choice.
	 */
	public String getPrimaryChoice() {
		if (_curBallot == null) return null;
		return usesCategoriesPrimary() ? _curBallot.primaryCategoryPreference() : _curBallot.primaryPreference();
	}

	/**
	 * Returns the index of the current ballot's primary choice.
	 * @return Index
	 */
	public int getPrimChoiceIndex() {
		if (_curBallot == null) return -1;
		return usesCategoriesPrimary() ? 
			nameCategoryToIndex(getPrimaryChoice()) : 
			namePartyToIndex(getPrimaryChoice());
	}
	
	/**
	 * Returns the secondary choice index of the string provided.
	 * @param name Secondary choice name
	 * @return Index
	 */
	public int getSecNameToIndex(String name) {
		if (_curBallot == null) return -1;
		return usesCategoriesSecondary() ?
				nameCategoryToIndex(name) :
				namePartyToIndex(name);
	}
	
	/**
	 * The secondary choice on the current ballot; depending on the {@link #usesCategoriesSecondary()} value,
	 * which defaults to false but can be set true when a party categorization is provided, it will be 
	 * either a category (if true) or a party (if false). If using parties, it will return the second
	 * unique party; if using categories, it will return the category of the second unique party. 
	 * @return The current ballot's primary choice.
	 */
	public String getSecondaryChoice() {
		if (_curBallot == null) return null;
		return usesCategoriesSecondary() ? _curBallot.secondCategoryPreference() : _curBallot.secondPreference();
	}
	
	/**
	 * The ordered choices of the current ballot; depending on the {@link #usesCategoriesSecondary()} value,
	 * which defaults to false but can be set true when a categorization is provided, it will be 
	 * either a list of categories (if true) or of parties (if false). If using parties, the list will
	 * contain the order in which the parties first appear; if using category, it will contain the primary
	 * party's category, and then the order that a non-primary party member of the category appears.
	 * 
	 * Let's say there are 4 parties, A-D, which each run two candidates, 1 and 2. Further, parties
	 * A and B are in category Z, and parties C and D are in category Y.
	 * 
	 * For a BTL ballot that looks like:
	 * A1, A2, C1, B2, B1, C2, D1, D2
	 * This becomes an ATL equivalent to:
	 * A, C, B, D
	 * 
	 * Then, if {@link #usesCategoriesSecondary()} is true, it becomes:
	 * Z, Y, Z
	 * Otherwise, it remains:
	 * A, C, B, D
	 * 
	 * @return List of ordered choices
	 */
	public String[] getSecondaryOrderedChoices() {
		if (_curBallot == null) return null;
		return usesCategoriesSecondary() ? 
				_curBallot.getFirstNCats(getNumberOfCategories() + 1) : 
				_curBallot.getFirstNParties(getNumberOfParties());
	}
		
	/**
	 * Taking a SpectrumParty, it adds the current preference between that party's upper and lower
	 * bounds to the SpectrumParty's preferences.
	 * @param spect SpectrumParty that will be updated.
	 */
	public void addPreferenceBetween(SpectrumParty spect) {
		if (_curBallot == null) return;
		if (usesCategoriesSecondary()) {
			spect.addPreference(_curBallot.preferBetweenCats(spect.getLowerBound(), spect.getUpperBound()));
		} else {
			spect.addPreference(_curBallot.preferBetweenParties(spect.getLowerBound(), spect.getUpperBound()));
		}
	
	}

	/**
	 * Boolean indicating if the current ballot is formal; returns false for informal ballots or non-ballots.
	 * @return If ballot is formal.
	 */
	public boolean ballotIsFormal() {
		if (_curBallot != null){
			return _curBallot.isFormal();
		}
		return false;
	}
	
	/**
	 * Returns the sequential number of the current ballot, equivalent to the number of
	 * ballots that have been processed so far.
	 * @return
	 */
	public int currentBallotNum() { return _ballotsProcessed; }
		
	
	// Console interaction methods.
	
	/**
	 * Prints a formatted version of the party list line-by-line with indexes included
	 */
	public void printFormattedParties() {
		System.out.println("Parties: ");
		for (int i = 0; i < (_parties.size()); i++) {
			System.out.println(i + ": " + _parties.get(i));
		}
	}	
	
	/**
	 * Prints out the current ballot.
	 */
	public void printCurrentBallot() {
		System.out.println(_curBallot);
	}
	
	/**
	 * If there are categories, it prints a formatted version of the category list line-by-line
	 * with indexes included.
	 */	
	public void printFormattedCategories() {
		if (!_ctgrsActive) return;
		System.out.println("Categories: ");
		for (int i = 0; i < (_categories.size()); i++) {
			System.out.println(i + ": " + _categories.get(i));
		}
	}
	
	/**
	 * @return Whether or not {@link BallotStream#_debug} is on.
	 */
	public boolean debugging() {
		return _debug;
	}
	
	
	// Categorization methods
		
	/**
	 * Provides a categorization scheme for the ballots if no ballots have been processed yet, and 
	 * creates the setting for what data ballot access functions will return. If any number of ballots 
	 * have already been processed, or any party is missing from the categorization, a 
	 * {@link CategorizationStatus} will be returned indicating why it failed, otherwise 
	 * {@link CategorizationStatus#SUCCEEDED} will be returned.
	 * <br><br>
	 * In an election with parties A, B, C, D, and E, it may be valuable to see if 
	 * parties A, B, and C tend to vote for each other more than they tend to vote for 
	 * parties D and E. This allows us to assign A, B, and C into category 1, and D and E into
	 * category 2, then compare how these two categories interact.
	 * <br><br>
	 * These categories can be compared in three ways:	<br>
	 * 	- Compare primary vote for a category against secondary votes for other parties<br>
	 * 		Party __| A | B | C | D | E<br>
	 * 		Ctgry 1 | X | X | X | X | X<br>
	 * 		_____ 2 | X | X | X | X | X<br>
	 * <br>
	 * 								or<br>
	 * <br>
	 * 					( )< Ctgry 2	Ctgry 1 >( )<br>
	 * 		Party A -----V------------------------V----- Party B<br>
	 * 											  <br>
	 * 	- Compare primary vote for a party against secondary votes for categories<br>
	 * 		Ctgry __| 1 | 2<br>
	 * 		Party A | X | X<br>
	 * 		_____ B | X | X<br>
	 * 		_____ C | X | X<br>
	 * 		_____ D | X | X<br>
	 * 		_____ E | X | X<br>
	 * 								or<br>
	 * <br>
	 * 				       ( )< Party A    Party B >( )<br>
	 * 		Category 1 -----V------------------------V----- Category 2<br>
	 * <br>
	 * 	- Compare primary vote for a category against secondary votes for other categories<br>
	 * 		Ctgry __| 1 | 2<br>
	 * 		Ctgry 1 | X | X<br>
	 * 		_____ 2 | X | X<br>
	 * <br>
	 * 								or<br>
	 * <br>
	 * 					   ( )< Ctgry 2	   Ctgry 1 >( )<br>
	 * 		Category 1 -----V------------------------V----- Category 2<br>
	 * <br>
	 * 	- (There is, of course, the fourth option of comparing primary vote for a party
	 * 		against secondary vote for a party, but this is the default that is used
	 * 		if not categorization is used.)<br>
	 * <br>
	 * If {@link #usesCategoriesPrimary()} is set to true, analysis will use the
	 * category of the primary vote;
	 * if {@link #usesCategoriesSecondary()} is set to true, analysis will use the 
	 * category of the secondary vote.
	 * 
	 * @param categories A list of names of all the categories
	 * @param categoryMap A hash map that maps every possible party into a category.
	 * @param useCtgrsPrimary Whether, in analysis, categories will be used for primary preferences.
	 * @param useCtgrsSecondary Whether, in analysis, categories will be used for secondary preferences.
	 * @return {@link CategorizatioNStatus} indicating if the categorization was accepted; categories 
	 * will be rejected if not every party is provided with a category, or if any number of ballots
	 * have already been processed. In the former case, the parties not included in the map should 
	 * be added to the map and then {@link #categorizeParties(ArrayList, Map, boolean, boolean)} be 
	 * called again; in the later case, categorization will only be accepted if the {@link restart} method is called.
	 */
	public void categorizeParties(ArrayList<String> categories, 
			Map<String, String> categoryMap, boolean useCtgrsPrimary, boolean useCtgrsSecondary) 
					throws ElectionResource.CategorizationException {
		
		if (_ballotsProcessed != 0) throw new ElectionResource.StreamInProgressException(null);
		
		for (String p : _parties) if (!categoryMap.containsKey(p) || !categories.contains(categoryMap.get(p))) {
			System.out.println(p + " " + _parties.indexOf(p) + " " + (categoryMap.containsKey(p) ? categoryMap.get(p) : "null"));
			throw new ElectionResource.MissingPartyException(getSecondaryChoice());
		}
		
		_categorization = categoryMap;
		_categories = categories;
		_ctgrsActive = true;
		_useCtgrsPrim = useCtgrsPrimary;
		_useCtgrsSec = useCtgrsSecondary;
				
		for (String g : _categories) _categorization.put(g, g);
		
		updateWhoIsRunning();
	}
	
	/**
	 * Indicates if the categorize are active.
	 */
	public boolean categoriesAreActive() { return _ctgrsActive; }
	
	/** Indicates if categories are being used for the secondary values. */
	public boolean usesCategoriesSecondary() { return  _ctgrsActive && _useCtgrsSec; }

	/** Indicates if categories are being used for the primary values. */	
	public boolean usesCategoriesPrimary() { return _ctgrsActive && _useCtgrsPrim; }
	
	/**
	 * Deactivates the categorization functionality if no ballots have been processed in this stream, 
	 * or if categories are already inactive; if these conditions are met, the function returns true,
	 * otherwise it returns false.
	 * @return Whether or not categorization has been deactivated; if true, categorization remains 
	 * active and cannot be deactivated until the {@link BallotStream} is restarted.
	 */
	public void stopCategories() throws ElectionResource.CategorizationException {
		if (_ballotsProcessed != 0 && _ctgrsActive == true) {
			throw new ElectionResource.StreamInProgressException("Stream has begun processing ballots.");
		}
		_ctgrsActive = false;
		_useCtgrsSec = false;
		_useCtgrsPrim = false;
		return;
	}
	
	/**
	 * Returns what category a party is a member of, or null if the string provided is not a party.
	 * @param party The party being checked for.
	 * @return String name of the category which the provided party is in.
	 */
	public String categoryOf(String party) { return _ctgrsActive ? _categorization.get(realNameOf(party)) : null; }

	/**
	 * Categorizes the election stream based on the category provided.
	 * @param cat
	 * @param useCatsPrimary
	 * @param useCatsSecondary
	 * @return
	 */
	public void predefinedCategorizations(Categories cat, boolean useCatsPrimary, boolean useCatsSecondary) 
		throws ElectionResource.CategorizationException {
		_elecInfo.premadeCategorizations(cat, this, useCatsPrimary, useCatsSecondary);
	}
}
