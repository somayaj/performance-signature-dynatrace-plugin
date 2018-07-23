/*
 * Copyright (c) 2014 T-Systems Multimedia Solutions GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tsystems.mms.apm.performancesignature.dynatrace;

import de.tsystems.mms.apm.performancesignature.dynatrace.configuration.CredProfilePair;
import de.tsystems.mms.apm.performancesignature.dynatrace.configuration.GenericTestCase;
import de.tsystems.mms.apm.performancesignature.dynatrace.rest.DTServerConnection;
import de.tsystems.mms.apm.performancesignature.dynatrace.rest.json.model.TestRunDefinition;
import de.tsystems.mms.apm.performancesignature.dynatrace.rest.xml.CommandExecutionException;
import de.tsystems.mms.apm.performancesignature.dynatrace.util.PerfSigUtils;
import de.tsystems.mms.apm.performancesignature.ui.util.PerfSigUIUtils;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.analysis.util.PluginLogger;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

import static de.tsystems.mms.apm.performancesignature.dynatrace.rest.DTServerConnection.*;

public class PerfSigStartRecording extends Builder implements SimpleBuildStep {
    private final String dynatraceProfile;
    private final String testCase;
    private String recordingOption;
    private boolean lockSession;

    @DataBoundConstructor
    public PerfSigStartRecording(final String dynatraceProfile, final String testCase) {
        this.dynatraceProfile = dynatraceProfile;
        this.testCase = StringUtils.deleteWhitespace(testCase);
    }

    @Override
    public void perform(@Nonnull final Run<?, ?> run, @Nonnull final FilePath workspace, @Nonnull final Launcher launcher, @Nonnull final TaskListener listener)
            throws InterruptedException, IOException {
        PluginLogger logger = PerfSigUIUtils.createLogger(listener.getLogger());
        DTServerConnection connection = PerfSigUtils.createDTServerConnection(dynatraceProfile);
        CredProfilePair pair = connection.getCredProfilePair();

        logger.log(Messages.PerfSigStartRecording_StartingSession());
        String extTestCase = run.getEnvironment(listener).expand(this.testCase);
        String sessionName = pair.getProfile() + "_" + run.getParent().getName() + "_Build-" + run.getNumber() + "_" + extTestCase;
        sessionName = sessionName.replace("/", "_");

        if (connection.getRecordingStatus()) {
            logger.log(Messages.PerfSigStartRecording_AnotherSessionStillRecording());
            PerfSigStopRecording stopRecording = new PerfSigStopRecording(dynatraceProfile);
            stopRecording.perform(run, workspace, launcher, listener);
        }

        String sessionId = null;
        try {
            sessionId = connection.startRecording(sessionName, Messages.PerfSigStartRecording_SessionTriggered(), getRecordingOption(), lockSession, false);
        } catch (CommandExecutionException e) {
            if (!e.getMessage().contains("licenses")) {
                throw new CommandExecutionException("error while starting session recording: " + e.getMessage());
            }
        }
        Date timeframeStart = new Date();
        if (sessionId != null) {
            logger.log(Messages.PerfSigStartRecording_StartedSessionRecording(pair.getProfile(), sessionName));
        } else {
            logger.log(Messages.PerfSigStartRecording_SessionRecordingError(pair.getProfile()));
        }

        logger.log(Messages.PerfSigStartRecording_RegisteringTestRun());
        String testRunId = null;
        try {
            Map<String, String> variables = run.getEnvironment(listener);
            TestRunDefinition body = new TestRunDefinition(run.getNumber())
                    .setVersionMajor(variables.get(BUILD_VAR_KEY_VERSION_MAJOR))
                    .setVersionMinor(variables.get(BUILD_VAR_KEY_VERSION_MINOR))
                    .setVersionRevision(variables.get(BUILD_VAR_KEY_VERSION_REVISION))
                    .setVersionMilestone(variables.get(BUILD_VAR_KEY_VERSION_MILESTONE))
                    .setCategory(variables.get(BUILD_VAR_KEY_CATEGORY) != null ? variables.get(BUILD_VAR_KEY_CATEGORY) : "performance")
                    .setPlatform(variables.get(BUILD_VAR_KEY_PLATFORM))
                    .setMarker(variables.get(BUILD_VAR_KEY_MARKER))
                    .addAdditionalMetaData("JENKINS_JOB", variables.get(BUILD_URL_ENV_PROPERTY));

            testRunId = connection.registerTestRun(body);
        } catch (CommandExecutionException e) {
            logger.log(Messages.PerfSigStartRecording_CouldNotRegisterTestRun() + e.getMessage());
        }
        if (testRunId != null) {
            logger.log(Messages.PerfSigStartRecording_StartedTestRun(pair.getProfile(), testRunId, PerfSigEnvContributor.TESTRUN_ID_KEY));
        }

        run.addAction(new PerfSigEnvInvisAction(extTestCase, sessionId, sessionName, timeframeStart, testRunId));
    }

    public String getTestCase() {
        return testCase;
    }

    public String getRecordingOption() {
        return recordingOption == null ? DescriptorImpl.defaultRecordingOption : recordingOption;
    }

    @DataBoundSetter
    public void setRecordingOption(final String recordingOption) {
        this.recordingOption = recordingOption;
    }

    public boolean isLockSession() {
        return lockSession;
    }

    @DataBoundSetter
    public void setLockSession(final boolean lockSession) {
        this.lockSession = lockSession;
    }

    public String getDynatraceProfile() {
        return dynatraceProfile;
    }

    @Symbol("startSession")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public static final boolean defaultLockSession = false;
        public static final String defaultRecordingOption = "all";

        @Restricted(NoExternalUse.class)
        @Nonnull
        public ListBoxModel doFillRecordingOptionItems(@AncestorInPath Item item) {
            if (PerfSigUIUtils.checkForMissingPermission(item)) {
                return new ListBoxModel();
            }
            return new ListBoxModel(new ListBoxModel.Option("all"), new ListBoxModel.Option("violations"), new ListBoxModel.Option("timeseries"));
        }

        @Restricted(NoExternalUse.class)
        public FormValidation doCheckTestCase(@AncestorInPath Item item, @QueryParameter final String testCase) {
            FormValidation validationResult = FormValidation.ok();
            if (PerfSigUIUtils.checkForMissingPermission(item)) {
                return validationResult;
            }

            try {
                Jenkins.checkGoodName(testCase);
                GenericTestCase.DescriptorImpl.addTestCases(testCase);
                return validationResult;
            } catch (Failure e) {
                return FormValidation.error(e.getMessage());
            }
        }

        @Nonnull
        @Restricted(NoExternalUse.class)
        public ListBoxModel doFillDynatraceProfileItems(@AncestorInPath Item item) {
            if (PerfSigUIUtils.checkForMissingPermission(item)) {
                return new ListBoxModel();
            }
            return PerfSigUtils.listToListBoxModel(PerfSigUtils.getDTConfigurations());
        }

        @Override
        public boolean isApplicable(final Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.PerfSigStartRecording_DisplayName();
        }
    }
}
