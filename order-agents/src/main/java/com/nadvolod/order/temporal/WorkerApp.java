package com.nadvolod.order.temporal;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;


public class WorkerApp {
    public static final String TASK_QUEUE = "order-fulfillment";

    public static void main(String[] args) {
        // WorkflowServiceStubS - the "S" at the end through me off because there is also WorkflowServiceStub
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        //2. create a client
        WorkflowClient client = WorkflowClient.newInstance(service);
        //3. create a factory and a worker that listens on a task queue
        WorkerFactory factory = WorkerFactory.newInstance(client);

        Worker worker = factory.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(OrderFulfillmentWorkflowImpl.class);

        //5. start polling
        factory.start();
        System.out.println("Worker started on queue. " + TASK_QUEUE);
    }
}
