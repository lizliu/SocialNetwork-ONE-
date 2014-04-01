package routing;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;




import core.*;

/**
 * This class overrides ActiveRouter in order to inject calls to a 
 * DecisionEngine object where needed add extract as much code from the update()
 * method as possible. 
 * 
 * <strong>Forwarding Logic:</strong> 
 * 
 * A DecisionEngineRouter maintains a List of Tuple<Message, Connection> in 
 * support of a call to ActiveRouter.tryMessagesForConnected() in 
 * DecisionEngineRouter.update(). Since update() is called so frequently, we'd 
 * like as little computation done in it as possible; hence the List that gets
 * updated when events happen. Four events cause the List to be updated: a new 
 * message from this host, a new received message, a connection goes up, or a 
 * connection goes down. On a new message (either from this host or received 
 * from a peer), the collection of open connections is examined to see if the
 * message should be forwarded along them. If so, a new Tuple is added to the
 * List. When a connection goes up, the collection of messages is examined to 
 * determine to determine if any should be sent to this new peer, adding a Tuple
 * to the list if so. When a connection goes down, any Tuple in the list
 * associated with that connection is removed from the List.
 * 
 * <strong>Decision Engines</strong>
 * 
 * Most (if not all) routing decision making is provided by a 
 * RoutingDecisionEngine object. The DecisionEngine Interface defines methods 
 * that enact computation and return decisions as follows:
 * 
 * <ul>
 *   <li>In createNewMessage(), a call to RoutingDecisionEngine.newMessage() is 
 * 	 made. A return value of true indicates that the message should be added to
 * 	 the message store for routing. A false value indicates the message should
 *   be discarded.
 *   </li>
 *   <li>changedConnection() indicates either a connection went up or down. The
 *   appropriate connectionUp() or connectionDown() method is called on the
 *   RoutingDecisionEngine object. Also, on connection up events, this first
 *   peer to call changedConnection() will also call
 *   RoutingDecisionEngine.doExchangeForNewConnection() so that the two 
 *   decision engine objects can simultaneously exchange information and update 
 *   their routing tables (without fear of this method being called a second
 *   time).
 *   </li>
 *   <li>Starting a Message transfer, a protocol first asks the neighboring peer
 *   if it's okay to send the Message. If the peer indicates that the Message is
 *   OLD or DELIVERED, call to RoutingDecisionEngine.shouldDeleteOldMessage() is
 *   made to determine if the Message should be removed from the message store.
 *   <em>Note: if tombstones are enabled or deleteDelivered is disabled, the 
 *   Message will be deleted and no call to this method will be made.</em>
 *   </li>
 *   <li>When a message is received (in messageTransferred), a call to 
 *   RoutingDecisionEngine.isFinalDest() to determine if the receiving (this) 
 *   host is an intended recipient of the Message. Next, a call to 
 *   RoutingDecisionEngine.shouldSaveReceivedMessage() is made to determine if
 *   the new message should be stored and attempts to forward it on should be
 *   made. If so, the set of Connections is examined for transfer opportunities
 *   as described above.
 *   </li>
 *   <li> When a message is sent (in transferDone()), a call to 
 *   RoutingDecisionEngine.shouldDeleteSentMessage() is made to ask if the 
 *   departed Message now residing on a peer should be removed from the message
 *   store.
 *   </li>
 * </ul>
 * 
 * <strong>Tombstones</strong>
 * 
 * The ONE has the the deleteDelivered option that lets a host delete a message
 * if it comes in contact with the message's destination. More aggressive 
 * approach lets a host remember that a given message was already delivered by
 * storing the message ID in a list of delivered messages (which is called the
 * tombstone list here). Whenever any node tries to send a message to a host 
 * that has a tombstone for the message, the sending node receives the 
 * tombstone.
 * 
 * @author PJ Dillon, University of Pittsburgh
 */
public class DecisionEngineRouter extends ActiveRouter
{
	public static final String PUBSUB_NS = "DecisionEngineRouter";
	public static final String ENGINE_SETTING = "decisionEngine";
	public static final String TOMBSTONE_SETTING = "tombstones";
	public static final String CONNECTION_STATE_SETTING = "";
	
