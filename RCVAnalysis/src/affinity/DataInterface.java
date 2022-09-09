package affinity;

import java.util.ArrayList;

/**
 * An interface that allows standard interaction with files containing multiple ballots from an
 * instant run-off ranked choice election.<br><br>
 * 
 * The interface allows:<br>
 * 	- requests for ballots, provided in the StandardBallot format; <br>
 * 	- queries if there are more ballots remaining in the file;<br>
 *  - lists of the candidates and parties running in the election;<br>
 *  - conversions from a candidate to the party they are a member of.<br>
 *  
 * @author Michael Peeler
 * @version July 30th, 2022
 */
public interface DataInterface {
	
	// Abstract methods relating to party and candidate lists.
	
	/**
	 * Collects a list of all the parties included in the election. This function should
	 * return the party names as they are interally processed by the instance of the 
	 * {@link DataInterface}. This method is used mainly in PartyNameCollector to create 
	 * Alias files.
	 * @return ArrayList<String> The internal list of the parties in the ballot set.
	 */
	public ArrayList<String> getPartiesUnaltered();
	
	/**
	 * Collects a list of the first member in every above the line ballot group.
	 * If a ballot group includes multiple members in their below the line candidates,
	 * then any secondary parties will not be included. Additionally, any party
	 * which runs only ungrouped candidates will not be included in this list.
	 * By default, it is equivalent to {@link #getPartiesUnaltered()}, but this can
	 * be overridden by specific implementations.
	 * @return ArrayList<String> The list of parties.
	 */
	default public ArrayList<String> getParties() {
		return getPartiesUnaltered();
	}
	
	/**
	 * A list of all parties competing, including ungrouped parties if ungrouped candidates
	 * are given a party designation, and secondary parties in a group if a group contains more 
	 * than one parties.
	 * @return The list of all possible parties.
	 */
	public ArrayList<String> getPartiesIncludingSecondary();
		
	/**
	 * Returns an ArrayListof all candidates that appear in the current set of ballots,
	 * in the form in which they appear as they are being processed by the instance of 
	 * the {@link DataInterface}. This may not necessarily be the same way they appear in 
	 * the file itself, but it is the list used internally.
	 * @return ArrayList<String> The internal list of candidates in the ballot set.
	 */
	public ArrayList<String> getCandidatesUnaltered();
	
	/**
	 * Returns an ArrayList of all candidates that appear in the current set
	 * of ballots, with their names potentially altered from the internal list,
	 * often to strip out unnecessary information like group identifiers. By 
	 * default, it is equivalent to getCandidatesUnaltered, but can be overridden
	 * by specific implementations.
	 * @return ArrayList<String> The list of altered candidates in the ballot set.
	 */
	default public ArrayList<String> getCandidates() {
		return getCandidatesUnaltered();
	}
	
	/**
	 * Returns the party that a specified candidate belongs to. The specific party name
	 * should be drawn from the {@link #getParties()} list.
	 * @param candidate
	 * @return Candidate's party.
	 */
	public String partyOf(String candidate);
	
	/**
	 * In elections where multiple parties can run under a single group name, this will
	 * return the name of the first party listed in the group that the provided party 
	 * is a member of. For instance, if the both Liberal and National are members of Group A,
	 * and Liberal is the first party, then feeding in the String "National" will yield a 
	 * return of "Liberal". This will work with party names as they appear both in
	 * the {@link #getPartiesUnaltered} list, the {@link #getParties} list, and the
	 * {@link #getPartiesIncludingSecondary} list, and will yield a return from either the 
	 * {@link #getPartiesUnaltered} list or the {@link #getParties} list. <br><br>
	 * 
	 * As of @date July 2022, no DataInterface yet overrides the default implementation, but it 
	 * could potentially be beneficial to implement it for elections like Aus Sen 2016, then
	 * utilize a combination of it and the {@link #getPartiesIncludingSecondary} to split up 
	 * parties like the Liberal and National in the automatic generation of alias files and party
	 * orderings.<br><br>
	 * 
	 * By default, and thus for every call as of @date July 2022 but ideally only for elections
	 * where a single group only contains a single party, this will return the same value provided
	 * as input.
	 * @param party The name of a potentially secondary party.
	 * @return The name of the primary party in the provided party's group.
	 */
	default public String firstPartyOfGroupOf(String party) {
		return party;
	}
	
	
	// Abstract methods relating to the file the DataInterface processes.

	/**

	 * Returns the file name of the file that the {@link DataInterface} is drawing from.
	 * @return File name.
	 */
	public String getCurFileName();
	
	
	// Abstract methods relating to new ballots.
	
	/**
	 * Returns whether or not the {@link DataInterface} has more ballots in it.
	 * To minimize space usage and memory allocation time, many implementations of this method
	 * will cause a call to it to destroy or overwrite a previously returned ballot.
	 * It should be assumed that a call of {@link #nextBallot()} or {@link #hasMoreBallots()}
	 * will destroy previously created ballots, so either a copy should be made or all operations
	 * completed on the ballot before either method is called.
	 * @return If there are more ballots in the stream.
	 */
	public boolean hasMoreBallots();
	
	/**
	 * Returns the next ballot, if there is one.
	 * To minimize space usage and memory allocation time, many implementations of this method
	 * will cause a call to it to destroy or overwrite a previously returned ballot.
	 * It should be assumed that a call of {@link #nextBallot()} or {@link #hasMoreBallots()} 
	 * will destroy previously created ballots, so either a copy should be made or all operations
	 * completed on the ballot before either method is called.
	 * @return The next ballot in the stream.
	 */
	public StandardBallot nextBallot();	
	
}
