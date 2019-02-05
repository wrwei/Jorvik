package profile.generation.popup.actions;
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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.epsilon.common.dt.util.LogUtil;
import org.eclipse.epsilon.emc.emf.CachedResourceSet;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import profile.generation.util.UtilityMethods;

public class CreatePapyrusProjectAction implements IObjectActionDelegate {

	//the shell
	private Shell shell;
	
	//the selected file path
	private String theSelectedFilePath;
	
	//the selected file parent folder, the destination project folder
	private String theSelectedFileParentFolder, theDestinationProjectFolder;
	
	//the selected file parent project
	private IProject theSelectedFileParentIProject;
	
	
	/**
	 * Constructor for Action1.
	 */
	public CreatePapyrusProjectAction() {
		super();
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		//get current shell
		shell = targetPart.getSite().getShell();
		//get selected file
		IStructuredSelection theSelectedFile = (IStructuredSelection) targetPart.getSite().getWorkbenchWindow().getSelectionService().getSelection();
		//get first element
        Object firstElement = theSelectedFile.getFirstElement();
        //get file
        IFile file = (IFile) Platform.getAdapterManager().getAdapter(firstElement,IFile.class);
        //populate strings and parent iProject
        theSelectedFileParentFolder = file.getParent().getLocation().toOSString();
        theDestinationProjectFolder = file.getProject().getLocation().toOSString();
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
					SubMonitor subMonitor = SubMonitor.convert(monitor, 140);
					try {
						//generate plugin.xml
						subMonitor.setTaskName("Generating the Plugin XML.");
						tahh.createThePluginXml(theSelectedFilePath, theDestinationProjectFolder);
						subMonitor.split(10);
						
						//generate the UML profile
						subMonitor.setTaskName("Generating the UML Profile.");
						tahh.createTheProfileUmlFile(theSelectedFilePath, theDestinationProjectFolder, theSelectedFileParentIProject);
						subMonitor.split(30);
						
						//generate UML Notation
						subMonitor.setTaskName("Generating Profile related files.");
						tahh.createTheModelProfileNotationFile(theDestinationProjectFolder);
						subMonitor.split(10);
						
						//generate UML DI
						subMonitor.setTaskName("Generating Profile related files.");
						tahh.createTheModelProfileDiFile(theDestinationProjectFolder);
						subMonitor.split(10);
						
						//done element types configuration
						subMonitor.setTaskName("Generating the Element Type Configuration.");
						tahh.createTheElementTypeConfigurations(theSelectedFilePath, theDestinationProjectFolder, theSelectedFileParentIProject);
						subMonitor.split(10);
						
						//generate the palette configuration
						subMonitor.setTaskName("Generating the Palette Configuration.");
						tahh.createThePaletteConfiguration(theSelectedFilePath, theDestinationProjectFolder, theSelectedFileParentIProject);
						subMonitor.split(10);

						//create the creation command
						subMonitor.setTaskName("Creating Creation Command.");
						tahh.createCreationCommand(theSelectedFilePath, theDestinationProjectFolder, theSelectedFileParentIProject);
						subMonitor.split(10);
						
						//done
						subMonitor.setTaskName("Generating the CSS.");
						tahh.createTheCSSFile(theSelectedFilePath, theDestinationProjectFolder, theSelectedFileParentIProject);
						subMonitor.split(10);

						//done
						subMonitor.setTaskName("Generating the Architecture Model.");
						tahh.createTheArchitectureModel(theSelectedFilePath, theDestinationProjectFolder, theSelectedFileParentIProject);
						subMonitor.split(10);

						//
						subMonitor.setTaskName("Generating the UML 2 EMF ETL file.");
						tahh.createTheUml2EmfETLFile(theSelectedFilePath, theDestinationProjectFolder, theSelectedFileParentIProject);
						subMonitor.split(10);
						
						//done
						subMonitor.setTaskName("Generating Build Properties.");
						tahh.createThebuildPropertiesFile(theDestinationProjectFolder);
						subMonitor.split(10);
						
						//tahh.copyTheIcons(theSelectedFilePath, theParentFolder);
						//tahh.copyTheShapes(theSelectedFilePath, theParentFolder);
						tahh.refresh(theSelectedFileParentIProject);	
					} catch (Exception ex) {
						LogUtil.log(ex);
						PlatformUI.getWorkbench().getDisplay()
								.syncExec(new Runnable() {
									public void run() {
										MessageDialog
												.openError(shell, "Error",
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
			//job1.setPriority(Job.SHORT);
			//job1.schedule();
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
