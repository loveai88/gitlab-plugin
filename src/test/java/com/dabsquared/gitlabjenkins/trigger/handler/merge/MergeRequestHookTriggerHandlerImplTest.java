package com.dabsquared.gitlabjenkins.trigger.handler.merge;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.Action;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.MergeRequestObjectAttributesBuilder;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterFactory;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterType;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.plugins.git.GitSCM;
import hudson.util.OneShotEvent;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.concurrent.ExecutionException;

import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.CommitBuilder.commit;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.MergeRequestHookBuilder.mergeRequestHook;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.MergeRequestObjectAttributesBuilder.mergeRequestObjectAttributes;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.ProjectBuilder.project;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.UserBuilder.user;
import static com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterConfig.BranchFilterConfigBuilder.branchFilterConfig;
import static com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilterFactory.newMergeRequestLabelFilter;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Robin Müller
 */
public class MergeRequestHookTriggerHandlerImplTest {

    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void mergeRequest_ciSkip() throws IOException, InterruptedException {
        assertThat(ciSkipTestHelper("enable build","enable build"), is(true));
        assertThat(ciSkipTestHelper("garbage [ci-skip] garbage","enable build"), is(false));
        assertThat(ciSkipTestHelper("enable build","garbage [ci-skip] garbage"), is(false));
    }

