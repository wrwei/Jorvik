package util;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.papyrus.infra.architecture.commands.IModelCreationCommand;
import org.eclipse.papyrus.uml.diagram.common.commands.ModelCreationCommandBase;
import org.eclipse.papyrus.uml.tools.utils.PackageUtil;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.PackageImport;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.resource.UMLResource;

public class CreationCommand extends ModelCreationCommandBase implements IModelCreationCommand {

	public static final String PROFILES_PATHMAP = "pathmap://resources/faulttree/";
	
	public static final String PROFILE_PATH = PROFILES_PATHMAP+ "model.profile.uml";

	public static final String PROFILE_URI = "=http://faulttree/1.0";

	@Override
	protected EObject createRootElement() {
		return UMLFactory.eINSTANCE.createModel();
	}
	
	@Override
	protected void initializeModel(EObject owner) {
		super.initializeModel(owner);
		Package packageOwner = (Package) owner;

		Profile profile = (Profile) PackageUtil.loadPackage(URI.createURI(PROFILE_PATH), owner.eResource().getResourceSet());
		if (profile != null) {
			PackageUtil.applyProfile(packageOwner, profile, true);
		}

		Profile standardUMLProfile = (Profile) PackageUtil.loadPackage(URI.createURI(UMLResource.STANDARD_PROFILE_URI), owner.eResource().getResourceSet());
		if (standardUMLProfile != null) {
			PackageUtil.applyProfile(packageOwner, standardUMLProfile, true);
		}

		Package umlPrimitiveTypes = PackageUtil.loadPackage(URI.createURI(UMLResource.UML_PRIMITIVE_TYPES_LIBRARY_URI), owner.eResource().getResourceSet());
		if (umlPrimitiveTypes != null){
			PackageImport pi = UMLFactory.eINSTANCE.createPackageImport();
			pi.setImportedPackage(umlPrimitiveTypes);
			packageOwner.getPackageImports().add(pi);
		}
	}
}

