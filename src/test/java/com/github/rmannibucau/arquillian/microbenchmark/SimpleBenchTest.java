package com.github.rmannibucau.arquillian.microbenchmark;

import com.github.rmannibucau.arquillian.microbenchmark.api.AssertionException;
import com.github.rmannibucau.arquillian.microbenchmark.api.MicroBenchmark;
import com.github.rmannibucau.arquillian.microbenchmark.api.MicroBenchmarkAssertion;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Random;

@RunWith(Arquillian.class)
public class SimpleBenchTest {
    @Deployment(testable = true)
    public static Archive<?> war() {
        return ShrinkWrap.create(WebArchive.class, "simple.war").addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    private static final Random RANDOM = new Random(System.currentTimeMillis());

    @Test
    @MicroBenchmark(iterations = 10)
    @MicroBenchmarkAssertion(total = 100)
    public void success() {
        sleep(1, 5);
    }

    @Test(expected = AssertionException.class)
    @MicroBenchmark(iterations = 10)
    @MicroBenchmarkAssertion(total = 1)
    public void failling() {
        sleep(5, 10);
    }

    private void sleep(final int min, final int max) {
        try {
            Thread.sleep(min + RANDOM.nextInt(max - min));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
