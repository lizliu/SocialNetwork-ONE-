package routing;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.Settings;

public class Degree extends ActiveRouter {
	public int degrees;

	public Degree(Settings s) {
		super(s);
		// TODO Auto-generated constructor stub
	}

	public Degree(Degree r) {
		super(r);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void init(DTNHost host, List<MessageListener> mListeners) {
		super.init(host, mListeners);
		read_social(host.toString());
	}

	public void read_social(String hostname) {
		try {
			String filepath = "ee\\Friends.txt";
			FileReader fr = new FileReader(filepath);
			BufferedReader br = new BufferedReader(fr);
			String s;
			int temp = 0;
			while ((s = br.readLine()) != null) {
				String[] line = s.split("\t");
				if (line[0].toString().equals(hostname) && line.length > 1) {
					String[] line2 = line[1].split(",");
					temp = line2.length;
					break;
				}
			}
			this.degrees = temp;
			fr.close();
			br.close();
		} catch (Exception e) {
			System.out.println("Parser Error:");
			e.printStackTrace();
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
		shouldSendMessageToHost();
	}

	public int getDegree() {
		return this.degrees;
	}

	public Connection shouldSendMessageToHost() {
		List<Connection> connections = new ArrayList<Connection>();
		List<Connection> cons = getConnections();
		for (Connection c : cons) {
			DTNHost otherHost = c.getOtherNode(getHost());
			Degree DRouter = (Degree) otherHost.getRouter();
			if (DRouter.getDegree() >= this.getDegree())
				connections.add(c);
		}

		if (connections.size() == 0 || this.getNrofMessages() == 0) {
			return null;
		}

		List<Message> messages = new ArrayList<Message>(
				this.getMessageCollection());
		this.sortByQueueMode(messages);
		return tryMessagesToConnections(messages, connections);
	}

	@Override
	public Degree replicate() {
		// TODO Auto-generated method stub
		return new Degree(this);
	}

}
