package affinity_analysis;

import java.util.ArrayList;
import java.util.Collections;

public class ThresholdMeasure implements Comparable<ThresholdMeasure>{
	
	private int _threshold;
	private int _either;
	private int _both;
	
	public ThresholdMeasure(int threshold, int either, int both) {
		_threshold = threshold;
		_either = either;
		_both = both;
	}
	
	public ThresholdMeasure(int threshold) {
		this(threshold, 0, 0);
	}
	
	public ThresholdMeasure() {
		this(50);
	}
	
	public int getEither() { return _either; }
	
	public int getBoth() { return _both; }
	
	public int getThreshold() { return _threshold; }
	
	public void setThreshold(int thresh) { _threshold = thresh; }
	
	public void addEither() { _either += 1; }
	
	public void addBoth() { _both += 1; }
	
	public int compareTo(ThresholdMeasure t) {
		
		if (t == null) return 1;
		
		if (getBoth() > t.getBoth()) {
			return -1;
		} else if (getBoth() < t.getBoth()) {
			return 1;
		} else {
			if (getEither() < t.getEither()) {
				return -1;
			} else if (getEither() > t.getEither()) {
				return 1;
			}
			else {
				if (getThreshold() < t.getThreshold()) {
					return -1;
				} else if (getThreshold() > t.getThreshold()) {
					return 1;
				} else {
					return 0;
				}
			}
		}
		
	}
	
	@Override
	public String toString() {
		return "Threshold: " + _threshold + " Both: " + _both + " Either: " + _either;
	}
	
	public static void main(String[] args) {

		ArrayList<ThresholdMeasure> lst = new ArrayList<>();
		for (int i = 50; i < 53; i++) {
			for (int j = 50; j < 53; j++) {
				for (int k = 50; k < 53; k++) {
					lst.add(new ThresholdMeasure(i, j, k));
				}
			}
		}
		
		for (int i = 0; i < lst.size(); i++) {
			lst.get(i).addBoth();
			lst.get(i).addEither();
		}
		
		
		Collections.sort(lst);
		for (int i = 0; i < lst.size(); i++) 
			System.out.println(lst.get(i) + " " + lst.get(i).compareTo(lst.get(i + 1 < lst.size() ? i + 1 : 0)));
	}
	
}
