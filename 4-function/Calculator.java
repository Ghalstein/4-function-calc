/******************************************************************************                                        
 *  Compilation:  javac Calculator.java                                                                                
 *  Execution:    java Calculator                                                                                      
 *  Dependencies: FlexStack.java                                                                                           
 *                                                                                                                     
 *  Adapted from EvaluteDeluxe.java at                                                                                 
 *    https://algs4.cs.princeton.edu/13stacks/EvaluateDeluxe.java.html                                                 
 *                                                                                                                     
 *  A command-line four-function calculator that evaluates arithmetic                                                  
 *  expressions using Dijkstra's two-stack algorithm.                                                                  
 *  Handles the following binary operators: +, -, *, / and parentheses.                                                
 *                                                                                                                     
 *  Limitation                                                                                                       
 *  --------------                                                                                                     
 *    -  can add additional operators and precedence orders, but they                                                  
 *       must be left associative (exponentiation is right associative)                                                
 *                                                                                                                     
 ******************************************************************************/
import java.util.Arrays; 
import java.util.Stack;
import java.util.Scanner;
import java.util.HashMap;
import java.util.HashSet; 
import java.util.Map; 
import java.lang.ArrayIndexOutOfBoundsException;

public class Calculator {
    // all operators -- for now -- as a string                                                                         
    private static String opsString = "()+-/*";
    private static HashMap<String, Double> variables = new HashMap<>();
    private static HashMap<String, AbstractSyntax> fn = new HashMap<>(); 
 
    // result of applying binary operator op to two operands val1 and val2                                             
    public static double eval(String op, double val1, double val2) {
        if (op.equals("+")) return val1 + val2;
        if (op.equals("-")) return val1 - val2;
        if (op.equals("/")){ 
            if(val2 == 0)
                throw new ArithmeticException("Cannot divide by zero"); // exception thrown for dividing by zero
            return val1 / val2;
        }

        if (op.equals("*")) return val1 * val2;
        throw new IllegalArgumentException("Invalid operator");
    }
 
    // put spaces around operators to simplify tokenizing                                                              
    public static String separateOps(String in) {
        for (int i = 0; i < opsString.length(); i++) {
            char c = opsString.charAt(i);
            in = in.replace(Character.toString(c), " " + c + " ");
        }
        return in.trim(); // remove leading and trailing spaces                                                        
    }

    public static int precedence(String op) {
        // operator precedence: "(" ")" << "+" "-" << "*" "/"                                                          
        return opsString.indexOf(op) / 2;
    }
 
