package miniJava.SyntacticAnalyzer;
import miniJava.ErrorReporter;

	/**
	 * Parser
	 *
	 * Grammar:
	 *   S ::= E '$'
	 *   E ::= T (opers T)*     
	 *   T ::= num | '(' E ')'
	 */
	

	public class Parser {

		private Scanner scanner;
		private ErrorReporter reporter;
		private Token token;
		private boolean trace = false;

		public Parser(Scanner scanner, ErrorReporter reporter) {
			this.scanner = scanner;
			this.reporter = reporter;
		}


		/**
		 * SyntaxError is used to unwind parse stack when parse fails
		 *
		 */
		class SyntaxError extends Error {
			private static final long serialVersionUID = 1L;	
		}

		/**
		 * start parse
		 */
		public void parse() {
			token = scanner.scan();
			try {
				parseProgram();
			}
			catch (SyntaxError e) { }
		}

		private void parseProgram() throws SyntaxError
		{
			parseClassDec();
			accept(TokenKind.EOT);
		}
		
		private void parseClassDec() throws SyntaxError
		{
			parseClassKW();
			parseId();
			parseLBrace();
			
			while (true)
			{
				if (token.kind == TokenKind.PUBLIC_KW || token.kind == TokenKind.PRIVATE_KW
					|| token.kind == TokenKind.STATIC_KW || token.kind == TokenKind.STATIC_KW
					|| token.kind == TokenKind.VOID_KW || token.kind == TokenKind.ID ||
					token.kind == TokenKind.INT_KW || token.kind == TokenKind.BOOLEAN_KW)
				{
					parseDeclarators();
					parseId();
					
					if (token.kind == TokenKind.SEMICOLON)
					{
						parseSemicolon();
					}
					
					else if (token.kind == TokenKind.LPAREN)
					{
						parseLParen();
						
						if (token.kind == TokenKind.RPAREN)
						{
							parseRParen();
						}
						
						else
						{
							parseParameterList();
							parseRParen();
						}
						
						parseLBrace();
						
						while (true)
						{
							if (token.kind == TokenKind.IF_KW || token.kind == TokenKind.WHILE_KW ||
									token.kind == TokenKind.THIS_KW || token.kind == TokenKind.ID ||
									token.kind == TokenKind.INT_KW || token.kind == TokenKind.BOOLEAN_KW)
							{
								parseStatement();
							}
							
							else
								break;
						}
						
						if (token.kind == TokenKind.RETURN_KW)
						{
							parseReturn();
							parseExpression();
							parseSemicolon();
						}
						
						parseRBrace();
					}
				}
				
				else
					break;
			}
			
			parseRBrace();
			
		}
		
		private void parseClassKW()
		{
			if (token.kind == TokenKind.CLASS_KW)
			{
				acceptIt();
			}
			
			else
			{
				// Error
			}
		}
		
		private void parseReturn()
		{
			if (token.kind == TokenKind.RETURN_KW)
			{
				acceptIt();
			}
			
			else
			{
				// Error
			}
		}
		
		private void parseRBrace()
		{
			if (token.kind == TokenKind.RBRACE)
			{
				acceptIt();
			}
			
			else
			{
				// Error
			}
		}
		
		private void parseStatement()
		{
			if (token.kind == TokenKind.LBRACKET)
			{
				parseLBracket();
				
				while (true)
				{
					if (token.kind == TokenKind.IF_KW || token.kind == TokenKind.WHILE_KW ||
							token.kind == TokenKind.THIS_KW || token.kind == TokenKind.ID ||
							token.kind == TokenKind.INT_KW || token.kind == TokenKind.BOOLEAN_KW)
					{
						parseStatement();
						parseRBracket();
					}
					
					else
						break;
				}
			}
			
			else if (token.kind == TokenKind.IF_KW)
			{
				parseIfKW();
				parseLParen();
				parseExpression();
				parseRParen();
				parseStatement();
				
				if (token.kind == TokenKind.ELSE_KW)
				{
					parseElseKW();
					parseStatement();
				}
			}
			
			else if (token.kind == TokenKind.WHILE_KW)
			{
				parseWhileKW();
				parseLParen();
				parseExpression();
				parseRParen();
				parseStatement();
			}
			
			else if (token.kind == TokenKind.BOOLEAN_KW)
			{
				parseId();
				parseEquals();
				parseExpression();
				parseSemicolon();
			}
			
			else if (token.kind == TokenKind.INT_KW)
			{
				parseIntKW();
				
				if (token.kind == TokenKind.LBRACKET)
				{
					parseLBracket();
					parseRBracket();
				}
				
				parseId();
				parseEquals();
				parseExpression();
				parseSemicolon();
			}
			
			else if (token.kind == TokenKind.ID ||
					token.kind == TokenKind.THIS_KW)
			{
				if (token.kind == TokenKind.ID)
				{
					parseId();
				}
				
				else
					parseThisKW();
				
				while (true)
				{
					if (token.kind == TokenKind.PERIOD)
					{
						parsePeriod();
						parseId();
					}
					
					else
						break;
				}
				
				if (token.kind == TokenKind.ID)
				{
					parseId();
					parseEquals();
					parseExpression();
					parseSemicolon();
				}
				
				else if (token.kind == TokenKind.LBRACKET)
				{
					parseLBracket();
					
					if (token.kind == TokenKind.RBRACKET)
					{
						parseRBracket();
						parseId();
						parseEquals();
						parseExpression();
						parseSemicolon();
					}
					
					else	// IxReference case
					{
						parseExpression();
						parseRBracket();
						parseEquals();
						parseExpression();
						parseSemicolon();
					}
				}
				
				else if (token.kind == TokenKind.LPAREN)
				{
					parseLParen();
					
					if (token.kind == TokenKind.NUM || token.kind == TokenKind.TRUE_KW
						|| token.kind == TokenKind.FALSE_KW || token.kind == TokenKind.NEW_KW
						|| token.kind == TokenKind.INT_KW || token.kind == TokenKind.UNOP
						|| token.kind == TokenKind.THIS_KW || token.kind == TokenKind.ID
						|| token.kind == TokenKind.LPAREN)
					{
						parseArgumentList();
					}
					
					parseRParen();
					parseSemicolon();
				}
			}
		}
		
		private void parseArgumentList()
		{
			parseExpression();
			
			while (true)
			{
				if (token.kind == TokenKind.COMMA)
				{
					parseComma();
					parseExpression();
				}
				
				else
					break;
			}
		}
		
		
		private void parseExpression()
		{
			if (token.kind == TokenKind.NEW_KW)
			{
				parseNewKW();
				
				if (token.kind == TokenKind.INT_KW)
				{
					parseIntKW();
					parseLBracket();
					parseExpression();
					parseRBracket();
				}
				
				else
				{
					parseId();
					
					if (token.kind == TokenKind.LPAREN)
					{
						parseLParen();
						parseRParen();
					}
					
					else if (token.kind == TokenKind.LBRACKET)
					{
						parseLBracket();
						parseExpression();
						parseRBracket();
					}
				}
			}
			
			else if (token.kind == TokenKind.NUM)
			{
				parseNum();
			}
			
			else if (token.kind == TokenKind.TRUE_KW)
			{
				parseTrueKW();
			}
			
			else if (token.kind == TokenKind.FALSE_KW)
			{
				parseFalseKW();
			}
			
			else if (token.kind == TokenKind.LPAREN)
			{
				parseLParen();
				parseExpression();
				parseRParen();
			}
			
			else if (token.kind == TokenKind.UNOP)
			{
				parseUnop();
				parseExpression();
			}
			
			else if (token.kind == TokenKind.THIS_KW ||
					token.kind == TokenKind.ID)
			{
				if (token.kind == TokenKind.THIS_KW)
				{
					parseThisKW();
				}
				
				else if (token.kind == TokenKind.ID)
				{
					parseId();
				}
				
				while (true)
				{
					if (token.kind == TokenKind.PERIOD)
					{
						parsePeriod();
						parseId();
					}
					
					else
						break;
				}
				
				if (token.kind == TokenKind.LPAREN)
				{
					parseLParen();
					
					// ( ArgumentList )?
					if (token.kind == TokenKind.NUM || token.kind == TokenKind.TRUE_KW
							|| token.kind == TokenKind.FALSE_KW || token.kind == TokenKind.NEW_KW
							|| token.kind == TokenKind.INT_KW || token.kind == TokenKind.UNOP
							|| token.kind == TokenKind.THIS_KW || token.kind == TokenKind.ID
							|| token.kind == TokenKind.LPAREN)
					{
						parseArgumentList();
					}
					
					parseRParen();
				}
				
				else
				{
					if (token.kind == TokenKind.LBRACKET)
					{
						parseLBracket();
						parseExpression();
						parseRBracket();
					}
				}
			}
			
			if (token.kind == TokenKind.BINOP)
			{
				parseBinop();
				parseExpression();
			}
		}
		
		private void parseBinop()
		{
			if (token.kind == TokenKind.BINOP)
			{
				acceptIt();
			}
			
			else
			{
				// Error
			}
		}
		
		private void parseUnop()
		{
			if (token.kind == TokenKind.UNOP)
			{
				acceptIt();
			}
			
			else
			{
				// Error
			}
		}
		
		private void parseNum()
		{
			if (token.kind == TokenKind.NUM)
			{
				acceptIt();
			}
			
			else
			{
				// Error
			}
		}
		
		private void parseTrueKW()
		{
			if (token.kind == TokenKind.TRUE_KW)
			{
				acceptIt();
			}
			
			else
			{
				// Error
			}
		}
		
		private void parseFalseKW()
		{
			if (token.kind == TokenKind.FALSE_KW)
			{
				acceptIt();
			}
			
			else
			{
				// Error
			}
		}
		private void parseNewKW()
		{
			if (token.kind == TokenKind.NEW_KW)
			{
				acceptIt();
			}
			
			else
			{
				// Error
			}
		}
		
		private void parseThisKW()
		{
			if (token.kind == TokenKind.THIS_KW)
			{
				acceptIt();
			}
			
			else
			{
				//Error
			}
		}
		
		private void parsePeriod()
		{
			if (token.kind == TokenKind.PERIOD)
			{
				acceptIt();
			}
			
			else
			{
				// Error
			}
		}
		private void parseEquals()
		{
			if (token.kind == TokenKind.EQUALS)
			{
				acceptIt();
			}
			
			else
			{
				// Error
			}
		}
		
		private void parseWhileKW()
		{
			if (token.kind == TokenKind.WHILE_KW)
			{
				acceptIt();
			}
			
			else
			{
				// Error
			}
		}
		
		private void parseElseKW()
		{
			if (token.kind == TokenKind.ELSE_KW)
			{
				acceptIt();
			}
			
			else
			{
				// Error
			}
		}
		
		private void parseIfKW()
		{
			if (token.kind == TokenKind.IF_KW)
			{
				acceptIt();
			}
			
			else
			{
				// Error
			}
		}
		
		private void parseParameterList()
		{
			parseType();
			parseId();

			while (true)
			{
				if (token.kind == TokenKind.COMMA)
				{
					parseComma();
					parseType();
					parseId();
				}
				
				else
					break;
			}
		}
		
		private void parseComma()
		{
			if (token.kind == TokenKind.COMMA)
			{
				acceptIt();
			}
			
			else
			{
				// Error
			}
		}
		
		private void parseRParen()
		{
			if (token.kind == TokenKind.RPAREN)
			{
				acceptIt();
			}
			
			else
			{
				acceptIt();
			}
		}
		
		private void parseLParen()
		{
			if (token.kind == TokenKind.LPAREN)
			{
				acceptIt();
			}
			
			else
			{
				// Error
			}
		}
		
		private void parseSemicolon()
		{
			if (token.kind == TokenKind.SEMICOLON)
			{
				acceptIt();
			}
			
			else
			{
				//Error
			}
		}
		
		private void parseFieldDec()
		{
			parseDeclarators();
			parseId();
		}
		
		private void parseDeclarators()
		{
			if (token.kind == TokenKind.PUBLIC_KW)
			{
				parsePublicDec();
			}
			
			else if (token.kind == TokenKind.PRIVATE_KW)
			{
				parsePrivateDec();
			}
			
			else
			{
				// Error
			}
			
			if (token.kind == TokenKind.STATIC_KW)
			{
				parseStaticDec();
			}
			
			if (token.kind == TokenKind.VOID_KW)
			{
				parseVoidDec();
			}
			
			else
			{
				parseType();
			}
		}
		
		private void parseType()
		{
			if (token.kind == TokenKind.BOOLEAN_KW)
			{
				parseBooleanKW();
				return;
			}
			
			else if (token.kind == TokenKind.INT_KW)
			{
				parseIntKW();
			}
			
			else if (token.kind == TokenKind.ID)
			{
				parseId();
			}
			
			else
			{
				// Error
			}
			
			if (token.kind == TokenKind.LBRACKET)
			{
				parseLBracket();
				parseRBracket();
			}
			
		}
		
		private void parseStaticDec()
		{
			if (token.kind == TokenKind.STATIC_KW)
			{
				acceptIt();
			}
		}
		
		private void parseVoidDec()
		{
			if (token.kind == TokenKind.VOID_KW)
			{
				acceptIt();
			}
		}
		
		private void parseRBracket()
		{
			if (token.kind == TokenKind.RBRACKET)
			{
				acceptIt();
			}
		}
		
		private void parseLBracket()
		{
			if (token.kind == TokenKind.LBRACKET)
			{
				acceptIt();
			}
		}
		
		private void parseIntKW()
		{
			if (token.kind == TokenKind.INT_KW)
			{
				acceptIt();
			}
		}
		
		private void parseBooleanKW()
		{
			if (token.kind == TokenKind.BOOLEAN_KW)
			{
				acceptIt();
			}
			
		}
		
		private void parsePublicDec()
		{
			if (token.kind == TokenKind.PUBLIC_KW)
			{
				acceptIt();
			}
		}
		
		private void parsePrivateDec()
		{
			if (token.kind == TokenKind.PRIVATE_KW)
			{
				acceptIt();
			}
		}
		
		private void parseId()
		{
			if (token.kind == TokenKind.ID)
			{
				acceptIt();
			}
		}
		
		private void parseLBrace()
		{
			if (token.kind == TokenKind.LBRACE)
			{
				acceptIt();
			}
		}
		
		private void acceptIt() throws SyntaxError {
			accept(token.kind);
		}

		/**
		 * verify that current token in input matches expected token and advance to next token
		 * @param expectedToken
		 * @throws SyntaxError  if match fails
		 */
		private void accept(TokenKind expectedTokenKind) throws SyntaxError {
			if (token.kind == expectedTokenKind) {
				if (trace)
					pTrace();
				token = scanner.scan();
			}
			else
				parseError("expecting '" + expectedTokenKind +
						"' but found '" + token.kind + "'");
		}

		/**
		 * report parse error and unwind call stack to start of parse
		 * @param e  string with error detail
		 * @throws SyntaxError
		 */
		private void parseError(String e) throws SyntaxError {
			reporter.reportError("Parse error: " + e);
			throw new SyntaxError();
		}

		// show parse stack whenever terminal is  accepted
		private void pTrace() {
			StackTraceElement [] stl = Thread.currentThread().getStackTrace();
			for (int i = stl.length - 1; i > 0 ; i--) {
				if(stl[i].toString().contains("parse"))
					System.out.println(stl[i]);
			}
			System.out.println("accepting: " + token.kind + " (\"" + token.spelling + "\")");
			System.out.println();
		}
}
