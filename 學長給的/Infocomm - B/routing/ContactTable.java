package routing;
import java.util.*;

import core.*;

public class ContactTable {
	
	private Map< String, Map< String, Integer > > Table;
	private Map< String, Map< String, Integer > > IndirectTable;
	private String host;
	
	public ContactTable(  )
	{	
		
	}
	
	public void initialContactTable( String host )
	{	
		this.host = host;
		Table = new  LinkedHashMap< String, Map< String, Integer > >();
		Table.put( host, new LinkedHashMap<String, Integer >() );
		Table.get( host ).put( host, new Integer( 0 ) );
		//System.out.println(Table.get(host).get(host));
		IndirectTable = new  LinkedHashMap< String, Map< String, Integer > >();
		IndirectTable.put( host, new LinkedHashMap<String, Integer >() );
		
	}
	
	public void AddNode( DTNHost b )
	{//PrintTable();
		if ( Table.containsKey( b.toString() ) ){
			return ;
		}
		
		if ( Table.get( host ).containsKey( b.toString() ) ){		
			return ;			
		}
		//PrintTable1();
		Table.put( b.toString(), new LinkedHashMap<String, Integer >() ) ;
		//System.out.println(host + " " + b.toString());
		
		for ( String key : Table.keySet() ) {
			
			if ( key.equals( host ) )
			{	//System.out.println(key+host);	
				
				Table.get( key ).put( b.toString(), new Integer( 1 ) ) ;
				Table.get( b.toString() ).put( key, new Integer( 1 ) ) ;//PrintTable1();
					
			}
			else if ( key.equals( b.toString() ) )
			{
				Table.get( key ).put( b.toString(), new Integer( 0 ) ) ;
			}
			else if ( ( ( SimbetRouter )b.getRouter() ).TablecontactedWith( key ) ) 
			{
			    Table.get( key ).put( b.toString(), new Integer( 1 ) ) ;
			    Table.get( b.toString() ).put( key, new Integer( 1 ) ) ;
			}
			else if( !( ( SimbetRouter )b.getRouter() ).TablecontactedWith( key ) ) 
			{
				Table.get( key ).put( b.toString(), new Integer( 0 ) ) ;
				Table.get( b.toString() ).put( key, new Integer( 0 ) ) ;
			}
		}
		
		//if( Table.get(host).containsKey(b.toString()) )
		//{
			for ( String key : IndirectTable.keySet() ) {	
				IndirectTable.get(key).remove(b.toString());	
			}
			//return;
		//}			
		IndirectTable.put( b.toString(), new LinkedHashMap<String, Integer >() ) ;
		for ( String key : IndirectTable.get(host).keySet() ) {	
			if( ( ( SimbetRouter )b.getRouter() ).TablecontactedWith( key ) )
				IndirectTable.get(b.toString()).put(key,  new Integer( 1 ) );
			else
				IndirectTable.get(b.toString()).put(key,  new Integer( 0 ) );
		}
		
		
		//System.out.println(host + " " + b.toString());
		ContactTable otherContactTable = ((SimbetRouter)b.getRouter()).getContactTable();
		//otherContactTable.PrintTable();
		for ( String key : otherContactTable.Table.keySet() ) {
			if ( !Table.containsKey(key) ) 
			{			
				for ( String key1 : IndirectTable.keySet() ) {
					List<DTNHost> nodes = SimScenario.getInstance().getHosts();
					//System.out.println(IndirectTable.size());
					for(DTNHost h : nodes)
					{
						//System.out.println( h.toString() + key1);
						if( h.toString().equals(key1) )
						{
							//System.out.println( "yes");
							if ( ( ( SimbetRouter )h.getRouter() ).TablecontactedWith( key ) )
							{
								IndirectTable.get(key1).put(key, new Integer(1));
							}else
							{
								IndirectTable.get(key1).put(key, new Integer(0));
							}
							break;
						}						
					}	
				}//key1		
			}//if
		}//key
		//System.out.println(host);
		//PrintTable();
	}
	
