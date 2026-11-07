package app.mikraal.activities.tasks.authz;

import dev.openfga.sdk.api.client.OpenFgaClient;
import dev.openfga.sdk.api.client.model.ClientBatchCheckItem;
import dev.openfga.sdk.api.client.model.ClientBatchCheckRequest;
import dev.openfga.sdk.api.client.model.ClientBatchCheckSingleResponse;
import dev.openfga.sdk.api.client.model.ClientCheckRequest;
import dev.openfga.sdk.api.client.model.ClientListObjectsRequest;
import dev.openfga.sdk.api.client.model.ClientReadRequest;
import dev.openfga.sdk.api.client.model.ClientTupleKey;
import dev.openfga.sdk.api.client.model.ClientTupleKeyWithoutCondition;
import dev.openfga.sdk.api.client.model.ClientWriteRequest;
import dev.openfga.sdk.api.configuration.ClientReadOptions;
import dev.openfga.sdk.api.model.AuthorizationModel;
import dev.openfga.sdk.api.model.ReadResponse;
import dev.openfga.sdk.api.model.TupleKey;
import dev.openfga.sdk.api.model.WriteAuthorizationModelRequest;
import dev.openfga.sdk.errors.FgaInvalidParameterException;
import dev.openfga.sdk.errors.FgaValidationError;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/** Blocking access to OpenFGA for this module. Only the authz package talks to OpenFGA. */
@Component
class OpenFga {

    /** A relationship tuple in OpenFGA notation, e.g. {@code person:1 owner task:2}. */
    record Tuple(String user, String relation, String object) {
    }

    private final OpenFgaClient client;
    private final JsonMapper json;

    OpenFga(OpenFgaClient client, JsonMapper json) {
        this.client = client;
        this.json = json;
    }

    boolean check(Tuple tuple) {
        return await(() -> client.check(new ClientCheckRequest()
                .user(tuple.user()).relation(tuple.relation())._object(tuple.object()))).getAllowed();
    }

    /** One server-side batch per call; answers come back in the order of {@code checks}. */
    List<Boolean> check(List<Tuple> checks) {
        if (checks.isEmpty()) {
            return List.of();
        }
        List<ClientBatchCheckItem> items = IntStream.range(0, checks.size())
                .mapToObj(i -> new ClientBatchCheckItem()
                        .user(checks.get(i).user()).relation(checks.get(i).relation())._object(checks.get(i).object())
                        .correlationId(Integer.toString(i)))
                .toList();
        Map<String, Boolean> byId = await(() -> client.batchCheck(new ClientBatchCheckRequest().checks(items)))
                .getResult().stream()
                .collect(Collectors.toMap(ClientBatchCheckSingleResponse::getCorrelationId, this::allowed));
        return IntStream.range(0, checks.size()).mapToObj(i -> byId.get(Integer.toString(i))).toList();
    }

    List<String> listObjects(String user, String relation, String type) {
        return await(() -> client.listObjects(new ClientListObjectsRequest().user(user).relation(relation).type(type)))
                .getObjects();
    }

    /** Every stored tuple matching the filter; null fields match anything. */
    List<Tuple> read(String user, String relation, String object) {
        var request = new ClientReadRequest().user(user).relation(relation)._object(object);
        List<Tuple> tuples = new ArrayList<>();
        String token = null;
        do {
            var options = new ClientReadOptions().continuationToken(token);
            ReadResponse page = await(() -> client.read(request, options));
            page.getTuples().stream().map(t -> tuple(t.getKey())).forEach(tuples::add);
            token = page.getContinuationToken();
        } while (token != null && !token.isEmpty());
        return tuples;
    }

    /** Writes and deletes in one transaction. */
    void write(Collection<Tuple> writes, Collection<Tuple> deletes) {
        if (writes.isEmpty() && deletes.isEmpty()) {
            return;
        }
        var request = new ClientWriteRequest()
                .writes(writes.stream().map(t -> new ClientTupleKey()
                        .user(t.user()).relation(t.relation())._object(t.object())).toList())
                .deletes(deletes.stream().map(t -> new ClientTupleKeyWithoutCondition()
                        .user(t.user()).relation(t.relation())._object(t.object())).toList());
        await(() -> client.write(request));
    }

    /** Writes {@code model} as the store's latest model unless the latest one already has the same types. */
    boolean ensureModel(WriteAuthorizationModelRequest model) {
        AuthorizationModel latest = await(client::readLatestAuthorizationModel).getAuthorizationModel();
        if (latest != null && Objects.equals(latest.getSchemaVersion(), model.getSchemaVersion())
                && pruned(latest.getTypeDefinitions()).equals(pruned(model.getTypeDefinitions()))) {
            return false;
        }
        await(() -> client.writeAuthorizationModel(model));
        return true;
    }

    /** The server stores empty relations and null metadata where the file has none, so compare without them. */
    private JsonNode pruned(Object value) {
        return prune(json.valueToTree(value));
    }

    private static JsonNode prune(JsonNode node) {
        if (node.isObject()) {
            ObjectNode copy = JsonNodeFactory.instance.objectNode();
            node.properties().forEach(p -> {
                JsonNode child = prune(p.getValue());
                if (!(child.isNull() || child.isEmpty() && child.isContainer()
                        || child.isString() && child.stringValue().isEmpty())) {
                    copy.set(p.getKey(), child);
                }
            });
            return copy;
        }
        if (node.isArray()) {
            ArrayNode copy = JsonNodeFactory.instance.arrayNode();
            node.forEach(item -> copy.add(prune(item)));
            return copy;
        }
        return node;
    }

    @FunctionalInterface
    interface Call<T> {
        CompletableFuture<T> start() throws FgaInvalidParameterException, FgaValidationError;
    }

    static <T> T await(Call<T> call) {
        try {
            return call.start().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AuthorizationUnavailableException(e);
        } catch (ExecutionException | FgaInvalidParameterException | FgaValidationError e) {
            throw new AuthorizationUnavailableException(e);
        }
    }

    private boolean allowed(ClientBatchCheckSingleResponse response) {
        if (response.getError() != null) {
            throw new AuthorizationUnavailableException(new IllegalStateException(response.getError().getMessage()));
        }
        return response.isAllowed();
    }

    private static Tuple tuple(TupleKey key) {
        return new Tuple(key.getUser(), key.getRelation(), key.getObject());
    }
}
