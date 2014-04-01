package org.mc2.pagerank;

public class Linkage {

	private String src = null;
	
	private String dst = null;
	
	private double srcRank = 0.0;
	
	public Linkage() {
		
	}
	
	public Linkage(String s, String d) {
		this.src = s;
		this.dst = d;
	}
	
	public double getSrcRank() {
		return srcRank;
	}

	public void setSrcRank(double srcRank) {
		this.srcRank = srcRank;
	}

	public String getSrc() {
		return src;
	}

	public void setSrc(String src) {
		this.src = src;
	}

	public String getDst() {
		return dst;
	}

	public void setDst(String dst) {
		this.dst = dst;
	}
	
}
