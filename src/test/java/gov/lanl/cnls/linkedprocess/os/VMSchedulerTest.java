package gov.lanl.cnls.linkedprocess.os;

import gov.lanl.cnls.linkedprocess.LinkedProcess;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Author: josh
 * Date: Jun 29, 2009
 * Time: 3:21:06 PM
 */
public class VMSchedulerTest extends TestCase {
    private static final int MAX_RANDOM_INT = 100000;

    private final VMScheduler.VMResultHandler resultHandler = createResultHandler();
    private final VMScheduler.LopStatusEventHandler eventHandler = createEventHandler();
    private final Map<String, JobResult> resultsByID = new HashMap<String, JobResult>();
    private VMScheduler scheduler;
    private Random random = new Random();
    private String vmType = "JavaScript";

    public void setUp() {
        resultsByID.clear();


    }

    public void tearDown() {
    }

    public void testCreateAndShutDownScheduler() throws Exception {
        scheduler = new VMScheduler(resultHandler, eventHandler);
        assertEquals(LinkedProcess.FarmStatus.ACTIVE, scheduler.getSchedulerStatus());
        scheduler.shutDown();
    }

    public void testCreateVM() throws Exception {
        scheduler = new VMScheduler(resultHandler, eventHandler);
        String vm1 = randomJID();
        scheduler.spawnVirtualMachine(vm1, vmType);
        scheduler.shutDown();
    }

    public void testAddMultipleVMs() throws Exception {
        scheduler = new VMScheduler(resultHandler, eventHandler);
        String vm1 = randomJID();
        String vm2 = randomJID();
        scheduler.spawnVirtualMachine(vm1, vmType);
        assertEquals(LinkedProcess.VMStatus.ACTIVE, scheduler.getVirtualMachineStatus(vm1));
        scheduler.spawnVirtualMachine(vm2, vmType);
        assertEquals(LinkedProcess.VMStatus.ACTIVE, scheduler.getVirtualMachineStatus(vm2));
        assertEquals(LinkedProcess.FarmStatus.ACTIVE, scheduler.getSchedulerStatus());
        scheduler.shutDown();
    }

    public void testSchedulerStatusAfterShutdown() {
        scheduler = new VMScheduler(resultHandler, eventHandler);
        scheduler.shutDown();
        assertEquals(LinkedProcess.FarmStatus.TERMINATED, scheduler.getSchedulerStatus());
    }

    public void testVMStatusAfterTermination() throws Exception {
        scheduler = new VMScheduler(resultHandler, eventHandler);
        String vm1 = randomJID();
        scheduler.spawnVirtualMachine(vm1, vmType);
        assertEquals(LinkedProcess.VMStatus.ACTIVE, scheduler.getVirtualMachineStatus(vm1));
        scheduler.terminateVirtualMachine(vm1);
        assertEquals(LinkedProcess.VMStatus.DOES_NOT_EXIST, scheduler.getVirtualMachineStatus(vm1));
        scheduler.shutDown();
    }

    public void testVMStatusAfterSchedulerShutDown() throws Exception {
        scheduler = new VMScheduler(resultHandler, eventHandler);
        String vm1 = randomJID();
        scheduler.spawnVirtualMachine(vm1, vmType);
        assertEquals(LinkedProcess.VMStatus.ACTIVE, scheduler.getVirtualMachineStatus(vm1));
        scheduler.shutDown();
        assertEquals(LinkedProcess.VMStatus.DOES_NOT_EXIST, scheduler.getVirtualMachineStatus(vm1));
    }

    public void testAddJob() throws Exception {
        scheduler = new VMScheduler(resultHandler, eventHandler);
        String vm1 = randomJID();
        scheduler.spawnVirtualMachine(vm1, vmType);
        Job job = randomShortRunningJob(vm1);
        scheduler.scheduleJob(vm1, job);
        scheduler.waitUntilFinished();
        assertEquals(1, resultsByID.size());
        assertNormalResult(job);
        scheduler.shutDown();
    }

    public void testLongRunningJob() throws Exception {
        scheduler = new VMScheduler(resultHandler, eventHandler);
        String vm1 = randomJID();
        scheduler.spawnVirtualMachine(vm1, vmType);
        Job job = randomLongRunningJob(vm1);
        scheduler.scheduleJob(vm1, job);
        scheduler.waitUntilFinished();
        assertEquals(1, resultsByID.size());
        assertNormalResult(job);
        scheduler.shutDown();
    }

    public void testAddMultipleJobs() throws Exception {
        scheduler = new VMScheduler(resultHandler, eventHandler);
        String vm1 = randomJID();
        scheduler.spawnVirtualMachine(vm1, vmType);
        Job job1 = randomShortRunningJob(vm1);
        Job job2 = randomShortRunningJob(vm1);
        scheduler.scheduleJob(vm1, job1);
        scheduler.scheduleJob(vm1, job2);
        scheduler.waitUntilFinished();
        assertEquals(2, resultsByID.size());
        assertNormalResult(job1);
        assertNormalResult(job2);
        scheduler.shutDown();
    }

