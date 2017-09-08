package org.eclipse.epsilon.emc.retyping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.eclipse.epsilon.eol.dom.AssignmentStatement;
import org.eclipse.epsilon.eol.dom.PropertyCallExpression;
import org.eclipse.epsilon.eol.dom.SpecialAssignmentStatement;
import org.eclipse.epsilon.eol.dom.Statement;
import org.eclipse.epsilon.eol.dom.StatementBlock;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.execute.context.FrameType;
import org.eclipse.epsilon.eol.execute.context.Variable;
import org.eclipse.epsilon.etl.dom.TransformationRule;
import org.eclipse.epsilon.etl.execute.context.IEtlContext;

public class RetypedElement {
	
	protected RetypingModel model;
	protected Object element;
	protected TransformationRule rule;
	protected HashMap<String, Object> properties = new HashMap<String, Object>();
	
	public RetypedElement(RetypingModel model, Object element, TransformationRule rule) {
		this.model = model;
		this.element = element;
		this.rule = rule;
	}
	
	public Object getProperty(String property) throws EolRuntimeException {
		if (!properties.containsKey(property)) {
			
			StatementBlock statementBlock = (StatementBlock) rule.getBody().getBody();
			for (Statement statement : statementBlock.getStatements()) {
				AssignmentStatement assignmentStatement = (AssignmentStatement) statement;
				String propertyName = ((PropertyCallExpression) assignmentStatement.getTargetExpression()).getPropertyNameExpression().getName();
				if (propertyName.equals(property)) {
					IEtlContext context = model.getRetypingModule().getContext();
					context.getFrameStack().enterLocal(FrameType.PROTECTED, rule, Variable.createReadOnlyVariable(rule.getSourceParameter().getName(), element));
					Object value = context.getExecutorFactory().execute(assignmentStatement.getValueExpression(), context);
					context.getFrameStack().leaveLocal(rule);
					
					if (assignmentStatement instanceof SpecialAssignmentStatement) {
						
						List<RetypedElement> retypedValues = new ArrayList<RetypedElement>();
						
						for (Object element : (Collection<?>) value) {
							// TODO: Check trace first
							for (TransformationRule rule : model.getRetypingModule().getTransformationRules()) {
								if (rule.appliesTo(element, context, false)) {
									RetypedElement retypedElement = new RetypedElement(getModel(), element, rule);
									context.getTransformationTrace().add(element, Arrays.asList(retypedElement), rule);
									retypedValues.add(retypedElement);
								}
							}
						}
						
						properties.put(property, retypedValues);
					}
					else {
						properties.put(property, value);
					}
					
				}
			}
			
		}
		return properties.get(property);
	}
	
	public RetypingModel getModel() {
		return model;
	}
	
	public Object getElement() {
		return element;
	}
	
}
