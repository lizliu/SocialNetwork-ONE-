package org.mc2.pagerank;

public class Rank {

	private String url = null;
	
	private double rank = 1.0;
	
	// ÉÈ³öÊý
	private int num = 0;
	
	private Accumulator acc = null;
	
	public Rank() {
		this.acc = new Accumulator();
	}
	
	public Rank(String u, double r) {
		this.url = u;
		this.rank = r;
		this.acc = new Accumulator();
	}
	
	public void addRankOut() {
		this.num++;
	}
	
	public int getNum() {
		return this.num;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public double getRank() {
		return rank;
	}

	public void setRank(double rank) {
		this.rank = rank;
	}

	public Accumulator getAcc() {
		return acc;
	}

	public void setAcc(Accumulator acc) {
		this.acc = acc;
	}
	
	
}
