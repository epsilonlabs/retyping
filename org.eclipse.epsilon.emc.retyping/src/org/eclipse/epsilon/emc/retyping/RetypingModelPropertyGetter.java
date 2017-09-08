package org.eclipse.epsilon.emc.retyping;

import org.eclipse.epsilon.eol.exceptions.EolIllegalPropertyException;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.execute.introspection.AbstractPropertyGetter;

public class RetypingModelPropertyGetter extends AbstractPropertyGetter {

	@Override
	public Object invoke(Object object, String property) throws EolRuntimeException {
		if (object instanceof RetypedElement) {
			return ((RetypedElement) object).getProperty(property);
		}
		throw new EolIllegalPropertyException(object, property, ast, context);
	}

}
