/*
 * DEBTT router for The One Simulator
 *
 * Copyright 2012 by Matthew Orlinski, released under GPLv3.
 */

package routing;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.tree.DefaultMutableTreeNode;
import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.Tuple;


public class DistributedExpectationBasedTreeRouter extends ActiveRouter {
	
	public static final String EXPECTATION_SETTINGS = "DistributedExpectationBasedTreeRouter";
	public static final String FRAME_SIZE = "frameSize";
	public static final String BASELIEN_LENGTH = "baselineLength";
	public static final String JOIN_COEFFICIENT = "joinCoefficient";
	public static final String REMOVE_COEFFICIENT = "removeCoefficient";
	
	// current working variables
	public Map<DTNHost, Integer> neighbourSet;
	public Set<DTNHost> markedForDeletion;
	public Map<DTNHost, DefaultMutableTreeNode> localCommunity;

	LinkedList<Double> pastAverages;
	double currBaseline = 0.0;

	private double joinCoefficient;
	private double removeCoefficient; 
	
	protected int frameSize;
	protected int baselineLength;
	
	public DistributedExpectationBasedTreeRouter(Settings s) {
		super(s);
		Settings simpleSettings = new Settings(EXPECTATION_SETTINGS);
		this.frameSize = simpleSettings.getInt(FRAME_SIZE);
		this.baselineLength = simpleSettings.getInt(BASELIEN_LENGTH);
		this.joinCoefficient = simpleSettings.getDouble(JOIN_COEFFICIENT);
		this.removeCoefficient = simpleSettings.getDouble(REMOVE_COEFFICIENT);
	}

	public DistributedExpectationBasedTreeRouter(DistributedExpectationBasedTreeRouter proto) {
		super(proto);
		this.frameSize = proto.frameSize;
		this.baselineLength = proto.baselineLength;
		this.joinCoefficient = proto.joinCoefficient;
		this.removeCoefficient = proto.removeCoefficient;
		
		neighbourSet = new HashMap<DTNHost, Integer>();
		localCommunity = new HashMap<DTNHost, DefaultMutableTreeNode>();
		markedForDeletion = new HashSet<DTNHost>();
		pastAverages = new LinkedList<Double>();
	}
	
	
	@Override
	public DistributedExpectationBasedTreeRouter replicate() {
		return new DistributedExpectationBasedTreeRouter(this);
	}
	
	
	@Override
	public void changedConnection(Connection con)
	{
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
	
	
	public void processPromotionToLocalCommunity() {	
		Iterator<Entry<DTNHost, Integer>> it = this.neighbourSet.entrySet().iterator();
		while (it.hasNext()) {
			Entry<DTNHost, Integer> pairs = it.next();
			double count =  pairs.getValue();
			
			if(count >= (this.joinCoefficient*this.currBaseline)) {

				this.localCommunity.put(pairs.getKey(),  ((DistributedExpectationBasedTreeRouter) pairs.getKey().getRouter()).getLocalCommunityTree(this.getHost()));
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
		DTNHost other;
		DistributedExpectationBasedTreeRouter othRouter;
		

		for (Connection con : getConnections()) {
			other = con.getOtherNode(getHost());
			othRouter = (DistributedExpectationBasedTreeRouter)other.getRouter();

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
				else if(commumesWithHostIndirectlyAndNotThis(othRouter, m.getTo())) {
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

	
	protected boolean commumesWithHostDirectly(DistributedExpectationBasedTreeRouter othRouter, DTNHost destination)
	{
		Map<DTNHost, DefaultMutableTreeNode> othersCommunity = othRouter.localCommunity;

		if(othersCommunity.containsKey(destination)) {
			return true;
		}
		return false;
	}

	
	protected boolean commumesWithHostIndirectlyAndNotThis(DistributedExpectationBasedTreeRouter othRouter, DTNHost destination)
	{	
		// want to find out if the other router has a path where this doesnt come before destination
		DefaultMutableTreeNode child;
		DefaultMutableTreeNode branchRoot;
		DefaultMutableTreeNode pathNode; 
		Enumeration depth;
		Enumeration path;
		Iterator<Entry<DTNHost, DefaultMutableTreeNode>> it =  othRouter.localCommunity.entrySet().iterator();
		
		while (it.hasNext()) {
			Entry<DTNHost, DefaultMutableTreeNode> pairs = it.next();	
			// get root
			branchRoot = pairs.getValue();
			if(branchRoot.getUserObject() != this.getHost()) {
				
				depth = branchRoot.depthFirstEnumeration();
				
				while (depth.hasMoreElements()) {
					child = (DefaultMutableTreeNode) depth.nextElement();

					if(child.getUserObject() == destination) {
						// search back
						path = child.pathFromAncestorEnumeration(branchRoot);
						while (path.hasMoreElements()) {
							pathNode = (DefaultMutableTreeNode) path.nextElement();
							if(pathNode.getUserObject() == this.getHost()) {
								break;
							}
							if(pathNode == branchRoot) {
								//System.out.println("Path Found");
								return true;
							}
						}
					}
				}
			}
		}
		//System.out.println("No path");
		return false;
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
	
	
	
	public DefaultMutableTreeNode  getLocalCommunityTree(DTNHost myHost) {
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(this.getHost());
		Iterator<Entry<DTNHost, DefaultMutableTreeNode >> it = this.localCommunity.entrySet().iterator();
		while (it.hasNext()) {
			Entry<DTNHost, DefaultMutableTreeNode > pairs = it.next();
			DTNHost host = (DTNHost) pairs.getKey();
			if(host != myHost) {
				DefaultMutableTreeNode child = new DefaultMutableTreeNode(host);
				child.add(pairs.getValue());
				root.add(child);			
			}
		}
		return root;
	}
	
	
	
	public void updateMarkedForDeletionAndBroadcastLocalCommunity() {
		// first off we want to mark any devices whos count is lower thn the baseline and are in the local cluster to be marked for deletion
		Iterator<Entry<DTNHost, DefaultMutableTreeNode>> it = this.localCommunity.entrySet().iterator();
		while (it.hasNext()) {
			
			Entry<DTNHost, DefaultMutableTreeNode> pairs = it.next();
			Integer count = this.neighbourSet.get(pairs.getKey());
			
			if((count == null) && this.localCommunity.containsKey(pairs.getKey())) {
				this.markedForDeletion.add(pairs.getKey());
			}
			else if((count < (this.removeCoefficient*this.currBaseline)) && this.localCommunity.containsKey(pairs.getKey())) {
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

	
	public Map<DTNHost, DefaultMutableTreeNode> getLocalCommunity() {
		return this.localCommunity;
	}
	
	
}