	protected boolean tombstoning;
	protected RoutingDecisionEngine decider;
	protected List<Tuple<Message, Connection>> outgoingMessages;
	
	protected boolean isBinary;
	protected int initialNrofCopies;
	public static final String MSG_COUNT_PROPERTY = "SPRAYANDWAIT_NS" + "." +
		"copies";
	
	protected Set<String> tombstones;
	/** how often TTL check (discarding old messages) is performed */
	public static int TTL_CHECK_INTERVAL = 60;
	/** sim time when the last TTL check was done */
	private double lastTtlCheck;
	public static int com_CHECK_INTERVAL = 345600;
	
	
	public static final int copy = 2;
	
	
	private double lastcomCheck;
	public List<String> deciderPickPath;
	public List<String> TempGraph;
	public Map<String, Integer> conntimes;
	protected Set<String> havesawmessage;
	/** 
	 * Used to save state machine when new connections are made. See comment in
	 * changedConnection() 
	 */
	protected Map<Connection, Integer> conStates;
	
	public DecisionEngineRouter(Settings s)
	{
		super(s);
		
		Settings routeSettings = new Settings(PUBSUB_NS);
		
		outgoingMessages = new LinkedList<Tuple<Message, Connection>>();
		
		decider = (RoutingDecisionEngine)routeSettings.createIntializedObject(
				"routing." + routeSettings.getSetting(ENGINE_SETTING));
		
		if(routeSettings.contains(TOMBSTONE_SETTING))
			tombstoning = routeSettings.getBoolean(TOMBSTONE_SETTING);
		else
			tombstoning = false;
		
		//tombstoning = true;
		if(tombstoning)
			tombstones = new HashSet<String>(10);
		conStates = new HashMap<Connection, Integer>(4);
		initialNrofCopies = copy ;
		isBinary = true;
		deciderPickPath = new LinkedList<String>();
		TempGraph = new LinkedList<String>();
		conntimes = new HashMap<String, Integer>();
		havesawmessage = new HashSet<String>();
		//System.out.println("tombstoning: " + tombstoning);
		
	}

	public DecisionEngineRouter(DecisionEngineRouter r)
	{
		super(r);
		outgoingMessages = new LinkedList<Tuple<Message, Connection>>();
		decider = r.decider.replicate();
		tombstoning = r.tombstoning;
		
		if(this.tombstoning)
			tombstones = new HashSet<String>(10);
		conStates = new HashMap<Connection, Integer>(4);
		this.initialNrofCopies = copy;
		this.isBinary = true;
		deciderPickPath = new LinkedList<String>();
		TempGraph = new LinkedList<String>();
		conntimes = new HashMap<String, Integer>();
		havesawmessage = new HashSet<String>();
	//System.out.println("2");	
	}

	@Override
	public MessageRouter replicate()
	{
		return new DecisionEngineRouter(this);
	}

	@Override
	/*public boolean createNewMessage(Message m)
	{
		if(decider.newMessage(m))
		{
			makeRoomForNewMessage(m.getSize());
			addToMessages(m, true);
			
			findConnectionsForNewMessage(m, getHost());
			return true;
		}
		return false;
	}*/
	public boolean createNewMessage(Message m)
	{
		if(decider.newMessage(m))
		{
			//if(m.getId().equals("M7"))
			//System.out.println("Host: " + getHost() + " Creating M7");
			makeRoomForNewMessage(m.getSize());
			m.setTtl(this.msgTtl);
			addToMessages(m, true); 
			m.addProperty(MSG_COUNT_PROPERTY, new Integer(initialNrofCopies));
			findConnectionsForNewMessage(m, getHost());
			//havesawmessage.add(m.toString());
			return true;
		}
		return false;
	}
	
