import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.function.Supplier;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * TAP for Java.  This is the first version
 * to use an independent class, as opposed to
 * directed inheritance.
 *
 * @version 0.2
 * @author Ethan Meng
 */
public class TapTest
{
    /** Default constructor. */
    public TapTest() { }

    /** Number of tests that have been run. */
    private int count = 0;

    /**
     * {@return the number of tests we ran}
     * @since 0.1
     */
    protected int count ()
    {
        return count;
    }

    /**
     * Number of tests we plan to run.  Zero is special;
     * it means we run as much as we need to.
     * Negative plan is not allowed.
     */
    private int plan = 0;

    // XXX: not sure if it is a good idea to expose this
    // (we'll blow up if users make a plan twice anyway, right?)
    /**
     * {@return the number of tests we plan to run, or
     * 0 if no plans are made yet.}
     *
     * @see #plan(int)
     * @since 0.1
     */
    protected int plan ()
    {
        return plan;
    }

    /**
     * Number of tests that failed.
     */
    private int fail = 0;

    /**
     * {@return the number of tests failed}
     * @since 0.1
     */
    protected int failed ()
    {
        return fail;
    }

    /**
     * Whether the test has ended.
     */
    private boolean ended = false;

    /**
     * {@return whether the test has ended}
     * (Previously, this was named ended. It feels silly
     * now, so I renamed it to just {@link #done}.)
     *
     * @since 0.2
     */
    protected boolean done ()
    {
        return ended;
    }

    /**
     * Print stack trace of an exception in form of diagnosis.
     *
     * @param e the throwable whose stack trace is delicious
     * @see #diag
     * @since 0.1
     */
    protected void confess (Throwable e)
    {
        StringWriter w = new StringWriter();
        e.printStackTrace(new PrintWriter(w));
        for (String line : w.toString().split("\\n"))
        {
            diag (line);
        }
    }

    /**
     * Make a plan right away.  This prints your plan to
     * standard output immediately for the world to see.
     * So plan carefully.  For example, plan(9) if you
     * plan to run 9 test cases.
     * <p>
     * {@code plan(0)} is currently a no-op, though I don't think
     * it makes sense and you should probably avoid it.
     * Use {@link #skip_all} if you plan to run zero tests.
     *
     * @param plan a plan you made
     * @return your best plan!!!!
     * @throws IllegalStateException if there is an active plan, or the
     *   new proposed plan is somehow negative (here we
     *   assume the closure of natural numbers under
     *   ordinary addition operation.)
     * @see #plan()
     * @since 0.1
     */
    protected int plan (int plan)
    {
        if (this.plan > 0) {
            throw new IllegalStateException("You already have a plan: " + this.plan);
        }
        if (plan < 0) {
            throw new IllegalStateException("Plan cannot be negative");
        }
        this.plan = plan;
        if (this.plan > 0) {
            System.out.println("1.." + plan);
        }
        return plan;
    }

    /**
     * Conclude the test.  Tally up our count and
     * see if it went to plan, or print out the plan
     * if we never announced our plan.  If things did
     * not go to plan, we'll present the plan and the
     * reality.  Then return a nonzero value (i.e. true).
     * <p>
     * In the latter scenario (i.e. we never announced
     * our plan), if the number of tests run is zero,
     * the effect is the same as calling {@link skip_all}
     * with reason {@code "No tests defined"}.  You should
     * probably treat that as an error, since a test suite
     * with no test cases is kind of useless.
     *
     * @throws IllegalStateException if test has already ended,
     *   or a subtest is ongoing
     * @return whether or not we have confessed our
     *   lament to standard output
     * @since 0.1
     */
    protected boolean done_testing ()
    {
        if (ended) {
            throw new IllegalStateException("I thought we were done here!");
        }
        if (subtesting) {
            throw new IllegalStateException(
                "You are in the middle of a subtest -- "
                + "did you mean to call done_subtest()?");
        }
        if (plan == 0) {
            if (count == 0) {
                System.out.println("1..0 # SKIP No tests defined");
            }
            else {
                System.out.println("1.." + count);
            }
        }
        else if (plan > 0 && count != plan) {
            diag ("You planned to run %d test%s, but %d test%s ran.",
                plan, plan == 1 ? "" : "s",
                count, count == 1 ? "" : "s");
            diag ("Seems like things did not go to plan.");
            return true;
        }
        ended = true;
        return false;
    }

