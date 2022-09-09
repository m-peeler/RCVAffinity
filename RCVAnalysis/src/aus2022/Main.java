package aus2022;

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
		new ConsoleInteraction(new BallotStream (new ElectionResources2022()), "data\\2022\\output\\user generated");
	}
}
