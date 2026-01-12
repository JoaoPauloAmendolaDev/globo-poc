package poc.globo.globostreaming.exception;

public class ActiveSubscriptionExistsException extends RuntimeException {
    public ActiveSubscriptionExistsException(String message) {
        super(message);
    }
}

