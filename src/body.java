    /** Number of tests that have been run. */
    private int count = 0;

    /**
     * {@return the number of tests we ran}
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
     */
    protected boolean ended ()
    {
        return ended;
    }

    /**
     * Print stack trace of an exception in form of diagnosis.
     *
     * @param e the throwable whose stack trace is delicious
     * @see #diag
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
     * reality.  Then exit with a &gt;0 status.
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
                plan, "s".repeat(plan == 1 ? 0 : 1),
                count, count == 1 ? " was" : "s were");
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
     * @return Am I okay?
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
     */
    protected void pass (String mess) {
        ok (true, mess, false, 0);
    }

    /**
     * {@code ok(0)} with no trace.
     *
     * @param mess a description
     * @see #ok(boolean, String, boolean, int)
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
     */
    protected void skip (int num, String mess) {
        for (int i = 0; i < num; ++i) {
            ok (true, "SKIP " + mess);
        }
    }

    /* Main program (involves heavy reflection code) */

    /**
     * Whether to print stack trace on test failure
     * or any uncaught exception.
     * See the --trace option in {@link #exec}.
     */
    private static boolean printTrace = false;

    /**
     * Entry point.
     *
     * @param prog argument 0; program name
     * @param args argument vector from argument 1 and on
     * @param type the class to test
     * @return exit code.  =0 on success, =1 on test failure,
     *    if {@code -e} or {@code --exit-code} is given, and
     *    =2 on incorrect command-line usage.
     */
    protected int exec(String prog, String[] args,
        Class<? extends __CLASS__> type)
    {
        int optind = 0;
        final String USAGE = String.format(
            "usage: %s [-e] [-t] [[-i] <range>...] [-x <range>...]",
            prog
        );

        // options
        boolean status = false;

        int[] rangeLo = new int[args.length];
        int[] rangeHi = new int[args.length];
        int rangeCount = 0;
        // include? +1 exclude? -1
        int color = +1;

        /* dynamic constructor https://stackoverflow.com/a/46390971 */
        Constructor<? extends __CLASS__> constructor;
        try {
            constructor = type.getDeclaredConstructor();
        }
        catch (NoSuchMethodException e) {
            skip_all ("cannot find the constructor of " + type.getName());
            confess (e);
            return 1;
        }

        /* acquire a representative instance before we do anything
         * (doing this because i imagine dynamically finding static
         * variables/methods will be hell) */
        __CLASS__ reference;
        try {
            reference = constructor.newInstance();
        }
        /* TO POTENTIAL READERS: Errors are intentionally not handled
         * because it indicates a fatal JVM error that we probably
         * should just allow happen.  (Yes, I know stack overflowerror...
         * but at this stage, you throw a stack overflowerror... you might
         * as well get rounded up :) */
        catch (Exception e) {
            skip_all ("instantiation of " + type.getName() + " failed");
            confess (e);
            return 1;
        }

        /* now lock him up in an interrogation room.... */
        final int availableTests;
        try {
            availableTests = reference.avail();
        }
        catch (Exception e) {
            skip_all (type.getName() + " reference refuses to "
                + "declare what tests it offers");
            confess (e);
            return 1;
        }

        final int firstTest;
        try {
            firstTest = reference.start();
        }
        catch (Exception e) {
            skip_all (type.getName() + " reference refuses to "
                + "declare what its first test is");
            confess (e);
            return 1;
        }

        if (firstTest < 0) {
            /* "test-n" would not be a valid Java
             * identifier; don't let that happen. */
            throw new IllegalArgumentException(
                "first test cannot be negative (you "
                + "gave me " + firstTest + ")");
        }
        int offset = firstTest - 1;

        for (; optind < args.length; ++optind) {
            switch (args[optind]) {
            case "-h":
            case "--help":
                System.out.println(USAGE);
                System.out.println();
                System.out.print("""
                This is a TAP tester written for Java.

                options:
                  -h, --help        print this help message
                  -t, --trace       include assertion stack trace
                  -e, --exit-code   set >0 exit code upon failure
                  -i, --include <range>...   include these ranges
                                    (default: include everything)
                  -x, --exclude <range>...   exclude these ranges
                                    (default: exclude nothing)

                Copyright 2025, Ethan Meng <uwisc at endfindme.com>
                """.stripIndent());
                return 0;
            case "-t":
            case "--trace":
                printTrace = true;
                break;
            case "-e":
            case "--exit-code":
                status = true;
                break;
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
                    return 2;
                }
                int start;
                try {
                    start = Integer.parseInt(operands[0]);
                }
                catch (NumberFormatException e) {
                    System.err.printf(
                        "error parsing start number %s in %s: %s\n",
                        operands[0], args[optind], e.getMessage()
                    );
                    return 2;
                }
                start -= offset;
                if (start < 1 || start > availableTests) {
                    System.err.printf(
                        "error: start number %s in %s out of range: "
                        + "must be at least %d and at most %d\n",
                        start + offset, args[optind], firstTest,
                        offset + availableTests
                    );
                    return 2;
                }
                rangeLo[rangeCount] = start * color;
                int end;
                try {
                    end = Integer.parseInt(operands[1]);
                }
                catch (NumberFormatException e) {
                    System.err.printf(
                        "error parsing start number %s in %s: %s\n",
                        operands[1], args[optind], e.getMessage()
                    );
                    return 2;
                }
                end -= offset;
                if (end < start - 1 || end > availableTests) {
                    System.err.printf(
                        "error: end number %s in %s out of range: "
                        + "must be at least %d and at most %d\n",
                        end + offset, args[optind], start - 1 + offset,
                        offset + availableTests
                    );
                    return 2;
                }
                rangeHi[rangeCount] = end * color;
                rangeCount++;
            }
        }

        // lambda wants to see something final
        final int RANGE_TOTAL_COUNT = rangeCount;
        final int INDEX_OFFSET = offset;
        int[] todo = IntStream.rangeClosed(1, availableTests)
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

        int[] plan = new int[todo.length];
        Throwable[] badplan = new Throwable[todo.length];
        int total = 0;

        /* ask him again; see if he spills anything
         * if he doesn't, i'll tell on him... */
        for (int i = 0; i < todo.length; ++i) {
            try {
                plan[i] = reference.subplan(todo[i]);
            }
            catch (Exception e) {
                // plan[i] = 0;
                badplan[i] = e; // confess later
            }
            total += plan[i];
        }

        if (total == 0) {
            skip_all ("nothing to test");
        }
        else {
            plan (total);
        }

        boolean allgood = true;
        for (int i = 0; i < todo.length; ++i) {
            int t = todo[i];
            int planet = plan[i];

            /* just because we didn't get a plan doesn't mean
             * we won't try.  it just means we are going to take */
            if (planet == 0 && badplan[i] != null) {
                diag ("Subtest " + t + " died on me when I asked for its plan:");
                confess (badplan[i]);
                allgood = false;
            }

            int goal = count + planet;
            __CLASS__ instance;
            try {
                instance = constructor.newInstance();
            }
            catch (Exception e) {
                String mess = type.getName() + " instantiation failed "
                    + "when I was just about to run subtest " + t;
                if (todo.length > 1) {
                    skip (goal - count, mess);
                }
                else {
                    skip_all (mess);
                }
                confess (e);
                continue;
            }
            instance.origin(count);

            String testName = "test" + t;
            Method test;
            try {
                test = type.getDeclaredMethod(testName);
            }
            catch (NoSuchMethodException e) {
                String mess = "missing " + testName;
                if (todo.length > 1) {
                    skip (goal - count, mess);
                }
                else {
                    skip_all (mess);
                }
                confess (e);
                continue;
            }
            if (!test.canAccess(instance)) {
                String mess = "cannot access " + testName
                    + " (access modifier too strict?)";
                if (todo.length > 1) {
                    skip (goal - count, mess);
                }
                else {
                    skip_all (mess);
                }
                continue;
            }
            boolean result;
            String verdict = null;
            String[] diagnosis = null;
            try {
                test.invoke(instance);
            }
            catch (InvocationTargetException e) {
                if (printTrace) {
                    StringWriter w = new StringWriter();
                    e.getCause().printStackTrace(new PrintWriter(w));
                    diagnosis = w.toString().split("\\n");
                }
                verdict = e.getCause().getMessage();
                // throwable message may be null!
                if (verdict == null) {
                    verdict = e.getCause().getClass().getName();
                }
            }
            catch (IllegalAccessException e) {
                // should never happen
                throw new IllegalStateException(e);
            }
            if (verdict != null) {
                diag (
                    "Test%s aborted with an "
                    + "exception: %s", todo.length > 1
                    ? String.format(" %d", t) : "", verdict
                );
                if (diagnosis != null) {
                    diag ("Stack trace:");
                    for (String line : diagnosis) {
                        diag (line);
                    }
                }
                fail += 1;
            }
            count += instance.count();
            while (count < goal) {
                if (verdict != null) {
                    skip (goal - count, "aborted due to "
                        + "previous fatal exception");
                }
                else {
                    fail (goal - count, "plan too ambitious");
                }
            }
        }

        return status && allgood && fail > 0 ? 1 : 0;
    }

    /*
     * The subtest model is a layer on top of TapTest.java.
     * Its function is purely informational; the planning and
     * everything is still controlled by the API there.
     */

    /** ID of the current subtest. */
    private int subtest = 0;
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
     * Initialize and Enter a subtest.  This switches the subtest
     * module to a state that must be followed by {@link #done_subtest}.
     *
     * @throws IllegalStateException if currently in a subset
     * @param id the test index.  Used for informational reports.
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
     */
    protected boolean done_subtest ()
    {
        if (!subtesting) {
            throw new IllegalStateException (
                "You are not in an ongoing subtest.");
        }
        /* Be more chatty if we are embedded */
        boolean embedded = origin < 0;
        subtesting = false;
        note ("End of subtest %d", subtest);
        note ("Ran %d test%s and failed %d test%s.",
                subcount, "s".repeat(subcount == 1 ? 0 : 1),
                subfail, "s".repeat(subfail == 1 ? 0 : 1));
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
            diag ("You passed TODO tests " + celebrate +
                  "! Nicely done.");
            diag ("You may tick %s off your bucket list now.",
                  subdone.size() == 1 ? "that" : "those");
        }
        if (subtodo > 0) {
            log (!embedded, "You still have %d TODO test%s to go.",
                subtodo, "s".repeat(subtodo == 1 ? 0 : 1));
        }
        if (subfail > 0) {
            log (!embedded, "It seems that subtest %d failed. "
                + "Tough luck.", subtest);
            return false;
        }
        if (embedded) {
            int plan = subplan (subtest);
            if (subcount != plan) {
                diag ("Bad plan! Subtest %d planned to run %d test%s, "
                    + "but ended up running %d test%s.",
                    subtest, plan, "s".repeat(plan == 1 ? 0 : 1),
                    subcount, "s".repeat(subcount == 1 ? 0 : 1));
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
     * @param testName test name
     * @return whether {@code a} and {@code b} are equal
     */
    protected void is(Object a, Object b, String mess) {
        is(a, b, mess, 1);
    }

    /**
     * Alternative version with call stack depth.
     *
     * @param a a
     * @param b b
     * @param testName
     * @param depth stack depth
     * @return the same thing
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
