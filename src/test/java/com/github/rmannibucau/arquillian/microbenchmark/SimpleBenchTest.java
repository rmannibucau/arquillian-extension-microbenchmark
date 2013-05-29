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

@RunWith(Arquillian.class)
public class SimpleBenchTest {
    @Deployment(testable = true)
    public static Archive<?> war() {
        return ShrinkWrap.create(WebArchive.class, "simple.war").addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    @MicroBenchmark(iterations = 5)
    @MicroBenchmarkAssertion(total = 30)
    public void success() {
        sleep();
    }

    @Test(expected = AssertionException.class)
    @MicroBenchmark(iterations = 5)
    @MicroBenchmarkAssertion(total = 1)
    public void failling() {
        sleep();
    }

    private void sleep() {
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