	@Override
	public void changedConnection(Connection con)
	{
		DTNHost myHost = getHost();
		DTNHost otherNode = con.getOtherNode(myHost);
		DecisionEngineRouter otherRouter = (DecisionEngineRouter)otherNode.getRouter();
		if(con.isUp())
		{
			decider.connectionUp(myHost, otherNode);
			//decider.updatecommunity(TempGraph, getHost());
			/*
			 * This part is a little confusing because there's a problem we have to
			 * avoid. When a connection comes up, we're assuming here that the two 
			 * hosts who are now connected will exchange some routing information and
			 * update their own based on what the get from the peer. So host A updates
			 * its routing table with info from host B, and vice versa. In the real
			 * world, A would send its *old* routing information to B and compute new
			 * routing information later after receiving B's *old* routing information.
			 * In ONE, changedConnection() is called twice, once for each host A and
			 * B, in a serial fashion. If it's called for A first, A uses B's old info
			 * to compute its new info, but B later uses A's *new* info to compute its
			 * new info.... and this can lead to some nasty problems. 
			 * 
			 * To combat this, whichever host calls changedConnection() first calls
			 * doExchange() once. doExchange() interacts with the DecisionEngine to
			 * initiate the exchange of information, and it's assumed that this code
			 * will update the information on both peers simultaneously using the old
			 * information from both peers.
			 */
			if(shouldNotifyPeer(con))
			{
				this.doExchange(con, otherNode);
				otherRouter.didExchange(con);
			/*	int connsize1 = otherRouter.decider.sumconn();
				otherRouter.conntimes.put(otherNode.toString(), new Integer(connsize1));
				int connsize2 = decider.sumconn();
				conntimes.put(myHost.toString(), new Integer(connsize2));*/
			}
		/*	if( Integer.parseInt(myHost.toString().substring(1)) > Integer.parseInt(otherNode.toString().substring(1)) )
				TempGraph.add(SimClock.getTime()+" CONN "+otherNode.toString().substring(1)+" "+
						myHost.toString().substring(1)+" up");
			else
				TempGraph.add(SimClock.getTime()+" CONN "+myHost.toString().substring(1)+" "+
						otherNode.toString().substring(1)+" up");
		
			*/
			/* for( String str : otherRouter.conntimes.keySet() )
			 {
				 if( !conntimes.containsKey(str) )
					 conntimes.put(str, otherRouter.conntimes.get(str));
				 else{
					 if(otherRouter.conntimes.get(str) >  conntimes.get(str) )
						 conntimes.put(str, otherRouter.conntimes.get(str));
						 
				 }
					 
			 }*/
				 
			 //System.out.println(conntimes);
			/*
			 * Once we have new information computed for the peer, we figure out if
			 * there are any messages that should get sent to this peer.
			 */
			Collection<Message> msgs = getMessageCollection();
			for(Message m : msgs)
			{
				if(decider.shouldSendMessageToHost(m, otherNode)){		
						outgoingMessages.add(new Tuple<Message,Connection>(m, con));

				}
			}
		}
		else
		{
			decider.connectionDown(myHost, otherNode);
			
			conStates.remove(con);
			
			/*	if( Integer.parseInt(myHost.toString().substring(1)) > Integer.parseInt(otherNode.toString().substring(1)) )
				TempGraph.add(SimClock.getTime()+" CONN "+otherNode.toString().substring(1)+" "+
						myHost.toString().substring(1)+" down");
			else
				TempGraph.add(SimClock.getTime()+" CONN "+myHost.toString().substring(1)+" "+
						otherNode.toString().substring(1)+" down");
			//
			
			for( String str : otherRouter.TempGraph )
			{
				if( !TempGraph.contains(str) )
					TempGraph.add(str);
				
			}
			
			Collections.sort(TempGraph, new EventComparatortest());
			*/
			//System.out.println(myHost.toString()+" "+TempGraph);
			/*
			 * If we  were trying to send message to this peer, we need to remove them
			 * from the outgoing List.
			 */
			for(Iterator<Tuple<Message,Connection>> i = outgoingMessages.iterator(); 
					i.hasNext();)
			{
				Tuple<Message, Connection> t = i.next();
				if(t.getValue() == con)
					i.remove();
			}
		}
	}
	
	protected void doExchange(Connection con, DTNHost otherHost)
	{
		conStates.put(con, 1);
		decider.doExchangeForNewConnection(con, otherHost);
	}
	
	/**
	 * Called by a peer DecisionEngineRouter to indicated that it already 
	 * performed an information exchange for the given connection.
	 * 
	 * @param con Connection on which the exchange was performed
	 */
	protected void didExchange(Connection con)
	{
		conStates.put(con, 1);
	}
	
