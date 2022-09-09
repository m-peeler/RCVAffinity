package affinity;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;


/**
 * An edit of the PartyArray class created by Marc and Connor
 * Hosts a list of {@link SpectrumParty} objects which story how often the 
 * ballots whose first preference is the {@link SpectrumParty}'s 
 * {@link SpectrumParty#getPartyName()} prefer the {@link Spectrum#_upperName}
 * party and how often they prefer the {@link Spectrum#_lowerName} party.
 * The {@link Spectrum} can also have its {@link #correlationWith(Spectrum)}
 * calculated with anothe {@link Spectrum}, and it can have the {@link #calcStandDev()}
 * taken of the parties inside the {@link Spectrum}.
 * 
 * @author Michael Peeler
 * @version July 31st, 2022
 *
 */

public class Spectrum {
		
	/** Random number generator for {@link #randomSpectrum(ArrayList)}. */
	private static final Random RANDOM = new Random();
	
	/** Array of {@link SpectrumParty} objects to add ballot information to. */
	ArrayList<SpectrumParty> _parties;


	// Instance variables related to the Spectrum's bounds parties.
	
	/** 
	 * The name of the party or group used as the lower bound in the {@link Spectrum}.
	 * The closer a {@link SpectrumParty#getSpectrumVals()} is to 0, the closer
	 * they are to this party or group.
	 */
	protected String _lowerName;
	/**
	 * The name of the party or group used as the upper bound in the {@link Spectrum}. 
	 * The closer a {@link SpectrumParty#getSpectrumVals()} is to 1, the closer
	 * they are to this party or group.
	 */
	protected String _upperName;
	
	/** The index of {@link #_lowerName} in {@link BallotStream#getSecOptions()}. */
	protected int _lowerIndex;
	/** The index of {@link #_upperName} in {@link BallotStream#getSecOptions()}. */
	protected int _upperIndex;
	
	
	// Comparator classes
	
	/** 
	 * Compares two spectra by their alternative standard deviation values, 
	 * {@link Spectrum#calcAltStandDev()}. Allows a set of spectra to be sorted
	 * by their standard deviation.
	 * 
	 * @author Michael Peeler
	 * @version July 31st, 2022
	 *
	 */
	public static class AltStandDevComparator implements Comparator<Spectrum> {
		
		public int compare(Spectrum one, Spectrum two) {
			double diff = one.calcAltStandDev() - two.calcAltStandDev();
			return diff > 0 ? 1 : diff < 0 ? -1 : 0;
		}
	}
	
	/** 
	 * Compares two spectra by their {@link _upperIndex} and {@link _lowerIndex}
	 * values. Allows a set of spectra to be sorted by these indices.
	 * {@link _upperIndex} takes priority, and if they are equivalent, then
	 * {@link _lowerIndex} is compared}. 
	 * 
	 * 0-1 < 0-2 < 1-0.
	 * 
	 * @author Michael Peeler
	 * @version July 31st, 2022
	 *
	 */	
	public static class IndexComparator implements Comparator<Spectrum> {
		public int compare(Spectrum one, Spectrum two) {
			if (one._upperIndex == two._upperIndex) 
				if (one._lowerIndex == two._lowerIndex)
					return 0;
				else
		 			return one._lowerIndex > two._lowerIndex ? 1 : -1;
		 			
			else 	return one._upperIndex > two._upperIndex ? 1 : -1;
		}
	}
	
	
	// Constructor methods
	
	/**
	 * Instantiates a spectrum with its upper and lower bounds parties, and
	 * the indices of those parties in the list of secondary options,
	 * as well as the list of primary options that will be placed
	 * between these parties.
	 * @param lowerName The name of the lower bounds party, {@link #_lowerName}
	 * @param lowerIndex The index of the {@link #_lowerName} in the
	 * 					{@link BallotStream#getSecOptions()}
	 * @param upperName The name of the upper bounds party, {@link #_upperName}
	 * @param upperIndex The index of the {@link #_upperName} in the
	 * 					{@link BallotStream#getSecOptions()}
	 * @param primaryNames The list of primary names, as provided by
	 * 					{@link BallotStream#getPrimOptions()}
	 */
	public Spectrum(String lowerName, int lowerIndex, String upperName, int upperIndex, ArrayList<String> primaryNames) {

		_lowerName = lowerName;
		_lowerIndex = lowerIndex;
		_upperName = upperName;
		_upperIndex = upperIndex;
		
		_parties = new ArrayList<SpectrumParty>();
		
		for (String name : primaryNames) {
			_parties.add(new SpectrumParty(name, _lowerName, _upperName));
		}
	}

	
	// Get methods 
	
