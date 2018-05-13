package ch.uzh.ifi.seal.ase.cscc.visitors;

import cc.kave.commons.model.naming.codeelements.IParameterName;
import cc.kave.commons.model.naming.types.ITypeParameterName;
import cc.kave.commons.model.ssts.declarations.IDelegateDeclaration;
import cc.kave.commons.model.ssts.declarations.IMethodDeclaration;
import cc.kave.commons.model.ssts.expressions.assignable.IInvocationExpression;
import cc.kave.commons.model.ssts.expressions.assignable.ILambdaExpression;
import cc.kave.commons.model.ssts.impl.visitor.AbstractTraversingNodeVisitor;
import cc.kave.commons.model.ssts.references.IVariableReference;
import cc.kave.commons.model.ssts.statements.IVariableDeclaration;

import java.util.Set;

public class ContextVisitor extends AbstractTraversingNodeVisitor<Set<String>, Void> {
    @Override
    public Void visit(IVariableDeclaration stmt, Set<String> overallContext) {
        // Add the identifier of the variable to the overall context
        overallContext.add(stmt.getType().getName());
        return null;
    }

    @Override
    public Void visit(IMethodDeclaration decl, Set<String> overallContext) {
        // Add the return type to the overall context
        overallContext.add(decl.getName().getReturnType().getName());
        // Add the method name itself to the overall context
        overallContext.add(decl.getName().getName());
        // Add the types of the parameters to the overall context
        for(IParameterName iParameterName : decl.getName().getParameters()) {
            overallContext.add(iParameterName.getValueType().getName());
        }
        return null;
    }

    @Override
    public Void visit(IInvocationExpression expr, Set<String> overallContext) {
        // Add the name of invoked the method to the overall context
        overallContext.add(expr.getMethodName().getName());
        return null;
    }
}