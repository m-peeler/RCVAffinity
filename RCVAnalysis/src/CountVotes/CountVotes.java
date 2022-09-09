package CountVotes;

import java.util.Scanner;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Backup class for a simplistic count and winner calculation for the NYC
 * Mayoral primary. Not particularly important unless you'd like to confirm
 * the results of the election, or potentially first preference vote totals.
 * 
 * Completely uncommented because it is not important and is a straightforward
 * implementation of the RCV algorithm.
 * 
 * @author Michael Peeler
 * @version July 30th, 2022
 *
 */
public class CountVotes {

	Map<String, ArrayList<String[]>> _voteTotals;
	
	public CountVotes() {
		_voteTotals = new HashMap<String, ArrayList<String[]>>();
	}
	
	public void removeLowestCandidate() {
		int minValue = 0;
		String minKey = null;
		for (String key : _voteTotals.keySet()) {
			if (key.equals("undervote") || key.equals("overvote")) {
				continue;
			}
			if (minKey == null) {
				minKey = key;
				minValue = _voteTotals.get(key).size();
			} else {
				if (_voteTotals.get(key).size() < minValue) {
					minValue = _voteTotals.get(key).size();
					minKey = key;
				}
			}
		}
		
		ArrayList<String[]> removedBallots = _voteTotals.remove(minKey);
		boolean moved = false;
		for (String[] bal : removedBallots) {
			moved = false;
			for (String vote : bal) {
				if (_voteTotals.containsKey(vote)) {
					_voteTotals.get(vote).add(bal);
					moved = true;
					break;
				}
			}
			if (!moved) {
				_voteTotals.get("undervote").add(bal);
			}
		}
	}
	
	public void addVote(String[] ballot) {
		int ind = 0;
		String party;
		if (ind >= ballot.length) party = "undervote";
		else party = ballot[ind];
		if (!_voteTotals.containsKey(party)) {
			_voteTotals.put(party, new ArrayList<String[]>());
		}
		_voteTotals.get(party).add(ballot);
	}
	
	public int getTotal() {
		int total = 0;
		for (String key : _voteTotals.keySet()) {
			if (!key.equals("undervote") && !key.equals("overvote")) {
				total += _voteTotals.get(key).size();
			}
		}
		return total;
	}
	
	public void printCurrentTotals() {
		
		float total = getTotal();
		System.out.println("There are " + total + " ballots that remain active.");
		
		for (String key : _voteTotals.keySet()) {
			float votes = _voteTotals.get(key).size();
			System.out.println(key + " has " + (int) votes + ", which is " + (100 * votes / total) + "% of remaining.");
		}
		assert(false);
	}
	
	public static void main(String[] args) {
		
		CountVotes count = new CountVotes();
	
		File file = new File("data//NYC2021//dataNYC");
		for (String fl : file.list()) {
			try {
				Scanner scan = new Scanner(new FileInputStream(file.getAbsoluteFile() + "//" + fl));
				String[] firstLine = scan.nextLine().split(",");
				ArrayList<Integer> indices = new ArrayList<Integer>();
				String position = "DEM Mayor";
				String[] positionNames = new String[5];
				for (int i = 0; i < 5; i++) {
					positionNames[i] = position + " Choice " + i + " of";
				}

				
				for (int i = 0; i < positionNames.length; i++) {
					for (int j = 0; j < firstLine.length; j++) {
						if (firstLine[j].contains(positionNames[i])) {
							indices.add(j);
						}
					}
					assert(indices.size() == i + 1);
				}
				
				System.err.println("In process of reading: " + fl);
				while (indices.size() > 0 && scan.hasNextLine()) {
					String[] ballot = scan.nextLine().split(",");
					String[] mayorBallot = new String[indices.size()];
					for (int i = 0; i < indices.size(); i++) {
						mayorBallot[i] = ballot[indices.get(i)];
					}
					count.addVote(mayorBallot);
				}
			} catch (FileNotFoundException e) {
				System.out.println(fl + " not found.");
			}

		}
		System.out.println(count._voteTotals.size());

		count.printCurrentTotals();		
	}
}
