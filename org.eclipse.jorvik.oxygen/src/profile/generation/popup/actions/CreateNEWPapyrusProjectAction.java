package profile.generation.popup.actions;

import java.util.ArrayList;

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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.epsilon.common.dt.util.LogUtil;
import org.eclipse.epsilon.emc.emf.CachedResourceSet;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ClasspathComputer;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.wizards.tools.UpdateBuildpathWizard;
import org.eclipse.pde.internal.ui.wizards.tools.UpdateClasspathJob;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import profile.generation.util.UtilityMethods;

public class CreateNEWPapyrusProjectAction implements IObjectActionDelegate {

	//the shell
	private Shell shell;
	
	//the selected file path
	private String theSelectedFilePath;
	
	//the selected file parent folder, the destination project folder
	private String theSelectedFileParentFolder, theDestinationIProjectFolder = null;
	
	//the selected file parent project
	private IProject theSelectedFileParentIProject, theDestinationIProject;

	/**
	 * Constructor for Action1.
	 */
	public CreateNEWPapyrusProjectAction() {
		super();
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		//get current shell
		shell = targetPart.getSite().getShell();
		
		//get current selection
		IStructuredSelection theSelectedFile = (IStructuredSelection) targetPart.getSite().getWorkbenchWindow()
				.getSelectionService().getSelection();
		
		//get first object
		Object firstElement = theSelectedFile.getFirstElement();
		IFile file = (IFile) Platform.getAdapterManager().getAdapter(firstElement, IFile.class);
		
		//populate folder strings and parent IProject
		theSelectedFileParentFolder = file.getParent().getLocation().toOSString();
		theSelectedFilePath = file.getLocation().toOSString();
		theSelectedFileParentIProject = file.getProject();
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {

		UtilityMethods tahh = new UtilityMethods(theSelectedFilePath);

		try {
			 IRunnableWithProgress op = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					SubMonitor subMonitor = SubMonitor.convert(monitor, 200);
					try {
						
						//create the project, with an src folder
						theDestinationIProject = tahh.createPluginProject();
						theDestinationIProjectFolder = theDestinationIProject.getLocation().toOSString();
						
						//done
						subMonitor.setTaskName("Generating the Project Manifest.");
						tahh.createTheManifestFile(theSelectedFilePath, theDestinationIProjectFolder);
						subMonitor.split(10);
						
						//generate plugin.xml
						subMonitor.setTaskName("Generating the Plugin XML.");
						tahh.createThePluginXml(theSelectedFilePath, theDestinationIProjectFolder);
						subMonitor.split(10);
						
						//generate the UML profile
						subMonitor.setTaskName("Generating the UML Profile.");
						tahh.createTheProfileUmlFile(theSelectedFilePath, theDestinationIProjectFolder, theSelectedFileParentIProject);
						subMonitor.split(30);
						
						//generate UML Notation
						subMonitor.setTaskName("Generating Profile related files.");
						tahh.createTheModelProfileNotationFile(theDestinationIProjectFolder);
						subMonitor.split(10);
						
						//generate UML DI
						subMonitor.setTaskName("Generating Profile related files.");
						tahh.createTheModelProfileDiFile(theDestinationIProjectFolder);
						subMonitor.split(10);
						
						//done element types configuration
						subMonitor.setTaskName("Generating the Element Type Configuration.");
						tahh.createTheElementTypeConfigurations(theSelectedFilePath, theDestinationIProjectFolder, theSelectedFileParentIProject);
						subMonitor.split(10);
						
						//generate the palette configuration
						subMonitor.setTaskName("Generating the Palette Configuration.");
						tahh.createThePaletteConfiguration(theSelectedFilePath, theDestinationIProjectFolder, theSelectedFileParentIProject);
						subMonitor.split(10);
						
						//create the creation command
						subMonitor.setTaskName("Creating Creation Command.");
						tahh.createCreationCommand(theSelectedFilePath, theDestinationIProjectFolder, theSelectedFileParentIProject);
						subMonitor.split(10);
						
						//done
						subMonitor.setTaskName("Generating the CSS.");
						tahh.createTheCSSFile(theSelectedFilePath, theDestinationIProjectFolder, theSelectedFileParentIProject);
						subMonitor.split(10);
						
						//done
						subMonitor.setTaskName("Generating the Architecture Model.");
						tahh.createTheArchitectureModel(theSelectedFilePath, theDestinationIProjectFolder, theSelectedFileParentIProject);
						subMonitor.split(10);
						
						//
						subMonitor.setTaskName("Generating the UML 2 EMF ETL file.");
						tahh.createTheUml2EmfETLFile(theSelectedFilePath, theDestinationIProjectFolder, theSelectedFileParentIProject);
						subMonitor.split(10);
						
						//done
						subMonitor.setTaskName("Generating Build Properties.");
						tahh.createThebuildPropertiesFile(theDestinationIProjectFolder);
						subMonitor.split(10);
						
						//done
						subMonitor.setTaskName("Copying Icons.");
						tahh.copyTheIcons(theSelectedFilePath, theSelectedFileParentIProject.getLocation().toOSString(),
								theDestinationIProjectFolder);
						subMonitor.split(30);
						
						subMonitor.setTaskName("Copying Shapes.");
						tahh.copyTheShapes(theSelectedFilePath,
								theSelectedFileParentIProject.getLocation().toOSString(), theDestinationIProjectFolder);
						subMonitor.setWorkRemaining(30);
						subMonitor.split(30);
						
						tahh.refresh(theDestinationIProject);
						
						
						IPluginModelBase model = PluginRegistry.findModel(theDestinationIProject);
						final IPluginModelBase[] modelArray = {model};

						UpdateClasspathJob j = new UpdateClasspathJob(modelArray);
						j.doUpdateClasspath(monitor, modelArray);


					} catch (Exception ex) {
						LogUtil.log(ex);
						PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
							public void run() {
								MessageDialog.openError(shell, "Error",
										"An error has occured. Please see the Error Log.");
							}

						});
					} finally {
						CachedResourceSet.getCache().clear();
					}
					//return Status.OK_STATUS;
				}
			};
			 new ProgressMonitorDialog(shell).run(true, true, op);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

}