    /**
     * Skip all tests.  Or skip the rest of the tests,
     * if you already have a plan yet somehow decide to
     * back out in the middle of it.  (Coward!)
     * <p>
     * This terminates the test, similar to how
     * {@link done_testing} does.
     *
     * @param reason a pretext for why you are skipping
     * @return number of tests skipped, or 0 if the entire
     *   test suite is skipped.  Note that the former can be
     *   zero if you run this while you happened to be done
     *   with all tests:  that's special, but not special
     *   enough to be the exception. ;)
     * @throws IllegalStateException if the test has ended
     * @since 0.1
     */
    protected int skip_all (String reason)
    {
        if (ended) {
            throw new IllegalStateException("test has ended; there is nothing to skip");
        }
        int skipper = 0;
        if (plan == 0) {
            System.out.println("1..0 # SKIP " + reason);
        }
        else {
            for (; count < plan; ++count) {
                fail ("SKIP " + reason);
                ++skipper;
            }
        }
        ended = true;
        return skipper;
    }

    /**
     * Abort all tests.
     *
     * @param reason why you want to bail out
     * @since 0.1
     */
    protected void bail_out (String reason)
    {
        System.out.println("Bail out! " + reason);
    }

    /**
     * Print diagnosis to standard error.
     * Write your stuff on ONE LINE PLEASE!
     * I won't split them for you.
     * <p>
     * {@link String#format(String, Object...)} is used
     * to perform string interpolation.  Verbatim percent
     * signs will need to be escaped.
     *
     * @param line the string to print, optionally containing
     *   {@link String#format(String, Object...)}-substitutions
     * @param args if you do have substitutions, the rest of
     *   the arguments to {@link String#format(String, Object...)}
     * @since 0.1
     */
    protected void diag (String line, Object... args)
    {
        line = String.format(line, args);
        if (line.isBlank()) {
            System.err.println("#");
        }
        else {
            System.err.println("# " + line);
        }
    }

    /**
     * {@link diag}'s distant relative that prints to standard out
     * instead.  I didn't have the time to add this when writing
     * this for P105, so I'll implement it here so I remember to
     * backport this if this seems fit.
     *
     * @param line the line to print, potentially with format specs
     * @param args parameters for said format specs
     * @since 0.1.6
     */
    protected void note (String line, Object... args)
    {
        line = String.format(line, args);
        if (line.isBlank()) {
            System.out.println("#");
        }
        else {
            System.out.println("# " + line);
        }
    }

    /**
     * Convenience function that branches into {@link note} or
     * {@link diag} depending on your mood.
     *
     * @param ok if you are happy, pass a smiley face (i.e. true).
     *   If you are unhappy, pass a frowny face (i.e. false).
     * @param line log message
     * @param args log parameters
     * @since 0.1.6
     */
    protected void log (boolean ok, String line, Object... args)
    {
        if (ok) {
            note (line, args);
        }
        else {
            diag (line, args);
        }
    }

    /**
     * I'm OK if you're ok.
     *
     * @param ok Are you OK?
     * @param mess a short description of what
     *   part of ok-ness you are checking, optionally
     *   followed by newline (LF)-delimited diagnosis
     * @return Am I okay
     * @since 0.1
     */
    protected boolean ok (boolean ok, String mess)
    {
        return ok (ok, mess, 1);
    }

    /**
     * {@link ok(boolean, String)} with an option to specify
     * stack trace level.  We will only bother to print a
     * stack trace if the assertion fails (i.e. {@code !ok}.)
     *
     * @param ok Are you OK?
     * @param mess a short and sweet description for the test,
     *   optionally followed by a list of lines for diagnosis
     * @param stacklevel the number of stack frames above
     *   the current caller (you, not me)
     * @return Am I okay?
     * @since 0.1
     */
    protected boolean ok (boolean ok, String mess, int stacklevel)
    {
        boolean is_todo = mess.startsWith("TODO");
        return ok (ok, mess, !ok || is_todo, stacklevel + 2);
    }

