package ir.neshane.customer.domain.architecture;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.*;
import java.util.spi.ToolProvider;

/** Production-bytecode dependency gate; specification classes are never included. */
public final class DomainArchitectureSpecification {
    private static final String ROOT = "ir.neshane.customer.domain";

    public static void main(String[] args) {
        if (args.length == 0) throw new IllegalArgumentException("Production class directories required");
        var probe = new TreeMap<String, Set<String>>();
        probe.put("a", Set.of("b")); probe.put("b", Set.of("a"));
        if (cycle(probe).isEmpty()) throw new AssertionError("Cycle detector failed its negative control");
        probe.put("b", Set.of());
        if (!cycle(probe).isEmpty()) throw new AssertionError("Cycle detector failed its positive control");

        var output = new StringWriter();
        var errors = new StringWriter();
        var arguments = new ArrayList<>(List.of("-verbose:package", "-filter:none"));
        for (String arg : args) arguments.add(Path.of(arg).toAbsolutePath().toString());
        int exit = ToolProvider.findFirst("jdeps").orElseThrow(() -> new IllegalStateException("JDK jdeps required"))
            .run(new PrintWriter(output), new PrintWriter(errors), arguments.toArray(String[]::new));
        if (exit != 0) throw new AssertionError("jdeps failed: " + errors);
        var graph = new TreeMap<String, Set<String>>();
        for (String line : output.toString().split("\\R")) {
            var fields = line.trim().split("\\s+");
            if (fields.length < 3 || !fields[1].equals("->")) continue;
            String from = fields[0], to = fields[2];
            if (from.equals(ROOT)) throw new AssertionError("Flat domain root is forbidden: " + line);
            if (!from.startsWith(ROOT + ".")) continue;
            graph.computeIfAbsent(from, ignored -> new TreeSet<>());
            if (to.startsWith(ROOT + ".")) {
                graph.computeIfAbsent(to, ignored -> new TreeSet<>());
                if (!from.equals(to)) graph.get(from).add(to);
            } else if (!to.startsWith("java.")) {
                throw new AssertionError("Non-JDK dependency from domain: " + line);
            }
        }
        if (graph.isEmpty()) throw new AssertionError("No production domain packages analyzed");
        var found = cycle(graph);
        if (!found.isEmpty()) throw new AssertionError("Domain package dependency cycle: " + String.join(" -> ", found));
        System.out.println("ARCHITECTURE PASS: " + graph.size() + " packages, "
            + graph.values().stream().mapToInt(Set::size).sum() + " edges, zero package cycles");
    }

    private static List<String> cycle(Map<String, Set<String>> graph) {
        var done = new HashSet<String>();
        var path = new ArrayList<String>();
        for (String node : graph.keySet()) {
            var found = visit(node, graph, done, path);
            if (!found.isEmpty()) return found;
        }
        return List.of();
    }

    private static List<String> visit(String node, Map<String, Set<String>> graph, Set<String> done, List<String> path) {
        int start = path.indexOf(node);
        if (start >= 0) {
            var found = new ArrayList<>(path.subList(start, path.size())); found.add(node); return found;
        }
        if (!done.add(node)) return List.of();
        path.add(node);
        for (String next : graph.getOrDefault(node, Set.of())) {
            var found = visit(next, graph, done, path);
            if (!found.isEmpty()) return found;
        }
        path.removeLast();
        return List.of();
    }
}