	/**
	 * @return The list of {@link SpectrumParty} for this {@link Spectrum}
	 */
	public ArrayList<SpectrumParty> getSpectrumParties()
												{ return _parties; 	}
	
	/**
	 * @return {@link #_lowerName}, the party at the lower bound of the 
	 * {@link Spectrum}
	 */
	public String getLowerName()				{ return _lowerName; }
	
	/**
	 * @return {@link #_lowerIndex}
	 */
	public int getLowerIndex()					{ return _lowerIndex; }
	
	/**
	 * @return {@link #_upperName}, the party at the lower bound of the 
	 * {@link Spectrum}
	 */
	public String getUpperName()				{ return _upperName; }
	
	/**
	 * @return {@link #_upperIndex}
	 */
	public int getUpperIndex()					{ return _upperIndex; }
	
	/**
	 * @param index The index of a specific party from {@link BallotStream#getPrimOptions()}
	 * @return The {@link SpectrumParty} of the index specified.
	 */
	public SpectrumParty getPartyAtIndex(int index)		{ return _parties.get(index); }
	
	
	// File access methdods
	
	/**
	 * This method will save the {@link SpectrumParty#getSpectrumVals()}
	 * of every {@link SpectrumParty} in the {@link Spectrum} to the file 
	 * provided. Will additionally save the highest and lowest 
	 * {@link SpectrumParty#getSpectrumVals()} to the file.
	 * @param out The {@link PrintWriter} which the spectrum values will be
	 * written out to.
	 */
	public void saveSpectVals(PrintWriter out) {
		
		double specVal = 0;
		double highestSpecVal = -1;
		String bestSpecValParty = "";
		double lowestSpecVal = -1;
		String lowestSpecValParty = "";
		int totalUsableBallots = 0;
		
		for (SpectrumParty temp : _parties) {
			
			// highest and lowest spectrum value calculations
			specVal = temp.getSpectrumVals();
			if (highestSpecVal == -1 && lowestSpecVal == -1) {
				if (!Double.isNaN(specVal) && specVal != 1 && specVal != 0) {
					highestSpecVal = specVal;
					bestSpecValParty = temp.getPartyName();
					lowestSpecVal = specVal;
					lowestSpecValParty = temp.getPartyName();
				}
			}
			if (specVal > highestSpecVal && specVal < 1 && specVal > 0) {
				highestSpecVal = specVal;
				bestSpecValParty = temp.getPartyName();
			}
			if (specVal < lowestSpecVal && specVal < 1 && specVal > 0) {
				lowestSpecVal = specVal;
				lowestSpecValParty = temp.getPartyName();
			}
			temp.printSpectrumToFile(out);
			totalUsableBallots += temp.getTotalVotes();
		}
		
		out.println("\n" + "Standard Deviation: ");
		out.printf("%.4f\n", calcStandDev());
		out.println("\n" + "Non-extreme Standard Deviation: ");
		out.printf("%.4f\n", calcAltStandDev());
		out.println("\n" + "Restricted Standard Deviation: ");
		out.printf("%.4f\n", calcRstrctStandDev());
		out.println("\n" + "Non-extreme Restricted Standard Deviation: ");
		out.printf("%.4f\n", calcRstrctAltStandDev());
		out.println("\n" + "Largest Spectrum Value: " + bestSpecValParty);
		out.printf("%.4f\n", highestSpecVal);
		out.println("\n" + "Lowest Spectrum Value: " + lowestSpecValParty);
		out.printf("%.4f\n", lowestSpecVal);
		out.println("\nTotal Ballots Both Appeared:\n" + totalUsableBallots);
		out.close();
	}
	
	
	// Methods to compare two spectra to eachother
	
