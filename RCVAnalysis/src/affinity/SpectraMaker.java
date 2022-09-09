package affinity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;

/**
 * Taking a {@link BallotStream} and a number of {@link Spectrum} bounds, 
 * {@link SpectraMaker} creates all the specified {@link Spectrum} and fills
 * them with data from the ballots in the {@link BallotStream}. It can
 * also provide information on the standard deviations of the spectra.
 * @author Marc D'Avanzo
 * @edited Connor Clark
 * @edited Michael Peeler
 * @version August 1st, 2022
 *
 */
public class SpectraMaker {

	/**
	 * The collection of all the {@link Spectrum} being made.
	 */
	private ArrayList<Spectrum> _spectra;
	/**
	 * The {@link BallotStream} that will provide ballots for the SpectrumArrays being made.
	 */
	private BallotStream _info;
	/**
	 * Indicates if ballots have been added to the {@link Spectrum} in the 
	 * {@link SpectraMaker} already; while false, more {@link Spectrum} can
	 * be added but the list of spectra cannot be accessed; while true,
	 * the spectra can be accessed but more cannot be added.
	 */
	private boolean _ballotsAdded;
	
	/**
	 * Takes in the {@link BallotStream} and creates the collection of {@link Spectrum}
	 * that will be used to hold the various spectra about to be made. Includes in
	 * the  either all spectra between lower and upper, or the spectrum
	 * of lower versus upper.
	 * 
	 * If all is true, lower is A and upper is B, the {@link SpectraMaker} will include: <br>
	 * 	A(A + 1), A(A + 2), A(A + 3)... AMax <br>
	 *  (A + 1)(A + 2), (A + 1)(A + 3), (A + 1)(A + 4)... (A + 1)Max <br>
	 * 	... <br>
	 *  (B - 1)B, (B - 1)(B + 1), ... (B - 1)Max <br><br>
	 * 
	 * If all is false, lower is A and upper is B, the {@link SpectraMaker} will include: <br>
	 *  AB <br><br>
	 *  
	 * If all is false, lower is -1 and upper is -1, the {@link SpectraMaker} will include:<br>
	 *  nothing<br><br>
	 * 
	 * @param info BallotStream that the ballots will come from.
	 * @param lower Minimal bound to be added
	 * @param upper One greater than the maximal bound to be added.
	 * @param all Whether all parties between the two provided should be added
	 * to the spectrum, indicated by true, or just the two provided.
	 */
	public SpectraMaker(BallotStream info, int lower, int upper, boolean all) {

		_info 			= info;
		_spectra 		= new ArrayList<Spectrum>();
		_ballotsAdded 	= false;
		
		if (lower != -1 && upper != -1) {
			if (all)	addAllPartyPairsBetween(lower, upper);
			else 		addPartyPairs(lower, upper);
		}
			
	}
	
	/**
	 * Takes in the BallotStream and creates the collection of SpectrumArrays that will
	 * be used to hold the various spectra about to be made. Includes in
	 * the spectrum array all spectra between upper and lower.
	 * 
	 * If lower is A and upper is B, the SpectrumArray will include: <br>
	 * 	A(A + 1), A(A + 2), A(A + 3)... AMax <br>
	 *  (A + 1)(A + 2), (A + 1)(A + 3), (A + 1)(A + 4)... (A + 1)Max <br>
	 * 	... <br>
	 *  (B - 1)B, (B - 1)(B + 1), ... (B - 1)Max <br>
	 *  
	 *  If lower or upper are -1, the {@link SpectraMaker} will be empty.
	 * 
	 * @param info BallotStream that the ballots will come from.
	 * @param lower Minimal bound to be added
	 * @param upper One greater than the maximal bound to be added.
	 */
	public SpectraMaker(BallotStream info, int lower, int upper) {
		this(info, lower, upper, true);
	}
	
	/**
	 * Takes in the BallotStream and creates the collection of SpectrumArrays that will
	 * will be used to hold the various spectra about to be made. Includes no
	 * spectra initially.
	 * @param info BallotStream that data will come from.
	 */
	public SpectraMaker(BallotStream info) {
		this(info, -1, -1);
	}

	/**
	 * Adds a spectrum containing the specified two parties to the
	 * SpectrumArray.
	 * @param lower Index of the lower party.
	 * @param upper Index of the upper party.
	 */
	public void addPartyPairs(int lower, int upper) {
		
		if (_ballotsAdded) return;
		
		String lw = _info.usesCategoriesSecondary() ? _info.indexCategoryToName(lower) : _info.indexPartyToName(lower);
		String up = _info.usesCategoriesSecondary() ? _info.indexCategoryToName(upper) : _info.indexPartyToName(upper);
		ArrayList<String> lst = _info.usesCategoriesPrimary() ? _info.getUniqueCategoriesList() : _info.getUniquePartyList();
		
		_spectra.add(	new Spectrum(lw, lower, up, upper, lst )	);
	}
	
