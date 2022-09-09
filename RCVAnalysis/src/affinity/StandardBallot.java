package affinity;

public class StandardBallot {
	
	
	// Mutable instance variables
	
	/** 
	 * The minimum number ranked below for a ballot to be counted 
	 * as valid below the line.
	 */
	private int			_minRankedBelow;
	/** 
	 * The minimum number ranked above for a ballot to be counted 
	 * as valid above the line.
	 */
	private int			_minRankedAbove;	
	/**
	 * The current votes above the line on the ballot.
	 */
	private String[]	_aboveTheLine;
	/**
	 * The current votes below the line on the ballot.
	 */
	private String[] 	_belowTheLine;
	/**
	 * The category corresponding to the party who recieved the
	 * above the line vote for that positon; 
	 * if 8 is "Category 1", then party 8 in {@link _aboveTheLine}
	 * is a member of "Category 1".
	 */
	private String[] 	_categoriesATL;
	/**
	 * The category corresponding to the party who recieved the
	 * below the line vote for that positon; 
	 * if 8 is "Category 1", then party 8 in {@link _belowTheLine}
	 * is a member of "Category 1".
	 */
	private String[] 	_categoriesBTL;	
	/**
	 * The status of the ballot, if it is {@link BallotStatus#ABOVE_THE_LINE},
	 * {@link BallotStatus#BELOW_THE_LINE}, {@link BallotStatus#INFORMAL},
	 * or {@link BallotStatus#UNFINALIZED}.
	 */
	private BallotStatus _status;
	/**
	 * Whether or not the ballot is currently using categories.
	 */
	private boolean 	_useCategories;
	/**
	 * The current ranking of which of two parties a ballot prefers,
	 * the upper or lower. Stored as a {@link Ranking}. Null
	 * if {@link #preferBetweenCats(String, String)} or 
	 * {@link #preferBetweenParties(String, String)} has not been called
	 * since the ballot was initialized or cleared, otherwise the value of the last
	 * time one of those methods were called. Used to reduce number of 
	 * reinstantiations needed for local variables.
	 */
	private Ranking 	_curRanking;
	
	
	// Enumerations
	
	/**
	 * Informs the querier which of the parties provided is prefered
	 * by the ballot; "First" or "Second" indicate if it was the first or 
	 * second which was liked the most, while "Prefered" indicates that 
	 * both parties eventually appeared on the ballot, while "Only"
	 * means only the higher ranking of the two parties appeared on the ballot.
	 * @author Michael Peeler
	 * @version August 3rd, 2022
	 *
	 */
	public static enum Ranking {
		/** 
		 * The first party was prefered, and the second did not
		 * appear on the ballot.
		 */
		FIRST_ONLY,		
		/** 
		 * The second party was prefered, and the first did not
		 * appear on the ballot.
		 */
		SECOND_ONLY,
		/** 
		 * The first party was prefered, and the second was also
		 * on the ballot.
		 */
		FIRST_PREFERED,
		/** 
		 * The second party was prefered, and the first was also
		 * on the ballot.
		 */
		SECOND_PREFERED,
		/** 
		 * Neither party was on the ballot.
		 */
		NEITHER,
		/**
		 * This ballot was not formal, and was unable to render judgement.
		 */
		INFORMAL
	}
	
	/**
	 * Enumeration that contains information on the specific status of a ballot, 
	 * if it is ATL, BTL, informal, or has not yet had {@link #validateBallot()}
	 * called on it to determine that.
	 * @author Michael Peeler
	 * @version August 4th
	 *
	 */
	public static enum BallotStatus {
		/**
		 * The ballot is a valid vote above the line, and not
		 * a valid vote above the line.
		 */
		ABOVE_THE_LINE,
		/**
		 * The ballot is a valid vote below the line; it may be
		 * valid above the line, but below takes precedent.
		 */
		BELOW_THE_LINE,
		/**
		 * The ballot is valid neither above nor below the line.
		 */
		INFORMAL,
		/**
		 * The ballot has not yet had {@link #validateBallot()} invoked
		 * to mark it as finalized and determine if it is valid.
		 */
		UNFINALIZED;
		
		/** Returns boolean indicating if ballot is not {@link BallotStatus#INFORMAL} */
		public boolean isFormal() {
			return this != INFORMAL;
		}
		
		/** Returns boolean indicating if ballot is {@link BallotStatus#ABOVE_THE_LINE} */
		public boolean isATL() {
			return this == ABOVE_THE_LINE;
		}
		
