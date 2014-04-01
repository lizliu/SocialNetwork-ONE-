package routing;
import java.util.*;



import core.*;



public class SimbetRouter extends ActiveRouter 
{
	public static final String SIMBET_NS = "SimbetRouter";
	public static final double DEFAULT_ALPHA = 0.5;	
	public static final String ALPHA_S = "alpha";
	
	protected RoutingDecisionEngine decider;
	protected Set<String> EncountVector; 
	protected ContactTable Contacmatric;
	protected ContactTable IndirectContacmatric;
	protected Set<String> havesawmessage;
	
	protected List<Tuple<Message, Connection>> outgoingMessages;
	
	/** 
	 * Used to save state machine when new connections are made. See comment in
	 * changedConnection() 
	 */
	protected Map<Connection, Integer> conStates;
	protected Map<String, Integer> EncountTable;
	private double alpha;
	private double beta;	
	
	int a[] = new int[5];
	

	public SimbetRouter(Settings s)
	{ 
		super(s);

		Settings routeSettings = new Settings(SIMBET_NS);
		
		if (routeSettings.contains(ALPHA_S)  ) {
			this.alpha = routeSettings.getDouble(ALPHA_S);
			this.beta =  1.0 - alpha;
		}
		else {
			this.alpha = DEFAULT_ALPHA;
			this.beta = 1.0 - alpha;
		}
		outgoingMessages = new LinkedList<Tuple<Message, Connection>>();
		havesawmessage = new HashSet<String>();
		EncountVector = new HashSet<String>(100);
		Contacmatric = new ContactTable();	
		conStates = new HashMap<Connection, Integer>(4);
		EncountTable = new HashMap<String, Integer>();
		//
	}

	public SimbetRouter(SimbetRouter r)
	{
		super(r);
		this.alpha = DEFAULT_ALPHA;
		this.beta = 1.0 - alpha;
		EncountVector = new HashSet<String>(100);
		havesawmessage = new HashSet<String>();
		Contacmatric = new ContactTable();	
		outgoingMessages = new LinkedList<Tuple<Message, Connection>>();
		//decider = r.decider.replicate();
		conStates = new HashMap<Connection, Integer>(4);
		EncountTable = new HashMap<String, Integer>();
	}

	@Override
	public MessageRouter replicate()
	{
		return new SimbetRouter(this);
	}


	@Override
	public boolean createNewMessage(Message m)
	{	//System.out.println("Host: " + getHost() + " Creating "+m.getId());
		//a[-1]=1;
			//if(m.getId().equals("M66"))
			//System.out.println("Host: " + getHost() + " Creating M66");
			makeRoomForNewMessage(m.getSize());
			m.setTtl(this.msgTtl);
			addToMessages(m, true); 
			havesawmessage.add(m.toString());
			findConnectionsForNewMessage(m, getHost());
			return true;
	
	}
	
