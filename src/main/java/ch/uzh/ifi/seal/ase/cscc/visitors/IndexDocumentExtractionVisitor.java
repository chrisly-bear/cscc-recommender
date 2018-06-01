package ch.uzh.ifi.seal.ase.cscc.visitors;

import cc.kave.commons.model.naming.codeelements.IMethodName;
import cc.kave.commons.model.ssts.IStatement;
import cc.kave.commons.model.ssts.expressions.IAssignableExpression;
import cc.kave.commons.model.ssts.expressions.assignable.IInvocationExpression;
import cc.kave.commons.model.ssts.impl.visitor.AbstractTraversingNodeVisitor;
import cc.kave.commons.model.ssts.statements.IAssignment;
import cc.kave.commons.model.ssts.statements.IExpressionStatement;
import ch.uzh.ifi.seal.ase.cscc.index.IndexDocument;
import ch.uzh.ifi.seal.ase.cscc.utils.CSCCConfiguration;

import java.util.*;


/**
 * Visitor that takes a body of statements into the visit method and returns a list of IndexDocuments
 */
public class IndexDocumentExtractionVisitor extends AbstractTraversingNodeVisitor<List<IndexDocument>, Void> {

    private final ContextVisitor CONTEXT_VISITOR = new ContextVisitor();

    @Override
    protected List<Void> visit(List<IStatement> body, List<IndexDocument> indexDocuments) {
        for (IStatement statement : body) {
            if (statement instanceof IExpressionStatement || statement instanceof IAssignment) {
                IAssignableExpression expression;
                if (statement instanceof IExpressionStatement) {
                    expression = ((IExpressionStatement) statement).getExpression();
                } else { // (statement instanceof IAssignment)
                    expression = ((IAssignment) statement).getExpression();
                }
                if (expression instanceof IInvocationExpression) {

                    final IMethodName methodName = ((IInvocationExpression) expression).getMethodName();
                    final String methodNameStr = methodName.getName();
                    String type = methodName.getDeclaringType().getFullName();
                    type = normalizeType(type);

                    // we don't want to index method names we don't know, nor constructor methods
                    if (!methodNameStr.equals("???") || !methodNameStr.equals("???")) {
                        if (!methodName.isConstructor()) {

                            // create line and overall context
                            List<IStatement> lastNStatements = getLastNStatementsBeforeStatement(body, body.indexOf(statement), CSCCConfiguration.LAST_N_CONSIDERED_STATEMENTS);
                            Set<String> overallContextSet = new HashSet<>();
                            Set<String> lineContextSet = new HashSet<>();
                            lastNStatements.forEach(iStatement -> iStatement.accept(CONTEXT_VISITOR, overallContextSet));
                            statement.accept(CONTEXT_VISITOR, lineContextSet);

                            // TODO: why do we have to remove it if line context is the same as method call?
                            if (lineContextSet.contains(methodNameStr)) {
                                //System.out.println("Line context would be the same as the name of the method, make line context empty");
                                lineContextSet.remove(methodNameStr);
                            }

                            // create a new IndexDocument
                            List<String> lineContext = new LinkedList<>();
                            lineContext.addAll(lineContextSet);
                            List<String> overallContext = new LinkedList<>();
                            overallContext.addAll(overallContextSet);
                            IndexDocument indexDocument = new IndexDocument(methodNameStr, type, lineContext, overallContext);
                            indexDocuments.add(indexDocument);
                        }
                    }
                }
            }
        }
        // call to parent, important so that bodies within body are also traversed
        // without this, not all InvocationExpressions are traversed
        return super.visit(body, indexDocuments);
    }

    /**
     * Removes the generic part of generic parts, which contains a lot of special symbols.
     * E.g. "System.Collections.Generic.List`1[[T -> MailChimp.Lists.Grouping, MailChimp]]"
     * becomes "System.Collections.Generic.List".
     * This has the side effect that we index types which differ in their generics as the same type.
     *
     * @param type type string to normalize
     * @return normalized type string
     */
    private String normalizeType(String type) {
        return type.split("`")[0];
    }

    /**
     * @param statements                A list of statements
     * @param indexOfStatement          The index of the statement from where the method should go backwards
     * @param lastNConsideredStatements The number of statements the method should go backwards
     * @return A list of statements that occured before the statement given by indexOfStatement or.
     * If the list doesn't contain enough elements, i.e (indexOfStatement - lastNConsideredStatements < 0),
     * then the return value is just a list statements up to the start of the list.
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
