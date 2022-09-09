package aus2016;

import affinity.ConsoleInteraction;

import affinity.BallotStream;

public class Main {

	/**
	 * Main method
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) {
		new ConsoleInteraction(new BallotStream (new ElectionResources2016()), "data\\2016\\output\\user-generated");
	}
}
