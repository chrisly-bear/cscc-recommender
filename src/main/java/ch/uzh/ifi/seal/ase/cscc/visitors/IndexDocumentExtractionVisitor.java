package ch.uzh.ifi.seal.ase.cscc.visitors;

import cc.kave.commons.model.ssts.IStatement;
import cc.kave.commons.model.ssts.expressions.IAssignableExpression;
import cc.kave.commons.model.ssts.expressions.assignable.IInvocationExpression;
import cc.kave.commons.model.ssts.impl.visitor.AbstractTraversingNodeVisitor;
import cc.kave.commons.model.ssts.statements.IExpressionStatement;
import ch.uzh.ifi.seal.ase.cscc.index.IndexDocument;

import java.util.*;


/**
 * Visitor that takes a body of statements into the visit method and returns a list of IndexDocuments
 */
public class IndexDocumentExtractionVisitor extends AbstractTraversingNodeVisitor<Void, IndexDocument> {

    private final int LAST_N_CONSIDERED_STATEMENTS = 6;

    @Override
    public IndexDocument visit(IExpressionStatement stmt, Void aVoid) {
        return super.visit(stmt, aVoid);
    }

    @Override
    protected List<IndexDocument> visit(List<IStatement> body, Void aVoid) {
        LinkedList<IStatement> statements = new LinkedList<>();
        statements.addAll(body);
        for (IStatement statement : body) {
            if (statement instanceof IExpressionStatement) {
                IAssignableExpression expression = ((IExpressionStatement) statement).getExpression();
                if (expression instanceof IInvocationExpression) {
                    // We have dedected a method invocation

                    // We can retrieve the simple name (not the full name) of the invocated method like this:
                    String methodName = ((IInvocationExpression) expression).getMethodName().getName();

                    // We can also retrieve the name of the type on which the method was invocated like this:
                    String type = ((IInvocationExpression) expression).getReference().getIdentifier();

                    // We don't care for parameters, so don't get them

                    // Now we can assemble the method call like it was in the source code
                    String rawMethodCall = type + "." + methodName + "()";

                    System.out.println(rawMethodCall);

                    // Now we get the last n statements before our method invocation
                    List<IStatement> lastNStatements = getLastNStatementsBeforeStatement(statements, statements.indexOf(statement), LAST_N_CONSIDERED_STATEMENTS);

                    // TODO Get method names, that includes names of declared methods as well as names of invocated methods
                    // TODO Get Java keywords
                    // TODO Get Class and Interface names
                    // TODO Find a way to get the line context


                    // TODO Take the list
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     *
     * @param statements A list of statements
     * @param indexOfStatement The index of the statement from where the method should go backwards
     * @param lastNConsideredStatements The number of statements the method should go backwards
     * @return  A list of statements that occured before the statement given by indexOfStatement or.
     *          If the list doesn't contain enough elements, i.e (indexOfStatement - lastNConsideredStatements < 0),
     *          then the return value is just a list statements up to the start of the list.
     */

    private List<IStatement> getLastNStatementsBeforeStatement(List<IStatement> statements, int indexOfStatement, int lastNConsideredStatements) {

        if (indexOfStatement - lastNConsideredStatements < 0) {
            lastNConsideredStatements = indexOfStatement;
        }

        int startIndex = indexOfStatement - lastNConsideredStatements;

        IStatement[] smts = new IStatement[statements.size()];
        statements.toArray(smts);

        IStatement[] res = new IStatement[lastNConsideredStatements];
        System.arraycopy(smts, startIndex, res, 0, lastNConsideredStatements);
        return Arrays.asList(res);
    }
}