	/**
	 * Returns the Pearson's Correlation value of this {@link Spectrum} and a
	 * second {@link Spectrum}, ignoring any {@link SpectrumParty} where one
	 * or both spectra has a null value for its 
	 * {@link SpectrumParty#getSpectrumVals()}.
	 * @param other The second {@link Spectrum}.
	 * @return The correlation.
	 */
	public double correlationWith(Spectrum other) {
		
		ArrayList<SpectrumParty> otherArray = other.getSpectrumParties();
		
		ArrayList<Double> x = new ArrayList<>();
		ArrayList<Double> y = new ArrayList<>();

		// Step 1: Remove pairs with an invalid entry.
		for (int i = 0; i < Math.min(otherArray.size(), _parties.size()); i++) {
			SpectrumParty curOther = otherArray.get(i);
			for (int j = 0; j < Math.min(otherArray.size(), _parties.size()); j++) {
				SpectrumParty curThis = _parties.get(j);

				if (curOther.getPartyName().equals(curThis.getPartyName())
						&& curThis.getSpectrumVals() != -1 && curOther.getSpectrumVals() != -1) {
					x.add(curThis.getSpectrumVals());
					y.add(curOther.getSpectrumVals());
				}
			}
		}
			
		return (correlation(x, y));
	}
	
	/**
	 * Defines a function with another {@link Spectrum} that returns how many
	 * {@link SpectrumParty} objects both contained non-null
	 * {@link SpectrumParty#getSpectrumVals()}, and were thusly compatable
	 * for comparison.
	 * @param other The other {@link Spectrum} being compared against.
	 * @return The total number of compatable {@link SpectrumParty}
	 */
	public double compatable(Spectrum other) {
		
		ArrayList<SpectrumParty> otherArray = other.getSpectrumParties();
		
		int cmptbl = 0;

		// Step 1: Remove pairs with an invalid entry.
		for (int i = 0; i < Math.min(otherArray.size(), _parties.size()); i++) {
			SpectrumParty curOther = otherArray.get(i);
			for (int j = 0; j < Math.min(otherArray.size(), _parties.size()); j++) {
				SpectrumParty curThis = _parties.get(j);

				if (curOther.getPartyName().equals(curThis.getPartyName())
						&& curThis.getSpectrumVals() != -1 && curOther.getSpectrumVals() != -1) {
					cmptbl++;
				}
			}
		}
			
		return cmptbl;
	}
	
	
	// Various standard deviation methods
	
	/**
	 * This method will calculate the standard deviation of the 
	 * {@link SpectrumParty#getSpectrumVals()} of each {@link SpectrumParty}
	 * in the {@link Spectrum}, ignoring only those with null values. 
	 * @return The classical standard deviation.
	 */
	public double calcStandDev() {
		
		ArrayList<Double> list = new ArrayList<Double>();
		double temp;
		for (SpectrumParty party : _parties) {
			temp = party.getSpectrumVals();
			if (temp > -1) list.add(temp);
		}
		
		return standardDev(list);
		
	}

	/**
	 * This method will calculate the alternative standard deviation of the 
	 * {@link SpectrumParty#getSpectrumVals()} of each {@link SpectrumParty}
	 * in the {@link Spectrum}, ignoring those with null values as well as
	 * those with maximal and minimal values of 1 and 0, respectively. 
	 * @return The alternative standard deviation.
	 */
	public double calcAltStandDev() {

		ArrayList<Double> list = new ArrayList<Double>();
		double temp;
		for (SpectrumParty party : _parties) {
			temp = party.getSpectrumVals();
			if (temp > -1 && temp != 0 && temp != 1) {
				list.add(temp);
			}
		}
		
		return standardDev(list);

	}
	
	/**
	 * This method will calculate the standard deviation of the 
	 * {@link SpectrumParty#getRestrictedSpectrumVals()} of each 
	 * {@link SpectrumParty} in the {@link Spectrum}, ignoring only 
	 * those with null values. 
	 * @return The classical standard deviation of the restricted spectrum
	 * values.
	 */
	public double calcRstrctStandDev() {
		ArrayList<Double> list = new ArrayList<Double>();
		double temp;
		for (SpectrumParty party : _parties) {
			temp = party.getRestrictedSpectrumVals();
			if (temp > -1) list.add(temp);
		}
		
		return standardDev(list);
	}
	
