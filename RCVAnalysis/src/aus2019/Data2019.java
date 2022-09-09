package aus2019;

import affinity.DataInterface;
import affinity.StandardBallot;

import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.io.FileInputStream;

/**
 * An implementation of the <code>DataInterface</code> interface for the 2019 Australian Election.
 * When provided with the data files from the election, it will provide a stream of ballots from the
 * election.
 * 
 * @author Michael Peeler
 * @version July 8th, 2022
 */
public class Data2019 implements DataInterface
{
	
	// Instance variables to convert from candidates to parties.

	/** A map that turns a group letter into the group party. */
	protected Map<String, String> _groupToParty;
	/** A map that turns a candidate name into the group they are a member of. */
	protected Map<String, String> _candidateToGroup;

	
	// Instance variable for the last ballot returned.
	
	/** The current ballot produced by the DataInterface. */
	private StandardBallot _curBallot;
	
	
	// Instance variables pertaining to the current file.
	
	/** A scanner for reading from the file that contains the ballots. */
	protected Scanner _scan;
	/** The file being read from, which contains the ballots. */
	protected final String _file;
	
	
	// Instance variables for understanding the current file.
	
	/** The first line of the data file CSV, which contains the names of data field.*/
	protected final String _firstLine;
	/** A tokenized list of the first line of the CSV. */
	protected final ArrayList<String> _tokenList;
	/** The index in the CSV where the first party appears. */
	private final int _firstPartyIndex;
	/** The index in the CSV where the first candidate appears. */
	private final int _firstCandidateIndex;
	/** The list of parties contesting the election. */
	private final ArrayList<String> _parties;
	/** The list of candidates contesting the election. */
	private final ArrayList<String> _candidates;
	
	
	// Instantiation methods.
	
	public Data2019 (String fileName) {
		
		_file = fileName;
		_candidateToGroup = new HashMap<String, String>();
		_groupToParty = new HashMap<String, String>();
		
		try {
			_scan = new Scanner(new FileInputStream(fileName));
		} catch (FileNotFoundException e) {
			System.out.println("File not found: " + fileName);
		}
		
		_firstLine = _scan.nextLine(); 
				
		// Tokenizes the first line.
		_tokenList = tokenize();
		
		_firstPartyIndex = calcFirstPartyIndex();
		_firstCandidateIndex = calcFirstCandidateIndex();
		
		_parties = collectParties();
		collectGroupIdentifiers();
		_candidates = collectCandidates();
		
		_curBallot = new StandardBallot(numParties(), numCandidates());
		
	}
	
	/** 
	 * Tokenizes the first line of the CSV to know what each field corresponds to. 
	 */
	private ArrayList<String> tokenize() {
		
		String[] st = _firstLine.split(",");
		ArrayList<String> tokenList = new ArrayList<String>();
		
		int i = 0;
		
		while (i < st.length) {
			tokenList.add(st[i]);
			// Checks for BOMs in the first position and sanitizes them if needed.
			if (tokenList.get(tokenList.size() - 1).charAt(0) > 6000) 
				tokenList.set(tokenList.size() - 1, tokenList.get(tokenList.size() - 1).substring(1));
			i++;
		}
		
		return tokenList;
		
	}

	/** Calculates the index where the first party appears; does so by looking for group letters,
	 * which are signified by a letter followed by a colon, such as "A:".*/
	private int calcFirstPartyIndex() {
		
		int i = 0;
		String[] st = _tokenList.get(i).split(":");
		
		while (st.length < 2) {
			i++;
			st = _tokenList.get(i).split(":");
		}

		return i;
	}
	
	/**
	 * Calculates and returns the index at which the first candidate appears.
	 */
	private int calcFirstCandidateIndex() {
		
		ArrayList<String> partyTokens = new ArrayList<String>();
		
		for (int i = 0; i < _tokenList.size(); i++) {
			
			String[] st = _tokenList.get(i).split(":");
			
			// Identifies the first candidate by searching for the first
			// repeat of a party identifier token.
			if (st.length > 1 && partyTokens.contains(st[0])) {
				return i;
			} 
			partyTokens.add(st[0]);

		}
		return _tokenList.size();
	}

	/**
	 * Returns a list of parties in the election. Removes group prefixes. 
	 * If no party name appears, the group letter is used instead.
	 */
	protected ArrayList<String> collectParties() {
		
		ArrayList<String> parties = getPartiesUnaltered();
		
		for (int i = 0; i < parties.size(); i ++) {
			// Splits the party name on the colon; if there is a name after the colon, updates the party name to only have that
			// name, otherwise updates the name the only be the group number.
			
			if (parties.get(i).split(":").length > 1) 
				parties.set(i, parties.get(i).split(":")[1].replace(",", ""));
			else 
				parties.set(i, parties.get(i).split(":")[0]);
			
		}
		
		return parties;
		
	}
		
	/** 
	 * Creates the groupToParty map. 
	 */
	protected void collectGroupIdentifiers() {
		
		ArrayList<String> groups = getPartiesUnaltered();

		// Splits the party name on the colon, then adds the group identifiers
		// and the full party name to the map.
		for (int i = 0; i < groups.size(); i ++) _groupToParty.put(groups.get(i).split(":")[0], groups.get(i));			

		// Adds the ungrouped identifier.
		_groupToParty.put("UG", "UG");

	}
		
