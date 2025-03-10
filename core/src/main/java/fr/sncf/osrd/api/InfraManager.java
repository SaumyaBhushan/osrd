package fr.sncf.osrd.api;

import com.squareup.moshi.JsonDataException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fr.sncf.osrd.reporting.exceptions.OSRDError;
import fr.sncf.osrd.infra.api.signaling.SignalingInfra;
import fr.sncf.osrd.infra.implementation.signaling.SignalingInfraBuilder;
import fr.sncf.osrd.infra.implementation.signaling.modules.bal3.BAL3;
import fr.sncf.osrd.railjson.schema.infra.RJSInfra;
import fr.sncf.osrd.reporting.warnings.WarningRecorder;
import fr.sncf.osrd.utils.jacoco.ExcludeFromGeneratedCodeCoverage;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class InfraManager {
    static final Logger logger = LoggerFactory.getLogger(InfraManager.class);

    private final ConcurrentHashMap<String, InfraCacheEntry> infraCache = new ConcurrentHashMap<>();

    public void forEach(BiConsumer<String, InfraCacheEntry> action) {
        infraCache.forEach(action);
    }

    public static final class InfraLoadException extends OSRDError {
        private static final long serialVersionUID = 4291184310194002894L;
        public static final String osrdErrorType = "infra_loading";

        public final InfraStatus sourceOperation;

        InfraLoadException(String message, InfraStatus sourceOperation, Throwable cause) {
            super(message, ErrorCause.USER, cause);
            this.sourceOperation = sourceOperation;
        }

        InfraLoadException(String message, InfraStatus sourceOperation) {
            super(message, ErrorCause.USER);
            this.sourceOperation = sourceOperation;
        }

        @Override
        public String toString() {
            return getMessage() + " in state " + sourceOperation.name() + ": " + super.getCause();
        }
    }

    public static final class UnexpectedHttpResponse extends Exception {
        private static final long serialVersionUID = 1052450937805248669L;

        public final transient Response response;

        UnexpectedHttpResponse(Response response) {
            super(String.format("unexpected http response %d", response.code()));
            this.response = response;
        }

        @Override
        public String toString() {
            return super.toString() + ": " + response.toString();
        }
    }

    public enum InfraStatus {
        INITIALIZING(false),
        DOWNLOADING(false),
        PARSING_JSON(false),
        PARSING_INFRA(false),
        CACHED(true),
        ERROR(true);

        static {
            INITIALIZING.transitions = new InfraStatus[] { DOWNLOADING };
            DOWNLOADING.transitions = new InfraStatus[] { PARSING_JSON, ERROR };
            PARSING_JSON.transitions = new InfraStatus[] { PARSING_INFRA, ERROR };
            PARSING_INFRA.transitions = new InfraStatus[] { CACHED, ERROR };
            // if an infrastructure is already cached, or in an error state,
            // a new version can re-trigger a downloaad
            CACHED.transitions = new InfraStatus[] { DOWNLOADING };
            ERROR.transitions = new InfraStatus[] { DOWNLOADING };
        }

        private InfraStatus(boolean isStable) {
            this.isStable = isStable;
        }

        public final boolean isStable;
        private InfraStatus[] transitions;

        boolean canTransitionTo(InfraStatus newStatus) {
            for (var status : transitions)
                if (status == newStatus)
                    return true;
            return false;
        }
    }

    public static final class InfraCacheEntry {
        public InfraStatus status = InfraStatus.INITIALIZING;
        public InfraStatus lastStatus = null;
        public Exception lastError = null;
        public SignalingInfra infra = null;
        public String expectedVersion = null;

        void transitionTo(InfraStatus newStatus) {
            assert status.canTransitionTo(newStatus);
            this.lastStatus = this.status;
            this.status = newStatus;
            if (newStatus.isStable)
                this.notifyAll();
        }

        public void registerError(Exception error) {
            transitionTo(InfraStatus.ERROR);
            this.lastError = error;
        }

        public void waitUntilStable() throws InterruptedException {
            while (!status.isStable)
                this.wait();
        }
    }

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .readTimeout(120, TimeUnit.SECONDS)
            .build();

    private final String baseUrl;
    private final String authorizationToken;

    public InfraManager(String baseUrl, String authorizationToken) {
        this.baseUrl = baseUrl;
        this.authorizationToken = authorizationToken;
    }

    private Request buildRequest(String endpointUrl) {
        var builder = new Request.Builder().url(endpointUrl);
        if (authorizationToken != null)
                builder = builder.header("Authorization", authorizationToken);
        return builder.build();
    }

    @ExcludeFromGeneratedCodeCoverage
    @SuppressFBWarnings({"RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"})
    private SignalingInfra downloadInfra(
            InfraCacheEntry cacheEntry,
            String infraId,
            String expectedVersion,
            WarningRecorder warningRecorder
    ) throws InfraLoadException {
        // create a request
        var endpointUrl = String.format("%sinfra/%s/railjson/", baseUrl, infraId);
        var request = buildRequest(endpointUrl);

        try {
            // use the client to send the request
            logger.info("starting to download {}", endpointUrl);
            cacheEntry.transitionTo(InfraStatus.DOWNLOADING);

            var response = httpClient.newCall(request).execute();
            if (!response.isSuccessful())
                throw new UnexpectedHttpResponse(response);

            // Parse the response
            logger.info("parsing the JSON of {}", endpointUrl);
            cacheEntry.transitionTo(InfraStatus.PARSING_JSON);

            RJSInfra rjsInfra;
            try (var body = response.body()) {
                rjsInfra = RJSInfra.adapter.fromJson(body.source());
            }

            if (rjsInfra == null)
                throw new JsonDataException("RJSInfra is null");

            // Parse railjson into a proper infra
            logger.info("parsing the infra of {}", endpointUrl);
            cacheEntry.transitionTo(InfraStatus.PARSING_INFRA);
            var infra = SignalingInfraBuilder.fromRJSInfra(
                    rjsInfra,
                    Set.of(new BAL3(warningRecorder)),
                    warningRecorder
            );

            // Cache the infra
            logger.info("successfuly cached {}", endpointUrl);
            cacheEntry.infra = infra;
            cacheEntry.expectedVersion = expectedVersion;
            cacheEntry.transitionTo(InfraStatus.CACHED);
            return infra;
        } catch (IOException | UnexpectedHttpResponse | JsonDataException e) {
            cacheEntry.registerError(e);
            throw new InfraLoadException("error while loading new infra", cacheEntry.lastStatus, e);
        } catch (Exception e) {
            cacheEntry.registerError(e);
            throw e;
        }
    }

    /** Load an infra given an id. Cache infra for optimized future call */
    @ExcludeFromGeneratedCodeCoverage
    @SuppressFBWarnings({"REC_CATCH_EXCEPTION"})
    public SignalingInfra load(String infraId, String expectedVersion, WarningRecorder warningRecorder)
            throws InfraLoadException, InterruptedException {
        try {
            var prevCacheEntry = infraCache.putIfAbsent(infraId, new InfraCacheEntry());
            var cacheEntry = infraCache.get(infraId);

            synchronized (cacheEntry) {
                // if there was no cache entry, download the infra again
                if (prevCacheEntry == null || cacheEntry.status == InfraStatus.ERROR
                        || expectedVersion != null && !expectedVersion.equals(cacheEntry.expectedVersion))
                    return downloadInfra(cacheEntry, infraId, expectedVersion, warningRecorder);

                // otherwise, wait for the infra to reach a stable state
                cacheEntry.waitUntilStable();
                if (cacheEntry.status == InfraStatus.CACHED)
                    return cacheEntry.infra;
                if (cacheEntry.status == InfraStatus.ERROR)
                    throw new InfraLoadException("cached exception", cacheEntry.lastStatus, cacheEntry.lastError);
                throw new InfraLoadException("invalid status after waitUntilStable", cacheEntry.status);
            }
        } catch (Exception e) {
            logger.error("exception while loading infra", e);
            throw e;
        }
    }
}
