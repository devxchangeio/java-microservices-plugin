/**
 * 
 */
package io.oneclicklabs.microservices.bootapp;

import java.io.File;
import java.net.URI;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

/**
 * @author karthy
 *
 */
public class BootAppProjectNewWizard extends Wizard implements INewWizard, IExecutableExtension {

	private static final String PAGE_NAME = "Selenium+ Project Wizard";

	private static final String WIZARD_NAME = "New Selenium+ Project";

	private static final String WIZARD_DESCRIPTION = "Create new Selenium+ Project with all assets.";

	private BootAppProjectNewWizardPage _pageOne;

	private IConfigurationElement _configurationElement;

	public static String PROJECT_NAME = "SAMPLE";

	@Override
	public void addPages() {
		super.addPages();

		_pageOne = new BootAppProjectNewWizardPage(PAGE_NAME);
		_pageOne.setTitle(WIZARD_NAME);
		_pageOne.setDescription(WIZARD_DESCRIPTION);
		addPage(_pageOne);
	}

	public BootAppProjectNewWizard() {
		setWindowTitle(WIZARD_NAME);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		String selenv = System.getenv(BaseProject.SELENIUM_PLUS_ENV);
		if (selenv != null) {

			File projectdir = new File(selenv);

			if (projectdir.exists()) {
				BaseProject.SELENIUM_PLUS = selenv;
				BaseProject.STAFDIR = System.getenv(BaseProject.STAFDIR_ENV);
				return;
			}
		}

		MessageDialog.openError(getShell(), BaseProject.MSG_INSTALL_NOT_FOUND, BaseProject.MSG_INSTALL_AND_RESTART);
	}

	@Override
	public boolean performFinish() {

		String name = _pageOne.getProjectName().toUpperCase();
		URI location = null;
		if (!_pageOne.useDefaults()) {
			location = _pageOne.getLocationURI();

		} // else location == null

		BaseProject.createProject(name, location, "sas", BaseProject.PROJECTTYPE_SELENIUM);

		BasicNewProjectResourceWizard.updatePerspective(_configurationElement);

		return true;

	}

	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
			throws CoreException {
		_configurationElement = config;
	}

}
