package miniJava.SyntacticAnalyzer;
import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

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
		
		private Package parseProgram() throws SyntaxError
		{
			Package programAst = null;
			ClassDeclList clListAst = parseClassDecList();
			
			programAst = new Package(clListAst, null);
			accept(TokenKind.EOT);
			ASTDisplay disp = new ASTDisplay();
			disp.showTree(programAst);
			return programAst;
		}
		
		private ClassDeclList parseClassDecList() throws SyntaxError
		{
			ClassDeclList clListAst = new ClassDeclList();
			while (true)
			{
				if (token.kind == TokenKind.CLASS_KW)
				{
					ClassDecl clAst = parseClassDec();
					clListAst.add(clAst);
				}
				else
					break;
			}
			return clListAst;
		}
		
		private ClassDecl parseClassDec() throws SyntaxError
		{
			ClassDecl clAst = null;
			FieldDeclList flAst = new FieldDeclList();
			MethodDeclList mlAst = new MethodDeclList();
			
			parseClassKW();
			
			String className = token.spelling;
			parseId();
			
			parseLBrace();
			
			while (true)
			{
				if (token.kind == TokenKind.PUBLIC_KW || token.kind == TokenKind.PRIVATE_KW
					|| token.kind == TokenKind.STATIC_KW || token.kind == TokenKind.STATIC_KW
					|| token.kind == TokenKind.VOID_KW || token.kind == TokenKind.ID ||
					token.kind == TokenKind.INT_KW || token.kind == TokenKind.BOOLEAN_KW)
				{
					MemberDecl mdAst = parseMemberDecl();
					
					// Field Declaration - object md represents it
					if (token.kind == TokenKind.SEMICOLON)
					{
						parseSemicolon();
						flAst.add((FieldDecl)mdAst);
					}
					
					// Method Declaration
					else
					{
						ParameterDeclList pdlAst = null;
						StatementList stlAst = null;
						Expression returnExp = null;
						parseLParen();
						if (token.kind == TokenKind.RPAREN)
						{
							parseRParen();
							// Keep pdlAst null
						}
						
						else
						{
							pdlAst = parseParameterList();
							parseRParen();
						}
						
						parseLBrace();
						
						while (true)
						{
							if (token.kind == TokenKind.IF_KW || token.kind == TokenKind.WHILE_KW ||
									token.kind == TokenKind.THIS_KW || token.kind == TokenKind.ID ||
									token.kind == TokenKind.INT_KW || token.kind == TokenKind.BOOLEAN_KW)
							{
								stlAst = parseStatementList();
							}
							
							else
								break;
						}
						
						if (token.kind == TokenKind.RETURN_KW)
						{
							parseReturn();
							returnExp = parseExpression();
							parseSemicolon();
						}
						
						parseRBrace();
						mdAst = new MethodDecl(mdAst, pdlAst, stlAst, returnExp, null);
						mlAst.add((MethodDecl)mdAst);
					}
				}
				
				else
					break;
			}
			
			clAst = new ClassDecl(className, flAst, mlAst, null);
			parseRBrace();
			return clAst;
		}
		
		private StatementList parseStatementList()
		{
			StatementList stlAst = new StatementList();		
			Statement stAst = null;
			if (token.kind == TokenKind.LBRACE)
			{
				parseLBrace();		
				while (true)
				{
					if (token.kind != TokenKind.RBRACE)
					{
						stAst = parseStatement();
						stlAst.add(stAst);
					}
					
					else
						break;
				}
				parseRBrace();
			}
			
			else {
				stAst = parseStatement();
				stlAst.add(stAst);
			}
			return stlAst;
		}
		
		private Statement parseStatement()
		{
			Statement stAst = null;
			if (token.kind == TokenKind.IF_KW)
			{
				stAst = parseIfKW();
			}
			
			else if (token.kind == TokenKind.WHILE_KW)
			{
				stAst = parseWhileKW();
			}
			
			else if (token.kind == TokenKind.BOOLEAN_KW)
			{
				stAst = parseVarDeclStmt();
			}
			
			else if (token.kind == TokenKind.INT_KW)
			{
				stAst = parseVarDeclStmt();
			}
			
			else if (token.kind == TokenKind.ID)
			{
				Type tAst = null;
				Expression eAst = null;
				String name = null;
				Identifier typeId = new Identifier(token, null);
				Reference idR = new IdRef(typeId, null);
				parseId();
				
				if (token.kind == TokenKind.ID)
				{
					// Type
					VarDeclStmt vdsAst = null;
					VarDecl vd = null;
					name = token.spelling;
					tAst = new ClassType(typeId, null);
					parseId();
					parseEquals();
					eAst = parseExpression();
					parseSemicolon();
					
					vd = new VarDecl(tAst, name, null);
					vdsAst = new VarDeclStmt(vd, eAst, null);
					stAst = vdsAst;
				}
				
				else if (token.kind == TokenKind.LPAREN)
				{
					// Ref
					CallStmt cstAst = null;
					ExprList elAst = null;
					parseLParen();
					if (token.kind != TokenKind.RPAREN)
					{
						elAst = parseArgumentList();
					}
					
					parseRParen();
					parseSemicolon();
					cstAst = new CallStmt(idR, elAst, null);
					stAst = cstAst;
				}
				
				else if (token.kind == TokenKind.LBRACKET)
				{
					parseLBracket();
					
					if (token.kind == TokenKind.RBRACKET)
					{
						// Type
						parseRBracket();
						VarDeclStmt vdsAst = null;
						VarDecl vd = null;
						name = token.spelling;
						tAst = new ClassType(typeId, null);
						tAst = new ArrayType(tAst, null);
						
						parseId();
						parseEquals();
						eAst = parseExpression();
						parseSemicolon();
						
						vd = new VarDecl(tAst, name, null);
						vdsAst = new VarDeclStmt(vd, eAst, null);
						stAst = vdsAst;
					}
					
					else
					{
						// IxRef
						AssignStmt asAst = null;
						IndexedRef irAst = null;
						eAst = parseExpression();
						parseRBracket();
						irAst = new IndexedRef(idR, eAst, null);
						
						parseEquals();
						eAst = parseExpression();
						parseSemicolon();
						asAst = new AssignStmt(irAst, eAst, null);
						stAst = asAst;
					}
				}
				
				else if (token.kind == TokenKind.PERIOD)
				{
					while (token.kind == TokenKind.PERIOD)
					{
						parsePeriod();
						Identifier typeId2 = new Identifier(token, null);
						idR = new QualifiedRef(idR, typeId2, null);
						parseId();
					}
					
					if (token.kind == TokenKind.LPAREN)
					{
						// Ref
						parseLParen();
						CallStmt cstAst = null;
						ExprList expListAst = null;
						if (token.kind != TokenKind.RPAREN)
						{
							expListAst = parseArgumentList();
						}
						
						parseRParen();
						parseSemicolon();
						cstAst = new CallStmt(idR, expListAst, null);
						stAst = cstAst;
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
							AssignStmt asAst = null;
							IndexedRef irAst = null;
							eAst = parseExpression();
							parseRBracket();
							irAst = new IndexedRef(idR, eAst, null);
							
							parseEquals();
							eAst = parseExpression();
							parseSemicolon();
							asAst = new AssignStmt(irAst, eAst, null);
							stAst = asAst;
						}
					}
					
					else if (token.kind == TokenKind.EQUALS)
					{
						// IxRef
						AssignStmt asAst = null;
						parseEquals();
						eAst = parseExpression();
						parseSemicolon();
						asAst = new AssignStmt(idR, eAst, null);
						stAst = asAst;
					}
				}
				
				else
				{
					// IxRef from Equals
					AssignStmt asAst = null;
					parseEquals();
					eAst = parseExpression();
					parseSemicolon();
					asAst = new AssignStmt(idR, eAst, null);
					stAst = asAst;
				}
			}
			
			else if (token.kind == TokenKind.THIS_KW)
			{
				Reference tr = new ThisRef(null);
				Expression eAst = null;
				parseThisKW();
				if (token.kind == TokenKind.LPAREN)
				{
					// Ref
					parseLParen();
					CallStmt csAst = null;
					ExprList expListAst = null;
					if (token.kind != TokenKind.RPAREN)
					{
						expListAst = parseArgumentList();
					}
					
					parseRParen();
					parseSemicolon();
					csAst = new CallStmt(tr, expListAst, null);
					stAst = csAst;
				}
				
				else if (token.kind == TokenKind.LBRACKET)
				{
					// IxRef
					parseLBracket();
					AssignStmt asAst = null;
					IndexedRef irRef = null;				
					eAst = parseExpression();
					parseRBracket();
					irRef = new IndexedRef(tr, eAst, null);
					
					parseEquals();
					eAst = parseExpression();
					parseSemicolon();
					asAst = new AssignStmt(irRef, eAst, null);
					stAst = asAst;
				}
				
				else if (token.kind == TokenKind.EQUALS)
				{
					AssignStmt asAst = null;
					parseEquals();
					eAst = parseExpression();
					parseSemicolon();
					asAst = new AssignStmt(tr, eAst, null);
					stAst = asAst;
				}
				
				else if (token.kind == TokenKind.PERIOD)
				{
					Identifier idR = null;
					
					while (token.kind == TokenKind.PERIOD)
					{
						parsePeriod();
						idR = new Identifier(token, null);
						parseId();
						tr = new QualifiedRef(tr, idR, null);
					}
					
					if (token.kind == TokenKind.LPAREN)
					{
						// Ref
						parseLParen();
						CallStmt csAst = null;
						ExprList expListAst = null;
						if (token.kind != TokenKind.RPAREN)
						{
							expListAst = parseArgumentList();
						}
						
						parseRParen();
						parseSemicolon();
						csAst = new CallStmt(tr, expListAst, null);
						stAst = csAst;
					}
					
					else if (token.kind == TokenKind.LBRACKET)
					{
						// IxRef
						AssignStmt asAst = null;
						IndexedRef irRef = null;
						parseLBracket();
						eAst = parseExpression();
						parseRBracket();
						irRef = new IndexedRef(tr, eAst, null);
						
						parseEquals();
						eAst = parseExpression();
						parseSemicolon();
						asAst = new AssignStmt(irRef, eAst, null);
						stAst = asAst;
					}
					
					else if (token.kind == TokenKind.EQUALS)
					{
						AssignStmt asAst = null;
						parseEquals();
						eAst = parseExpression();
						parseSemicolon();
						asAst = new AssignStmt(tr, eAst, null);
						stAst = asAst;
					}
				}
				
				else
				{
					// IxRef from Equals
					AssignStmt asAst = null;
					parseEquals();
					eAst = parseExpression();
					parseSemicolon();
					asAst = new AssignStmt(tr, eAst, null);
					stAst = asAst;
				}
			}
			return stAst;
		}
		
		private VarDeclStmt parseVarDeclStmt() {
			VarDeclStmt vdsAst = null;
			Expression eAst = null;
			BaseType bt = null;
			String typeName = null;
			
			if (token.kind == TokenKind.BOOLEAN_KW)
			{
				parseBooleanKW();
				bt = new BaseType(TypeKind.BOOLEAN, null);
				
				typeName = token.spelling;
				parseId();
				parseEquals();
				
				eAst = parseExpression();
				parseSemicolon();
			}
			
			else if (token.kind == TokenKind.INT_KW)
			{
				parseIntKW();
				
				if (token.kind == TokenKind.LBRACKET)
				{
					parseLBracket();
					parseRBracket();
					bt = new BaseType(TypeKind.ARRAY, null);
				}
				
				else
					bt = new BaseType(TypeKind.INT, null);
				
				typeName = token.spelling;
				parseId();
				parseEquals();
				eAst = parseExpression();
				parseSemicolon();
			}
			
			VarDecl vd = new VarDecl(bt, typeName, null);
			vdsAst = new VarDeclStmt(vd, eAst, null);
			return vdsAst;
		}

		private ExprList parseArgumentList()
		{
			ExprList expListAst = new ExprList();
			Expression eAst = parseExpression();
			expListAst.add(eAst);
			while (true)
			{
				if (token.kind == TokenKind.COMMA)
				{
					parseComma();
					eAst = parseExpression();
					expListAst.add(eAst);
				}
				
				else
					break;
			}
			
			return expListAst;
		}
		
		private Expression parseExpression()
		{
			Expression eAst = null;
			if (token.kind == TokenKind.NEW_KW)
			{
				parseNewKW();
				NewExpr newAst = null;
				Type bt = null;
				Expression eAst2 = null;
				
				if (token.kind == TokenKind.INT_KW)
				{
					bt = new BaseType(TypeKind.INT, null);
					parseIntKW();
					
					parseLBracket();
					eAst2 = parseExpression();
					parseRBracket();
					
					newAst = new NewArrayExpr(bt, eAst2, null);
				}
				
				else
				{
					Token classIdentifier = token;
					parseId();
					
					if (token.kind == TokenKind.LPAREN)	// Object
					{
						Identifier cid = new Identifier(classIdentifier, null);
						ClassType ct = new ClassType(cid, null);
						parseLParen();
						parseRParen();
						
						newAst = new NewObjectExpr(ct, null);
					}
					
					else if (token.kind == TokenKind.LBRACKET)	// Array
					{
						bt = new BaseType(TypeKind.CLASS, null);
						
						parseLBracket();
						eAst2 = parseExpression();
						parseRBracket();
						
						newAst = new NewArrayExpr(bt, eAst2, null);
					}
				}
				eAst = newAst;
				eAst = binopExpression(eAst);
			}
			
			else if (token.kind == TokenKind.NUM)
			{
				LiteralExpr lexp = parseNum();
				eAst = lexp;
				eAst = binopExpression(eAst);
			}
			
			else if (token.kind == TokenKind.TRUE_KW)
			{
				LiteralExpr lexp = parseTrueKW();
				eAst = lexp;
				eAst = binopExpression(eAst);
			}
			
			else if (token.kind == TokenKind.FALSE_KW)
			{
				LiteralExpr lexp = parseFalseKW();
				eAst = lexp;
				eAst = binopExpression(eAst);
			}
			
			else if (token.kind == TokenKind.LPAREN)
			{
				parseLParen();
				parseExpression();
				parseRParen();
				eAst = binopExpression(eAst);
			}
			
			else if (token.kind == TokenKind.SUBTRACT ||
					token.kind == TokenKind.UNOP)
			{
				Operator op = new Operator(token, null);
				Expression eAst2 = null;
				parseUnop();
				if (token.kind == TokenKind.SUBTRACT)
					parseError("Not a valid miniJava expression.");
				else
				{
					eAst2 = parseExpression();
					UnaryExpr uexp = new UnaryExpr(op, eAst2, null);
					eAst = uexp;
				}
				eAst = binopExpression(eAst);
			}
			
			else  //if (token.kind == TokenKind.THIS_KW || token.kind == TokenKind.ID)
			{
				Reference refAst = null;
				
				if (token.kind == TokenKind.THIS_KW)
				{
					parseThisKW();
					refAst = new ThisRef(null);
				}
				
				else //if (token.kind == TokenKind.ID)
				{
					Identifier someId = new Identifier(token, null);
					parseId();
					refAst = new IdRef(someId, null);
				}

				while (true)
				{
					
					if (token.kind == TokenKind.PERIOD)
					{
						parsePeriod();
						Identifier nextId = new Identifier(token, null);
						parseId();
						refAst = new QualifiedRef(refAst, nextId, null);
					}
					
					else
						break;
				}
				
				if (token.kind == TokenKind.LPAREN)		// IxReference (ArgList?)
				{
					parseLParen();
					CallExpr cexp = null;
					ExprList expListAst = null;
					// ( ArgumentList )?
					if (token.kind != TokenKind.RPAREN)
					{
						expListAst = parseArgumentList();
					}
					
					parseRParen();
					cexp = new CallExpr(refAst, expListAst, null);
					eAst = cexp;
				}
				
				else if (token.kind == TokenKind.LBRACKET)	// IxReference
				{
					RefExpr rexp = null;
					parseLBracket();
					Expression someAst = parseExpression();
					parseRBracket();
					refAst = new IndexedRef(refAst, someAst, null);
					
					rexp = new RefExpr(refAst, null);
					eAst = rexp;
				}
				
				else if (token.kind == TokenKind.BINOP || token.kind == TokenKind.SUBTRACT
						|| token.kind == TokenKind.SEMICOLON)
				{
					RefExpr rexp = new RefExpr(refAst, null);
					eAst = rexp;
					eAst = binopExpression(eAst);
				}
			}
			
			/*if (token.kind == TokenKind.BINOP || token.kind == TokenKind.SUBTRACT)
			{
				Operator expOp = new Operator(token, null);
				parseBinop();
				if (expOp.kind == TokenKind.SUBTRACT)
				{
					if (token.kind == TokenKind.SUBTRACT)
						parseError("Not a valid miniJava expression.");
				}
				Expression eAst2 = parseExpression();
				eAst = new BinaryExpr(expOp, eAst, eAst2, null);
			}*/
			
			return eAst;
		}
		
		private Expression binopExpression(Expression thisAst) 
		{
			Expression eAst = thisAst;
			if (token.kind == TokenKind.BINOP || token.kind == TokenKind.SUBTRACT)
			{
				Operator expOp = new Operator(token, null);
				parseBinop();
				
				if (expOp.kind == TokenKind.SUBTRACT)
				{
					if (token.kind == TokenKind.SUBTRACT)
						parseError("Not a valid miniJava expression.");
				}
				
//				else if (expOp.spelling == "||")
//				{
//					Expression eAst2 = parseExpression();
//					
//					eAst = new BinaryExpr(expOp, eAst, eAst2, null);
//				}
				
				Expression eAst2 = parseExpression();
				eAst = new BinaryExpr(expOp, eAst, eAst2, null);
			}
			return eAst;
		}
		
		private ParameterDeclList parseParameterList()
		{
			ParameterDeclList pdlAst = new ParameterDeclList();
			ParameterDecl pdAst = null;
			
			//TypeKind tk = parseType();
			//BaseType bt = new BaseType(tk, null);
			Type bt = parseType();
			
			String name = token.spelling;
			parseId();
			
			pdAst = new ParameterDecl(bt, name, null);
			pdlAst.add(pdAst);
			
			while (true)
			{
				ParameterDecl pd2Ast = null;
				if (token.kind == TokenKind.COMMA)
				{
					parseComma();
					//TypeKind tk2 = parseType();
					//BaseType bt2 = new BaseType(tk2, null);
					Type bt2 = parseType();
					String name2 = token.spelling;
					parseId();
					pd2Ast = new ParameterDecl(bt2, name2, null);
					pdlAst.add(pd2Ast);
				}
				
				else
					break;
			}
			return pdlAst;
		}
		
		private MemberDecl parseMemberDecl()
		{
			MemberDecl md = null;
			boolean isPrivate = false, isStatic = false;
			
			Type memType = null;
			String name = null;
			
			if (token.kind == TokenKind.PUBLIC_KW)
			{
				isPrivate = false;
				parsePublicDec();
			}
			
			else if (token.kind == TokenKind.PRIVATE_KW)
			{
				isPrivate = true;
				parsePrivateDec();
			}
			
			if (token.kind == TokenKind.STATIC_KW)
			{
				isStatic = true;
				parseStaticDec();
			}
			
			if (token.kind == TokenKind.VOID_KW)
			{
				memType = new BaseType(TypeKind.VOID, null);
				parseVoidDec();
			}
			
			else
			{
				//TypeKind type = parseType();
				//memType = new BaseType(type, null);
				memType = parseType();
			}
			
			name = token.spelling;
			parseId();

			md = new FieldDecl(isPrivate, isStatic, memType, name, null);
			return md;
		}
		
		private Type parseType()
		{
			Type typeReturned = null;
			if (token.kind == TokenKind.BOOLEAN_KW)
			{
				parseBooleanKW();
				typeReturned = new BaseType(TypeKind.BOOLEAN, null);
				return typeReturned;
			}
			
			else if (token.kind == TokenKind.INT_KW)
			{
				parseIntKW();
				typeReturned = new BaseType(TypeKind.INT, null);
				if (token.kind == TokenKind.LBRACKET)
				{
					parseLBracket();
					parseRBracket();
					typeReturned = new ArrayType(typeReturned, null);
				}
				return typeReturned;
			}
			
			else
			{
				Identifier id = new Identifier(token, null);
				parseId();
				typeReturned = new ClassType(id, null);
				if (token.kind == TokenKind.LBRACKET)
				{
					parseLBracket();
					parseRBracket();
					typeReturned = new ArrayType(typeReturned, null);
				}
				return typeReturned;
			}
		}
		
		private void parseLineComment()
		{
			//accept(TokenKind.LINE_COMMENT);
			while (token.kind != TokenKind.EOL)
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
			
			accept(TokenKind.BLOCK_COMMENT_CLOSE);
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
		
		private LiteralExpr parseNum()
		{
			LiteralExpr lexp = null;
			IntLiteral ilexp = new IntLiteral(token, null);
			lexp = new LiteralExpr(ilexp, null);
			accept(TokenKind.NUM);
			return lexp;
		}
		
		private LiteralExpr parseTrueKW()
		{
			LiteralExpr lexp = null;
			BooleanLiteral blexp = new BooleanLiteral(token, null);
			lexp = new LiteralExpr(blexp, null);
			accept(TokenKind.TRUE_KW);
			return lexp;
		}
		
		private LiteralExpr parseFalseKW()
		{
			LiteralExpr lexp = null;
			BooleanLiteral blexp = new BooleanLiteral(token, null);
			lexp = new LiteralExpr(blexp, null);
			accept(TokenKind.FALSE_KW);
			return lexp;
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
		
		private WhileStmt parseWhileKW()
		{
			WhileStmt whileAst = null;
			accept(TokenKind.WHILE_KW);
			
			parseLParen();
			Expression eAst = parseExpression();
			parseRParen();
			Statement stAst = parseStatement();
			whileAst = new WhileStmt(eAst, stAst, null);
			
			return whileAst;
		}
		
		private void parseElseKW()
		{
			accept(TokenKind.ELSE_KW);
		}
		
		private IfStmt parseIfKW()
		{
			IfStmt ifAst = null;
			accept(TokenKind.IF_KW);
			
			parseLParen();
			Expression eAst = parseExpression();
			parseRParen();
			Statement stAst = parseStatement();
			Statement stAst2 = null;
			if (token.kind == TokenKind.ELSE_KW)
			{
				parseElseKW();
				stAst2 = parseStatement();
			}
			ifAst = new IfStmt(eAst, stAst, stAst2, null);
			return ifAst;
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
