import java.io.Serializable;

public class FileData implements Serializable {
    String fileName;
    int fileSize;

    public FileData(String fileName, int fileSize){
        this.fileName = fileName;
        this.fileSize = fileSize;
    }
}