	@Override
	public void changedConnection(Connection con)
	{	//a[-1]=1;
		DTNHost myHost = getHost();
		DTNHost otherNode = con.getOtherNode(myHost);

		SimbetRouter otherRouter = (SimbetRouter)otherNode.getRouter();
		
		if(con.isUp())
		{	
			//Lock
			if(shouldNotifyPeer(con))
			{		
				this.doExchange(con, otherNode);
				otherRouter.didExchange(con);
				
				EncountVector.add( otherNode.toString() );
				otherRouter.EncountVector.add( myHost.toString() );
				
				if(Contacmatric.IsNull())
				{
					Contacmatric.initialContactTable(getHost().toString());			
				}
				
				if( otherRouter.Contacmatric.IsNull() )
				{
					otherRouter.Contacmatric.initialContactTable(otherNode.toString());
				}
				
			}
			
			UpdateSimilar(myHost, otherNode);
			Contacmatric.AddNode( otherNode );
			
			/*
			 * Once we have new information computed for the peer, we figure out if
			 * there are any messages that should get sent to this peer.
			 */
			Collection<Message> msgs = getMessageCollection();
			for(Message m : msgs)
			{		
				if( decideSendMsg( m, otherNode, myHost ) )
				{
					outgoingMessages.add(new Tuple<Message,Connection>(m, con));
				}
			}
			
		}
		else
		{
			//Contacmatric.DeleteNode( otherNode );		
			conStates.remove(con);
			
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
		
		retVal = con.startTransfer(getHost(), m);
		if (retVal == RCV_OK) { // started transfer
			addToSendingConnections(con);
		}
		else if( retVal == DENIED_DELIVERED )
		{
			this.deleteMessage(m.getId(), false);//update outgoingMessages
			
		}
		
		return retVal;
	}

	@Override
	public int receiveMessage(Message m, DTNHost from)
    { 	
        int recvCheck = checkReceiving(m); 
        if (recvCheck != RCV_OK) {
           return recvCheck;
        }
	    if(isDeliveredMessage(m) )
	 	   return DENIED_DELIVERED; 
	    
	    //假如這個host已經成經收過這個message了，則別再傳一次了
	   /* if( havepastsaw(m) )
	    {
	    	return DENIED_HAVESAW;
	    }*/
        return super.receiveMessage(m, from);
    }
	

	@Override
	public Message messageTransferred(String id, DTNHost from)
	{ 
		havesawmessage.add(id);
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
		
		boolean isFinalRecipient = isFinalDest(aMessage, getHost());
		boolean isFirstDelivery =  isFinalRecipient && 
			!isDeliveredMessage(aMessage);
		
		if (outgoing!=null && shouldSaveReceivedMessage(aMessage, getHost())) 
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
		int rule = 0;//no use
	    //	System.out.println(aMessage.getId()+"--"+from.toString()+"--"+ getHost().toString());
		for (MessageListener ml : this.mListeners) {
			ml.messageTransferred(aMessage, from, getHost(),
					isFirstDelivery);
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
			{

				i.remove();
				break;
			}
		}
			
		/*this.deleteMessage(transferred.getId(), false);
		
		for(Iterator<Tuple<Message, Connection>> i = outgoingMessages.iterator(); 
		i.hasNext();)
		{
			Tuple<Message, Connection> t = i.next();
			if(t.getKey().getId().equals(transferred.getId()))		
			{
				i.remove();			
			}
		}	
		*/
	}


	@Override
	public void update()
	{	// a[-1] = 0;
		super.update();

		if (!canStartTransfer() || isTransferring()) {
			return; // nothing to transfer or is currently transferring 
		} 

		tryMessagesForConnected(outgoingMessages); 
		
		//當message因為ttl被drop掉後，要將outgoingMessages裡的含有相同message的key remove掉
		for(Iterator<Tuple<Message, Connection>> i = outgoingMessages.iterator(); 
			i.hasNext();)
		{	
			Tuple<Message, Connection> t = i.next();
			if(!this.hasMessage(t.getKey().getId()))
			{	
				i.remove();//System.out.println("!!!");	
			}
		}
		
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
			//System.out.println(from.toString()+" "+other.toString()+" "+m.getId());
			if(other != from && decideSendMsg(m, other, my))
			{
				outgoingMessages.add(new Tuple<Message, Connection>(m, c));
			}
		}
			
	}
	
	protected boolean TablecontactedWith( String from )
	{
		return Contacmatric.Tablecontains( from ) ;
	}
	
