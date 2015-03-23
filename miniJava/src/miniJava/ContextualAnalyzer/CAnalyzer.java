package miniJava.ContextualAnalyzer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.*;

public class CAnalyzer implements Visitor<IdentificationTable, Object>{

	IdentificationTable aTable = null;
	ErrorReporter reporter = null;
	MethodDecl mainMethod = null, presentMethodDecl = null;
	VarDecl presentVarDecl = null;
	ClassDecl presentClassDecl = null;
	
	@Override
	public Type visitPackage(Package prog, IdentificationTable arg) {
		aTable = arg;
		reporter = new ErrorReporter();
		
		ArrayList<String> predefDecl = new ArrayList<String>();
		predefDecl.add("class String { }");
		predefDecl.add("class _PrintStream { public void println(int n){} }");
		predefDecl.add("class System { public static _PrintStream out; }");
	
		for (String a : predefDecl) {
			Scanner scan;
			try {
				scan = new Scanner(a, reporter, true);
				Parser parse = new Parser(scan, reporter);
				Package progP = parse.parse();
				ClassDecl cd = progP.classDeclList.get(0);
				handleClassDec(cd, arg);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		for (ClassDecl cDecl : prog.classDeclList) {
			handleClassDec(cDecl, arg);
		}
		
		for (ClassDecl cDecl : prog.classDeclList) {
			cDecl.visit(this, arg);
		}
		
		if (mainMethod == null)
			reporter.reportError("***Must have main method");
		return null;
	}
	
	@Override
	public Type visitClassDecl(ClassDecl cd, IdentificationTable arg) {
		presentClassDecl = cd;
		for (FieldDecl fdecl : cd.fieldDeclList) {
			fdecl.visit(this, cd.idTable);
		}
		
		for (MethodDecl mdecl : cd.methodDeclList) {
			mdecl.visit(this, cd.idTable);
		}
		
		return null;
	}

	@Override
	public Type visitFieldDecl(FieldDecl fd, IdentificationTable arg) {
		fd.type.visit(this, arg);
		if(fd.type.typeKind == TypeKind.VOID) {
			reporter.reportError("***Field " + fd.name + " at " + fd.posn + " cannot have type 'void'");
		}
		return null;
	}
	
	@Override
	public Type visitMethodDecl(MethodDecl md, IdentificationTable arg) {
		presentMethodDecl = md;
		if (isMainMethod(md)){
			if (mainMethod != null)
				reporter.reportError("***Main method at" + md.posn + " already exists at " + mainMethod.posn);
			else
				mainMethod = md;
		}	
		
		md.type.visit(this, arg);
		for (ParameterDecl pd : md.parameterDeclList) {
			pd.visit(this, md.idTable);
		}
		
		for (Statement st : md.statementList) {
			st.visit(this, md.idTable);
		}

		if(md.type.typeKind != TypeKind.VOID && md.returnExp == null) {
			reporter.reportError("***Method " + md.name + " at " + md.posn + " should have a return expression");
		} 
		else if(md.type.typeKind == TypeKind.VOID && md.returnExp != null) {
			reporter.reportError("***Method " + md.name + " at " + md.posn + " should not have a return expression");
		}
		
		if (md.returnExp != null) {
			Type aType = (Type)md.returnExp.visit(this, md.idTable);
			if (aType.typeKind != md.type.typeKind){
				reporter.reportError("***Expected " + md.type + " but got " + aType + " at " + md.returnExp.posn);
			}
		}
		return null;
	}

	@Override
	public Type visitParameterDecl(ParameterDecl pd, IdentificationTable arg) {
		arg.enter(pd.name, pd);
		pd.type.visit(this, arg);
		if(pd.type.typeKind == TypeKind.VOID) {
			reporter.reportError("***Parameter " + pd.name + " at " + pd.posn + " cannot have type 'void'");
		}
		return null;
	}

	@Override
	public Type visitVarDecl(VarDecl decl, IdentificationTable arg) {
		arg.enter(decl.name, decl);
		decl.type.visit(this, arg);
		if(decl.type.typeKind == TypeKind.VOID) {
			reporter.reportError("***Variable " + decl.name + " at " + decl.posn + " cannot have type 'void'");
		}
		return null;
	}

	@Override
	public Type visitBaseType(BaseType type, IdentificationTable arg) {
		return type;
	}

	@Override
	public Type visitClassType(ClassType type, IdentificationTable arg) {
		type.className.visit(this, arg);
		
		String name = type.className.spelling;
		if (aTable.retrieveInScope(name) == null) {
			reporter.reportError("***Class '" + name + "' undeclared at " + type.posn);
		}
		return type;
	}

	@Override
	public Type visitArrayType(ArrayType type, IdentificationTable arg) {
		type.eltType.visit(this, arg);
		return type;
	}

	@Override
	public Type visitBlockStmt(BlockStmt stmt, IdentificationTable arg) {
		arg.openScope();
		
		for (Statement st : stmt.sl) {
			st.visit(this, arg);
		}
		
		arg.closeScope();
		return null;
	}

	@Override
	public Type visitVardeclStmt(VarDeclStmt stmt, IdentificationTable arg) {
		stmt.varDecl.visit(this, arg);
		
		presentVarDecl = stmt.varDecl;
		Type varDeclType = (Type)stmt.initExp.visit(this, arg);
		presentVarDecl = null;
		
		if (!checkEquality(varDeclType,stmt.varDecl.type))
			reporter.reportError("***Expected " + stmt.varDecl.type + " but got " + varDeclType + " at " + stmt.initExp.posn);
		
		return null;
	}

	@Override
	public Type visitAssignStmt(AssignStmt stmt, IdentificationTable arg) {
		Type stmtRefType = (Type)stmt.ref.visit(this, arg);
		Type stmtValType = (Type)stmt.val.visit(this, arg);
		
		if (!checkEquality(stmtRefType,stmtValType))
			reporter.reportError("***Expected " + stmtRefType + " but got " + stmtValType + " at " + stmt.posn);

		if(stmt.ref instanceof ThisRef)
			reporter.reportError("***Should not use 'this' keyword at " + stmt.posn);

		else if(stmt.ref.decl instanceof MethodDecl)
			reporter.reportError("***Should not assign a value to a method at " + stmt.posn);

		else if(stmt.ref instanceof QualifiedRef) {
			QualifiedRef ref = (QualifiedRef) stmt.ref;
			if(ref.ref.decl.type.typeKind == TypeKind.ARRAY && ref.id.spelling.equals("length"))
				reporter.reportError("***Cannot use length field at " + stmt.posn);
		}
		return null;
	}

	@Override
	public Type visitCallStmt(CallStmt stmt, IdentificationTable arg) {
		stmt.methodRef.visit(this, arg);
		
		MethodDecl md = (MethodDecl) stmt.methodRef.decl;
		if (md.parameterDeclList.size() != stmt.argList.size()) {
			reporter.reportError("***Expected " + md.parameterDeclList.size() + " number of parameters at " + stmt.posn
					+ " but got " + stmt.argList.size() + " parameters");
			return null;
		}
		
		for (int i = 0; i < md.parameterDeclList.size(); i++) {
			Expression exp = stmt.argList.get(i);
			Type expType = (Type)exp.visit(this, arg);
			ParameterDecl pd = md.parameterDeclList.get(i);
			
			if (!checkEquality(expType,pd.type))
				reporter.reportError("***Parameter " + pd.name + " is of type " + pd.type + " but got " + expType + " at " + exp.posn);
		}
		return null;
	}

	@Override
	public Type visitIfStmt(IfStmt stmt, IdentificationTable arg) {
		Type condType = (Type)stmt.cond.visit(this, arg);
		if(condType.typeKind != TypeKind.BOOLEAN)
			reporter.reportError("***Expected boolean but got " + condType + " at " + condType.posn);
		
		if(stmt.thenStmt instanceof VarDeclStmt) {
			reporter.reportError("***Cannot have only VarDecl statement in a conditional at " + stmt.thenStmt.posn);
			return null;
		}
		stmt.thenStmt.visit(this, arg);
		if (stmt.elseStmt != null) {
			if(stmt.elseStmt instanceof VarDeclStmt) {
				reporter.reportError("***Cannot have only VarDecl statement in a conditional at " + stmt.elseStmt.posn);
				return null;
			}
			stmt.elseStmt.visit(this, arg);
		}
		return null;
	}

	@Override
	public Type visitWhileStmt(WhileStmt stmt, IdentificationTable arg) {
		Type condType = (Type)stmt.cond.visit(this, arg);
		if (condType.typeKind != TypeKind.BOOLEAN)
			reporter.reportError("***Expected boolean but got " + condType + " at " + condType.posn);
		
		if(stmt.body instanceof VarDeclStmt) {
			reporter.reportError("***Cannot have only VarDecl statement in a conditional at " + stmt.body.posn);
			return null;
		}
		stmt.body.visit(this, arg);
		return null;
	}

	@Override
	public Type visitUnaryExpr(UnaryExpr expr, IdentificationTable arg) {
		Type expType = (Type)expr.expr.visit(this, arg);
		Type opType = (Type)expr.operator.visit(this, arg);
		if (!checkEquality(expType, opType)) {
			reporter.reportError("***Expression type at " + expType.posn + " does not match Operator " +
					expr.operator.spelling + " type which is " + opType);
		}
		return opType;
	}

	@Override
	public Type visitBinaryExpr(BinaryExpr expr, IdentificationTable arg) {
		Type exp1 = (Type)expr.left.visit(this, arg);
		Type exp2 = (Type)expr.right.visit(this, arg);
	
		String operator = expr.operator.spelling;
		if (operator.equals("==") || operator.equals("!=")) {
			if (!checkEquality(exp1, exp2)) {
				reporter.reportError("***Left expression type " + exp1.typeKind + " at " + exp1.posn + " does not match "
				+ "right expression type " + exp2.typeKind + " at " + exp2.posn);
			}
			return new BaseType(TypeKind.BOOLEAN, expr.posn);
		}
		
		else if (operator.equals("&&") || operator.equals("||")){
			BaseType tmp = new BaseType(TypeKind.BOOLEAN, expr.posn);
			if (!checkEquality(exp1, tmp))
				reporter.reportError("The type " + tmp + " and " + exp1 + " don't match");
			if (!checkEquality(exp2, tmp))
				reporter.reportError("The type " + tmp + " and " + exp2 + " don't match");
		}
		
		else if (operator.equals(">") || operator.equals("<") || operator.equals("+")
				|| operator.equals("-") || operator.equals("*") || operator.equals("/")
				|| operator.equals("<=") || operator.equalsIgnoreCase(">=")) {
			BaseType tmp = new BaseType(TypeKind.INT, expr.posn);
			if (!checkEquality(exp1, tmp))
				reporter.reportError("The type " + tmp + " and " + exp1 + " don't match");
			if (!checkEquality(exp2, tmp))
				reporter.reportError("The type " + tmp + " and " + exp2 + " don't match");
		}
		return (Type)expr.operator.visit(this, arg);
	}

	@Override
	public Type visitRefExpr(RefExpr expr, IdentificationTable arg) {
		Type refType = (Type)expr.ref.visit(this, arg);
		Declaration decl = arg.retrieveAnyScope(expr.ref.spelling);

		if(decl instanceof ClassDecl) {
			reporter.reportError("***Cannot reference ClassDecl at " + expr.posn);
		}

		else if(decl instanceof MethodDecl) {
			reporter.reportError("***Cannot reference MethodDecl at " + expr.posn);
		}

		if(!(expr.ref instanceof QualifiedRef) && decl instanceof FieldDecl) {
			FieldDecl fd = (FieldDecl) decl;
			if(presentMethodDecl.isStatic && !fd.isStatic) {
				reporter.reportError("***Cannot access non-static member from a static method at " + expr.posn);
			}
		}

		return refType;
	}

	@Override
	public Type visitCallExpr(CallExpr expr, IdentificationTable arg) {
		Type refType = (Type)expr.functionRef.visit(this, arg);
		
		MethodDecl md = (MethodDecl)expr.functionRef.decl;
		if (md.parameterDeclList.size() != expr.argList.size()) {
			reporter.reportError("***Expected " + md.parameterDeclList.size() + " parameters at " + expr.posn
					+ " but got " + expr.argList.size() + " parameters instead.");
			return refType;
		}
		
		for (int i = 0; i < md.parameterDeclList.size(); i++) {
			Expression exp = expr.argList.get(i);
			Type expType = (Type)exp.visit(this, arg);
			ParameterDecl pd = md.parameterDeclList.get(i);
			if (!checkEquality(pd.type,expType)) {
				reporter.reportError("***Parameter " + pd.name + " is of type " + pd.type + " but got " + expType 
						+ " at " + exp.posn);
			}
		}
		return refType;
	}

	@Override
	public Type visitLiteralExpr(LiteralExpr expr, IdentificationTable arg) {
		return (Type)expr.lit.visit(this, arg);
	}

	@Override
	public Type visitNewObjectExpr(NewObjectExpr expr, IdentificationTable arg) {
		return (Type)expr.classtype.visit(this, arg);
	}

	@Override
	public Type visitNewArrayExpr(NewArrayExpr expr, IdentificationTable arg) {
		Type aType = (Type)expr.eltType.visit(this, arg);
		if (aType.typeKind != TypeKind.INT && aType.typeKind != TypeKind.CLASS)
			reporter.reportError("Array cannot be of type " + aType + " at " + aType.posn);
		
		Type aType2 = (Type)expr.sizeExpr.visit(this, arg);
		if (aType2.typeKind != TypeKind.INT) {
			BaseType tmpType = new BaseType(TypeKind.INT, expr.sizeExpr.posn);
			reporter.reportError("Expected " + tmpType + " but got " + aType2 + " at " + tmpType.posn);
		}
		return new ArrayType(aType, expr.posn);
	}

	@Override
	public Type visitQualifiedRef(QualifiedRef ref, IdentificationTable arg) {
		Type aType = (Type)ref.ref.visit(this, arg);

		// Check if method
		if(ref.ref.decl instanceof MethodDecl) {
			reporter.reportError("***Cannot use MethodDecl " + ref.ref.decl.name + " in a qualified reference at " + ref.ref.posn);
			return new BaseType(TypeKind.ERROR, ref.ref.posn);
		}

		// Check if Array
		if(aType.typeKind == TypeKind.ARRAY) {
			if(!ref.id.spelling.equals("length")) {
				reporter.reportError("***Array property " + ref.id.spelling + " not valid at " + ref.id.posn);
				return new BaseType(TypeKind.ERROR, ref.id.posn);
			}
			
			return new BaseType(TypeKind.INT, ref.id.posn);
		}
		// Check if Class
		if(aType.typeKind != TypeKind.CLASS) {
			reporter.reportError("***Qualified reference must be a class or an array at " + ref.posn);
			return new BaseType(TypeKind.ERROR, ref.posn);
		}
		
		ClassType ct = (ClassType) aType;
		ClassDecl cd = (ClassDecl) aTable.retrieveInScope(ct.className.spelling);
		if(cd == null) {
			reporter.reportError("***ClassDecl is null at " + ct.posn);
			return new BaseType(TypeKind.ERROR, ref.posn);
		}

		MemberDecl md = (MemberDecl) cd.idTable.retrieveInScope(ref.id.spelling);
		if(md == null) {
			reporter.reportError("***MethodDecl is null at " + ref.id.posn);
			return new BaseType(TypeKind.ERROR, ref.posn);
		}
		
		// Check if static
		Declaration decl = arg.retrieveAnyScope(ref.ref.spelling);
		if(decl instanceof ClassDecl && !md.isStatic) {
			reporter.reportError("***MethodDecl is not static at " + ref.id.posn);
		}
		
		// Check if private
		if(md.isPrivate && cd != presentClassDecl) {
			reporter.reportError("***MethodDecl is private at " + ref.id.posn);
		}
		
		ref.decl = md;
		ref.id.decl = md;
		ref.spelling = ref.id.spelling;
		
		return md.type;
	}

	@Override
	public Type visitIndexedRef(IndexedRef ref, IdentificationTable arg) {
		Type refId = (Type)ref.ref.visit(this, arg);
		ref.spelling = ref.ref.spelling;
		// Check if it is an array
		if(refId.typeKind != TypeKind.ARRAY) {
			reporter.reportError("***Cannot use " + refId + " as an array at " + ref.ref.posn);
			return refId;
		}
		
		// Check if the index is an integer
		Type refExp = (Type)ref.indexExpr.visit(this, arg);
		if(refExp.typeKind != TypeKind.INT) {
			BaseType type = new BaseType(TypeKind.INT, ref.indexExpr.posn);
			reporter.reportError("***Expected " + type + " but got " + refExp + " at " + type.posn);
		}
		
		// Check that array type is class or int
		if (((ArrayType)refId).eltType.typeKind == TypeKind.CLASS) {
			ClassType aType = (ClassType) ((ArrayType)refId).eltType;
			ref.decl = arg.retrieveAnyScope(aType.className.spelling);
			return ref.decl.type;
		}
		else if (((ArrayType)refId).eltType.typeKind == TypeKind.INT){
			ref.decl = null;
			return new BaseType(TypeKind.INT, ref.posn);
		}
		else
			return new BaseType(TypeKind.UNSUPPORTED, ref.posn);

	}

	@Override
	public Type visitIdRef(IdRef ref, IdentificationTable arg) {
		ref.id.visit(this, arg);
		ref.decl = ref.id.decl;
		ref.spelling = ref.id.spelling;
		return ref.decl.type;
	}

	@Override
	public Type visitThisRef(ThisRef ref, IdentificationTable arg) {
		ref.spelling = "this";
		ref.decl = presentClassDecl;
		if (presentMethodDecl.isStatic){
			reporter.reportError("***Cannot use 'this' with static method at " + ref.posn);
		}
		return ref.decl.type;
	}

	@Override
	public Object visitIdentifier(Identifier id, IdentificationTable arg) {
		if(presentVarDecl != null && presentVarDecl.name.equals(id.spelling)) {
			reporter.reportError("Cannot use variable in its declaration at " + id.posn);
		}
		
		Declaration tmp = arg.retrieveAnyScope(id.spelling);
		if (tmp != null) {
//			if (tmp instanceof ClassDecl && tmp.type == null) {
//				tmp.type = new ClassType(new Identifier(new Token(TokenKind.ID, id.spelling, tmp.posn), tmp.posn), tmp.posn);
//			}
			id.decl = tmp;
		}
		else{
			reporter.reportError("***" + id.spelling + " not found at " + id.posn);
			System.exit(4);
		}
		
		if(id.decl instanceof MemberDecl) {
			MemberDecl md = (MemberDecl) id.decl;
			if(presentMethodDecl.isStatic && !md.isStatic) {
				reporter.reportError("Cannot access non-static MethodDecl " + md.name + " from a static method at " + id.posn);
			}
		}
		return id.decl.type;
	}

	@Override
	public Object visitOperator(Operator op, IdentificationTable arg) {
	    if (op.spelling.equals("!=") || op.spelling.equals("==") || op.spelling.equals(">=")
	    	|| op.spelling.equals("<=") || op.spelling.equals("<") || op.spelling.equals(">")
	    	|| op.spelling.equals("||") || op.spelling.equals("&&") || op.spelling.equals("!")){
	    	return new BaseType(TypeKind.BOOLEAN, op.posn);
	    }
	    else if (op.spelling.equals("+") || op.spelling.equals("-") || op.spelling.equals("*")
	    		|| op.spelling.equals("/")){
	    	return new BaseType(TypeKind.INT, op.posn);
	    }
	    return new BaseType(TypeKind.ERROR, op.posn);
	}

	@Override
	public Type visitIntLiteral(IntLiteral num, IdentificationTable arg) {
		return new BaseType(TypeKind.INT, num.posn);
	}

	@Override
	public Type visitBooleanLiteral(BooleanLiteral bool, IdentificationTable arg) {
		return new BaseType(TypeKind.BOOLEAN, bool.posn);
	}

	@Override
	public Object visitRef(Reference ref, IdentificationTable arg) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public boolean checkEquality(Type a, Type b) {
		if (a.typeKind != b.typeKind)
			return false;
		
		else if (a.typeKind == TypeKind.CLASS){
			ClassType ct = (ClassType)a, ct2 = (ClassType)b;
			if (ct.className.spelling.equals(ct2.className.spelling))
				return true;
			else
				return false;
		}
		
		else if (a.typeKind == TypeKind.ARRAY){
			ArrayType at = (ArrayType)a, at2 = (ArrayType)b;
			return checkEquality(at.eltType, at2.eltType);
		}
		
		return true;
	}
	public boolean isMainMethod(MethodDecl newFd) {
		if (!newFd.isPrivate && newFd.isStatic && newFd.type.typeKind == TypeKind.VOID
				&& newFd.name.equals("main")) {
			if (newFd.parameterDeclList.size() == 1) {
				ParameterDecl tmp = newFd.parameterDeclList.get(0);
				if (tmp.type instanceof ArrayType && tmp.name.equals("args")) {
					ArrayType tmpArType = (ArrayType)tmp.type;
					if (tmpArType.eltType instanceof ClassType) {
						ClassType tmpClType = (ClassType)tmpArType.eltType;
						if (tmpClType.className.spelling.equals("String"))
							return true;
					}
				}
			}
		}
		return false;
	}
	
	public void handleClassDec(ClassDecl cd, IdentificationTable arg) {	
		arg.enter(cd.name, cd);
		cd.idTable = new IdentificationTable();
		cd.idTable.setPrevScope(arg);
		for(FieldDecl fDecl : cd.fieldDeclList) {
			cd.idTable.enter(fDecl.name, fDecl);
		}
		
		for (MethodDecl mDecl : cd.methodDeclList) {
			cd.idTable.enter(mDecl.name, mDecl);
			mDecl.idTable = new IdentificationTable();
			mDecl.idTable.setPrevScope(cd.idTable);
		}
	}
}
