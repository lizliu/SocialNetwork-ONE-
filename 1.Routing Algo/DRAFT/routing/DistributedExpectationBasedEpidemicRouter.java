/*
 * DEBTE router for The One Simulator
 *
 * Copyright 2012 by Matthew Orlinski, released under GPLv3.
 */

package routing;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;




import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.Tuple;


public class DistributedExpectationBasedEpidemicRouter extends ActiveRouter {
	
	public static final String EXPECTATION_SETTINGS = "DistributedExpectationBasedEpidemicRouter";
	public static final String FRAME_SIZE = "frameSize";
	public static final String BASELIEN_LENGTH = "baselineLength";
	public static final String JOIN_COEFFICIENT = "joinCoefficient";
	public static final String REMOVE_COEFFICIENT = "removeCoefficient";
	
	// current working variables
	public Map<DTNHost, Integer> neighbourSet;
	public Set<DTNHost> markedForDeletion;
	public Map<DTNHost, HashSet<DTNHost>> localCommunity;

	LinkedList<Double> pastAverages;
	double currBaseline = 0.0;

	private double joinCoefficient;
	private double removeCoefficient; 
	
	protected int frameSize;
	protected int baselineLength;
	
	public DistributedExpectationBasedEpidemicRouter(Settings s) {
		super(s);
		Settings simpleSettings = new Settings(EXPECTATION_SETTINGS);
		this.frameSize = simpleSettings.getInt(FRAME_SIZE);
		this.baselineLength = simpleSettings.getInt(BASELIEN_LENGTH);
		this.joinCoefficient = simpleSettings.getDouble(JOIN_COEFFICIENT);
		this.removeCoefficient = simpleSettings.getDouble(REMOVE_COEFFICIENT);
	}

	
	public DistributedExpectationBasedEpidemicRouter(DistributedExpectationBasedEpidemicRouter proto) {
		super(proto);
		this.frameSize = proto.frameSize;
		this.baselineLength = proto.baselineLength;
		this.joinCoefficient = proto.joinCoefficient;
		this.removeCoefficient = proto.removeCoefficient;
		
		neighbourSet = new HashMap<DTNHost, Integer>();
		localCommunity = new HashMap<DTNHost, HashSet<DTNHost>>();
		markedForDeletion = new HashSet<DTNHost>();
		pastAverages = new LinkedList<Double>();
	}

	
	@Override
	public DistributedExpectationBasedEpidemicRouter replicate() {
		return new DistributedExpectationBasedEpidemicRouter(this);
	}
	
	
	@Override
	public void changedConnection(Connection con) {
		DTNHost myHost = getHost();
		DTNHost otherNode = con.getOtherNode(myHost);
			
		if(con.isUp())
		{
			if(this.neighbourSet.containsKey(otherNode)) {
				this.neighbourSet.put(otherNode, this.neighbourSet.get(otherNode) + 1);
			}
			else
				this.neighbourSet.put(otherNode, 1);
			
		}

	}

	
	protected boolean commumesWithHostDirectly(DistributedExpectationBasedEpidemicRouter othRouter, DTNHost destination) {
		Map<DTNHost, HashSet<DTNHost>> othersCommunity = othRouter.localCommunity;

		//if(othersCommunity.containsKey(destination) && !othRouter.isMarkedForDeletion(destination)) {
		if(othersCommunity.containsKey(destination)) {
			//System.out.println("longer path found 1");
			return true;
		}
		return false;
		
	}


	protected boolean commumesWithHostIndirectly(DistributedExpectationBasedEpidemicRouter othRouter, DTNHost destination) {
		HashSet<DTNHost> othersBranch = othRouter.getBranchSetOnly();
		if(othersBranch.contains(destination)) {
			return true;
		}
		return false;
	}
	
	
	public HashSet<DTNHost> getBranchSetOnly() {
		HashSet<DTNHost> tempSet = new HashSet<DTNHost>();
		Iterator<Entry<DTNHost, HashSet<DTNHost>>> it = this.localCommunity.entrySet().iterator();
		while (it.hasNext()) {
			Entry<DTNHost, HashSet<DTNHost>> pairs = it.next();
			// now also add the branches
			HashSet<DTNHost> branch = pairs.getValue();
			tempSet.addAll(branch);
		}
		return tempSet;
	}
	
	
	public void processPromotionToLocalCommunity() {	
		Iterator<Entry<DTNHost, Integer>> it = this.neighbourSet.entrySet().iterator();
		while (it.hasNext()) {
			Entry<DTNHost, Integer> pairs = it.next();
			double count =  pairs.getValue();
			
			if(count >= (this.joinCoefficient*this.currBaseline)) {

				this.localCommunity.put(pairs.getKey(),  ((DistributedExpectationBasedEpidemicRouter) pairs.getKey().getRouter()).getLocalCommunitySet());
			}
			
		}
	}
	
	
	/**
	 * Tries to send all other messages to all connected hosts
	 * 
	 * @return The return value of {@link #tryMessagesForConnected(List)}
	 */
	private Tuple<Message, Connection> tryOtherMessages() {
		List<Tuple<Message, Connection>> messages = 
			new ArrayList<Tuple<Message, Connection>>(); 

		Collection<Message> msgCollection = getMessageCollection();

		for (Connection con : getConnections()) {
			DTNHost other = con.getOtherNode(getHost());
			DistributedExpectationBasedEpidemicRouter othRouter = (DistributedExpectationBasedEpidemicRouter)other.getRouter();

			if (othRouter.isTransferring()) {
				continue; // skip hosts that are transferring
			}

			for (Message m : msgCollection) {
				if (othRouter.hasMessage(m.getId())) {
					continue; // skip messages that the other one has
				}
				if(commumesWithHostDirectly(othRouter, m.getTo())) {						
					messages.add(new Tuple<Message, Connection>(m,con));
				}
				else if(commumesWithHostIndirectly(othRouter, m.getTo())) {
					messages.add(new Tuple<Message, Connection>(m,con));
				}
			}			
		}

		if (messages.size() == 0) {
			return null;
		}

		// sort the message-connection tuples
		return tryMessagesForConnected(messages);	// try to send messages
	}


