import com.google.api.services.drive.DriveRequest;

import java.io.IOException;

public class Utils {

    public static <T> T executeWithBackoff(DriveRequest<T> dr) throws IOException {
        int count = 0;
        int backoff = 1;
        int maxTries = 10;
        while (true) {
            try {
                return dr.execute();
            } catch (IOException e) {
                try {
                    System.out.println("Failure: " + e.getMessage() + " Backing off " + backoff);
                    Thread.sleep(backoff * 1000);
                    backoff = backoff * 2;
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
                if (++count == maxTries) throw e;
            }
        }
    }
}
