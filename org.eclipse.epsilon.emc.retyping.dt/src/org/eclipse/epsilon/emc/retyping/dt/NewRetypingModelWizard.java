package org.eclipse.epsilon.emc.retyping.dt;

import org.eclipse.epsilon.common.dt.util.LogUtil;
import org.eclipse.epsilon.common.dt.wizards.AbstractNewFileWizard;
import org.eclipse.epsilon.emc.plainxml.PlainXmlModel;
import org.w3c.dom.Element;

public class NewRetypingModelWizard extends AbstractNewFileWizard {

	@Override
	public String getTitle() {
		return "New Retyping Model";
	}

	@Override
	public String getExtension() {
		return "rtm";
	}

	@Override
	public String getDescription() {
		return "This wizard creates a new retyping model file with *.rtm extension";
	}
	
	@Override
	protected String determineInitialContents() {
		try {
			PlainXmlModel model = new PlainXmlModel();
			model.setReadOnLoad(false);
			model.load();
			Element root = model.createInstance("t_retyping");
			model.setRoot(root);
			root.appendChild(model.createInstance("t_epl"));
			root.appendChild(model.createInstance("t_etl"));
			return model.getXml();
		}
		catch (Exception ex) {
			LogUtil.log(ex);
			return null;
		}
	}
}
