package nsw2015;

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
 * An implementation of the <code>DataInterface</code> interface for the 2019 New South Wales Election.
 * When provided with the data files from the election, it will provide a stream of ballots from the
 * election.
 * 
 * @author Michael Peeler
 * @version July 8th, 2022
 */
public class DataNSW2015 implements DataInterface {
	
	// Static variables
	
	/** The ID for the state; in this case, always NSW's state abbreviation. */
	private static final String STATE_ID = "NSW";

	
	// Instance variables related to parties and candidates.
	
	/** A list of parties that contest above the line, in the order they appear on the ballot. */
	private ArrayList<String> _partyOrder;
	/** 
	 * A list of all parties that contest the election, including those that only contest 
	 * below the line ungrouped.
	 */
	private ArrayList<String> _allParties;
	/** A list of all candidates contesting the election. */
	private ArrayList<String> _candidateList;
	/** A map from candidates to the party they are a member of. */
	private Map<String, String> _candidateToParty;
	/** 
	 * A map from a group to its primary party, as defined by it being the first
	 * party to be in that group in the file.
	 */
	private Map<String, String> _groupToParty;
	
	
	// Instance variables relating to indices inside the CSV file.
	
	/** Index of the group identifier in the CSV file. */
	private int _groupIndex;
	/** Index of the candidate name in the CSV file. */
	private int _nameIndex;
	/** 
	 * Index of the voter ID number, used to identify multiple lines which all
	 * belong to the same ballot.
	 */
	private int _voterIDIndex;
	/** 
	 * Index of the entry in the CSV which specifies if the ballot is
	 * single above the line, ranked above the line, or ranked below the line.
	 */
	private int _typeIndex;
	/**
	 * Index of the ranking given to the specified party or candidate.
	 */
	private int _rankIndex;
	/**
	 * Index of the entry in the CSV which specifies if the ballot is formal or not.
	 */
	private int _formalityIndex;
	
	
	// Instance variables related to the files the <code>DataInterface</code> is reading from.
	
	/** A scanner for reading from the file that contains the ballots. */
	public Scanner _scan;
	/** The file which contains the ballots being read by this DataInterface. */
	private final String _dataFile;
	
	
	// Instance variables related to StandardBallots and preserving information about future ballots.
	
	/** The last ballot provided. */
	private StandardBallot _curBallot;
	/** The next ballot that will be provided; generated if <code>hasMoreBallots()</code> is called. */
	private StandardBallot _nextBallot;
	/**
	 * The last output from the scanner; preserved because the end of a ballot can only be detected
	 * in a file by seeing that a new ballot ID number has started.
	 */
	private String[] _nextLine;	

			
	
	// Instantiation methods.
	
