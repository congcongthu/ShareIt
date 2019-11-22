package sjtu.opennet.honvideo;

public class VideoExceptions {
    static class UnexpectedEndException extends Exception{
        public UnexpectedEndException(){
            super("Unexpected End Task. Please judge end task outside the task use isEnd() or isDestroy().");
        }
        public UnexpectedEndException(String message){
            super(message);
        }
        public UnexpectedEndException(String message, Throwable cause){
            super(message, cause);
        }
        public UnexpectedEndException(Throwable cause){
            super(cause);
        }
        public  UnexpectedEndException(String message, Throwable cause, boolean enableSuppression, boolean writeableStatckTrace){
            super(message, cause, enableSuppression, writeableStatckTrace);
        }
    }
}
