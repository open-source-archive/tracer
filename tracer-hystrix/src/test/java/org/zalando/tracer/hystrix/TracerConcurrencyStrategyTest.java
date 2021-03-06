package org.zalando.tracer.hystrix;

import com.hystrix.junit.HystrixRequestContextRule;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zalando.tracer.Trace;
import org.zalando.tracer.Tracer;

import java.util.concurrent.Callable;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public final class TracerConcurrencyStrategyTest {

    @Rule
    public HystrixRequestContextRule hystrix = new HystrixRequestContextRule();

    private final Tracer tracer = Tracer.builder()
            .trace("X-Trace", () -> "76f6046c-1b56-11e6-8c85-8fc9ee29f631")
            .build();
    private final Trace trace = tracer.get("X-Trace");

    @Before
    public void setUp() {
        final HystrixPlugins plugins = HystrixPlugins.getInstance();
        final HystrixConcurrencyStrategy delegate = new HystrixConcurrencyStrategy() {
            @Override
            public <T> Callable<T> wrapCallable(final Callable<T> callable) {
                return () -> {
                    // to verify that the delegate has already access to the trace
                    assertThat(trace.getValue(), is("76f6046c-1b56-11e6-8c85-8fc9ee29f631"));
                    return callable.call();
                };
            }
        };
        plugins.registerConcurrencyStrategy(new TracerConcurrencyStrategy(tracer, delegate));
    }

    @Test
    public void shouldGetTrace() {
        tracer.start();

        try {
            final String traceId = new GetTrace().execute();
            assertThat(traceId, is("76f6046c-1b56-11e6-8c85-8fc9ee29f631"));
        } finally {
            tracer.stop();
        }
    }

    private final class GetTrace extends HystrixCommand<String> {

        public GetTrace() {
            super(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"));
        }

        @Override
        protected String run() {
            return trace.getValue();
        }

    }

}