    /**
     * {@link ok(boolean, String, int)} with an option to specify
     * whether a stack trace is produce.  This is the most general
     * form of the {@code ok()} API.
     *
     * @param ok Are you OK?
     * @param mess a short and sweet description for the test,
     *   optionally followed by a list of lines for diagnosis
     * @param trace Previously, this meant whether I should
     *   print a stack trace.  In hindsight, this is a bit
     *   silly, since who doesn't what a stack trace.  So
     *   now it means "do I print to the error stream so
     *   that prove will then show the message to the user?"
     * @param stacklevel the number of stack frames above
     *   the current caller (you, not me)
     * @return Am I okay?
     * @since 0.1
     */
    protected boolean ok (boolean ok, String mess, boolean trace, int stacklevel)
    {
        String[] reasons = splitlines(mess);
        boolean unreasonable = reasons.length == 0;
        boolean is_todo = !unreasonable && reasons[0].contains("TODO");
        boolean is_skip = !unreasonable && reasons[0].contains("SKIP");
        StringBuilder chant = new StringBuilder();
        String number;
        chant.append(ok ? "ok " : "not ok ");
        if (origin >= 0) {
            number = Integer.toString(++count + origin);
        }
        else {
            number = subtest + "." + (++count);
        }
        ++subcount;
        chant.append(number);
        if (!unreasonable) {
            chant.append(is_todo || is_skip ? " # " : " - ");
            chant.append(reasons[0]);
        }
        if (origin >= 0) {
            System.out.println(chant.toString());
        }
        else {
            note (chant.toString());
        }
        String verb = ok ? "passed" : "failed";
        if (trace || reasons.length > 1) {
            log (!trace, "Assertion %s `%s' %s:", number, reasons[0], verb);
            Throwable e = new AssertionError();
            StackTraceElement[] s = e.getStackTrace();
            if (s.length < 0 || stacklevel >= s.length) {
                log (!trace, "Stack trace unavailable "
                    + "(level " + stacklevel + " out of bounds"
                    + " for call stack of depth " + s.length + ")");
            }
            else {
                log (!trace, "      at " + s[stacklevel]);
            }
        }
        for (int i = 1; i < reasons.length; ++i) {
            log (!trace, reasons[i]);
        }
        if (!(ok || is_todo)) {
            fail++; subfail++;
        }
        if (is_todo) {
            if (ok) {
                subdone.add(count);
            }
            else {
                subtodo++;
            }
        }
        return ok;
    }

    /**
     * Split by line feed, skipping at most one empty
     * string at the end.
     * <p>
     * Note the intentional design to return the empty
     * list on an empty string <i>or</i> a correctly
     * line-delimited text containing a single blank line
     * (i.e. a string with just the line feed itself).
     *
     * @param text string of text
     * @return lines
     */
    private static String[] splitlines(String text)
    {
        String[] lines = text.split("\\n", -1);
        int n = lines.length;
        /* The following guard exists for robustness, but
         * split(...) returning an empty array on a negative
         * limit is badly misbehaved, speaking from my tests. */
        if (n == 0) {
            return lines;
        }
        if (lines[n - 1].isEmpty()) {
            return Arrays.copyOf(lines, n - 1);
        }
        return lines;
    }

    /**
     * {@code ok(1)} with no trace.
     *
     * @param mess a description
     * @see #ok(boolean, String, boolean, int)
     * @since 0.1
     */
    protected void pass (String mess) {
        ok (true, mess, false, 0);
    }

    /**
     * {@code ok(0)} with no trace.
     *
     * @param mess a description
     * @see #ok(boolean, String, boolean, int)
     * @since 0.1
     */
    protected void fail (String mess)
    {
        ok (false, mess, false, 0);
    }

    /**
     * Pass {@code count} tests.
     *
     * @param count how many tests to pass
     * @param mess a description
     * @since 0.1.6
     */
    protected void pass (int count, String mess)
    {
        for (int i = 0; i < count; ++i) {
            pass (mess);
        }
    }

    /**
     * Fail {@code count} tests.
     *
     * @param count how many tests to fail
     * @param mess a description
     * @since 0.1.6
     */
    protected void fail (int count, String mess)
    {
        for (int i = 0; i < count; ++i) {
            fail (mess);
        }
    }

    /**
     * The much-needed complement to {@link skip_all}.  This skips
     * a specified number of tests that are marked uniformly with the
     * same message.
     *
     * @param num number of tests to skip; only a positive number of
     *   {@code num} will be skipped.
     * @param mess skip message.  Multi-line is not supported.
     * @since 0.1.6
     */
    protected void skip (int num, String mess) {
        for (int i = 0; i < num; ++i) {
            ok (true, "SKIP " + mess);
        }
    }

