package ir.neshane.contract.http;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class OpenApiLoader {

    private OpenApiLoader() {}

    public static OpenAPI load(Path path) {
        String source;
        try {
            source = Files.readString(path);
        } catch (IOException exception) {
            throw new ContractViolationException("Unable to read OpenAPI contract: " + path);
        }

        ParseOptions options = new ParseOptions();
        options.setResolve(false);
        options.setResolveFully(false);
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(source, null, options);
        List<String> messages = result.getMessages() == null ? List.of() : result.getMessages();
        if (result.getOpenAPI() == null || !messages.isEmpty()) {
            throw new ContractViolationException(
                    "Invalid OpenAPI contract " + path + ":", messages.isEmpty()
                            ? List.of("parser returned no OpenAPI model")
                            : messages);
        }
        return result.getOpenAPI();
    }
}
