package hudson.plugins.git.util;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Project;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.IGitAPI;

import java.io.Serializable;

/**
 * Abstract base class for BuildChooser. BuildChooser will select what to build and when.
 */
public abstract class ExtensionBuildChooser implements IBuildChooser, ExtensionPoint, Serializable {

    String name = null;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static ExtensionList<ExtensionBuildChooser> all() {
        return Hudson.getInstance().getExtensionList(ExtensionBuildChooser.class);
    }

    public abstract void setUtilities(GitSCM gitSCM, IGitAPI git, GitUtils gitUtils, BuildData data);

    /**
     * Is this build chooser required to be used. Useful when some other
     * functionality depends on using this specific build chooser. If there's two
     * or more build choosers which are required, original build chooser is used.
     * Defaults to false.
     *  @param p Project which is being built.
     *  @return
     */
    public boolean isRequiredFor(AbstractProject p) {
        return false;
    }

}