	public void DeleteNode( DTNHost b  )
	{	
		Table.remove( b.toString() );
		
		for ( String key : Table.keySet() ) {	
			Table.get(key).remove(b.toString());	
		}

	}
	
	public boolean Tablecontains( String from )
	{	
		//PrintTable();	
		return Table.containsKey(from);		
	}
	
	public boolean IndirectTablecontains( String from )
	{	
		//PrintTable();	
		return IndirectTable.containsKey(from);		
	}
	
	public int getindirectSim( Message m )
	{	
		//PrintTable();	
			int sumsimilar = 0;
			if (IndirectTable.get(host).containsKey(m.toString())){
				for (String key : IndirectTable.keySet())
				{
					sumsimilar += IndirectTable.get(key).get(m.toString());
					
				}
			}
				return sumsimilar;
		
	}
	public int getsize()
	{	
		return Table.size();
	}
	
	public double computeBetweenness( )
	{	
		int size = this.getsize();
		
		//System.out.println( size  + "  " + Table.get(host).size());
		
		int i = 0, j = 0;
		int A[][] = new int[size][size];	
		int B[][] = new int[size][size];
		int C[][] = new int[size][size];
		int D[][] = new int[size][size];
		//PrintTable();	
		for ( String row : Table.keySet() ) {
			j = 0;
			for ( String col : Table.get(row).keySet() ) {	
				A[i][j] = Table.get(row).get(col);
				j++;
			}
			i++;		
		}
		
		/*for (int m = 0; m < B.length; m++) {
			for (int p = 0; p < B[0].length; p++) {
				System.out.print(B[m][p]+" ");
			}	
			System.out.println();
		}
		System.out.println();*/
		
		//A*A
		for (int m = 0; m < A.length; m++) {
			for (int p = 0; p < A[0].length; p++) {
				for (int n = 0; n < A[0].length; n++) {
					B[m][p] = B[m][p] + A[m][n] * A[n][p];
				}
			}
		}

		//1-A
		for( int row = 0; row < A.length; row++ )
		{
		    for( int col = 0; col < A.length; col++ )
			{
		        C[row][col] = 1 - A[row][col];
			}
	    }
		
		//A*A*[1-A]
		for (int m = 0; m < A.length; m++) {
			for (int p = 0; p < A[0].length; p++) {
				for (int n = 0; n < A[0].length; n++) {
					D[m][p] = D[m][p] + B[m][n] * C[n][p];
				}
			}
		}

		double reciprocal = 0.0;
		for( int row = 0; row < A.length; row++ )
		{
		    for( int col = 0; col < A.length; col++ )
			{
		    	if( C[row][col] != 0.0 )
		    		reciprocal += 1 / C[row][col];
			}
	    }
		
		//System.out.println(reciprocal);
		return reciprocal;
	}
	
	public int computeSimilar( Message m )
	{
		int sim = 0;
		for ( String key : Table.keySet()) {
			
			if( Table.get(host).get(key) == 1 && Table.get(m.getTo().toString()).get(key) == 1 )
				sim++;
		}
		
		return sim;
		
	}
	
	public void PrintTable1()
	{
		System.out.print( "     " );
		for ( String key2 : Table.get(host).keySet() ) {		
					System.out.print( key2 + " " );
		}
		System.out.println();
		
		for ( String key1 : Table.keySet() ) {
			System.out.print( key1 + " | " );
			for ( String key2 : Table.get(key1).keySet() ) {		
					System.out.print(  Table.get(key1).get(key2) + "   " );
			}
			System.out.println();
		}

		System.out.println( "--------------------------------");
		
	}
	
	public void PrintTable()
	{
		
		for ( String key1 : Table.keySet() ) {
			for ( String key2 : Table.get(key1).keySet() ) {		
					System.out.print(  Table.get(key1).get(key2) + " " );
			}
			System.out.println();
		}

		System.out.println( "--------------------------------");
		
	}
	
	public boolean IsNull()
	{
		return Table == null;
	}

}
