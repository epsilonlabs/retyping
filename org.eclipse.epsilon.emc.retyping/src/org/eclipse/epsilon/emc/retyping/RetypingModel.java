package org.eclipse.epsilon.emc.retyping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.epsilon.emc.plainxml.PlainXmlModel;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.exceptions.models.EolEnumerationValueNotFoundException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelElementTypeNotFoundException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.exceptions.models.EolNotInstantiableModelElementTypeException;
import org.eclipse.epsilon.eol.execute.introspection.IPropertyGetter;
import org.eclipse.epsilon.eol.models.CachedModel;
import org.eclipse.epsilon.eol.models.IModel;
import org.eclipse.epsilon.eol.types.EolModelElementType;
import org.eclipse.epsilon.epl.EplModule;
import org.eclipse.epsilon.epl.execute.PatternMatchModel;
import org.eclipse.epsilon.etl.EtlModule;
import org.eclipse.epsilon.etl.dom.TransformationRule;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class RetypingModel extends CachedModel<Object> {
	
	public static String PROPERTY_FILE = "file";
	protected List<IModel> retypedModels = new ArrayList<IModel>();
	
	public static void main(String[] args) throws Exception {
		
		PlainXmlModel xml = new PlainXmlModel();
		xml.setName("In");
		xml.setXml("<?xml version=\"1.0\"?><model>" + 
				"<person name=\"Tom\" gender=\"male\"><pet name=\"Jack\"/></person>" + 
				"<person name=\"Jayne\" gender=\"female\"><pet name=\"Clara\"/><pet name=\"Linda\"/></person>" + 
				"</model>");
		xml.load();
		
		EplModule epl = new EplModule();
		epl.parse(
				"pattern PetAndOwner p : t_person, pet : t_pet from : p.c_pet");
		
		EtlModule etl = new EtlModule();
		etl.parse(
				"rule Person2Male transform s : In!t_person to t : Out!Male { guard: s.a_gender = 'male' t.name = s.a_name; t.pets ::= s.c_pet; }" + 
				"rule Person2Female transform s : In!t_person to t : Out!Female { guard: s.a_gender = 'female' t.name = s.a_name; t.pets ::= s.c_pet; }" + 
				"rule Pet2Pet transform s : In!t_pet to t : Out!Pet { t.name = s.a_name; }" + 
				"rule PetAndOwner transform s : P!PetAndOwner to t : Out!PetAndOwner { t.ownerName = s.p.a_name; t.petName = s.pet.a_name; }");
		
		RetypingModel retyped = new RetypingModel();
		retyped.setName("M");
		retyped.setRetypingModule(etl);
		retyped.setPatternMatchingModule(epl);
		retyped.getRetypedModels().add(xml);
		retyped.load();
		
		EolModule eol = new EolModule();
		eol.parse("PetAndOwner.all.ownerName.println();");
		eol.getContext().getModelRepository().addModel(retyped);
		eol.execute();
	}
	
	protected EplModule patternMatchingModule = null;
	protected EtlModule retypingModule = null;
	protected BiMap<Object, Object> trace = HashBiMap.create();
	
	@Override
	public boolean owns(Object instance) {
		return instance instanceof RetypedElement && ((RetypedElement) instance).getModel() == this;
	}
	
	protected List<TransformationRule> getRulesForType(String type) {
		
		List<TransformationRule> transformationRules = new ArrayList<TransformationRule>();
		
		for (TransformationRule rule : retypingModule.getTransformationRules()) {
			try {
				EolModelElementType targetType = (EolModelElementType) rule.getTargetParameters().get(0).getType(retypingModule.getContext());
				if (targetType.getTypeName().equals(type)) {
					transformationRules.add(rule);
				}
			} catch (EolRuntimeException e) {
				e.printStackTrace();
			}
		}
		
		System.out.println(type + " " + transformationRules.size());
		
		return transformationRules;
	}
	
	@Override
	public boolean hasType(String type) {
		return !getRulesForType(type).isEmpty();
	}
	
	public List<IModel> getRetypedModels() {
		return retypedModels;
	}
	
	@Override
	protected Collection<Object> getAllOfTypeFromModel(String type) throws EolModelElementTypeNotFoundException {
		try {
			List<Object> allOfTypeFromModel = new ArrayList<Object>();
			
			for (TransformationRule rule : getRulesForType(type)) {
				EolModelElementType sourceType = ((EolModelElementType) rule.getSourceParameter().getType(retypingModule.getContext()));
				for (Object element : retypingModule.getContext().getModelRepository().getModelByName(sourceType.getModelName()).getAllOfType(sourceType.getTypeName())) {
					if (rule.appliesTo(element, retypingModule.getContext(), false)) {
						allOfTypeFromModel.add(new RetypedElement(this, element, rule));
					}
				}
			}
			return allOfTypeFromModel;
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	protected Object getCacheKeyForType(String type) throws EolModelElementTypeNotFoundException {
		return type;
	}
	
	public void setRetypingModule(EtlModule retypingModule) {
		this.retypingModule = retypingModule;
	}
	
	public EtlModule getRetypingModule() {
		return retypingModule;
	}
	
	public EplModule getPatternMatchingModule() {
		return patternMatchingModule;
	}
	
	public void setPatternMatchingModule(EplModule patternMatchingModule) {
		this.patternMatchingModule = patternMatchingModule;
	}
	
	protected RetypingModelPropertyGetter propertyGetter =  new RetypingModelPropertyGetter();
	
	@Override
	public IPropertyGetter getPropertyGetter() {
		return propertyGetter;
	}
	
	@Override
	protected Collection<Object> getAllOfKindFromModel(String kind) throws EolModelElementTypeNotFoundException {
		return getAllOfTypeFromModel(kind);
	}

	@Override
	protected Object createInstanceInModel(String type)
			throws EolModelElementTypeNotFoundException, EolNotInstantiableModelElementTypeException {
		return null;
	}

	@Override
	protected void loadModel() throws EolModelLoadingException {
		try {
			
			for (IModel retypedModel : retypedModels) {
				retypingModule.getContext().getModelRepository().addModel(retypedModel);
				patternMatchingModule.getContext().getModelRepository().addModel(retypedModel);
			}
			
			PlaceholderModel placeholder = new PlaceholderModel();
			placeholder.setName("Out");
			retypingModule.getContext().getModelRepository().addModel(placeholder);
			
			PatternMatchModel patternMatchModel = (PatternMatchModel) patternMatchingModule.execute();
			patternMatchModel.setName("P");
			retypingModule.getContext().getModelRepository().addModel(patternMatchModel);
		} catch (EolRuntimeException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void disposeModel() {
		for (IModel retypedModel : retypedModels) {
			retypedModel.dispose();
		}
	}

	@Override
	protected boolean deleteElementInModel(Object instance) throws EolRuntimeException { return false; }

	@Override
	public Object getEnumerationValue(String enumeration, String label) throws EolEnumerationValueNotFoundException { return null; }
	
	@Override
	public String getTypeNameOf(Object instance) { return null; }

	@Override
	public Object getElementById(String id) { return null; }

	@Override
	public String getElementId(Object instance) { return null; }

	@Override
	public void setElementId(Object instance, String newId) {}
	
	@Override
	public boolean isInstantiable(String type) { return false; }
	
	@Override
	protected Collection<String> getAllTypeNamesOf(Object instance) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean store(String location) { return false; }

	@Override
	public boolean store() { return false; }

	@Override
	protected Collection<Object> allContentsFromModel() {
		return null;
	}
}
