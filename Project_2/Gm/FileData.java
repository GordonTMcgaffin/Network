import java.io.Serializable;

public class FileData implements Serializable {
    String fileName;
    int fileSize;

    String protocol;

    public FileData(String fileName, int fileSize, String protocol) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.protocol = protocol;
    }
}
