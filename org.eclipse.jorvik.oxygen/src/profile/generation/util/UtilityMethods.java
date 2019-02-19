package profile.generation.util;

/*******************************************************************************
 * Copyright (c) 2019 The University of York.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Athanathias Zolotas - initial API and implementation
 *     Ran Wei - Enhanced features and additional APIs
 ******************************************************************************/

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.egl.EglFileGeneratingTemplate;
import org.eclipse.epsilon.egl.EglFileGeneratingTemplateFactory;
import org.eclipse.epsilon.egl.EglTemplateFactoryModuleAdapter;
import org.eclipse.epsilon.emc.emf.EmfMetaModel;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.emc.plainxml.PlainXmlModel;
import org.eclipse.epsilon.emc.uml.UmlModel;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.models.IModel;
import org.eclipse.epsilon.eol.models.IRelativePathResolver;
import org.eclipse.epsilon.etl.EtlModule;
import org.eclipse.epsilon.evl.EvlModule;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.pde.internal.core.natures.PDE;

import profile.generation.activator.Activator;

@SuppressWarnings("restriction")
public class UtilityMethods {

	//name
	String name;
	
	String project_name;
	
	//progress monitor
	IProgressMonitor progressMonitor = new NullProgressMonitor();
	
	//get current workspace
	IWorkspace workspace = ResourcesPlugin.getWorkspace();
	
	//get root of workspace
	IWorkspaceRoot root = workspace.getRoot();
	
	IProject project;

	public UtilityMethods(String theSelectedFilePath) {
		//get name of the overall EPackage of a certain emfatic source
		String _name = getNameOfEPackage(theSelectedFilePath);
		project_name = "org.papyrus." + _name.toLowerCase();
		this.name = _name.toLowerCase();
		project = root.getProject(project_name);
	}
	
	public String checkAnnotatedEcore(String theSelectedFilePath, IProject theSelectedFileParentIProject) throws Exception {
		//emf source
		EmfModel sourceModel = createAndLoadAnEmfModel("http://www.eclipse.org/emf/2002/Ecore", theSelectedFilePath, "Source", "true", "false");
		
		ArrayList<IModel> allTheModels = new ArrayList<IModel>();
		allTheModels.addAll(Arrays.asList(sourceModel));
		String ret = performEVLTransformation(allTheModels, "files/checkAnnotatedEcore.evl");
		return ret;
	}
	

	public IProject createPluginProject() throws CoreException {
		if (!project.exists()) {
			project.create(progressMonitor);
		}
		project.open(progressMonitor);
		IProjectDescription desc = project.getDescription();
		desc.setNatureIds(new String[] { PDE.PLUGIN_NATURE, JavaCore.NATURE_ID });
		project.setDescription(desc, progressMonitor);
		
		
		IJavaProject javaProject = JavaCore.create(project);
		
		IClasspathEntry[] buildPath = {
				JavaCore.newSourceEntry(project.getFullPath().append("src")),
						JavaRuntime.getDefaultJREContainerEntry() };

		javaProject.setRawClasspath(buildPath, project.getFullPath().append(
				"bin"), progressMonitor);

		IFolder srcFolder = project.getFolder("src");
		srcFolder.create(IResource.NONE, true, progressMonitor);
		IFolder resourcesFolder = project.getFolder("resources");
		resourcesFolder.create(IResource.FOLDER, true, progressMonitor);
		
		IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(srcFolder);
		IPackageFragment fragment = root.createPackageFragment("util", true, progressMonitor);

		return project;
	}
	
	public void createThePluginXml(String theSelectedFilePath, String theDestinationIProjectFolder) throws Exception {
		//get the source ecore metamodel
		EmfModel sourceModel = createAndLoadAnEmfModel("http://www.eclipse.org/emf/2002/Ecore", theSelectedFilePath, "Source", "true", "false");

		//target model is plain XML
		PlainXmlModel targetModel = new PlainXmlModel();
		StringProperties targetProperties = new StringProperties();
		targetProperties.put(PlainXmlModel.PROPERTY_FILE,
				theDestinationIProjectFolder + File.separator + "plugin.xml");
		targetProperties.put(PlainXmlModel.PROPERTY_NAME, "Target");
		targetProperties.put(PlainXmlModel.PROPERTY_READONLOAD, "false");
		targetProperties.put(PlainXmlModel.PROPERTY_STOREONDISPOSAL, "true");
		targetModel.load(targetProperties);

		ArrayList<IModel> allTheModels = new ArrayList<IModel>();
		allTheModels.addAll(Arrays.asList(sourceModel, targetModel));
		//execute pluginXMLGenerationM2M on source+target
		doTheETLTransformation(allTheModels, "files/pluginXmlGenerationM2M.etl");
	}
	
	public void createThebuildPropertiesFile(String theDestinationIProjectFolder) throws IOException {
		BufferedWriter output = new BufferedWriter(
				new FileWriter(theDestinationIProjectFolder + File.separator + "build.properties", false));
		try {
			output.write("bin.includes = META-INF/,\\\n" + "plugin.xml\n");
			output.close();
		} catch (IOException ex) {
			System.out.println("Error writing to file...");
		}
	}
	