	/**
	 * This method will calculate the alternative standard deviation of the 
	 * {@link SpectrumParty#getRestrictedSpectrumVals()} of each 
	 * {@link SpectrumParty} in the {@link Spectrum}, ignoring those with
	 * null values as well as those with maximal and minimal values of 1 and
	 * 0, respectively. 
	 * @return The alternative standard deviation of the restricted spectrum
	 * values.
	 */
	public double calcRstrctAltStandDev() {
		ArrayList<Double> list = new ArrayList<Double>();
		double temp;
		for (SpectrumParty party : _parties) {
			temp = party.getRestrictedSpectrumVals();
			if (temp > -1 && temp != 0 && temp != 1) {
				list.add(temp);
			}
		}
		
		return standardDev(list);
	}
		
	
	// Overridden methods
	
	/** Converts the spectrum into a string of the two bounds' names. */
	@Override
	public String toString() {
		return "Lower Bound: " + _lowerName + " Upper Bound: " + _upperName;
	}
	
	
	// Static methods
	
	/**
	 * Calculates the Pearson's Correlation of two lists of doubles.
	 * @param x First list.
	 * @param y Second list.
	 * @return The Pearson's Correlation
	 */
	public static double correlation(ArrayList<Double> x, ArrayList<Double> y) {
		/** Formula: 
		*   Pearson's Correlation
		*  
		*   For sets X and Y with elements X0 to Xn and Y0 to Yn:
		*  
		* 	 			sum ( (Xi - mean(X)) * (Yi - mean(Y)))
		* 	r = -------------------------------------------------------
		* 		sqrt ( sum ( (Xi - mean(X)) ^ 2 * (Yi - mean(Y)) ^ 2 ) )
		* 
		*   defining V(Ni) to mean Ni - mean(N), we can say:
		* 
		*  		 		sum( V(Xi) * V(Yi) )
		*   r = -----------------------------------
		*       sqrt ( sum (V(Xi) ^ 2) * sum (V(Yi) ^ 2))
		*/

		double meanX = 0, meanY = 0, entries = 0;
		
		for (int i = 0; i < Math.min(x.size(), y.size()); i++) {
			meanX += x.get(i);
			meanY += y.get(i);
			entries ++;
		}
		
		meanX = meanX / entries;
		meanY = meanY / entries;
		
		double difX, difY;
		double topSum = 0, bottomXSum = 0, bottomYSum = 0 ;
		
		for (int i = 0; i < entries; i++) {
			difX = x.get(i) - meanX;
			difY = y.get(i) - meanY;
			topSum += difX * difY;
			bottomXSum += difX * difX;
			bottomYSum += difY * difY;
		}
		
		double bottom = Math.sqrt(bottomXSum * bottomYSum);
		double cor = topSum / bottom;
				
		return cor;
	}
	
	/**
	 * Calculates the standard deviation of the ArrayList of doubles provided.
	 * @param dataset
	 * @return standard deviation of data
	 */
	public static double standardDev(ArrayList<Double> data) {
		// Step 1: mean
		double mean = 0;
		for (int c = 0; c < data.size(); c++) mean += data.get(c);
		mean /= data.size();

		// Step 2: squared deviation from mean
		double dev;
		double sumDev = 0;
		for (int i = 0; i < data.size(); i++) {
			dev = data.get(i) - mean;
			sumDev += dev * dev;
		}

		// Step 3: divide by list size minus one
		sumDev /= (data.size() - 1);

		// Step 4: Square root for final answer
		return Math.sqrt(sumDev);
	}
	
	/**
	 * Returns a {@link Spectrum} with a random value for each {@link SpectrumParty}
	 * in it; for future use in KMean tests.
	 * @param partyNames Primary category list of names.
	 * @return Random spectrum.
	 */
	public static Spectrum randomSpectrum(ArrayList<String> partyNames) {
		
		Spectrum rtrn = new Spectrum("Random", -1, "Random", -1, partyNames);
		
		int totalBallots = 100000;
		
		for (int i = 0; i < partyNames.size(); i++) { 
			SpectrumParty sp = rtrn.getPartyAtIndex(i);
			int noPreference = RANDOM.nextInt(totalBallots);
			int preferA = RANDOM.nextInt(totalBallots - noPreference);

			sp.setLowerPreferred(preferA);
			sp.setUpperPreferred(totalBallots - noPreference - preferA);
			sp.setNeitherPreferred(noPreference);
		}
		
		return rtrn;
		
	}
}
