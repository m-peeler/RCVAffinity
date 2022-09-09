package affinity_analysis;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;
import java.util.ArrayList;

import affinity.Spectrum;
import affinity.SpectrumParty;

/**
 * Program to read output files from my Austrialia Spectrum program
 * @author cclark5
 *
 */
public class MultiSpectrumAnalysis {
	
	ArrayList<Spectrum> _arr;
	
	File _folder;
	
	private class absoluteComparator implements Comparator<DoubleIndexTable<Spectrum, Double>.TwoIndexEntry> {
		public int compare(DoubleIndexTable<Spectrum, Double>.TwoIndexEntry d, DoubleIndexTable<Spectrum, Double>.TwoIndexEntry e) {
			if (Math.abs(d.getEntry()) > Math.abs(e.getEntry())) return 1;
			else if (Math.abs(d.getEntry()) < Math.abs(e.getEntry())) return -1;
			else return 0;
		}
	}
	
	public MultiSpectrumAnalysis(String dataFolder, String outputFolder) {
		
		_folder = new File (outputFolder);
		_arr = this.collectSpectraFromFiles(dataFolder);
		
	}
	
	public MultiSpectrumAnalysis(ArrayList<Spectrum> spects, String outputFolder) {
		
		_folder = new File(outputFolder);
		_arr = spects;
		
	}
	
