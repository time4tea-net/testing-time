package net.time4tea.time.executors.test;

import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class ControllableSimpleScheduledExecutorServiceTest {

    private ControllableSimpleScheduledExecutorService service = new ControllableSimpleScheduledExecutorService();
    private AtomicInteger counter = new AtomicInteger();

    @Test
    public void isntShutdownUntilItIs() throws Exception {
        assertThat(service.isShutdown(), equalTo(false));
        service.shutdown();
        assertThat(service.isShutdown(), equalTo(true));
    }

    @Test
    public void runsScheduledTasksThatAreSubmittedAtTheCorrectTime() throws Exception {
        assertThat(counter.get(), equalTo(0));
        service.schedule(() -> counter.incrementAndGet(), Duration.ofSeconds(1));
        assertThat(counter.get(), equalTo(0));
        service.timePasses(Duration.ofSeconds(1));
        assertThat(counter.get(), equalTo(1));
    }

    @Test
    public void cancellingAScheduledTaskBeforeItRunsWillCancelIt() throws Exception {
        ScheduledFuture<Integer> future = service.schedule(() -> counter.incrementAndGet(), Duration.ofSeconds(1));
        future.cancel(true);
        service.timePasses(Duration.ofSeconds(1));
        assertThat(counter.get(), equalTo(0));
    }


    public static class ControllableSimpleScheduledExecutorService implements SimpleScheduledExecutorService {

        private long clock = 0L;
        private boolean isShutdown = false;
        private List<SimpleScheduleTask> tasks = new ArrayList<>();

        private class SimpleScheduleTask<T> implements ScheduledFuture<T> {
            private final Callable<T> callable;
            private boolean isCancelled = false;
            private final Duration delay;
            private final long timeToRun;
            private T result;

            public SimpleScheduleTask(Callable<T> callable, Duration delay, long timeToRun) {
                this.callable = callable;
                this.delay = delay;
                this.timeToRun = timeToRun;
            }

            @Override
            public long getDelay(TimeUnit unit) {
                return unit.convert(timeToRun - clock, TimeUnit.MILLISECONDS);
            }

            @Override
            public int compareTo(Delayed o) {
                throw new UnsupportedOperationException("james didn't write");
            }

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                isCancelled = true;
                return true;
            }

            @Override
            public boolean isCancelled() {
                throw new UnsupportedOperationException("james didn't write");
            }

            @Override
            public boolean isDone() {
                throw new UnsupportedOperationException("james didn't write");
            }

            @Override
            public T get() throws InterruptedException, ExecutionException {
                throw new UnsupportedOperationException("james didn't write");
            }

            @Override
            public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                throw new UnsupportedOperationException("james didn't write");
            }

            public void execute() {
                try {
                    if (! isCancelled) callable.call();
                } catch (Exception e) {

                }
            }
        }

        @Override
        public boolean isShutdown() {
            return isShutdown;
        }

        @Override
        public <T> ScheduledFuture<T> schedule(Callable<T> callable, Duration delay) {
            return enqueue(new SimpleScheduleTask<>(
                    callable, delay, clock + delay.toMillis()
            ));
        }

        private <T> ScheduledFuture<T> enqueue(SimpleScheduleTask<T> task) {
            tasks.add(task);
            return task;
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable runnable, Duration delay) {
            return null;
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable runnable, Duration initialDelay, Duration delay) {
            throw new UnsupportedOperationException("james didn't write");
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, Duration initialDelay, Duration period) {
            throw new UnsupportedOperationException("james didn't write");
        }

        public void shutdown() {
            isShutdown = true;
        }

        public void timePasses(Duration duration) {
            clock += duration.toMillis();
            runPendingTasks();
        }

        private void runPendingTasks() {
            for (SimpleScheduleTask task : tasks) {
                if (task.timeToRun <= clock) {
                    try {
                        task.execute();
                    } catch (Exception e) {

                    }
                }
            }
        }
    }
}