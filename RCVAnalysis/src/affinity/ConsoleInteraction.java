package affinity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

import affinity.ElectionResource.Categories;
import affinity_analysis.MatricesAnalysis;
import affinity_analysis.MultiSpectrumAnalysis;
import aus2016.ElectionResources2016;
import aus2019.ElectionResources2019;
import aus2022.ElectionResources2022;
import nsw2015.ElectionResourcesNSW2015;
import nsw2019.ElectionResourcesNSW2019;
import nyc2021.ElectionResourcesNYC2021;
import nyc2021.ElectionResourcesNYC2021.Race;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.List;

/**
 * Abstract version of the main class for the Australian Elections program.
 * Allows for various different implementations for differing elections to be run using the
 * same console interaction functionalities. This includes categorizing parties together,
 * running spectrums between various different parties, and generating a 
 * matrix for the election.
 * 
 * @author Connor Clark
 * @revised Michael Peeler
 * @version July 8th, 2022
 */
public class ConsoleInteraction {

	// Instance variables
	
	/**
	 * The {@link BallotStream being used by the {@link ConsoleInteraction},
	 * if only using a single {@link BallotStream}. 
	 */
	protected BallotStream 		_info;
	/**
	 * The directory that data will be saved to.
	 */
	private String 				_dest;
	
	
	// Static variables
	
	/**
	 * The subdirectory of {@link ConsoleInteraction#_dest} that 
	 * spectra will be saved to.
	 */
	private final static String 	SPECTRA_SUBDIR 	= "\\spectra";
	/**
	 * The subdirectory of {@link #_dest} that matrices
	 * will be saved to.
	 */
	private final static String 	MATRIX_SUBDIR 	= "\\matrices";
	/**
	 * A {@link DateTimeFormatter} that will be used to make file names / directory
	 * names unique; should not contain "\" or "." in it.
	 */
	private static DateTimeFormatter DTF 			= DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss");
	/** Scanner to get user input. */
	private static Scanner 			SCAN 			= new Scanner(System.in);
	
	
	// Constructors
	
	/**
	 * Invoked to create a {@link ConsoleInteraction} which allows 
	 * the user to select which election they would like to analyze
	 * and where they would like to save the results to. 
	 */
	public ConsoleInteraction() {
		
		boolean cont 	= true;
		boolean pick 	= true;
		boolean mult 	= true;
		boolean loc 	= true;
		
		while (cont) {
			
			// User picks the specific election they would like to run.
			if (pick) {
				mult = 0 == intAnswer("Would you like to run an analysis on multiple datasets (0), or"
						+ " work with a single dataset at a time (1)?", 0, 1);
				if (!mult) this._info = userPicksBallotStreams();
			}
			
			if (loc) {
				// Picks the relative position that the data will be saved to.
				_dest = stringAnswer("What relative path should data be saved to?", "data\\default");
			}
			
			if (mult) {
				
				// Add in the functionality to select which election and which measures and which
				// groupings you want to run.
				runAllBallotStreamsAllMeasures();
			} else {
				userRunsAnElectionAnalysis();			
			}
			// Checks before looping occurs.
			cont = yesNoAnswer("Would you like to keep running tests?");
			if (cont) pick = mult ? true : yesNoAnswer("Would you like to change elections for your next analysis?");

		}
	}
	
	/**
	 * Creates a {@link ConsoleInteraction} that uses the election specified by
	 * the {@link ElectionResource}; then allows user to pick analyses to
	 * run on the election. Save results to the default location specified
	 * by {@link #ConsoleInteraction(BallotStream)}.
	 * @param elecResour {@link ElectionResource} that a {@link BallotStream}
	 * is created with.
	 */
	public ConsoleInteraction(ElectionResource elecResour) {
		this(new BallotStream(elecResour));
	}
	