	/**
	 * empties marked for deletion
	 */
	public boolean isMarkedForDeletion(DTNHost dest) {
		return this.markedForDeletion.contains(dest);
	}
	
	
	
	public void calculateNewBaseline() {
		double count = 0.0;
		// calculate average of contact lengths for this frame
		Iterator<Entry<DTNHost, Integer>> it = this.neighbourSet.entrySet().iterator();
		while (it.hasNext()) {
			Entry<DTNHost, Integer> pairs = it.next();
			count += (Integer) pairs.getValue();
		}
		if(this.neighbourSet.size() > 0) {
			count = ((double) count) / ((double) this.neighbourSet.size());
		}
		
		if(pastAverages.size() > this.baselineLength)
			pastAverages.removeFirst();
		pastAverages.addLast(count);
		
		double baseline = 0.0;
		// now the bastline is the average of averages
		for(double frameAverage : this.pastAverages) {
			baseline += frameAverage;
		}
		//if(this.getHost().toString().compareTo("A20") == 0)
		//	System.out.println(this.getHost()+" - "+(baseline / this.pastAverages.size()));
		this.currBaseline = (baseline / this.pastAverages.size()); 
	}
	
	
	public HashSet<DTNHost> getLocalCommunitySet() {
		HashSet<DTNHost> tempSet = new HashSet<DTNHost>();
		Iterator<Entry<DTNHost, HashSet<DTNHost>>> it = this.localCommunity.entrySet().iterator();
		while (it.hasNext()) {
			Entry<DTNHost, HashSet<DTNHost>> pairs = it.next();
			DTNHost host = (DTNHost) pairs.getKey();
			tempSet.add(host);
			// now also add the branches
			HashSet<DTNHost> branch = pairs.getValue();
			tempSet.addAll(branch);
		}
		return tempSet;
	}
	
	public HashSet<DTNHost> getLocalCommunitySetMinusMarkedForDeletion() {
		HashSet<DTNHost> tempSet = new HashSet<DTNHost>();
		Iterator<Entry<DTNHost, HashSet<DTNHost>>> it = this.localCommunity.entrySet().iterator();
		while (it.hasNext()) {
			Entry<DTNHost, HashSet<DTNHost>> pairs = it.next();
			DTNHost host = (DTNHost) pairs.getKey();
			if(!isMarkedForDeletion(host)) {
				tempSet.add(host);
				// now also add the branches
				HashSet<DTNHost> branch = pairs.getValue();
				tempSet.addAll(branch);
			}
		}
		return tempSet;
	}

	
	public void updateMarkedForDeletionAndBroadcastLocalCommunity() {
		// first off we want to mark any devices whos count is lower thn the baseline and are in the local cluster to be marked for deletion
		Iterator<Entry<DTNHost, HashSet<DTNHost>>> it = this.localCommunity.entrySet().iterator();
		while (it.hasNext()) {
			
			Entry<DTNHost, HashSet<DTNHost>> pairs = it.next();
			Integer count = this.neighbourSet.get(pairs.getKey());
			
			//if(this.getHost().getAddress() == 25 && pairs.getKey().getAddress() == 40)
			//	System.out.println(this.getHost().getAddress()+" "+pairs.getKey().getAddress()+" "+count);
			if((count == null) && this.localCommunity.containsKey(pairs.getKey())) {
				this.markedForDeletion.add(pairs.getKey());
			}
			else if((count < (this.removeCoefficient*this.currBaseline)) && this.localCommunity.containsKey(pairs.getKey())) {
				//if(this.getHost().getAddress() == 25 && pairs.getKey().getAddress() == 40)
				//	System.out.println(this.getHost().getAddress()+" "+pairs.getKey().getAddress()+" "+count+" "+this.currBaseline);
				this.markedForDeletion.add(pairs.getKey());
			}
		}
		
	}
	
	
	public void emptyMarkedForDeletion() {
		for(DTNHost host : this.markedForDeletion) {
			this.localCommunity.remove(host);
		}
		this.markedForDeletion.clear();
	}
	
	
	@Override
	public void update() {
		super.update();
		/**
		 * 
		 * Start expectation logic
		 * 
		 */
		double simTime = SimClock.getTime(); // (seconds since start)
		double timeInFrame = simTime % this.frameSize;
		if(timeInFrame == 0)  {
			emptyMarkedForDeletion();
			calculateNewBaseline();
			processPromotionToLocalCommunity();
			updateMarkedForDeletionAndBroadcastLocalCommunity();
			resetNeighbourTables();
		}

		// For each connection increment the connection time by 1		
		for(Connection c : getConnections()) {
			DTNHost peer = c.getOtherNode(getHost());
			if(this.neighbourSet.containsKey(peer)) {
				this.neighbourSet.put(peer, this.neighbourSet.get(peer) + 1);
			}
			else
				this.neighbourSet.put(peer, 1);
		}
		processPromotionToLocalCommunity();
		
		
		if (isTransferring() || !canStartTransfer()) {
			return; 
		}
		
		if (exchangeDeliverableMessages() != null) {
			return; 
		}
		tryOtherMessages();
		


	}

	
	public void resetNeighbourTables() {
		this.neighbourSet = new HashMap<DTNHost, Integer>();
	}
	
	
	public Map<DTNHost, HashSet<DTNHost>> getLocalCommunity() {
		return this.localCommunity;
	}
	
	
}
