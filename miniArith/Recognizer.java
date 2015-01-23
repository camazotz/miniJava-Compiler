/**
 *   COMP 520 
 *   Simple expression scanner and parser
 *     following package structure of a full compiler
 *
 *  Parser grammar:
 *   S ::= E '$'
 *   E ::= T (oper T)*     
 *   T ::= num | '(' E ')'
 *
 *  Scanner grammar:
 *   num ::= digit digit*
 *   digit ::= '0' | ... | '9'
 *   oper ::= '+' | '*'
 */

package miniArith;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import miniArith.SyntacticAnalyzer.Parser;
import miniArith.SyntacticAnalyzer.Scanner;

/**
 * Recognize whether input is an arithmetic expression as defined by
 * a simple context free grammar for expressions and a scanner grammar.
 * 
 */
public class Recognizer {


	/**
	 * @param args  if no args provided parse from keyboard input
	 *              else args[0] is name of file containing input to be parsed  
	 */
	public static void main(String[] args) {

		InputStream inputStream = null;
		if (args.length == 0) {
			System.out.println("Enter Expression");
			inputStream = System.in;
		}
		else {
			try {
				inputStream = new FileInputStream(args[0]);
			} catch (FileNotFoundException e) {
				System.out.println("Input file " + args[0] + " not found");
				System.exit(1);
			}		
		}

		ErrorReporter reporter = new ErrorReporter();
		Scanner scanner = new Scanner(inputStream, reporter);
		Parser parser = new Parser(scanner, reporter);

		System.out.println("Syntactic analysis ... ");
		parser.parse();
		System.out.print("Syntactic analysis complete:  ");
		if (reporter.hasErrors()) {
			System.out.println("INVALID arithmetic expression");
			System.exit(4);
		}
		else {
			System.out.println("valid arithmetic expression");
			System.exit(0);
		}
	}
}