	/**
	 * Creates a {@link ConsoleInteraction} that uses the elction specified by
	 * the {@link ElectionResource}; then allows user to pick analyses to
	 * run on the election. Saves results to the location specified by the
	 * String parameter. 
	 * @param elecResour {@link ElectionResource} that a {@link BallotStream}
	 * is created with. 
	 * @param dest
	 */
	public ConsoleInteraction(ElectionResource elecResour, String dest) {
		this(new BallotStream(elecResour), dest);
	}

	/**
	 * Creates a {@link ConsoleInteraction} that uses the election specified
	 * by the {@link BallotStream}; then allows user to pick analyses to run 
	 * on the election. Save results to the default location specified here,
	 * currently "data\default" relative to the folder the program is being 
	 * run from.
	 * @param info The {@link BallotStream} that analysis will be run on.
	 */
	public ConsoleInteraction(BallotStream info) {
		this(info, "data\\default");
	}
	
	/**
	 * Creates a {@link ConsoleInteraction} that uses the election specified
	 * by the {@link BallotStream}; then allows user to pick analyses to run 
	 * on the election. Save results to the location specified by the String
	 * argument, relative to the folder the program is run from.
	 * @param info The {@link BallotStream} that analysis will be run on.
	 * @param dest The location data will be saved to.
	 */	
	public ConsoleInteraction(BallotStream info, String dest) {
		this._info = info;
		this._dest = dest;
		userRunsAnElectionAnalysis();
	}
		
	
	// Static methods to access specific information
	
	/**
	 * Returns a list with a {@link BallotStream} for all current elections.
	 * @return A {@link BallotStream} for every election.
	 */
	public static List<BallotStream> allCurrentBallotStreams() {
		List<BallotStream> arr = new ArrayList<>();
		
		arr.add(new BallotStream (new ElectionResourcesNYC2021	(Race.COMPTROLLER_DEM)));
		arr.add(new BallotStream (new ElectionResourcesNYC2021	(Race.MAYORAL_DEM)));
		arr.add(new BallotStream (new ElectionResources2016		()));
		arr.add(new BallotStream (new ElectionResources2019		()));
		arr.add(new BallotStream (new ElectionResources2022		()));
		arr.add(new BallotStream (new ElectionResourcesNSW2019	()));
		arr.add(new BallotStream (new ElectionResourcesNSW2015	()));
		
		return arr;
	}

	
	// Generic user response helper methods; only these methods
	// directly use the scanner.
	
	/** 
	 * Allows the user to be queried for a response that yields a string as 
	 * the answer.
\	 * @param question The question that is being asked.
	 * @param dflt The default answer for response if a blank response is submitted.
	 * @return The user's response.
	 */
	private String stringAnswer(String question, String dflt) {
		System.out.println(question + " Type your answer, or hit enter to use the default value of \"" + dflt +"\".");
		
		String response = SCAN.nextLine();
		return (response.equals("")) ? dflt : response;
	}
	
	/**
	 * Allows the user to be queried for a response that yields a
	 * yes or no answer, returned as a boolean where {@code true} is
	 * "yes" and {@code false} is "no".
	 * @param question Question being asked.
	 * @return boolean answe
	 */
	private boolean yesNoAnswer(String question) {		
		while (true) {
			System.out.println(question + " y/n");

			switch (SCAN.nextLine().toLowerCase()) {
			case "y":	return true;
			case "n":	return false;
			default:
				System.out.println("Ouch! That's invalid. Let's try this again.");		
			}
		}
	}
	