	/**
	 * Adds all parties with a first index of at least lower and not greater than upper, 
	 * and a second index of not greater than max, into the SpectrumArray.
	 * 
	 * If lower is A, upper is B, and max is M the SpectrumArray will include: <br>
	 * 	A(A + 1), A(A + 2), A(A + 3)... AM <br>
	 *  (A + 1)(A + 2), (A + 1)(A + 3), (A + 1)(A + 4)... (A + 1)M <br>
	 * 	... <br>
	 *  (B - 1)B, (B - 1)(B + 1), ... (B - 1)M <br>
	 * 
	 * @param lower Minimal lower index.
	 * @param upper One greater than the maximal lower index.
	 * @param max One greater than the maximal upper index.
	 */
	public void addAllPartyPairsBetween(int lower, int upper, int max) {
		
		if (_ballotsAdded) return;
		
		for (int i = lower; i < upper - 1; i++) {
			for (int j = i + 1; j < max; j++) {
				addPartyPairs(i, j);
			}
		}
	}
	
	/**
	 * Adds all parties with a first index of at least lower and not greater than upper
	 * into the SpectrumArray. 
	 *  
	 * If lower is A, upper is B, and the number of total parties is M, the SpectrumArray will include: <br>
	 * 	A(A + 1), A(A + 2), A(A + 3)... AM <br>
	 *  (A + 1)(A + 2), (A + 1)(A + 3), (A + 1)(A + 4)... (A + 1)M <br>
	 * 	... <br>
	 *  (B - 1)B, (B - 1)(B + 1), ... (B - 1)M <br>
	 * 
	 * @param lower Minimal lower index.
	 * @param upper One greater than the maximal lower index.
	 * @param max One greater than the maximal upper index.
	 */	
	public void addAllPartyPairsBetween(int lower, int upper) {
		if (_ballotsAdded) return;
		addAllPartyPairsBetween(lower, upper, _info.getSecNumOptions());
	}
	
	
	public void makeAndSaveCurrentSpectrumPairs(String dest) {
		moveDataIntoSpectra();
		saveSpectra(dest);
	}
	
	/**
	 * Removes all parties 
	 * @param lower
	 * @param upper
	 * @param dest
	 */
	public void makeAndSaveSpectrumPairsBetween(int lower, int upper, String dest) {
		addAllPartyPairsBetween(lower, upper);
		moveDataIntoSpectra();
		saveSpectra(dest);
	}

	/**
	 * Creates a spectrum for every pair of parties in the BallotStream, moves all ballots, and
	 * saves the results as output to the destination provided.
	 * @param dest File path to the location where output should be saved.
	 */
	public void makeAndSaveAllSpectrumPairs(String dest) {
		makeAndSaveSpectrumPairsBetween(0, _info.getSecNumOptions(), dest);
	}
	
	/**
	 * Takes every ballot in the {@link BallotStream} and adds it to every {#link Spectrum}
	 * in the collection. The {@link BallotStream} is not reset at the end of the operation.
	 * {@link #_ballotsAdded} is changed to true at the end of the ballot processing.
	 */
	public void moveDataIntoSpectra() {

		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
		
		// Runs for all ballots
		while (_info.hasMoreBallots()) {
					
			// Advances ballot and skips if it is not formal.
			_info.nextBallot();
			if (! _info.ballotIsFormal()) continue;
			
			// Iterates through all states and adds the preference to those where
			// both parties compete on the ballot.
			for (Spectrum cur : _spectra) {
				if (_info.secRunsInCurrentState(cur.getUpperName())
					&& _info.secRunsInCurrentState(cur.getLowerName())) 
				{
					_info.addPreferenceBetween(cur.getPartyAtIndex(_info.getPrimChoiceIndex()));
				}
			}		
			// Prints every million ballots if debugging is on.
			if (_info.debugging() && _info.currentBallotNum() % 1000000 == 0) {
				System.err.println(_info.currentBallotNum() + " ballots have been read so far. Time: " + dtf.format(LocalDateTime.now()));
			}
		}
		
		_ballotsAdded = true;
	}

	/**
	 * If data has been moved from the {@link BallotStream} into the various
	 * {@link Spectrum} by then {@link #moveDataIntoSpectra()} method, prints the 
	 * spectrum values and the standard deviations; otherwise, does nothing.
	 * @param outputFolder The filepath to a directory where the various
	 * {@link Spectrum} will be saved to.
	 */
	public void printSpectra() {
		if (!_ballotsAdded) return;
		for (int i = 0; i < _spectra.size(); i++) {
			for (SpectrumParty temp : _spectra.get(i).getSpectrumParties()) {
				System.out.println(temp.getPartyName());
				temp.printTotalVotes();
			}
		}
	}

