package com.knowesis.sift.bricks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.restlet.routing.Template;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class Bricks {

	List< Object[] > _allBricks;
	HashMap< String, String > _logicalOperatorsToJava;
	HashMap< String, String > _englishToRegex;
	JsonArray _allBricksJsonArray;
	
	public Bricks(){
		super();
		_allBricks = new ArrayList< Object[] >();
		_allBricksJsonArray = new JsonArray();
		
		_logicalOperatorsToJava = new LinkedHashMap< String, String >();
		_logicalOperatorsToJava.put( "not equals to", "equals" );
		_logicalOperatorsToJava.put( "not equal to", " != " );
		_logicalOperatorsToJava.put( "equals to", "equals" );
		_logicalOperatorsToJava.put( "greater than or equal to", " >= " );
		_logicalOperatorsToJava.put( "less than or equal to", " <= " );
		_logicalOperatorsToJava.put( "greater than", " > " );
		_logicalOperatorsToJava.put( "less than", " < " );
		_logicalOperatorsToJava.put( "equal to", " == " );
		_logicalOperatorsToJava.put( "does not contain", "contains" );
		_logicalOperatorsToJava.put( "does not start with", "startsWith" );
		_logicalOperatorsToJava.put( "does not end with", "endsWith" );
		_logicalOperatorsToJava.put( "contains", "contains" );
		_logicalOperatorsToJava.put( "starts with", "startsWith" );
		_logicalOperatorsToJava.put( "ends with", "endsWith" );
		//_logicalOperatorsToJava.put( "NOT", " ! " );
		
		
		_englishToRegex = new HashMap< String, String >();
		_englishToRegex.put( "NumericOperator", "(greater than|less than|greater than or equal to|less than or equal to|equal to|not equal to)" );
		_englishToRegex.put( "StringOperator", "(equals to|not equals to|contains|does not contain|starts with|ends with|does not start with|does not end with)" );
//		_englishToRegex.put( "StringValue", "([a-zA-Z0-9_\"]+)" );
		_englishToRegex.put( "StringValue", "(.+)" );
		_englishToRegex.put( "NumericValue", "(-?[0-9]*\\.?[0-9])" );
		_englishToRegex.put( "openBracketToRegex", "((\\(\\s?)*)" );
		_englishToRegex.put( "closeBracketToRegex", "\\s?((\\)\\s?)*)" );
		_englishToRegex.put( "spacesToRegex", "\\s?" );
		_englishToRegex.put( "logicalConnectorsToRegex", "(AND|OR)?" );
		_englishToRegex.put( "OfferId", "([a-zA-Z0-9_\"]+)" );
		_englishToRegex.put( "ProgramId", "([a-zA-Z0-9_\"]+)" );
	}

	public Function< String, String >normalizeSpaces = ( input ) -> input.replaceAll( "( )+", " " );
	public Function< String, String >normalizeNewline = ( input ) -> input.replaceAll( "(\\(\\n\\()+", "((" );
	
	public void addSiftTypesToEngToRegex(Map<String, String> siftTypesObj) {
		if(siftTypesObj != null) {
			Iterator<String> iter = siftTypesObj.keySet().iterator();
			while(iter.hasNext()) {
				String key = iter.next();
				_englishToRegex.put(key, "([a-zA-Z0-9_>=<! \"]+)");
			}
		}
	}
	
	public void addBrick( String annotatedStmt, String javaTemplate ) {
		Object[] aBrick = new Object[ 4 ];
		aBrick[ 0 ] = annotatedStmt;
		aBrick[ 1 ] = this.createRegex( annotatedStmt  );
//		aBrick[ 2 ] = Pattern.compile( regex );
		aBrick[ 2 ] = Pattern.compile( ( String )aBrick[ 1 ] );
		aBrick[ 3 ] = javaTemplate;
		_allBricks.add( aBrick );
		
		_allBricksJsonArray.add( new JsonPrimitive( annotatedStmt ) );
	}
	
	
	public JsonObject getTypeAheadMatches( String brickSnippet ) {
		String[] allStatements = brickSnippet.split( "\\n" );
		String brickStatement = allStatements[ allStatements.length - 1 ];
		List< Object[] > matchingList = _allBricks.stream()
				.filter( ( input ) -> ( ( ( String )input[ 0 ] ).toUpperCase().contains( brickStatement.toUpperCase() ) ) )
				.collect( Collectors.toList() );
		JsonObject result = new JsonObject();
		result.addProperty( "status", "success" );
		JsonArray resultArray = new JsonArray();
		if( matchingList != null && matchingList.size() == 0 )  
			result.add( "result", _allBricksJsonArray );
		else {
		    matchingList.forEach( ( aBrick ) -> resultArray.add( new JsonPrimitive( ( String )aBrick[ 0 ]) ) );
		    result.add( "result", resultArray );
		}    
		return result;
	}

	
	public boolean validateMatchingParanthesis( String input ) {
		input = normalizeSpaces.apply( input );
		Stack< Integer > stack = new Stack< Integer >();
		input.chars().forEach( ( ch ) -> {
			if( ch == '(' ) stack.push( ch );
			else if( ch == ')' && !stack.isEmpty() && stack.peek() == '(' ) stack.pop();
			else if( ch == ')' && stack.isEmpty() ) stack.push( ch );
		} );
		return stack.isEmpty();
	}
	
	
	public JsonObject validate( String brickSnippet ) {
		brickSnippet = normalizeSpaces.apply( brickSnippet );
		brickSnippet = normalizeNewline.apply( brickSnippet );
		String[] allStatements = brickSnippet.split( "\\n" );
		Predicate< String > invalidStatementFilter = ( input ) ->  ! _allBricks.stream().
					anyMatch( ( aBrick ) -> ( ( Pattern ) aBrick[ 2 ] ).matcher( input ).matches()  );
		JsonArray errors = new JsonArray();
		if( ! validateMatchingParanthesis( brickSnippet ) )
			errors.add( new JsonPrimitive( "Paranthesis does not match" ) );	
		for( int i=0; i<allStatements.length; i++ ) {
			if( ( i == ( allStatements.length - 1 ) && ( allStatements[ i ].trim().endsWith( "AND" ) || allStatements[ i ].trim().endsWith( "OR" ) ) ) ||
			    ( i <  ( allStatements.length - 1 ) && ! ( allStatements[ i ].trim().endsWith( "AND" ) || allStatements[ i ].trim().endsWith( "OR" ) ) ) )  
				errors.add( new JsonPrimitive( "AND|OR connectors are invalid - " + allStatements[ i ] ) );
		}
		ArrayList< String > stmtErrors = new ArrayList< String >();
		stmtErrors.addAll( Arrays.asList( allStatements ).stream()
				.filter( invalidStatementFilter )
				.collect( Collectors.toList() ) );
		stmtErrors.forEach( ( anError ) -> errors.add( new JsonPrimitive( "Type mismatch or invalid statement - " + anError ) ) );
		JsonObject result = new JsonObject();
		if( errors != null && errors.size() == 0 )
		    result.addProperty( "status", "success" );
		else {
			result.addProperty( "status", "failure" );
			result.add( "errors", errors );
		}
		return result;
	}
	
	
	public JsonObject convertToJava( String brickSnippet ) {
		JsonObject validationResult = this.validate( brickSnippet );
		if( validationResult.get( "status" ).getAsString().equals( "failure" ) )
			return validationResult;
		brickSnippet = normalizeSpaces.apply( brickSnippet );
		brickSnippet = normalizeNewline.apply( brickSnippet );
		String[] allStatements = brickSnippet.split( "\\n" );
		Function< String, String > replacer = ( brickStatement ) -> {
			Object[] matchingBrick = _allBricks.stream()
					.filter( ( aBrick ) -> ( ( Pattern ) aBrick[ 2 ] ).matcher( brickStatement ).matches() )
					.findFirst()
					.get();
			System.out.println(  " matching Brick fot stmt : " + brickStatement + " =  " + matchingBrick[ 1 ] );
			Matcher matcher = ( ( Pattern )matchingBrick[ 2 ] ).matcher( brickStatement );
			matcher.matches();
			String javaStatement = matchingBrick[ 3 ].toString();
			String partialStmt =  matcher.replaceAll( javaStatement );
			if(partialStmt.matches(".*(AND|OR)[\\s]+(\\)).*")){
				String[] tempArr1 = partialStmt.split("(AND)[\\s]+(\\))");
				String[] tempArr2 = partialStmt.split("(OR)[\\s]+(\\))");
				if(tempArr1.length == 2)
					partialStmt = tempArr1[0] + ") AND";
				if(tempArr2.length == 2)
					partialStmt = tempArr2[0] + ") OR";
			}
			
//			Matcher tempstmtPattern = Pattern.compile( ".*(AND)[\\s]+(\\))$" ).matcher( partialStmt );
//			tempstmtPattern.matches();
//			partialStmt = tempstmtPattern.group( 1 ) + " !" + tempstmtPattern.group( 3 );
			
			partialStmt = partialStmt.replaceAll( "AND$", "&&" ).replaceAll( "OR$", "||" );
			Set< String > keys = _logicalOperatorsToJava.keySet();
			Iterator< String > iterator = keys.iterator();
			while( iterator.hasNext() ) {
				String key = iterator.next();
				if( (key.contains( "not equals to" ) || key.contains( "does not contain" ) || key.contains( "does not start with" ) || key.contains( "does not end with" )) && partialStmt.contains(  key ) ) {
					Matcher stmtPattern = Pattern.compile( "((\\(\\s?)*)(.+)" ).matcher( partialStmt );
					stmtPattern.matches();
					partialStmt = stmtPattern.group( 1 ) + " !" + stmtPattern.group( 3 );
				} 
				partialStmt = partialStmt.replace( key, _logicalOperatorsToJava.get( key ) );
			}			
			return partialStmt;
        	};
		String result = Arrays.asList( allStatements ).stream().map( replacer ).collect( Collectors.joining( "\n" ) );
		JsonObject jRes = new JsonObject();
		jRes.addProperty( "status", "success" );
		jRes.addProperty( "result", result );
		return jRes;
	}
	
	public String createRegex( String input ) {
		input = normalizeSpaces.apply( input );
		input = input.replaceAll( " ", "\\\\s?" );
		Pattern pattern = Pattern.compile( "\\{\\w+\\}" );
		Matcher matcher = pattern.matcher( input );
		matcher.matches();
		while( matcher.find() ) {
			input = input.replace( matcher.group(), _englishToRegex.get( matcher.group().replace( "{", "" ).replace( "}", "" ) ) );
		}	
		return _englishToRegex.get( "spacesToRegex" ) + _englishToRegex.get( "openBracketToRegex" )  +  input + _englishToRegex.get( "closeBracketToRegex" ) 
		                                              + _englishToRegex.get( "logicalConnectorsToRegex" ) 
		                                              + _englishToRegex.get( "spacesToRegex" ) ;
	}
	
	public static void rec(StringBuilder stb) {
		
	}
	
	public static String format(String brickSnippet) {
		
		System.out.println(brickSnippet);
		String[] allStatements = brickSnippet.split( "\\n" );

		StringBuilder formattedbrickSnippet = new StringBuilder();
		
        for(int i=0 ; i < allStatements.length ; i++) {

        	if((allStatements[i].trim().endsWith(")") || allStatements[i].trim().endsWith("(")) && i != allStatements.length-1) {
        		StringBuilder s1 = new StringBuilder();
        		StringBuilder s2 = new StringBuilder(allStatements[i]);
        		String lineStr = allStatements[i];
        		int lastIndex = lineStr.length()-1;
        		
        		char c = s2.charAt(lastIndex);
        		
        		while(lastIndex !=0 && (c == '(' || c== ')' || c == ' ')) {
        			s1.append(c);
           		    s2.deleteCharAt(lastIndex);
        			c = s2.charAt(--lastIndex);
        		}
        		
        		formattedbrickSnippet.append(s2.toString()).append(System.lineSeparator());
        		formattedbrickSnippet.append(s1.toString());
        	}else {

        		formattedbrickSnippet.append(allStatements[i]).append(System.lineSeparator());
        		
        	}
        	
        	
        }


        allStatements = formattedbrickSnippet.toString().split("\\n");

        StringBuilder tempString = new StringBuilder();
        formattedbrickSnippet.setLength(0);

		
		for(int i=0 ; i < allStatements.length ; i++) {
			
			System.out.println("Testing :allStatements[i].trim()==\"(\" || allStatements[i].trim()==\")\"");
			System.out.println(allStatements[i].trim()=="(");
			System.out.println(allStatements[i].trim()==")");
			System.out.println(allStatements[i]=="(");
			System.out.println(allStatements[i]==")");
			System.out.println(allStatements[i].trim().contentEquals("("));
			System.out.println(allStatements[i].contentEquals(")"));
			
			if(allStatements[i].trim().contentEquals("(") || allStatements[i].trim().contentEquals(")") ) {
				tempString.append(allStatements[i]);
			}else {
				String format ;
				if(tempString.length()!=0) {
					format = tempString.toString()+allStatements[i]+System.lineSeparator();
					tempString.setLength(0);
				}else {
					format = allStatements[i]+System.lineSeparator();
				}
				formattedbrickSnippet.append(format);
			}
		}
		
		System.out.println(formattedbrickSnippet);
		
		return formattedbrickSnippet.toString();
	}

			
	public static void main( String[ ] args ) {
		Bricks bricks = new Bricks();
		
//		bricks.addBrick( "Current Balance is {NumericOperator} {NumericValue}", 
////				         "\\s?((\\(\\s?)*)Current\\s?Balance\\s?is\\s?(greater than|less than|equal to)\\s?([0-9]+)\\s?((\\)\\s?)*)(AND|OR)?\\s?",
//				         "$1 LATEST_BALANCE_Lifetime $3 $4 $5 $7" ); //First Group = open paranthesis, Last but one Group = close parnathesis, Last Group = Logical Connectors
//		
//		bricks.addBrick( "Been {NumericValue} days since the last {OfferId}", 
////				          "\\s?((\\(\\s?)*)Been\\s?([0-9]+)\\s?days\\s?since\\s?the\\s?last\\s?([a-zA-Z0-9]+)\\s?((\\)\\s?)*)(AND|OR)?\\s?",
//				         "$1 daysSinceLastOffer( $4 ) >= $3 $5 $7" );
//		
//		bricks.addBrick( "Average of last {NumericValue} Topup is {NumericOperator} {NumericValue}", 
////				         "\\s?((\\(\\s?)*)Average\\s?of\\s?last\\s?([0-9]+)\\s?Topup\\s?is\\s?(greater than|less than|equal to)\\s?([0-9]+)\\s?((\\)\\s?)*)(AND|OR)?\\s?",
//				         "$1 getAverageTopup( $3 ) $4 $5 $6 $8" );
//		
//		bricks.addBrick( "The value of offer attribute {StringValue} is {StringOperator} {StringValue}", 
////		         "this will be autocreated",
//		         "$1 getOfferAttribute( OFFER_ID, $3 ).$4( $5 ) $6 $8" );
//		
//		bricks.addBrick( "Customer Has Not Taken {OfferId} from {ProgramId}", 
////		         "this will be autocreated",
//		         "$1 offerNotTaken(  $4 $3 ) $5 " );
//		
//		bricks.addBrick( "Subscribers' Balance Before Credit {StringOperator} {StringValue}", 
//		         			"$1 BALANCE_BEFORE_CREDIT_SERIES_LifeTime.$3( $4 ) $5 $7" );
//		bricks.addBrick( "Type Of Account {StringOperator} {StringValue}", 
//     			"$1 ACCOUNT_TYPE.$3( $4 ) $5 $7" );
//		bricks.addBrick( "Balance From the incoming record {NumericOperator} {NumericValue}", 
//     			"$1 BALANCE $3 $4 $5 $7" );
//		
//		
//		bricks.addBrick("Subscriber Balance Check Count {NumericOperator} {NumericValue}",
//				"$1 BALANCE_Check_Count_Daily $3 $4 $5 $7");
//		bricks.addBrick("A Party Number {StringOperator} {StringValue}", "$1 A_PARTY_NUMBER.$3( $4 ) $5 $7");
//		bricks.addBrick("Type Of Incoming Record {StringOperator} {StringValue}", "$1 RECORD_TYPE.$3( $4 ) $5 $7");
//		bricks.addBrick("Number of subscriber contacted for category {StringValue} on date {NumericValue} {NumericOperator} {NumericValue}",
//				"$1 A_PARTY_NUMBER $3 $4 $5 $7");
//		
//		bricks.addBrick("Payment Account Type {StringOperator} {StringValue}", "$1 ACCOUNT_TYPE_LifeTime.$3( $4 ) $5 $7");
//		bricks.addBrick("Subscriber Balance Check Count {NumericOperator} {NumericValue}", "$1 BALANCE_Check_Count_Daily $3 $4 $5 $7");
//		bricks.addBrick("BTL Special Cluster","$1 BTL_SPECIAL_GROUP $3 $5");
//		//bricks.addBrick("NOT BTL Special Cluster","$1 BTL_SPECIAL_GROUP $3 $5");
//		
//		bricks.addBrick("return testing function for {ProgramId} and {OfferId} {StringOperator} {StringValue}","$1 testBFunction( $3,$4 ).$5($6) $7 $9 ");
//		
//		System.out.println( "---------Test for Typeahead----------------" );
//		String input = "";
//		System.out.println(  "input : " + input );
//		JsonObject typeAheadList = bricks.getTypeAheadMatches( input );
//		System.out.println( "\noutput : " + typeAheadList.toString() );
//		
//		System.out.println( "\n\n----------Test for validation success 1----------------" );		
//		input = "(        Current     Balance is greater than 10 AND\nBeen 10 days since the last OFFERABC12  )           OR    \nAverage of last 10 Topup is greater than 100       ";
//		System.out.println(  "input : " + input );
//		System.out.println( "\noutput : " +  bricks.validate( input ) );
//		
////		System.out.println( "\n\n----------Test for wrong parameter type---------------" );
////		input = "( Current     Balance is greater than 10 AND\nBeen 10vg days since the last OFFERABC12        )  OR    \nAverage of last 10fd Topup is greater than 100        ";
////		System.out.println(  "input : " + input );
////		System.out.println( "\noutput : " +  bricks.validate( input ) );
////
////		System.out.println( "\n\n----------Test for Mismatching Paranthesis---------------" );
////		input = "(     ( (Current     Balance is greater than 10 AND\nBeen 10 days since the last OFFERABC12 )      \nAverage of last 10 Topup is greater than 100) )     )  ";
////		System.out.println(  "input : " + input );
////		System.out.println( "\noutput : " +  bricks.validate( input ) );
////		
////		System.out.println( "\n\n----------Test for Bricks to java Conversion 2---------------" );
////		input = "((Current     Balance is greater than 10 AND\n The value of offer attribute \"offerGroup\" is string not equal to \"refill\" AND\n Been 10 days since the last OFFERABC12 )    OR  \n"
////				+ "( Average of last 10 Topup is greater than 100 ) )";
////		System.out.println(  "input : " + input );
////		System.out.println( "\noutput : " + bricks.convertToJava( input ).get( "result" ).getAsString() );
////		System.out.println( "\noutput : " + bricks.validate( input ) );
////		
////		System.out.println( "\n\n----------Test for creating Regex ---------------" );
////		input = "Current Balance is {NumericOperator} {NumericValue}";
////		System.out.println(  "input : " + input );
////		String output = bricks.createRegex( input );
////		System.out.println( "Eoutput : " + "\\s?((\\(\\s?)*)Current\\s?Balance\\s?is\\s?(greater than|less than|greater than or equal to|less than or equal to|equal to|not equal to)\\s?([0-9]+)\\s?((\\)\\s?)*)(AND|OR)?\\s?" );
////		System.out.println( "Aoutput : " + output );
////
////		System.out.println( "------------" );
////
////		input = "Been {NumericValue} days since the last {OfferId}";
////		System.out.println(  "input : " + input );
////		output = bricks.createRegex( input );
////		System.out.println( "Eoutput : " +  "\\s?((\\(\\s?)*)Been\\s?([0-9]+)\\s?days\\s?since\\s?the\\s?last\\s?([a-zA-Z0-9]+)\\s?((\\)\\s?)*)(AND|OR)?\\s?" );
////		System.out.println( "Aoutput : " + output );
////		
////		System.out.println( "------------" );
////
////		input = "Average of last {NumericValue} Topup is {NumericOperator} {NumericValue}";
////		System.out.println(  "input : " + input );
////		output = bricks.createRegex( input );
////		System.out.println( "Eoutput : " +   "\\s?((\\(\\s?)*)Average\\s?of\\s?last\\s?([0-9]+)\\s?Topup\\s?is\\s?(greater than|less than|greater than or equal to|less than or equal to|equal to|not equal to)\\s?([0-9]+)\\s?((\\)\\s?)*)(AND|OR)?\\s?" );
////		System.out.println( "Aoutput : " + output );
//		
////		System.out.println( "\n\n----------Test for validation success actual----------------" );		
////		input = "( Subscribers' Balance Before Credit equals to \"value\" AND \n( Type Of Account equals to \"value\" ) AND \nBalance From the incoming record equal to 20 )";
////		System.out.println(  "input : " + input );
////		System.out.println( "\noutput : " +  bricks.validate( input ) );
////		System.out.println( "\nJava Conversion : " +  bricks.convertToJava( input ).get( "result" ).getAsString() );
////		
////		System.out.println( "\n\n----------Test for validation success actual----------------" );		
//////		input = "(Subscriber Balance Check Count  equal to 12 OR \nA Party Number equals to \"girish\" OR \n(Type Of Incoming Record starts with \"giri\"))";
////		input = "(Payment Account Type does not end with \"type 1\" OR \nSubscriber Balance Check Count  not equal to 21)";
//////		input = "(Subscriber Balance Check Count  not equal to 21 AND \nPayment Account Type does not end with \"type $1\")";
////		System.out.println(  "input : " + input );
////		System.out.println( "\noutput : " +  bricks.validate( input ) );
////		System.out.println( "\nJava Conversion : " +  bricks.convertToJava( input ).get( "result" ).getAsString() );
////
////		System.out.println( "\n\n----------Test for validation success actual----------------" );		
//////		input = "(Subscriber Balance Check Count  equal to 12 OR \nA Party Number equals to \"girish\" OR \n(Type Of Incoming Record starts with \"giri\"))";
//////		input = "( Subscribers' Balance Before Credit equals to \"value\" AND \nBalance From the incoming record equal to 20 AND \n( BTL Special Cluster ) )";
////		input = "(return testing function for \"DTAC_LINK_ 45_90\" and \"Refill_100_Get_Bonus_040\" equals to \"value\" )";
//////		System.out.println(  "input : " + input );
////		System.out.println( "\noutput : " +  bricks.validate( input ) );
////		System.out.println( "\nJava Conversion : " +  bricks.convertToJava( input ).get( "result" ).getAsString() );
//		
//		System.out.println( "\n\n----------Test for validation success actual----------------" );		
////		input = "(BTL Special Cluster AND \n(Payment Account Type does not end with \"type 1\" OR \nPayment Account Type equals to \"qwe\"))";
////		input = "((BTL Special Cluster OR \nPayment Account Type does not end with \"type 1\"))";
//		input = "(\n(BTL Special Cluster OR \nBTL Special Cluster))";
////		input = "(Subscriber Balance Check Count  not equal to 21 AND \nPayment Account Type does not end with \"type $1\")";
//		System.out.println(  "input : " + input );
//		System.out.println( "\noutput : " +  bricks.validate( input ) );
//		System.out.println( "\nJava Conversion : " +  bricks.convertToJava( input ).get( "result" ).getAsString() );
//		
		
		Bricks brickUtil = new Bricks();
		
		Map<String,String> siftTypesObj = new HashMap<>();
		siftTypesObj.put("DAILY_USAGE_SERVICE_TYPE", "VOICE,SMS,DATA,ROAMING,ROAMING_VOICE,ROAMING_SMS,ANY,");
		siftTypesObj.put("DAILY_USAGE_CONDITION_COLUMNS", "TOTAL_VOICEFREECOUNT,TOTAL_VOICEFREEDURATION,TOTAL_VOICECHARGEDCOUNT,TOTAL_VOICECHARGEDDURATION,TOTAL_VOICESPEND,TOTAL_VOICEONNET_FREECOUNT,TOTAL_VOICEONNET_FREEDURATION,TOTAL_VOICEONNET_CHARGEDCOUNT,TOTAL_VOICEONNET_CHARGEDDURATION,TOTAL_VOICEONNET_SPEND,TOTAL_VOICEOFFNET_FREECOUNT,TOTAL_VOICEOFFNET_FREEDURATION,TOTAL_VOICEOFFNET_CHARGEDCOUNT,TOTAL_VOICEOFFNET_CHARGEDDURATION,TOTAL_VOICEOFFNET_SPEND,TOTAL_VOICEIDD_FREECOUNT,TOTAL_VOICEIDD_FREEDURATION,TOTAL_VOICEIDD_CHARGEDCOUNT,TOTAL_VOICEIDD_CHARGEDDURATION,TOTAL_VOICEIDD_SPEND,TOTAL_VOICEROAMING_FREECOUNT,TOTAL_VOICEROAMING_FREEDURATION,TOTAL_VOICEROAMING_CHARGEDCOUNT,TOTAL_VOICEROAMING_CHARGEDDURATION,TOTAL_VOICEROAMING_SPEND,TOTAL_SMSFREECOUNT,TOTAL_SMSCHARGEDCOUNT,TOTAL_SMSSPEND,TOTAL_SMSONNET_FREECOUNT,TOTAL_SMSONNET_CHARGEDCOUNT,TOTAL_SMSONNET_SPEND,TOTAL_SMSOFFNET_FREECOUNT,TOTAL_SMSOFFNET_CHARGEDCOUNT,TOTAL_SMSOFFNET_SPEND,TOTAL_SMSIDD_FREECOUNT,TOTAL_SMSIDD_CHARGEDCOUNT,TOTAL_SMSIDD_SPEND,TOTAL_SMSROAMING_FREECOUNT,TOTAL_SMSROAMING_CHARGEDCOUNT,TOTAL_SMSROAMING_SPEND,TOTALVOLUME,TOTALPPU,TOTAL3GVOLUME,TOTAL4GVOLUME,");
		siftTypesObj.put("CONDITION_OPERATOR", "<,>,<=,>=,=,!=,CONTAINS, CONDITION=GREATER THAN,LESS THAN,EQUAL TO,LESS THAN OR EQUAL TO,GREATER THAN OR EQUAL TO, ");
		siftTypesObj.put("GEO_REGION", "SALALA,MUSCAT,");
		siftTypesObj.put("MATH_OPERATION", "SUM,COUNT,AVERAGE,MIN,MAX,AVERAGE_FREQUENCY,STANDARD_DEVIATION,MEDIAN,MOST_FREQUENT,DAYS_SINCE,UNIQUECOUNT,");
		siftTypesObj.put("DAILY_USAGE_SELECTION_COLUMNS", "VOICE,SMS,DATA,ROAMING,ROAMING_VOICE,ROAMING_SMS,TOTAL_VOICEFREECOUNT,TOTAL_VOICEFREEDURATION,TOTAL_VOICECHARGEDCOUNT,TOTAL_VOICECHARGEDDURATION,TOTAL_VOICESPEND,TOTAL_VOICEONNET_FREECOUNT,TOTAL_VOICEONNET_FREEDURATION,TOTAL_VOICEONNET_CHARGEDCOUNT,TOTAL_VOICEONNET_CHARGEDDURATION,TOTAL_VOICEONNET_SPEND,TOTAL_VOICEOFFNET_FREECOUNT,TOTAL_VOICEOFFNET_FREEDURATION,TOTAL_VOICEOFFNET_CHARGEDCOUNT,TOTAL_VOICEOFFNET_CHARGEDDURATION,TOTAL_VOICEOFFNET_SPEND,TOTAL_VOICEIDD_FREECOUNT,TOTAL_VOICEIDD_FREEDURATION,TOTAL_VOICEIDD_CHARGEDCOUNT,TOTAL_VOICEIDD_CHARGEDDURATION,TOTAL_VOICEIDD_SPEND,TOTAL_VOICEROAMING_FREECOUNT,TOTAL_VOICEROAMING_FREEDURATION,TOTAL_VOICEROAMING_CHARGEDCOUNT,TOTAL_VOICEROAMING_CHARGEDDURATION,TOTAL_VOICEROAMING_SPEND,TOTAL_SMSFREECOUNT,TOTAL_SMSCHARGEDCOUNT,TOTAL_SMSSPEND,TOTAL_SMSONNET_FREECOUNT,TOTAL_SMSONNET_CHARGEDCOUNT,TOTAL_SMSONNET_SPEND,TOTAL_SMSOFFNET_FREECOUNT,TOTAL_SMSOFFNET_CHARGEDCOUNT,TOTAL_SMSOFFNET_SPEND,TOTAL_SMSIDD_FREECOUNT,TOTAL_SMSIDD_CHARGEDCOUNT,TOTAL_SMSIDD_SPEND,TOTAL_SMSROAMING_FREECOUNT,TOTAL_SMSROAMING_CHARGEDCOUNT,TOTAL_SMSROAMING_SPEND,TOTALVOLUME,TOTALPPU,TOTAL3GVOLUME,TOTAL4GVOLUME,");
		siftTypesObj.put("SERVICE_TYPE", "VOICE,SMS,DATA,PPU,OFFNET_VOICE,ONNET_VOICE,IDD_VOICE,IDD_SMS,RAOMING_VOICE,ROAMING_SMS,");
		siftTypesObj.put("OfferCategory", "BASICInfoIDDData_Roaming - OU_RoamingDEFAULTData_RoamingINOG_RoamingDataRegistrationVoiceRoamingLoyaltyeLifeP2PSTD - DataSTDIN_RoamingOU_RoamingVoicePegaInbound");
		
		
		brickUtil.addSiftTypesToEngToRegex(siftTypesObj);
		
//		List<Map<String, Object>> bricksUsed= new ArrayList<>();
//	
//		Map<String, Object> a1 = new HashMap<>();
//		a1.put("id", "applySalesforceGlobalPolicy");
//		a1.put("bt", "Apply Sales force global policy");
//		a1.put("jt", "$1 applySalesforceGlobalPolicy(  )  $3 $5");
//		a1.put("type", "Function");
//		a1.put("values", "[OR]");
//		a1.put("level", 2);
//		
//		bricksUsed.add(a1);
//
//		Map<String, Object> a2 = new HashMap<>();
//		a2.put("id", "IMEIChanged_01");
//		a2.put("bt", "NOT Has imei changed");
//		a2.put("jt", "$1 !IMEIChanged(  )  $3 $5");
//		a2.put("type", "Function");
//		a2.put("values", "[ AND ]");
//		a2.put("level", 2);
//		
//		bricksUsed.add(a2);
//
//		Map<String, Object> a3 = new HashMap<>();
//		a3.put("id", "applySalesforceGlobalPolicy_01");
//		a3.put("bt", "NOT Apply Sales force global policy");
//		a3.put("jt", "$1 !applySalesforceGlobalPolicy(  )  $3 $5");
//		a3.put("type", "Function");
//		a3.put("values", "[ AND ]");
//		a3.put("level", 1);
//		
//		bricksUsed.add(a3);
//
//		
//		
//		for (Map<String, Object> innrBrickObj : bricksUsed) {
//			brickUtil.addBrick((String)innrBrickObj.get("bt"), (String)innrBrickObj.get("jt"));
//		}

		
		brickUtil.addBrick("NOT Apply Sales force global policy", "$1 !applySalesforceGlobalPolicy(  )  $3 $5");

		
// 		bt=Apply Sales force global policy, 
//		jt=$1 applySalesforceGlobalPolicy(  )  $3 $5, 
		brickUtil.addBrick("Apply Sales force global policy", "$1 applySalesforceGlobalPolicy(  )  $3 $5");

//		bt=Has imei changed, 
//		jt=$1 IMEIChanged(  )  $3 $5, 
		brickUtil.addBrick("Has imei changed", "$1 IMEIChanged(  )  $3 $5");
		
//		bt=Account Type {StringOperator} {StringValue}, 
//		jt=$1 ACCOUNT_TYPE_LifeTime.$3( $4 ) $5 $7, 
		brickUtil.addBrick("Account Type {StringOperator} {StringValue}", "$1 ACCOUNT_TYPE_LifeTime.$3( $4 ) $5 $7");
		
//		bt=NOT Apply Sales force global policy, 
//		jt=$1 !applySalesforceGlobalPolicy(  )  $3 $5, 
		brickUtil.addBrick("NOT Apply Sales force global policy", "$1 !applySalesforceGlobalPolicy(  )  $3 $5");

//		bt=NOT Has imei changed, 
//		jt=$1 !IMEIChanged(  )  $3 $5, 
		brickUtil.addBrick("NOT Has imei changed", "$1 !IMEIChanged(  )  $3 $5");

//		bt=Account Type {StringOperator} {StringValue}, 
//		jt=$1 ACCOUNT_TYPE_LifeTime.$3( $4 ) $5 $7, 
		brickUtil.addBrick("Account Type {StringOperator} {StringValue}", "$1 ACCOUNT_TYPE_LifeTime.$3( $4 ) $5 $7");

		

		
		String bExpression = "(\n" + 
				"( \n" + 
				"( \n" + 
				"( Apply Sales force global policy AND \n" + 
				"Has imei changed ) AND \n" + 
				"NOT Has imei changed ) AND \n" + 
				"( Apply Sales force global policy AND \n" + 
				"( NOT Apply Sales force global policy AND \n" + 
				"NOT Has imei changed ) AND  )  ) \n" + 
				"Account Type equals to \"PRIME\" )";
		bExpression = format(bExpression);
		
		JsonObject jRes = brickUtil.convertToJava(bExpression);

		System.out.println(jRes);
		
		
	}
}