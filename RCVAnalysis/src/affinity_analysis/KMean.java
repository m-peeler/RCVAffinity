package affinity_analysis;

import java.util.ArrayList;
import affinity.Spectrum;
import affinity.SpectrumParty;


/**
 * Class to cluster parties using the KMean algorithm. I have begun building this based on the javaml/clustering/KMeans library,
 * adapted to work with SpectrumArrays; however, for lack of time, I have not finished this implementation.
 * 
 * The KMean will need to have some form of partial data cluster analysis. I would recommend some form of imputation, but Dr. Healy believes, and
 * I agree, that this appears to be future work.
 * 
 * @author Michael Peeler
 *
 */
public class KMean {
	
	private int _clusters;
	
 	private int _iters;
		
	private ArrayList<Spectrum> _centroids;
	
	public KMean(int clusters, int iterations) {
		this._clusters = clusters;
		this._iters = iterations;
	}
	
	public ArrayList<Spectrum>[] cluster(ArrayList<Spectrum> arr) {
		if (arr.size() == 0) {
			throw new RuntimeException("No data has been provided.");
		}
		if (_clusters == 0) {
			throw new RuntimeException("This cannot be run with no clusters.");
		}
		
		_centroids = new ArrayList<>();
		ArrayList<String> partyNames = new ArrayList<>();
				
		for (SpectrumParty sp : arr.get(0).getSpectrumParties()) partyNames.add(sp.getPartyName());
		
		for (int i = 0; i < _clusters; i++) {
			Spectrum rnd = Spectrum.randomSpectrum(partyNames);
			_centroids.add(rnd);
		}
		
		int iterationCount = 0;
		boolean centroidsChanged = true;
		boolean randomCentroids = true;
		while (randomCentroids || (iterationCount < this._iters && centroidsChanged)) {
			iterationCount++;
			
			int[] assignment = new int[arr.size()];
			
			for (int i = 0; i < arr.size(); i++) {
				int tmpCluster = 0;
				double minDistance = _centroids.get(0).correlationWith(arr.get(i));

				for (int j = 1; j < _centroids.size(); j++) {
					double dist = _centroids.get(j).correlationWith(arr.get(i));
				
					if (minDistance > dist) {
						minDistance = dist;
						tmpCluster = j;
					}
				}
				assignment[i] = tmpCluster;
			}
			
			//double[][] sumPosition = new double[_clusters][partyNames.size()];
			//int[] countPositions = new int[_clusters];
			for (int i = 0; i < arr.size(); i++) {
				Spectrum in = arr.get(i);
			
				for (int j = 0; j < partyNames.size(); j++) {
					double temVal = in.getPartyAtIndex(j).getSpectrumVals();
					if (temVal == -1) {
						
					}
					/// Stopped implementing here; was unsure how to calculate the sumPosition
					/// with missing data.
					/// sumPosition[assignment[i]][j] += 
				}
				
			}
			
		}
		
		return null;
		
	}
	
	
	
}