		/** Returns boolean indicating if ballot is {@link BallotStatus#BELOW_THE_LINE} */
		public boolean isBTL() {
			return this == BELOW_THE_LINE;
		}
		
		/** Returns boolean indicating if ballot is not {@link BallotStatus#UNFINALIZED} */
		public boolean isFinal() {
			return this != UNFINALIZED;
		}
	}
	
	
	// Static strings for weird cases.
	
	/** 
	 * String to indicate that a vote slot contains a collision, i.e. that
	 * multiple parties or candidates were indicated for that slot.
	 */
	public static final String COLLISION = "** COLLISION ** DO NOT INCLUDE ** END PROCESSING OF BALLOT **";
	/** String to indicate that a ballot is informal. */
	public static final String INFORMAL_BALLOT = "** THIS BALLOT IS INFORMAL **";
	/**
	 * String to indicate that a ballot does not express a second choice.
	 */
	public static final String NO_SECOND_CHOICE = "** THIS BALLOT EXPRESSES NO SECOND CHOICE **";
		
	
	// Constructors
	
	/**
	 * Fully choice constructor; creates a new {@link StandardBallot} with
	 * the parameters provided.
	 * @param atlSize Size of the {@link #_aboveTheLine} ballot
	 * @param btlSize
	 * @param minAbove
	 * @param minBelow
	 */
	public StandardBallot(int atlSize, int btlSize, int minAbove, int minBelow) {
		updateSize(atlSize, btlSize);
		_minRankedAbove = minAbove;
		_minRankedBelow = minBelow;
		_status = BallotStatus.UNFINALIZED;
		_curRanking = null;
		_useCategories = false;
	}
	
	public StandardBallot(int atlSize, int btlSize) {
		this(atlSize, btlSize, 1, 6);
	}
	
	public StandardBallot(String[] aboveTheLine, String[] belowTheLine, int minAbove, int minBelow) {
		this(aboveTheLine.length, belowTheLine.length, minAbove, minBelow);
		this._aboveTheLine = aboveTheLine;
		this._belowTheLine = belowTheLine;
	}

	public StandardBallot(String[] aboveTheLine, String[] belowTheLine) {
		this(aboveTheLine, belowTheLine, 1, 6);
	}
	
	public StandardBallot() {
		this(10, 20, 1, 6);
	}
	
	protected void updateSize(int atlSize, int btlSize) {
				
		this._aboveTheLine = new String[atlSize];
		this._belowTheLine = new String[btlSize];
		
		if (_useCategories) {
			_categoriesATL = new String[atlSize];
			_categoriesBTL = new String[btlSize];
		}
		
		_status = BallotStatus.UNFINALIZED;
		_curRanking = null;		
	}
	
	/**
	 * Clears the ballot of instance variables, aside from
	 * variables related to the minimum number of ATL and BTL 
	 * votes.
	 * Clears: {@link #_aboveTheLine}, {@link #_belowTheLine},
	 * {@link #_categoriesATL}, {@link #_categoriesBTL},
	 * {@link #_status}, {@link StandardBallot#_curRanking},
	 * {@link StandardBallot#_rankedAboveTheLine} and 
	 * {@link #_rankedBelowTheLine}.
	 */
	public void clearBallot() {
		for (int i = 0; i < _aboveTheLine.length; i++) _aboveTheLine[i] = null;
		for (int i = 0; i < _belowTheLine.length; i++) _belowTheLine[i] = null;
		if (_useCategories) {
			for (int i = 0; i < _categoriesATL.length; i++) _categoriesATL[i] = null;
			for (int i = 0; i < _categoriesBTL.length; i++) _categoriesBTL[i] = null;
		}		
		_status = BallotStatus.UNFINALIZED;
		_curRanking = null;
	}
	
	public void setCatsATL(int index, String category, boolean stopCollide) {
		if (_status.isFinal()) return;
		
		if (stopCollide) _categoriesATL[index] = _categoriesATL[index] == null ? category : COLLISION;
		else _categoriesATL[index] = category;
	}
	
	public void setCatsBTL(int index, String category, boolean stopCollide) {
		if (_status.isFinal()) return;

		if (stopCollide) _categoriesBTL[index] = _categoriesBTL[index] == null ? category : COLLISION;
		else _categoriesBTL[index] = category;
	}
	
