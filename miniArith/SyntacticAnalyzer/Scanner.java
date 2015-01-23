/**
 *  Scan the first line of an input stream
 *
 *  Grammar:
 *   num ::= digit digit*
 *   digit ::= '0' | ... | '9'
 *   oper ::= '+' | '*'
 *   
 *   whitespace is the space character
 */
package miniArith.SyntacticAnalyzer;

import java.io.*;
import miniArith.ErrorReporter;

public class Scanner{

	private InputStream inputStream;
	private ErrorReporter reporter;

	private char currentChar;
	private StringBuilder currentSpelling;

	public Scanner(InputStream inputStream, ErrorReporter reporter) {
		this.inputStream = inputStream;
		this.reporter = reporter;

		// initialize scanner state
		readChar();
	}

	/**
	 * skip whitespace and scan next token
	 * @return token
	 */
	public Token scan() {

		// skip whitespace
		while (currentChar == ' ')
			skipIt();

		// collect spelling and identify token kind
		currentSpelling = new StringBuilder();
		TokenKind kind = scanToken();

		// return new token
		return new Token(kind, currentSpelling.toString());
	}


	public TokenKind scanToken() {

		// scan Token
		switch (currentChar) {
		case '+':
			takeIt();
			return(TokenKind.PLUS);

		case '*':
			takeIt();
			return(TokenKind.TIMES);

		case '(': 
			takeIt();
			return(TokenKind.LPAREN);

		case ')':
			takeIt();
			return(TokenKind.RPAREN);

		case '0': case '1': case '2': case '3': case '4':
		case '5': case '6': case '7': case '8': case '9':
			while (isDigit(currentChar))
				takeIt();
			return(TokenKind.NUM);

		case '$':
			return(TokenKind.EOT);

		default:
			scanError("Unrecognized character '" + currentChar + "' in input");
			return(TokenKind.ERROR);
		}
	}

	private void takeIt() {
		currentSpelling.append(currentChar);
		nextChar();
	}

	private void skipIt() {
		nextChar();
	}

	private boolean isDigit(char c) {
		return (c >= '0') && (c <= '9');
	}

	private void scanError(String m) {
		reporter.reportError("Scan Error:  " + m);
	}


	private final static char eolUnix = '\n';
	private final static char eolWindows = '\r';

	/**
	 * advance to next char in inputstream
	 * detect end of line or end of file and substitute '$' as distinguished eot terminal
	 */
	private void nextChar() {
		if (currentChar != '$')
			readChar();
	}

	private void readChar() {
		try {
			int c = inputStream.read();
			currentChar = (char) c;
			if (c == -1 || currentChar == eolUnix || currentChar == eolWindows) {
				currentChar = '$';
			}
			else if (currentChar == '$') {
				scanError("Illegal character '$' in input");
			}
		} catch (IOException e) {
			scanError("I/O Exception!");
			currentChar = '$';
		}
	}
}
