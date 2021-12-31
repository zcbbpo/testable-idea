package org.testable.foo;

import com.alibaba.testable.core.annotation.MockInvoke;
import org.testable.idea.ext.ExtAbc;

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
}
