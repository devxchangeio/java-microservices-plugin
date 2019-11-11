package io.oneclicklabs.microservices.plugin.wizards;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
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
public class SoapWizard extends Wizard implements INewWizard {
	private SoapWizardPage page;
	private ISelection selection;

	public static String containerName = null;
	public static String packageName = null;
	public static String fileName = null;
	public static String nameSpace = null;


	public SoapWizard() {
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
		page = new SoapWizardPage(selection);
		addPage(page);
	}

	public boolean performFinish() {
		containerName = page.getContainerName();
		packageName = page.getPackageName();
		fileName = page.getClassName();
		nameSpace = page.getNameSpaceName();
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
		String contents = getEnpointClass(containerName);
		final ICompilationUnit cu = ipackage.createCompilationUnit(fileName + ".java", contents, false, monitor);

		String webserviceconfig = getWebserviceconfigClass(containerName);
		final ICompilationUnit cu1 = ipackage.createCompilationUnit("WebServiceConfig.java", webserviceconfig, false,
				monitor);

		String saoprequest = getSoapRequestClass(containerName);
		final ICompilationUnit cu2 = ipackage.createCompilationUnit("GetSoapRequest.java", saoprequest, false,
				monitor);

		String soapresponse = getSoapResponseClass(containerName);
		final ICompilationUnit cu3 = ipackage.createCompilationUnit("GetSoapResponse.java", soapresponse, false,
				monitor);

		IFile file = container.getFile(new Path("/src/main/resources/sample.xsd"));

		InputStream stream = createFileAsStream(containerName);
		if (file.exists()) {
			file.setContents(stream, true, true, monitor);
		} else {
			file.create(stream, true, monitor);
		}
		stream.close();


		monitor.worked(1);
	}

	private void throwCoreException(String message) throws CoreException {
		IStatus status = new Status(IStatus.ERROR, "microservices-plugin", IStatus.OK, message, null);
		throw new CoreException(status);
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}

	private String getEnpointClass(String containerName) throws Exception {
		StringBuffer sb = new StringBuffer();
		BufferedReader reader;
		InputStream input = this.getClass().getResourceAsStream("/templates/wsendpoint.template");
		reader = new BufferedReader(new InputStreamReader(input));
		String line = null;
		while ((line = reader.readLine()) != null) {
			System.out.println(line);
			line = line.replaceAll("#classname", fileName);
			line = line.replaceAll("#package", packageName);
			line = line.replaceAll("#namespace_uri", nameSpace);
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

	private String getWebserviceconfigClass(String containerName) throws Exception {
		StringBuffer sb = new StringBuffer();
		BufferedReader reader;
		InputStream input = this.getClass().getResourceAsStream("/templates/webserviceconfig.template");
		reader = new BufferedReader(new InputStreamReader(input));
		String line = null;
		while ((line = reader.readLine()) != null) {
			System.out.println(line);
			line = line.replaceAll("#classname", fileName);
			line = line.replaceAll("#package", packageName);
			line = line.replaceAll("#namespace_uri", nameSpace);

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

	private String getSoapRequestClass(String containerName) throws Exception {
		StringBuffer sb = new StringBuffer();
		BufferedReader reader;
		InputStream input = this.getClass().getResourceAsStream("/templates/getsoaprequest.template");
		reader = new BufferedReader(new InputStreamReader(input));
		String line = null;
		while ((line = reader.readLine()) != null) {
			System.out.println(line);
			line = line.replaceAll("#classname", fileName);
			line = line.replaceAll("#package", packageName);
			line = line.replaceAll("#namespace_uri", nameSpace);

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

	private String getSoapResponseClass(String containerName) throws Exception {
		StringBuffer sb = new StringBuffer();
		BufferedReader reader;
		InputStream input = this.getClass().getResourceAsStream("/templates/getsoapresponse.template");
		reader = new BufferedReader(new InputStreamReader(input));
		String line = null;
		while ((line = reader.readLine()) != null) {
			System.out.println(line);
			line = line.replaceAll("#classname", fileName);
			line = line.replaceAll("#package", packageName);
			line = line.replaceAll("#namespace_uri", nameSpace);

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

	private String getSimpleXsd(String containerName) throws Exception {
		StringBuffer sb = new StringBuffer();
		BufferedReader reader;
		InputStream input = this.getClass().getResourceAsStream("/templates/sample-xsd.template");
		reader = new BufferedReader(new InputStreamReader(input));
		String line = null;
		while ((line = reader.readLine()) != null) {
			System.out.println(line);
			line = line.replaceAll("#classname", fileName);
			line = line.replaceAll("#package", packageName);
			line = line.replaceAll("#namespace_uri", nameSpace);

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
	
	private InputStream createFileAsStream(String containerName)
			throws Exception {
		String contents = getSimpleXsd(containerName);
		return new ByteArrayInputStream(contents.getBytes());
	}

}