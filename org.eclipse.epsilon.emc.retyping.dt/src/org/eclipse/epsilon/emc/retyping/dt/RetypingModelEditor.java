package org.eclipse.epsilon.emc.retyping.dt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.epsilon.common.dt.launching.dialogs.BrowseWorkspaceUtil;
import org.eclipse.epsilon.common.dt.util.LogUtil;
import org.eclipse.epsilon.emc.plainxml.PlainXmlModel;
import org.eclipse.epsilon.eol.exceptions.models.EolModelElementTypeNotFoundException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.w3c.dom.Element;

public class RetypingModelEditor extends EditorPart {

	protected FormToolkit toolkit;
	protected ScrolledForm form;
	protected Text eplSourceText;
	protected Text etlSourceText;
	protected ModelsConfigurationComposite modelsConfigurationComposite;
	protected PlainXmlModel model = null;
	protected String savedState = "";
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		model.store();
		savedState = model.getXml();
		firePropertyChange(PROP_DIRTY);
	}

	@Override
	public void doSaveAs() { /* TODO: Later */}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInputWithNotify(input);
		setPartName(input.getName());
		model = new PlainXmlModel();
		model.setCachingEnabled(false);
		model.setFile(((FileEditorInput) input).getFile().getLocation().toFile());
		try {
			model.load();
		} catch (EolModelLoadingException e) {
			throw new PartInitException(e.getMessage(), e);
		}
		savedState = model.getXml();
	}

	@Override
	public boolean isDirty() {
		return !model.getXml().equals(savedState);
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {

		toolkit = new FormToolkit(parent.getDisplay());
		form = toolkit.createScrolledForm(parent);
		form.setText("Retyping Model Configuration");
		form.getBody().setLayout(new GridLayout());
		
		// Retyped Models section
		
		Section section = toolkit.createSection(form.getBody(), 
				  Section.DESCRIPTION|Section.TITLE_BAR|Section.EXPANDED|Section.TWISTIE);
		section.setText("Retyped Models");
		section.setDescription("The models to be retyped. An additional model named \"P\" holding the results of pattern matching will also be implicitly available in the ETL transformation.");
		section.setLayoutData(new GridData(GridData.FILL_BOTH));
		Composite sectionClient = toolkit.createComposite(section);
		section.setClient(sectionClient);
		sectionClient.setLayout(new GridLayout());
		
		modelsConfigurationComposite = new ModelsConfigurationComposite(sectionClient, SWT.FILL) {
			@Override
			public void modelsChanged() {
				super.modelsChanged();
				try {
					for (Element modelElement : new ArrayList<Element>(model.getAllOfType("t_model"))) {
						model.deleteElement(modelElement);
					}
					
					for (String modelProperties : modelsConfigurationComposite.getModels()) {
						Element modelElement = model.createInstance("t_model");
						model.getRoot().appendChild(modelElement);
						modelElement.setAttribute("properties", modelProperties);
					}
				}
				catch (Exception ex) {}
				firePropertyChange(PROP_DIRTY);
			}
		};
		modelsConfigurationComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		// Patterns and Rules section
		
		section = toolkit.createSection(form.getBody(), 
				  Section.DESCRIPTION|Section.TITLE_BAR|Section.EXPANDED|Section.TWISTIE);
		section.setText("Patterns and Rules");
		section.setDescription("The EPL patterns and ETL transformation rules that will be used to retype the model(s) above.");
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		sectionClient = toolkit.createComposite(section);
		section.setClient(sectionClient);
		
		sectionClient.setLayout(new GridLayout(3, false));
		
		eplSourceText = createSourceText("Retyping patterns:", sectionClient);
		etlSourceText = createSourceText("Retyping rules:", sectionClient);
		
		try {
			eplSourceText.setText(model.getAllOfType("t_epl").iterator().next().getAttribute("file"));
			etlSourceText.setText(model.getAllOfType("t_etl").iterator().next().getAttribute("file"));
			List<String> retypedModels = new ArrayList<String>();
			for (Element modelElement : model.getAllOfType("t_model")) {
				retypedModels.add(modelElement.getAttribute("properties"));
			}
			modelsConfigurationComposite.setModels(retypedModels);
		} catch (EolModelElementTypeNotFoundException e) {
			LogUtil.log(e);
		}
		
		eplSourceText.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				try {
					model.getAllOfType("t_epl").iterator().next().setAttribute("file", eplSourceText.getText());
					firePropertyChange(PROP_DIRTY);
				} catch (Exception ex) {}
			}
		});
		
		etlSourceText.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				try {
					model.getAllOfType("t_etl").iterator().next().setAttribute("file", etlSourceText.getText());
					firePropertyChange(PROP_DIRTY);
				} catch (Exception ex) {}
			}
		});
	}
	
	protected Text createSourceText(String title, Composite parent) {
		
		Label label = new Label(parent, SWT.NULL);
		label.setText(title);
		final Text text = new Text(parent, SWT.BORDER);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Button button = new Button(parent, SWT.NULL);
		button.setText("Browse...");
		
		button.addListener(SWT.Selection, new Listener() {
			
			@Override
			public void handleEvent(Event event) {
				String selected = 
					BrowseWorkspaceUtil.browseFilePath(RetypingModelEditor.this.getSite().getShell(), "Stuff"
					, "More stuff", null);
				
				if (selected!=null) text.setText(selected);
			}
		});
		
		text.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				firePropertyChange(PROP_DIRTY);
			}
		});
		
		return text;
	}

	@Override
	public void setFocus() {
		form.setFocus();
	}

	@Override
	public void dispose() {
		super.dispose();
		form.dispose();
		toolkit.dispose();
	}

}
