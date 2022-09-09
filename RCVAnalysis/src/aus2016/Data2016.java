package aus2016;

import java.util.ArrayList;

import affinity.DataInterface;
import affinity.StandardBallot;

import java.util.Scanner;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.io.FileInputStream;
import java.util.HashSet;

/**
 * An implementation of the <code>DataInterface</code> interface for the 2016 Australian Election.
 * When provided with the data files from the election, it will provide a stream of ballots from the
 * election.
 * 
 * @author Michael Peeler
 * @version July 30th, 2022
 */
public class Data2016 implements DataInterface {
	
	// Data structures pertaining to candidates and parties.
	
	/** A list of parties in the order that they appear on the ballots. */
	private ArrayList<String> 	_partyOrder;
	/**
	 *  A list of all parties that run in the election; includes some parties that run ungrouped,
	 *  or grouped with other parties. 
	 */
	private ArrayList<String> 	_allParties;
	/**
	 * A list of all candidates that run in the election, in the order that they appear on the
	 * ballot.
	 */
	private ArrayList<String> 	_posToCand;
	/** A map from a candidate name to the name of they party they belong to. */
	private Map<String, String> _candToParty;

	
	// Instance variables pertaining to the current files.
	
	/** 
	 * The ID of the state which the current ballots are from; it is the abbreviation of the
	 * state's name, eg. New South Wales becomes NSW and Queensland become QLD.
	 */
	private final String 	_stateID;	
	/** Scanner that reads ballot information from the dataFile. */
	public Scanner 			_scan;
	/** File that ballots are being read from. */
	private final String 	_dataFile;
	
	
	// Ballot creation fields, to store the current ballot and minimize how often
	// space has to be allocated for a new ballot.
	
	/** The current ballot of the DataInterface. */
	private StandardBallot 	_curBallot;
		
	
	/** 
	 * Instantiation methods, primarily dealing with reading candidate information from the
	 * identification files.
	 * 
	 * @param namesFile
	 * @param dataFile NEED TO COMMENT >> IF UNCOMMENTED, READ SPECIFICATIONS INTERNAL TO {@link #makeCandidateToParty}.
	 */
	