	public DataNSW2015 (String namesFile, String dataFile) {
		
		_dataFile = dataFile;
		
		_candidateList = 	new ArrayList<String>();
		_candidateToParty = new HashMap<String, String>();
		_groupToParty = 	new HashMap<String, String>();
		_partyOrder =		new ArrayList<String>();
		_allParties = 		new ArrayList<String>();
		
		Scanner candScan;
		try {
			candScan = new Scanner(new FileInputStream(namesFile));		
			String firstLine = candScan.nextLine();
			String[] tokens = firstLine.split(",");
			
			int party = -1;
			int name = -1;
			int group = -1;
						
			for (int i = 0; i < tokens.length; i++) {
				if (tokens[i].equals("Party")) party = i;
				if (tokens[i].equals("Group/Candidates in Ballot Order")) name = i;
				if (tokens[i].equals("Group")) group = i;
			}
			
			String[] candInfo = candScan.nextLine().split(",", -1);
			String curGroup = "";
			
			while (candInfo != null) {
				_candidateList.add(candInfo[name]);
				_candidateToParty.put(candInfo[name], party(candInfo[party], candInfo[group]));
				
				if (!curGroup.equals(candInfo[group]) && !candInfo[group].equals("UG")) {
					curGroup = candInfo[group];
					_partyOrder.add(party(candInfo[party], candInfo[group]));
					_groupToParty.put(candInfo[group], _partyOrder.get(_partyOrder.size() - 1));
				}
				
				if (candScan.hasNextLine()) candInfo = candScan.nextLine().split(",", -1);
				else candInfo = null;
			}
			
			_allParties = new ArrayList<String>(new HashSet<String>(_candidateToParty.values()));
			
		} catch (FileNotFoundException e) {
			System.out.println("File not found: " + namesFile);
		}
		
		try {
			_scan = new Scanner(new FileInputStream(dataFile));
			String[] firstLineTokens = _scan.nextLine().split("\t");
			
			for (int i = 0; i < firstLineTokens.length; i++) {
				if (firstLineTokens[i].equals("GroupCode")) _groupIndex = i;
				if (firstLineTokens[i].equals("CandidateName")) _nameIndex = i;
				if (firstLineTokens[i].equals("Formality")) _formalityIndex = i;
				if (firstLineTokens[i].equals("Type")) _typeIndex = i;
				if (firstLineTokens[i].equals("VCBallotPaperID")) _voterIDIndex = i;
				if (firstLineTokens[i].equals("PreferenceNumber")) _rankIndex = i;
			}
			
			if (_scan.hasNextLine()) {
				_nextLine = _scan.nextLine().split("\t");
			}

		} catch (FileNotFoundException e) {
			System.out.println("File not found: " + dataFile);
		}
				
		_curBallot = new StandardBallot(numParties(), numCandidates());
	}
	
	/** Converts from the party name in the data file to the official 
	 * party name; namely, provides unnamed groups with a name, of the form
	 * _group letter_ - _state_ID_; group A would become "A - NSW". Also removes
	 * commas from party names.
	 * @param party Party whose official name is being searched for.
	 * @param group Group of the party.
	 * @return The official party name.
	 */
	private String party(String party, String group) {
		if (party == null || party.equals("")) {
			
			// Specifically here because of that bastard Ron POULSEN
			if (group.equals("UG")) return "INDEPENDENT";
			else return group.replace(",", "") + " - " + STATE_ID;
			
		}
		return party.replace(",", "");
	}
	
	
	// Methods related to candidates and parties.

	/**
	 * Returns the list of candidates that contested the election
	 * below the line.
	 */
	@Override
	public ArrayList<String> getCandidates() {
		return _candidateList;
	}
	
	/**
	 * Equivalent to getCandidates.
	 */
	@Override
	public ArrayList<String> getCandidatesUnaltered() {
		return _candidateList;
	}

	/**
	 * A list of all primary parties in an above the line group, as defined
	 * by the party that the first candidate for the group was a member of.
	 * E.g.:
	 * Group A 	-	1	-	Alice	- Party 1
	 * 		 A	- 	2	-	Bob		- Party 2
	 * 		 B	- 	1	-	Chris	- Party 3
	 * 		 B 	-	2	-	Dave	- Party 4
	 * Then the grouped list would be:
	 * ["Party 1", "Party 3"]
	 */
	@Override
	public ArrayList<String> getParties() { return _partyOrder; }
	
	/**
	 * Equivalent to getGroupedParties.
	 */
	@Override
	public ArrayList<String> getPartiesUnaltered() {
		return _partyOrder;
	}
	
	/**
	 * A list of all parties contesting the election, including those 
	 * running in groups with other parties, and those who are running ungrouped.
	 */
	@Override
	public ArrayList<String> getPartiesIncludingSecondary() {	return _allParties; }

	/**
	 * The number of groups running in the election above the line, equivalent to the
	 * size of <code>getGroupedParties</code>.
	 */
 	private int numParties() 		{ return _partyOrder.size(); }
 	
	/** The number of candidates in the race, equivalent to the length of getCandidates(). */
	private int numCandidates() 	{ return _candidateList.size(); }
	
