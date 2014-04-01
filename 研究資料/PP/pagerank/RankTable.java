package org.mc2.pagerank;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class RankTable {

	private List<Rank> table;
	
	public RankTable() {
		table = new ArrayList();
	}
	
	public void add(Rank r) {
		table.add(r);
	}
	
	public Iterator<Rank> iterator() {
		return table.iterator();
	}
	
}
