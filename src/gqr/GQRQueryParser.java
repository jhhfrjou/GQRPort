package gqr;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import datalog.DatalogParser;
import datalog.DatalogScanner;

/**
 * 
 * @author george konstantinidis (george@konstantinidis.org)
 * Parser for safe conjunctive datalog-like rules(user queries or views).
 */
public class GQRQueryParser {

	// PRIVATE //
	private final File fFile;
	private int readupto = 0;
	private int limit = 0;
	private DatalogQuery query = null;
	private Scanner sc = null;

	/**
	 * @param file full name of an existing, readable file.
	 * @param limit number of line of the file up to which it will parse.
	 */
	public GQRQueryParser(File file,int limit){
		this.limit = limit;
		fFile = file;
		try {
			sc = new Scanner(fFile);
		} catch (FileNotFoundException e1) {
			throw new RuntimeException(e1);
		}
	}

	/**
	 * Parses one line of the file given to the parser and creates one query. Each time called gets next line.
	 * In EOF is reached (in last call) it returns null
	 * @return a DatalogQuery
	 */
	public DatalogQuery parseNextQuery()
	{
		if(sc.hasNextLine() && readupto<=limit)
		{
			readupto++;
//			System.out.println(readupto+"here");
		
			String next= sc.nextLine();
//			System.out.println(next);
			DatalogScanner scanner = new DatalogScanner(new StringReader(next));
			DatalogParser parser = new DatalogParser(scanner);
			try{
				query = parser.query();
				
			} catch (RecognitionException re) {
				throw new RuntimeException(re);
			} catch (TokenStreamException e) {
				throw new RuntimeException(e);
			}
		}else
			return null;
		return query;
	}

	//	/**   */
	//	  public final void processLineByLine() throws FileNotFoundException {
	//	    Scanner sc = new Scanner(fFile);
	//	    try {
	//	      //first use a Scanner to get each line
	//	    	SourceQuery sq1 = null;
	//	    	boolean firstTimeInTheLoop = true;
	//	      while ( sc.hasNextLine() ){
	//				DatalogScanner scanner = new DatalogScanner(new StringReader(sc.nextLine()));
	//				DatalogParser parser = new DatalogParser(scanner);
	//				try{
	//				query = parser.query();
	//				
	//
	//				
	//				if(!firstTimeInTheLoop)
	//				{
	//					System.out.println("First query contained in the second?-->"+sq1.isContained(Util.castQueryAsISISourceQuery(query)));
	//				}
	//				
	//				if(firstTimeInTheLoop)
	//				{
	//					sq1 = Util.castQueryAsISISourceQuery(query);
	//					firstTimeInTheLoop = false;
	//				}
	//				
	//				query.getQueryPJs();
	//				System.out.println(query);
	//				} catch (RecognitionException re) {
	//					throw new RuntimeException(re);
	//				} catch (TokenStreamException e) {
	//					throw new RuntimeException(e);
	//				}
	//	      }
	//	    }
	//	    finally {
	//	      //ensure the underlying stream is always closed
	//	      sc.close();
	//	    }
	//	  }


	//	/**
	//	 * Returns last query read by the parseQuery method.
	//	 */
	//	public DatalogQuery getQuery() {
	//		return query;
	//	}

	//	private void check(DatalogQuery query) {
	//		
	//		for(Variable v:query.getExistentialVariables())
	//		{
	//			if(!v.isExistential())
	//				System.out.println("ERRROR---> I didn't find this free:\n\t Query: "+query+"\n\t Variable: "+v);
	//		}
	//		for(Variable v:query.getHeadVariables())
	//		{
	//			if(v.isExistential())
	//				System.out.println("ERRROR---> I mistakenly found this as free:\n\t Query: "+query+"\n\t Variable: "+v);
	//		}	
	//	}

	//	private void breakdown(DatalogQuery query) {
	//		
	//	}






}