    /**
     * Command-line argument parser.
     *
     * @param prog argument 0; program name
     * @param args argument vector from argument 1 and on
     * @param start starting number of our tests
     * @param avail number of tests we have
     * @return tests we should run; or an array of a
     *   single negative number as error status
     * @throws IllegalArgumentException if {@code start}
     *   is negative, and other weird stuff happens
     * @since 0.2
     */
    public static int[] parse_cmd (String prog,
        String[] args, int start, int avail)
    {
        if (start < 0) {
            /* "test-n" would not be a valid Java
             * identifier; don't let that happen. */
            throw new IllegalArgumentException(
                "first test cannot be negative (you "
                + "gave me " + start + ")");
        }
        int offset = start - 1;

        int optind = 0;
        final String USAGE = String.format(
            "usage: %s [[-i] <range>...] [-x <range>...]",
            prog
        );

        // options

        int[] rangeLo = new int[args.length];
        int[] rangeHi = new int[args.length];
        int rangeCount = 0;
        // include? +1 exclude? -1
        int color = +1;

        for (; optind < args.length; ++optind) {
            switch (args[optind]) {
            case "-h":
            case "--help":
                System.out.println(USAGE);
                System.out.println();
                System.out.print("""
                This is TAP for Java.

                options:
                  -h, --help        print this help message
                  -i, --include <range>...   include these ranges
                                    (default: include everything)
                  -x, --exclude <range>...   exclude these ranges
                                    (default: exclude nothing)
                """.stripIndent());
                return new int[0];
            case "-x":
            case "--exclude":
                color = -1;
                break;
            case "-i":
            case "--include":
                color = +1;
                break;
            default:
                /* parse perl-style integer range */
                String[] operands = args[optind].split("\\.\\.");
                if (operands.length != 2) {
                    System.err.printf(
                        "error: %s does not look like "
                        + "start..end\n", args[optind]
                    );
                    return new int[] {-2};
                }
                int begin;
                try {
                    begin = Integer.parseInt(operands[0]);
                }
                catch (NumberFormatException e) {
                    System.err.printf(
                        "error parsing start number %s in %s: %s\n",
                        operands[0], args[optind], e.getMessage()
                    );
                    return new int[] {-2};
                }
                begin -= offset;
                if (begin < 1 || begin > avail) {
                    System.err.printf(
                        "error: start number %s in %s out of range: "
                        + "must be at least %d and at most %d\n",
                        begin + offset, args[optind], begin,
                        offset + avail
                    );
                    return new int[] {-2};
                }
                rangeLo[rangeCount] = begin * color;
                int end;
                try {
                    end = Integer.parseInt(operands[1]);
                }
                catch (NumberFormatException e) {
                    System.err.printf(
                        "error parsing start number %s in %s: %s\n",
                        operands[1], args[optind], e.getMessage()
                    );
                    return new int[] {-2};
                }
                end -= offset;
                if (end < begin - 1 || end > avail) {
                    System.err.printf(
                        "error: end number %s in %s out of range: "
                        + "must be at least %d and at most %d\n",
                        end + offset, args[optind], begin - 1 + offset,
                        offset + avail
                    );
                    return new int[] {-2};
                }
                rangeHi[rangeCount] = end * color;
                rangeCount++;
            }
        }

        // lambda wants to see something final
        // TODO: rewrite in something that doesn't count on lambda
        final int RANGE_TOTAL_COUNT = rangeCount;
        final int INDEX_OFFSET = offset;
        return IntStream.rangeClosed(1, avail)
            .filter(t -> {
                int thingsToInclude = 0;
                for (int j = RANGE_TOTAL_COUNT; j > 0; --j) {
                    int i = j - 1;
                    int antiColor;
                    boolean decision;
                    if (rangeLo[i] > 0) {
                        antiColor = +1;
                        decision = true;
                        thingsToInclude++;
                    }
                    else {
                        antiColor = -1;
                        decision = false;
                    }

                    if (antiColor * rangeLo[i] <= t &&
                        t <= antiColor * rangeHi[i])
                    {
                        return decision;
                    }
                }
                // include the rest ONLY if there are no
                // include rules; this makes something like
                // 1..0 effectively an exclude-all rule.
                return thingsToInclude == 0;
            })
            .map(t -> t + INDEX_OFFSET).toArray();
    }