	@Override
	protected int startTransfer(Message m, Connection con)
	{
		int retVal;
		
		if (!con.isReadyForTransfer()) {
			return TRY_LATER_BUSY;
		}
		Integer nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROPERTY);
		
		retVal = con.startTransfer(getHost(), m);
	
		if (retVal == RCV_OK) { 			
			addToSendingConnections(con);// started transfer			
		}
		else if(tombstoning && retVal == DENIED_DELIVERED)
		{
			this.deleteMessage(m.getId(), false);
			tombstones.add(m.getId());
		}
		else if (deleteDelivered && (retVal == DENIED_OLD || retVal == DENIED_DELIVERED) //){
				  && decider.shouldDeleteOldMessage(m, con.getOtherNode(getHost()))) {
			/* final recipient has already received the msg -> delete it */
			this.deleteMessage(m.getId(), false);
		}
		
		return retVal;
	}

	@Override
	/*public int receiveMessage(Message m, DTNHost from)
	{
		if(isDeliveredMessage(m) || (tombstoning && tombstones.contains(m.getId())))
			return DENIED_DELIVERED;
			
		return super.receiveMessage(m, from);
	}*/
	public int receiveMessage(Message m, DTNHost from)
    {//System.out.println(from.toString()+""+getHost().toString());
        int recvCheck = checkReceiving(m); 
        if (recvCheck != RCV_OK) {
           return recvCheck;
        }
	    if(isDeliveredMessage(m) || (tombstoning && tombstones.contains(m.getId())))
	 	   return DENIED_DELIVERED; 
	    
	   /* if( havepastsaw(m) )
	    {
	    	return DENIED_HAVESAW;
	    }*/
        return super.receiveMessage(m, from);
    }
	
	
	

	@Override
	public Message messageTransferred(String id, DTNHost from)
	{
	/*	DecisionEngineRouter otherRouter = (DecisionEngineRouter)from.getRouter();
		for( String str : otherRouter.TempGraph )
		{
			if( !TempGraph.contains(str) )
				TempGraph.add(str);
		}
		Collections.sort(TempGraph, new EventComparatortest());
		*/
		//havesawmessage.add(id);
		
		Message incoming = removeFromIncomingBuffer(id, from);
	
		if (incoming == null) {
			throw new SimError("No message with ID " + id + " in the incoming "+
					"buffer of " + getHost());
		}
		
		incoming.setReceiveTime(SimClock.getTime());
		
		Message outgoing = incoming;
		for (Application app : getApplications(incoming.getAppID())) {
			// Note that the order of applications is significant
			// since the next one gets the output of the previous.
			outgoing = app.handle(outgoing, getHost());
			if (outgoing == null) break; // Some app wanted to drop the message
		}
		
		Message aMessage = (outgoing==null)?(incoming):(outgoing);
		
		boolean isFinalRecipient = decider.isFinalDest(aMessage, getHost());
		boolean isFirstDelivery =  isFinalRecipient && 
			!isDeliveredMessage(aMessage);
		
		if (outgoing!=null && decider.shouldSaveReceivedMessage(aMessage, getHost())) 
		{
			// not the final recipient and app doesn't want to drop the message
			// -> put to buffer
			addToMessages(aMessage, false);
			
			// Determine any other connections to which to forward a message
			findConnectionsForNewMessage(aMessage, from);
		}
		
		if (isFirstDelivery)
		{
			this.deliveredMessages.put(id, aMessage);
		}
				
		int rule;
		rule = decider.findbubbleraprule( aMessage, from, getHost());
		
		
		Integer nrofCopies = (Integer)aMessage.getProperty(MSG_COUNT_PROPERTY);
		
		if (isBinary) {
			// in binary S'n'W the receiving node gets ceil(n/2) copies
			/*
			 * if(decider.othershouldDeleteSentMessage(aMessage, from)) {
			 * nrofCopies--;
			 * 
			 * }else
			 */
			if (nrofCopies <= 1)
				nrofCopies = 1;
			else
				nrofCopies = (int) Math.ceil(nrofCopies / 2.0);
		} else {
			// in standard S'n'W the receiving node gets only single copy
			nrofCopies = 1;
		}
		
		
		aMessage.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
		for (MessageListener ml : this.mListeners) {
			ml.messageTransferred(aMessage, from, getHost(),
					isFirstDelivery);
					//ml.cc(nrofCopies,isFirstDelivery, aMessage, from, getHost());
		}
		
		return aMessage;
	}

	@Override
	protected void transferDone(Connection con)
	{ 
		Message transferred = this.getMessage(con.getMessage().getId());
		
		for(Iterator<Tuple<Message, Connection>> i = outgoingMessages.iterator(); 
		i.hasNext();)
		{
			Tuple<Message, Connection> t = i.next();
			if(t.getKey().getId().equals(transferred.getId()) && 
					t.getValue().equals(con))
			{//System.out.println("2");	
				i.remove();
				break;
			}
		}
		
		if(decider.shouldDeleteSentMessage(transferred, con.getOtherNode(getHost())))
		{	
			this.deleteMessage(transferred.getId(), false);
			
			for(Iterator<Tuple<Message, Connection>> i = outgoingMessages.iterator(); 
			i.hasNext();)
			{
				Tuple<Message, Connection> t = i.next();
				if(t.getKey().getId().equals(transferred.getId()))
				{//System.out.println("3");
					i.remove();
					
				}
			}
		}
	
		
		if (transferred == null) { // message has been dropped from the buffer after..
			return; // ..start of transfer -> no need to reduce amount of copies
		}
		
		/* reduce the amount of copies left */
		Integer nrofCopies;
		nrofCopies = (Integer)transferred.getProperty(MSG_COUNT_PROPERTY);
		
		if (isBinary) {
			DTNHost othernode = con.getOtherNode(getHost());
		
			if (othernode.equals(transferred.getTo())) {

				nrofCopies = 0;
			} 
			else if( nrofCopies<= 1 )
			
			{
				nrofCopies = 0;
			}else
				nrofCopies /= 2;
		} else {
			nrofCopies--;
		}
		
		transferred.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
	}

	@Override
	/*public void update()
	{
		super.update();
		if (!canStartTransfer() || isTransferring()) {
			return; // nothing to transfer or is currently transferring 
		}
		
		tryMessagesForConnected(outgoingMessages);
		
		for(Iterator<Tuple<Message, Connection>> i = outgoingMessages.iterator(); 
		i.hasNext();)
		{
			Tuple<Message, Connection> t = i.next();
			if(!this.hasMessage(t.getKey().getId()))
			{
				i.remove();
			}
		}
	}*/
	public void update()
	{
		super.update();

		/* time to do a TTL check and drop old messages? Only if not sending */
		if (SimClock.getTime() - lastTtlCheck >= TTL_CHECK_INTERVAL && 
			sendingConnections.size() == 0) {
			
			dropExpiredMessages();
			lastTtlCheck = SimClock.getTime();
		}
		
		/*if (SimClock.getTime() - lastcomCheck >= com_CHECK_INTERVAL) {	
			//decider.updatecommunity(TempGraph, getHost());
			try{
				String str = SimClock.getTime()+"_"+getHost()+".txt";
				FileWriter output = new FileWriter(str);
				for (Iterator<String> i = TempGraph.iterator(); i.hasNext();) {
					
					String event = i.next();
					output.write(event+"\n");
				}

					output.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			for (Iterator<String> i = TempGraph.iterator(); i.hasNext();) {			
				String event = i.next();
				String[] time = event.split(" ");				
				if (Double.parseDouble(time[0]) < lastcomCheck) {
					i.remove();
				} else
					break;
			}
			lastcomCheck = SimClock.getTime();
		}*/
	
		if (!canStartTransfer() || isTransferring()) {
			return; // nothing to transfer or is currently transferring 
		} 
		
		
		// try messages that could be delivered to final recipient */
		/*if (exchangeDeliverableMessages() != null) {
			return;
		}
		*/
		
	  for(Iterator<Tuple<Message, Connection>> i = outgoingMessages.iterator(); 
				i.hasNext();)
		{		
				Tuple<Message, Connection> t = i.next();
				Integer nrofCopies = (Integer)t.getKey().getProperty(MSG_COUNT_PROPERTY);
			
				
				//if(nrofCopies <= 1 && !t.getKey().getTo().toString().equals(
				//		t.getValue().getOtherNode(getHost()).toString()))
				
				//if(nrofCopies <= 0)
				if(nrofCopies <= 0)
				{	
					i.remove();	
				}
				
			
		}
		 
			
		Tuple<Message, Connection> tt = tryMessagesForConnected(outgoingMessages);
		/*
		if (tt != null)
			deciderPickPath.add(SimClock.getTime()+" "+getHost().toString().substring(1)+" "+tt.getKey().getTo().toString().substring(1)
					+" "+tt.getValue().getOtherNode(getHost()).toString().substring(1));
		*/

		for(Iterator<Tuple<Message, Connection>> i = outgoingMessages.iterator(); 
			i.hasNext();)
		{
			Tuple<Message, Connection> t = i.next();
			if(!this.hasMessage(t.getKey().getId()))
			{
				i.remove();
			}
		}
		
		//Collections.sort(outgoingMessages, new TupleComparator());
	}
	
	
	
	
	public RoutingDecisionEngine getDecisionEngine()
	{
		return this.decider;
	}

	protected boolean shouldNotifyPeer(Connection con)
	{
		Integer i = conStates.get(con);
		return i == null || i < 1;
	}
	
	protected void findConnectionsForNewMessage(Message m, DTNHost from)
	{
		DTNHost my = getHost();
		for(Connection c : getConnections())
		{
			DTNHost other = c.getOtherNode(getHost());
			
			if(other != from && decider.shouldSendMessageToHost(m, other))
			{
				outgoingMessages.add(new Tuple<Message, Connection>(m, c));
			}
		}
	}

	@Override
	protected Tuple<Message, Connection> tryMessagesForConnected(
			List<Tuple<Message, Connection>> tuples) {
		if (tuples.size() == 0) {
			return null;
		}
		
		
		
		
		DTNHost myHost = getHost();
		Map< String, List<Tuple<Message, Connection>> >ruletable = new HashMap< String, List<Tuple<Message, Connection>>>();
		ruletable.put("dest", new  LinkedList<Tuple<Message, Connection>>());
		ruletable.put("local", new  LinkedList<Tuple<Message, Connection>>());
		ruletable.put("intocommunity", new  LinkedList<Tuple<Message, Connection>>());
		ruletable.put("global", new  LinkedList<Tuple<Message, Connection>>());
		
		
		for (Tuple<Message, Connection> t : tuples) {
			Message m = t.getKey();
			Connection con = t.getValue();
			String str = decider.FindBestConn(m, con.getOtherNode(myHost),myHost);
			String[] strs = str.split("_");
			if(strs[0].equals("dest"))
			{
				ruletable.get("dest").add(t);
			}
			else if(strs[0].equals("local")){//System.out.println(strs[0]);
				ruletable.get("local").add(t);
			}else if(strs[0].equals("intocommunity")){				
				ruletable.get("intocommunity").add(t);
			}else if(strs[0].equals("global")){
				ruletable.get("global").add(t);		
			}
			
		}
		 //System.out.println(ruletable.get("dest"));

		boolean flag = false;
		Message tmpm = null;
		Connection tmpcon = null;
		Tuple<Message, Connection> tt = null;
		for (Tuple<Message, Connection> tp : ruletable.get("dest")) {
			Message m = tp.getKey();
			Connection con = tp.getValue();
			if (m.getTo().equals(con.getOtherNode(myHost))) {
				if (startTransfer(m, con) == RCV_OK) {
					tt = tp;
					tmpm = m;
					tmpcon = con;
					flag = true;
					break;
				}

			}
		}
		if (flag) {
			for (Iterator<Tuple<Message, Connection>> i = outgoingMessages
					.iterator(); i.hasNext();) {
				Tuple<Message, Connection> pair = i.next();
				if (pair.getKey().getId().equals(tmpm.getId())
						&& !pair.getValue().equals(tmpcon)) {
					// System.out.println(tmpm.getId());
					i.remove();
				}
			}
			//System.out.println(tt);
			return tt;
		}
		
		
		Map<Message, List<Connection>> MesTable = new HashMap<Message,List<Connection>>();
		for (Tuple<Message, Connection> t : tuples) {
			Message m = t.getKey();
			Connection con = t.getValue();
			
			List<Connection> Mylist;
			if (!MesTable.containsKey(m)) {
				Mylist = new  LinkedList<Connection>();
				MesTable.put(m,Mylist);
			}else{			
				Mylist = MesTable.get(m);
			}
			Mylist.add(con);
			
		}

		for (Tuple<Message, Connection> tp : ruletable.get("local")) {
			Message m = tp.getKey();
			Connection con = tp.getValue();

			Connection bestcon = SelectConn(m, MesTable.get(m));
			// System.out.println(bestcon);
			//if (bestcon != null)
				if (startTransfer(m, bestcon) == RCV_OK)
					return new Tuple<Message, Connection>(m, bestcon);
		}

		for (Tuple<Message, Connection> tp : ruletable.get("intocommunity")) {
			Message m = tp.getKey();
			Connection con = tp.getValue();

			Connection bestcon = SelectConn(m, MesTable.get(m));
			// System.out.println(bestcon);
		//	if (bestcon != null)
				if (startTransfer(m, bestcon) == RCV_OK)
					return new Tuple<Message, Connection>(m, bestcon);
		}

		for (Tuple<Message, Connection> tp : ruletable.get("global")) {
			Message m = tp.getKey();
			Connection con = tp.getValue();

			Connection bestcon = SelectConn(m, MesTable.get(m));
			// System.out.println(bestcon);
			//if (bestcon != null)
				if (startTransfer(m, bestcon) == RCV_OK)
					return new Tuple<Message, Connection>(m, bestcon);
		}
		
	
		return null;
	}
	
	private Connection SelectConn(Message m, List<Connection> Connlist) {
		// TODO Auto-generated method stub
		
		DTNHost myHost = getHost();
		
		double local_max = 0;
		double global_max = 0;
		Connection local_max_conn = null;
		Connection global_max_conn = null;
		Connection intocommunity_conn = null;
		for( Connection con :Connlist  ){
			String str = decider.FindBestConn(m, con.getOtherNode(myHost),myHost);
			
			String[] strs = str.split("_");
			
		
			if(strs[0].equals("local")){//System.out.println(strs[0]);
				if( Double.parseDouble(strs[1]) > local_max )
				{
					local_max =  Double.parseDouble(strs[1]);	
					local_max_conn = con;
				}
			}else if(strs[0].equals("intocommunity")){		
				
				intocommunity_conn = con;
			}else if(strs[0].equals("global")){
				
				if( Double.parseDouble(strs[1]) > global_max )
				{
					global_max =  Double.parseDouble(strs[1]);	
					global_max_conn = con;
				}				
			}
		}
			
		
		if( local_max_conn != null )
		{
			return local_max_conn;}
		else if( intocommunity_conn != null ) {
			
		return intocommunity_conn;
		}
		else if( global_max_conn != null ) 
			return global_max_conn;
//System.out.println("1");
		return null;
	}
	
	private class TupleComparator implements
			Comparator<Tuple<Message, Connection>> {

		public int compare(Tuple<Message, Connection> tuple1,
				Tuple<Message, Connection> tuple2) {
			// delivery probability of tuple1's message with tuple1's connection
			double p1 = tuple1.getKey().getCreationTime();

			// -"- tuple2...
			double p2 = tuple2.getKey().getCreationTime();

			// bigger probability should come first
			if (p2 - p1 == 0) {
				/* equal probabilities -> let queue mode decide */
				// return compareByQueueMode(tuple1.getKey(), tuple2.getKey());
				return 0;
			} else if (p2 - p1 > 0) {
				return -1;
			} else {
				return 1;
			}
		}
	}
	
	class EventComparatortest  implements Comparator
	{
		public int compare(Object arg1, Object arg2) {

			  String[] str1 = ((String)arg1).split(" ");
			  String[] str2 = ((String)arg2).split(" ");
			  Double a = Double.parseDouble(str1[0]);
			  Double b = Double.parseDouble(str2[0]);
		
			   return a.compareTo(b);
			  
			 }
	}
	public boolean havepastsaw( Message m )
	{
		return this.havesawmessage.contains(m.getId());
	}
	
}
