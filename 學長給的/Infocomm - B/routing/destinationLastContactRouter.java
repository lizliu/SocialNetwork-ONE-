package routing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;

public class destinationLastContactRouter extends ActiveRouter {
	private Map<DTNHost, Double> recently;

	public destinationLastContactRouter(Settings s) {
		super(s);
		// TODO Auto-generated constructor stub
		initRecently();
	}

	public destinationLastContactRouter(destinationLastContactRouter r) {
		super(r);
		// TODO Auto-generated constructor stub
		initRecently();
	}

	private void initRecently() {
		this.recently = new HashMap<DTNHost, Double>();
	}

	@Override
	public void changedConnection(Connection con) {
		if (con.isUp()) {
			DTNHost otherHost = con.getOtherNode(getHost());
			recently.put(otherHost, SimClock.getTime());
		}
		else {
			DTNHost otherHost = con.getOtherNode(getHost());
			recently.put(otherHost, SimClock.getTime());
		}
	}

	@Override
	public void update() {
		super.update();
		if (!canStartTransfer() || isTransferring()) {
			return; // nothing to transfer or is currently transferring
		}
		/* try messages that could be delivered to final recipient */
		if (exchangeDeliverableMessages() != null) {
			return;
		}
		this.tryAllMessagesToAllConnections();
	}

	@Override
	protected Connection tryAllMessagesToAllConnections(){
		List<Connection> connections = getConnections();
		if (connections.size() == 0 || this.getNrofMessages() == 0) {
			return null;
		}
		
		List<Message> messages = 
			new ArrayList<Message>(this.getMessageCollection());
		this.sortByQueueMode(messages);
		
		for(Message m : messages){
			for(Connection c : connections){
				DTNHost otherHost = c.getOtherNode(getHost());
				destinationLastContactRouter desRouter = (destinationLastContactRouter)otherHost.getRouter();
				if(desRouter.getRecentlyTime(m.getTo()) > this.getRecentlyTime(m.getTo())){
					int retVal = startTransfer(m, c); 
					if (retVal == RCV_OK) {
						return c;	// accepted a message, don't try others
					}
					else if (retVal > 0) { 
						return null; // should try later -> don't bother trying others
					}
				}
			}
		}
		return null;
	}

	public Double getRecentlyTime(DTNHost to) {
		if(recently.containsKey(to))
			return recently.get(to);
		else
			return -1.0;
	}

	@Override
	public destinationLastContactRouter replicate() {
		// TODO Auto-generated method stub
		return new destinationLastContactRouter(this);
	}

}