    /**
     * Make a plan, the vector version.
     *
     * @param plan your multitudinous list of plans
     * @return your best plan in aggregate
     */
    public int plan (int[] plan)
    {
        /* Holy moly i love you Java 8 */
        return plan ( IntStream.of(plan).sum() );
    }

    /* Flags for run output. (I don't want an enum since
     * that would compile to a different class file)
     *
     * This should go without saying, but DO NOT RELY
     * on these concretely values; use them with the name
     * I gave them.  It is reasonable to assume that I
     * won't need more than 32 bits though. */

    /** One or more bad subplans. */
    public static final int EX_BADPLAN = 0x0001;

    /** One or more test methods not found. */
    public static final int EX_NOTMETH = 0x0002;

    /** Runtime exception. */
    public static final int EX_BADMETH = 0x0004;
    /** Static initializer error. */
    public static final int EX_BADINIT = 0x0008;
    /** Invocation error. */
    public static final int EX_BADCALL = 0x0010;

    /** Constructor (factory) error. */
    public static final int EX_ERRINIT = 0x0020;

    /**
     * Execute tests on another object.  Will use reflection magic.
     * Note that the return status only concerns itself with fatal
     * errors, not test errors; that is usually the responsibility
     * for TAP consumers.  The only exception is EX_BADPLAN, which
     * you may filter out if you think it miscounted.
     *
     * @param todo list of test numbers
     * @param plan list of plans; must have the same dimensions
     * @param factory a function that returns a reference to
     *   a fresh test object
     * @return error status; see the EX_* flags
     * @since 0.2
     */
    public int run (int[] todo, int[] plan, Supplier<?> factory)
    {
        int offense = 0;

        for (int i = 0; i < todo.length; ++i) {
            int t = todo[i];
            int planet = plan[i];
            String testName = "test" + t;

            Object user;
            TapTest self = new TapTest();
            self.subplan(planet);
            Method test;

            try {
                user = factory.get();
            }
            catch (Exception e) {
                diag (testName + ": construction failed");
                confess (e);
                offense |= EX_ERRINIT;
                continue;
            }

            if (user == null) {
                diag (testName + ": constructor returned null");
                offense |= EX_ERRINIT;
                continue;
            }

            Class<?> type = user.getClass();
            try {
                test = type.getDeclaredMethod(testName, TapTest.class);
            }
            catch (NoSuchMethodException e) {
                diag (testName + ": method not found");
                confess (e);
                offense |= EX_NOTMETH;
                continue;
            }

            try {
                test.invoke(user, self);
            }
            catch (IllegalAccessException
                    | IllegalArgumentException e)
            {
                diag (testName + ": invocation error");
                confess (e);
                offense |= EX_BADCALL;
                continue;
            }
            catch (InvocationTargetException e) {
                Throwable c;
                if (e.getCause() instanceof ExceptionInInitializerError) {
                    diag (testName + ": static initializer error");
                    c = e.getCause().getCause();
                    offense |= EX_BADINIT;
                }
                else {
                    diag (testName + ": runtime exception/error");
                    c = e.getCause();
                    offense |= EX_BADMETH;
                }
                confess (c);
            }

            /* We are more likely to use subtests than not, yes...
             * But subtests are still SUB-tests -- we are going to
             * report that for the FULL test here. */
            int subcount = self.count();
            int subfail = self.failed();
            if (subcount != planet) {
                diag ("%s planned to run %d test%s, "
                    + "but ran %d instead.",
                    testName, planet,
                    planet == 1 ? "" : "s", subcount);
            }
            offense |= EX_BADPLAN;
            count += subcount;
            fail = subfail;
        }

        return offense;
    }

