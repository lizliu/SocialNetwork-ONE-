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

public class PeopleRank extends ActiveRouter {
	public double peoplerank_value;
	private Set<String> peoplerank_friends;

	public PeopleRank(Settings s) {
		super(s);
		// TODO Auto-generated constructor stub
	}

	public PeopleRank(PeopleRank r) {
		super(r);
		this.peoplerank_friends = new HashSet<String>();
		// TODO Auto-generated constructor stub
	}

	@Override
	public void init(DTNHost host, List<MessageListener> mListeners) {
		super.init(host, mListeners);
		read_social(host.toString().substring(1));
		read_friends(host.toString());
	}

	public void read_social(String hostname) {
		try {
			//String filepath = "ee\\Social_PeopleRank.txt";
			String filepath = "ee\\Social_Weighted_PeopleRank.txt";
			FileReader fr = new FileReader(filepath);
			BufferedReader br = new BufferedReader(fr);
			String s;
			double temp = 0;
			while ((s = br.readLine()) != null) {
				String[] line = s.split("\t");
				if (line[0].toString().equals(hostname)) {
					temp = Double.parseDouble(line[1]);
					break;
				}
			}
			this.peoplerank_value = temp;
			fr.close();
			br.close();
		} catch (Exception e) {
			System.out.println("Parser Error:");
			e.printStackTrace();
		}
	}

	public void read_friends(String hostname) {
		try {
			String filepath = "ee\\Friends.txt";
			FileReader fr = new FileReader(filepath);
			BufferedReader br = new BufferedReader(fr);
			String s;
			while ((s = br.readLine()) != null) {
				String[] line1 = s.split("\t");
				if (line1[0].toString().equals(hostname)) {
					String[] line2 = s.split(",");
					for (String str : line2)
						peoplerank_friends.add(str);
					break;
				}
			}

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
		// System.out.println(getHost().toString()+"\t"+this.peoplerank_value);
		if (!canStartTransfer() || isTransferring()) {
			return; // nothing to transfer or is currently transferring
		}
		/* try messages that could be delivered to final recipient */
		if (exchangeDeliverableMessages() != null) {
			return;
		}
		shouldSendMessageToHost();
	}

	public double getpeoplerank_value() {
		return this.peoplerank_value;
	}

	public Connection shouldSendMessageToHost() {
		List<Connection> connections = new ArrayList<Connection>();
		List<Connection> cons = getConnections();
		for (Connection c : cons) {
			DTNHost otherHost = c.getOtherNode(getHost());
			PeopleRank DRouter = (PeopleRank) otherHost.getRouter();
			if (DRouter.getpeoplerank_value() >= this.getpeoplerank_value())
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
	public PeopleRank replicate() {
		// TODO Auto-generated method stub
		return new PeopleRank(this);
	}

}
