package routing;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;

public class FrequencyRouter extends ActiveRouter {
	protected int totalNrofContacts;

	public FrequencyRouter(Settings s) {
		super(s);
		totalNrofContacts = 0;
		// TODO Auto-generated constructor stub
	}

	public FrequencyRouter(FrequencyRouter r) {
		super(r);
		this.totalNrofContacts = r.totalNrofContacts;
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void changedConnection(Connection con) {
		if (con.isUp()) {
			this.totalNrofContacts++;
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
		List<Connection> connections = new LinkedList<Connection>();
		for(Connection con : getConnections()){
			DTNHost otherHost = con.getOtherNode(getHost());
			FrequencyRouter FRRouter = (FrequencyRouter)otherHost.getRouter();
			if(FRRouter.gettotalNrofContacts() >= this.gettotalNrofContacts())
				connections.add(con);
		}
		if (connections.size() == 0 || this.getNrofMessages() == 0) {
			return null;
		}
		List<Message> messages = 
			new ArrayList<Message>(this.getMessageCollection());
		this.sortByQueueMode(messages);

		return tryMessagesToConnections(messages, connections);
	}
	
	public int gettotalNrofContacts(){
		return totalNrofContacts;
	}
	
	@Override
	public FrequencyRouter replicate() {
		// TODO Auto-generated method stub
		return new FrequencyRouter(this);
	}

}
