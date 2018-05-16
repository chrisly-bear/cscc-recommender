package ch.uzh.ifi.seal.ase.cscc.visitors;

import cc.kave.commons.model.naming.codeelements.IParameterName;
import cc.kave.commons.model.ssts.blocks.ICatchBlock;
import cc.kave.commons.model.ssts.blocks.IForEachLoop;
import cc.kave.commons.model.ssts.blocks.ITryBlock;
import cc.kave.commons.model.ssts.declarations.IMethodDeclaration;
import cc.kave.commons.model.ssts.expressions.assignable.ICastExpression;
import cc.kave.commons.model.ssts.impl.visitor.AbstractTraversingNodeVisitor;
import cc.kave.commons.model.ssts.statements.IVariableDeclaration;

public class VariableTypeFindingVisitor extends AbstractTraversingNodeVisitor<String, Void> {

    // The identifier of the variable to which we are trying to find the matching type
    private final String identifierOfVariable;

    public VariableTypeFindingVisitor(String identifierOfVariable) {
        this.identifierOfVariable = identifierOfVariable;
    }

    private boolean matchesIdentifier(String identifierOfVariable) {
        return this.identifierOfVariable.equals(identifierOfVariable);
    }

    @Override
    public Void visit(IVariableDeclaration stmt, String typeName) {
        if (matchesIdentifier(stmt.getReference().getIdentifier())) {
            typeName = stmt.getType().getName();
        }
        return null;
    }

    @Override
    public Void visit(IMethodDeclaration decl, String typeName) {
        for (IParameterName iParameterName : decl.getName().getParameters()) {
            if (matchesIdentifier(iParameterName.getIdentifier())) {
                typeName = iParameterName.getValueType().getName();
                break;
            }
        }
        return null;
    }

    @Override
    public Void visit(ICastExpression expr, String typeName) {
        if (matchesIdentifier(expr.getReference().getIdentifier())) {
            typeName = expr.getTargetType().getName();
        }
        return null;
    }

    @Override
    public Void visit(ITryBlock block, String typeName) {
        for (ICatchBlock catchBlock : block.getCatchBlocks()) {
            if (matchesIdentifier(catchBlock.getParameter().getIdentifier())) {
                typeName = catchBlock.getParameter().getValueType().getName();
            }
        }
        return null;
    }

    @Override
    public Void visit(IForEachLoop block, String typeName) {
        if (matchesIdentifier(block.getDeclaration().getReference().getIdentifier())) {
            typeName = block.getDeclaration().getType().getName();
        }
        return null;
    }
}