	/**
	 * Returns the party of the <candidate> as displayed in the original header line 
	 * of the document; this includes the group identifier.
	 */
	@Override
	public String partyOf(String c)	{ return _candidateToParty.get(c); }


	
	// Methods related to the DataInformer's file.

	/**
	 * The name of the file that this DataInterface is reading from.
	 */
	@Override
	public String getCurFileName()	{ return _dataFile; }

	
	// Methods related to getting a new ballot.
	
	/**
	 * Informs the requester of if there are any more ballots in this DataInterface.
	 * A call of <code>nextBallot</code> or <code>hasMoreBallots</code> will destroy the
	 * previously created ballot and overwrite it with the newly created ballot.
	 */
	@Override
	public boolean hasMoreBallots() 	{
		_nextBallot = nextBallot();
		return _nextBallot != null; 
	}
	
	/**
	 * Finds all lines in the file that belong to the next ballot, and returns an
	 * ArrayList that contains the tokenized version of each of those lines.
	 * @return All lines in the next ballot.
	 */
	protected ArrayList<String[]> collectCurBallotLines() {

		if (_nextLine == null) 	return null; 
		
		ArrayList<String[]> tempBallotArr = new ArrayList<>();
		
		tempBallotArr.add(_nextLine);
		_nextLine = null;
		
		while (_scan.hasNextLine()) {
			_nextLine = _scan.nextLine().split("\t");
			
			// Adds all lines that have matching voter ID numbers.
			if (_nextLine[_voterIDIndex].equals(tempBallotArr.get(0)[_voterIDIndex])) {
				tempBallotArr.add(_nextLine);
				_nextLine = null;
			} else {
				break;
			}
		}
		
		return tempBallotArr;
	
	}
	
	/**
	 * Takes an array of all the lines in the ballot and assembles it into a StandardBallot;
	 * places it in the ballot provided as an argument.
	 * @param tempBallot The list of tokenized lines in the ballot.
	 * @param ballot The StandardBallot that this information will be put into.
	 * @return The fully constructed StandardBallot.
	 */
	protected StandardBallot putArrIntoBallot(ArrayList<String[]> tempBallot, StandardBallot ballot) {
		
		int parsed;
		ballot.clearBallot();
		
		for (String[] line : tempBallot) {
			
			// Takes every line in the ballot and adds the preference expressed in that line
			// to the StandardBallot, assuming a preference is expressed.
			
			// Deals with above the line votes.
			if (line[_rankIndex].equals("")) continue;
			
			if (line[_typeIndex].equals("SATL") || line[_typeIndex].equals("RATL")) {
				
				parsed = Integer.parseInt(line[_rankIndex]) - 1;
				
				if (parsed < numParties()) {
					ballot.setATL(parsed, _groupToParty.get(line[_groupIndex]), false);
				}
				
				// Deals with below the line votes.
			} else if (line[_typeIndex].equals("BTL")) {
				
				parsed = Integer.parseInt(line[_rankIndex]) - 1;
				
				if (parsed < numCandidates()) {
					ballot.setBTL(parsed, line[_nameIndex], false);
				}
			}
		}
		
		return ballot;
	}

	/**
	 * Provides the next ballot in the file of this DataInterface.
	 * A call of <code>nextBallot</code> or <code>hasMoreBallots</code> will destroy the
	 * previously created ballot and overwrite it with the newly created ballot.
	 */
	@Override
	public StandardBallot nextBallot() 	{
		
		if (_nextBallot != null) {
			_curBallot = _nextBallot;
			_nextBallot = null;
			return _curBallot;
		}
				
		ArrayList<String[]> tempBallotArr = collectCurBallotLines();
		
		if (tempBallotArr == null) return null;
		
		if (tempBallotArr.get(0)[_formalityIndex].equals("Informal")) {
			return nextBallot();
		}
		
		_curBallot = putArrIntoBallot(tempBallotArr, _curBallot);
		
		return _curBallot;
	}
	
}