	/**
	 * Returns a list of the candidates in the election. Candidates are assumed to appear
	 * after parties in the CSV. This function calls collectCandidatesUnaltered() and
	 * removes group prefixes. Also creates the _candidateToGroup map.
	 */
	protected ArrayList<String> collectCandidates() {
		ArrayList<String> candidates = getCandidatesUnaltered();
		String[] split;
		
		for (int i = 0; i < candidates.size(); i ++) {
			
			// Splits the candidate name on the colon
			split = candidates.get(i).split(":");
			
			_candidateToGroup.put(candidates.get(i), split[0]);
			_candidateToGroup.put(split.length > 1 ? split[1] : "", split[0]);	
			
			candidates.set(i, split.length > 1 ? split[1] : "");
			
		}
		
		return candidates;
		
	}
	
	
	// Methods related to candidates and parties.
	
	/** 
	 * Returns a list of candidates as they have been cleaned
	 */
	public ArrayList<String> getCandidates() 	{ return _candidates; }

	/**
	 * Returns the list of candidates as they appear in the CSV.
	 */
	public ArrayList<String> getCandidatesUnaltered() {
		ArrayList<String> candidates = new ArrayList<String>(_tokenList.subList(_firstCandidateIndex, _tokenList.size()));
		return candidates;
	}
	
	/**
	 * Returns the ArrayList<String> of parties without group identifier prefixes.
	 */
	public ArrayList<String> getParties() 	{ return _parties; }
	
	/**
	 * Identical to getGroupedParties for this implementation; returns all parties that 
	 * are officially recorded as having contested the election.
	 */
	public ArrayList<String> getPartiesIncludingSecondary() { return _parties; }
	
	/**
	 * Returns a list of parties in the election as they appear in the CSV. Parties
	 * are assumed to include a prefix group identifier. Parties are assumed to start
	 * at element 6, as 2019 data includes six cells of non-party identifiers.
	 * @return ArrayList<String> List of the party names as they appear in the CSV.
	 */
	public ArrayList<String> getPartiesUnaltered() {		
		return new ArrayList<String>(_tokenList.subList(_firstPartyIndex, _firstCandidateIndex));
	}
	
	/**
	 * Returns the number of groups in the election above the line; identical to the size of 
	 * getGroupedParties.
	 * @return Number of parties.
	 */
	private int numParties() 		{ return _parties.size(); }
	
	/**
	 * Returns the number of candidates in the election.
	 * @return Number of candidates.
	 */
	private int numCandidates() 		{ return _candidates.size(); }
	
	/**
	 * Returns the party of the <candidate> as displayed in the original header line 
	 * of the document; this includes the group identifier.
	 */
	public String partyOf(String candidate) {
		return _groupToParty.get(_candidateToGroup.get(candidate));
	}

	
	// Methods related to the DataInformer's file.
	
	/**
	 * The name of the file that is currently being read from.
	 */
	public String getCurFileName() 			{ return _file; }
	
	
	// Methods related to getting a new ballot.
	
	/**
	 * Checks if there are more ballots available in the file the DataInterface is reading from.
	 */
	public boolean hasMoreBallots() { return _scan.hasNextLine(); }
	
	/**
	 * Parses the string of the data file into an array that can be turned into a ballot.
	 * @param str String being parsed; the original line in the CSV file.
	 * @return A string array which only contains the vote fields from the CSV; order is
	 * preserved.
	 */
	public String[] standardizeParse(String str) {
		
		String[] tempBallotArr = str.split(",");
		String[] ballotArr = new String[numParties() + numCandidates()];
		
		for (int i = _firstPartyIndex; i < tempBallotArr.length; i++) {
			ballotArr[i - _firstPartyIndex] = tempBallotArr[i].equals("") ? null : tempBallotArr[i];
		}

		return ballotArr;
	}
		
	/**
	 * Takes an ordered list of votes, tempBallot, and moves them into a StandardBallot named ballot, 
	 * which is returned.
	 * 
	 * This changes the ordering from the set ballot order into a preferential order, where the first preference
	 * appears first, second preference second, and so on.
	 * @param tempBallot Ordered vote list.
	 * @param ballot Ballot which votes are being put into.
	 * @return The newly constructed ballot.
	 */
	protected StandardBallot putVoteIntoBallot(String[] tempBallot, StandardBallot ballot) {
		
		int parsed;
		ballot.clearBallot();
		
		// Moves above the line votes into the StandardBallot
		for (int i = 0; i < numParties(); i++) {
			if (tempBallot[i] == null) continue;
			
			parsed = Integer.parseInt(tempBallot[i]) - 1;
			if (parsed < numParties()) {
				ballot.setATL(parsed, _parties.get(i), true);
			}

		}
		
		// Moves below the line votes into the StandardBallot
		for (int i = numParties(); i < numParties() + numCandidates(); i++) {
			if (tempBallot[i] == null) continue;
			
			parsed = Integer.parseInt(tempBallot[i]) - 1;
			if (parsed < numCandidates()) {
				ballot.setBTL(parsed, _candidates.get(i - numParties()), true);
			}
		}
		
		return ballot;
	}
		
	/**
	 * Provides the next ballot in the file in the form of a StandardBallot.
	 * Uses unofficial party names with group letters stripped off.
	 * A call of <code>nextBallot</code> will destroy the previously created
	 * ballot and overwrite it with the newly created ballot.
	 */
	@Override
	public StandardBallot nextBallot() {
		
		String[] tempBallotArr = standardizeParse(_scan.nextLine());

		_curBallot = putVoteIntoBallot(tempBallotArr, _curBallot);
		return _curBallot;
		
	}
	
}
