package ir.sharif.math.ap2023.hw5;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static org.junit.Assert.fail;

public class AllRule implements TestRule {
    private final MultiThreadCopierTest multiThreadCopierTest;

    public AllRule(MultiThreadCopierTest multiThreadCopierTest) {
        this.multiThreadCopierTest = multiThreadCopierTest;
    }

    private class RepeatStatement extends Statement {
        private final Statement statement;
        private final Description description;
        private final int repeat;

        public RepeatStatement(Statement statement, Description description, int repeat) {
            this.statement = statement;
            this.description = description;
            this.repeat = repeat;
        }

        @Override
        public void evaluate() throws Throwable {
            for (int i = 0; i < repeat; i++) {
                try {
                    log("start %s test for %d time", description.getMethodName(), i + 1);
                    statement.evaluate();
                    if (multiThreadCopierTest.isFail()) {
                        fail("fails in tear down: " + multiThreadCopierTest.getFailMessage());
                    } else {
                        log("test %s passed for %d time", description.getMethodName(), i + 1);
                    }
                } catch (Throwable throwable) {
                    log("test %s fails at %d time", description.getMethodName(), i + 1);
                    throw throwable;
                }
            }
        }

        private void log(String format, Object... args) {
            System.out.printf(format, args);
            System.out.println();
            System.err.printf(format, args);
            System.err.println();
        }

    }

    @Override
    public Statement apply(Statement statement, Description description) {
        Statement result = statement;
        Repeat repeat = description.getAnnotation(Repeat.class);
        if (repeat != null) {
            int times = repeat.value();
            result = new RepeatStatement(statement, description, times);
        }
        return result;
    }
}
