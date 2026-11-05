package ir.neshane.contract.tenant;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class TenantContractVerifier {

    private TenantContractVerifier() {}

    public static void main(String[] args) {
        if (args.length != 2 || !"validate-candidate".equals(args[0])) {
            throw new TenantContractException("Expected validate-candidate <contracts/tenant>");
        }
        validateCandidate(Path.of(args[1]));
    }

    static void validateCandidate(Path root) {
        JsonSupport json = new JsonSupport();
        TenantContractCatalog catalog = new TenantContractCatalog(root, json);
        List<String> violations = new ArrayList<>();
        JsonNode manifestSchema = json.read(root.resolve("manifest.schema.json"));
        json.validateSchemaDeclaration(manifestSchema, "manifest.schema.json")
                .forEach(violations::add);

        for (TenantContractEntry entry : catalog.entries()) {
            JsonNode schema = json.read(entry.schemaPath());
            json.validateSchemaDeclaration(schema, entry.schemaPath().toString())
                    .forEach(violations::add);
            if (!entry.schemaId().equals(schema.path("$id").asText())) {
                violations.add(entry.schemaPath() + ": $id does not match manifest schemaId");
            }
        }
        if (!violations.isEmpty()) {
            throw new TenantContractException("Tenant candidate contract validation failed:", violations);
        }
        System.out.println("Validated " + catalog.entries().size()
                + " proposed V3-021 tenant contract schema(s); no accepted baseline was created");
    }

    static List<String> validateInstance(
            Path root, String contractName, String version, JsonNode instance) {
        JsonSupport json = new JsonSupport();
        TenantContractEntry entry = new TenantContractCatalog(root, json).find(contractName, version);
        return json.validate(json.read(entry.schemaPath()), instance);
    }
}