	/**
	 * If data has been moved from the {@link BallotStream} into the various
	 * {@link Spectrum} by then {@link #moveDataIntoSpectra()} method, saves the 
	 * spectrum values and the standard deviations to a file named after the indexes
	 * of the two parties being compared; this file is saved to the filepath specified
	 * by the outputFolder String. Otherwise, does nothing.
	 * @param outputFolder The filepath to a directory where the various
	 * {@link Spectrum} will be saved to.
	 */
	public void saveSpectra(String outputFolder) {
		
		if (!_ballotsAdded) return;
		
		PrintWriter out;
		
		for (Spectrum arr : _spectra) {
		
			File file = new File(outputFolder + "\\" +
						String.format("%02d-%02d.txt",
								arr.getLowerIndex(),
								arr.getUpperIndex())
			);
			
			try {
				file.getParentFile().mkdirs();
				file.createNewFile();
				out = new PrintWriter(new FileWriter(file));
				out.println("Lower bound: " + arr.getLowerName() + ", Upper Bound: " + arr.getUpperName());
			} catch (IOException e) {
				System.err.println("Error in saveSpectra: File Not Found");
				e.printStackTrace();
				return;
			}
			arr.saveSpectVals(out);
	

		}
		
	}
	
	/**
	 * If data has been moved from the {@link BallotStream} into the various
	 * {@link Spectrum} by then {@link #moveDataIntoSpectra()} method, returns
	 * the {@link Spectrum} with the highest {@link Spectrum#calcStandDev()}
	 * of spectrum values in all the {@link Spectrum} in {@link SpectraMaker}.
	 * Otherwise, returns null.
	 * @return The {@link Spectrum} with the highest alternative standard deviation.
	 */
	public Spectrum bestStandardDeviation() {
		
		if (!_ballotsAdded) return null;
		
		int bestInd = 0;
		double bestVal = -1.0;
		double curVal = -1.0;
		
		for (int i = 0; i < _spectra.size(); i++) {
			curVal = getStandardDeviation(i);
			if (curVal > bestVal) {
				bestInd = i;
				bestVal = curVal;
			}
		}
		
		return _spectra.get(bestInd);
	}
	
	/**
	 * If data has been moved from the {@link BallotStream} into the various
	 * {@link Spectrum} by then {@link #moveDataIntoSpectra()} method, returns
	 * the {@link Spectrum} with the highest {@link Spectrum#calcAltStandDev()}
	 * of spectrum values in all the {@link Spectrum} in {@link SpectraMaker}.
	 * Otherwise returns null.
	 * @return The {@link Spectrum} with the highest alternative standard deviation.
	 */
	public Spectrum bestAltStandardDeviation() {
		
		if (!_ballotsAdded) return null;
		
		int bestInd = 0;
		double bestVal = -1.0;
		double curVal = -1.0;
		
		for (int i = 0; i < _spectra.size(); i++) {
			curVal = getAltStandardDeviation(i);
			if (curVal > bestVal) {
				bestInd = i;
				bestVal = curVal;
			}
		}
		
		return _spectra.get(bestInd);
	}
	
	/**
	 * If data has been moved from the {@link BallotStream} into the various
	 * {@link Spectrum} by then {@link #moveDataIntoSpectra()} method, returns
	 * the standard deviation of the {@link Spectrum} at the specified index,
	 * otherwise returns -1.
	 * @param index Index in the {@link #_spectra} list of {@link Spectrum} 
	 * that the standard deviation will be gotten from.
	 * @return The standard deviation of the specified SpectrumArray.
	 */
	public double getStandardDeviation(int index) {
		return _ballotsAdded ? _spectra.get(index).calcStandDev() : -1;
	}

	/**
	 * If data has been moved from the {@link BallotStream} into the various
	 * {@link Spectrum} by then {@link #moveDataIntoSpectra()} method, returns
	 * the alternative standard deviation of the {@link Spectrum} at the 
	 * specified index, otherwise returns -1.
	 * @param index Index in the {@link #_spectra} list of {@link Spectrum} 
	 * that the alternative standard deviation will be gotten from.
	 * @return The standard deviation of the specified SpectrumArray.
	 */
	public double getAltStandardDeviation(int index) {
		return _ballotsAdded ? _spectra.get(index).calcAltStandDev() : -1;
	}
	
	/**
	 * If data has been moved from the {@link BallotStream} into the various
	 * {@link Spectrum} by then {@link #moveDataIntoSpectra()} method, returns
	 * an ArrayList of the {@link Spectrum} that were generated, 
	 * otherwise returns null.
	 * @return
	 */
	public ArrayList<Spectrum> getSpectra() {
		return _ballotsAdded ? _spectra : null;
	}

}