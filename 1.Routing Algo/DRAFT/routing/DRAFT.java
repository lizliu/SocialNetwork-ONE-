/* 
 * Distributed Rise And Fall spatio-Temporal (DRAFT) clustering algorithm for the ONE simulator
 * Copyright 2012 Matthew Orlinski
 * Released under GPLv3. See LICENSE.txt for details. 
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

public class DRAFT extends ActiveRouter {
	
	public static final String DRAFT_NS = "DRAFT";

		/** Total contact time threshold for adding a node to the familiar set 
	 * -setting id {@value} 
	 */
	public static final String FAMILIAR_SETTING = "familiarThreshold";
	public static final String DEGRADE = "degrade";
	public static final String FRAME_SIZE = "frameSize";
	
	// current working variables
	public Map<DTNHost, Double> neighbourSet;
	public Set<DTNHost> markedForDeletion;
	public Map<DTNHost, DefaultMutableTreeNode> localClusterCache;
	
	protected double familiarThreshold;
	protected double degrade;
	protected int frameSize;

	public DRAFT(Settings s) {
		super(s);
		Settings simpleSettings = new Settings(DRAFT_NS);
		this.familiarThreshold = simpleSettings.getDouble(FAMILIAR_SETTING);
		this.degrade = simpleSettings.getDouble(DEGRADE);
		this.frameSize = simpleSettings.getInt(FRAME_SIZE);
	}
	
	public DRAFT(DRAFT proto) {
		super(proto);
		this.frameSize = proto.frameSize;
		this.familiarThreshold = proto.familiarThreshold;
		this.degrade = proto.degrade;
		
		neighbourSet = new HashMap<DTNHost, Double>();
		markedForDeletion = new HashSet<DTNHost>();
		localClusterCache = new HashMap<DTNHost, DefaultMutableTreeNode>();
		
	}
	
	@Override
	public DRAFT replicate() {
		return new DRAFT(this);
	}
	
	@Override
	public void changedConnection(Connection con)
	{
		DTNHost myHost = getHost();
		DTNHost otherNode = con.getOtherNode(myHost);
		DRAFT otherRouter = (DRAFT)otherNode.getRouter();
		if(con.isUp())
		{
			if(this.neighbourSet.containsKey(otherNode)) {
				this.neighbourSet.put(otherNode, this.neighbourSet.get(otherNode) + 1);
			}
			else
				this.neighbourSet.put(otherNode, 1.0);

			// check local community information with new connections
			checkLocalCommunity(con);
			// Do node deletion
			if(this.localClusterCache.containsKey(otherNode))
			{			
				Iterator<DTNHost> it = this.markedForDeletion.iterator();
				while (it.hasNext()) {
					DTNHost host = it.next();

					if(otherRouter.markedForDeletion.contains(host)) {
						it.remove();
						this.localClusterCache.remove(host);
						otherRouter.markedForDeletion.remove(host);
						otherRouter.localClusterCache.remove(host);
					}			
				}
				Iterator<DTNHost> its = this.markedForDeletion.iterator();
				while (its.hasNext()) {
					DTNHost host = its.next();
					if(otherRouter.neighbourSet.containsKey(host)) {
						its.remove();
					}			
				}
			}
		}
	}

	
	


	public void checkLocalCommunity(Connection con) {
	
		DTNHost peer = con.getOtherNode(getHost());
		DRAFT peerC = (DRAFT) con.getOtherNode(getHost()).getRouter();

		// 2) check that the connection has met the time threshold. If so:
		if(this.neighbourSet.get(peer) >= this.familiarThreshold) {
			//System.out.println("Peer " + peer + " passed familiar threshold.");
			// 3) if device is in Do then remove it
			if(this.markedForDeletion.contains(peer))
				this.markedForDeletion.remove(peer);
			
			// 4.1 if the node is still ot in the local community, give it another chance with this secondary promotion mechanism
			if(!this.localClusterCache.containsKey(peer))
			{
				this.localClusterCache.put(peer, peerC.getLocalCluster(this.getHost()));
			}
		}		
	}

	// This function uses some tree logic when there is no need
	// (sorry it was the result of a copy/paste)
	public DefaultMutableTreeNode getLocalCluster(DTNHost dtnHost) {
		
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(this.getHost());
		Iterator<Entry<DTNHost, DefaultMutableTreeNode >> it = this.localClusterCache.entrySet().iterator();
		while (it.hasNext()) {
			Entry<DTNHost, DefaultMutableTreeNode > pairs = it.next();
			DTNHost host = (DTNHost) pairs.getKey();
			if (host != dtnHost && host != this.getHost()) {
				DefaultMutableTreeNode child = new DefaultMutableTreeNode(host);
				child.add(pairs.getValue());
				root.add(child);
			}
		}
		return root;
	}
	
	protected boolean commumesWithHost(DTNHost h)
	{
		return(this.localClusterCache.containsKey(h));
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
			DRAFT othRouter = (DRAFT)other.getRouter();

			if (othRouter.isTransferring()) {
				continue; // skip hosts that are transferring
			}

			for (Message m : msgCollection) {
				if (othRouter.hasMessage(m.getId())) {
					continue; // skip messages that the other one has
				}
				
				if(othRouter.commumesWithHost(m.getTo())) // peer is in local commun. of dest
					messages.add(new Tuple<Message, Connection>(m,con));
			}			
		}

		if (messages.size() == 0) {
			return null;
		}

		// sort the message-connection tuples
		return tryMessagesForConnected(messages);	// try to send messages
	}
	
	
	@Override
	public void update() {
		super.update();
		if (isTransferring() || !canStartTransfer()) {
			return; 
		}
		
		if (exchangeDeliverableMessages() != null) {
			return; 
		}
		tryOtherMessages();
		
		
		// For each connection increment the connection time by 1		
		for(Connection c : getConnections()) {
			DTNHost peer = c.getOtherNode(getHost());
			DRAFT peerC = (DRAFT) c.getOtherNode(getHost()).getRouter();
			
			if(this.neighbourSet.containsKey(peer)) {
				this.neighbourSet.put(peer, this.neighbourSet.get(peer) + 1);
			}
			else {
				this.neighbourSet.put(peer, 1.0);
			}
			
			if(this.neighbourSet.get(peer) >= this.familiarThreshold) {
				checkLocalCommunity(c);
			}
		}
				
		
		double simTime = SimClock.getTime(); // (seconds since start)
		double timeInFrame = simTime % this.frameSize;
		if(timeInFrame == 0)  {
			emptyMarkedForDeletion();
			decreaseAllNodes();			
		}
		
		
	}
	
	

	public void markNodeForDeletion(DTNHost node) {
		this.markedForDeletion.add(node);
	}
	
	private void decreaseAllNodes() {
		Iterator<Entry<DTNHost, Double>> it = neighbourSet.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = it.next();
			Double newValue = (((Double) pairs.getValue()) * this.degrade);
			neighbourSet.put((DTNHost) pairs.getKey(), newValue);
			if(newValue < (this.familiarThreshold) && this.localClusterCache.containsKey((DTNHost) pairs.getKey())) {
				markNodeForDeletion((DTNHost) pairs.getKey());
			}
		}
	}
	
	/**
	 * empties marked for deletion
	 */
	public void emptyMarkedForDeletion() {
		// Any nodes that are still in the mark for deletion pile, delete
		for(DTNHost host : this.markedForDeletion) {
			if(this.localClusterCache.containsKey(host)) {
				this.localClusterCache.remove(host);
			}
		}
	}


	
}
