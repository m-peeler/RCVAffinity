package affinity;

// Replaces affinity.Party.java

import java.io.PrintWriter;

/**
 * A class to store a party's preference between two other parties; stores
 * how often a ballot with party A as the first preference vote chooses
 * party B, verses how often it choose party C. Based on Marc's "Party" class
 * and Connor's "ManualParty" class.
 * 
 * @author Michael Peeler
 * @version August 2nd, 2022
 *
 */

public class SpectrumParty {

	/** Information about what this {@link SpectrumParty} represents */
	
	/** Official name of the party whose votes are being counted. */
	private String _partyName; 
	/** 
	 * Upper party which this party is being compared against; the higher the 
	 * spectrum value, the closer to this party they are.
	 */
	private String _upperBound;
	/**
	 * Lower party which this party is being compared against; the lower the
	 * spectrum value, the closer to this party they are.
	 */
	private String _lowerBound;

	
	/** Specific counts internal to the {@link SpectrumParty} */
	
	/** How many of this party's votes rank the upper party and not the lower party. */
	private int _upperOnlyRank;
	/** How many of this party's votes rank the lower party and not the upper party. */
	private int _lowerOnlyRank;
	/** How many of this party's votes rank the upper party before the lower party. */
	private int _upperPreferRank;
	/** How many of this party's votes rank the lower party before the upper party. */
	private int _lowerPreferRank;
	/** How many of this party's votes rank neither party. */
	private int _neitherRank;

	/**
	 * Main constructor, which takes the names of the party, as well as the
	 * lower and upper parties it will be placed on a spectrum between.
	 * @param partyName
	 * @param lBound
	 * @param uBound
	 */
	public SpectrumParty(String partyName, String lower, String upper) {
		
		this._partyName = partyName;
		
		_lowerBound = lower;
		_upperBound = upper;
				
		_neitherRank = 0;
		_lowerOnlyRank = 0;
		_upperOnlyRank = 0;
		_lowerPreferRank = 0;
		_upperPreferRank = 0;
		
	}

	
	// Methods related to getting and setting the number of
	// preferences. 
	
	/**
	 * @return How many of this party's voters prefer the upper party to the lower party.
	 */
	public int getUpperPrefered() 		{ return _upperPreferRank; }
	
	/**
	 * @return How many ballots either ranked upper above lower, or ranked upper but did not rank lower.
	 */
	public int getUpperRanked() 		{ return _upperOnlyRank + _upperPreferRank; }

	/**
	 * @return How many of this party's voters prefer the lower party to the upper party.
	 */
	public int getLowerPrefered() 		{ return _lowerPreferRank; }	
	
	/**
	 * @return How many ballots either ranked lower above upper, or ranked lower but did not rank upper.
	 */
	public int getLowerRanked()			{ return _lowerOnlyRank + _lowerPreferRank; }
	
	/**
	 * @return How many first preference votes this party has received.
	 */
	public int getTotalVotes() 		{ return _upperOnlyRank + _lowerOnlyRank + _neitherRank + _lowerPreferRank + _upperPreferRank; }
	
	/**
	 * @param val Sets {@link SpectrumParty#_upperPreferRank} to this value.
	 */
	public void setUpperPreferred(int val) 	{ 
		_upperPreferRank = val; 
	}

	/**
	 * @param val Sets {@link SpectrumParty#_upperOnlyRank} to this value.
	 */
	public void setUpperOnly(int val)		{ 
		_upperOnlyRank = val; 
	}

	/**
	 * @param val Sets {@link #_lowerPreferRank} to this value.
	 */
	public void setLowerPreferred(int val) 	{ 
		_lowerPreferRank = val; 
	}
	
	/**
	 * @param val Sets {@link #_lowerOnlyRank} to this value.
	 */
	public void setLowerOnly(int val)		{ 
		_lowerOnlyRank = val; 
	}
	