	public void setATL(int index, String preferred, boolean stopCollide) {
		if (_status.isFinal()) return;
		
		if (stopCollide) _aboveTheLine[index] = _aboveTheLine[index] == null ? preferred : COLLISION;
		else _aboveTheLine[index] = preferred;
	}
	
	public void setBTL(int index, String preferred, boolean stopCollide) {
		if (_status.isFinal()) return;

		if (stopCollide) _belowTheLine[index] = _belowTheLine[index] == null ? preferred : COLLISION;
		else _belowTheLine[index] = preferred;
		
	}
	
	public void turnCategoriesOn() {
		_useCategories = true;
		_categoriesATL = new String[_aboveTheLine.length];
		_categoriesBTL = new String[_belowTheLine.length];
	}
	
	public void stopUsingCategories() {
		_useCategories = false;
	}
	
	public boolean usingCategories() {
		return _useCategories;
	}
	
	public String[] getAboveLine()	{ return _aboveTheLine; }
	
	public String[] getBelowLine() 	{ return _belowTheLine; }
	
	public boolean isFormal()		{ return _status.isFormal(); }
	
	public boolean isValidBTL() 	{ return _status.isBTL(); }
	
	/**
	 * Validates the StandardBallot to determine if is formal and if it is below the line. 
	 * Should be called when the ballot is finished being created.
	 */
	public StandardBallot validateBallot() {	
		// Checks formality by ensuring that a minimum number of votes have either 
		// been cast above the line or below the line.
		if (numberRanked(_belowTheLine) < _minRankedBelow) {
			if (numberRanked(_aboveTheLine) < _minRankedAbove) {
				_status = BallotStatus.INFORMAL;
				return this;
			} else {
				_status = BallotStatus.ABOVE_THE_LINE;
			}
		} else {
			_status = BallotStatus.BELOW_THE_LINE;
		}
		
		return this;
	}
	
	/**
	 * Sets the ballot to formal, regardless of what the specific vote is.
	 * @return
	 */
	public StandardBallot setFormal() {
		
		_status = numberRanked(_belowTheLine) > numberRanked(_aboveTheLine) ? 
				BallotStatus.BELOW_THE_LINE : BallotStatus.ABOVE_THE_LINE;
		return this;
		
	}

	/**
	 * Returns the party that the ballot supports with its top-ranked vote. If the
	 * ballot contains formal above and below the line votes, the below the line will
	 * be preferred and returned. If the ballot is informal, StandardBallot.INFORMAL_BALLOT
	 * will be returned.
	 * @return
	 */
	public String primaryPreference() { 
		if (!_status.isFinal()) return null;
		
		if (!isFormal()) 		return INFORMAL_BALLOT;
		
		if (isValidBTL()) 		return _belowTheLine[0];
		else 					return _aboveTheLine[0];
		
	}
	
	public String primaryCategoryPreference() {
		if (!_status.isFinal() || ! _useCategories) return null;

		if (!isFormal()) 		return INFORMAL_BALLOT;
		
		if (isValidBTL()) 		return _useCategories ? _categoriesBTL[0] : null;
		else 					return _useCategories ? _categoriesATL[0] : null;
		
	}
	
	/**
	 * Returns the second unique preference expressed on a ballot. Returns 
	 * StandardBallot.NO_SECOND_CHOICE if no second choice is expressed,
	 * or if the ballot is informal.
	 * @return
	 */
	public String secondPreference() {
		if (!_status.isFinal()) return null;

		String rtrn = getFirstNParties(2)[1];		
		return rtrn == null ? NO_SECOND_CHOICE : rtrn;
		
	}
	
	public String secondCategoryPreference() {
		if (!_status.isFinal() || !_useCategories) return null;
		
		String fstChoice = primaryPreference();
		String[] ballot, category;		
		
		if (isValidBTL()) {
			ballot = _belowTheLine;
			category = _categoriesBTL;
		} else {
			ballot = _aboveTheLine;
			category = _categoriesATL;
		}
		for (int i = 1; ballot[i] != null && !ballot[i].equals(COLLISION) && i < _belowTheLine.length; i++) {
			if (!fstChoice.equals(ballot[i])) return category[i];
		}

		return NO_SECOND_CHOICE ;
		
	}

	
	// Methods related to getting the first n parties or categories.
	
