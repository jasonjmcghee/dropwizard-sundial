package org.knowm.dropwizard.sundial;

import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.client.Client;

import org.junit.ClassRule;
import org.junit.Test;

import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit.DropwizardAppRule;

public class SundialBundleIT extends SundialBundleITBase {
    @ClassRule
    public static DropwizardAppRule<TestConfiguration> app = buildApp("test.yml", new Before() {
        public void before(Environment environment) {
            client = new JerseyClientBuilder(environment).build("test");
        }
    });

    static Client client;

    @Test
    public void shouldRegisterTasks() throws InterruptedException {
        final int start;

        synchronized (TestJob.lock) {
            start = TestJob.runs;
            startJob(app, client, TestApp.JOB_NAME);
            while (TestJob.runs == start) {
                TestJob.lock.wait(1000);
                assert TestJob.runs != start : "Timed out";
            }
        }

        assertThat(TestJob.runs).isEqualTo(start + 1);

        assertThat(stopJob(app, client, TestApp.JOB_NAME)).isTrue();
    }

    @Test
    public void shouldIncrementCounters() throws InterruptedException {
        final int start;

        synchronized (TestJob.lock) {
            start = TestJob.runs;

            assertThat(readTimerCount(app, client, "org.knowm.dropwizard.sundial.MetricsReporter.job.TestJob"))
                .isEqualTo(start);

            startJob(app, client, TestApp.JOB_NAME);
            while (TestJob.runs == start) {
                TestJob.lock.wait(1000);
                assert TestJob.runs != start : "Timed out";
            }
        }

        assertThat(readTimerCount(app, client, "org.knowm.dropwizard.sundial.MetricsReporter.job.TestJob"))
            .isEqualTo(start + 1);
        assertThat(readTimerCount(app, client, "org.knowm.dropwizard.sundial.MetricsReporter.job.TestJob"))
            .isEqualTo(TestJob.runs);
    }
}
