package ir.neshane.contract.routing;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class RoutingContractVerifier {

    private RoutingContractVerifier() {}

    public static void main(String[] args) {
        if (args.length != 2 || !"validate-candidate".equals(args[0])) {
            throw new RoutingContractException("Expected validate-candidate <contracts/routing>");
        }
        validateCandidate(Path.of(args[1]));
    }

    static void validateCandidate(Path root) {
        JsonSupport json = new JsonSupport();
        RoutingContractCatalog catalog = new RoutingContractCatalog(root, json);
        List<String> violations = new ArrayList<>();
        json.validateSchemaDeclaration(json.read(root.resolve("manifest.schema.json")),
                        "manifest.schema.json")
                .forEach(violations::add);
        for (RoutingContractEntry entry : catalog.entries()) {
            JsonNode schema = json.read(entry.schemaPath());
            json.validateSchemaDeclaration(schema, entry.schemaPath().toString())
                    .forEach(violations::add);
            if (!entry.schemaId().equals(schema.path("$id").asText())) {
                violations.add(entry.schemaPath() + ": $id does not match manifest");
            }
        }
        if (!violations.isEmpty()) {
            throw new RoutingContractException("Routing candidate validation failed:", violations);
        }
        System.out.println("Validated " + catalog.entries().size()
                + " proposed V3-022 routing schemas; no accepted baseline was created");
    }

    static List<String> validateInstance(
            Path root, String name, String version, JsonNode instance) {
        JsonSupport json = new JsonSupport();
        RoutingContractEntry entry = new RoutingContractCatalog(root, json).find(name, version);
        return json.validate(json.read(entry.schemaPath()), instance);
    }
}
