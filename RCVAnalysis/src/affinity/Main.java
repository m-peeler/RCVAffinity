package affinity;

/**
 * This is the class that the {@link affinity} package is run out of.
 * @author Michael Peeler
 * @version August 4th, 2022
 *
 */
public class Main {
	
	/**
	 * Simple method to count a few interesting numbers - how many preferences are
	 * espoused (currently not working), how many ballots there are total, and how
	 * many ballots are BTL (currently not working).
	 */
	public static void countInterestingNumbers() {
		
		int totalBallots = 0, prefrencesEspoused = 0, belowTheLineValid = 0;
		
		for (BallotStream b : ConsoleInteraction.allCurrentBallotStreams()) {
			int currentPrefrences = 0, btlValid = 0;
			while (b.hasMoreBallots()) {
				totalBallots += 1;
				b.nextBallot();
				// Changes to BallotStream removed the functionality to do this - it may be worth it to
				// add back a method that can do this.
				//currentPrefrences += b.currentBallot().numberRanked(b.currentBallot().getAboveLine());
				//currentPrefrences += b.currentBallot().numberRanked(b.currentBallot().getBelowLine());
				//if (b.currentBallot().isValidBTL()) btlValid += 1;
			}
			
			System.err.println(String.format(
					"Ballots in %1$s: %2$d\nPrefrences in %1$s: %3$d\nBTL Valid in %1$s: %4$d",
					b.getElectionName(), b.currentBallotNum(), currentPrefrences, btlValid));
			
			prefrencesEspoused += currentPrefrences;
			belowTheLineValid += btlValid;

		}
		
		System.err.println(String.format(
				"Total ballots: %1$d\nTotal prefrences: %2$d\nTotal BTL: %3$d", 
				totalBallots, prefrencesEspoused, belowTheLineValid));
	}
	
	/**
	 * Runs the console application.
	 * @param args
	 */
	public static void main(String[] args) {
		new ConsoleInteraction();
	}
	
}