    public static Double evaluate(String[] tokens) {
        // Edsger Dijkstra's shunting-yard (two-stack) algorithm                                                       
        Stack<String> ops  = new Stack<String>();
        Stack<Double> vals = new Stack<Double>();
        // evaluate the input one token at a time                                                                      
        for (String s : tokens) {
            // token is a value                                                                                        
            if (! opsString.contains(s)) {
                try{
                    vals.push(Double.parseDouble(s));
                }catch(NumberFormatException nfe){
                    if(variables.containsKey(s)){
                    double num = variables.get(s);
                    vals.push(num);
                    }else
                        throw new IllegalStateException("Variable undefined");
                }
                continue;
            }
            // token is an operator                                                                                    
            while (true) {
                // the last condition ensures that the operator with                                                   
                // higher precedence is evaluated first                                                                
                if (ops.isEmpty() || s.equals("(") ||
                    (precedence(s) > precedence(ops.peek()))) {
                    ops.push(s);
                    break;
                }
                // evaluate expression                                                                                 
                String op = ops.pop();
                // ignore left parentheses                                                                         
                if (op.equals("("))
                    break;
                else {
                    // evaluate operator and two operands; 
                    // push result to value stack                             
                    double val2 = vals.pop();
                    double val1 = vals.pop();
                    vals.push(eval(op, val1, val2));
                }
            }
        }

        // evaluate operator and operands remaining on two stacks                                                      
        while (!ops.isEmpty()) {
            String op = ops.pop();
            double val2 = vals.pop();
            double val1 = vals.pop();
            vals.push(eval(op, val1, val2));
        }
        // last value on stack is value of expression                                                                  
        return vals.pop();
    }
 
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        // our command line prompt                                                                                     
        System.out.print("> ");
        mainLoop: // label for the loop
        while (input.hasNext()) {
            // read in next line as a string                                                                           
            String ln = input.nextLine().trim();
            // quits the program
            if(ln.toLowerCase().equals("quit")) System.exit(0);  
            // tokenize -- separate operands and operators into a string array
            try{
            Parser t = new Parser(ln); // creates an instacne of the parser class
            AbstractSyntax a = t.parse(); // creates an abstract syntax object
            if(a.toString().equals("[eval,null,null,null]")){ // ignores an "enter key" 
                continue mainLoop;
            }   
            String[] strArr = a.getExp(); // var for expressions
            String[] parArr = a.getParams(); // var for parameters
            // checks for the variables used in def type
            if(a.getType().equals("def")){
                HashSet<String> params = new HashSet<>();
                for(int i = 0; i < parArr.length; i++){
                    if(Parser.alphanum(parArr[i]))
                        params.add(parArr[i]);
                }
                for(int i = 0; i < strArr.length; i++){
                    // cecks that non parameter variables used are defined
                    if(!variables.containsKey(strArr[i]) && (Parser.alphanum(strArr[i])) && !(params.contains(strArr[i]))){
                        System.out.println("Sorry, you are using an undefined variable \"" + strArr[i] + "\" in the expression \"" + ln +".\"" +
                            "\nPlease enter a numerical expression, define a variable, define a function, " + 
                            "\ncall a defined function or tpye \"quit\" to quit the program:");
                        System.out.print("> ");
                        continue mainLoop;
                    }
                }
            }
            // handles assign types
            if(a.getType().equals("assign")){
                double num = evaluate(strArr); // evaluates the right side of the assignment
                String var = a.getName();
                variables.put(var, num); // stores the variable and its value to a hashmap
                System.out.println("Variable " + var + " and its value " 
                + num + " have been stored.");
                System.out.print("> ");
                continue mainLoop;
            }
            // handles def types by adding the valid function to the hashtable
            if(a.getType().equals("def")){
                String func = a.getName();
                fn.put(func, a); // stores the function name and its abstract syntax to a hashmap
                System.out.println("Function \"" + func + "(" 
                + parArr[0] + "," + parArr[1] + ")\" has been stored.");
                System.out.print("> ");
                continue mainLoop;
            }                                        

            // handles call types
            if(a.getType().equals("call")){
                if(parArr.length != 2) // amkes sure there 2 params
                    throw new VerifyError("Incorrect Amount of Parameters");
                else if(fn.containsKey(a.getName())){
                    AbstractSyntax f = fn.get(a.getName());
                    String[] fParam = f.getParams();
                    String[] fExp = f.getExp();
                    String p1 = parArr[0];
                    String p2 = parArr[1];
                    String[] newArr = new String[fExp.length];
                    for(int i = 0; i < fExp.length; i++){
                        // converts parameter variables to specified values
//                        System.out.println(fExp[i] + " " + fParam[0] + " " + fParam[1]);
                        if(fExp[i].equals(fParam[0])){
                            newArr[i] = p1;
                        }
                        if(fExp[i].equals(fParam[1])){
                            newArr[i] = p2;
                        }
                        if(!fExp[i].equals(fParam[1]) && !fExp[i].equals(fParam[0])){
                            newArr[i] = fExp[i];
                        }
                    }
                    System.out.println(evaluate(newArr)); // evaluates the call
                    }else
                        System.out.println("Sorry, the function, \"" + a.getName() + "\" is unrecognized." +
                        "\nPlease enter a numerical expression, define a variable, define a function, " + 
                            "\ncall a defined function or tpye \"quit\" to quit the program:");
                        System.out.print("> ");
                        continue mainLoop;
            }
            String[] tokens = separateOps(ln).split("\\s+");
            // evaluate and print 
            if(a.getType().equals("eval"))                                                                                    
                System.out.println(evaluate(tokens));
            }catch(ArithmeticException ae){
                System.out.println("Sorry, the expression \"" + ln +  
                "\" involves dividing by zero.\nPlease enter a numerical expression, define a variable, define a function, " + 
                "\ncall a defined function or tpye \"quit\" to quit the program:");
            }catch(VerifyError ve){
                System.out.println("Sorry, you must provide exactly two valid parameters for functions." + 
                "\nPlease enter a numerical expression, define a variable, define a function, " + 
                "\ncall a defined function or tpye \"quit\" to quit the program:");
            }catch(ArrayIndexOutOfBoundsException aioobe){
                System.out.println("Sorry, you have provided an incorrect number of paramters."
                +"\nPlease enter a numerical expression, define a variable, define a function, " + 
                "\ncall a defined function or tpye \"quit\" to quit the program:");
            }catch(IllegalStateException ise){
                System.out.println("Sorry, one or more variables used in the expression \"" + ln +  
                "\" are undefined.\nPlease enter a numerical expression, define a variable, define a function, " + 
                "\ncall a defined function or tpye \"quit\" to quit the program:");
            }catch(IllegalArgumentException iae){
                System.out.println("Sorry, you are using invalid operators." + 
                "\nPlease enter a numerical expression, define a variable, define a function, " + 
                "\ncall a defined function or tpye \"quit\" to quit the program:");
            }catch(UnsupportedOperationException uoe){
                System.out.println("Sorry, there may be a missing operator or an invalid input entered." +
                "\nPlease enter a numerical expression, define a variable, define a function, " + 
                "\ncall a defined function or tpye \"quit\" to quit the program:");
            }catch(RuntimeException re){
                System.out.println("Sorry, your input could not be parsed the way it is typed." +
                "\nPlease enter either a numerical expression such as \"x + y\"," + 
                "\ndefine a variable suhc as \"x = 1\", \ndefine a function such as \"def sum(x,y): x + y\", " + 
                "\ncall a defined function such as \"sum(x,y)\" \nor tpye \"quit\" to quit the program:");
            }catch(NoSuchFieldError nsfe){
                System.out.println("Your input appears to be incomplete." +
                "\nPlease enter a numerical expression, define a variable, define a function, " + 
                "\ncall a defined function or tpye \"quit\" to quit the program:");
            }catch(AssertionError ae){
                System.out.println("Sorry, you entered an unrecognized input." +
                "\nPlease enter a numerical expression, define a variable, define a function, " + 
                "\ncall a defined function or tpye \"quit\" to quit the program:");
            }catch(InstantiationError ie){
                System.out.println("Sorry, you have entered an invalid function name." +
                "\nPlease enter a numerical expression, define a variable, define a function, " + 
                "\ncall a defined function or tpye \"quit\" to quit the program:");
            }catch(IllegalAccessError iae){
                System.out.println("Sorry, you have entered an invalid function call." +
                "\nPlease enter a numerical expression, define a variable, define a function, " + 
                "\ncall a defined function or tpye \"quit\" to quit the program:");
            }
            // our command line prompt                                                                                 
            System.out.print("> ");
        }
    }
}
