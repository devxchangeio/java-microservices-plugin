package io.oneclicklabs.microservices.plugin.wizards;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.wst.common.componentcore.ArtifactEdit;

/**
 * 
 * @author karthy
 * 
 */
public class SwaggerConfigWizard extends Wizard implements INewWizard {
	private SwaggerConfigWizardPage page;
	private ISelection selection;

	public static String containerName = null;
	public static String packageName = null;
	public static String controllerPath = null;
	public static String fileName = null;

	public SwaggerConfigWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	public void loadProperties() {
		Properties props = new Properties();
		InputStream is = null;
		try {
			is = getClass()
					.getResourceAsStream("/properties/wizard.properties");
			props.load(is);
		} catch (IOException e) {
			try {
				if (is != null)
					is.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		} finally {
			try {
				if (is != null)
					is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	/**
	 * Adding the page to the wizard.
	 */

	public void addPages() {
		page = new SwaggerConfigWizardPage(selection);
		addPage(page);
	}

	/**
	 * This method is called when 'Finish' button is pressed in the wizard. We
	 * will create an operation and run it using wizard as execution context.
	 */
	public boolean performFinish() {
		containerName = page.getContainerName();
		packageName = page.getPackageName();
		fileName = page.getClassName();
		controllerPath=page.getControllerPath();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException {
				try {
					doFinish(containerName, fileName, packageName, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error",
					realException.getMessage());
			return false;
		}
		return true;
	}

	/**
	 * The worker method. It will find the container, create the file if missing
	 * or just replace its contents, and open the editor on the newly created
	 * file.
	 * 
	 * @throws Exception
	 */

	@SuppressWarnings("unused")
	private void doFinish(String containerName, String fileName,
			String packageName, IProgressMonitor monitor) throws Exception {
		// create a sample file
		monitor.beginTask("Creating " + fileName, 2);
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource resource = root.findMember(new Path(containerName));
		if (!resource.exists() || !(resource instanceof IContainer)) {
			throwCoreException("Container \"" + containerName
					+ "\" does not exist.");
		}

		IContainer container = (IContainer) resource;
		IJavaProject project = (IJavaProject) container.getProject().getNature(
				"org.eclipse.jdt.core.javanature");
		IPackageFragment ipackage = project.getAllPackageFragmentRoots()[0]
				.createPackageFragment(packageName, false, monitor);
		String contents = getSwaggerConfigClass(containerName);
		final ICompilationUnit cu = ipackage.createCompilationUnit(fileName
				+ ".java", contents, false, monitor);
		
		monitor.worked(1);
	}

	private void throwCoreException(String message) throws CoreException {
		IStatus status = new Status(IStatus.ERROR, "microservices-plugin", IStatus.OK,
				message, null);
		throw new CoreException(status);
	}

	/**
	 * We will accept the selection in the workbench to see if we can initialize
	 * from it.
	 * 
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}

	private String getSwaggerConfigClass(String containerName) throws Exception {
		StringBuffer sb = new StringBuffer();
		BufferedReader reader;
		InputStream input = this.getClass().getResourceAsStream(
				"/templates/swaggerconfig.template");
		reader = new BufferedReader(new InputStreamReader(input));
		String line = null;
		while ((line = reader.readLine()) != null) {
			System.out.println(line);
			line = line.replaceAll("#classname", fileName);
			line = line.replaceAll("#package", packageName);
			line = line.replaceAll("#controllerpath", controllerPath);
			boolean skip = false;
			line = line.replaceAll("#projectname", containerName);
			if (!skip) {
				sb.append(line);
				sb.append("\n");
			}

		}
		reader.close();
		reader.close();
		return sb.toString();

	}
	
	public void saveEdit(ArtifactEdit edit) {
		edit.saveIfNecessary(null);
		edit.dispose();
	}

}