/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public abstract class Reference extends AST
{
	public Reference(SourcePosition posn){
		super(posn);
	}

	public <A, R> R visit(Visitor<A, R> v, A o) {
		return null;
	}
	
	public Declaration decl;
	public String spelling;
}
