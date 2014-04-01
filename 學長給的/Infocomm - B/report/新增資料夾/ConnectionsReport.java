/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package report;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Map;
import core.ConnectionListener;
import core.DTNHost;
import core.SimClock;

/**
 * This report counts the number of contacts each hour
 * 
 * @author Frans Ekman
 */
public class ConnectionsReport extends Report implements ConnectionListener {

	private LinkedList<Integer> contactCounts;
	private int currentHourCount;
	private int currentHour;
	private Map<String, Integer> ConnectionCount;
	public ConnectionsReport() {
		init();
	}
	
	@Override
	public void init() {
		super.init();
		contactCounts = new LinkedList<Integer>();
		ConnectionCount = new HashMap <String, Integer>();
	}
	
	public void hostsConnected(DTNHost host1, DTNHost host2) {
		int time = SimClock.getIntTime() / 5000;
		while (Math.floor(time) > currentHour) {
			contactCounts.add(new Integer(currentHourCount));
			currentHourCount = 0;
			currentHour++;
		} 
		if( !ConnectionCount.containsKey(host1.toString()) )
		{
			ConnectionCount.put(host1.toString(), new Integer(1));
		}else
		{
			Integer i = ConnectionCount.get(host1.toString());
			i = i + 1;
			ConnectionCount.put(host1.toString(), i);
		}
		
		
		if( !ConnectionCount.containsKey(host2.toString()) )
		{
			ConnectionCount.put(host2.toString(), new Integer(1));
		}else
		{
			Integer j = ConnectionCount.get(host2.toString());
			j = j + 1;
			ConnectionCount.put(host2.toString(), j);
		}
		currentHourCount++;
	}

	public void hostsDisconnected(DTNHost host1, DTNHost host2) {
		// Do nothing
	}

	public void done() {
		
		for( String key: this.ConnectionCount.keySet() )
		{
			write( key.substring(1) + "\t" + ConnectionCount.get(key) );
		}	
		write("");
		
		Iterator<Integer> iterator = contactCounts.iterator();
		int hour = 5;
		while (iterator.hasNext()) {
			Integer count = (Integer)iterator.next();
			write(hour + "\t" + count);
			hour += 5;
		}
		super.done();
	}
	
}