	/**
	 * Takes an integer input between specific bounds, and queries
	 * the user until they give an answer that within those bounds.
	 * Both max and min are inclusive bounds. The question provided
	 * will be queried and the scanner will be used to take input.
	 * If bounds are equal, no bounds will be applied.
	 * @param question Question being asked
	 * @param min Inclusive lower bounds of acceptable answers.
	 * @param max Inclusive upper bounds of acceptable answers.
	 * @return
	 */
	private int intAnswer(String question, int min, int max) {
		do {
			System.out.println(question);
			try {
				int val = SCAN.nextInt();
				SCAN.nextLine(); // Clears the scanner because nextInt cuts the input before the newline character.
				
				if (min != max && (val > max || val < min)) {
					System.out.println("That was an invalid answer! Let's try this again:");
				} else return val;

			} catch (InputMismatchException e) {
				System.out.println("That was rude! I'd like an integer next time.");
			}
		} while (true);
	}	
	
	/**
	 * Takes a question and recieves multiple user inputs, seperated by spaces.
	 * Question will have "Answers should be seperated by spaces" added to
	 * the end of it. 
	 * @param question Question being asked
	 * @return User's answer.
	 */
	private String[] multiPartAnswer(String question) {
		System.out.println(question + " Answers should be seperated by spaces.");
		String[] rtrn = SCAN.nextLine().split(" ");
		return rtrn;
	}
		
	
	// Mass testing methods
	
	/**
	 * Using the list of all {@link BallotStreams}, it runs 
	 * matrix and spectrum measures on all predefined 
	 * categorizations of each election.
	 */
	private static void runAllBallotStreamsAllMeasures() {
		runProvidedBallotStreamsAllMeasures(allCurrentBallotStreams());
	}

	/**
	 * Takes the provided list of {@link BallotStream}s and runs
	 * matrix and spectrum measures on all predefined 
	 * categorizations of each election.
	 * @param arr The list of {@link BallotStreams}
	 */
	private static void runProvidedBallotStreamsAllMeasures(List<BallotStream> arr) {
		runProvidedBallotStreamsWithProvidedCatsAllMeasures(arr, new ArrayList<Categories>(Arrays.asList(Categories.values())));
	}
	
	/**
	 * Takes the provided list of {@link BallotStream} and runs
	 * matrix and spectrum measures on the predefined 
	 * categorizations provided in the list of {@link Categories}.
	 * @param arr Elections this will be run on.
	 * @param cats Categories this will be run using.
	 */
	private static void runProvidedBallotStreamsWithProvidedCatsAllMeasures(
			List<BallotStream> arr, List<Categories> cats) {
		runProvidedBallotStreamsWithProvidedCatsSelectedMeasures(arr, cats, true, true);
	}
	
	/**
	 * Takes the provided list of {@link BallotStream} and runs
	 * matrix and/or spectrum measures, as specified, on the predefined 
	 * categorizations provided in the list of {@link Categories}.
	 * @param arr Elections this will be run on.
	 * @param cats Categories this will be run using.
	 * @param makeSpectra Whether spectrum should be made.
	 * @param makeMatrices Whether matrices should be made.
	 */
	private static void runProvidedBallotStreamsWithProvidedCatsSelectedMeasures(
			List<BallotStream> arr, List<Categories> cats,
			boolean makeSpectra, boolean makeMatrices) {
		for (BallotStream stream : arr) {
			for (Categories cat : cats) {
				try {
					stream.predefinedCategorizations(cat, true, true);					
				} catch (ElectionResource.CategorizationException e) { 
					continue; 
				}
				
				System.err.println("Processing " + stream.getElectionName() + ": " + cat.toTitle() + " Categorization");
				spectrumAndMatrixMaker(stream, cat.toString().toLowerCase(), makeSpectra, makeMatrices);
				stream.restart();					
			}
		}
	}
	
