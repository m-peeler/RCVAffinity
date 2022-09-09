package nyc2021;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import affinity.DataInterface;
import affinity.StandardBallot;

/**
 * An implementation of the <code>DataInterface</code> interface for the 2021 New York City Election.
 * When provided with the data files from the election, it will provide a stream of ballots from the
 * election.
 * 
 * @author Michael Peeler
 * @version July 8th, 2022
 */
public class DataNYC2021 implements DataInterface {
	
	// Instance variables related to the candidates running in the election.
	
	/** A list of the candidates that are running in the NYC election. */
	private ArrayList<String> 	_candidates;
	/** A map that converts from an ID number to a candidate name. */
	private Map<String, String> _idsToNames;
	
	
	// Instance variable for the last ballot returned.
	
	/** The last created StandardBallot. */
	private StandardBallot 		_curBallot;
	
	
	// The immutable instance variables relating to the file locations and the election name.
	
	/** The file that data in this DataInterface is being drawn from. */
	private final String 		_dataFile;
	/** The file that the _idsToNames map was created using. */
	private final String		_idNumFile;
	/** The name of the current election being measured. */
	private final String 		_raceName;
	/** The file containing the list of candidates running. */
	private final String 		_namesFile;

	
	// Instance variables for the file being read from.
	
	/** The scanner used to read ballots from <code>_dataFile</code>. */
	private Scanner 			_scan;
	/** The first line of the data file CSV, which contains the names of data fields. */
	private String 				_firstLine;
	/** A tokenized list of the first line of the CSV. */
	private ArrayList<String>	_tokenList;
	/** A list of the indices that the specified election occurs in the current CSV. */
	private ArrayList<Integer> 	_indices;
		
	
	// Instantiation methods.
	
	public DataNYC2021 (String namesFile, String idNumFile, String dataFile, String race) {
		
		// Saves immutable data.
		_raceName = race;
		_dataFile = dataFile;
		_idNumFile = idNumFile;
		_namesFile = namesFile;
		
		// Collects id number to candidate name mapping.
		_idsToNames = new HashMap<String, String>();
				
		Scanner candScan;
		try {
			candScan = new Scanner(new FileInputStream(_idNumFile));
			makeIDToNames(candScan);
		} catch (FileNotFoundException e){
			System.out.println("Error in Candidate Scanner");
		}
		
		// Collects candidates running in the election. 
		
		try {
			candScan = new Scanner(new FileInputStream(_namesFile));
			_candidates = findCandidates(candScan);
		} catch (FileNotFoundException e) {
			System.out.println("Error in Candidate List scanner");
		}

		// Starts scanner of data file.
		
		try {
			_scan = new Scanner(new FileInputStream(_dataFile));
		} catch (FileNotFoundException e) {
			System.out.println("File not found: " + dataFile);
			e.printStackTrace();
		}
		
		// Parses first line to find relevant data.
		
		_firstLine = _scan.nextLine(); 
		_tokenList = tokenize();
		_indices = getRaceIndices();	
		_curBallot = new StandardBallot(_indices.size(), 0);
		
	}
	
	/**
	 * Collects the list of candidates, as listed in the scanner provided
	 * to the method.
	 * @param scan Scanner of file with the candidate names in it.
	 * @return A list of all the candidates running.
	 */
	private ArrayList<String> findCandidates(Scanner scan) {
	
		ArrayList<String> candidates = new ArrayList<>();
		
		while (scan.hasNextLine()) {
			candidates.add(scan.nextLine());
		}
		
		return candidates;
	}
	
	/**
	 * Returns a tokenized ArrayList of the first line, with potential BOMs removed.
	 * @return Tokenized first line.
	 */
	private ArrayList<String> tokenize() {
		
		String[] st = _firstLine.split(",");
		ArrayList<String> tokenList = new ArrayList<>();
		
		for (int i = 0; i < st.length; i++) {
			tokenList.add(st[i]);
			// Checks for BOMs in the first position and sanitizes them if needed.
			if (tokenList.get(tokenList.size() - 1).charAt(0) > 6000) 
				tokenList.set(tokenList.size() - 1, tokenList.get(tokenList.size() - 1).substring(1));
		}
		
		return tokenList;
		
	}
	
