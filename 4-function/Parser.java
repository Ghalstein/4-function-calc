/*************************************************************************
 *  Compilation:  javac Parser.java
 *  Execution:    java Parser
 * 
 *  A parser for the F-Calc language.
 ************************************************************************/
import java.util.Scanner;

public class Parser {
    private String theLine;  // the line to be parsed
    private int lineIndex;   // the next character to begin with
    private boolean done;    // all characters have been processed
    private String token;    // current token
    private String opsString = ":,==()+-*/";
    private String expToken; // for expression checking

    // Check if given string adheres to the usual variable/function name syntax
    public static boolean alphanum(String varName) {
	if (varName.length() <= 0 || ! Character.isLetter(varName.charAt(0)))
	    return false;
	for (int i = 0; i < varName.length(); i++)
	    if (!Character.isLetterOrDigit(varName.charAt(i)))
		return false;
	return true;
    }
    // Check if number
    public static boolean num(String varName) {
	try {
	    Double.parseDouble(varName);
	    return true;
	}
	catch (NumberFormatException e) {
	    return false;
	}
    }
    
    public Parser(String input){
        theLine = input.trim();
        lineIndex = 0;
        isDone();
    }
    
    public boolean isDone(){
        done =  (lineIndex >= theLine.length());
        return done;
    }

    // finite-state based tokenizer
    public String getToken(){
        //instantiate the token that will eventually be returned
        String theToken = "";
        
        int state = 0;                //current state
        char current;
        
	final String whitespace = " \n\t";
        final int START = 0;          //parsing
        final int VARIABLE = 1;       //found the beginning of a variable name
        final int NUMBER = 2;         //found a leading number
        final int DECIMALPOINT = 3;   //found a decimal point
        final int FRACTION = 4;       //fractional part
        
	while (lineIndex < theLine.length() &&   
	       whitespace.indexOf(theLine.charAt(lineIndex)) != -1)
	    lineIndex++;   // skip leading white spaces
	while (!isDone()){	
        current = theLine.charAt(lineIndex++);  // next character

        //finite state machine
        switch(state){
		    case START: 
			if (opsString.contains(Character.toString(current))) {
			    theToken += current;
			    return theToken;
			}
			else if (Character.isLetter(current)) state = VARIABLE;
			else if (Character.isDigit(current))  state = NUMBER;
			else 
			    throw new AssertionError("tokenizer: Unrecognized input (" + current + ")");
			theToken += current;
			break;
	                
		    case VARIABLE: 
			if (Character.isLetterOrDigit(current))       
			    theToken += current;
			else { // backup one character
			    lineIndex--;
			    return theToken;
			}
			break;
	                
		    case NUMBER:
			if (Character.isDigit(current))
			    theToken += current;
			else if (current == '.') {
			    theToken += current;
			    state = DECIMALPOINT;
			}
			else{ // backup one character
			    lineIndex--;
			    return theToken;
			}
			break;
	                
		    case DECIMALPOINT: 
			if (Character.isDigit(current)) {
			    theToken += current;
			    state = FRACTION;
			}
			else{
			    throw new NumberFormatException("tokenizer: number cannot end in a decimal point");
			}
			break;   
	                            
		    case FRACTION:
			if (Character.isDigit(current)) 
			    theToken += current;
			else {
			    lineIndex--;
	                    return theToken;
			}
			break;
	                        
		    default: return null;
	            }
	        }
		if (state == DECIMALPOINT) {
		    throw new NumberFormatException("tokenizer: number cannot end in a decimal point");	    
	}
        return theToken;
    }

    public String separateOps(String in) {
	for (int i = 0; i < opsString.length(); i++) {
	    char c = opsString.charAt(i);
            in = in.replace(Character.toString(c), " " + c + " ");
	}
        return in.trim(); // remove leading and trailing spaces
    }

    // validate the next token against the parameter string
    private void advance(String t) {
	String check = getToken();
	if (! check.equals(t))
	    throw new RuntimeException("unexpected token: " + check + " instead of " + t);
    }

