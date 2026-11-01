package ir.neshane.contract.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class EventSchemaCompatibilityTest {

    private final JsonSupport json = new JsonSupport();
    private final EventSchemaCompatibilityChecker checker = new EventSchemaCompatibilityChecker();

    @Test
    void optionalAdditionAndEnumNarrowingAreConsumerBackwardCompatible() throws Exception {
        var baseline = json.read(resource("schemas/baseline.schema.json"));
        var candidate = json.read(resource("schemas/additive-candidate.schema.json"));

        assertThat(checker.breakingChanges(baseline, candidate)).isEmpty();
    }

    @Test
    void breakingChangesAreRejectedConservatively() throws Exception {
        var baseline = json.read(resource("schemas/baseline.schema.json"));
        var candidate = json.read(resource("schemas/breaking-candidate.schema.json"));

        assertThat(checker.breakingChanges(baseline, candidate))
                .anyMatch(change -> change.contains("type changed"))
                .anyMatch(change -> change.contains("enum widened"))
                .anyMatch(change -> change.contains("new required property newrequired"))
                .anyMatch(change -> change.contains("removed required property changekind"))
                .anyMatch(change -> change.contains("schema reference changed"))
                .anyMatch(change -> change.contains("schema.tags items: type changed"));
    }

    @Test
    void additivePropertyIsRejectedWhenTheExistingConsumerSchemaIsClosed() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode baseline = (ObjectNode) json.read(resource("schemas/baseline.schema.json"));
        baseline.put("additionalProperties", false);
        ObjectNode candidate = baseline.deepCopy();
        ((ObjectNode) candidate.path("properties"))
                .set("note", mapper.createObjectNode().put("type", "string"));

        assertThat(checker.breakingChanges(baseline, candidate))
                .anyMatch(change -> change.contains("new property note"));
    }

    @Test
    void removingAnEnumConstraintIsRejectedAsWidening() throws Exception {
        ObjectNode baseline = (ObjectNode) json.read(resource("schemas/baseline.schema.json"));
        ObjectNode candidate = baseline.deepCopy();
        ((ObjectNode) candidate.path("properties").path("changekind")).remove("enum");

        assertThat(checker.breakingChanges(baseline, candidate))
                .anyMatch(change -> change.contains("enum constraint removed"));
    }

    @Test
    void definitionAndUnknownKeywordChangesRequireManualReview() throws Exception {
        ObjectNode baseline = (ObjectNode) json.read(resource("schemas/baseline.schema.json"));
        ObjectNode candidate = baseline.deepCopy();
        ((ObjectNode) candidate.path("$defs").path("detail")).put("maxProperties", 2);
        candidate.put("futureAssertion", true);

        assertThat(checker.breakingChanges(baseline, candidate))
                .anyMatch(change -> change.contains("$defs changed; manual review required"))
                .anyMatch(change -> change.contains("unsupported keyword futureAssertion changed"));
    }

    private Path resource(String name) throws Exception {
        return Path.of(getClass().getResource("/" + name).toURI());
    }
}
