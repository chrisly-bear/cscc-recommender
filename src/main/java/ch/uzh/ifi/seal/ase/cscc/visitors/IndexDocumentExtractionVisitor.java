package ch.uzh.ifi.seal.ase.cscc.visitors;

import cc.kave.commons.model.ssts.IStatement;
import cc.kave.commons.model.ssts.expressions.IAssignableExpression;
import cc.kave.commons.model.ssts.expressions.assignable.IInvocationExpression;
import cc.kave.commons.model.ssts.impl.statements.VariableDeclaration;
import cc.kave.commons.model.ssts.impl.visitor.AbstractTraversingNodeVisitor;
import cc.kave.commons.model.ssts.statements.IExpressionStatement;
import ch.uzh.ifi.seal.ase.cscc.index.IndexDocument;

import java.util.*;


/**
 * Visitor that takes a body of statements into the visit method and returns a list of IndexDocuments
 */
public class IndexDocumentExtractionVisitor extends AbstractTraversingNodeVisitor<Void, IndexDocument> {

    private final int LAST_N_CONSIDERED_STATEMENTS = 6;

    private final ContextVisitor contextVisitor = new ContextVisitor();

    @Override
    protected List<IndexDocument> visit(List<IStatement> body, Void aVoid) {

        List<IndexDocument> indexDocuments = new ArrayList<>();

        for (IStatement statement : body) {
            if (statement instanceof IExpressionStatement) {
                IAssignableExpression expression = ((IExpressionStatement) statement).getExpression();
                if (expression instanceof IInvocationExpression) {
                    // We have detected a method invocation

                    // We can retrieve the simple name (not the full name) of the invocated method like this:
                    String methodCall = ((IInvocationExpression) expression).getMethodName().getName();

                    // We can also retrieve the name of the type on which the method was invocated like this:
                    String type = findTypeOfVariableOnWhichMethodWasInvocated(body, ((IInvocationExpression) expression).getReference().getIdentifier());

                    // Now we get the last n statements before our method invocation
                    List<IStatement> lastNStatements = getLastNStatementsBeforeStatement(body, body.indexOf(statement), LAST_N_CONSIDERED_STATEMENTS);

                    Set<String> overallContext = new HashSet<>();
                    Set<String> lineContext = new HashSet<>();

                    lastNStatements.forEach(iStatement -> iStatement.accept(contextVisitor, overallContext));

                    statement.accept(contextVisitor, lineContext);

                    if (lineContext.contains(methodCall)) {
                        System.out.println("Line context would be the same as the name of the method, make line context empty");
                        lineContext.remove(methodCall);
                    }

                    List<String> overallContextList = new ArrayList<>();
                    overallContextList.addAll(overallContext);

                    List<String> lineContextList = new ArrayList<>();
                    lineContextList.addAll(lineContext);

                    IndexDocument indexDocument = new IndexDocument(methodCall, type, lineContextList, overallContextList);

                    System.out.println(indexDocument.toString());

                    // TODO Get Java keywords
                }
            }
        }
        return Collections.emptyList();
    }

    private String findTypeOfVariableOnWhichMethodWasInvocated(List<IStatement> statements, String identifier) {
        String result = "";
        for (IStatement iStatement : statements) {
            if (iStatement instanceof VariableDeclaration) {
                String identifierOfThisDeclaration = ((VariableDeclaration) iStatement).getReference().getIdentifier();
                if (identifier.equals(identifierOfThisDeclaration)) {
                    String type = ((VariableDeclaration) iStatement).getType().getName();
                    result = type;
                }
            }
        }
        return result;
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