	/**
	 * Runs matrices and spectra on the {@link BallotStream}, as specified,
	 * and saves to the {@link BallotStream}'s {@link ElectionResource}'s
	 * {@link ElectionResource#getOutputLocation()}, with whatever
	 * specified "extra" is provided.
	 * @param stream Stream that data will be drawn from.
	 * @param extra Additional name to add to save location.
	 * @param makeSpectra Whether spectra should be made.
	 * @param makeMatrices Whether matrices should be made.
	 */
	private static void spectrumAndMatrixMaker(BallotStream stream, String extra, boolean makeSpectra, boolean makeMatrices) {
		
		File folder = new File(stream.getOutputLocation() + "\\" + extra + DTF.format(LocalDateTime.now()));
		
		if (makeMatrices || makeSpectra) folder.mkdirs();

		if (makeMatrices) {

			System.err.println("|---> Making Matrices");
		
			Matrices mtrx = new Matrices(stream);
			mtrx.makeMatricies();
			mtrx.saveMatrices(folder.getPath() + MATRIX_SUBDIR + "\\");
		
			MatricesAnalysis ma = new MatricesAnalysis(mtrx);
			ma.allAnalyses(folder.getPath() + "\\matrices analysis\\results.csv");
		
			stream.restart();
		}
		
		if (makeSpectra) {
			System.err.println("|---> Making Spectra");
		
			SpectraMaker m = new SpectraMaker(stream);
			m.makeAndSaveAllSpectrumPairs(folder.getPath() + SPECTRA_SUBDIR);
	
			MultiSpectrumAnalysis msa = new MultiSpectrumAnalysis(m.getSpectra(), folder.getPath() + "\\spectrum analysis");
			msa.allAnalyses();
		}
	}
	
	
	// Methods to get a user to select election parameters
	
	/**
	 * Allows the user to select which election they would
	 * like to run a singular election analysis on.
	 * @param scan Scanner for input
	 * @return The {@link BallotStream} for the election
	 * they chose.
	 */
	private BallotStream userPicksBallotStreams() {
		List<BallotStream> streamOpts = allCurrentBallotStreams();
		
		StringBuilder question = new StringBuilder("Pick which election you would like to choose");
		for (int i = 0; i < streamOpts.size(); i++) {
			if (i == 0) question.append(": ");
			else if (i == streamOpts.size() - 1) question.append(" or ");
			else question.append(", ");
			question.append(streamOpts.get(i).getElectionName() + " (" + i + ")");
		}
		question.append(".");
				
		int choice = intAnswer(question.toString(), 0, streamOpts.size() - 1);
		return streamOpts.get(choice);
		
	}
	
	/**
	 * Have user select the party bounds of a single spectrum
	 * analysis
	 * @param scan Scanner for user input.
	 */
	private void runManualSelection() {

		// create variables
		int lowBound;
		int highBound;
		
		String lowPartyName;
		String highPartyName;

		// Create a list of the secondary options, format it, and print it
		if (_info.usesCategoriesSecondary()) 	_info.printFormattedCategories();
		else 									_info.printFormattedParties();
		
		
		lowBound = intAnswer("\nUsing the indices provided, please select the lower party bounds:", 0, _info.getSecNumOptions() - 1);
		lowPartyName = _info.usesCategoriesSecondary() ? _info.indexCategoryToName(lowBound) : _info.indexPartyToName(lowBound);
			System.out.println(lowPartyName + " selected.");
		
		highBound = intAnswer("\nPlease select the upper party bounds: ", lowBound + 1, _info.getSecNumOptions() - 1);
		highPartyName = _info.usesCategoriesSecondary() ? _info.indexCategoryToName(highBound) : _info.indexPartyToName(highBound);
			System.out.println(highPartyName + " selected.");

		// Confirmation
			System.out.println("\n" + "Now calculating for " + lowPartyName + " as the lower bound and "
				+ highPartyName + " as the upper bound.");

		SpectraMaker test = new SpectraMaker(_info);
					 test.addPartyPairs(lowBound, highBound);
					 test.makeAndSaveCurrentSpectrumPairs(_dest + SPECTRA_SUBDIR);
					 test.printSpectra();
		
		System.out.println("Standard Deviation: " + test.getStandardDeviation(0));
		System.out.println("Alt Standard Deviation: " + test.getAltStandardDeviation(0));
		System.out.println("Finished!");
		
	}
 
