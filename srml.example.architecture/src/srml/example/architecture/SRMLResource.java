/*****************************************************************************
 * Copyright (c) 2015 CEA LIST.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/ 
 * 
 *  SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Benoit Maggi (CEA LIST) benoit.maggi@cea.fr - Initial API and implementation
 *  Benoit Maggi (CEA LIST) benoit.maggi@cea.fr - #507956
 *****************************************************************************/
package srml.example.architecture;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;

/**
 * Utility class to get informations on SysML resources
 *
 */
public final class SRMLResource {

	public static final String PROFILES_PATHMAP = "pathmap://MYSIMPLERML/"; //$NON-NLS-1$	
	
	public static final String PROFILE_PATH = PROFILES_PATHMAP+"srml.profile.uml"; //$NON-NLS-1$

	public static final String PROFILE_URI = "http://mySimpleRML/1.0"; //$NON-NLS-1$

//	public static final String LIBRARIES_PATHMAP = "pathmap://SysML14_LIBRARIES/"; //$NON-NLS-1$	 
//	
//	public static final String LIBRARY_PATH = LIBRARIES_PATHMAP+"SysML-Standard-Library.uml"; //$NON-NLS-1$
//
//	public static final String LIBRARY_ROOT_ID = "SysML.package_packagedElement_Libraries"; //$NON-NLS-1$
//	
//	public static final String QUDV_LIBRARY_PATH = LIBRARIES_PATHMAP+"QUDV.uml"; //$NON-NLS-1$
//
//	public static final String QUDV_LIBRARY_ROOT_ID = "QUDV"; //$NON-NLS-1$
	
	private static final String UNNAMED = "unnamed"; //$NON-NLS-1$

	private SRMLResource(){
	}
	
	/**
	 * Creates a model with SysML profile applied
	 *
	 * @param resourceSet
	 *        the resource set in which to create the model
	 * @return the created model
	 */
	public static Model createSysMLModel(ResourceSet resourceSet) {//, String modelName
		return createSysMLModel(resourceSet, UNNAMED+"."+UMLResource.FILE_EXTENSION, UNNAMED); //$NON-NLS-1$
	}

	/**
	 * Creates a model with SysML profile applied
	 *  
	 * @param resourceSet the resource set in which to create the model
	 * @param resourceName name of the resource
	 * @param modelName name of the model
	 * @return the created model
	 */
	public static Model createSysMLModel(ResourceSet resourceSet, String resourceName, String modelName) {
		Model model = UMLFactory.eINSTANCE.createModel();
		model.setName(modelName);
		Resource resource = resourceSet.createResource(URI.createURI(resourceName));
		resource.getContents().add(model);
		// Apply UML Standard profile
		// Retrieve standard profile
		Resource standardProfileResource = resourceSet.getResource(URI.createURI(UMLResource.STANDARD_PROFILE_URI), true);
		Profile umlStdProfile = (Profile) EcoreUtil.getObjectByType(standardProfileResource.getContents(), UMLPackage.Literals.PACKAGE);
		// Apply to the model
		model.applyProfile(umlStdProfile);
		// Retrieve SysML profile and apply with subprofile
		Resource sysmlProfileResource = resourceSet.getResource(URI.createURI(SRMLResource.PROFILE_PATH), true);
		Profile sysmlProfile = (Profile) EcoreUtil.getObjectByType(sysmlProfileResource.getContents(), UMLPackage.Literals.PACKAGE);
		model.applyProfile(sysmlProfile);
		return model;
	}

	/**
	 * Recursive function
	 * @param element the initial uml2 element
	 * @param profileLookup the profile we are looking for
	 * @return a package, a model profiled by SysML or null
	 */
	public static Package getFirstSysMLMatchingContainerProfiledBySysMLProfile(Element element){
		return getFirstMatchingContainerProfiledBy(element,SRMLResource.PROFILE_URI);
	}

	/**
	 * Recursive function
	 * @param element the initial uml2 element
	 * @param profileUri the profile we are looking for
	 * @return a package, a model or null
	 */
	public static Package getFirstMatchingContainerProfiledBy(Element element, String profileUri){
		if(element instanceof Package){
			Package pakage = (Package)element;
			
			EList<Profile>  appliedProfiles = pakage.getAppliedProfiles();
			if (appliedProfiles!=null){
				for (Profile appliedProfile : appliedProfiles) {
					//as soon as we match with the profile we return true
					if(appliedProfile.getURI()!=null && appliedProfile.getURI().compareTo(profileUri)==0){
						return pakage;
					}
				}		
			}

			Element elementOwner = element.getOwner();
			if(elementOwner!=null){
				return getFirstMatchingContainerProfiledBy(elementOwner, profileUri);
			}
		}
		//If no match then look at the nearest package
		if(element!=null){
			Package myNearestPackage = element.getNearestPackage();
			if(myNearestPackage!=element){
				return getFirstMatchingContainerProfiledBy(myNearestPackage, profileUri);
			}
		}
		//If we are here, it means we have no lookuped profile attached at one container of our element.
		return null;
	}


	/**
	 * @param element
	 * @return true if the element is profiled with SysML profile
	 */
	public static boolean isSysMLProfileApplied(Element element){
		return isProfiledByProfile(element,SRMLResource.PROFILE_URI);
	}


	/**
	 * @param element
	 * @param profileUri
	 * @return true if the element is profiled by the given profileUri
	 */
	public static boolean isProfiledByProfile(Element element, String profileUri){
		return getFirstMatchingContainerProfiledBy(element,profileUri)!=null;
	}
}