    public void testAddConcurrentLongRunningJobs() throws Exception {
        scheduler = new VMScheduler(resultHandler, eventHandler);
        String vm1 = randomJID();
        scheduler.spawnVirtualMachine(vm1, vmType);
        Job job1 = randomLongRunningJob(vm1);
        Job job2 = randomLongRunningJob(vm1);
        scheduler.scheduleJob(vm1, job1);
        scheduler.scheduleJob(vm1, job2);
        scheduler.waitUntilFinished();
        assertEquals(2, resultsByID.size());
        assertNormalResult(job1);
        assertNormalResult(job2);
        scheduler.shutDown();
    }

    public void testInvalidExpression() throws Exception {
        scheduler = new VMScheduler(resultHandler, eventHandler);
        String vm1 = randomJID();
        scheduler.spawnVirtualMachine(vm1, vmType);
        Job job = randomInvalidJob(vm1);
        scheduler.scheduleJob(vm1, job);
        scheduler.waitUntilFinished();
        assertEquals(1, resultsByID.size());
        JobResult result = resultsByID.get(job.getJobId());
        assertEquals(JobResult.ResultType.ERROR, result.getType());
        assertNotNull(result.getException());
        assertEquals(null, result.getExpression());
        scheduler.shutDown();
    }

    public void testValidButExceptionGeneratingExpression() throws Exception {
        scheduler = new VMScheduler(resultHandler, eventHandler);
        String vm1 = randomJID();
        scheduler.spawnVirtualMachine(vm1, vmType);
        Job job = randomValidButExceptionGeneratingJob(vm1);
        scheduler.scheduleJob(vm1, job);
        scheduler.waitUntilFinished();
        assertEquals(1, resultsByID.size());
        assertErrorResult(job);
        scheduler.shutDown();
    }

    public void testAbortLongRunningJob() throws Exception {
        scheduler = new VMScheduler(resultHandler, eventHandler);
        String vm1 = randomJID();
        scheduler.spawnVirtualMachine(vm1, vmType);
        Job job = randomLongRunningJob(vm1);
        scheduler.scheduleJob(vm1, job);
        scheduler.abortJob(vm1, job.getJobId());
        scheduler.waitUntilFinished();
        assertEquals(1, resultsByID.size());
        assertCancelledResult(job);
        scheduler.shutDown();
    }

    ////////////////////////////////////////////////////////////////////////////

    private VMScheduler.VMResultHandler createResultHandler() {
        return new VMScheduler.VMResultHandler() {

            public void handleResult(JobResult result) {
                resultsByID.put(result.getJob().getJobId(), result);
            }
        };
    }

    private VMScheduler.LopStatusEventHandler createEventHandler() {
        return new VMScheduler.LopStatusEventHandler() {

            public void schedulerStatusChanged(LinkedProcess.FarmStatus newStatus) {
                // Do nothing.
            }

            public void virtualMachineStatusChanged(String vmJID, LinkedProcess.VMStatus newStatus) {
                // Do nothing.
            }
        };
    }

    private void assertNormalResult(final Job job) {
        JobResult result = resultsByID.get(job.getJobId());
        assertEquals(JobResult.ResultType.NORMAL_RESULT, result.getType());
        // Note: not "1", but "1.0", as the resulting Object is a Double (for
        // some reason).  This is not particularly important for the test.
        assertEquals("1.0", result.getExpression());
        assertNull(result.getException());
    }

    private void assertErrorResult(final Job job) {
        JobResult result = resultsByID.get(job.getJobId());
        assertEquals(JobResult.ResultType.ERROR, result.getType());
        assertNotNull(result.getException());
        assertEquals(null, result.getExpression());
    }

    private void assertCancelledResult(final Job job) {
        JobResult result = resultsByID.get(job.getJobId());
        assertEquals(JobResult.ResultType.CANCELLED, result.getType());
        assertNotNull(result.getException());
        assertEquals(null, result.getExpression());
    }

    private String randomJID() {
        return "a" + random.nextInt(MAX_RANDOM_INT) + "@example.com";
    }

    private Job randomJob(final String vmJID,
                          final String expression) {
        String appJID = "?";
        String iqID = "job" + random.nextInt(MAX_RANDOM_INT);
        return new Job(vmJID, appJID, iqID, expression);
    }

    private Job randomShortRunningJob(final String vmJID) {
        return randomJob(vmJID, "1 + 0;");
    }

    private Job randomLongRunningJob(final String vmJID) {
        return randomJob(vmJID, "var p=1; for (i=0; i<100000; i++) {p *= 7; p /= 7;} p;");
    }

    private Job randomInvalidJob(final String vmJID) {
        return randomJob(vmJID, "0...0;");
    }

    private Job randomValidButExceptionGeneratingJob(final String vmJID) {
        return randomJob(vmJID, "idontexist[2] = 0;");
    }
}
