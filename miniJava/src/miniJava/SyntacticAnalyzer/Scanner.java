package miniJava.SyntacticAnalyzer;

import java.io.*;

import miniJava.ErrorReporter;

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
		
		if (currentChar == '{')
		{
			takeIt();
			return(TokenKind.LBRACE);
		}
		
		else if (currentChar == '}')
		{
			takeIt();
			return(TokenKind.RBRACE);
		}
		
		else if (currentChar == '[')
		{
			takeIt();
			return(TokenKind.LBRACKET);
		}
		
		else if (currentChar == ']')
		{
			takeIt();
			return(TokenKind.RBRACKET);
		}
		
		else if (currentChar == '(')
		{
			takeIt();
			return(TokenKind.LPAREN);
		}
		
		else if (currentChar == ')')
		{
			takeIt();
			return(TokenKind.RPAREN);
		}
		
		else if (currentChar == ';')
		{
			takeIt();
			return(TokenKind.SEMICOLON);
		}
		
		else if (currentChar == '+')
		{
			takeIt();
			return(TokenKind.BINOP);
		}
		
		else if (currentChar == '-')
		{
			takeIt();
			return(TokenKind.SUBTRACT);
		}
		
		else if (currentChar == '*')
		{
			takeIt();
			return(TokenKind.BINOP);
		}
		
		else if (currentChar == '!')
		{
			takeIt();
			if (currentChar == '=')
			{
				takeIt();
				return(TokenKind.BINOP);
			}
			
			return(TokenKind.UNOP);
		}
		
		else if (currentChar == '=')
		{
			takeIt();
			if (currentChar == '=')
			{
				takeIt();
				return(TokenKind.EQUALS);
			}
			return(TokenKind.BINOP);
		}
		
		else if (currentChar == '>')
		{
			takeIt();
			if (currentChar == '=')
			{
				takeIt();
			}
			return(TokenKind.BINOP);
		}
		
		else if (currentChar == '<')
		{
			takeIt();
			if (currentChar == '=')
			{
				takeIt();
			}
			return(TokenKind.BINOP);
		}
		
		else if (currentChar == '|')
		{
			takeIt();
			if (currentChar == '|')
			{
				takeIt();
				return TokenKind.BINOP;
			}
			System.out.println("There is no second |");
		}
		
		else if (currentChar == '&')
		{
			takeIt();
			if (currentChar == '&')
			{
				takeIt();
				return TokenKind.BINOP;
			}
			
			System.out.println("There is no second &");
		}
		
		else if (currentChar == '>')
		{
			takeIt();
			return(TokenKind.BINOP);
		}
		
		else if (currentChar == '<')
		{
			takeIt();
			return(TokenKind.BINOP);
		}
		
		else if (currentChar == '/')
		{
			takeIt();
			if (currentChar == '/')
			{
				takeIt();
				return(TokenKind.LINE_COMMENT);
			}
			
			else if (currentChar == '*')
			{
				takeIt();
				return(TokenKind.BLOCK_COMMENT_OPEN);
			}
			
			else
			{
				return(TokenKind.BINOP);
			}
		}
		
		else if (currentChar == '*')
		{
			takeIt();
			if (currentChar == '/')
			{
				takeIt();
				return(TokenKind.BLOCK_COMMENT_CLOSE);
			}
			
			else
			{
				return TokenKind.BINOP;
			}
		}
		
		else if (Character.toString(currentChar).equals("[0-9]"))
		{
			while (Character.toString(currentChar).equals("[0-9]"))
			{
				takeIt();
			}
			return(TokenKind.NUM);
		}
		
		else if (Character.toString(currentChar).equals("^[a-zA-Z0-9]"))
		{
			while (Character.toString(currentChar).equals("^[a-zA-Z0-9]"))
			{
				takeIt();
			}
			
			if (currentSpelling.equals("class"))
			{
				return TokenKind.CLASS_DEC;
			}
			
			else if (currentSpelling.equals("return"))
			{
				return TokenKind.RETURN_DEC;
			}
			
			else if (currentSpelling.equals("this"))
			{
				return TokenKind.THIS_DEC;
			}
			
			else if (currentSpelling.equals("if"))
			{
				return TokenKind.IF_KW;
			}
			
			else if (currentSpelling.equals("else"))
			{
				return TokenKind.ELSE_KW;
			}
			
			else if (currentSpelling.equals("while"))
			{
				return TokenKind.WHILE_KW;
			}
			
			else if (currentSpelling.equals("new"))
			{
				return TokenKind.NEW_KW;
			}
			
			else if (currentSpelling.equals("true"))
			{
				return TokenKind.TRUE_KW;
			}
			
			else if (currentSpelling.equals("false"))
			{
				return TokenKind.FALSE_KW;
			}
			
			else if (currentSpelling.equals("boolean"))
			{
				return TokenKind.BOOLEAN_KW;
			}
			
			else
			{
				return TokenKind.ID;
			}
		}
		
		else if (currentChar == '$')
		{
			return(TokenKind.EOT);
		}
		
		return(TokenKind.ERROR);	
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