	/**
	 * Provided an integer n, returns a String array of n elements, containing the first
	 * n unique choices made by the ballot; if a ballot did not make n unique
	 * choices, any additional positions will be filled with null.
	 * @param n
	 * @return
	 */
	public String[] getFirstNParties(int n) {
		
		if (!_status.isFinal()) return null;

		// Sets ballot to the valid ballot being used
		if (isFormal()) {
			if (isValidBTL()) {
				return firstNParties(n, _belowTheLine);
			} else {
				return firstNParties(n, _aboveTheLine);
			}
		} else {
			return new String[n];
		}
		
	}
	
	/**
	 * Provided an integer n, returns a String array of n elements, containing the category 
	 * of the first choice made by the ballot, and then the first time a member of a category
	 * which is not the first choice appears for the first n - 1 categories; if a ballot did 
	 * not make n - 1 unique category choices after the first, any additional positions will 
	 * be filled with null.
	 * 
	 * See {@link #firstNCategories}.
	 * @param n How many categoreis the return value will, at most, contain.
	 * @return The string array of categories
	 */
	public String[] getFirstNCats(int n) {
		
		if (!_status.isFinal() || !_useCategories) return null;
		
		// Sets ballot to the valid ballot being used
		if (_useCategories && isFormal()) {
			if (isValidBTL()) {
				return firstNCategories(n, _belowTheLine, _categoriesBTL);
			} else {
				return firstNCategories(n, _aboveTheLine, _categoriesATL);
			}
		} else {
			return new String[n];
		}
	}
	
	/**
	 * Helper method for {@link #getFirstNParties(int)}.
	 * Returns a list with the first n unique parties to appear 
	 * on the provided ballot; if there are not n unique parties,
	 * all remaining spots will be null. No entry will be repeated.
	 * @param n Maximum number of parties
	 * @param ballot The ballot this data is requested from.
	 * @return A String[] of the first n unique parties.
	 */
	private String[] firstNParties(int n, String[] ballot) {
				
		if (!_status.isFinal()) return null;
		
		int size = 1;
		boolean inListAlready;
		String[] rtrn = new String[n];
		rtrn[0] = ballot[0];
		
		for (int i = 1; size < n && i < ballot.length && 
				(ballot[i] != null) && !ballot[i].equals(COLLISION); i++) {
			
			// Checks if the category is already in the return list
			inListAlready = false;
			for (int j = 0;	j < n && (rtrn[j] != null); j++) {
				if (ballot[i].equals(rtrn[j])) {
					inListAlready = true;
					break;
				}
			}
			
			// Adds the party to the return list and increments the size if it is
			// not already in the list
			if (inListAlready) continue;
			else {
				rtrn[size] = ballot[i];
				size++;
			}
		}
		
		return rtrn;
	}
	
	/**
	 * Helper methd for {@link #getFirstNCats(int)}.
	 * Returns an n-element list that begins with the category of the primary party
	 * and subsequently includes the order in which a category's first party that 
	 * is not the primary party appears. There will be no repeated values, except
	 * for the category of the primary party, which may appear again if a different 
	 * party that is in the category is voted for on the ballot.<br><br>
	 * 
	 * For the following ballot and an n of 5:<br>
	 * Parties: A, A, B, C, B, D, E, F <br>
	 * Category: 1, 1, 2, 1, 2, 4, 1, 2 <br>
	 * The output will be:<br>
	 * 1, 2, 1, 4, null<br>
	 * || || || || |=> Because there is no vote for category 3<br>
	 * || || || |=> From the 6th preference, for D <br>
	 * || || |=> From the 4th preference, for C<br>
	 * || |=> From the 3rd preference, for B<br>
	 * |=> From the 1st preference, for A<br>
	 * @param n How many categories the output should maximally contain.
	 * @param ballot The ballot being analyzed.
	 * @param cats The categories which correspond to the preferences 
	 * on the ballot. The category of ballot[i] should be category[i].
	 * @return The list of the first N categories.
	 */
	private String[] firstNCategories(int n, String[] ballot, String[] cats) {
				
		if (!_status.isFinal() || !_useCategories) return null;
		
		boolean inListAlready;
		String[] rtrn = new String[n];
		rtrn[0] = cats[0];
		int size = 1;
		
		for (int i = 1; size < n && i < ballot.length && 
				(ballot[i] != null) && !ballot[i].equals(COLLISION); i++) {
			
			// Checks if the party is already in the return list
			inListAlready = false;
			// Skips a vote if it is from the primary vote party
			if (ballot[i].equals(ballot[0])) continue; 
			
			for (int j = 1;	j < n && (rtrn[j] != null); j++) {
				if (cats[i].equals(rtrn[j])) {
					inListAlready = true;
					break;
				}
			}
			
			// Adds the category to the return list and increments the size if it is
			// not already in the list
			if (inListAlready) continue;
			else {
				rtrn[size] = cats[i];
				size++;
			}
		}
		
		return rtrn;
		
	}
	
	
	// Methods related to preference between two parties.
	
