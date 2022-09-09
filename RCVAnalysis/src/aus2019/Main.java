package aus2019;

import java.io.IOException;

import affinity.ConsoleInteraction;
import affinity.BallotStream;

public class Main {
	
	/**
	 * Main method
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		new ConsoleInteraction(new BallotStream (new ElectionResources2019()), "data\\2019\\output\\user generated");
	}
}
