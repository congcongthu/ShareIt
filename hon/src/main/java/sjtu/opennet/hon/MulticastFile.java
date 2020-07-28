package sjtu.opennet.hon;

public class MulticastFile {
    String threadId;
    String fileId;
    String senderAddress;
    String fileName;
    String filePath;
    long sendTime;

    public MulticastFile(String threadId, String FileId,  String senderAddress, String fileName, String filePath, long sendTime) {
        this.threadId = threadId;
        this.fileId = fileId;
        this.senderAddress = senderAddress;
        this.fileName = fileName;
        this.filePath = filePath;
        this.sendTime = sendTime;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public String getSenderAddress() {
        return senderAddress;
    }

    public void setSenderAddress(String senderAddress) {
        this.senderAddress = senderAddress;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getSendTime() {
        return sendTime;
    }

    public void setSendTime(long sendTime) {
        this.sendTime = sendTime;
    }
}