	/**
	 * Automatically run through every bounding pair possible, between the range provided.
	 * Also calculates and saves the best standard deviation.
	 * @throws IOException
	 */
	private void runAutoSelection() {
		while (true) {
			if (_info.usesCategoriesSecondary()) _info.printFormattedCategories(); 
			else 								_info.printFormattedParties();
	
			int start = intAnswer("\nStarting Position, or -1 to terminate:", -1, _info.getSecNumOptions() - 1);
			int end = intAnswer("Ending Position, or -1 to terminate (this value is exclusive):", -1, _info.getSecNumOptions());
			
			if (start == -1 || end == -1) {
				System.out.println("Terminating auto selection.");
				return;
			} else if (end <= start) {
				// Loops
				System.out.println("One of those inputs is illegal! Try again.");
			} else {
				SpectraMaker spects = new SpectraMaker(_info);
				spects.makeAndSaveSpectrumPairsBetween(start, end, _dest + SPECTRA_SUBDIR);
				bestStandardDeviations(spects);
				
				System.out.println("Finished!");
				// Escapes loop when successfully completed.
				return;
			}
		}
	}
	
	/**
	 * Finds and saves to file the spectrum with the highest standard deviation and alternative
	 * standard deviation.
	 * @param spects
	 */
	private void bestStandardDeviations(SpectraMaker spects) {
		Spectrum bestNorm = spects.bestStandardDeviation();
		Spectrum bestAlt = spects.bestAltStandardDeviation();
		
		String standDevPair = String.format("%02d-%02d", bestNorm.getLowerIndex(), bestNorm.getUpperIndex());
		String altStandDevPair = String.format("%02d-%02d", bestAlt.getLowerIndex(), bestAlt.getUpperIndex());
		
		Double normDev = bestNorm.calcStandDev();
		Double altDev = bestAlt.calcAltStandDev();
		
		System.out.println("Best Standard Deviation: Pairing " + standDevPair + " " + normDev);
		System.out.println("Best Alt Standard Deviation: Pairing " + altStandDevPair + " " + altDev);
		
		saveBestStandardDev(normDev, standDevPair, altDev, altStandDevPair);
	}

	/**
	 * Save the best standard deviation from a test to an output file.
	 */
	private void saveBestStandardDev(double stdDev, String stdDevParty, double altDev, String altDevParty) {
		File file = new File(_dest + SPECTRA_SUBDIR + "\\BestStandDev.txt");
		try {
			file.createNewFile();
			PrintWriter writer = new PrintWriter(new FileWriter(file, false));
			writer.println("Best Standard Deviation: ");
			writer.printf("%.4f\n", stdDev);
			writer.println("Case: " + stdDevParty);
			writer.println("\n" + "Best Non-Extreme Standard Deviation: ");
			writer.printf("%.4f\n", altDev);
			writer.println("Case: " + altDevParty);
			writer.close();
		} catch (IOException e) {
			System.out.println("Unable to create or write to " + file.getPath());
		}
	}

