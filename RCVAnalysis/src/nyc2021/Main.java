package nyc2021;

import affinity.ConsoleInteraction;
import nyc2021.ElectionResourcesNYC2021.Race;
import affinity.BallotStream;

public class Main {
	
	/**
	 * Main method
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) {
		new ConsoleInteraction(new BallotStream (new ElectionResourcesNYC2021(Race.MAYORAL_DEM)), "data\\NYC2021\\output\\user generated");
	}
}