	protected boolean decideSendMsg( Message m, DTNHost otherNode, DTNHost myHost )
	{
		if( otherNode == m.getTo()  )
		{
			return true;
		}
		int SimUtiln = getSimilar( myHost,  m );
		int SimUtilm = getSimilar( otherNode, m );				
		//System.out.println(SimUtiln + "--" + SimUtilm);
		double BetUtiln = (( SimbetRouter )myHost.getRouter()).Contacmatric.computeBetweenness( );
		double BetUtilm = (( SimbetRouter )otherNode.getRouter()).Contacmatric.computeBetweenness( );
		
		double SimUtiln_d, SimUtilm_d;
		if( ( SimUtiln + SimUtilm ) == 0  )
		{
			 SimUtiln_d = 0;
			 SimUtilm_d = 0;
		}
		else
		{
			 SimUtiln_d = (double) SimUtiln / ( SimUtiln + SimUtilm );
			 SimUtilm_d = (double) SimUtilm / ( SimUtiln + SimUtilm );
		}
		
		double BetUtiln_d, BetUtilm_d;
		if( ( BetUtiln + BetUtilm) == 0 )
		{
			 BetUtiln_d = 0;
			 BetUtilm_d = 0;//System.out.println( BetUtiln_d + "  " + BetUtilm_d );
		}
		else
		{
			 BetUtiln_d = BetUtiln / ( BetUtiln + BetUtilm );
			 BetUtilm_d = BetUtilm / ( BetUtiln + BetUtilm );		
		}				
		
		double simBetUtiln_d = alpha * SimUtiln_d + beta * BetUtiln_d;
		double simBetUtilm_d = alpha * SimUtilm_d + beta * BetUtilm_d;
		//System.out.println(simBetUtiln_d + "  " + simBetUtilm_d);
		//System.out.println(m.toString() +" "+  otherNode+ " "+ myHost);
		if( simBetUtiln_d < simBetUtilm_d )
		{
			return true;
		}
		
		return false;
		
	}
	
	protected ContactTable getContactTable()
	{
		
		return this.Contacmatric ;
		
	}
	
	protected Map<String, Integer> getEncountTable()
	{
		
		return this.EncountTable ;
		
	}
	protected  Set<String> getEncountVector()
	{
		
		return this.EncountVector ;
		
	}
	
	/*protected int getSimilar( DTNHost Node, Message m )
	{	
		
		Map<String, Integer>  myEncountTable = (( SimbetRouter )Node.getRouter()).getEncountTable();
		ContactTable  myContactTable = (( SimbetRouter )Node.getRouter()).getContactTable();
		if( myEncountTable.containsKey(m.getTo().toString()) )
		{
			return myEncountTable.get(m.getTo().toString());
		}else
		{
			return myContactTable.getindirectSim( m );
		}
		
	}*/
	protected int getSimilar( DTNHost Node, Message m )
	{	
		
		
		ContactTable  myContactTable = (( SimbetRouter )Node.getRouter()).getContactTable();
		
		int simvalue = 0;
		if( myContactTable.Tablecontains( m.getTo().toString() ) )
		{
			simvalue = myContactTable.computeSimilar( m );
			//System.out.println(simvalue);		
		}else if( myContactTable.IndirectTablecontains( m.getTo().toString() ) )
		{
			simvalue = myContactTable.getindirectSim( m );
		}
			
		
		return simvalue;	
	}
	
	public boolean isFinalDest(Message m, DTNHost aHost)
	{
		return m.getTo() == aHost; // Unicast Routing
	}
	
	public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost)
	{
		return m.getTo() != thisHost;
	}
	public boolean havepastsaw( Message m )
	{
		return this.havesawmessage.contains(m.getId());
	}
	
	public void UpdateSimilar( DTNHost my, DTNHost other )
	{
		Set<String> myVector = (( SimbetRouter )my.getRouter()).getEncountVector();
		Set<String> otherVector = (( SimbetRouter )other.getRouter()).getEncountVector();
		//System.out.println(my + " : " + myVector);
		int count = 0;
		for( String h : otherVector )
		{	
			//System.out.print(h + " ");
			if( myVector.contains( h ) )
			{//System.out.print(count);
				count++;
			}
		}
		//System.out.println();
		this.EncountTable.put(other.toString(), new Integer(count));
		//System.out.println(other.toString()+otherVector);
	}
	
}
