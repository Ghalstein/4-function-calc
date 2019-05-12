import java.util.Arrays;
 
// A simple Abstract Syntax data structure for F-Calc.                                                                 
// Four variables used as follows:                                                                                     
//  type -- one of the four types of statements in F-Calc:                                                             
//          "eval", "assign", "def", "call"                                                                            
//  name -- var name for assignment, function name for "def" and "call"                                                
//  params -- parameters for "def" and "call"                                                                          
//  exp -- the expression to be evaluated, not used for "call"                                                         
 
public class AbstractSyntax {
    private String type;
    private String name;
    private String[] params;
    private String[] exp;
 
    public String getType() { return type; }
    public String getName() { return name; }
    public String[] getParams() { return params; }
    public String[] getExp() { return exp; }
 
    public void setType(String t) { type = t; }
    public void setName(String n) { name = n; }
    public void setParams(String[] p) { params = p; }
    public void setExp(String[] e) { exp = e; }
 
    public String toString() {
        return "[" + type + "," + name + "," + Arrays.toString(params) + "," + Arrays.toString(exp) + "]";
    }
}