    // This is the top-level parser
    public AbstractSyntax parse() {
	AbstractSyntax AST = new AbstractSyntax();

	String t = getToken();
	String name = null;
	if (t.equals("def")) { // function definition
	    name = getToken(); // function name
	    if (alphanum(name)) {
		AST.setType("def");
		AST.setName(name);
		advance("(");
		AST.setParams(parseParams(false));
		advance(":");		
	    }
	    else
			throw new InstantiationError("invalid function name");
	}
	else if (alphanum(t)) { // assignment or call
	    AST.setName(t); // name
	    String check = getToken();
	    if (check.equals("=")) { // assignment
		AST.setType("assign");
	    }
	    else if (check.equals("(")) { // call
		AST.setType("call");
		AST.setParams(parseParams(true));
	    }
	    else { // default to ordinary expression; reset to beginning of expression
		AST.setType("eval"); 		
		lineIndex = 0;
	    }
	} 
	else { // default to ordinary expression; reset to beginning of expression
	    AST.setType("eval"); 
	    lineIndex = 0;
	} 
	if (lineIndex < theLine.length()){
	    if (AST.getType().equals("call"))
		throw new IllegalAccessError("invalid function call");
	    int saveLineIndex = lineIndex;
	    expToken = getToken(); // do a proper parse to check for errors
	    exp();
	    if (! expToken.equals(""))
		throw new UnsupportedOperationException("missing operator " + expToken);
	    String remain = theLine.substring(saveLineIndex);
	    AST.setExp(separateOps(remain).split("\\s+"));
	} 
	else if (AST.getType().equals("assign"))
	    throw new NoSuchFieldError("missing right-hand side to assignment");
	return AST;
    }

    // expression parser -- left-recursion removed
    private void exp() { // exp --> term expPrime
	term();
	prime("+-");
    }
    private void prime(String ops) { // prime --> ops term/factor prime | epsilon
	while (!expToken.equals("") && ops.contains(expToken)) {
	    if (isDone())
		throw new NoSuchFieldError("incomplete expression " + expToken);
	    expToken = getToken();
	    if (ops.equals("+-"))
		term();
	    else if (ops.equals("*/"))
		factor();
	}
    }
    private void term() { // term --> factor termPrime
	factor();
	prime("*/");
    }
    private void factor() { // factor --> ID | NUM | ( exp )
	if (alphanum(expToken))
	    expToken = getToken();
	else if (expToken.equals("(")) {
	    expToken = getToken();
	    exp();
	    if (expToken.equals(")"))
		expToken = getToken();
	    else
		throw new NoSuchFieldError("missing ) " + expToken);
	}
	else {
	    try {
		Double check = Double.parseDouble(expToken);
	    }
	    catch (RuntimeException e) {
		throw new RuntimeException("unexpected token " + expToken);
	    }
	    expToken = getToken();
	}
    }

    // parameters -- comma separated variables
    private String[] parseParams(boolean call) {
	boolean var = true; // need a var to start
	String result = "";
	String t = getToken();
	while (! t.equals(")")) {
	    if (isDone())
		throw new VerifyError("invalid parameters");
	    if (var)
		if (alphanum(t))
		    result += (" " + t);
		else if (call && num(t))
		    result += (" " + t);
		else
		    throw new VerifyError("invalid parameter: " + t);		    
	    else if (t.equals(",") && ! var) ;
	    else
		throw new VerifyError("invalid parameter: " + t);
	    t = getToken();
	    if (t.equals(")") && ! var)
		throw new VerifyError("missing parameter: " + t);		
	    var = ! var;
	}
	result = result.trim();
	if (result.equals(""))
	    return new String[0];
	else
	    return result.split("\\s+");
    }

    public static void main(String[] args) {
	Scanner sc = new Scanner(System.in);
	String in = sc.nextLine();
	Parser t = new Parser(in);
	AbstractSyntax a = t.parse();
	System.out.println(a);
    }
}