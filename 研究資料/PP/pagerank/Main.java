package org.mc2.pagerank;
import java.util.Iterator;

public class Main {

	public static void main(String args[]) {

		RankTable rt = new RankTable();
		rt.add(new Rank("a", 0.15));
		rt.add(new Rank("b", 0.15));
		rt.add(new Rank("c", 0.15));
		rt.add(new Rank("d", 0.15));

		LinkageTable lt = new LinkageTable();
		lt.add(new Linkage("b", "a"));
		lt.add(new Linkage("b", "c"));
		lt.add(new Linkage("c", "a"));
		lt.add(new Linkage("d", "a"));
		lt.add(new Linkage("d", "b"));
		lt.add(new Linkage("d", "c"));

		Iterator<Rank> rtIt = rt.iterator();
		Iterator<Linkage> ltIt;
		while (rtIt.hasNext()) {
			Rank rank = rtIt.next();
			ltIt = lt.iterator();
			while (ltIt.hasNext()) {
				Linkage link = ltIt.next();
				if (link.getSrc().equals(rank.getUrl())) {
					rank.addRankOut();
				}
			}
		}
		
		printRankTable(rt);
		
		for (int i = 0; i < 40; i++) {
			
			System.out.println("======================= round "+ i +"=========================");
			
			rtIt = rt.iterator();
			while (rtIt.hasNext()) {
				Rank rank = rtIt.next();
				ltIt = lt.iterator();
				while (ltIt.hasNext()) {
					Linkage link = ltIt.next();
					if (rank.getUrl().equals(link.getSrc())) {
						link.setSrcRank(rank.getRank() / rank.getNum());
					}
				}
			}

			printLinkageTable(lt);

			rtIt = rt.iterator();
			while (rtIt.hasNext()) {
				Rank rank = rtIt.next();
				ltIt = lt.iterator();
				while (ltIt.hasNext()) {
					Linkage link = ltIt.next();
					if (rank.getUrl().equals(link.getDst())) {
						rank.getAcc().add(link.getSrcRank());
					}
				}
			}

			 printRankTable(rt);

			rtIt = rt.iterator();
			while (rtIt.hasNext()) {
				Rank rank = rtIt.next();
				if (rank.getAcc().getNum() != 0) {
					rank.setRank((0.15 + 0.85*rank.getAcc().getRank()));
				}
				rank.getAcc().clear();
			}
			// printRankTable(rt);
		}
		printRankTable(rt);
	}

	public static void printRankTable(RankTable rt) {
		Iterator<Rank> it = rt.iterator();
		it = rt.iterator();
		while (it.hasNext()) {
			Rank rank = it.next();
			Accumulator acc = rank.getAcc();
			System.out.println(rank.getUrl() + " : " + rank.getRank() + " : "
					+ rank.getNum() + " : (" + acc.getRank() + ","
					+ acc.getNum() + ")");
		}
		System.out.println();
	}

	public static void printLinkageTable(LinkageTable lt) {
		Iterator<Linkage> it = lt.iterator();
		while (it.hasNext()) {
			Linkage link = it.next();
			System.out.println(link.getSrc() + " : " + link.getDst() + " : "
					+ link.getSrcRank());
		}
		System.out.println();
	}

}