	public Data2016 (String namesFile, String dataFile) {
		
		// Stores the provided file names.
		_dataFile 		= dataFile;
		
		// Gets the StateID from the provided file name.
		String stateID 	= dataFile.split("-")[dataFile.split("-").length - 1];
		_stateID 		= stateID.substring(0, stateID.indexOf(".") != -1 ? stateID.indexOf(".") : stateID.length());
		
		// Creates data structures to hold party and candidate information.
		_posToCand 		= new ArrayList<String>();
		_candToParty 	= new HashMap<String, String>();
		_partyOrder 	= new ArrayList<String>();
		_allParties 	= new ArrayList<String>();
		
		/* Creates a scanner for the nameFile and collects all the names inside of it running for
		 * senate election in the specific state.. */
		Scanner senScan;
		try {
			senScan 		= new Scanner(new FileInputStream(namesFile));		
			makeCandidateToParty(senScan);
			_allParties 	= new ArrayList<String>(new HashSet<String>(_candToParty.values()));
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		// Creates a scanner for the data file, skipping the two empty lines at the top.
		try {
			_scan = new Scanner(new FileInputStream(dataFile));
			_scan.nextLine();
			_scan.nextLine();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

				
		// Creates the DataInterface's StandardBallot.
		_curBallot 		= new StandardBallot(numParties(), numCandidates());
	}
	
	/**
	 * Provided with a scanner of the candidate id file provided as the first argument when
	 * instantiating {@link #Data2016(String, String)}, it will create the {@link _positionToCandidate} 
	 * list, the {@link #_candidateToParty} map, and the {@link #_partyOrder} list.
	 * @param senScan Scanner object to read the Senate candidates from the candidate ID file.
	 */
	private void makeCandidateToParty(Scanner senScan) {
		String firstLine = senScan.nextLine();
		String[] tokens = firstLine.split(",");
		
		int party = -1, lstname = -1, fstname = -1,
			state = -1, group = -1;
		
		// Gets positional indexes for each of the needed data fields.
		for (int i = 0; i < tokens.length; i++) {
			switch (clean(tokens[i])) {
			case "state_ab":
				state = i;	 break;
			case "party_ballot_nm":
				party = i; 	 break;
			case "surname":
				lstname = i; break;
			case "ballot_given_nm":
				fstname = i; break;
			case "ticket":
				group = i;	 break;
			}
		}
		
		// Moves the scanner to the Senate section.
		String[] candInfo = skipToStateSen(senScan);
		String curGroup = "";
		
		// This code assumes that the candidates will be clumped together by group,
		// with all of group A appearing before all of group B, and so on.
		
		// It also assumes that candidates will appear in the same order that they appear
		// in the ballot data files, and that the groups will appear in the same order as
		// they appear in the ballot data files.
		while (candInfo != null && clean(candInfo[state]).equals(_stateID)) {
			// Cleans the candidate's name and adds them to the candidate list.
			// Cleaning occurs to remove quotation marks from the beginning and end of the CSV fields.
			_posToCand.add(cleanName(candInfo[fstname], candInfo[lstname]));
			// Puts the new name into the candidateToParty map with a cleaned version of the party name.
			_candToParty.put(cleanName(candInfo[fstname], candInfo[lstname]), cleanParty(candInfo[party], candInfo[group]));
			
			// Adds the party into the _partyOrder list if it is not already.
			if (!curGroup.equals(candInfo[group]) && !clean(candInfo[group]).equals("UG")) {
				curGroup = candInfo[group];
				_partyOrder.add(cleanParty(candInfo[party], candInfo[group]));
			}
			
			// Moves to the next candidate.
			if (senScan.hasNextLine()) candInfo = senScan.nextLine().split(",");
			else candInfo = null;
		}
	}
	
	/**
	 * Cleans a party name from the file version, removing quotation marks and
	 * other "dirty" portions. If there is no provided name for the party, the
	 * group number, in combination with the state, is used.
	 * @param dirtyParty Party being cleaned, as it appears in the file.
	 * @param dirtyGroup Group of the party being cleaned, as it appears in the file.
	 * @return Cleaned name of the party.
	 */
	private String cleanParty(String dirtyParty, String dirtyGroup) {
		String cleaned = clean(dirtyParty);
		if (cleaned == null || cleaned.equals("")) {
			
			// Specifically here because of that bastard Ron POULSEN
			if (clean(dirtyGroup).equals("UG")) return "Independent";
			// Non-named parties are assigned the name of _groupLetter_ - _stateID;
			// eg: a non-named member of group A in NSW becomes "A - NSW".
			else return clean(dirtyGroup).replace(",", "") + " - " + _stateID;
			
		}
		// Removes any commas to avoid problems.
		return cleaned.replace(",", "");
	}
	
	/**
	 * Removes the quotation marks in the dirty string.
	 * @param dirty String with quotations that will be removed.
	 * @return dirty, sans quotation marks.
	 */
	private String clean(String dirty) {
		return dirty.replace("\"", "");
	}
	
	/**
	 * Cleans a first and last name and assembles them into a candidate's full name.
	 * @param first Dirty first name.
	 * @param last Dirty last name.
	 * @return Cleaned full name.
	 */
	private String cleanName(String first, String last) {
		return clean(first) + " " + clean(last);
	}

	/**
	 * Advances the candidate scanner until it reaches the senate candidates.
	 * @return An array of the first candidate information line, split on commas.
	 */
	private String[] skipToStateSen(Scanner senScan) {
		
		String curLine = senScan.nextLine();
		while (true) {
			
			String[] st = curLine.split(",");
			
			// Stops advancing when it reaches the Senate election for this state.
			// Returns the first line of the senate candidate info for this state,
			// or null if no senate entries exist.
			if (st.length > 2 && st[1].equals("\"S\"") && st[2].equals('"' + _stateID + '"')) 
				return curLine.split(",");
			if (!senScan.hasNext()) return null;
			
			curLine = senScan.nextLine();
		}
		
	}
	
	
	// Methods relating to party and candidate lists.
	
	/**
	 * Returns a list of all parties available in this DataInterface.
	 * @return All parties contesting in these ballots.
	 */
	@Override
	public ArrayList<String> getPartiesIncludingSecondary() {
		return _allParties;
	}
	
	/**
	 * Returns a list of the groups competing, named after the first member of the group to appear
	 * in the candidate list.
	 */
	@Override
	public ArrayList<String> getParties() {
		return _partyOrder;
	}

	/**
	 * Returns a list of the grouped parties as they originally appear in the file.
	 */
	@Override
	public ArrayList<String> getPartiesUnaltered() {
		return _partyOrder;
	}

	/**
	 * Returns a list of all candidates competing in these ballots.
	 */
	@Override
	public ArrayList<String> getCandidates() {
		return _posToCand;
	}

	/**
	 * Returns a list of all candidates competing in these ballots, as they appear
	 * in the candidate file.
	 */
	@Override
	public ArrayList<String> getCandidatesUnaltered() {
		return _posToCand;
	}
	
	/**
	 * Returns the number of parties contesting the election.
	 * @return
	 */
	private int numParties() 		{ return _partyOrder.size(); }
	
	/**
	 * Returns the number of candidates contesting the election.
	 * @return
	 */
	private int numCandidates() 		{ return _posToCand.size(); }	
	
	/**
	 * Returns the party of a candidate provided.
	 */
	@Override
	public String partyOf(String c)	{ return _candToParty.get(c); }

	
	// Methods related to the DataInformer's file.

	/**
	 * Returns the file that ballots are currently being drawn from.
	 */
	@Override
	public String getCurFileName()	{ return _dataFile; }

	
	// Methods related to getting a new ballot.
	
	/**
	 * Boolean indicating if there are more ballots in the DataStream.
	 */
	@Override
	public boolean hasMoreBallots() 	{ return _scan.hasNextLine(); }
	
	/**
	 * String provided has the vote section separated out, is then split on the commas,
	 * and finally then has blanks replaced by nulls and the * and / replaced by 1s.
	 * @param str
	 * @return
	 */
	protected String[] standardizeParse(String str) {
		
		String[] temp = str.split("\"")[1].split(",");
		String[] rtrn = new String[numParties() + numCandidates()];
		
		for (int i = 0; i < temp.length; i++) {
			if (temp[i].equals("*") || temp[i].equals("/") ) rtrn[i] = "1";
			else if (temp[i].equals("" ) || temp[i].equals("-1")) rtrn[i] = null;
			else rtrn[i] = temp[i];
		}
		
		return rtrn;
	
	}
	
	/**
	 * Using the provided temporary ballot, the choices are moved into a StandardBallot.
	 * The ordering goes from a set ballot ordering to a preferential ordering; that is,
	 * if the ballot was originally set up such that a vote would appear:
	 * Labor |	Liberal |	Center	|	Animal Justice | Christian
	 *   4,		  1,			  2,		3,				__
	 * In the StandardBallot it would become:
	 * ["Liberal", "Center", "Animal Justice", "Labor", null]
	 * @param tempBallot String array of ordered party selection.
	 * @param ballot StandardBallot that votes will be placed into.
	 * @return New StandardBallot
	 */
	protected StandardBallot putVoteIntoBallot(String[] tempBallot, StandardBallot ballot) {
		
		//Ensure that the resources used in the information transfer are empty.
		int parsed;
		ballot.clearBallot();
		
		// Moves in ATL votes.
		for (int i = 0; i < numParties(); i++) {
			if (tempBallot[i] == null) continue;
			
			parsed = Integer.parseInt(tempBallot[i]) - 1;
			if (parsed < numParties()) {
				ballot.setATL(parsed, _partyOrder.get(i), true);
			}
		}
		
		// Moves in BTL votes.
		for (int i = numParties(); i < tempBallot.length; i++) {
			if (tempBallot[i] == null) continue;
			
			parsed = Integer.parseInt(tempBallot[i]) - 1;
			if (parsed < numCandidates()) {
				ballot.setBTL(parsed, _posToCand.get(i - numParties()), true);
			}
		}
		
		return ballot;
	}

	/**
	 * Provides the next ballot in the sequence.
	 * A call of <code>nextBallot</code> will destroy the previously created
	 * ballot and overwrite it with the newly created ballot.
	 */
	@Override
	public StandardBallot nextBallot() 	{
		
		String[] tempBallotArr = standardizeParse(_scan.nextLine());
	
		_curBallot = putVoteIntoBallot(tempBallotArr, _curBallot);
		return _curBallot;
		
	}

}
