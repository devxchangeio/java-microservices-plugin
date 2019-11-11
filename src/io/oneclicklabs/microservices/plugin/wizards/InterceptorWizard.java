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
import org.eclipse.wst.common.componentcore.ArtifactEdit;

/**
 * 
 * @author karthy
 * 
 */
public class InterceptorWizard extends Wizard implements INewWizard {
	private InterceptorWizardPage page;
	private ISelection selection;

	public static String containerName = null;
	public static String packageName = null;
	public static String fileName = null;

	public InterceptorWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	public void loadProperties() {
		Properties props = new Properties();
		InputStream is = null;
		try {
			is = getClass().getResourceAsStream("/properties/wizard.properties");
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

	public void addPages() {
		page = new InterceptorWizardPage(selection);
		addPage(page);
	}

	public boolean performFinish() {
		containerName = page.getContainerName();
		packageName = page.getPackageName();
		fileName = page.getClassName();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
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
			MessageDialog.openError(getShell(), "Error", realException.getMessage());
			return false;
		}
		return true;
	}

	@SuppressWarnings("unused")
	private void doFinish(String containerName, String fileName, String packageName, IProgressMonitor monitor)
			throws Exception {
		monitor.beginTask("Creating " + fileName, 2);
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource resource = root.findMember(new Path(containerName));
		if (!resource.exists() || !(resource instanceof IContainer)) {
			throwCoreException("Container \"" + containerName + "\" does not exist.");
		}

		IContainer container = (IContainer) resource;
		IJavaProject project = (IJavaProject) container.getProject().getNature("org.eclipse.jdt.core.javanature");
		IPackageFragment ipackage = project.getAllPackageFragmentRoots()[0].createPackageFragment(packageName, false,
				monitor);
		String contents = getInterceptorClass(containerName);
		final ICompilationUnit cu = ipackage.createCompilationUnit(fileName + ".java", contents, false, monitor);

		String appConfig = getAppConfigClass(containerName);
		final ICompilationUnit cu1 = ipackage.createCompilationUnit("AppConfig.java", appConfig, false, monitor);

		monitor.worked(1);
	}

	private void throwCoreException(String message) throws CoreException {
		IStatus status = new Status(IStatus.ERROR, "microservices-plugin", IStatus.OK, message, null);
		throw new CoreException(status);
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}

	private String getInterceptorClass(String containerName) throws Exception {
		StringBuffer sb = new StringBuffer();
		BufferedReader reader;
		InputStream input = this.getClass().getResourceAsStream("/templates/interceptor.template");
		reader = new BufferedReader(new InputStreamReader(input));
		String line = null;
		while ((line = reader.readLine()) != null) {
			System.out.println(line);
			line = line.replaceAll("#classname", fileName);
			line = line.replaceAll("#package", packageName);
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

	private String getAppConfigClass(String containerName) throws Exception {
		StringBuffer sb = new StringBuffer();
		BufferedReader reader;
		InputStream input = this.getClass().getResourceAsStream("/templates/appconfig.template");
		reader = new BufferedReader(new InputStreamReader(input));
		String line = null;
		while ((line = reader.readLine()) != null) {
			System.out.println(line);
			line = line.replaceAll("#classname", fileName);
			line = line.replaceAll("#package", packageName);
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