package split_algorithm;

import java.util.Arrays;
import java.util.Scanner;

/**
 * Generates the input variables for a spectrum when running all cases for n parties.
 * @author cclark5
 *
 */
public class Main {
	
	public static double[] splitAlgorithm() {
		Scanner scan = new Scanner(System.in);

		System.out.println("Num of Parties: ");
		int n = scan.nextInt();
		System.out.println("Number of Machines: ");
		int k = scan.nextInt();

		double[] answers = new double[k];

		for (int i = 1; i <= k; i++) {
			double t = ((double) i / (double) k);
			double temp = n * (1 - Math.sqrt(t));
			temp = Math.round(temp);
			answers[i - 1] = temp;
		}

		System.out.println(Arrays.toString(answers));
		scan.close();
		return answers;
	}

	public static void main(String[] args) {
		splitAlgorithm();
	}

}
