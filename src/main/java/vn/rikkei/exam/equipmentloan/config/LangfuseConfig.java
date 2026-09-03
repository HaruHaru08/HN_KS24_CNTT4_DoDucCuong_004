//package vn.rikkei.exam.equipmentloan.config;
//
//import lombok.Value;
//import org.antlr.runtime.debug.Tracer;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class LangfuseConfig {
//    @Value("${langfuse.host}")
//    private String host;
//    @Value("${LANGFUSE_PUBLIC_KEY}")
//    private String publicKey;
//    @Value("${LANGFUSE_SECRET_KEY}")
//    private String secretKey;
//    @Value("${otel.exporter.otlp.endpoint}")
//    private String otelEndpoint;
//    @Value("${spring.application.name}")
//    private String serviceName;
//
//    @Bean
//    public LangfuseClient langfuseClient() {
//        return LangfuseClient.builder()
//                .url(host)
//                .credentials(publicKey, secretKey)
//                .build();
//    }
//    @Bean
//    public OpenTelemetry openTelemetry() {
//        String auth = publicKey + ":" + secretKey;
//
//        String basicAuth = Base64.getEncoder()
//                .encodeToString(auth.getBytes(StandardCharsets.UTF_8));
//
//        OtlpHttpSpanExporter exporter =
//                OtlpHttpSpanExporter.builder()
//                        .setEndpoint(
//                                otelEndpoint + "/v1/traces"
//                        )
//                        .addHeader(
//                                "Authorization",
//                                "Basic " + basicAuth
//                        )
//                        .addHeader(
//                                "x-langfuse-ingestion-version",
//                                "4"
//                        )
//                        .build();
//
//        SdkTracerProvider tracerProvider =
//                SdkTracerProvider.builder()
//                        .addSpanProcessor(BatchSpanProcessor.builder(exporter).build())
//                        .build();
//
//        OpenTelemetrySdk openTelemetrySdk =
//                OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).buildAndRegisterGlobal();
//
//        return openTelemetrySdk;
//    }
//    @Bean
//    public Tracer tracer(OpenTelemetry openTelemetry) {
//        return openTelemetry.getTracer("vn.rikkei.exam.equipmentloan", "1.0.0");
//    }
//}
