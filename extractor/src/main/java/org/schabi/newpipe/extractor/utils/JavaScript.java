package org.schabi.newpipe.extractor.utils;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptableObject;

import javax.annotation.Nullable;

public final class JavaScript {
    @Nullable
    private static JavaScriptInterpreter externalJavaScriptInterpreter;

    private JavaScript() {
    }

    public static void compileOrThrow(final String function) {
        try {
            final Context context = Context.enter();
            context.setOptimizationLevel(-1);

            // If it doesn't compile it throws an exception here
            context.compileString(function, null, 1, null);
        } finally {
            Context.exit();
        }
    }

    public static String run(final String function,
                             final String functionName,
                             final String... parameters) {
        if (externalJavaScriptInterpreter != null) {
            return externalJavaScriptInterpreter.run(function, functionName, parameters);
        }

        try {
            final Context context = Context.enter();
            context.setOptimizationLevel(-1);
            final ScriptableObject scope = context.initSafeStandardObjects();

            context.evaluateString(scope, function, functionName, 1, null);
            final Function jsFunction = (Function) scope.get(functionName, scope);
            final Object result = jsFunction.call(context, scope, scope, parameters);
            return result.toString();
        } finally {
            Context.exit();
        }
    }

    /**
     * Sets an external JavaScript interpreter, that is used to run JavaScript code.
     *
     * @param javaScriptInterpreter the complete JavaScript base player code
     */
    public static void setExternalJavaScriptInterpreter(
            @Nullable final JavaScriptInterpreter javaScriptInterpreter) {
        JavaScript.externalJavaScriptInterpreter = javaScriptInterpreter;
    }
}
