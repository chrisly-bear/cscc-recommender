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
public class IndexDocumentExtractionVisitor extends AbstractTraversingNodeVisitor<List<IndexDocument>, Void> {

    // Constant determining how many statements backwards should be included in the overall context
    private final int LAST_N_CONSIDERED_STATEMENTS = 6;

    private final ContextVisitor contextVisitor = new ContextVisitor();

    @Override
    public List<Void> visit(List<IStatement> body, List<IndexDocument> indexDocuments) {

        for (IStatement statement : body) {
            if (statement instanceof IExpressionStatement) {
                IAssignableExpression expression = ((IExpressionStatement) statement).getExpression();
                if (expression instanceof IInvocationExpression) {
                    // We have detected a method invocation

                    // Retrieve the simple name (not the full name) of the invoked method
                    String methodCall = ((IInvocationExpression) expression).getMethodName().getName();

                    // Retrieve the name of the type on which the method was invoked:
                    String type = findTypeOfVariable(body, ((IInvocationExpression) expression).getReference().getIdentifier());

                    // Get the last n statements before our method invocation
                    List<IStatement> lastNStatements = getLastNStatementsBeforeStatement(body, body.indexOf(statement), LAST_N_CONSIDERED_STATEMENTS);

                    Set<String> overallContext = new HashSet<>();
                    Set<String> lineContext = new HashSet<>();

                    // Visit every statement seperately and extract the information needed for the overall context
                    lastNStatements.forEach(iStatement -> iStatement.accept(contextVisitor, overallContext));

                    // Take this statement and visit it to extract the information needed for the line context
                    statement.accept(contextVisitor, lineContext);

                    // If the line context only contains the method invocation we detected, the line context is empty
                    if (lineContext.contains(methodCall)) {
                        lineContext.remove(methodCall);
                    }

                    List<String> overallContextList = new ArrayList<>();
                    overallContextList.addAll(overallContext);

                    List<String> lineContextList = new ArrayList<>();
                    lineContextList.addAll(lineContext);

                    IndexDocument indexDocument = new IndexDocument(methodCall, type, lineContextList, overallContextList);

                    indexDocuments.add(indexDocument);

                    // TODO Get Java keywords
                }
            }
        }
        return null;
    }

    /**
     * Takes a list of statements and a variable identifier and returns the type of the variable
     * @param statements The list of statements that should be searched for the matching variable type
     * @param identifier The identifier (i.e. name) of the variable for which the type should be found
     * @return The full name of the type of the variable if found, empty string if not found
     */
    private String findTypeOfVariable(List<IStatement> statements, String identifier) {
        String result = "";
        for (IStatement iStatement : statements) {
            if (iStatement instanceof VariableDeclaration) {
                String identifierOfThisDeclaration = ((VariableDeclaration) iStatement).getReference().getIdentifier();
                if (identifier.equals(identifierOfThisDeclaration)) {
                    String type = ((VariableDeclaration) iStatement).getType().getFullName();
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
