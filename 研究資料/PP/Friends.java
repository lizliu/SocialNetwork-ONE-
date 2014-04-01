/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package report;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.PeopleRank;
import routing.RoutingDecisionEngine;
import routing.community.CommunityDetectionEngine;

import core.DTNHost;
import core.Settings;
import core.SimScenario;
import core.UpdateListener;

public class Friends extends Report implements UpdateListener {
	Map<DTNHost, Set<DTNHost>> friends;

	public Friends() {
		friends = new HashMap<DTNHost, Set<DTNHost>>();
		init();
	}

	public void updated(List<DTNHost> hosts) {
		for (DTNHost h : hosts) {
			PeopleRank r = (PeopleRank) h.getRouter();
			friends.put(h, r.getfriends());
		}
	}

	@Override
	public void done() {
		int i = 0;
		int number = 0;
		String statsText = "";
		//write("========");
		//write("Friend");
		//write("========");
		//write("nID\t\tnID...");
		for (DTNHost h1 : friends.keySet()) {
			PeopleRank r1 = (PeopleRank) h1.getRouter();
			statsText = h1.toString().substring(1) + "\t";
			number = 0;
			for (DTNHost h2 : friends.keySet()) {
				PeopleRank r2 = (PeopleRank) h2.getRouter();
				if (h1.equals(h2))
					continue;
				else {
					Set<String> thisinterests = r1.getinterests();
					Set<String> otherinterests = r2.getinterests();
					int commonInterests = 0;
					Iterator<String> iterator = otherinterests.iterator();
					while (iterator.hasNext()) {
						if (thisinterests.contains(iterator.next()))
							commonInterests++;
					}
					if (commonInterests >= r1.k_interest) {
						//statsText = statsText + "," + h2.toString();
						i++;
						number++;
					} else
						;
				}
			}
			write(statsText+number);
			statsText = "";
		}
		/*
		write("");	
		write("Define a social relationship between two nodes u and v if they are sharing "+PeopleRank.k_interest+" common interests.");
		write("Total degree = " + i);
		write("Edges = " + i/2);
		write("Average degree = " + i/65.0);
		write("");
		write("========");
		write("Interest");
		write("========");
		write("nID\t\t1~35");
		for (DTNHost h1 : friends.keySet()) {
			PeopleRank r1 = (PeopleRank) h1.getRouter();
			statsText = h1.toString() + "\t";
			Set<String> thisinterests = r1.getinterests();
			statsText = statsText + "\t" + thisinterests;
			write(statsText);
			statsText = "";
		}
		*/
		super.done();
	}
}