	/**
	 * Checks the current ballot and determines if it supports the first or the second
	 * party. Returns a {@link Ranking} indicating which, if either, party it supports.
	 * 
	 * @param lBound the name of the lower party bound entered by the user
	 * @param uBound The name of the upper bound entered by the user
	 * @return A string that says which party, if any, is supported
	 */
	public Ranking preferBetweenParties(String first, String second) {
		if (!_status.isFinal()) return null;

		if (!isFormal())	return Ranking.INFORMAL;
		
		if (isValidBTL())	return preferredBTL(first, second);
		else 				return preferredATL(first, second);

	}
	
	/**
	 * Checks the current ballot and determines if it supports the first or the second
	 * category. Returns a {@link Ranking} indicating which, if either, category it
	 * supports. Support is determined by the first time a member of a category
	 * appears and it is not the same as the first vote of the ballot. 
	 * 
	 * @param lBound the name of the lower party bound entered by the user
	 * @param uBound The name of the upper bound entered by the user
	 * @return A string that says which party, if any, is supported
	 */
	public Ranking preferBetweenCats(String first, String second) {
		if (!_status.isFinal()) return null;

		if (!isFormal())	return Ranking.INFORMAL;
		
		if (isValidBTL()) 	return preferredCatBTL(first, second);
		else				return preferredCatATL(first, second);
	}
	
	/**
	 * Returns which of the two parties, first or first, is preferred in the above
	 * the line vote.
	 * @param first
	 * @param second
	 * @return {@link Ranking} indicating which party was preferred.
	 */
	private Ranking preferredATL(String first, String second) {
		
		_curRanking = Ranking.NEITHER;
		
		for (int u = 0; u < _aboveTheLine.length; u++) {
			
			// Valid votes end when a number is skipped or a collision (two rankings of the same 
			// number) is found.
			if (_aboveTheLine[u] == null || _aboveTheLine[u].equals(StandardBallot.COLLISION))
				return _curRanking;
			if (_aboveTheLine[u].equals(first)) {
				if (_curRanking.equals(Ranking.NEITHER)) _curRanking = Ranking.FIRST_ONLY;
				else if (_curRanking.equals(Ranking.SECOND_ONLY)) return Ranking.SECOND_PREFERED; 
			}
			if (_aboveTheLine[u].equals(second)) {
				if (_curRanking.equals(Ranking.NEITHER))_curRanking = Ranking.SECOND_ONLY;
				else if (_curRanking.equals(Ranking.FIRST_ONLY)) return Ranking.FIRST_PREFERED;
			}

		}
		
		return _curRanking;
		
	}
	
	/**
	 * Returns which of the two categories, first or first, is preferred in the above
	 * the line vote, ecluding when the vote is for the primary choice party.
	 * @param first First category
	 * @param second Second category
	 * @return {@link Ranking} indicating which category was preferred.
	 */
	private Ranking preferredCatATL(String first, String second) {
		_curRanking = Ranking.NEITHER;
		
		for (int u = 0; u < _categoriesATL.length; u++) {
			
			if (_categoriesATL[u] == null || _categoriesATL[u].equals(StandardBallot.COLLISION))
				return _curRanking;
			if (_categoriesATL[u].equals(first) && _aboveTheLine[u] != _aboveTheLine[0]) {
				if (_curRanking.equals(Ranking.NEITHER)) _curRanking = Ranking.FIRST_ONLY;
				else if (_curRanking.equals(Ranking.SECOND_ONLY)) return Ranking.SECOND_PREFERED;
			}
			if (_categoriesATL[u].equals(second) && _aboveTheLine[u] != _aboveTheLine[0]) {
				if (_curRanking.equals(Ranking.NEITHER))_curRanking = Ranking.SECOND_ONLY;
				else if (_curRanking.equals(Ranking.FIRST_ONLY)) return Ranking.FIRST_PREFERED;
			}
		}
		
		return _curRanking;
		
	}
	