    /**
     * Execute tests that fails fast.  This had to be a
     * separate method, because we need to throw anything.
     * Primarily for use in jdb(1) since I have no idea how
     * to jump to the stack frame of the cause exception
     * rather than the exception itself.
     * <p>
     * Sorry for copy-and-pasting the whole thing; but I
     * really can't think of a better way to add this feature.
     *
     * @param todo list of test numbers
     * @param plan list of plans; must have the same dimensions
     * @param factory a function that returns a reference to
     *   a fresh test object
     * @param fatal types of exceptions to make fatal
     * @return error status; see the EX_* flags
     * @throws NullPointerException if specifically
     *   if {@code factory} returns null (for EX_ERRINIT)
     * @throws IllegalStateException if the number of tests run
     *   in any subtest does not match the provided subplan
     *   (for EX_BADPLAN)
     * @throws Throwable literally anything else
     * @since 0.2
     */
    public int runff (int[] todo, int[] plan,
        Supplier<?> factory, int fatal) throws Throwable
    {
        int offense = 0;

        for (int i = 0; i < todo.length; ++i) {
            int t = todo[i];
            int planet = plan[i];
            String testName = "test" + t;

            Object user;
            TapTest self = new TapTest();
            self.subplan(planet);
            Method test;

            try {
                user = factory.get();
            }
            catch (Exception e) {
                diag (testName + ": construction failed");
                offense |= EX_ERRINIT;
                if ((fatal & offense) != 0) {
                    throw e;
                }
                confess (e);
                continue;
            }

            /* Performance-wise, I don't know if this does anything
             * I'm just doing this because I finally just learned
             * how to write functional Java (1.8) for once... :) */
            if ((fatal & EX_ERRINIT) != 0) {
                Objects.requireNonNull(testName,
                    () -> testName + ": constructor returned null");
            }
            else {
                if (user == null) {
                    diag (testName + ": constructor returned null");
                    offense |= EX_ERRINIT;
                }
                continue;
            }

            Class<?> type = user.getClass();
            try {
                test = type.getDeclaredMethod(testName, TapTest.class);
            }
            catch (NoSuchMethodException e) {
                diag (testName + ": method not found in "
                    + type.getName());
                offense |= EX_NOTMETH;
                if ((fatal & offense) != 0) {
                    throw e;
                }
                confess (e);
                continue;
            }

            try {
                test.invoke(user, self);
            }
            catch (IllegalAccessException
                    | IllegalArgumentException e)
            {
                diag (testName + ": invocation error");
                offense |= EX_BADCALL;
                if ((fatal & offense) != 0) {
                    throw e;
                }
                confess (e);
                continue;
            }
            catch (InvocationTargetException e) {
                Throwable c;
                if (e.getCause() instanceof ExceptionInInitializerError) {
                    diag (testName + ": static initializer error");
                    c = e.getCause().getCause();
                    offense |= EX_BADINIT;
                }
                else {
                    diag (testName + ": runtime exception/error");
                    c = e.getCause();
                    offense |= EX_BADMETH;
                }
                if ((fatal & offense) != 0) {
                    throw c;
                }
                confess (c);
            }

            int subcount = self.count();
            int subfail = self.failed();
            if (subcount != planet) {
                diag ("%s planned to run %d test%s, "
                    + "but ran %d instead.",
                    testName, planet,
                    planet == 1 ? "" : "s", subcount);
            }
            offense |= EX_BADPLAN;
            if ((fatal & offense) != 0) {
                throw new IllegalStateException(
                    testName + " subplan foiled");
            }
            count += subcount;
            fail = subfail;
        }

        return offense;
    }

    /*
     * The subtest model is a layer on top of TapTest.java.
     * Its function is purely informational; the planning and
     * everything is still controlled by the API there.
     */

    /** ID of the current subtest. */
    private int subtest = 0;
    /** Expected subtotal from the current subtest. */
    private int subplan = 0;
    /** Subtotal of the current subtest. */
    private int subcount = 0;
    /** Failed subtotal of the current subtest. */
    private int subfail = 0;
    /** Whether we are currently in a subtest. */
    private boolean subtesting = false;

    /** Failed todo tests. */
    private int subtodo = 0;
    /** Passed todo tests. */
    private List<Integer> subdone = new ArrayList<Integer>();

    /**
     * Declare that all subtests from here must run
     * this many tests.  This supercedes the subplan()
     * method that should overridden to be of use in the
     * previous embedded version.
     *
     * @param plan number of test cases expected for
     *   subsequent subtests to run, if positive; it is
     *   otherwise ineffective.
     * @since 0.2
     */
    protected void subplan (int plan)
    {
        subplan = plan;
    }

    /**
     * Initialize and Enter a subtest.  This switches the subtest
     * module to a state that must be followed by {@link #done_subtest}.
     *
     * @throws IllegalStateException if currently in a subset
     * @param id the test index.  Used for informational reports.
     * @since 0.1.6
     */
    protected void init_subtest (int id)
    {
        if (subtesting) {
            throw new IllegalStateException (
                "You are already in subtest "
                + subtest + ".");
        }
        subtest = id;
        subcount = 0;
        subfail = 0;
        subdone.clear();
        subtesting = true;
        note ("Start subtest %d", subtest);
    }