	/**
	 * Allows the user to categorize multiple parties together, if they so desire.
	 * In an election with parties A, B, C, D, and E, it may be valuable to see if 
	 * parties A, B, and C tend to vote for each other more than they tend to vote for 
	 * parties D and E. This allows us to assign A, B, and C into category 1, and D and E into
	 * category two, then compare how these two categories interact.
	 *
	 * <br>
	 * In an election with parties A, B, C, D, and E, it may be valuable to see if 
	 * parties A, B, and C tend to vote for each other more than they tend to vote for 
	 * parties D and E. This allows us to assign A, B, and C into category 1, and D and E into
	 * category 2, then compare how these two categories interact.
	 * <br>
	 * These categories can be compared in three ways:	<br>
	 * 	- Compare primary vote for a category against secondary votes for other parties<br>
	 * 		Party __| A | B | C | D | E<br>
	 * 		Ctgry 1 | X | X | X | X | X<br>
	 * 		_____ 2 | X | X | X | X | X<br>
	 * <br>
	 * 								or<br>
	 * <br>
	 * 					( )< Ctgry 2	Ctgry 1 >( )<br>
	 * 		Party A -----V------------------------V----- Party B<br>
	 * 											  <br>
	 * 	- Compare primary vote for a party against secondary votes for categories<br>
	 * 		Ctgry __| 1 | 2<br>
	 * 		Party A | X | X<br>
	 * 		_____ B | X | X<br>
	 * 		_____ C | X | X<br>
	 * 		_____ D | X | X<br>
	 * 		_____ E | X | X<br>
	 * 								or<br>
	 * <br>
	 * 				       ( )< Party A    Party B >( )<br>
	 * 		Category 1 -----V------------------------V----- Category 2<br>
	 * <br>
	 * 	- Compare primary vote for a category against secondary votes for other categories<br>
	 * 		Ctgry __| 1 | 2<br>
	 * 		Ctgry 1 | X | X<br>
	 * 		_____ 2 | X | X<br>
	 * <br>
	 * 								or<br>
	 * <br>
	 * 					   ( )< Ctgry 2	   Ctgry 1 >( )<br>
	 * 		Category 1 -----V------------------------V----- Category 2<br>
	 * <br>
	 * 	- (There is, of course, the fourth option of comparing primary vote for a party
	 * 		against secondary vote for a party, but this is the default that is used
	 * 		if not categorization is used.)<br>
	 * <br>
	 * 
	 * @param scan The console input scanner where input is being read from; if this is closed,
	 * then System.in is closed and input cannot be taken, so it should not be closed.
	 * @param useCategoriesPrimary If set to true, analysis will use the category of the primary vote;
	 * @param useCategoriesSecondary If set to true, analysis will use the category of the secondary vote.	
	 */
	private void userSelectsCategories(boolean useCategoriesPrimary, boolean useCategoriesSecondary) 
		throws ElectionResource.CategorizationException {
		
		// If there is currently a categorization
		if (_info.usesCategoriesSecondary()) {
			System.out.println();
			if (yesNoAnswer("Would you like to maintain the current categorization?")) {
				return;
			}
			else {
				_info.stopCategories();
			}
		}
		
		int count = intAnswer("How many categories would you like to make? Type number, or -1 to terminate.", -1, 1000000);
		if (count == -1) return;
		
		ArrayList<String> categories = new ArrayList<>();
		HashMap<String, String> categorization = new HashMap<>();
		
		_info.printFormattedParties();
		
		for (int i = 0; i < count; i++) {
		
			categories.add(stringAnswer("What is this category's name?", "Category " + (i + 1)));
			
			String[] categoryMembers = multiPartAnswer("Type the indicies of all parties in this category.");
			
			for (String memb : categoryMembers) {
				try {
					memb = _info.indexPartyToName(Integer.parseInt(memb));	
					if (categorization.containsKey(memb)) {
						String q = memb + " is already in the " + categorization.get(memb) + 
								" category. Should it be reassigned to " + categories.get(i) + "?";
						boolean reassign = yesNoAnswer(q);
						
						if (reassign) 	categorization.put(memb, categories.get(i));
					} 
					else categorization.put(memb, categories.get(i));
				} catch (NumberFormatException e) {}
			}
		}
		
		for (int i = 0; i < categories.size(); i++) System.out.println(i + ": " + categories.get(i));
		
		for (String p : _info.getUniquePartyList()) {
			if (!categorization.containsKey(p)) {
				int ind = intAnswer("Which category should " + p + " be in?", 0, categories.size() - 1);
				categorization.put(p,  categories.get(ind));
			}
		}
		
		_info.categorizeParties(categories, categorization, useCategoriesPrimary, useCategoriesSecondary);
	}
	