	public void createTheManifestFile(String theSelectedFilePath, String theDestinationIProjectFolder) throws IOException {
		new File(theDestinationIProjectFolder + File.separator + "META-INF").mkdir();
		BufferedWriter output = new BufferedWriter(new FileWriter(
				theDestinationIProjectFolder + File.separator + "META-INF" + File.separator + "MANIFEST.MF",
				false));
		try {
			output.write("Manifest-Version: 1.0\n" + "Bundle-ManifestVersion: 2\n" + "Bundle-Name: " + project_name + "\n"
					+ "Bundle-SymbolicName: " + project_name + ";singleton:=true\n" + "Bundle-Version: 1.0.0.qualifier\n"
					+ "Require-Bundle:"
					+ " org.eclipse.ui,\n"
					+ " org.eclipse.core.runtime,\n"
					+ " org.eclipse.core.resources,\n"
					+ " org.eclipse.emf.ecore,\n"
					+ " org.eclipse.uml2.types,\n"
					+ " org.eclipse.uml2.uml,\n"
					+ " org.eclipse.papyrus.uml.diagram.common,\n"
					+ " org.eclipse.papyrus.uml.extensionpoints,\n"
					+ " org.eclipse.papyrus.uml.diagram.clazz,\n"
					+ " org.eclipse.papyrus.infra.core,\n"
					+ " org.eclipse.papyrus.infra.types,\n"
					+ " org.eclipse.papyrus.infra.types.core,\n"
					+ " org.eclipse.papyrus.infra.architecture,\n"
					+ " org.eclipse.gmf.runtime.diagram.core\n");
			output.close();
		} catch (IOException ex) {
			System.out.println("Error writing to file...");
		}
	}
	
	public void createTheProfileUmlFile(String theSelectedFilePath, String theDestinationIProjectFolder, IProject theSelectedFileParentIProject) throws Exception {

		// The emfatic (ecore) source
		EmfModel sourceModel = 	createAndLoadAnEmfModel("http://www.eclipse.org/emf/2002/Ecore", theSelectedFilePath, "Source", "true", "false");
		
		// The ultimate goal: the UML profile
		UmlModel targetModel = createAndLoadAUmlModel("http://www.eclipse.org/uml2/5.0.0/UML", theDestinationIProjectFolder + File.separator + "model.profile.uml", "Profile", "false", "true");
		
		// The UML Metamodel
		UmlModel umlMetaModel = createAndLoadAUmlModel("http://www.eclipse.org/emf/2002/Ecore", "pathmap://UML_METAMODELS/UML.metamodel.uml", "UMLM2", "true", "false");
		
		// The UML Ecore Metamodel
		EmfMetaModel umlEcoreMetaModel = createAndLoadAnEmfMetaModel("http://www.eclipse.org/uml2/5.0.0/UML", "UMLEcore", "true", "false");
		
		// The ECore Metamodel
		EmfMetaModel ECoreMetaModel = createAndLoadAnEmfMetaModel("http://www.eclipse.org/emf/2002/Ecore", "EcoreM2", "true", "false");
		
		// The Ecore Primitive Types
		UmlModel ecorePrimitiveTypesModel = new UmlModel();
		StringProperties ecorePrimitiveTypesModelProperties = new StringProperties();
		ecorePrimitiveTypesModelProperties.put(UmlModel.PROPERTY_MODEL_FILE,
				"pathmap://UML_LIBRARIES/EcorePrimitiveTypes.library.uml");
		ecorePrimitiveTypesModelProperties.put(UmlModel.PROPERTY_NAME, "ECorePrimitiveTypes");
		ecorePrimitiveTypesModelProperties.put(UmlModel.PROPERTY_READONLOAD, "true");
		ecorePrimitiveTypesModelProperties.put(UmlModel.PROPERTY_STOREONDISPOSAL, "false");
		ecorePrimitiveTypesModel.load(ecorePrimitiveTypesModelProperties, (IRelativePathResolver) null);
		
		ArrayList<IModel> allTheModels = new ArrayList<IModel>();
		allTheModels.addAll(Arrays.asList(sourceModel, targetModel, umlMetaModel, umlMetaModel, umlEcoreMetaModel, ECoreMetaModel, ecorePrimitiveTypesModel));
		doTheETLTransformation(allTheModels, "files/emf2umlprofile2Annotations.etl");
		
		// The emfatic (ecore) source
		sourceModel = 	createAndLoadAnEmfModel("http://www.eclipse.org/emf/2002/Ecore", theSelectedFilePath, "Source", "true", "false");
		// The UML Metamodel
		umlMetaModel = createAndLoadAUmlModel("http://www.eclipse.org/emf/2002/Ecore", "pathmap://UML_METAMODELS/UML.metamodel.uml", "UMLM2", "true", "false");
		// The UML Ecore Metamodel
		umlEcoreMetaModel = createAndLoadAnEmfMetaModel("http://www.eclipse.org/uml2/5.0.0/UML", "UMLEcore", "true", "false");
		// The ECore Metamodel
		ECoreMetaModel = createAndLoadAnEmfMetaModel("http://www.eclipse.org/emf/2002/Ecore", "EcoreM2", "true", "false");
		// The Ecore Primitive Types
		ecorePrimitiveTypesModel = new UmlModel();
		ecorePrimitiveTypesModelProperties = new StringProperties();
		ecorePrimitiveTypesModelProperties.put(UmlModel.PROPERTY_MODEL_FILE,
						"pathmap://UML_LIBRARIES/EcorePrimitiveTypes.library.uml");
		ecorePrimitiveTypesModelProperties.put(UmlModel.PROPERTY_NAME, "ECorePrimitiveTypes");
		ecorePrimitiveTypesModelProperties.put(UmlModel.PROPERTY_READONLOAD, "true");
		ecorePrimitiveTypesModelProperties.put(UmlModel.PROPERTY_STOREONDISPOSAL, "false");
		ecorePrimitiveTypesModel.load(ecorePrimitiveTypesModelProperties, (IRelativePathResolver) null);
		allTheModels.clear();
		targetModel = createAndLoadAUmlModel("http://www.eclipse.org/uml2/5.0.0/UML", theDestinationIProjectFolder + File.separator + "model.profile.uml", "Profile", "true", "true");
		allTheModels.addAll(Arrays.asList(sourceModel, targetModel, umlMetaModel, umlMetaModel, umlEcoreMetaModel, ECoreMetaModel, ecorePrimitiveTypesModel));
		doTheUsersETLTransformation(allTheModels, "emf2umlprofile2Annotations.etl", theSelectedFileParentIProject);
	}
	
