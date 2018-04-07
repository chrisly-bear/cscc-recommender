package ch.uzh.ifi.seal.ase.cscc.visitors;

import cc.kave.commons.model.ssts.IStatement;
import cc.kave.commons.model.ssts.declarations.IMethodDeclaration;
import cc.kave.commons.model.ssts.expressions.assignable.IInvocationExpression;
import cc.kave.commons.model.ssts.impl.visitor.AbstractTraversingNodeVisitor;

import java.util.List;

public class MethodInvocationVisitor extends AbstractTraversingNodeVisitor<List<Integer>, Void> {

    /**
     * Visit all method declarations and count how many method invocations they contain
     */
    @Override
    public Void visit(IMethodDeclaration decl, List<Integer> methodInvocationCounts) {
//        System.out.println("### Method Decl: " + decl.getName());
        List<IStatement> methodBody = decl.getBody();
        MutableInt methodInvocationCount = new MutableInt(0);
        for (IStatement stmt : methodBody) {
//            System.out.println(stmt.getClass().getSimpleName());
            stmt.accept(new StatementMethodInvocationVisitor(), methodInvocationCount);
        }
        methodInvocationCounts.add(methodInvocationCount.getValue());
        return null;
    }

    private class StatementMethodInvocationVisitor extends AbstractTraversingNodeVisitor<MutableInt, Void> {
        /**
         * Count all method invocations
         */
        @Override
        public Void visit(IInvocationExpression methodInvocation, MutableInt methodInvocationCount) {
            methodInvocationCount.incrementBy(1);
//            System.out.println("@@@ Method Invocation (" + methodInvocationCount.getValue() + ") : " + methodInvocation.getMethodName());
            return null;
        }
    }

    /**
     * Mutable integer class which can be passed by reference
     */
    private class MutableInt {

        private int value;

        public MutableInt(int i) {
            this.value = i;
        }

        public void incrementBy(int i) {
            this.value += i;
        }

        public int getValue() {
            return this.value;
        }
    }

}