	/**
	 * Returns which of the two parties, first or second, is preferred in the below
	 * the line vote.
	 * @param ballotATL
	 * @param firstParty
	 * @param secondParty
	 * @return Preference.Prefers indicating if the FIRST, SECOND, or NEITHER party were prefered.
	 */
	private Ranking preferredBTL(String first, String second) {
		
		_curRanking = Ranking.NEITHER;
		
		for (int u = 0; u < _belowTheLine.length; u++) {
			
			// Valid votes end when a number is skipped or a collision (two rankings of the same 
			// number) is found.
			if (_belowTheLine[u] == null || _belowTheLine[u].equals(StandardBallot.COLLISION))
				return _curRanking;
			if (_belowTheLine[u].equals(first)) {
				if (_curRanking.equals(Ranking.NEITHER)) 
					_curRanking = Ranking.FIRST_ONLY;
				else if (_curRanking.equals(Ranking.SECOND_ONLY)) 
					return Ranking.SECOND_PREFERED; 
			}
			if (_belowTheLine[u].equals(second)) {
				if (_curRanking.equals(Ranking.NEITHER))
					_curRanking = Ranking.SECOND_ONLY;
				else if (_curRanking.equals(Ranking.FIRST_ONLY)) 
					return Ranking.FIRST_PREFERED;
			}

		}
		
		return _curRanking;

	}
	
	/**
	 * Returns which of the two categories, first or first, is preferred in the below
	 * the line vote, ecluding when the vote is for the primary choice party.
	 * @param first First category
	 * @param second Second category
	 * @return {@link Ranking} indicating which category was preferred.
	 */
	private Ranking preferredCatBTL(String first, String second) {
		_curRanking = Ranking.NEITHER;
		
		for (int u = 0; u < _categoriesBTL.length; u++) {
			
			if (_categoriesBTL[u] == null || _categoriesBTL[u].equals(StandardBallot.COLLISION))
				return _curRanking;
			if (_categoriesBTL[u].equals(first) && _belowTheLine[u] != _belowTheLine[0]) {
				if (_curRanking.equals(Ranking.NEITHER)) 
					_curRanking = Ranking.FIRST_ONLY;
				else if (_curRanking.equals(Ranking.SECOND_ONLY)) 
					return Ranking.SECOND_PREFERED;
			}
			if (_categoriesBTL[u].equals(second) && _belowTheLine[u] != _belowTheLine[0]) {
				if (_curRanking.equals(Ranking.NEITHER))
					_curRanking = Ranking.SECOND_ONLY;
				else if (_curRanking.equals(Ranking.FIRST_ONLY)) 
					return Ranking.FIRST_PREFERED;
			}
		}
		
		return _curRanking;
		
	}
	
	/**
	 * Turns the ballot into a string version.
	 */
	public String toString() {
		
		String atl = "[";
		String btl = "[";
		for (String s : _aboveTheLine) atl += s + ", ";
		for (String s : _belowTheLine) btl += s + ", ";
		
		return "Above The Line: " + atl + "]\nBelow The Line: " + btl + "]";
	}
	
	/**
	 * Counts how many valid rankings are on the ballot.
	 * @param ballot A String array containing either the above or below the line ballot.
	 * @return The number of valid votes on the ballot.
	 */
	public int numberRanked(String[] ballot) {
		int highestNum = 0;
		
		for (; highestNum < ballot.length;) {
			// Valid votes end when a number is skipped or a collision (two rankings of the same 
			// number) is found; otherwise, all votes are valid.
			if (ballot[highestNum] == null || ballot[highestNum] == StandardBallot.COLLISION)
				break;
			highestNum++;
		}
		
		return highestNum;
	}
	
	/**
	 * Returns a deep copy of the ballot object.
	 */
	public StandardBallot clone() {
		StandardBallot copy = new StandardBallot();
		
		copy._minRankedBelow = this._minRankedBelow;
		copy._minRankedAbove = this._minRankedAbove;
		
		copy._aboveTheLine = this._aboveTheLine.clone();
		copy._belowTheLine = this._belowTheLine.clone();
		copy._categoriesBTL = this._categoriesBTL.clone();
		copy._categoriesATL = this._categoriesATL.clone();
		
		copy._status = this._status;
		copy._useCategories = this._useCategories;
		
		copy._curRanking = this._curRanking;
		
		return copy;
	}

}
