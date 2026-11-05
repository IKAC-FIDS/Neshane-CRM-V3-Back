package ir.neshane.contract.http;

import io.swagger.v3.oas.models.OpenAPI;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class HttpContractVerifier {

    private HttpContractVerifier() {}

    public static void main(String[] args) {
        if (args.length == 0) {
            throw new ContractViolationException("Expected validate or diff command");
        }
        switch (args[0]) {
            case "validate" -> validate(Path.of(required(args, 1)));
            case "diff" -> diff(Path.of(required(args, 1)), Path.of(required(args, 2)));
            default -> throw new ContractViolationException("Unknown command: " + args[0]);
        }
    }

    static void validate(Path root) {
        List<Path> contracts;
        try (Stream<Path> files = Files.walk(root)) {
            contracts = files.filter(Files::isRegularFile)
                    .filter(HttpContractVerifier::isOpenApiDocument)
                    .sorted(Comparator.naturalOrder())
                    .toList();
        } catch (IOException exception) {
            throw new ContractViolationException("Unable to enumerate contracts: " + root);
        }
        if (contracts.isEmpty()) {
            throw new ContractViolationException("No OpenAPI documents found under " + root);
        }
        HttpContractRules rules = new HttpContractRules();
        List<String> violations = new ArrayList<>();
        contracts.forEach(path -> rules.validate(OpenApiLoader.load(path)).stream()
                .map(issue -> path + ": " + issue)
                .forEach(violations::add));
        if (!violations.isEmpty()) {
            throw new ContractViolationException("HTTP contract validation failed:", violations);
        }
        System.out.println("Validated " + contracts.size() + " OpenAPI documents");
    }

    static void diff(Path baselinePath, Path candidatePath) {
        OpenAPI baseline = OpenApiLoader.load(baselinePath);
        OpenAPI candidate = OpenApiLoader.load(candidatePath);
        List<String> changes = new OpenApiCompatibilityChecker().breakingChanges(baseline, candidate);
        if (!changes.isEmpty()) {
            throw new ContractViolationException("Breaking HTTP contract changes detected:", changes);
        }
        System.out.println("No breaking HTTP contract changes detected");
    }

    private static boolean isOpenApiDocument(Path path) {
        String name = path.getFileName().toString();
        return "openapi.yaml".equals(name) || name.endsWith(".openapi.yaml");
    }

    private static String required(String[] args, int index) {
        if (args.length <= index || args[index].isBlank()) {
            throw new ContractViolationException("Missing command argument " + index);
        }
        return args[index];
    }
}
