package nsw2019;

import affinity.ConsoleInteraction;
import affinity.BallotStream;

public class Main {
	
	/**
	 * Main method
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) {
		new ConsoleInteraction(new BallotStream (new ElectionResourcesNSW2019()), "data\\NSW2019\\output\\user generated");
	}
}