    @Test
    public void mergeRequest_build_when_opened() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = new MergeRequestHookTriggerHandlerImpl(Arrays.asList(State.opened, State.reopened), EnumSet.noneOf(Action.class), false, false);
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.opened);

        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void mergeRequest_build_when_reopened() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = new MergeRequestHookTriggerHandlerImpl(Arrays.asList(State.opened, State.reopened), EnumSet.noneOf(Action.class), false, false);
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.reopened);

        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void mergeRequest_build_when_opened_with_approved_action_enabled() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = new MergeRequestHookTriggerHandlerImpl(Arrays.asList(State.opened, State.reopened), Arrays.asList(Action.approved), false, false);
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.opened);

        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void mergeRequest_build_when_accepted() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = new MergeRequestHookTriggerHandlerImpl(Arrays.asList(State.merged), EnumSet.noneOf(Action.class), false, false);
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.merged);

        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void mergeRequest_build_when_accepted_with_approved_action_enabled() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = new MergeRequestHookTriggerHandlerImpl(Arrays.asList(State.merged), Arrays.asList(Action.approved), false, false);
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.merged);

        assertThat(buildTriggered.isSignaled(), is(true));
    }


    @Test
    public void mergeRequest_build_when_closed() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = new MergeRequestHookTriggerHandlerImpl(Arrays.asList(State.closed), EnumSet.noneOf(Action.class), false, false);
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.closed);

        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void mergeRequest_build_when_closed_with_actions_enabled() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = new MergeRequestHookTriggerHandlerImpl(Arrays.asList(State.closed), Arrays.asList(Action.approved), false, false);
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.closed);

        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void mergeRequest_do_not_build_for_accepted_when_nothing_enabled() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        do_not_build_for_state_when_nothing_enabled(State.merged);
    }

    @Test
    public void mergeRequest_do_not_build_for_updated_when_nothing_enabled() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        do_not_build_for_state_when_nothing_enabled(State.updated);
    }

    @Test
    public void mergeRequest_do_not_build_for_reopened_when_nothing_enabled() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        do_not_build_for_state_when_nothing_enabled(State.reopened);
    }

    @Test
    public void mergeRequest_do_not_build_for_opened_when_nothing_enabled() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        do_not_build_for_state_when_nothing_enabled(State.opened);
    }

    @Test
    public void mergeRequest_do_not_build_when_accepted_some_enabled() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = new MergeRequestHookTriggerHandlerImpl(Arrays.asList(State.opened, State.updated), Arrays.asList(Action.approved), false, false);
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.merged);

        assertThat(buildTriggered.isSignaled(), is(false));
    }

    @Test
    public void mergeRequest_build_for_accepted_state_when_approved_action_triggered() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = new MergeRequestHookTriggerHandlerImpl(Arrays.asList(State.opened, State.updated), Arrays.asList(Action.approved), false, false);
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.merged, Action.approved);

        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void mergeRequest_do_not_build_when_closed() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = new MergeRequestHookTriggerHandlerImpl(Arrays.asList(State.opened, State.updated), Arrays.asList(Action.approved), false, false);
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.closed);

        assertThat(buildTriggered.isSignaled(), is(false));
    }

    @Test
    public void mergeRequest_do_not_build_for_updated_state_and_approved_action_when_both_not_enabled() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = new MergeRequestHookTriggerHandlerImpl(Arrays.asList(State.opened), EnumSet.noneOf(Action.class), false, false);
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.updated, Action.approved);

        assertThat(buildTriggered.isSignaled(), is(false));
    }

    @Test
    public void mergeRequest_do_not_build_for_updated_state_and_approved_action_when_updated_enabled_but_approved_not() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = new MergeRequestHookTriggerHandlerImpl(Arrays.asList(State.updated), EnumSet.noneOf(Action.class), false, false);
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.updated, Action.approved);

        assertThat(buildTriggered.isSignaled(), is(false));
    }

    @Test
    public void mergeRequest_build_for_update_state_when_updated_state_and_approved_action_enabled() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = new MergeRequestHookTriggerHandlerImpl(Arrays.asList(State.opened, State.updated), Arrays.asList(Action.approved), false, false);
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.updated, Action.approved);

        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void mergeRequest_build_for_update_state_and_action_when_updated_state_and_approved_action_enabled() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = new MergeRequestHookTriggerHandlerImpl(Arrays.asList(State.opened, State.updated), Arrays.asList(Action.approved), false, false);
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.updated, Action.update);

        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void mergeRequest_do_not_build_for_update_state_and_action_when_opened_state_and_approved_action_enabled() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = new MergeRequestHookTriggerHandlerImpl(Arrays.asList(State.opened), Arrays.asList(Action.approved), false, false);
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.updated, Action.update);

        assertThat(buildTriggered.isSignaled(), is(false));
    }

    @Test
    public void mergeRequest_build_for_update_state_when_updated_state_and_merge_action() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = new MergeRequestHookTriggerHandlerImpl(Arrays.asList(State.opened, State.updated), Arrays.asList(Action.approved), false, false);
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.updated, Action.merge);

        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void mergeRequest_build_for_approved_action_when_opened_state_and_approved_action_enabled() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = new MergeRequestHookTriggerHandlerImpl(Arrays.asList(State.opened), Arrays.asList(Action.approved), false, false);
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.updated, Action.approved);

        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void mergeRequest_build_for_approved_action_when_only_approved_enabled() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = new MergeRequestHookTriggerHandlerImpl(EnumSet.noneOf(State.class), EnumSet.of(Action.approved), false, false);
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.updated, Action.approved);

        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void mergeRequest_build_only_when_approved_and_not_when_updated() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        mergeRequest_build_only_when_approved(Action.update);
    }

    @Test
    public void mergeRequest_build_only_when_approved_and_not_when_opened() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        mergeRequest_build_only_when_approved(Action.open);
    }

    @Test
    public void mergeRequest_build_only_when_approved_and_not_when_merge() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        mergeRequest_build_only_when_approved(Action.merge);
    }

    private void do_not_build_for_state_when_nothing_enabled(State state) throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = new MergeRequestHookTriggerHandlerImpl(EnumSet.noneOf(State.class), EnumSet.noneOf(Action.class), false, false);
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, state);

        assertThat(buildTriggered.isSignaled(), is(false));
    }

	private void mergeRequest_build_only_when_approved(Action action)
			throws GitAPIException, IOException, InterruptedException {
		MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = new MergeRequestHookTriggerHandlerImpl(EnumSet.noneOf(State.class), EnumSet.of(Action.approved), false, false);
	    OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, action);

	    assertThat(buildTriggered.isSignaled(), is(false));
	}

    private OneShotEvent doHandle(MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler, Action action) throws GitAPIException, IOException, InterruptedException {
        return doHandle(mergeRequestHookTriggerHandler, defaultMergeRequestObjectAttributes().withAction(action));
    }

    private OneShotEvent doHandle(MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler, State state) throws GitAPIException, IOException, InterruptedException {
        return doHandle(mergeRequestHookTriggerHandler, defaultMergeRequestObjectAttributes().withState(state));
    }

    private OneShotEvent doHandle(MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler, State state, Action action) throws GitAPIException, IOException, InterruptedException {
        return doHandle(mergeRequestHookTriggerHandler, defaultMergeRequestObjectAttributes().withState(state).withAction(action));
    }

	private OneShotEvent doHandle(MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler,
			MergeRequestObjectAttributesBuilder objectAttributes) throws GitAPIException, IOException, NoHeadException,
			NoMessageException, UnmergedPathsException, ConcurrentRefUpdateException, WrongRepositoryStateException,
			AmbiguousObjectException, IncorrectObjectTypeException, MissingObjectException, InterruptedException {
		Git.init().setDirectory(tmp.getRoot()).call();
        tmp.newFile("test");
        Git git = Git.open(tmp.getRoot());
        git.add().addFilepattern("test");
        RevCommit commit = git.commit().setMessage("test").call();
        ObjectId head = git.getRepository().resolve(Constants.HEAD);
        String repositoryUrl = tmp.getRoot().toURI().toString();

        final OneShotEvent buildTriggered = new OneShotEvent();
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.setScm(new GitSCM(repositoryUrl));
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                buildTriggered.signal();
                return true;
            }
        });
        project.setQuietPeriod(0);
		mergeRequestHookTriggerHandler.handle(project, mergeRequestHook()
                .withObjectAttributes(objectAttributes
            		    .withTargetBranch("refs/heads/" + git.nameRev().add(head).call().get(head))
            		    .withLastCommit(commit().withAuthor(user().withName("test").build()).withId(commit.getName()).build())
                    .build())
                .withProject(project()
                    .withWebUrl("https://gitlab.org/test.git")
                    .build()
                )
                .build(), true, BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
            newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        return buildTriggered;
	}

    private boolean ciSkipTestHelper(String MRDescription, String lastCommitMsg) throws IOException, InterruptedException {
        final OneShotEvent buildTriggered = new OneShotEvent();
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                buildTriggered.signal();
                return true;
            }
        });
        project.setQuietPeriod(0);
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = new MergeRequestHookTriggerHandlerImpl(Arrays.asList(State.opened, State.reopened), Arrays.asList(Action.approved), false, false);
        mergeRequestHookTriggerHandler.handle(project, mergeRequestHook()
                .withObjectAttributes(defaultMergeRequestObjectAttributes().withDescription(MRDescription).withLastCommit(commit().withMessage(lastCommitMsg).withAuthor(user().withName("test").build()).withId("testid").build()).build())
                .build(), true, BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
            newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        return buildTriggered.isSignaled();
    }

	private MergeRequestObjectAttributesBuilder defaultMergeRequestObjectAttributes() {
		return mergeRequestObjectAttributes()
		    .withIid(1)
            .withAction(Action.update)
            .withState(State.opened)
		    .withTitle("test")
		    .withTargetProjectId(1)
		    .withSourceProjectId(1)
		    .withSourceBranch("feature")
		    .withTargetBranch("master")
		    .withSource(project()
		        .withName("test")
		        .withNamespace("test-namespace")
		        .withHomepage("https://gitlab.org/test")
		        .withUrl("git@gitlab.org:test.git")
		        .withSshUrl("git@gitlab.org:test.git")
		        .withHttpUrl("https://gitlab.org/test.git")
		        .build())
		    .withTarget(project()
		        .withName("test")
		        .withNamespace("test-namespace")
		        .withHomepage("https://gitlab.org/test")
		        .withUrl("git@gitlab.org:test.git")
		        .withSshUrl("git@gitlab.org:test.git")
		        .withHttpUrl("https://gitlab.org/test.git")
		        .build());
	}

}