	public void createTheModelProfileNotationFile(String theDestinationIProjectFolder) throws IOException {
		BufferedWriter output = new BufferedWriter(
				new FileWriter(theDestinationIProjectFolder + File.separator + "model.profile.notation", false));
		try {
			output.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
					+ "<xmi:XMI xmi:version=\"2.0\" xmlns:xmi=\"http://www.omg.org/XMI\"/>\n");
			output.close();
		} catch (IOException ex) {
			System.out.println("Error writing to file...");
		}
	}

	public void createTheModelProfileDiFile(String theDestinationIProjectFolder) throws IOException {
		BufferedWriter output = new BufferedWriter(
				new FileWriter(theDestinationIProjectFolder + File.separator + "model.profile.di", false));
		try {
			output.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
					+ "<architecture:ArchitectureDescription xmi:version=\"2.0\" xmlns:xmi=\"http://www.omg.org/XMI\" "
					+ "xmlns:architecture=\"http://www.eclipse.org/papyrus/infra/core/architecture\" contextId=\"org.eclipse.papyrus.uml.architecture.Profile\"/>");
			output.close();
		} catch (IOException ex) {
			System.out.println("Error writing to file...");
		}
	}

	public void createThePaletteConfiguration(String theSelectedFilePath, String theDestinationIProjectFolder, IProject theSelectedFileParentIProject) throws Exception {
		//emf source
		EmfModel sourceModel = createAndLoadAnEmfModel("http://www.eclipse.org/emf/2002/Ecore", theSelectedFilePath, "Source", "true", "false");
		
		//palette 
		EmfModel targetModel = createAndLoadAnEmfModel("http://www.eclipse.org/papyrus/diagram/paletteconfiguration/0.8, "
				+ "http://www.eclipse.org/papyrus/infra/elementtypesconfigurations/1.2", theDestinationIProjectFolder + File.separator
				+ "resources" + File.separator + name + ".paletteconfiguration", "Target", "false", "true");
		
		//palette metamodel
		EmfMetaModel paletteConfigurationM2 = createAndLoadAnEmfMetaModel("http://www.eclipse.org/papyrus/diagram/paletteconfiguration/0.8", "PaletteConfigurationM2", "true", "false");
		
		//element types model
		EmfModel elementTypes = createAndLoadAnEmfModel("http://www.eclipse.org/papyrus/infra/elementtypesconfigurations/1.2", 
				theDestinationIProjectFolder + File.separator + "resources" + File.separator + "diagramshapes.elementtypesconfigurations", "ElementTypes", "true", "false");

		ArrayList<IModel> allTheModels = new ArrayList<IModel>();
		allTheModels.addAll(Arrays.asList(sourceModel, targetModel, paletteConfigurationM2, elementTypes));
		doTheETLTransformation(allTheModels, "files/paletteConfigurationM2M.etl");
		
		// User's transformation, if any
		sourceModel = createAndLoadAnEmfModel("http://www.eclipse.org/emf/2002/Ecore", theSelectedFilePath, "Source", "true", "false");
		//palette 
		targetModel = createAndLoadAnEmfModel("http://www.eclipse.org/papyrus/diagram/paletteconfiguration/0.8, "
						+ "http://www.eclipse.org/papyrus/infra/elementtypesconfigurations/1.2", theDestinationIProjectFolder + File.separator
						+ "resources" + File.separator + name + ".paletteconfiguration", "Target", "false", "true");
				
		//palette metamodel
		paletteConfigurationM2 = createAndLoadAnEmfMetaModel("http://www.eclipse.org/papyrus/diagram/paletteconfiguration/0.8", "PaletteConfigurationM2", "true", "false");
		
		//element types model
		elementTypes = createAndLoadAnEmfModel("http://www.eclipse.org/papyrus/infra/elementtypesconfigurations/1.2", 
				theDestinationIProjectFolder + File.separator + "resources" + File.separator + "diagramshapes.elementtypesconfigurations", "ElementTypes", "true", "false");

		allTheModels.clear();
		allTheModels.addAll(Arrays.asList(sourceModel, targetModel, paletteConfigurationM2, elementTypes));
		doTheUsersETLTransformation(allTheModels, "paletteConfigurationM2M.etl", theSelectedFileParentIProject);
	}
	
	public void createTheArchitectureModel(String theSelectedFilePath, String theDestinationIProjectFolder, IProject theSelectedFileParentIProject) throws Exception {
		//emf source
		EmfModel sourceModel = createAndLoadAnEmfModel("http://www.eclipse.org/emf/2002/Ecore", theSelectedFilePath, "Source", "true", "false");
		
		//target architecture model
		EmfModel targetModel = createAndLoadAnEmfModel("http://www.eclipse.org/papyrus/infra/core/architecture,"
				+ "http://www.eclipse.org/emf/2002/Ecore, "
				+ "http://www.eclipse.org/papyrus/infra/elementtypesconfigurations/1.2, "
				+ "http://www.eclipse.org/papyrus/infra/gmfdiag/representation, "
				+ "http://www.eclipse.org/papyrus/diagram/paletteconfiguration/0.8, "
				+ "http://www.eclipse.org/papyrus/infra/core/architecture/representation", theDestinationIProjectFolder + File.separator
				+ "resources" + File.separator + name + ".architecture", "Architecture", "false", "true");
		
		//element types model
		EmfModel elementTypes = createAndLoadAnEmfModel("http://www.eclipse.org/papyrus/infra/elementtypesconfigurations/1.2", 
				theDestinationIProjectFolder + File.separator + "resources" + File.separator + "diagramshapes.elementtypesconfigurations", "ElementTypes", "true", "false");
		
		//palette model
		EmfModel palette = createAndLoadAnEmfModel("http://www.eclipse.org/papyrus/diagram/paletteconfiguration/0.8", theDestinationIProjectFolder + File.separator
				+ "resources" + File.separator + name + ".paletteconfiguration", "Palette", "true", "false");
		
		//uml metamodel
		EmfMetaModel umlMetamodel = createAndLoadAnEmfMetaModel("http://www.eclipse.org/uml2/5.0.0/UML", "UML", "true", "false");

		ArrayList<IModel> allTheModels = new ArrayList<IModel>();
		allTheModels.addAll(Arrays.asList(sourceModel, targetModel, elementTypes, palette, umlMetamodel));
		doEOLTransformation(allTheModels, "files/architecture_gen.eol");

		// User's transformation, if any
//		sourceModel = createAndLoadAnEmfModel("http://www.eclipse.org/emf/2002/Ecore", theSelectedFilePath, "Source", "true", "false");
//		targetModel = createAndLoadAnEmfModel("http://www.eclipse.org/papyrus/infra/core/architecture,"
//				+ "http://www.eclipse.org/emf/2002/Ecore, "
//				+ "http://www.eclipse.org/papyrus/infra/elementtypesconfigurations/1.2, "
//				+ "http://www.eclipse.org/papyrus/infra/gmfdiag/representation, "
//				+ "http://www.eclipse.org/papyrus/diagram/paletteconfiguration/0.8, "
//				+ "http://www.eclipse.org/papyrus/infra/core/architecture/representation", theDestinationIProjectFolder + File.separator
//				+ "resources" + File.separator + name + ".architecture", "Architecture", "false", "true");
//		
//		elementTypes = createAndLoadAnEmfModel("http://www.eclipse.org/papyrus/infra/elementtypesconfigurations/1.2", 
//				theDestinationIProjectFolder + File.separator + "resources" + File.separator + "diagramshapes.elementtypesconfigurations", "ElementTypes", "true", "false");
//		
//		palette = createAndLoadAnEmfModel("http://www.eclipse.org/papyrus/diagram/paletteconfiguration/0.8", theDestinationIProjectFolder + File.separator
//				+ "resources" + File.separator + name + ".paletteconfiguration", "Palette", "true", "false");
//		
//		umlMetamodel = createAndLoadAnEmfMetaModel("http://www.eclipse.org/uml2/5.0.0/UML", "UML", "true", "false");
//
//		allTheModels.clear();
//		allTheModels.addAll(Arrays.asList(sourceModel, targetModel, elementTypes, palette, umlMetamodel));
//		doEOLTransformation(allTheModels, "architecture_gen.eol", theSelectedFileParentIProject);
	}
	
	public void createCreationCommand(String theSelectedFilePath, String theDestinationIProjectFolder, IProject theSelectedFileParentIProject) throws Exception {
		//emf source
		EmfModel sourceModel = createAndLoadAnEmfModel("http://www.eclipse.org/emf/2002/Ecore", theSelectedFilePath, "Source", "true", "false");

		//egl factory and module
		EglFileGeneratingTemplateFactory factory = new EglFileGeneratingTemplateFactory();
		EglTemplateFactoryModuleAdapter eglModule = new EglTemplateFactoryModuleAdapter(factory);

		eglModule.getContext().getModelRepository().addModel(sourceModel);

		java.net.URI EglFile = Activator.getDefault().getBundle().getResource("files/creationCommandGeneration.egl").toURI();

		EglFileGeneratingTemplate template = (EglFileGeneratingTemplate) factory.load(EglFile);
		template.process();
		File target = new File(theDestinationIProjectFolder + File.separator + "src" + File.separator + "util" + File.separator
				+ "CreationCommand.java");
		target.createNewFile();
		template.generate(target.toURI().toString());
	}

	public void createTheCSSFile(String theSelectedFilePath, String theDestinationIProjectFolder, IProject theSelectedFileParentIProject) throws Exception {
		EmfModel sourceModel = createAndLoadAnEmfModel("http://www.eclipse.org/emf/2002/Ecore", theSelectedFilePath, "Source", "true", "false");

		EglFileGeneratingTemplateFactory factory = new EglFileGeneratingTemplateFactory();
		EglTemplateFactoryModuleAdapter eglModule = new EglTemplateFactoryModuleAdapter(factory);

		eglModule.getContext().getModelRepository().addModel(sourceModel);

		java.net.URI EglFile = Activator.getDefault().getBundle().getResource("files/cssFileGeneration.egl").toURI();

		EglFileGeneratingTemplate template = (EglFileGeneratingTemplate) factory.load(EglFile);
		template.process();
		File target = new File(theDestinationIProjectFolder + File.separator + "resources" + File.separator + name
				+ "Diagram.css");
		target.createNewFile();
		template.generate(target.toURI().toString());
		
		//User's CSS generation. 
		File dir = new File(theSelectedFileParentIProject.getLocation().toOSString() + File.separator + "transformations");
		FilenameFilter filter = new FilenameFilter() {
	         public boolean accept (File dir, String name) { 
	        	 return name.equals("cssFileGeneration.egl");
	         } 
	    }; 
	    String[] children = dir.list(filter);
	    if (children == null) {
	    	System.out.println("Either dir does not exist or is not a directory"); 
	    } else if (children.length > 0) {
	    	sourceModel = createAndLoadAnEmfModel("http://www.eclipse.org/emf/2002/Ecore", theSelectedFilePath, "Source", "true", "false");
			factory = new EglFileGeneratingTemplateFactory();
			eglModule = new EglTemplateFactoryModuleAdapter(factory);
			eglModule.getContext().getModelRepository().addModel(sourceModel);
			EglFile = new File(theSelectedFileParentIProject.getLocation().toOSString() + File.separator + "transformations" + File.separator + children[0]).toURI();
			template = (EglFileGeneratingTemplate) factory.load(EglFile);
			template.process();
			target = new File(theDestinationIProjectFolder + File.separator + "resources" + File.separator + name
					+ "diagram.css");		
			// Append to file, not generate a new one.
			template.append(target.toURI().toString());
	    }
	}
	
	
	

	public void createTheUml2EmfETLFile(String theSelectedFilePath, String theDestinationIProjectFolder, IProject theSelectedFileParentIProject) throws Exception {
		EmfModel sourceModel = createAndLoadAnEmfModel("http://www.eclipse.org/emf/2002/Ecore", theSelectedFilePath, "Source", "true", "false");

		EglFileGeneratingTemplateFactory factory = new EglFileGeneratingTemplateFactory();
		EglTemplateFactoryModuleAdapter eglModule = new EglTemplateFactoryModuleAdapter(factory);

		eglModule.getContext().getModelRepository().addModel(sourceModel);

		java.net.URI EglFile = Activator.getDefault().getBundle().getResource("files/uml2emfETLfileGeneration.egl").toURI();

		EglFileGeneratingTemplate template = (EglFileGeneratingTemplate) factory.load(EglFile);
		template.process();
		File target = new File(theDestinationIProjectFolder + File.separator + "resources" + File.separator + name
				+ "uml2emf.etl");
		target.createNewFile();
		template.generate(target.toURI().toString());
	}
	
	public void createTheTypesConfigurations(String theSelectedFilePath, String theDestinationIProjectFolder, IProject theSelectedFileParentIProject) throws Exception {
		// Our transformation
		EmfModel sourceModel = createAndLoadAnEmfModel("http://www.eclipse.org/emf/2002/Ecore", theSelectedFilePath, "Source", "true", "false");
		EmfModel targetModel = createAndLoadAnEmfModel("http://www.eclipse.org/papyrus/uml/types/applystereotypeadvice/1.1, http://www.eclipse.org/papyrus/infra/elementtypesconfigurations/1.1, http://www.eclipse.org/papyrus/uml/types/stereotypematcher/1.1", theDestinationIProjectFolder + File.separator
				+ "resources" + File.separator + "modelelement.typesconfigurations", "Target", "false", "true");
		ArrayList<IModel> allTheModels = new ArrayList<IModel>();
		allTheModels.addAll(Arrays.asList(sourceModel, targetModel));
		doTheETLTransformation(allTheModels, "files/typesConfigurationsM2M.etl");
		
		// User's transformation, if any
		sourceModel = createAndLoadAnEmfModel("http://www.eclipse.org/emf/2002/Ecore", theSelectedFilePath, "Source", "true", "false");
		targetModel = createAndLoadAnEmfModel("http://www.eclipse.org/papyrus/uml/types/applystereotypeadvice/1.1, http://www.eclipse.org/papyrus/infra/elementtypesconfigurations/1.1, http://www.eclipse.org/papyrus/uml/types/stereotypematcher/1.1", theDestinationIProjectFolder + File.separator
				+ "resources" + File.separator + "modelelement.typesconfigurations", "Target", "true", "true");
		allTheModels.clear();
		allTheModels.addAll(Arrays.asList(sourceModel, targetModel));
		doTheUsersETLTransformation(allTheModels, "typesConfigurationsM2M.etl", theSelectedFileParentIProject);
	}

	public void createTheElementTypeConfigurations(String theSelectedFilePath, String theDestinationIProjectFolder, IProject theSelectedFileParentIProject) throws Exception {
		EmfModel sourceModel = createAndLoadAnEmfModel("http://www.eclipse.org/emf/2002/Ecore", theSelectedFilePath, "Source", "true", "false");
		
		ArrayList<String> metamodelURIs = new ArrayList<String>();
		EmfModel targetModel = createAndLoadAnEmfModel("http://www.eclipse.org/papyrus/infra/elementtypesconfigurations/1.2, "
				+ "http://www.eclipse.org/papyrus/uml/types/applystereotypeadvice/1.1, "
				+ "http://www.eclipse.org/papyrus/uml/types/stereotypematcher/1.1", theDestinationIProjectFolder + File.separator
				+ "resources" + File.separator + "diagramshapes.elementtypesconfigurations", "Target", "false", "true");
		
		EmfModel umltypes = createAndLoadAnEmfModel("http://www.eclipse.org/papyrus/infra/elementtypesconfigurations/1.2", 
				"platform:/plugin/org.eclipse.papyrus.uml.service.types/model/uml.elementtypesconfigurations", "UMLTypes", "true", "false");
		
		EmfModel umlDITypes = createAndLoadAnEmfModel("http://www.eclipse.org/papyrus/infra/elementtypesconfigurations/1.2", 
				"platform:/plugin/org.eclipse.papyrus.uml.service.types/model/umldi.elementtypesconfigurations", "UMLDITypes", "true", "false");

		ArrayList<IModel> allTheModels = new ArrayList<IModel>();
		allTheModels.addAll(Arrays.asList(sourceModel, targetModel, umltypes, umlDITypes));
		doTheETLTransformation(allTheModels, "files/elementTypesConfigurationsM2M.etl");

		// User's transformation, if any
		sourceModel = createAndLoadAnEmfModel("http://www.eclipse.org/emf/2002/Ecore", theSelectedFilePath, "Source", "true", "false");
		targetModel = createAndLoadAnEmfModel("http://www.eclipse.org/papyrus/infra/elementtypesconfigurations/1.2, "
				+ "http://www.eclipse.org/papyrus/uml/types/applystereotypeadvice/1.1, "
				+ "http://www.eclipse.org/papyrus/uml/types/stereotypematcher/1.1", theDestinationIProjectFolder + File.separator
				+ "resources" + File.separator + "diagramshapes.elementtypesconfigurations", "Target", "false", "true");
		umltypes = createAndLoadAnEmfModel("http://www.eclipse.org/papyrus/infra/elementtypesconfigurations/1.2", 
				"platform:/plugin/org.eclipse.papyrus.uml.service.types/model/uml.elementtypesconfigurations", "UMLTypes", "true", "false");
		umlDITypes = createAndLoadAnEmfModel("http://www.eclipse.org/papyrus/infra/elementtypesconfigurations/1.2", 
				"platform:/plugin/org.eclipse.papyrus.uml.service.types/model/umldi.elementtypesconfigurations", "UMLDI", "true", "false");

		allTheModels.clear();
		allTheModels.addAll(Arrays.asList(sourceModel, targetModel, umltypes, umlDITypes));
		doTheUsersETLTransformation(allTheModels, "elementTypesConfigurationsM2M.etl", theSelectedFileParentIProject);
	}

	public void copyTheIcons(String theSelectedFilePath, String theSelectedFileParentFolder, String theDestinationIProjectFolder) throws IOException {

		ArrayList<String> iconPaths = getTheListOfIconPathsInModel(theSelectedFilePath);
		for (String iconPath : iconPaths) {

			// In order to be able to do the copy, I need firstly to create the
			// target directory. I do that by striping the name of the file.
			// The target directory is: the target project location + the
			// content of the icon details set in EMF without the file name.
			String theTargetDirectory = theDestinationIProjectFolder + File.separator
					+ iconPath.substring(0, iconPath.lastIndexOf("/"));
			File targetDir = new File(theTargetDirectory);
			if (!targetDir.exists()) {
				targetDir.mkdir();
			}
			String fromIconPath = theSelectedFileParentFolder + File.separator + iconPath;
			String toIconPath = theDestinationIProjectFolder + File.separator + iconPath;
			copyFiles(fromIconPath, toIconPath);

		}
	}

	public void copyTheShapes(String theSelectedFilePath, String theSelectedFileParentFolder, String theDestinationIProjectFolder) throws IOException {
		ArrayList<String> shapePaths = getTheListOfShapePathsInModel(theSelectedFilePath);
		for (String shapePath : shapePaths) {
			// In order to be able to do the copy, I need firstly to create the
			// target directory. I do that by striping the name of the file.
			// The target directory is: the target project location + the
			// content of the shape details set in EMF without the file name.
			String theTargetDirectory = theDestinationIProjectFolder + File.separator
					+ shapePath.substring(0, shapePath.lastIndexOf("/"));
			File targetDir = new File(theTargetDirectory);
			if (!targetDir.exists()) {
				targetDir.mkdir();
			}
			String fromShapePath = theSelectedFileParentFolder + File.separator + shapePath;
			String toShapePath = theDestinationIProjectFolder + File.separator + shapePath;
			copyFiles(fromShapePath, toShapePath);
		}
	}

	private void copyFiles(String from, String to) throws IOException {
		Path fromPath = Paths.get(from);
		Path toPath = Paths.get(to);
		CopyOption[] options = new CopyOption[] { StandardCopyOption.REPLACE_EXISTING,
				StandardCopyOption.COPY_ATTRIBUTES };
		java.nio.file.Files.copy(fromPath, toPath, options);
	}

	private ArrayList<String> getTheListOfIconPathsInModel(String theSelectedFilePath) {
		File f = new File(theSelectedFilePath);
		URI fileURI = URI.createFileURI(f.getAbsolutePath());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());

		ResourceSet resourceSet = new ResourceSetImpl();
		Resource resource1 = resourceSet.getResource(fileURI, true);
		ArrayList<String> iconPaths = new ArrayList<String>();
		TreeIterator<EObject> allContents = resource1.getAllContents();
		while (allContents.hasNext()) {
			EObject next = allContents.next();
			if (next instanceof EAnnotation) {
				EAnnotation annotation = (EAnnotation) next;
				for (String theKey : annotation.getDetails().keySet()) {
					if (theKey.equals("icon")) {
						iconPaths.add(annotation.getDetails().get(theKey));
					}
				}
			}
		}
		return iconPaths;
	}

	private ArrayList<String> getTheListOfShapePathsInModel(String theSelectedFilePath) {
		File f = new File(theSelectedFilePath);
		URI fileURI = URI.createFileURI(f.getAbsolutePath());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());

		ResourceSet resourceSet = new ResourceSetImpl();
		Resource resource1 = resourceSet.getResource(fileURI, true);
		ArrayList<String> shapePaths = new ArrayList<String>();
		TreeIterator<EObject> allContents = resource1.getAllContents();
		while (allContents.hasNext()) {
			EObject next = allContents.next();
			if (next instanceof EAnnotation) {
				EAnnotation annotation = (EAnnotation) next;
				for (String theKey : annotation.getDetails().keySet()) {
					if (theKey.equals("shape")) {
						shapePaths.add(annotation.getDetails().get(theKey));
					}
				}
			}
		}
		return shapePaths;
	}

	
	//get the name of the overall containing package
	private String getNameOfEPackage(String theSelectedFilePath) {
		// The emfatic (ecore) source
		File f = new File(theSelectedFilePath);
		URI fileURI = URI.createFileURI(f.getAbsolutePath());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());

		ResourceSet resourceSet = new ResourceSetImpl();
		Resource resource1 = resourceSet.getResource(fileURI, true);
		EPackage wdwPackage = (EPackage) resource1.getContents().get(0);

		return wdwPackage.getName();
	}
	
	private EmfModel createAndLoadAnEmfModel(String metamodelURI, String modelFile, String modelName, String readOnLoad, String storeOnDisposal) throws EolModelLoadingException {
		EmfModel theModel = new EmfModel();
		StringProperties properties = new StringProperties();
		properties.put(EmfModel.PROPERTY_METAMODEL_URI, metamodelURI);
		properties.put(EmfModel.PROPERTY_MODEL_FILE, modelFile);
		properties.put(EmfModel.PROPERTY_NAME, modelName);
		properties.put(EmfModel.PROPERTY_READONLOAD, readOnLoad);
		properties.put(EmfModel.PROPERTY_STOREONDISPOSAL, storeOnDisposal);
		theModel.load(properties, (IRelativePathResolver) null);
		return theModel;
	}
	
	private EmfMetaModel createAndLoadAnEmfMetaModel(String metamodelUri, String modelName, String readOnLoad, String storeOnDisposal) throws EolModelLoadingException {
		EmfMetaModel metamodel = new EmfMetaModel();
		StringProperties properties = new StringProperties();
		properties.put(EmfMetaModel.PROPERTY_METAMODEL_URI, metamodelUri);
		properties.put(EmfMetaModel.PROPERTY_NAME, modelName);
		properties.put(EmfMetaModel.PROPERTY_READONLOAD, readOnLoad);
		properties.put(EmfMetaModel.PROPERTY_STOREONDISPOSAL, storeOnDisposal);
		metamodel.load(properties, (IRelativePathResolver) null);
		return metamodel;
	}
	
	private UmlModel createAndLoadAUmlModel(String metamodelUri, String modelFile, String modelName, String readOnLoad, String storeOnDisposal) throws EolModelLoadingException {
		UmlModel umlModel = new UmlModel();
		StringProperties properties = new StringProperties();
		properties.put(UmlModel.PROPERTY_METAMODEL_URI, metamodelUri);
		properties.put(UmlModel.PROPERTY_MODEL_FILE, modelFile);
		properties.put(UmlModel.PROPERTY_NAME, modelName);
		properties.put(UmlModel.PROPERTY_READONLOAD, readOnLoad);
		properties.put(UmlModel.PROPERTY_STOREONDISPOSAL, storeOnDisposal);
		umlModel.load(properties, (IRelativePathResolver) null);
		return umlModel;
	}
	
	private void doTheETLTransformation(ArrayList<IModel> allTheModels, String theFile) throws Exception {
		EtlModule etlModule = new EtlModule();
		for (IModel theModel : allTheModels) {
			etlModule.getContext().getModelRepository().addModel(theModel);
		}
		java.net.URI etlFile = Activator.getDefault().getBundle()
				.getResource(theFile).toURI();
		etlModule.parse(etlFile);
		etlModule.execute();
		etlModule.getContext().getModelRepository().dispose();
	}
	
	private String performEVLTransformation(ArrayList<IModel> allTheModels, String theFile) throws Exception {
		String ret = null;
		EvlModule evlModule = new EvlModule();
		for (IModel theModel : allTheModels) {
			evlModule.getContext().getModelRepository().addModel(theModel);
		}
		java.net.URI evlFile = Activator.getDefault().getBundle()
				.getResource(theFile).toURI();
		evlModule.parse(evlFile);
		evlModule.execute();
		if (evlModule.getContext().getUnsatisfiedConstraints().size() > 0) {
			ret = evlModule.getContext().getUnsatisfiedConstraints().toString();
		}
		evlModule.getContext().getModelRepository().dispose();
		return ret;
	}
	
	private void doEOLTransformation(ArrayList<IModel> allTheModels, String theFile) throws Exception {
		EolModule eolModule = new EolModule();
		for (IModel theModel : allTheModels) {
			eolModule.getContext().getModelRepository().addModel(theModel);
		}
		java.net.URI etlFile = Activator.getDefault().getBundle()
				.getResource(theFile).toURI();
		eolModule.parse(etlFile);
		eolModule.execute();
		eolModule.getContext().getModelRepository().dispose();
	}
	
	private void doTheUsersETLTransformation(ArrayList<IModel> allTheModels, String theFile, IProject theSelectedFileParentIProject) throws Exception {
		File dir = new File(theSelectedFileParentIProject.getLocation().toOSString() + File.separator + "transformations");
		FilenameFilter filter = new FilenameFilter() {
	         public boolean accept (File dir, String name) { 
	        	 return name.equals(theFile);
	         } 
	    }; 
	    String[] children = dir.list(filter);
	    if (children == null) {
	    	System.out.println("Either dir does not exist or is not a directory"); 
	    } else if (children.length > 0) {
	    	EtlModule etlModule = new EtlModule();
	    	for (IModel theModel : allTheModels) {
	    		etlModule.getContext().getModelRepository().addModel(theModel);
	  		}
	  		File etlFile = new File(theSelectedFileParentIProject.getLocation().toOSString() + File.separator + "transformations" + File.separator + children[0]);
	  		etlModule.parse(etlFile);
	  		etlModule.execute();
	  		etlModule.getContext().getModelRepository().dispose();
	  	}
	}

	public void refresh(IProject parentProject) throws CoreException {
		parentProject.refreshLocal(1, null);
	}

}