	/**
	 * Printout about whether or not a categorization succeeded; specially made
	 * for each of the switch cases.
	 * @param cs The results of the call to 
	 * {@link #userSelectsCategories(boolean, boolean)} or 
	 * {@link BallotStream#predefinedCategorizations(Categories, boolean, boolean)}.
	 * @param categorizationName The name of the categorization attempted.
	 */
	private void categorizationResultPrintout(ElectionResource.CategorizationException ce,
			String categorizationName) {
		if (ce == null) {
			System.err.println(categorizationName + " successfully implemented.");
		} else if (ce instanceof ElectionResource.NotDefinedException) {
			System.err.println(categorizationName + " not defined. Continuing with uncategorized election.");
		} else if (ce instanceof ElectionResource.MissingPartyException) {
			System.err.println(categorizationName + " failed to implement; at least one party was missing. Continuing with uncategorized election.");
		} else if (ce instanceof ElectionResource.StreamInProgressException) {
			System.err.println(categorizationName + " categorization is not defined for this type of ElectionResource. Continuing with uncategorized election.");
		}
	}
	
	/**
	 * User selects parameters for an election analysis, including if it
	 * will be matrices or spectra, if the parties will be grouped, and
	 * how bounds will be picked.
	 */
	private void userRunsAnElectionAnalysis() {
		
		while (true) {
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss");
	
			ArrayList<Categories> cats = new ArrayList<>(Arrays.asList(Categories.values()));
			StringBuilder question = new StringBuilder("Would you like to run ");
			for (int i = 0; i < cats.size(); i++) {
				question.append(cats.get(i).toString() + " (" + i + "), ");
			}
			question.append("or user selected categories (" + cats.size() + ")?");
			
			int categories = intAnswer(question.toString(), 0, cats.size());
			
			if (categories == cats.size()) {
				
				String q = "Would you categories used primary and secondary (0), categories used secondary (1), or the categories used primary (2)?";
				int compare = intAnswer(q, 0, 2);
				
				ElectionResource.CategorizationException excep = null;
				try {
					userSelectsCategories(compare != 1, compare != 2);
				} catch (ElectionResource.CategorizationException e) {
					excep = e;
				}
				categorizationResultPrintout(excep, "User defined categories");
				
			} else if (categories > 0 && categories < cats.size()) {
				String q = "Would you categories used primary and secondary (0), categories used secondary (1), or the categories used primary (2)?";
				int compare = intAnswer(q, 0, 2);
				
				ElectionResource.CategorizationException excep = null;
				try {
					_info.predefinedCategorizations(cats.get(categories), compare != 1, compare != 2);
				} catch (ElectionResource.CategorizationException e) {
					excep = e;
				}
				
				categorizationResultPrintout(excep,	cats.get(categories).toString() + " Categories");
			} else {
				ElectionResource.CategorizationException excep = null;
				try {
					_info.predefinedCategorizations(Categories.UNCATEGORIZED, true, true);
				} catch (ElectionResource.CategorizationException e) {
					excep = e;
				} 
				
				categorizationResultPrintout(excep, "Uncategorization");
			}
			
			if (intAnswer("Would you like to run spectrum (0) or matrix (1)?", 0, 1) == 0) {
				int answer = intAnswer("Would you like to run manual (0) or automatic (1) selection?", 0, 1);
				
				if (answer == 0) 	runManualSelection();
				else				runAutoSelection();
				
			} else {
	
				Matrices matrix = new Matrices(_info, true, true, false, true, true);
				matrix.makeMatricies();
				String f 			= new File(_dest).getPath();
				matrix.saveMatrices(f + MATRIX_SUBDIR + "\\" + dtf.format(LocalDateTime.now()) + "\\");	
				
			}
			
			
			if (yesNoAnswer("Would you like to run more tests on this dataset?")) {
				_info.restart();
			} else {
				return;				
			}
		}
	}
}