    /**
     * Conclude and Exit a subtest.  The reverts the state of
     * subtest module to the one set by {@link init_subtest}.
     *
     * @throws IllegalStateException if not currently in a subset
     * @return the appropriate return value per P101/P102 requirements.
     * @since 0.1.6
     */
    protected boolean done_subtest ()
    {
        if (!subtesting) {
            throw new IllegalStateException (
                "You are not in an ongoing subtest.");
        }
        subtesting = false;
        /* Be more chatty if we are embedded */
        boolean embedded = origin < 0;
        note ("End of subtest %d", subtest);
        note ("Ran %d test%s and failed %d test%s.",
                subcount, subcount == 1 ? "" : "s",
                subfail, subfail == 1 ? "" : "s");
        if (embedded && !subdone.isEmpty()) {
            /* String.join wants an array/iterable of CharSequences.
             * Fair enough... but I don't want anything more than a
             * one-liner, so gonna use Java 8 Stream.... :) */
            String celebrate = String.join(", ", subdone
            /* There are two flavors of Integer.toString, one static
             * and one of instance (overrides Object.toString()).
             * Sadly, Java can't decide between which one's which.
             * So we will use something less ambiguous. */
                .stream().map(String::valueOf)
            /* Without this, Java would think this is an Object[] */
                .toArray(String[]::new));
            diag ("You passed TODO test"
                + (subdone.size() == 1 ? " " : "s ")
                + celebrate + "! Nicely done.");
            diag ("You may tick %s off your bucket list now.",
                  subdone.size() == 1 ? "that" : "those");
        }
        if (subtodo > 0) {
            log (!embedded, "You still have %d TODO test%s to go.",
                subtodo, subtodo == 1 ? "" : "s");
        }
        if (subfail > 0) {
            log (!embedded, "It seems that subtest %d failed. "
                + "Tough luck.", subtest);
            return false;
        }
        if (embedded) {
            if (subplan > 0 && subcount != subplan) {
                diag ("Bad plan! Subtest %d planned to run %d test%s, "
                    + "but ended up running %d test%s.",
                    subtest, subplan, subplan == 1 ? "" : "s",
                    subcount, subcount == 1 ? "" : "s");
                return false;
            }
        }
        return true;
    }

    /**
     * Offset to add to printed test number. May be the number of
     * tests before this test.  Whatever makes sense to you.
     */
    private int origin = 0;

    /**
     * Override offset to add to the printed test number.  Pass Negative
     * to enable embedded mode (for JUnit5).
     * @param offset the desired offset
     * @since 0.1.6
     */
    protected void origin(int offset) { origin = offset; }

    /* Advanced test functions */

    /**
     * Assert {@code a} equals {@code b}.
     * Null is only considered to be equal to null.
     * Mimics is() in Test::More.  Note that the order
     * is reversed compared to JUnit.
     *
     * @param a left operand; what we got
     * @param b right operand; what we expected
     * @param mess test name
     * @return whether {@code a} and {@code b} are equal
     * @since 0.1.6
     */
    protected boolean is(Object a, Object b, String mess) {
        return is(a, b, mess, 1);
    }

    /**
     * Alternative version with call stack depth.
     *
     * @param a a
     * @param b b
     * @param testName test name
     * @param depth stack depth
     * @return the same thing
     * @since 0.1.6
     */
    protected boolean is(
        Object a, Object b, String testName, int depth
    ) {
        StringBuilder mess = new StringBuilder();
        mess.append(testName); mess.append("\n");
        boolean good = !(
            a == null ^ b == null ||
            a != null && !a.equals(b)
        );
        if (!good) {
            mess.append(String.format("Verdict: %s != %s\n", a, b));
            mess.append("      got: ");
            if (a != null) {
                mess.append(a.getClass().getName()); mess.append(" - ");
            }
            mess.append(a); mess.append("\n");

            mess.append(" expected: ");
            if (b != null) {
                mess.append(b.getClass().getName()); mess.append(" - ");
            }
            mess.append(b); mess.append("\n");
        }
        return ok (good, mess.toString(), depth + 1);
    }
}
/* vim:set sts=4 sw=4 et ts=8 ai si: */
