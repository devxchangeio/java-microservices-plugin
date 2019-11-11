/**
 * 
 */
package io.oneclicklabs.microservices.bootapp;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

/**
 * @author karthy
 *
 */
public class BootAppProjectNature implements IProjectNature {

    // ID of the natures, which consists of Bundle-SymbolicName + ID
    public static final String NATURE_ID = "io.oneclicklabs.microservices.bootapp";

    private IProject project;

    @Override
    public void configure() throws CoreException {
            // only called once the nature has been set

            // configure the project...
    }

    @Override
    public void deconfigure() throws CoreException {
            // only called once the nature has been set

            // reset the project configuration...
    }

    @Override
    public IProject getProject() {
            return project;
    }

    @Override
    public void setProject(IProject project) {
            this.project = project;
    }

}