	/**
	 * @param val Sets {@link SpectrumParty#_neitherRank} to this value.
	 */
	public void setNeitherPreferred(int val) 	{ 
		_neitherRank = val; 
	}

	/**
	 * @return This party's official name, {@link #_partyName}
	 */
	public String getPartyName()	{ return _partyName; }
	
	/**
	 * @return The name of the lower bound party this is on a {@link Spectrum} between;
	 * {@link #_lowerBound}
	 */
	public String getLowerBound()	{ return _lowerBound; }
	
	/**
	 * @return The name of the upper bound party this is on a {@link Spectrum} between;
	 * {@link #_upperBound}
	 */
	public String getUpperBound()	{ return _upperBound; }
	
	/**
	 * @return The {@link SpectrumParty}'s spectrum value.
	 */
	public double getSpectrumVals()	{ 
		
		if (getUpperRanked() + getLowerRanked() < 1) return -1;
		else return (double) getUpperRanked() / ((double) getUpperRanked() + (double) getLowerRanked()); 
		
	}
	
	/**
	 * @return The {@link SpectrumParty}'s spectrum value when only ballots
	 * where both ends of the spectrum are counted. 
	 */
	public double getRestrictedSpectrumVals() {
		if (_upperPreferRank + _lowerPreferRank < 1) return -1;
		return (double) _upperPreferRank / ((double) _upperPreferRank + (double) _lowerPreferRank);
	}
	
	
	// Method to add new ballot data to the SpectrumParty.
	
	/**
	 * Adds a preference based on the {@link StandardBallot.Ranking} value
	 * provided.
	 * @param pref The specific preference of the ballot being added.
	 */
	public void addPreference(StandardBallot.Ranking pref) {
		
		switch (pref) {
		case INFORMAL:
			break;
		case FIRST_ONLY:
			_lowerOnlyRank++;
			break;
		case SECOND_ONLY:
			_upperOnlyRank++;
			break;
		case FIRST_PREFERED:
			_lowerPreferRank++;
			break;
		case SECOND_PREFERED:
			_upperPreferRank++;
			break;
		case NEITHER:
			_neitherRank++;
			break;
		}
	}
	
	
	// Printing and saving methods.
	
	/**
	 * Generates the formatted String with information about the party's spectrum
	 * value.
	 * @return String containing spectrum information.
	 */
	private String spectrumString() {

		String s = new String();
		s += "\n" + _partyName + " SpectrumVals: \nBallot Size: " + getTotalVotes() + "\n";
		s += String.format("Ur/(Ur+Lr) = %.4f\n", getSpectrumVals());
		s += String.format("Up/(Up+Lp) = %.4f\n", getRestrictedSpectrumVals());
		s += "Ur - Lr: " + (getUpperRanked() - getLowerRanked()) + "\nTotal Votes: vs " + _lowerBound + ": " +
				getLowerRanked() + " " + _upperBound + ": " + getUpperRanked() + "\n";
		s += "Up - Lp: " + (_upperPreferRank - _lowerPreferRank) + "\nTotal Ranked Above: vs " + _lowerBound + ": " + _lowerPreferRank
				+ " " + _upperBound + ": " + _upperPreferRank + "\n";
		s += "No " + _upperBound + " or " + _lowerBound + " ranked: " + _neitherRank;
		return s;
	}
	
	/**
	 * Prints information about the spectrum values.
	 */
	public void printSpectVals() 			{ System.out.println(spectrumString()); }

	/**
	 * Saves information about spectrum values using the <out> PrintWriter object.
	 * @param out PrintWriter that saves the string to a file.
	 */
	public void printSpectrumToFile(PrintWriter out) 	{ out.println(spectrumString()); }
	
	/**
	 * Prints out a formatted total number of votes the party has received.
	 */
	public void printTotalVotes() 	{
		System.out.println("When " + _upperBound + " and " + _lowerBound + 
				" appear, " + getTotalVotes() + " ballots have this first preference.");
	}
	
}