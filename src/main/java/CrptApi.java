import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CrptApi {
    private TimeUnit timeUnit;
    private int requestLimit;
    private ObjectMapper objectMapper;
    private ExecutorService executorService;
    private ScheduledExecutorService scheduledExecutorService;
    private HttpClient httpClient;
    private AtomicInteger requestCounter;
    private String url;

    public CrptApi(TimeUnit timeUnit, Integer requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.objectMapper = new ObjectMapper();
        this.executorService = Executors.newFixedThreadPool(5);
        this.httpClient = HttpClient.newHttpClient();
        this.url = "https://ismp.crpt.ru/api/v3/lk/documents/create";
        reseterRequestCounter();
    }
    private void reseterRequestCounter(){
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        requestCounter = new AtomicInteger(0);
        scheduledExecutorService.scheduleAtFixedRate(
                () -> {
                    requestCounter.set(0);
                    System.out.println("Reset request counter");
                    },
                0,
                convertTimeUnit(timeUnit),
                TimeUnit.MILLISECONDS
        );
    }

    private Runnable prepareWrappedHttpRequest(CrptApi.Document document){
        return () -> {
            try {
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(document)))
                        .build();
                httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding());
            } catch (Exception ex) {}
        };
    }

    public void createDocument(CrptApi.Document document, String signature){
        synchronized (this) {
            while (requestCounter.incrementAndGet() > requestLimit) {
                try {
                    Thread.sleep(1000);
                }catch (InterruptedException ex){}
            }
            executorService.execute(prepareWrappedHttpRequest(document));
        }
    }

    public long convertTimeUnit(TimeUnit timeUnit){
        switch (timeUnit) {
            case DAYS:
                return TimeUnit.DAYS.toMillis(1);
            case HOURS:
                return TimeUnit.HOURS.toMillis(1);
            case MINUTES:
                return TimeUnit.MINUTES.toMillis(1);
            case SECONDS:
                return TimeUnit.SECONDS.toMillis(1);
            case MILLISECONDS:
                return 1l;
            case MICROSECONDS:
                return 1 / 1000;
            case NANOSECONDS:
                return 1 / 1000000;
            default:
                return 0l;
        }
    }

    @Data
     public static class Document {
        private String participantInn;
        private String docId;
        private String docStatus;
        private String docType;
        private boolean importRequest;
        private String ownerInn;
        private String producerInn;
        private String productionDate;
        private String productionType;
        private String certificateDocument;
        private String certificateDocumentDate;
        private String certificateDocumentNumber;
        private String tnvedCode;
        private String uitCode;
        private String uituCode;
        private String regDate;
        private String regNumber;
    }

    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 2);
        for (int i = 0; i < 6; i++) {
            Runnable run1 = () -> {
                CrptApi.Document doc = new CrptApi.Document();
                crptApi.createDocument(doc, "Signature");
            };
            new Thread(run1).start();
        }
    }

}
