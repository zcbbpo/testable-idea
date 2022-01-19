package org.testable.foo;

import com.alibaba.testable.core.annotation.MockInvoke;
import org.testable.idea.ext.ExtAbc;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jim
 */
public class A {
    private String a;

    @MockInvoke(targetClass = ExtAbc.class, targetMethod = "zzf")
    public void foo() {
        A a = new A();
        a.foo();
    }

    public void test1() {


        List<F> list = new ArrayList<>();

        m1(list);
        ApiImp a = new ApiImp();
        a.save(list);
    }



    public <T extends C> void m1(List<T> l) {

    }

    static class ApiImp implements ApiF<C> {

        @Override
        public void save(List<? extends C> l) {

        }
    }

    static interface ApiF<T> {
        void save(List<? extends T> l);
    }


    static class C {

    }

    static class F extends C {

    }
}