	/**
	 * Finds the indices in the tokenized list that correspond to the race being measured,
	 * and returns a list which contains them.
	 * @return List of indices for the specified race.
	 */
	private ArrayList<Integer> getRaceIndices() {
		ArrayList<Integer> indices = new ArrayList<>();
		
		for (int i = 0; i < _tokenList.size(); i++) {
			if (_tokenList.get(i).contains(_raceName)) {
				indices.add(i);
			}
		}
		
		return indices;
	}
	
	/**
	 * Uses the scanner of the id number to name file and creates a map to turn
	 * ID numbers into their name.
	 * @param candScan A scanner of the <code>_idNumFile</code>.
	 */
	private void makeIDToNames(Scanner candScan) {
			
		String[] curLine;
		while (candScan.hasNextLine()) {
			curLine = candScan.nextLine().split(",");
			_idsToNames.put(curLine[0], curLine[1]);
		}
		
	}

	
	// All implementations of methods related to "parties" (in this case, candidates), 
	// and "candidates" (in this implementation, always null).
	
	/** Returns a list of the candidates that are running in this election. */
	@Override
	public ArrayList<String> getParties() {
		return _candidates;
	}

	/** Equivalent to getGroupedParties. */
	@Override
	public ArrayList<String> getPartiesIncludingSecondary() {
		return _candidates;
	}

	/** Equivalent to getGroupedParties. */
	@Override
	public ArrayList<String> getPartiesUnaltered() {
		return _candidates;
	}

	/** Implemented only to satisfy the interface; always returns null. */
	@Override
	public ArrayList<String> getCandidates() {
		return null;
	}

	/** Implemented only to satisfy the interface; always returns null. */
	@Override
	public ArrayList<String> getCandidatesUnaltered() {
		return null;
	}

	/** 
	 * Converts ID numbers to the candidate name, and returns candidate name if called with
	 * a candidate name. 
	 */
	@Override
	public String partyOf(String candidate) {
		return _idsToNames.get(candidate);
	}

	
	// Methods relating to the file the DataInterface processes.

	/**
	 * Returns the name of the file which this DataInterface is reading from.
	 */
	@Override
	public String getCurFileName() {
		return _dataFile;
	}

	
	// Methods relating to getting new ballots.
	
	/** Indicates whether or not more ballots remain in this DataInterface. */
	@Override
	public boolean hasMoreBallots() 	{ return _scan.hasNextLine(); }
	
	/** 
	 * Turns the string from the data file into an preference-ordered array; also replaces
	 * "undervote" with null string.
	 */
	public String[] standardizeParse(String line) {
		String[] rtrn = line.split(",");
		
		for (int i = 0; i < rtrn.length; i++) {
			if (rtrn[i].equals("undervote")) rtrn[i] = null;
		}
		
		return rtrn;
	}
	
	/**
	 * Moves the preference-ordered array into the StandardBallot provided.
	 * @param tempBallotArr The preference-ordered ballot array.
	 * @param curBallot The StandardBallot the votes are being moved into.
	 * @return The constructed StandardBallot.
	 */
	public StandardBallot putArrIntoBallot(String[] tempBallotArr, StandardBallot curBallot) {
	
		curBallot.clearBallot();
		
		for (int i = 0; i < _indices.size(); i++) {
			if (tempBallotArr[_indices.get(i)] != null) {
				curBallot.setATL(i, _idsToNames.get(tempBallotArr[_indices.get(i)]), false);
			}
		}
		
		return curBallot;
	}

	/**
	 * Returns the next ballot in the data file.
	 */
	@Override
	public StandardBallot nextBallot() {
		String[] tempBallotArr = standardizeParse(_scan.nextLine());
		return putArrIntoBallot(tempBallotArr, _curBallot);
	}
	
}
