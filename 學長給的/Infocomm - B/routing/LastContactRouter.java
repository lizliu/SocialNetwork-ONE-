package routing;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;

public class LastContactRouter extends ActiveRouter {
	protected double recently;
	public LastContactRouter(Settings s) {
		super(s);
		// TODO Auto-generated constructor stub
		recently = 0;
	}

	public LastContactRouter(LastContactRouter r) {
		super(r);
		// TODO Auto-generated constructor stub
		this.recently = r.recently;
	}
	@Override
	public void changedConnection(Connection con) {
		if (con.isUp()) {
			recently = SimClock.getTime();
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
			LastContactRouter lcRouter = (LastContactRouter)otherHost.getRouter();
			if(lcRouter.getRecentlyTime() >= this.getRecentlyTime())
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
	public Double getRecentlyTime() {
		return recently;
	}
	@Override
	public LastContactRouter replicate() {
		// TODO Auto-generated method stub
		return new LastContactRouter(this);
	}

}
