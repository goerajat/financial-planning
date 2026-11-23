package rg.financialplanning.web.aggregator;

public class AggregatorException extends RuntimeException {
    public AggregatorException(String message) {
        super(message);
    }

    public AggregatorException(String message, Throwable cause) {
        super(message, cause);
    }
}
