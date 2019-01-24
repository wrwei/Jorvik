package tree.profile.example.architecture;

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

public class TreeCreationCommand extends ModelCreationCommandBase implements IModelCreationCommand {


	@Override
	protected EObject createRootElement() {
		// TODO Auto-generated method stub
		return UMLFactory.eINSTANCE.createModel();
	}
	
	@Override
	protected void initializeModel(EObject owner) {
		super.initializeModel(owner);
		Package packageOwner = (Package) owner;

		// Retrieve SysML profile and apply with Sub-profile
		Profile sysml = (Profile) PackageUtil.loadPackage(URI.createURI(TreeResource.PROFILE_PATH), owner.eResource().getResourceSet());
		System.err.println(sysml);
		if (sysml != null) {
			PackageUtil.applyProfile(packageOwner, sysml, true);
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