	public void highestAverageDistance() {
		int numArrays =_arr.size();
		int numParties = _arr.get(0).getSpectrumParties().size();
		
		String[] spectrumNames = new String[numArrays];
		
		double[][] distances = new double[numArrays][numArrays];
		double distance;
		double counter;
		double max = 0;
		int[] indexes = new int[2];
		
		for (int i = 0; i < numArrays; i++) {
			Spectrum primSpectrum = _arr.get(i);
			
			spectrumNames[i] = primSpectrum.getLowerName() + "-" + primSpectrum.getUpperName();
			
			for (int j = 0; j < numArrays; j++) {
				Spectrum secSpectrum = _arr.get(j);
				distance = 0;
				counter = 0;
				for (int k = 0; k < numParties; k++) {
					double firstPrimVal = primSpectrum.getPartyAtIndex(k).getSpectrumVals();
					double firstSecVal = secSpectrum.getPartyAtIndex(k).getSpectrumVals();
					
					for (int l = 0; l < numParties; l++) {
						double secPrimValue = primSpectrum.getPartyAtIndex(l).getSpectrumVals();
						double secSecValue = secSpectrum.getPartyAtIndex(l).getSpectrumVals();
						
						if (firstPrimVal == -1 || firstSecVal == -1 || secPrimValue == -1 || secSecValue == -1) {
							continue;
						}
						counter += 1;
						
						distance += Math.sqrt(Math.pow(firstPrimVal - secPrimValue, 2) + Math.pow(firstSecVal - secSecValue, 2));
						
					}
					
				}
				distances[i][j] = distance / counter;
				if (distances[i][j] > max) {
					max = distances[i][j];
					indexes[0] = i;
					indexes[1] = j;
				}
			}
		}
		
		String bestString = "Highest Average Distance is between #" + indexes[0] + ", " + spectrumNames[indexes[0]] +
				" and #" + indexes[1] + ", " + spectrumNames[indexes[1]] + ", which was " + max + ".";
		
		File a = new File(_folder.getPath() + "\\highest distance.csv");
		File b = new File(_folder.getPath() + "\\best distance");
		PrintWriter p;
		PrintWriter pb;

		try {
			a.createNewFile();
			p = new PrintWriter(new FileWriter(a));
			b.createNewFile();
			pb = new PrintWriter(new FileWriter(b));
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		pb.print(bestString);
		
		p.print(",");
		for (int i = 0; i < numArrays; i ++) {
			p.print(spectrumNames[i] + ",");
		}
		p.print("\n");
		
		for (int i = 0; i < numArrays; i++) {
			p.print(spectrumNames[i] + ",");
			for (int j = 0; j < numArrays; j++) {
				p.print(distances[i][j] + ",");
			}
			p.print("\n");
		}
		
		p.close();
		pb.close();
		
	}
	
	public void allAnalyses() {
		saveBestStandardDev();
		smallestCorrel();
		highestAverageDistance();
		condenseSpectraToCSV();
	}
	
	public void smallestCorrel() { smallestCorrel(0.25); }
	
	public void smallestCorrel(double threshold) {
		DoubleIndexTable<Spectrum, Double> correls = new DoubleIndexTable<>();
		
		ArrayList<Spectrum> important = new ArrayList<>();

		if (_arr.size() < 50) for (Spectrum a : _arr) important.add(a);
		else {
			for (int i = 0; i < _arr.size(); i++) {
				if (_arr.get(i).calcStandDev() > threshold) {
					important.add(_arr.get(i));
				}
			}
		}
		
		if (important.size() == 0 && _arr.size() > 0) { 
			smallestCorrel(threshold - 0.05);
			return;
		}
	
		for (int i = 0; i < important.size() - 1; i++) {
			for (int j = i + 1; j < important.size(); j++) {
				correls.put(important.get(i), important.get(j), important.get(i).correlationWith(important.get(j)));
			}
		}
		
		ArrayList<DoubleIndexTable<Spectrum, Double>.TwoIndexEntry> correlList = correls.entrySet();
				
		Collections.sort(correlList, new absoluteComparator());
		
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss");
		
		File file = new File(_folder.getPath() + "\\smallest correlation " + dtf.format(LocalDateTime.now()) + ".csv");
		PrintWriter pw = null;
		
		try {
			file.getParentFile().mkdirs();
			file.createNewFile();
			pw = new PrintWriter(new FileWriter(file));

		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (correlList.size() == 0) {
			System.out.println("Uh-oh");
		}
		
		StringBuilder sb = new StringBuilder();
		for (SpectrumParty sp : correlList.get(0).getIndOne().getSpectrumParties()) sb.append(sp.getPartyName() + ","); 
		pw.println(sb.toString());
		
		for (int i = 0; correlList.get(i).getEntry() < 0.1; i++) {
			DoubleIndexTable<Spectrum, Double>.TwoIndexEntry correl = correlList.get(i);
			Spectrum spec1 = correl.getIndOne();
			Spectrum spec2 = correl.getIndTwo();
			StringBuilder s = new StringBuilder();
			s.append(String.format("%1$03d-%2$03d", spec1.getLowerIndex(), spec1.getUpperIndex()) + "," + spec1.getLowerName() + "," + spec1.getUpperName() + ",");
			s.append(String.format("%1$03d-%2$03d", spec2.getLowerIndex(), spec2.getUpperIndex()) + "," + spec2.getLowerName() + "," + spec2.getUpperName() + ",");
			s.append(String.format("%1.4f,", correlList.get(i).getEntry()));
			s.append(spec1.compatable(spec2));
			s.append("\n");
			for (SpectrumParty sp : spec1.getSpectrumParties()) s.append(String.format("%.4f,", sp.getSpectrumVals()));
			s.append("\n");
			for (SpectrumParty sp : spec2.getSpectrumParties()) s.append(String.format("%.4f,", sp.getSpectrumVals()));
			pw.println(s.toString());
		}
			
		pw.close();
	}
	
	
	private String safeForRegex(String unsafe) {
		String safe = unsafe.replace("\\", "\\\\")	
				.replace(")", "\\)").replace("(", "\\(")
				.replace("|", "\\|").replace("*", "\\*")
				.replace("+", "\\+").replace(".", "\\.")
				.replace("$", "\\$").replace("^","\\^")
				.replace("%", "\\%");
		return safe;
	}
	
	private ArrayList<Spectrum> collectSpectraFromFiles(String inputFolder) {
		Scanner current;
		ArrayList<Spectrum> spectra = new ArrayList<>();

		String[] fileList = new File(inputFolder).list();
		Arrays.parallelSort(fileList);

		ArrayList<String> parties = new ArrayList<>();
		int fileIndex = 0;
		if (fileList[fileIndex].equals("BestStandDev.txt")) fileIndex++;
		try {
			current = new Scanner(new FileInputStream(inputFolder + '/' + fileList[fileIndex]));
			String searchFor = " SpectrumVals: ";
			while (current.hasNextLine()) {
				String line = current.nextLine();
				if (line.contains(searchFor)) parties.add(line.replace(searchFor, ""));
			}
		} catch (IOException e) {
			System.out.println("Error scanning " + fileList[fileIndex]);
		}
		
		
		for (int i = 0; i < fileList.length; i++) {
			
			if (fileList[i].equals("BestStandDev.txt")) 
				continue;
			
			try {
				current = new Scanner(new FileInputStream(inputFolder + '/' + fileList[i]));
			} catch (IOException e) {
				continue;
			}
	
			// Gets the index from the file name because index uses index in
			// secondary list while parties list will be primary category list.
			String inds = fileList[i].replace(".txt", "");
			
			String partyOne = null;
			String partyTwo = null;
			int partyIndOne = Integer.parseInt(inds.substring(0, inds.indexOf("-")));
			int partyIndTwo = Integer.parseInt(inds.substring(inds.indexOf("-") + 1));
			String partyCur = "";

			
			if (current.hasNextLine()) {
				String[] partyList = current.nextLine().replace("Lower bound: ", "").split(", Upper Bound: ");
				partyOne = partyList[0];
				partyTwo = partyList[1];				
			}
			
			Spectrum spect = new Spectrum(partyOne, partyIndOne, partyTwo, partyIndTwo, parties);
			
			int total = 0, lowerPref = 0, upperPref = 0, lowerTotal = 0, upperTotal = 0;
			double spectrum = 0;
			
			while (current.hasNextLine()) {
				
				String test = current.nextLine();
				if (test.contains(" SpectrumVals: ")) {
					if (!partyCur.equals("")) {
						SpectrumParty old = spect.getPartyAtIndex(parties.indexOf(partyCur));
						old.setNeitherPreferred(total - lowerPref - upperPref);
						old.setLowerOnly(lowerTotal - lowerPref);
						old.setUpperOnly(upperTotal - upperPref);
						old.setLowerPreferred(lowerPref);
						old.setUpperPreferred(upperPref);

						if (Math.abs(old.getSpectrumVals() - spectrum) > 0.0001) {
							System.err.println(partyCur + ": New spectrum value of " + old.getSpectrumVals() + " not equivalent to read value of " + spectrum);
						}
						
						total = 0; lowerPref = 0; upperPref = 0; lowerTotal = 0; upperTotal = 0;
						spectrum = 0;
					}
					partyCur = test.replace(" SpectrumVals: ", "");
				}
				if (test.contains("Ballot Size: ")) {
					total = Integer.parseInt(test.replace("Ballot Size: ", ""));
				}
				if (test.contains("Total Votes: vs ")) {
					test = test.replace("Total Votes: vs " + partyOne + ": ", "");
					String[] totals = test.split(safeForRegex(" " + partyTwo + ": "));
					lowerTotal = Integer.parseInt(totals[0]);
					upperTotal = Integer.parseInt(totals[1]);
				}
				if (test.contains("Total Ranked Above: vs ")) {
					test = test.replace("Total Ranked Above: vs " + partyOne + ": ", "");
					String[] totals = test.split(safeForRegex(" " + partyTwo + ": "));
					lowerPref = Integer.parseInt(totals[0]);
					upperPref = Integer.parseInt(totals[1]);
				}
				if (test.contains("Ur/(Ur+Lr) = ")) {
					test = test.replace("Ur/(Ur+Lr) = ", "");
					spectrum = Double.parseDouble(test);
				}
			}
			
			spectra.add(spect);

		}

		return spectra;
	}
	
	public void condenseSpectraToCSV() {
		PrintWriter outputWriter = null;
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss");
		
		File output = new File(_folder.getPath() + "\\collected stand dev " + dtf.format(LocalDateTime.now()) + ".csv");

		try {
			// create file
			output.getParentFile().mkdirs();
			output.createNewFile();
			outputWriter = new PrintWriter(new FileWriter(output));
			outputWriter.print("Case,First Party,Second Party,");

		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		for (int i = 0; i < _arr.get(0).getSpectrumParties().size(); i++) {
			outputWriter.print(_arr.get(0).getPartyAtIndex(i).getPartyName() + ",");
		}
		outputWriter.print("Standard Deviation,Non-Extreme Standard Deviation,Total Usable Ballots\n");
		
		for (Spectrum spec : _arr) {
			outputWriter.print(String.format("%1$03d-%2$03d", spec.getLowerIndex(), spec.getUpperIndex()));
			outputWriter.print("," + spec.getLowerName() + "," + spec.getUpperName() + ",");
			int totalUsableBallots = 0;
			
			for (SpectrumParty sp : spec.getSpectrumParties()) {
				outputWriter.print(sp.getSpectrumVals() + ",");
				totalUsableBallots += sp.getTotalVotes();
			}
			
			outputWriter.print(spec.calcStandDev() + "," + spec.calcAltStandDev() + "," + totalUsableBallots + "\n");
		}
		
		outputWriter.close();
	}
	
	public void saveBestStandardDev() {
		Spectrum bestSpec = null;
		Spectrum bestAltSpec = null;
		for (Spectrum s : _arr) {
			if (bestSpec == null) {
				bestSpec = s;
			} else if (s.calcStandDev() > bestSpec.calcStandDev()) {
				bestSpec = s;
			}
			
			if (bestAltSpec == null) {
				bestAltSpec = s;
			} else if (s.calcAltStandDev() > bestAltSpec.calcAltStandDev()) {
				bestAltSpec = s;
			}
		}
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss");

		File output = new File(_folder.getPath() + "\\best stand dev " + dtf.format(LocalDateTime.now()) + ".txt");
		PrintWriter pw = null;
		
		try {
			output.getParentFile().mkdirs();
			output.createNewFile();
			pw = new PrintWriter(new FileWriter(output));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		pw.print("Best standard deviation: " + bestSpec.calcStandDev()  + "\t" + String.format("%1$03d-%2$03d", bestSpec.getLowerIndex(), bestSpec.getUpperIndex()));
		pw.print("\t");
		pw.print("Best non-extreme standard deviation: " + bestAltSpec.calcStandDev() + "\t" + String.format("%1$03d-%2$03d", bestAltSpec.getLowerIndex(), bestAltSpec.getUpperIndex()));
		
		pw.close();
	}

}
