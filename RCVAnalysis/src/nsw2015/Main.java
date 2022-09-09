package nsw2015;

import affinity.ConsoleInteraction;
import affinity.BallotStream;

public class Main {
	
	/**
	 * Main method
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) {
		new ConsoleInteraction(new BallotStream (new ElectionResourcesNSW2015()), "data\\NSW2015\\output\\user generated");
	}
}
