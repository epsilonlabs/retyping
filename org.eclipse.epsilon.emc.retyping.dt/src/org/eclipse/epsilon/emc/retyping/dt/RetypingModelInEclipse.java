package org.eclipse.epsilon.emc.retyping.dt;

import java.io.File;

import org.eclipse.epsilon.common.dt.launching.extensions.ModelTypeExtension;
import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.emc.plainxml.PlainXmlModel;
import org.eclipse.epsilon.emc.retyping.RetypingModel;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.models.IModel;
import org.eclipse.epsilon.eol.models.IRelativePathResolver;
import org.eclipse.epsilon.epl.EplModule;
import org.eclipse.epsilon.etl.EtlModule;
import org.w3c.dom.Element;

public class RetypingModelInEclipse extends RetypingModel {
	
	@Override
	public void load(StringProperties properties, IRelativePathResolver resolver) throws EolModelLoadingException {
		super.load(properties, resolver);
	
		PlainXmlModel helper = new PlainXmlModel();
		File file = new File(resolver.resolve(properties.getProperty(RetypingModel.PROPERTY_FILE)));
		helper.setFile(file);
		helper.load();
		
		try {
			// TODO: EPL is optional
			patternMatchingModule = new EplModule();
			patternMatchingModule.parse(new File(resolver.resolve(helper.getAllOfType("t_epl").iterator().next().getAttribute("file"))));
			retypingModule = new EtlModule();
			retypingModule.parse(new File(resolver.resolve(helper.getAllOfType("t_etl").iterator().next().getAttribute("file"))));
			
			for (Element modelElement : helper.getAllOfKind("t_model")) {
				StringProperties modelProperties = new StringProperties(modelElement.getAttribute("properties"));
				IModel model = ModelTypeExtension.forType(modelProperties.getProperty("type")).createModel();
				model.load(modelProperties, resolver);
				retypedModels.add(model);
			}
			
			loadModel();
			
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
}
