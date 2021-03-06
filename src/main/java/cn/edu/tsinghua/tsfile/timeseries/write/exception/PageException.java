package cn.edu.tsinghua.tsfile.timeseries.write.exception;

/**
 * Exception occurs when writing a page
 */
public class PageException extends WriteProcessException {

    private static final long serialVersionUID = 7385627296529388683L;

    public PageException(String msg) {
        super(msg);
    }
}
