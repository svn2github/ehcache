package net.sf.ehcache.osgi;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;

import java.util.Date;

import net.sf.ehcache.osgi.util.TestUtil;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.quartz.DateBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

/**
 * @author hhuynh
 * 
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class SimpleQuartzTest {

  @Configuration
  public Option[] config() {
    return options(mavenBundle("org.quartz-scheduler", "quartz").versionAsInProject(),
        wrappedBundle(maven("c3p0", "c3p0").versionAsInProject()), TestUtil.commonOptions());
  }

  @Test
  public void testQuartz() throws Exception {
    SchedulerFactory sf = new StdSchedulerFactory();
    Scheduler sched = sf.getScheduler();
    Date runTime = DateBuilder.evenMinuteDate(new Date());
    JobDetail job = JobBuilder.newJob(HelloJob.class)
        .withIdentity("job1", "group1").build();
    org.quartz.Trigger trigger = TriggerBuilder.newTrigger().withIdentity("trigger1", "group1")
        .startAt(runTime).build();
    sched.scheduleJob(job, trigger);
    sched.start();
    try {
      Thread.sleep(5000L);
    } catch (Exception e) {
    }
    sched.shutdown(true);
  }

  private static class HelloJob implements Job {
    public void execute(JobExecutionContext context) throws JobExecutionException {
      System.out.println("Hello world!");
    }
  }
}