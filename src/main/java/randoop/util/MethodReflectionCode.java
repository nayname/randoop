package randoop.util;

import static randoop.Globals.lineSep;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/** Wraps a method together with its arguments, ready for execution. Can be run only once. */
public final class MethodReflectionCode extends ReflectionCode {

  private final Method method;
  /** May be null. */
  private final Object receiver;

  private final Object[] inputs;

  public MethodReflectionCode(Method method, Object receiver, Object[] inputs) {
    this.receiver = receiver;
    this.method = method;
    this.inputs = inputs;

    if (!this.method.isAccessible()) {
      this.method.setAccessible(true);
      Log.logLine("not accessible: " + this.method);
      // TODO something is bizarre - it seems that a public method can be
      // not-accessible sometimes. RatNum(int,int)
      // TODO you cannot just throw the exception below - because no sequences
      // will be created in the randoop.experiments.
      // throw new IllegalStateException("Not accessible: " + this.meth);
    }
  }

  private boolean isInstanceMethod() {
    return !Modifier.isStatic(method.getModifiers());
  }

  @SuppressWarnings("Finally")
  @Override
  public void runReflectionCodeRaw() {
    Log.logLine("runReflectionCodeRaw: " + method);
    try {
      this.retval = this.method.invoke(this.receiver, this.inputs);
      try {
        Log.logLine("runReflectionCodeRaw(" + method + ") => " + retval);
      } catch (OutOfMemoryError e) {
        Log.logLine("runReflectionCodeRaw(" + method + ") => [value too large to print]");
      }
      if (receiver == null && isInstanceMethod()) {
        throw new ReflectionCodeException(
            "receiver was null - expected NPE from call to: " + method);
      }
    } catch (NullPointerException e) {
      this.exceptionThrown = e;
    } catch (InvocationTargetException e) {
      // The underlying method threw an exception
      this.exceptionThrown = e.getCause();
    } catch (Throwable e) {
      // Any other exception indicates Randoop should not have called the method
      String message =
          String.format(
              "error invoking %s on %d arguments:",
              method, (receiver == null ? 0 : 1) + inputs.length);
      if (receiver != null) {
        message += lineSep + "  " + receiver;
      }
      for (Object input : inputs) {
        message += lineSep + "  " + input;
      }
      throw new ReflectionCodeException(message, e);
    }
  }

  @Override
  public String toString() {
    return "Call to "
        + method
        + " receiver: "
        + receiver
        + " args: "
        + Arrays.toString(inputs)
        + status();
  }
}
