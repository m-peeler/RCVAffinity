package affinity_analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

// Returns an object of type T corresponding to two unordered objects of type I. 
public class DoubleIndexTable<I, T extends Comparable<T>> {
	
	public class TwoIndexEntry implements Comparable<TwoIndexEntry> {
		I _indOne;
		I _indTwo;
		T _entry;
		
		public TwoIndexEntry(I one, I two, T ent) {
			_indOne = one; _indTwo = two; _entry = ent;
		}
		
		public T getEntry() { return _entry; }
		
		public I getIndOne() { return _indOne; }
		
		public I getIndTwo() { return _indTwo; }
		
		public int compareTo(TwoIndexEntry snd) { return _entry.compareTo(snd._entry); }
		
		public String toString() {
			return _indOne + "  |  " + _indTwo + "  |  " + _entry;
		}
		
	}
	
	Map<I, Map<I, T>> _table;
	
	public DoubleIndexTable() {
		_table = new HashMap<>();
	}
		 
	public T get(I fst, I snd) {
		
		T op1 = _table.containsKey(fst) ? _table.get(fst).get(snd) : null;
		T op2 = _table.containsKey(snd) ? _table.get(snd).get(fst) : null;
		
		return op1 != null ? op1 : op2;
	}
	
	public void put(I fst, I snd, T val) {
		if (_table.containsKey(fst)) {
			_table.get(fst).put(snd, val);
			if (_table.containsKey(snd)) {
				_table.get(snd).remove(snd);
			}
		} else if (_table.containsKey(snd)) {
			_table.get(snd).put(fst, val);
		} else {
			_table.put(fst, new HashMap<>());
			_table.get(fst).put(snd, val);
		}
	}
	
	public ArrayList<TwoIndexEntry> entrySet() {
		
		ArrayList<TwoIndexEntry> arr = new ArrayList<>();
		
		for (Entry<I, Map<I,T>> e : _table.entrySet()) {
			for (Entry<I,T> f : e.getValue().entrySet()) {
				arr.add(new TwoIndexEntry(e.getKey(), f.getKey(), f.getValue()));
			}
		}
		
		Collections.sort(arr);

		return arr;
	}
	
}