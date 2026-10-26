package com.tomtom.genesis.platform.jdk17;

import java.util.Random;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.Collectors;

/**
 * Java 17 (JEP 356) - enhanced pseudo-random number generators.
 *
 * Turns "random numbers in Java" from one concrete class into an interface
 * hierarchy with a service-loader-style factory and several modern algorithms.
 */
public class RandomGenerators {

    public static void main(String[] args) {
        theOldSituation();
        theInterface();
        whatIsAvailable();
        splittableAndJumpable();
        choosing();
    }

    private static void theOldSituation() {
        header("What we had before");

        System.out.println("  java.util.Random          - a 48-bit LCG from 1995. Statistically weak,");
        System.out.println("                              and every call does a CAS on an AtomicLong.");
        System.out.println("  ThreadLocalRandom (7)     - fast, per-thread, but a final class with a");
        System.out.println("                              private constructor - impossible to abstract over.");
        System.out.println("  SplittableRandom (8)      - good for fork/join, but again unrelated to Random.");
        System.out.println("  SecureRandom              - cryptographic, slow, different API shape.");
        System.out.println();
        System.out.println("Four unrelated types. Writing a method that takes 'some source of randomness'");
        System.out.println("meant accepting java.util.Random and forcing everyone into the slow one.");
    }

    private static void theInterface() {
        header("RandomGenerator - one interface over all of them");

        RandomGenerator gen = RandomGenerator.getDefault();
        System.out.println("getDefault() implementation = " + gen.getClass().getSimpleName());
        System.out.println("nextInt(100)                = " + gen.nextInt(100));
        System.out.println("nextDouble()                = " + String.format("%.4f", gen.nextDouble()));
        System.out.println("nextBoolean()               = " + gen.nextBoolean());

        // Streams come from the interface, so every implementation has them.
        String five = gen.ints(5, 1, 7).mapToObj(String::valueOf).collect(Collectors.joining(", "));
        System.out.println("ints(5, 1, 7)               = " + five + "   (five dice rolls)");

        // The old Random now implements the new interface, so legacy code fits in.
        RandomGenerator legacy = new Random(42);
        System.out.println("new Random(42) is a RandomGenerator = " + (legacy instanceof RandomGenerator));

        System.out.println();
        System.out.println("Take RandomGenerator as a parameter, not Random. Tests can then pass a");
        System.out.println("fixed-seed generator and production can pass the fast one, with no");
        System.out.println("change to the method under test.");
    }

    private static void whatIsAvailable() {
        header("Discovering algorithms at runtime");

        String algorithms = RandomGeneratorFactory.all()
                .map(RandomGeneratorFactory::name)
                .sorted()
                .collect(Collectors.joining(", "));
        System.out.println("available on this JDK:");
        System.out.println("  " + algorithms);

        RandomGenerator named = RandomGenerator.of("Xoshiro256PlusPlus");
        System.out.println();
        System.out.println("RandomGenerator.of(\"Xoshiro256PlusPlus\").nextInt(100) = " + named.nextInt(100));

        RandomGeneratorFactory<RandomGenerator> factory =
                RandomGeneratorFactory.of("L64X128MixRandom");
        System.out.println("factory.create(seed) is reproducible:");
        System.out.println("  run 1 = " + factory.create(2024L).nextInt(1000));
        System.out.println("  run 2 = " + factory.create(2024L).nextInt(1000) + "   (same seed, same value)");

        System.out.println();
        System.out.println("The naming scheme: LXM algorithms are written L<a>X<b>Mix, where <a> is");
        System.out.println("the size of the linear congruential part and <b> the xor-shift part.");
        System.out.println("Bigger numbers mean a longer period and more state.");
    }

    private static void splittableAndJumpable() {
        header("The interesting capability interfaces");

        RandomGeneratorFactory<RandomGenerator> factory = RandomGeneratorFactory.of("L64X128MixRandom");
        System.out.println("L64X128MixRandom:");
        System.out.println("  isSplittable  = " + factory.isSplittable());
        System.out.println("  isJumpable    = " + factory.isJumpable());
        System.out.println("  isStreamable  = " + factory.isStreamable());
        System.out.println("  stateBits     = " + factory.stateBits());

        RandomGenerator.SplittableGenerator parent =
                RandomGenerator.SplittableGenerator.of("L64X128MixRandom");
        RandomGenerator.SplittableGenerator child = parent.split();
        System.out.println();
        System.out.println("parent.nextInt(100) = " + parent.nextInt(100));
        System.out.println("child.nextInt(100)  = " + child.nextInt(100));

        System.out.println();
        System.out.println("split() produces a generator statistically independent of its parent.");
        System.out.println("That is what you want in parallel work: each task splits its own");
        System.out.println("generator instead of sharing one and contending on it - and unlike");
        System.out.println("seeding each thread by hand, the streams are guaranteed not to overlap.");
        System.out.println();
        System.out.println("jump() and leap() skip a fixed distance ahead in the sequence, which");
        System.out.println("gives reproducible per-worker substreams in simulations.");
    }

    private static void choosing() {
        header("Which one to use");

        System.out.println("  general purpose, single thread   -> RandomGenerator.getDefault()");
        System.out.println("                                      (L32X64MixRandom on current JDKs)");
        System.out.println("  many threads, no shared state    -> ThreadLocalRandom.current()");
        System.out.println("  fork/join or parallel streams    -> a SplittableGenerator, then split()");
        System.out.println("  reproducible tests               -> RandomGeneratorFactory.of(..).create(seed)");
        System.out.println("  anything security related        -> SecureRandom, and nothing else");
        System.out.println();
        System.out.println("None of the algorithms here are cryptographically secure. Observing a few");
        System.out.println("outputs lets an attacker predict the rest. Tokens, passwords, session ids");
        System.out.println("and nonces always come from SecureRandom.");
    }

    private static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
