package org.mc2.pagerank;

public class Accumulator {

	private double rank = 0.0;
	
	private int num = 0;
	
	public Accumulator() {
		this.rank = 0.0;
		this.num = 0;
	}
	
	public Accumulator(double r, int n) {
		this.rank = r;
		this.num = n;
	}
	
	public void add(double r) {
		this.rank += r;
		this.num ++;
	}

	public double getRank() {
		return rank;
	}

	public int getNum() {
		return num;
	}
	
	public void clear() {
		this.rank = 0.0;
		this.num = 0;
	}

	
}
