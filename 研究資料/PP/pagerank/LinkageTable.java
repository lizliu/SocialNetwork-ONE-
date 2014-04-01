package org.mc2.pagerank;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


public class LinkageTable {

	private List<Linkage> table = null;
	
	public LinkageTable() {
		this.table = new LinkedList<Linkage>();
	}
	
	public void add(Linkage l) {
		this.table.add(l);
	}
	
	public Iterator<Linkage> iterator() {
		return this.table.iterator();
	}
	
}
