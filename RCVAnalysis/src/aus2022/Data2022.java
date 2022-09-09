package aus2022;

import affinity.DataInterface;
import affinity.StandardBallot;

import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.io.FileInputStream;

/**
 * An implementation of the <code>DataInterface</code> interface for the 2022 Australian Election.
 * When provided with the data files from the election, it will provide a stream of ballots from the
 * election.
 * 
 * @author Michael Peeler
 * @version July 8th, 2022
 */
public class Data2022 implements DataInterface
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
	
	/** The scanner used to read ballots from <code>_dataFile</code>. */
	protected Scanner _scan;
	/** The file being read from, which contains the ballots. */
	protected final String _file;
	/** The ID string for the current state; equivalent to the state's abbreviation. */
	protected final String _stateID;
	
	
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
	private ArrayList<String> _parties;
	/** The list of candidates contesting the election. */
	private ArrayList<String> _candidates;
	
	
	// Instantiation methods.
	
	public Data2022 (String fileName) 				{
		
		_file = fileName;
		_candidateToGroup = new HashMap<String, String>();
		_groupToParty = new HashMap<String, String>();
		
		try {
			_scan = new Scanner(new FileInputStream(fileName));
		} catch (FileNotFoundException e) {
			System.out.println("File not found: " + fileName);
			e.printStackTrace();
		}
		
		_firstLine = _scan.nextLine(); 
				
		_stateID = fileName.split(".csv")[0].split("27966-")[1];
		
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
	 * @return List of the first line tokens.
	 */
	private ArrayList<String> tokenize()			{
		
		String[] st = _firstLine.split(",");
		ArrayList<String> tokens = new ArrayList<>();
		
		boolean inParties = false;
		int i = 0;
		
		while (i < st.length) {
			String curToken = st[i];
			
			if (inParties && !curToken.contains(":")) 
				tokens.set(tokens.size() - 1, tokens.get(tokens.size() - 1) + curToken);
			else tokens.add(curToken);
			
			if (curToken.equals(" Paper No")) inParties = true;
			
			// Checks for BOMs in the first position and sanitizes them if needed.
			if (tokens.get(tokens.size() - 1).charAt(0) > 6000) 
				tokens.set(tokens.size() - 1, tokens.get(tokens.size() - 1).substring(1));
			i++;
		}
		
		return tokens;
	}

	/**
	 * Calculates the index at which the first party above the line vote.
	 * @return First party index.
	 */
	private int calcFirstPartyIndex()				{
		
		int i = 0;
		while (!_tokenList.get(i).contains(":")) i++;
		return i;
		
	}
	
	/**
	 * Calculates the first index that a candidate appears at.
	 * @return The first party index.
	 */
	private int calcFirstCandidateIndex() 			{
		
		ArrayList<String> tokens = new ArrayList<String>();
		
		for (int i = _firstPartyIndex; i < _tokenList.size(); i++) {
			
			String[] st = _tokenList.get(i).split(":");		
			
			if (st.length > 1 && tokens.contains(st[0])) return i;
			
			tokens.add(st[0]);

		}
		
		return _tokenList.size();
	}
	
	/**
	 * Sets variable parties to a list of the parties in the election. Removes group prefixes. 
	 * If no party name appears, the group letter is returned.
	 */
	private ArrayList<String> collectParties()		{
		
		ArrayList<String> parties = getPartiesUnaltered();
		
		for (int i = 0; i < parties.size(); i ++) {
			
			// Splits the party name on the colon; if there is a name after the colon, updates the party name to only have that
			// name, otherwise updates the name the only be the group number.
			
			if (parties.get(i).split(":").length > 1) 
				parties.set(i, parties.get(i).split(":")[1].replace(",",""));
			else
				parties.set(i, parties.get(i).split(":")[0] + " - " + _stateID);
		}
		
		return parties;
				
	}
		
	/**
	 * Creates the map from group identifiers to party names.
	 */
	private void collectGroupIdentifiers() 			{
		
		ArrayList<String> groups = getPartiesUnaltered();

		// Splits the party name on the colon, then adds the group identifiers
		// and the full party name to the map.
		for (int i = 0; i < groups.size(); i ++) {
			_groupToParty.put(groups.get(i).split(":")[0], _parties.get(i));			
		}

		// Adds the ungrouped identifier.
		_groupToParty.put("UG", "Independent");

	}
		
	/**
	 * Creates a list of the candidates in the election. Candidates are assumed to appear
	 * after parties in the CSV. Also adds candidates, both with and without party prefixes,
	 * to the _candidateToGroup map.
	 */
	private ArrayList<String> collectCandidates() 	{
		
		ArrayList<String> candidates = getCandidatesUnaltered();
		String[] split;
		
		for (int i = 0; i < candidates.size(); i ++) {
			
			// Splits the candidate name on the colon
			split = candidates.get(i).split(":");
			
			_candidateToGroup.put(candidates.get(i), split[0]);
			_candidateToGroup.put(split.length > 1 ? split[1].replace(",", "") : "", split[0]);
									
			candidates.set(i, split.length > 1 ? split[1] : "");
	
		}
		
		return candidates;
		
	}

	
	// Methods related to candidates and parties.
	
	/**
	 * Provides the list of altered candidate names.
	 */
	@Override
	public ArrayList<String> getCandidates() 		{ return _candidates; }

	/**
	 * Returns the list of candidates as they appear in the CSV.
	 */
	@Override
	public ArrayList<String> getCandidatesUnaltered() {
		ArrayList<String> candidates = new ArrayList<String>(_tokenList.subList(_firstCandidateIndex, _tokenList.size()));
		return candidates;
	}
	
	/**
	 * Returns the ArrayList<String> of parties without group identifier prefixes.
	 */
	@Override
	public ArrayList<String> getParties() 	{ return _parties; }
	
	/**
	 * Identical to getGroupedParties for this implementation.
	 */
	@Override
	public ArrayList<String> getPartiesIncludingSecondary() 		{ return _parties; }
	
	/**
	 * Returns a list of parties in the election as they appear in the CSV. Parties
	 * are assumed to include a prefix group identifier. Parties are assumed to start
	 * at element 6, as 2019 data includes six cells of non-party identifiers.
	 * @return ArrayList<String> List of the party names as they appear in the CSV.
	 */
	@Override
	public ArrayList<String> getPartiesUnaltered() 	{		
		return new ArrayList<String>(_tokenList.subList(_firstPartyIndex, _firstCandidateIndex));
	}
	
	/** The number of groups running in the election above the line, equivalent to the 
	 * size of getGroupedParties().
	 */
	private int numParties() 						{ return _parties.size(); }
	
	/** The number of candidates in the race, equivalent to the length of getCandidates(). */
	private int numCandidates() 					{ return _candidates.size(); }

	/**
	 * Returns the party of the <candidate> as displayed in the original header line 
	 * of the document; this includes the group identifier.
	 */
	@Override
	public String partyOf(String candidate) {
		return _groupToParty.get(_candidateToGroup.get(candidate));
	}
	
	
	// Methods related to the DataInformer's file.
	
	/**
	 * The name of the file that this DataInterface is reading from.
	 */
	@Override
	public String getCurFileName() 			{ return _file; }
	
	
	// Methods related to getting a new ballot.
	
	/**
	 * Checks if there are more ballots available in the file the DataInterface is reading from.
	 */
	@Override
	public boolean hasMoreBallots() { return _scan.hasNextLine(); }
	
	/**
	 * Parses the string of the data file into an array that can be turned into a ballot.
	 * A call of <code>nextBallot</code> or <code>hasMoreBallots</code> will destroy the
	 * previously created ballot and overwrite it with the newly created ballot.
	 * @param str String being parsed; the original line in the CSV file.
	 * @return A string array which only contains the vote fields from the CSV; order is
	 * preserved.
	 */
	private String[] standardizeParse(String str) {
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
	private StandardBallot putVoteIntoBallot(String[] tempBallot, StandardBallot ballot) {
		
		int parsed;
		ballot.clearBallot();
		
		// Moves in ATL votes
		for (int i = 0; i < numParties(); i++) {
			if (tempBallot[i] == null) continue;
			
			parsed = Integer.parseInt(tempBallot[i]) - 1;
			if (parsed < numParties()) {
				ballot.setATL(parsed, _parties.get(i), true);
			}

		}
		
		// Moves in BTL votes.
		for (int i = numParties(); i < numParties() + numCandidates(); i++) {
			if (tempBallot[i] == null) continue;
			
			parsed = Integer.parseInt(tempBallot[i]) - 1;
			if (parsed < numCandidates()) {
				ballot.setBTL(parsed, getCandidatesUnaltered().get(i - numParties()), true);
			}
		}
		
		return ballot;
	}
		
	/**
	 * Provides the next ballot in the file in the form of a StandardBallot.
	 * Uses official names, as determined by the BallotInfo class.
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
