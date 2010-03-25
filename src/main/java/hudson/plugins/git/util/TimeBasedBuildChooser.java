package hudson.plugins.git.util;


import hudson.model.Action;
import hudson.model.Result;
import hudson.plugins.git.*;
import org.joda.time.DateTime;
import org.spearce.jgit.lib.Commit;
import org.spearce.jgit.lib.ObjectId;

import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.logging.Logger;

public class TimeBasedBuildChooser implements IBuildChooser {

    private final String separator = "#";
    private final IGitAPI               git;
    private final GitUtils              utils;
    private final GitSCM                gitSCM;

    //-------- Data -----------
    private final BuildData             data;

    public TimeBasedBuildChooser(GitSCM gitSCM, IGitAPI git, GitUtils utils, BuildData data)
    {
        this.gitSCM = gitSCM;
        this.git = git;
        this.utils = utils;
        this.data = data == null ? new BuildData() : data;
    }

    /**
     * Determines which Revisions to build.
     *
     * If only one branch is chosen and only one repository is listed, then
     * just attempt to find the latest revision number for the chosen branch.
     *
     * If multiple branches are selected or the branches include wildcards, then
     * use the advanced usecase as defined in the getAdvancedCandidateRevisons
     * method.
     *
     * @throws IOException
     * @throws GitException
     */
    public Collection<Revision> getCandidateRevisions(boolean isPollCall, String singleBranch)
            throws GitException, IOException {
      
        Revision last = data.getLastBuiltRevision();
        String result = git.getAllLogEntries();
        Collection<TimedCommit> commits = sortRevList(result);
        Iterator<TimedCommit> i = commits.iterator();
        ArrayList<Revision> revs = new ArrayList<Revision>();
        DateTime lastBuilt = null;

        while(i.hasNext()) {
            TimedCommit tc = i.next();
            //When encountered last build, break
            if(last != null && tc.commit.name().equals(last.getSha1String())) {
                lastBuilt = tc.when;
                break;
            }
            revs.add(new Revision(tc.commit));
        }
        //check if there's commits on same second which hasn't been built yet
        while(i.hasNext()) {
            TimedCommit tc = i.next();
            if(tc.when.isEqual(lastBuilt)) {
                if(!data.hasBeenBuilt(tc.commit)) {
                    revs.add(new Revision(tc.commit));
                }
            } else {
                //was older, so it is already built (also rest of them are built)
                break;
            }
        }

        if(last == null) {
            return revs;
        }
        if(revs.size() == 0 && !isPollCall) {
            return Collections.singletonList(last);
        }
        //reverse order
        ArrayList<Revision> finalRevs = new ArrayList<Revision>();
        for(int j = revs.size() - 1 ; j >= 0 ; j--) {
            finalRevs.add(revs.get(j));

        }
        return finalRevs;

    }

    private Collection<TimedCommit> sortRevList(String logOutput) {
        SortedSet<TimedCommit> timedCommits = new TreeSet<TimedCommit>();
        String[] lines = logOutput.split("\n");
        for (String s : lines ) {
            timedCommits.add(parseCommit(s));
        }
        return timedCommits;
    }

    private TimedCommit parseCommit(String line) {

        String[] lines = line.split(separator);
        /*Line has ' in the beginning and in the end */
        String id = lines[0].substring(1);
        String date = lines[1].substring(0, lines[1].length() - 1 );
        //From seconds to milliseconds
        return new TimedCommit(ObjectId.fromString(id),
                new DateTime(Long.parseLong(date) * 1000));
    }
    
    private class TimedCommit implements Comparable<TimedCommit> {

        private ObjectId commit;
        public DateTime when;

        public TimedCommit(ObjectId c, DateTime when) {
            this.commit = c;
            this.when = when;
        }

        public ObjectId getCommit() {
            return commit;
        }

        public int compareTo(TimedCommit o) {
            //I want newest to be first
            return -(when.compareTo(o.when));
        }
     }

    public Build revisionBuilt(Revision revision, int buildNumber, Result result )
    {
        Build build = new Build(revision, buildNumber, result);
        data.saveBuild(build);
        return build;
    }


    public Action getData()
    {
        return data;
    }

}
