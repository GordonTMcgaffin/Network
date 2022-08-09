import java.io.Serializable;

public class FileData implements Serializable {
    String fileName;
    int fileSize;
    int fileTransferSize;
    public FileData(String fileName, int fileSize, int fileTransferSize){
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileTransferSize = fileTransferSize;
    }
}
