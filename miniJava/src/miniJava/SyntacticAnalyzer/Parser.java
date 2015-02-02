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
			
			while (token.kind == TokenKind.EOL || token.kind == TokenKind.LINE_COMMENT
					|| token.kind == TokenKind.BLOCK_COMMENT_OPEN)
			{
				if (token.kind == TokenKind.LINE_COMMENT)
				{
					parseLineComment();
				}
				
				else if (token.kind == TokenKind.EOL)
				{
					parseEOL();
				}
				
				else if (token.kind == TokenKind.BLOCK_COMMENT_OPEN)
				{
					parseBlockComment();
				}
			}
			
			System.out.println(token.kind);
			try {
				parseProgram();
			}
			catch (SyntaxError e) { }
		}
		
		private void parseProgram() throws SyntaxError
		{
			while (true)
			{
				
				if (token.kind == TokenKind.CLASS_KW)
				{
					parseClassDec();
				}
				
				else
					break;
			}

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
					
					// Field Declaration
					if (token.kind == TokenKind.SEMICOLON)
					{
						parseSemicolon();
					}
					
					// Method Declaration
					else
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
		
		private void parseStatement()
		{
			if (token.kind == TokenKind.LBRACE)
			{
				parseLBrace();
				
				while (true)
				{
					if (token.kind != TokenKind.RBRACE)
					{
						parseStatement();
					}
					
					else
						break;
				}
				
				parseRBrace();
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
				parseBooleanKW();
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
			
			else if (token.kind == TokenKind.ID)
			{
				parseId();
				if (token.kind == TokenKind.ID)
				{
					// Type
					parseId();
					parseEquals();
					parseExpression();
					parseSemicolon();
				}
				
				else if (token.kind == TokenKind.LPAREN)
				{
					// Ref
					parseLParen();
					if (token.kind != TokenKind.RPAREN)
					{
						parseArgumentList();
					}
					
					parseRParen();
					parseSemicolon();
				}
				
				else if (token.kind == TokenKind.LBRACKET)
				{
					parseLBracket();
					
					if (token.kind == TokenKind.RBRACKET)
					{
						// Type
						parseRBracket();
						
						parseId();
						parseEquals();
						parseExpression();
						parseSemicolon();
					}
					
					else
					{
						// IxRef
						parseExpression();
						parseRBracket();
						
						parseEquals();
						parseExpression();
						parseSemicolon();
					}
				}
				
				else if (token.kind == TokenKind.PERIOD)
				{
					while (token.kind == TokenKind.PERIOD)
					{
						parsePeriod();
						parseId();
					}
					
					if (token.kind == TokenKind.LPAREN)
					{
						// Ref
						parseLParen();
						
						if (token.kind != TokenKind.RPAREN)
						{
							parseArgumentList();
						}
						
						parseRParen();
						parseSemicolon();
					}
					
					else if (token.kind == TokenKind.LBRACKET)
					{
						parseLBracket();
						if (token.kind == TokenKind.RBRACKET)
						{
							// Type
							parseRBracket();
							parseId();
							parseEquals();
							parseExpression();
							parseSemicolon();
						}
						
						else
						{
							// IxRef
							parseExpression();
							parseRBracket();
							
							parseEquals();
							parseExpression();
							parseSemicolon();
						}
					}
					
					else if (token.kind == TokenKind.EQUALS)
					{
						parseEquals();
						parseExpression();
						parseSemicolon();
					}
				}
				
				else
				{
					// IxRef from Equals
					parseEquals();
					parseExpression();
					parseSemicolon();
				}
			}
			
			else if (token.kind == TokenKind.THIS_KW)
			{
				parseThisKW();
				if (token.kind == TokenKind.LPAREN)
				{
					// Ref
					parseLParen();
					
					if (token.kind != TokenKind.RPAREN)
					{
						parseArgumentList();
					}
					
					parseRParen();
					parseSemicolon();
				}
				
				else if (token.kind == TokenKind.LBRACKET)
				{
					// IxRef
					parseLBracket();
					parseExpression();
					parseRBracket();
					
					parseEquals();
					parseExpression();
					parseSemicolon();
				}
				
				else if (token.kind == TokenKind.EQUALS)
				{
					parseEquals();
					parseExpression();
					parseSemicolon();
				}
				
				else if (token.kind == TokenKind.PERIOD)
				{
					while (token.kind == TokenKind.PERIOD)
					{
						parsePeriod();
						parseId();
					}
					
					if (token.kind == TokenKind.LPAREN)
					{
						// Ref
						parseLParen();
						
						if (token.kind != TokenKind.RPAREN)
						{
							parseArgumentList();
						}
						
						parseRParen();
						parseSemicolon();
					}
					
					else if (token.kind == TokenKind.LBRACKET)
					{
						// IxRef
						parseLBracket();
						parseExpression();
						parseRBracket();
						
						parseEquals();
						parseExpression();
						parseSemicolon();
					}
					
					else if (token.kind == TokenKind.EQUALS)
					{
						parseEquals();
						parseExpression();
						parseSemicolon();
					}
				}
				
				else
				{
					// IxRef from Equals
					parseEquals();
					parseExpression();
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
			
			else if (token.kind == TokenKind.SUBTRACT ||
					token.kind == TokenKind.UNOP)
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
					if (token.kind != TokenKind.RPAREN)
					{
						parseArgumentList();
					}
					
					parseRParen();
				}
				
				else if (token.kind == TokenKind.LBRACKET)
				{
						parseLBracket();
						parseExpression();
						parseRBracket();
				}
				
			}
			
			if (token.kind == TokenKind.BINOP || token.kind == TokenKind.SUBTRACT)
			{
				parseBinop();
				parseExpression();
			}
			
			/*else
			{
				accept(TokenKind.EXPRESSION);
			}*/
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
			
			else
			{
				parseId();
			}
			
			if (token.kind == TokenKind.LBRACKET)
			{
				parseLBracket();
				parseRBracket();
			}
			
		}
		
		private void parseLineComment()
		{
			//accept(TokenKind.LINE_COMMENT);
			while (token.kind != TokenKind.EOL && token.kind != TokenKind.EOT)
				token = scanner.scan();
			System.out.println(token.kind);
		//	accept(TokenKind.EOL);
		}
		
		private void parseBlockComment()
		{
			//accept(TokenKind.BLOCK_COMMENT_OPEN);
			while (token.kind != TokenKind.BLOCK_COMMENT_CLOSE &&
					token.kind != TokenKind.EOT)
			{
				token = scanner.scan();
			}
			
			System.out.println(token.kind);
			
			if (token.kind == TokenKind.EOT)
				accept(TokenKind.EOT);
			
			//accept(TokenKind.BLOCK_COMMENT_CLOSE);
			else
			{
				token = scanner.scan();
				System.out.println(token.kind);
			}
		}
		
		private void parseComma()
		{
			accept(TokenKind.COMMA);
		}
		
		private void parseRParen()
		{
			accept(TokenKind.RPAREN);
		}
		
		private void parseLParen()
		{
			accept(TokenKind.LPAREN);
		}
		
		private void parseSemicolon()
		{
			accept(TokenKind.SEMICOLON);
		}
		
		private void parseBinop()
		{
			if (token.kind == TokenKind.SUBTRACT)
				accept(TokenKind.SUBTRACT);
			else
				accept(TokenKind.BINOP);
		}
		
		private void parseUnop()
		{
			if (token.kind == TokenKind.SUBTRACT)
				accept(TokenKind.SUBTRACT);
			else
				accept(TokenKind.UNOP);
		}
		
		private void parseNum()
		{
			accept(TokenKind.NUM);
		}
		
		private void parseTrueKW()
		{
			accept(TokenKind.TRUE_KW);
		}
		
		private void parseFalseKW()
		{
			accept(TokenKind.FALSE_KW);
		}
		
		private void parseNewKW()
		{
			accept(TokenKind.NEW_KW);
		}
		
		private void parseThisKW()
		{
			accept(TokenKind.THIS_KW);
		}
		
		private void parsePeriod()
		{
			accept(TokenKind.PERIOD);
		}
		
		private void parseEquals()
		{
			accept(TokenKind.EQUALS);
		}
		
		private void parseWhileKW()
		{
			accept(TokenKind.WHILE_KW);
		}
		
		private void parseElseKW()
		{
			accept(TokenKind.ELSE_KW);
		}
		
		private void parseIfKW()
		{
			accept(TokenKind.IF_KW);
		}
		
		private void parseStaticDec()
		{
			accept(TokenKind.STATIC_KW);
		}
		
		private void parseVoidDec()
		{
			accept(TokenKind.VOID_KW);
		}
		
		private void parseRBracket()
		{
			accept(TokenKind.RBRACKET);
		}
		
		private void parseLBracket()
		{
			accept(TokenKind.LBRACKET);
		}
		
		private void parseIntKW()
		{
			accept(TokenKind.INT_KW);
		}
		
		private void parseClassKW()
		{
			accept(TokenKind.CLASS_KW);
			
		}
		
		private void parseReturn()
		{
			accept(TokenKind.RETURN_KW);
		}
		
		private void parseRBrace()
		{
			accept(TokenKind.RBRACE);
		}
		
		private void parseBooleanKW()
		{
			accept(TokenKind.BOOLEAN_KW);		
		}
		
		private void parsePublicDec()
		{
			accept(TokenKind.PUBLIC_KW);
		}
		
		private void parsePrivateDec()
		{
			accept(TokenKind.PRIVATE_KW);
		}
		
		private void parseId()
		{
			accept(TokenKind.ID);
		}
		
		private void parseLBrace()
		{
			accept(TokenKind.LBRACE);
		}
		
		private void parseEOL()
		{
			accept(TokenKind.EOL);
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
				System.out.println(token.kind);
				
				while (token.kind == TokenKind.EOL || token.kind == TokenKind.LINE_COMMENT
						|| token.kind == TokenKind.BLOCK_COMMENT_OPEN)
				{
					if (token.kind == TokenKind.LINE_COMMENT)
					{
						parseLineComment();
					}
					
					else if (token.kind == TokenKind.EOL)
					{
						parseEOL();
					}
					
					else if (token.kind == TokenKind.BLOCK_COMMENT_OPEN)
					{
						parseBlockComment();
					}
				}
				
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
